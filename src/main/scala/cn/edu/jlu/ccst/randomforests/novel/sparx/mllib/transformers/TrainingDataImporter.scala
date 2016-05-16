package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.transformers

import cn.edu.jlu.ccst.randomforests.novel.sparx.{Configuration, DefaultSparkRunner, RecipesImporter, SparkRunnable}
import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.FlowData
import cn.edu.jlu.ccst.randomforests.novel.sparx.model.Recipe
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

/**
  * Read data from a json file, parse it, transform it into a bag of recipes and produce a FlowData object
  */
object TrainingDataImporter extends SparkRunnable with Transformer[RDD[Recipe]] {

  def main(args: Array[String]) = {
    DefaultSparkRunner(this.getClass.getName, args).run(this)
  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {
    // import the recipes
    val recipes = RecipesImporter.importFrom(configuration.inputTrainingData)

    // Create the flowData object
    val flowData = TrainingDataImporter.transform(recipes)

    flowData.save(configuration.dataPath)

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

    val ingredientToIndex = ingredientsList
      .zipWithIndex.toMap

    val data = recipes.map { r =>
      val label = cuisineToIndex(r.cuisine)
      val filteredIngredients = r.ingredients.filter(ingredientsList.contains(_))
      val values = filteredIngredients.map(i => 1.0).toArray
      val indices = filteredIngredients.map(ingredientToIndex(_)).sorted.toArray
      val vector = Vectors.sparse(ingredientToIndex.size, indices, values)
      LabeledPoint(label, vector)
    }.cache

    // Print some info
    // TODO: Log some info sounds even better
    val allIngredientsByCuisine = recipes.map(r => (r.cuisine, r.ingredients)).reduceByKey(_ ++ _)
      .map(x => (x._1, x._2.distinct.size))
      .collect.sortWith(_._2 > _._2)
    val mostIngredientsByCuisine = recipes.map(r => (r.cuisine, r.ingredients.size)).reduceByKey(_ max _)
      .collect.sortWith(_._2 > _._2)

    println("\n### Total recipes per cuisines")
    println(f"| ${"Cuisine"}%-40s | ${"Recipes"}%-15s |")
    println(f"| :--------------------------------------- | --------------: |")
    cuisines.foreach(x => println(f"| ${x._1}%-40s | ${x._2}%15d |"))

    println("\n### Total ingredients per cuisines")
    println(f"| ${"Cuisine"}%-40s | ${"Ingredients #"}%-15s |")
    println(f"| :--------------------------------------- | --------------: |")
    allIngredientsByCuisine.foreach(x => println(f"| ${x._1}%-40s | ${x._2}%15d |"))

    println("\n### Max ingredients used in a recipe per cuisines")
    println(f"| ${"Ingredient"}%-40s | ${"Max Ingr."}%-15s |")
    println(f"| :--------------------------------------- | --------------: |")
    mostIngredientsByCuisine.foreach(x => println(f"| ${x._1}%-40s | ${x._2}%15d |"))

    println("\n### Top 20 ingredients")
    println(f"| ${"Ingredient"}%-40s | ${"Occurrences"}%-15s |")
    println(f"| :--------------------------------------- | --------------: |")
    ingredients.take(20).foreach(x => println(f"| ${x._1}%-40s | ${x._2}%15d |"))

    println("\n### Bottom 20 ingredients")
    println(f"| ${"Ingredient"}%-40s | ${"Occurrences"}%-15s |")
    println(f"| :--------------------------------------- | --------------: |")
    ingredients.reverse.take(20).foreach(x => println(f"| ${x._1}%-40s | ${x._2}%15d |"))

    FlowData(data, cuisineToIndex, ingredientToIndex)

  }

}
