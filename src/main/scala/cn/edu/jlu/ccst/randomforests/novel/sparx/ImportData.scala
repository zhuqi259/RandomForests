package cn.edu.jlu.ccst.randomforests.novel.sparx

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.transformers.{ChiSqSelectorTransformer, TrainingDataImporter}
import org.apache.spark.SparkContext

/**
 * Import training data from files and save the LabeledVectors
 * along with the label and feature mappings.
 */
object ImportData extends SparkRunnable {

  def main(args: Array[String]) = {

    DefaultSparkRunner(this.getClass.getName, args).run(this)

  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {


    // import the recipes
    val recipes = RecipesImporter.importFrom(configuration.inputTrainingData)

    // Transform the recipes into training data
    val flowData = TrainingDataImporter
      .transform(recipes)
      .map(ChiSqSelectorTransformer(0.8).transform)

    // Store the flow data for later processing
    flowData.save(configuration.dataPath)

  }

}
