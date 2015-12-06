package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         某古寺的一块石碑上依稀刻有一些神秘的自然数。
  *         专家研究发现：这些数是由1,3,5,7,9这5个奇数字排列组成的5位素数，
  *         同时去掉它的最高位与最低位数字后的3位数还是素数，同时去掉它的高二位与低二位数字后的一位数还是素数。
  *         因此人们把这些神秘的素数称为金蝉素数，喻意金蝉脱壳之后仍为美丽的金蝉。
  *
  *         试求出石碑上的金蝉素数。
  */
class ScalaTest11 extends TAction {
  def isPrime(n: Int): Boolean = (2 to n / 2).forall {
    case i if i * i <= n && n % i == 0 => false
    case _ => true
  }

  override def doSomething() = {
    val result = {
      for (p <- Vector(1, 3, 5, 7, 9).permutations
           if Vector(p.slice(1, 4), p.zipWithIndex.filter(_._2 % 2 == 0).map(_._1), p).forall {
             case i if isPrime(i.reduceLeft(10 * _ + _)) => true
             case _ => false
           }) yield p.reduceLeft(10 * _ + _)
    }.toArray
    println(result mkString ", ")
    println(result.length)
  }
}

object ScalaTest11 extends App {
  new ScalaTest11 with Timer doSomething()
}

