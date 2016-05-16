package cn.edu.jlu.ccst.randomforests.novel.sparx

import cn.edu.jlu.ccst.randomforests.novel.sparx.model.Recipe
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.json4s.jackson.JsonMethods._

/**
  * Import the recipes from a given file into Spark
  */
object RecipesImporter extends SparkRunnable {

  /**
    * Import recipes and save them as a Spark RDD
    *
    * @param args
    */
  def main(args: Array[String]) = {

    DefaultSparkRunner(this.getClass.getName, args).run(this)

  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {

    val recipes = importFrom(configuration.inputTrainingData)

    recipes.saveAsObjectFile(configuration.recipesPath)

  }

  def importFrom(path: String)(implicit sc: SparkContext): RDD[Recipe] = {

    val rawData: RDD[(LongWritable, Text)] =
      sc.newAPIHadoopFile[LongWritable, Text, CustomLineInputFormat](path)

    implicit lazy val formats = org.json4s.DefaultFormats

    rawData.map(x => parse(x._2.toString)).map(
      json => {
        val id = (json \ "id").extract[Int]
        val cuisine = (json \ "cuisine").extractOrElse[String]("unknown").toLowerCase
        val ingredients = (json \ "ingredients").extractOrElse[List[String]](List()).map(_.toLowerCase)
        Recipe(id, cuisine, ingredients)
      }
    )
  }

}
