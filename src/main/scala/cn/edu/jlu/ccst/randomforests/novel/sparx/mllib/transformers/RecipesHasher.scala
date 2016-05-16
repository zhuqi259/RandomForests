package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.transformers

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.FlowData
import cn.edu.jlu.ccst.randomforests.novel.sparx.{Configuration, DefaultSparkRunner, RecipesImporter, SparkRunnable}
import cn.edu.jlu.ccst.randomforests.novel.sparx.model._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

/**
  * Read data from a json file, parse it, transform it into a bag of recipes and produce a FlowData object
  */
object RecipesHasher extends SparkRunnable with Transformer[RDD[Recipe]] {


  def main(args: Array[String]) = {

    DefaultSparkRunner(this.getClass.getName, args).run(this)

  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {
    // import the recipes
    val recipes = RecipesImporter.importFrom(configuration.inputTrainingData)

    // Create the flowData object
    val flowData = RecipesHasher.transform(recipes)

    // TODO... now what?
  }

  def transform(recipes: RDD[Recipe]): FlowData = {

    // Normally we should keep this an RDD, but we have a small list here
    val cuisines = recipes.map(r => r.cuisine)
      .map((_, 1)).reduceByKey(_ + _).sortBy(_._2, false) // sort the cuisines by the number of recipes descending
      .collect
    val cuisinesList = cuisines.map(_._1).toSeq

    // Normally we should keep this an RDD, but we have a small list here
    val ingredients = recipes.flatMap(r => r.ingredients)
      .map((_, 1)).reduceByKey(_ + _)
      //  .filter(_._2 != 1) // Should we filter out the "irrelevant features"?
      .sortBy(_._1).sortBy(_._2, false) // sort the ingredients by occurrences descending
      .collect
    val ingredientsList = ingredients.map(_._1).toSeq

    val cuisineToIndex = cuisinesList
      .zipWithIndex.toMap

    val hashingTF = new HashingTF()

    val terms = recipes.map(r => r.ingredients)
    val features = hashingTF.transform(terms).cache

    val labels = recipes.map(r => cuisineToIndex(r.cuisine))

    val data = labels.zip(features).map(x => LabeledPoint(x._1, x._2))

    val ingredientToIndex = ingredientsList
      .map(in => (in, hashingTF.indexOf(in))).toMap


    println("------------")
    ingredientToIndex.toSeq.sortBy(_._2).splitAt(20)._1.foreach(println)
    ingredientToIndex.toSeq.sortBy(_._2).reverse.splitAt(20)._1.foreach(println)
    println("------------")



    val fd = FlowData(data, cuisineToIndex, ingredientToIndex)

    recipes.take(10).zip(data.take(10)).foreach { case (r, lp) =>
      println(r)
      println(lp)
      println(f"${lp.label.toInt}%3d | ${fd.indexToLabel(lp.label.toInt)}")
      val features = lp.features.toArray
      (0 until features.size).foreach { i =>
        if (features(i) > 0) {
          println("  - " + fd.indexToFeature(i))
        }
      }
    }

    val idf = new IDF(minDocFreq = 2).fit(features)
    val tfidf: RDD[Vector] = idf.transform(features)

    val ndata = labels.zip(tfidf).map(x => LabeledPoint(x._1, x._2))
    println("------------")
    ndata.take(10).foreach(println)
    println("------------")

    FlowData(data, cuisineToIndex, ingredientToIndex)


  }

}

