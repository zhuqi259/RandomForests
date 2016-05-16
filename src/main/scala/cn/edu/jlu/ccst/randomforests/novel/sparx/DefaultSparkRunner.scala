package cn.edu.jlu.ccst.randomforests.novel.sparx

import org.apache.spark.{SparkConf, SparkContext}

/**
  * Convenience object that can run any given SparkRunner
  */
case class DefaultSparkRunner(runnerName: String, args: Array[String]) {

  implicit val appConf = Configuration(args)

  val defSparkConf = new SparkConf(true)
  val sparkConf = defSparkConf.setAppName(runnerName)
    .setMaster(defSparkConf.get("spark.master", "spark://spark-server:7077"))
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.executor.memory", "5g")
    .set("spark.cores.max", "4")
    .set("spark.akka.frameSize", "100")

  implicit val sc = new SparkContext(sparkConf)

  def run(runnable: SparkRunnable): Unit = runnable.run

}
