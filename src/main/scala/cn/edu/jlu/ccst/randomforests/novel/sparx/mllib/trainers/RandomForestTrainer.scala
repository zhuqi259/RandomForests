package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.trainers

import cn.edu.jlu.ccst.randomforests.novel.removeHdfsFile
import cn.edu.jlu.ccst.randomforests.novel.sparx.{Configuration, printEvaluationMetrix}
import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.FlowData
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.{SparkConf, SparkContext}

/**
  *
  * @param maxDepth
  * @param maxBins
  * @param numTrees
  * @param impurity acceptable values: "gini" or "entropy"
  * @param featureSubsetStrategy
  */
class RandomForestTrainer(maxDepth: Int = 15,
                          maxBins: Int = 32,
                          numTrees: Int = 20,
                          impurity: String = "gini",
                          featureSubsetStrategy: String = "auto")
  extends Trainer[RandomForestModel] {

  def train(flowData: FlowData)(implicit sc: SparkContext) = {

    val numClasses = flowData.labelToIndex.size + 1
    val numFeatures = flowData.featureToIndex.size

    val trainingData = flowData.data

    //  Empty categoricalFeaturesInfo indicates all features are continuous.
    val categoricalFeaturesInfo = (0 until numFeatures).map(i => i -> 2).toMap

    RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)
  }

}

object RandomForestTrainer {

  def apply() = new RandomForestTrainer

  def main(args: Array[String]) = {

    val conf = new SparkConf(true).setAppName(this.getClass.getSimpleName).
      setMaster("local[*]")

    implicit val sc = new SparkContext(conf)
    implicit val configuration = Configuration(args)

    val flowData = FlowData.load(configuration.dataPath)

    val (model, metrics) = RandomForestTrainer().trainEvaluate(flowData)

    removeHdfsFile(configuration.randomForestPath)
    model.save(configuration.randomForestPath)

    println(s"### ${model.self.getClass.getSimpleName} model evaluation")

    printEvaluationMetrix(metrics)

  }

}
