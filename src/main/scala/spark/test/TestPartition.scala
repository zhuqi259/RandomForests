package spark.test

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport

/**
  * @author zhuqi259
  *
  *         测试RDD分区
  */
object TestPartition extends SparkContextSupport {
  def main(args: Array[String]) = {
    assert(args.length > 0)
    val _from = args(0)
    val x = sc.textFile(_from)
    println(x.collect() mkString ", ")
    // repartition后数据乱了
    println(x.repartition(13).collect() mkString ", ")
  }
}
