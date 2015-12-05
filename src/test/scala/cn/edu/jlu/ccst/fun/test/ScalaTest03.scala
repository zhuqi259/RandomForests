package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         完全数（Perfect number)，又称完美数或完备数，是一些特殊的自然数。
  *         它所有的真因子(即除了自身以外的约数）的和（即因子函数），恰好等于它本身。
  *         例如，第一个完全数是6，它有约数1、2、3、6，除去它本身6外，其余3个数相加，1+2+3=6。
  *         第二个完全数是28，它有约数1、2、4、7、14、28，除去它本身28外，其余5个数相加，1+2+4+7+14=28。
  *
  *         编程求10000以内的完全数。
  */
class ScalaTest03 extends TAction {
  def isPerfectNumber(n: Int): Boolean = {
    def loop(t: Int, a: Int, b: Int): Int = {
      if (a > b) t - n
      else if (a == b && a * b == n) t + a
      else loop(if (n % a == 0) t + a + b else t, a + 1, n / (a + 1))
    }
    loop(0, 1, n) == n
  }

  override def doSomething() = {
    val result = for (i <- 2 to 10000 if isPerfectNumber(i)) yield i
    println(result mkString ", ")
  }
}

object ScalaTest03 extends App {
  new ScalaTest03 with Timer doSomething()
}
