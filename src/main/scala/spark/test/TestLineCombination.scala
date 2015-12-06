package spark.test

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport

/**
  * @author zhuqi259
  *
  *         文件每n行合并
  */
object TestLineCombination extends SparkContextSupport {

  def test(): Unit = {
    val s = List("123", "234", "345", "456", "567", "678")
    val n = 3
    // spark RDD 没有 foldLeft
    val x = s.zipWithIndex.foldLeft(List.empty[List[String]])((result, lineWithIndex) => {
      lineWithIndex match {
        case (line, index) =>
          if (index % n == 0) List(line) :: result else (line :: result.head) :: result.tail
      }
    }).reverse.map(_.reverse)
    println(x)
  }

  def test2() = {
    val s = List("123", "234", "345", "456", "567", "678")
    val length = s.length
    val n = 2
    val numSlices = length / n
    val x = sc.parallelize(s, numSlices).zipWithIndex().aggregate(List.empty[List[String]])(seqOp = (result, lineWithIndex) => {
      lineWithIndex match {
        case (line, index) =>
          if (index % n == 0) List(line) :: result else (line :: result.head) :: result.tail
      }
    }, combOp = (x, y) => x ::: y)
    x.map(_.reverse).foreach(println)
  }

  def main(args: Array[String]) = {
    // test()
    // test2()
    assert(args.length > 1)
    val _from = args(0)
    val _to = args(1)

    val s = sc.textFile(_from).collect()
    val n = if (args.length > 2) args(2).toInt else 2
    val numSlices = s.length / n
    val x = sc.parallelize(s, numSlices).zipWithIndex().aggregate(List.empty[List[String]])(seqOp = (result, lineWithIndex) => {
      lineWithIndex match {
        case (line, index) =>
          if (index % n == 0) List(line) :: result else (line :: result.head) :: result.tail
      }
    }, combOp = (x, y) => x ::: y).map(_.reverse mkString " ")
    sc.parallelize(x).saveAsTextFile(_to)
  }
}
