package cn.edu.jlu.ccst.randomforests

import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.spark.rdd.RDD

package object novel {
  def removeHdfsFile(path: String) = {
    val hdfs = FileSystem.get(new Configuration())
    val workingPath = new Path(path)
    hdfs.delete(workingPath, true) // delete recursively
  }

  def removeLocalFile(path: String) = {
    FileUtils.deleteQuietly(new java.io.File(path))
  }

  /**
    * Run a block and return the block result and the runtime in millis
    *
    * @param block
    * @return
    */
  def timeCode[T](block: => T): (T, Long) = {
    val start = System.currentTimeMillis()
    val result = block
    val runtime = System.currentTimeMillis() - start
    (result, runtime)
  }

  def coolMyCPU(seconds: Int) = {
    println(s"Sleeping for $seconds seconds... (cooling of my CPU)")
    Thread.sleep(seconds * 1000)
  }

  /**
    * Count distinct values in an RDD of Arrays of T
    *
    * @param input
    * @tparam T
    * @return
    */
  def countDistinctValsByCol_1[T](input: RDD[Array[T]]) = {

    //TODO SOOO SLOWWWWW
    val zero: Seq[List[T]] = (1 to input.first.size).map(_ => List[T]())

    def countByKey[T](in: List[T]): List[(T, Int)] =
      in.map((_, 1)).groupBy(_._1).
        map { case (s, c) => (s, c.size) }.toList

    input.
      aggregate(zero)(
        { (acc, row) => acc.zip(row).
          map { case (all, x) => x +: all }
        }, {
          (c1, c2) =>
            c1.zip(c2).map(x => x._1 ++ x._2)
        }
      ).map {
      countByKey
    }
  }

  /**
    *
    * @param input
    * @tparam T
    * @return
    */
  def countDistinctValsByCol_2[T](input: RDD[Array[T]]) = {

    //TODO SOOO SLOWWWWW
    val zero: Seq[List[(T, Int)]] = (1 to input.first.size).map(_ => List[(T, Int)]())

    def countByKey[T](in: List[(T, Int)]): List[(T, Int)] =
      in.groupBy(_._1).
        map { case (s, c) => (s, c.map(_._2).sum) }.toList

    input.
      aggregate(zero)(
        { (acc, row) => row.zip(acc).
          map { case (x, all) => (x, 1) :: all }
        }, {
          (c1, c2) =>
            c1.zip(c2).map(x => x._1 ++ x._2).
              map(countByKey(_))
        }
      )
  }
}
