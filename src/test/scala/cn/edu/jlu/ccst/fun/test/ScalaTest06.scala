package cn.edu.jlu.ccst.fun.test

/**
  * @author zhuqi259
  *         所谓勾股数，一般是指能够构成直角三角形3条边的3个正整数（a,b,c)。
  *         即a²+b²=c²，a，b，cΣN
  *
  *         求10000以内的勾股数。
  */
class fun1(n: Int) extends TAction {
  override def doSomething() = {
    val result = for {
      a <- 1 to n
      b <- a + 1 to n
      d = a * a + b * b
      c = math.sqrt(d); f = c.toInt if !(c > n) && f * f == d // c>n 后其实可以跳出里层循环了
    } yield (a, b, c.toInt)
    println(result mkString ", ")
    println(result.length)
  }
}

class fun2(n: Int) extends TAction {
  override def doSomething() = {
    val result = for {
      a <- 1 to n
      b <- a + 1 to n
      c = math.sqrt(a * a + b * b) if !(c > n) && c.isValidInt // c>n 后其实可以跳出里层循环了
    } yield (a, b, c.toInt)
    println(result mkString ", ")
    println(result.length)
  }
}

class fun3(n: Int) extends TAction {
  override def doSomething() = {
    val result = {
      for {
        a <- 1 to n
        b <- a + 1 to n
        c = math.sqrt(a * a + b * b) if c.isValidInt
      } yield (a, b, c.toInt)
    }.toStream
    val xx = result.filter(_._3 <= n) // Stream + filter效率几乎没有提高
    println(xx mkString ", ")
    println(xx.length)
  }
}

class fun4(n: Int) extends TAction {
  override def doSomething() = {
    val xxx = {
      for (a <- 1 to n) yield {
        val s = {
          for {
            b <- a + 1 to n
            c = math.sqrt(a * a + b * b) if c.isValidInt
          } yield (a, b, c.toInt)
        }.toStream
        s.takeWhile(_._3 <= n) //应该是数据量太少 takeWhile break优势不明显
      }
    }
    val yyy = xxx.flatten // 或者是flatten多了一处时间消耗
    println(yyy mkString ", ")
    println(yyy.length)
  }
}

object ScalaTest06 extends App {
  new fun1(10000) with Timer doSomething()
  new fun2(10000) with Timer doSomething()
  new fun3(10000) with Timer doSomething()
  new fun4(10000) with Timer doSomething()
}
