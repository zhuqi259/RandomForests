package cn.edu.jlu.ccst.randomforests.wordsegmenter

import java.io.{File, FileOutputStream}
import java.nio.ByteBuffer
import java.util.Properties

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinRouter
import cn.edu.jlu.ccst.randomforests.util.ScalaFileUtils
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.io.Source

/**
  * @author zhuqi259
  *         akka  分词并行
  */
object WordSegmenter extends App {

  /**
    * @param nrOfWorkers 定义我们会启动多少工作actor
    * @param nrOfElements 定义发送给工作actor的每个整数段的大小
    * @param dict  训练字典
    * @param stopWord 停止词字典
    * @param inputFiles 待分词数据
    */
  def doWork(nrOfWorkers: Int, nrOfElements: Int, dict: String, stopWord: String, inputFiles: Array[(String, String, String)]) {

    val ant = new Ant(dict, stopWord, inputFiles)
    //    val fileName = inputPrefix + ccc
    //    val prefix = outputPrefix + ccc

    // Create an Akka system
    val system = ActorSystem("WSSystem")

    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[WSListener], name = "listener")

    // create the master
    val master = system.actorOf(Props(new WSMaster(
      ant, nrOfWorkers, nrOfElements, listener)),
      name = "master")

    // start work
    master ! WSStartWork
  }

  override def main(args: Array[String]): Unit = {
    val size = args.length // size = 8
    assert(size > 6 && size % 2 == 0)
    val input = for (i <- 0 until (size - 6) / 2) yield (args(4) + args(6 + 2 * i), args(6 + 2 * i + 1), args(5) + args(6 + 2 * i))
    doWork(nrOfWorkers = args(0).toInt, nrOfElements = args(1).toInt, dict = args(2), stopWord = args(3), inputFiles = input.toArray)
  }
}

// 定义消息
sealed trait WSMessage

// 1. StartWork => 发送给master启动工作
case object WSStartWork extends WSMessage

// 2. WSRealWork => master发送给各个worker，包含工作内容jobs [start, end)
// case class WSRealWork(ant: Ant, fileName: String, entryKey: String, filePrefix: String, names: List[String]) extends WSMessage
case class WSRealWork(ant: Ant, start: Int, end: Int) extends WSMessage

// 3. WSResult => worker发送[返回]给master的结果(此处返回分词成功/失败)
case class WSResult(flag: Boolean) extends WSMessage

// 4. WSHeart => master发送给Listener的消息，包含最终计算结果和消耗时间
case class WSHeart(current: Int, all: Int, allGood: Boolean, duration: Duration, finalFlag: Boolean) extends WSMessage


/**
  * 分词器实际工作者
  * @param dict 训练字典
  * @param stopWord 停止词字典
  * @param inputFiles 待分词数据
  *                   Array[(String, String, String)]
  *                   (fileName, key, prefix) => (输入文件前缀，key[搜狗数据集标志]，输出文件前缀)
  */
class Ant(dict: String, stopWord: String, inputFiles: Array[(String, String, String)]) {
  val times = 3

  val classifier = {
    val props: Properties = new Properties
    props.setProperty("sighanCorporaDict", dict)
    props.setProperty("serDictionary", dict + "/dict-chris6.ser.gz")
    props.setProperty("inputEncoding", ScalaFileUtils.UTF8)
    props.setProperty("sighanPostProcessing", "true")
    val classifier = new CRFClassifier[CoreLabel](props)
    classifier.loadClassifierNoExceptions(dict + "/ctb.gz", props)
    classifier
  }
  val stopWordSet = Source.fromFile(stopWord).getLines.toSet

  def doWordSegmentation(data: String, destination: String): Boolean = {
    print("-")
    def loop(time: Int): Boolean = {
      if (time >= times) false
      else {
        try {
          val result = classifier.segmentString(data).toArray.asInstanceOf[Array[String]]
          val cleanedResult = (for (item <- result if !stopWordSet.contains(item) && item.length > 1) yield item) mkString " "
          val f = new FileOutputStream(destination).getChannel
          f write ByteBuffer.wrap(cleanedResult.getBytes)
          f.close()
          true
        } catch {
          case _: Throwable => loop(time + 1)
        }
      }
    }
    loop(0)
  }

