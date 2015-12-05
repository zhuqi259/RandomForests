package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         水仙花数是指一个n位数（n≥3），它的每个位上的数字的n次幂之和等于它本身。
  *         例如：1^^3+5^^3+3^^3=153。
  *
  *         求100~9999之间所有的水仙花数。
  */
class ScalaTest02 extends TAction {
  def isArmstrongNumber(n: Int): Boolean = {
    def splitNumber(t: Int, list: List[Int]): List[Int] = {
      if (t == 0) list
      else splitNumber(t / 10, t % 10 :: list)
    }
    val list = splitNumber(n, Nil)
    val k = list.length
    list.map(x => math.pow(x, k).toInt).sum == n
  }

  override def doSomething() = {
    val result = for (i <- 100 to 9999 if isArmstrongNumber(i)) yield i
    println(result mkString ", ")
  }
}

object ScalaTest02 extends App {
  new ScalaTest02 with Timer doSomething()
}
