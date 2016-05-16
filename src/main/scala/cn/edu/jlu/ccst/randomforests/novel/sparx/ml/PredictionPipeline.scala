package cn.edu.jlu.ccst.randomforests.novel.sparx.ml

import cn.edu.jlu.ccst.randomforests.novel.sparx.{Configuration, RecipesImporter}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

object PredictionPipeline {

  // TODO make it spark 1.4.x friendly
  def main(args: Array[String]) = {

    val configuration = Configuration(args)

    val defConf = new SparkConf(true)
    val conf = defConf.setAppName("CuisineRecipesImportData").
      setMaster(defConf.get("spark.master", "local[*]"))

    implicit val sc = new SparkContext(conf)

    val sqc = new SQLContext(sc)

    val recipes = RecipesImporter.importFrom(configuration.recipesPath)

    val data = sqc.createDataFrame(recipes)
    //
    //    // Index labels, adding metadata to the label column;
    //    // we also fit data to preserve the mapping.
    //    val labelIndexer = new StringIndexer()
    //      .setInputCol("cuisines")
    //      .setOutputCol("label")
    //      .fit(data)
    //
    //    // Hash the ingredients names (strings to doubles)
    //    val ingredientsHash = new HashingTF()
    //      .setInputCol("ingredients")
    //      .setOutputCol("features")
    //
    //    // Automatically identify categorical features, and index them.
    //    val featureIndexer =
    //      new VectorIndexer()
    //      .setInputCol("features")
    //      .setOutputCol("indexedFeatures")
    //      .setMaxCategories(2) // features with > ? distinct values are treated as continuous
    //
    //    // Train a model.
    //    val trainer = new NaiveBayes()
    //      .setLabelCol("label")
    //      .setFeaturesCol("features")
    //
    //
    //    // Map the prediction to a human readable value
    //    val labelConverter = new IndexToString()
    //      .setInputCol("prediction")
    //      .setOutputCol("predictedLabel")
    //      .setLabels(labelIndexer.labels)
    //
    //    // Build the pipeline
    //    val pipeline = new Pipeline()
    //      .setStages(Array(
    //        labelIndexer,
    //        ingredientsHash,
    ////        featureIndexer,
    //        trainer,
    //        labelConverter))
    //
    //    // Split the data into training and test sets (5% held out for testing)
    //    val Array(trainingData, testData) = data.randomSplit(Array(0.2, 0.8))
    //
    //    val model = pipeline.fit(trainingData)
    //
    //    println("----------------")
    //
    //    println(s"Model Params: \n${model.explainParams()}")
    //
    //    val predictions = model.transform(testData)
    //
    //
    //    println("---------------------------------")
    //    println("PREDICTIONS")
    //    predictions.printSchema()
    //
    //    predictions.take(40).foreach(println)
    //
    //    val collectedPredictions = predictions.select("cuisines", "predictedLabel", "prediction", "label")
    //      .collect
    //    println(f"${"Actual"}%30s  |  ${"Predicted"}")
    //    println(f"${"---------------------------"}%30s  |  ${"---------------------------"}")
    //    collectedPredictions
    //      .take(20)
    //      .foreach(r => println(f"${r.getAs[String]("cuisines")}%30s  |  ${r.getAs[String]("predictedLabel")}"))
    //
    //
    ////    val accuracy = collectedPredictions
    ////      .filter(r => r.getAs[Double]("prediction") == r.getAs[Double]("label")).size.toDouble / predictions.count()
    //
    //    // Select (prediction, true label) and compute test error
    //    val evaluator = new MulticlassClassificationEvaluator()
    //      .setLabelCol("label")
    //      .setPredictionCol("prediction")
    //      .setMetricName("precision")
    //    val accuracy = evaluator.evaluate(predictions)
    //
    //    println("---------------------------------")
    //    println(f"Accuracy = ${accuracy * 100}%.4f%%")
    //    println(f"Error    = ${(1 - accuracy) * 100}%.4f%%")

  }
}
