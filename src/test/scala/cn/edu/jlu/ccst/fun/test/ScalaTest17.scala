package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         类似7, 37, 67, 97, 127, 157，这样由素数组成的数列叫做等差素数数列。素数数列具有项数的限制，
  *         一般指素数数列的项数有多少个连续项，最多可以存在多少个连续项。
  *
  *         编程找出100以内的等差素数数列(项数 > 2)。
  */
class ScalaTest17 extends TAction {

  def prime(n: Int): (Vector[Int], Array[Boolean]) = {
    val flag = Array.fill(n + 1)(false)
    var primes = Vector.empty[Int]
    for (i <- 2 to n if !flag(i)) {
      primes :+= i
      for (j <- i to n / i) {
        flag(i * j) = true
      }
    }
    (primes, flag)
  }

  override def doSomething() = {
    val n = 100
    val (primes, flag) = prime(n)
    val len = primes.length
    val flag2 = Array.fill(n + 1, n + 1)(false)

    def calc(a0: Int, a1: Int): Vector[Int] = {
      val diff = a1 - a0
      def loop(i: Int, acc: Vector[Int]): Vector[Int] = {
        if (i > n || flag(i) || flag2(i)(diff)) acc
        else loop(i + diff, acc :+ i)
      }
      val ret = loop(a1 + diff, Vector(a0, a1))
      //      var an = a1 + diff
      //      var ret = Vector(a0, a1)
      //      while (an <= n && !flag(an) && !flag2(an)(diff)) {
      //        ret :+= an
      //        an += diff
      //      }
      for (i <- ret) {
        flag2(i)(diff) = true
      }
      ret
    }
    val result = {
      for {
        i <- 0 until len
        j <- i + 1 until len
        tmp = calc(primes(i), primes(j)) if tmp.length > 2
      } yield tmp
    }
    println(result mkString "\n")
    println(result.length)
  }
}

object ScalaTest17 extends App {
  new ScalaTest17 with Timer doSomething()
}
