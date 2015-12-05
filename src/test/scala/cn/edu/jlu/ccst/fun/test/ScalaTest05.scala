package cn.edu.jlu.ccst.fun.test

import scala.concurrent.duration._

/**
  * @author zhuqi259
  *         黑洞数又称陷阱数，是类具有奇特转换特性的整数。
  *         任何一个数字不全相同的整数，经有限“重排求差”操作，总会得到某一个或一些数，这些数即为黑洞数。
  *         “重排求差”操作即把组成该数的数字重排后得到的最大数减去重排后得到的最小数。
  *
  *         举个例子，3位数的黑洞数为495.
  *         简易推导过程：随便找个数，如297,3个位上的数从小到大和从大到小各排一次，为972和279，
  *         相减得693。按上面做法再做一次，得到594，再做一次，得到495，之后反复都得到495。
  *
  *         验证4位数的黑洞数为6174。
  */

class BlackHole(startValue: Int) {
  type State = Int
  val initialState = startValue

  def next(state: State): State = {
    val a = state.toString.map(_ - '0')
    val s = a.sorted
    val s1 = s.reduceLeft(10 * _ + _)
    val s2 = s.reverse.reduceLeft(10 * _ + _)
    s2 - s1
  }

  class Path(val history: List[State], val endState: State) {
    def extend = new Path(endState :: history, next(endState))

    override def toString = history.reverse.mkString(" ") + " " + endState
  }

  val initialPath = new Path(Nil, initialState)

  def from(paths: Set[Path], explored: Set[State]): Stream[Set[Path]] = {
    if (paths.isEmpty) Stream.empty
    else {
      val more = for {
        path <- paths
        next = path.extend
        if !(explored contains next.endState)
      } yield next
      paths #:: from(more, explored ++ more.map(_.endState))
    }
  }

  val pathSets = from(Set(initialPath), Set(initialState))

  def solution: Stream[Path] = {
    for {
      pathSet <- pathSets
      path <- pathSet
    } yield path
  }

  def result: List[State] = {
    val path = solution.last
    val repeated = next(path.endState)
    val history = path.history.reverse
    if (history.contains(repeated))
      history.drop(history.indexOf(repeated)) :+ path.endState // :+ List尾部添加元素
    else
      List(path.endState)
  }
}


object ScalaTest05 extends App {

  def single(n: Int): Boolean = {
    n.toString.toSet.size == 1
  }

  def fun(n: Int): Int = {
    val a = n.toString.map(_ - '0')
    val s = a.sorted
    val s1 = s.reduceLeft(10 * _ + _)
    val s2 = s.reverse.reduceLeft(10 * _ + _)
    // println(s1, s2, s2 - s1)
    if (s2 - s1 == n) n
    else fun(s2 - s1)
  }

  def calculate(n: Int):(List[List[Int]], Int) = {
    // n位数
    val start: Long = System.currentTimeMillis
    val min = math.pow(10, n - 1).toInt
    val max = math.pow(10, n).toInt - 1
    val result = {
      for (i <- min to max if !single(i)) yield {
        new BlackHole(i).result
      }
    }.distinct.toList
    // println(result, result.size)
    // key去重
    val xx = result.zip(result).map(x => (x._1.sorted, x._2)).groupBy(_._1)
      .map { case (group, traversable) => traversable.reduce { (a, b) => (a._1, a._2) } }.values.toList
    val duration = (System.currentTimeMillis - start).millis
    println("duration = %s".format(duration))
    (xx, xx.size)
  }

  val start: Long = System.currentTimeMillis
  for (i <- 1 to 6) {
    val (result, size) = calculate(i)
    println(result, size)
  }
  val duration = (System.currentTimeMillis - start).millis
  println("\n\nduration = %s".format(duration))
}
