package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         打印回型矩阵如图：
  *         1      2     3     4     5     6     7      8
  *         28     29    30    31    32    33    34     9
  *         27     48    49    50    51    52    35    10
  *         26     47    60    61    62    53    36    11
  *         25     46    59    64    63    54    37    12
  *         24     45    58    57    56    55    38    13
  *         23     44    43    42    41    40    39    14
  *         22     21    20    19    18    17    16    15
  */
class ScalaTest18 extends TAction {
  override def doSomething() = {
    val n = 8
    val result = Array.ofDim[Int](n, n)
    var p = 0
    var q = n - 1
    var t = 1
    while (p < q) {
      for (i <- p until q) {
        result(p)(i) = t
        t += 1
      }
      for (i <- p until q) {
        result(i)(q) = t
        t += 1
      }
      for (i <- q.until(p, -1)) {
        result(q)(i) = t
        t += 1
      }
      for (i <- q.until(p, -1)) {
        result(i)(p) = t
        t += 1
      }
      p += 1
      q -= 1
    }
    if (p == q) result(p)(p) = t

    println(result.map(_ mkString "\t") mkString "\n")
  }
}

object ScalaTest18 extends App {
  new ScalaTest18 with Timer doSomething()
}
