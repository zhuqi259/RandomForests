package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         若两个素数之差为2，则这两个素数就是孪生素数
  *
  *         编写程序找出1~100之间的所有孪生素数。
  */
class ScalaTest10 extends TAction {

  //筛法找素数
  def prime1(n: Int): Vector[Int] = {
    val flag = Array.fill(n + 1)(false)
    var primes = Vector.empty[Int]
    for (i <- 2 to n if !flag(i)) {
      primes = primes :+ i
      for (j <- i to n / i) {
        flag(i * j) = true
      }
    }
    primes
  }

  // prime1 tailrec版本
  def prime2(n: Int): Vector[Int] = {
    val flag = Array.fill(n + 1)(false)
    def loop(i: Int, primes: Vector[Int]): Vector[Int] = {
      if (i > n) primes
      else if (flag(i)) loop(i + 1, primes)
      else {
        for (j <- i to n / i) {
          flag(i * j) = true
        }
        loop(i + 1, primes :+ i)
      }
    }
    loop(2, Vector.empty[Int])
  }

  override def doSomething() = {
    val primes = prime2(100)
    val result = for (i <- 1 until primes.length if primes(i) - primes(i - 1) == 2) yield (primes(i - 1), primes(i))
    println(result mkString ", ")
    println(result.length)
  }
}

object ScalaTest10 extends App {
  new ScalaTest10 with Timer doSomething()
}

