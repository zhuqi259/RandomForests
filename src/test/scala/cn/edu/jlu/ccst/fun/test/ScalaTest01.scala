package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         0~9这10个数字可以组成多少不重复的3位数？
  */
object ScalaTest01 extends App {
  val result = {
    for (i <- 1 to 9) yield {
      for (j <- 0 to 9 if j != i) yield {
        for (k <- 0 to 9 if k != j && k != i) yield {
          i * 100 + 10 * j + k
        }
      }
    }
  }.flatten.flatten
  println(result)
  println(result.length)
}
