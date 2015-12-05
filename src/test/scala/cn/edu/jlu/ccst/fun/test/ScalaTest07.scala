package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         如果某个数的平方的末尾几位等于这个数，那么就称这个数为自守数。显然，5和6是一位自守数（5*5=25，6*6=36)。
  *         25*25=625,76*76=5776，所以25和76是两位自守数。
  *
  *         求10000以内的自守数。
  */
class ScalaTest07 extends TAction {

  def selfGuard(n: Int): Boolean = {
    (n * n) % math.pow(10, n.toString.length).toInt == n
  }

  override def doSomething() = {
    val result = for (i <- 1 to 10000 if selfGuard(i)) yield i
    println(result mkString ", ")
  }
}

object ScalaTest07 extends App {
  new ScalaTest07 with Timer doSomething()
}