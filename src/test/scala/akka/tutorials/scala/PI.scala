package akka.tutorials.scala

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinRouter

import scala.concurrent.duration._

/**
  * akka并行的例子
  */
object PI extends App {

  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) {
    // Create an Akka system
    val system = ActorSystem("PiSystem")

    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener)),
      name = "master")

    // start the calculation
    master ! Calculate
  }

  calculate(nrOfWorkers = 4, nrOfElements = 100000, nrOfMessages = 10000)

}

// 定义了四种消息
sealed trait PiMessage

// 1. Calculate => 发送给master启动计算
case object Calculate extends PiMessage

// 2. Work => master发送给各个worker，包含工作内容
case class Work(start: Int, nrOfElements: Int) extends PiMessage

// 3. Result => worker发送[返回]给master的计算结果
case class Result(value: Double) extends PiMessage

// 4. PiApproximation => master发送给Listener的消息，包含最终计算结果和消耗时间
case class PiApproximation(pi: Double, duration: Duration) extends PiMessage

/**
  * worker actor
  */
class Worker extends Actor {

  // calculatePiFor ...
  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    def loop(i: Int, acc: Double): Double = {
      if (i >= start + nrOfElements) acc
      else loop(i + 1, acc + 4.0 * (1 - (i & 1) * 2) / (2 * i + 1))
    }
    loop(start, 0.0)
    //      var acc = 0.0
    //      for (i ← start until (start + nrOfElements))
    //        acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    //      acc
  }

  def receive = {
    case Work(start, nrOfElements) =>
      sender ! Result(calculatePiFor(start, nrOfElements)) // perform the work
  }
}

/**
  * master actor
  * @param nrOfWorkers 定义我们会启动多少工作actor
  * @param nrOfMessages 定义会有多少整数段发送给工作actor
  * @param nrOfElements 定义发送给工作actor的每个整数段的大小
  * @param listener 用来向外界报告最终的计算结果
  */
class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef)
  extends Actor {

  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

  def receive = {
    // handle messages ...
    case Calculate => // 用来启动计算过程
      for (i <- 0 until nrOfMessages) workerRouter ! Work(i * nrOfElements, nrOfElements)
    case Result(value) => // 汇总结果
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) {
        // Send the result to the listener
        listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
        // Stops this actor and all its supervised children
        context.stop(self)
      }
  }
}

class Listener extends Actor {
  def receive = {
    case PiApproximation(pi, duration) =>
      println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s"
        .format(pi, duration))
      context.system.shutdown()
  }
}

