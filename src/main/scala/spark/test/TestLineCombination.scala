package spark.test

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.Partitioner

/**
  * @author zhuqi259
  *
  *         文件每n行合并
  */
class ZQPartitioner(numParts: Int, n: Int) extends Partitioner {
  //覆盖分区数
  override def numPartitions: Int = numParts

  //覆盖分区号获取函数
  override def getPartition(key: Any): Int = {
    key.toString.toInt / n
  }
}

object TestLineCombination extends SparkContextSupport {

  def test1(): Unit = {
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

  def test3(args: Array[String]) = {
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

  def test4(args: Array[String]) = {
    assert(args.length > 1)
    val _from = args(0)
    val _to = args(1)
    val data = sc.textFile(_from)
    data.cache()
    val n = if (args.length > 2) args(2).toInt else 2
    val count = data.count()
    val num = count / n
    val numSlices = if (count % n == 0) num else num + 1
    val y = data.zipWithIndex().map(x => (x._2, x._1)).partitionBy(new ZQPartitioner(numSlices.toInt, n)).aggregate(List.empty[List[(Long, String)]])(seqOp = (result, indexedLine) => {
      if (indexedLine._1 % n == 0)
        List(indexedLine) :: result
      else
        (indexedLine :: result.head) :: result.tail
    }, combOp = (x, y) => x ::: y).map(x => x.sortBy(_._1).map(_._2) mkString " ")
    sc.parallelize(y).saveAsTextFile(_to)
  }


  def beautiful(args: Array[String]) = {
    assert(args.length > 1)
    val _from = args(0)
    val _to = args(1)
    val data = sc.textFile(_from)
    data.cache()
    val n = if (args.length > 2) args(2).toInt else 2
    val count = data.count()
    val num = count / n
    val numSlices = if (count % n == 0) num else num + 1
    data.zipWithIndex().map(x => (x._2, x._1)).partitionBy(new ZQPartitioner(numSlices.toInt, n)).mapPartitions {
      iter => {
        var res = List.empty[String]
        // res.::=(iter.toList.sortBy(_._1).map(_._2) mkString " ")
        // iter 就是 按照行顺序的，不需要排序了
        res.::=(iter.toList.map(_._2) mkString " ")
        res.iterator
      }
    }.saveAsTextFile(_to)
  }

  def main(args: Array[String]) = {
    // test1()
    // test2()
    // test3(args)
    // test4(args)
    beautiful(args)
  }
}
