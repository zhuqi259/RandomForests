package akka.tutorials.scala

import java.net.URLDecoder
import java.util.Properties

import cn.edu.jlu.ccst.randomforests.util.ScalaFileUtils
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.util.CoreMap

import scala.concurrent.duration._

/**
  *
  * @author zhuqi259
  *
  *         中文分词的测试用例
  */
class TextSegmentation(val data: String) extends Serializable {
  val classifier = {
    val props: Properties = new Properties
    props.setProperty("sighanCorporaDict", data)
    props.setProperty("serDictionary", data + "/dict-chris6.ser.gz")
    props.setProperty("inputEncoding", ScalaFileUtils.UTF8)
    props.setProperty("sighanPostProcessing", "true")
    val classifier = new CRFClassifier(props)
    classifier.loadClassifierNoExceptions(data + "/ctb.gz", props)
    // flags must be re-set after data is loaded
    classifier.flags.setProperties(props)
    classifier
  }
}

object TextSegmentation extends Serializable {
  val doSegment = (data: String, classifier: CRFClassifier[_ <: CoreMap]) => {
    classifier.segmentString(data).toArray.asInstanceOf[Array[String]]
  }

  def decode(s: String, enc: String = "UTF8"): String = {
    URLDecoder.decode(s, enc)
  }

  def main(args: Array[String]) {
    val start: Long = System.currentTimeMillis
    val data = args(0)
    val segment = new TextSegmentation(data)
    val classifier = segment.classifier

    val sentences = List("南京市长江大桥", "他和我在学校里常打桌球", "南京,市长,江大桥", "期待双12能淘到更有性价比的宝贝")
    val results = sentences.map(doSegment(_, classifier)).map(_.toSeq)
    println(results.mkString("\n"))
    println("\n\tCalculation time: \t%s"
      .format((System.currentTimeMillis - start).millis))
  }
}

