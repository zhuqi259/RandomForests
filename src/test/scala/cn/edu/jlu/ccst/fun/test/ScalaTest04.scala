package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         220的真因数之和为1+2+4+5+10+11+20+22+44+55+110=284
  *         284的真因数之和为1+2+4+71+142=220
  *         毕达哥拉斯把这样的数对A、B称为相亲数：A的真因数之和为B，而B的真因数之和为A。
  *
  *         求100000以内的相亲数。
  */
class ScalaTest04 extends TAction {
  def sumOfFactors(n: Int): Int = {
    def loop(t: Int, a: Int, b: Int): Int = {
      if (a > b) t - n
      else if (a == b && a * b == n) t + a
      else loop(if (n % a == 0) t + a + b else t, a + 1, n / (a + 1))
    }
    loop(0, 1, n)
  }

  override def doSomething() = {
    val result = for {
      i <- 2 to 100000
      j = sumOfFactors(i) if i < j && sumOfFactors(j) == i
    } yield (i, j)
    println(result mkString ", ")
  }
}

object ScalaTest04 extends App {
  new ScalaTest04 with Timer doSomething()
}
