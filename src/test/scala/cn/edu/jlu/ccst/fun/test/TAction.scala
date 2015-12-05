package cn.edu.jlu.ccst.fun.test

import scala.concurrent.duration._

/**
  * @author zhuqi259
  *         定义了一个AOP模式的trait,用于打印方法执行时间
  */
trait TAction {
  def doSomething()
}

trait Timer extends TAction {
  abstract override def doSomething() = {
    val start: Long = System.currentTimeMillis
    super.doSomething()
    val duration = (System.currentTimeMillis - start).millis
    println("duration = %s".format(duration))
  }
}