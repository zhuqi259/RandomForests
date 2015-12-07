package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         编写程序找出10~900之间的所有可逆素数（可逆素数是指一个素数的各位数值顺序颠倒后得到的数仍为素数，如113、311）。
  *         (i, j) => i,j 不等
  */
class ScalaTest12 extends TAction {

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
    val primes = prime(900)
    val result = for {
      i <- 10 to 900
      if primes.contains(i)
      j = i.toString.map(_ - '0').reverse.reduceLeft(10 * _ + _)
      if i < j && primes.contains(j)
    } yield (i, j)
    println(result mkString ", ")
    println(result.length)
  }
}

object ScalaTest12 extends App {
  new ScalaTest12 with Timer doSomething()
}