package cn.edu.jlu.ccst.randomforests.util

import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by zhuqi259 on 2015/12/1.
  */
trait SparkContextSupport {
  val conf = new SparkConf()
    .setAppName("RandomForests")
    .setMaster("spark://spark-server:7077")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.executor.memory", "5g")
    .set("spark.cores.max", "4")
    .set("spark.akka.frameSize", "100")
  val sc = new SparkContext(conf)
}
