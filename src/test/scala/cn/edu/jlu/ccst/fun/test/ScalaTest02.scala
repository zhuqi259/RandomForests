package cn.edu.jlu.ccst.fun.test

import scala.math.pow
import scala.concurrent.duration._

/**
  * @author zhuqi259
  *         水仙花数是指一个n位数（n≥3），它的每个位上的数字的n次幂之和等于它本身。
  *         例如：1^^3+5^^3+3^^3=153。
  *
  *         求100~9999之间所有的水仙花数。
  */
object ScalaTest02 extends App {
  def isArmstrongNumber(n: Int): Boolean = {
    def splitNumber(t: Int, list: List[Int]): List[Int] = {
      if (t == 0) list
      else splitNumber(t / 10, t % 10 :: list)
    }
    val list = splitNumber(n, Nil)
    val k = list.length
    list.map(x => pow(x, k).toInt).sum == n
  }

  val start: Long = System.currentTimeMillis
  val result = for (i <- 100 to 9999 if isArmstrongNumber(i)) yield i
  val duration = (System.currentTimeMillis - start).millis
  println(result mkString ", ")
  println("duration = %s".format(duration))
}
