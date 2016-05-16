package cn.edu.jlu.ccst.randomforests.novel.sparx

import cn.edu.jlu.ccst.randomforests.novel.removeHdfsFile
import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.{FlowData, Model}
import cn.edu.jlu.ccst.randomforests.novel.sparx.model.{PredictedRecipe, PredictionData}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.tree.model.RandomForestModel

/**
  * An example of following through the process from data import to predictions.
  *
  * The example is mainly designed for multi class classification with discrete features
  *
  */
object BuildPredictions extends SparkRunnable {

  def main(args: Array[String]) = {

    DefaultSparkRunner(this.getClass.getName, args).run(this)

  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {

    import DaoUtils._

    // Load the flow data
    val flowData = FlowData.load(configuration.dataPath)

    // Import the test recipes for predictions
    val testRecipes = RecipesImporter.importFrom(configuration.inputTestingData)

    // Transform the test recipes into feature vectors
    val testData = testRecipes.map { r =>
      // Don't have it, don't use it
      val filteredIngredients = r.ingredients.filter(flowData.featureToIndex.contains(_))
      val values = filteredIngredients.map(i => 1.0).toArray
      val indices = filteredIngredients.map(flowData.featureToIndex).sorted.toArray
      val vector = Vectors.sparse(flowData.featureToIndex.size, indices, values)
      (r.id, vector)
    }.cache

    // Set the models are we using for predictions
    val models: List[Model[_]] = List(RandomForestModel.load(sc, configuration.randomForestPath))


    // Load the metrics so we can produce nice prediction data beans
    // TODO: loadMetrix() returns option, maybe this should be fixed;
    // TODO: probably PredictionData should take an option for metrics
    val metrics = models.map(model => model.name -> loadMetrix(model).get).toMap

    // Prepare the data to be predicted
    val predictionData = testData.map { case (recipeId, featuresVector) =>
      val predictions = models.map { model =>
        val prediction = model.predict(featuresVector)
        model.name -> prediction
      }.toMap
      (recipeId, predictions, featuresVector)
    }

    // Predict the recipes
    val predictedRecipes = predictionData.map {
      case (recipeId, predictions, featuresVector) =>
        val ingredientsIndices = featuresVector.toSparse.indices
        val ingredients = ingredientsIndices.map(flowData.indexToFeature(_)).toSeq
        val predictionData = predictions.map { p =>
          val modelName = p._1
          val predictedCuisine = flowData.indexToLabel(p._2.toInt)
          val classMetrics = metrics(modelName).metricsByLabel(predictedCuisine)
          PredictionData(modelName, predictedCuisine, classMetrics)
        }.toSeq //HR as in Human Readable

        PredictedRecipe(recipeId, ingredients, predictionData)
    }

    // Print some results
    predictedRecipes.take(20).foreach(printPrediction)
    printPredictionLegend

    // This is one way to do it, probably not the best one
    // TODO: Find a better way of persisting/exporting the results
    removeHdfsFile(configuration.outputPredictionsPath)
    predictedRecipes.saveAsObjectFile(configuration.outputPredictionsPath)
  }


  def printPrediction(p: PredictedRecipe): Unit = {

    println(s"Recipe Id: ${p.id}")
    println("  Ingredients:")
    p.ingredients.foreach(i => println(s"  - ${i}"))
    println("  Predictions and class specific metrics:")
    println(f"  | ${"Model Name"}%-30s | ${"Prediction"}%-30s | ${"Prec/cls"}%-8s | ${"TPR/cls"}%-8s | ${"FPR/cls"}%-8s |")
    println("  | -------------------- | -------------------- | -------- | -------- | --------")
    p.predictions.foreach(p =>
      println(f"  | ${p.model}%-30s | ${p.prediction}%-30s | ${p.metrics.precision * 100}%7.4f%% | ${p.metrics.truePositiveRate * 100}%7.4f%% | ${p.metrics.falsePositiveRate * 100}%7.4f%% |")
    )
  }

  def printPredictionLegend(): Unit = {
    val legend = "" ::
      ("| Legend ||") ::
      ("| ------ | ----------------------- |") ::
      ("| TPR    | True Positive Rate      |") ::
      ("| FPR    | False Positive Rate     |") ::
      ("| Prec   | Precision (TP / LC)     |") ::
      ("| cls    | Class (label)           |") ::
      Nil
    println(legend.mkString("\n"))
  }

}
