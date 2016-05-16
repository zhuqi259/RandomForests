package cn.edu.jlu.ccst.randomforests.novel.sparx

import java.io.{BufferedOutputStream, PrintWriter}

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.Model
import cn.edu.jlu.ccst.randomforests.novel.sparx.model.PredictedRecipe
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.tree.model.RandomForestModel

import scala.util.Try

object ExportToCSV extends SparkRunnable {

  def main(args: Array[String]) = {
    DefaultSparkRunner(this.getClass.getName, args).run(this)
  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {

    val models: List[Model[_]] = List(RandomForestModel.load(sc, configuration.randomForestPath))

    // Load the predictions
    val predictions = sc.objectFile[PredictedRecipe](configuration.outputPredictionsPath)

    // Create the HDFS file system handle
    val hdfs = FileSystem.get(new org.apache.hadoop.conf.Configuration())

    // Create an output stream for each model type
    val dos = models.map { m =>
      val path = new Path(s"${configuration.outputPredictionsPath}_${m.name}.csv")
      (m.name, new PrintWriter(new BufferedOutputStream(hdfs.create(path, true)), true))
    }.toMap

    predictions.collect.foreach { predict =>
      predict.predictions.foreach { p =>
        val os = dos(p.model)
        val record = s"${predict.id}, ${p.prediction}\n"
        Try(os.print(record))
      }
    }
    // Close the streams
    dos.values.foreach(os => Try(os.close()))
  }
}
