package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         素数的平方是回文，比如11*11=121。
  *
  *         求不超过1000的平方回文素数。
  */
class ScalaTest14 extends TAction {

  def prime(n: Int): Set[Int] = {
    val flag = Array.fill(n + 1)(false)
    val primes = scala.collection.mutable.Set.empty[Int]
    for (i <- 2 to n if !flag(i)) {
      primes += i
      for (j <- i to n / i) {
        flag(i * j) = true
      }
    }
    primes.toSet
  }

  override def doSomething() = {
    val primes = prime(1000)
    val result = for {
      i <- 2 to 1000
      if primes.contains(i)
      j = i * i if j == j.toString.map(_ - '0').reverse.reduceLeft(10 * _ + _)
    } yield i
    println(result mkString ", ")
    println(result.length)
  }
}

object ScalaTest14 extends App {
  new ScalaTest14 with Timer doSomething()
}

