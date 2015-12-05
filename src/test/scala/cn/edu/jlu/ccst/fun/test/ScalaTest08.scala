package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         所谓反序数，即有这样成对的数，其特点是其中一个数的数字排列顺序完全颠倒过来，就变成另一个数，如102和201，36和63等，
  *         简单的理解就是顺序相反的两个数，我们把这种成对的数互称为反序数。反序数唯一不可能出现以0结尾的数。
  *
  *         一个3位数各位上的数字都不相同，它和它的反序数的乘积是280021，这个3位数应是多少？
  */
class ScalaTest08 extends TAction {
  override def doSomething() = {
    val result = {
      for {
        a <- 1 to 9
        b <- 0 to 9 if a != b
        c <- a + 1 to 9 if c != b
        t1 = 100 * a + 10 * b + c; t2 = 100 * c + 10 * b + a if t1 * t2 == 280021
      } yield (t1, t2)
    }
    println(result mkString ", ")
  }
}

object ScalaTest08 extends App {
  new ScalaTest08 with Timer doSomething()
}
