package cn.edu.jlu.ccst.randomforests.novel.sparx

import org.apache.spark.{SparkConf, SparkContext}

/**
  *
  */
object Main {

  def main(args: Array[String]) = {

    implicit val configuration = Configuration(args)

    val defConf = new SparkConf(true)
    val conf = defConf.setAppName("Spark Random Forests")
      .setMaster(defConf.get("spark.master", "spark://spark-server:7077"))
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.executor.memory", "5g")
      .set("spark.cores.max", "4")
      .set("spark.akka.frameSize", "100")

    implicit val sc = new SparkContext(conf)

    run
  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {

    val runners = List(
      ImportData,
      BuildModels,
      BuildPredictions,
      ExportToCSV)

    runners.foreach(_.run)
  }

}
