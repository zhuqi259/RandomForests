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

    val cuisines1 = Array("C000007", "C000008", "C000010", "C000013", "C000014",
      "C000016", "C000020", "C000022", "C000023", "C000024")
    val cuisines2 = Array("C000008", "C000010", "C000013", "C000014",
      "C000016", "C000020", "C000022", "C000023", "C000024")

    predictions.collect.foreach { predict =>
      predict.predictions.foreach { p =>
        val os = dos(p.model)
        val record =
          configuration.inputDataType match {
            case "cuisines" =>
              s"${predict.id}, ${p.prediction}\n"
            case "SogouC.mini" =>
              s"${predict.id}, ${p.prediction}, ${cuisines1(predict.id)}\n"
            case "SogouC.reduced" =>
              s"${predict.id}, ${p.prediction}, ${cuisines2(predict.id)}\n"
          }
        Try(os.print(record))
      }
    }
    // Close the streams
    dos.values.foreach(os => Try(os.close()))
  }
}