  val jobs = {
    // {} for 上一层的括号必须加上, 运算符优先级
    for ((fileName, key, prefix) <- inputFiles) yield {
      val set = ScalaFileUtils.getFileMap(fileName, key).entrySet
      for (entry <- set) yield {
        val names = entry.getValue
        for (name <- names) yield {
          val source = fileName + File.separator + entry.getKey + File.separator + name
          val destination = prefix + File.separator + entry.getKey + File.separator + name
          (source, destination)
        }
      }
    }
  }.flatten.flatten

  val length = jobs.length

}

/**
  * worker
  */
class WSWorker extends Actor {

  def doWordSegmentation(ant: Ant, start: Int, end: Int): Boolean = {
    var flag = true
    for (i ← start until end) {
      val (source, destination) = ant.jobs(i)
      try {
        val lines = Source.fromFile(source, ScalaFileUtils.GBK)
        val result = (for (data <- lines.getLines()) yield ScalaFileUtils.htmlDecode(data.replace("\t", " "))) mkString " "
        val dir: File = new File(destination)
        val res: Boolean = dir.getParentFile.mkdirs
        if (!res) {
        }
        flag &= ant.doWordSegmentation(result, destination)
      } catch {
        case _: Throwable =>
          println("\n\n\tReadFile Error! (index, source, destination)=> (%d, %s, %s)\n\n".format(i, source, destination))
      }
    }
    flag
  }

  def receive = {
    case WSRealWork(ant, start, end) =>
      println("(start,end) => (%d, %d)".format(start, end))
      sender ! WSResult(doWordSegmentation(ant, start, end)) // perform the work
  }
}

/**
  * master
  * @param ant 工作者
  * @param nrOfWorkers 定义我们会启动多少工作actor
  * @param nrOfElements 定义发送给工作actor的每个整数段的大小
  * @param listener 用来向外界报告最终的结果
  */
class WSMaster(ant: Ant, nrOfWorkers: Int, nrOfElements: Int, listener: ActorRef)
  extends Actor {

  var nrOfGoodResults: Int = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter = context.actorOf(
    Props[WSWorker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

  val quotientOfMessages = ant.length / nrOfElements
  val remainderOfMessages = ant.length % nrOfElements
  val nrOfMessages = if (remainderOfMessages == 0) quotientOfMessages else quotientOfMessages + 1

  def receive = {
    // handle messages ...
    case WSStartWork => // 用来启动分词
      println("ant.length = " + ant.length)
      println("quotientOfMessages = " + quotientOfMessages)
      println("nrOfMessages = " + nrOfMessages)
      println("remainderOfMessages = " + remainderOfMessages)
      for (i <- 0 until quotientOfMessages) {
        val start = i * nrOfElements
        val end = start + nrOfElements
        workerRouter ! WSRealWork(ant, start, end)
      }
      if (remainderOfMessages > 0) {
        val start = quotientOfMessages * nrOfElements
        val end = start + remainderOfMessages
        workerRouter ! WSRealWork(ant, start, end)
      }
    case WSResult(value) => // 汇总结果
      if (value)
        nrOfGoodResults += 1
      nrOfResults += 1
      if (nrOfResults < nrOfMessages) {
        // Send the result to the listener
        listener ! WSHeart(current = nrOfResults, all = nrOfMessages, allGood = nrOfGoodResults == nrOfResults, duration = (System.currentTimeMillis - start).millis, finalFlag = false)
      } else if (nrOfResults == nrOfMessages) {
        // Send the result to the listener
        listener ! WSHeart(current = nrOfResults, all = nrOfMessages, allGood = nrOfGoodResults == nrOfResults, duration = (System.currentTimeMillis - start).millis, finalFlag = true)
        // Stops this actor and all its supervised children
        context.stop(self)
      }
  }
}

class WSListener extends Actor {
  def receive = {
    case WSHeart(current, all, allGood, duration, finalFlag) =>
      println("\n\n\tcurrent: %d, all: %d,allGood: %s\tCalculation time: \t%s\n\n"
        .format(current, all, allGood, duration))
      if (finalFlag)
        context.system.shutdown()
  }
}