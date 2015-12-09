package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         杨辉三角
  *         1
  *         1   1
  *         1	  2	  1
  *         1	  3 	3	  1
  *         1	  4	  6	  4	  1
  *         1	  5	  10  10  5   1
  *         1   6   15  20  15  6   1
  *
  **/
class ScalaTest20 extends TAction {

  def yang(n: Int) = {
    var a = Array.empty[Int]
    for (i <- 0 until n) {
      a :+= 1
      for (j <- (i - 1).until(0, -1)) {
        a(j) = a(j) + a(j - 1)
      }
      println(a mkString "\t")
    }
  }

  def yang2(n: Int) = {
    // 1=>11=> 111=>121 => 1121=>1321=>1331 => 11331=>14331=>14631=>14641=> 114641=>154641=>1 5 10 6 4 1=> 1 5 10 10 4 1=> 1 5 10 10 5 1
    var a = Array.empty[Int]
    for (i <- 0 until n) {
      a = 1 +: a
      for (j <- 1 until i) {
        a(j) = a(j) + a(j + 1)
      }
      println(a mkString "\t")
    }
  }

  override def doSomething() = {
    yang(7)
    yang2(7)
  }
}

object ScalaTest20 extends App {
  new ScalaTest20 with Timer doSomething()
}