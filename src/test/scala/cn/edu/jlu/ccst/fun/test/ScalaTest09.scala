package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         素数（质数）指的是不能被分解的数，除了1和它本身之外就没有其他数能够整除。（1不是素数）
  *
  *         求100以内的所有素数。
  */
class ScalaTest09 extends TAction {

  def isPrimeNumber(n: Int, primes: Vector[Int]): Boolean = {
    val s: Stream[Int] = {
      for (k <- primes if k * k <= n) yield k
    }.toStream
    val rest = s.takeWhile(n % _ != 0)
    rest.length == s.length
  }

  def prime(n: Int): Vector[Int] = {
    def loop(i: Int, primes: Vector[Int]): Vector[Int] = {
      if (i > n) primes
      else loop(i + 1, if (isPrimeNumber(i, primes)) primes :+ i else primes)
    }
    loop(2, Vector.empty[Int])
  }


  override def doSomething() = {
    val primes = prime(100)
    println(primes mkString ", ")
    println(primes.length)
  }
}

object ScalaTest09 extends App {
  new ScalaTest09 with Timer doSomething()
}
