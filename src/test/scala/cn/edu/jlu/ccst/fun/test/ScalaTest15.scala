package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         法国数学家梅森尼对这类形如2^^n-1的素数特别感兴趣，做过很多有意义的工作，后人把此类数命名为梅森尼数。
  *         已经证明了，如果2n-1是素数，则幂指数n必须是素数，然而，反过来并不对，当n是素数时，2^^n-1不一定是素数。
  *         例如，人们已经找出2^^11-1是一个合数，23可以除尽它，2^^23-1是一个合数，47可以除尽它。
  *
  *         编程找出指数n在（2,50）中的梅森尼数。
  */
class ScalaTest15 extends TAction {

  // 2^^50 - 1 超过Int.MaxValue
  def isPrime(n: Long): Boolean = ((2: Long) to math.sqrt(n).toLong).forall {
    case i if n % i == 0 => false
    case _ => true
  }

  def masonNumber(n: Int) = {
    for {
      i <- 2 to n
      if isPrime(i)
      j = math.pow(2, i).toLong - 1
      if isPrime(j)
    } yield (i, j)
  }

  override def doSomething() = {
    val result = masonNumber(50)
    println(result mkString ", ")
    println(result.length)
  }
}

object ScalaTest15 extends App {
  new ScalaTest15 with Timer doSomething()
}
