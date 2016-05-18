package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.trainers

import cn.edu.jlu.ccst.randomforests.novel._
import cn.edu.jlu.ccst.randomforests.novel.sparx._
import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.FlowData
import org.apache.spark.mllib.tree.{EnhancedRandomForest, EnhancedRandomForestModel}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by zhuqi259 on 2016/5/18.
  */
class EnhancedRandomForestTrainer(maxDepth: Int = 15,
                                  maxBins: Int = 32,
                                  numTrees: Int = 15,
                                  impurity: String = "gini",
                                  featureSubsetStrategy: String = "auto",
                                  useWeights: Boolean = true,
                                  oob: Boolean = true,
                                  varImport: Boolean = false)
  extends Trainer[EnhancedRandomForestModel] {

  def train(flowData: FlowData)(implicit sc: SparkContext) = {

    val numClasses = flowData.labelToIndex.size + 1
    val numFeatures = flowData.featureToIndex.size

    val trainingData = flowData.data

    //  Empty categoricalFeaturesInfo indicates all features are continuous.
    val categoricalFeaturesInfo = (0 until numFeatures).map(i => i -> 2).toMap

    EnhancedRandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins, useWeights, oob, varImport)
  }
}


object EnhancedRandomForestTrainer {

  def apply() = new EnhancedRandomForestTrainer

  def main(args: Array[String]) = {

    val conf = new SparkConf(true).setAppName(this.getClass.getSimpleName).
      setMaster("local[*]")

    implicit val sc = new SparkContext(conf)
    implicit val configuration = Configuration(args)

    val flowData = FlowData.load(configuration.dataPath)

    val (model, metrics) = EnhancedRandomForestTrainer().trainEvaluate(flowData)

    removeHdfsFile(configuration.enhancedRandomForestPath)
    model.save(configuration.enhancedRandomForestPath)

    println(s"### ${model.self.getClass.getSimpleName} model evaluation")

    printEvaluationMetrix(metrics)

  }

}

