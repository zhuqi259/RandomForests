package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         所谓回文素数是指，对一个整数n从左向右和从右向左读结果值相同且是素数，即称为回文素数。
  *
  *         求不超过1000的回文素数。
  */
class ScalaTest13 extends TAction {

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
      if primes.contains(i) && i == i.toString.map(_ - '0').reverse.reduceLeft(10 * _ + _)
    } yield i
    println(result mkString ", ")
    println(result.length)
  }
}

object ScalaTest13 extends App {
  new ScalaTest13 with Timer doSomething()
}
