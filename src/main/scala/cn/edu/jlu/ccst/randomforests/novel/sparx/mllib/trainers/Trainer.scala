package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.trainers

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.{FlowData, Model, MulticlassMetrix}
import cn.edu.jlu.ccst.randomforests.novel.timeCode
import org.apache.spark.SparkContext
import org.apache.spark.mllib.evaluation.MulticlassMetrics

/**
  * Trainer interface for unifying the MLLib.
  */
trait Trainer[M] {

  def train(flowData: FlowData)(implicit sc: SparkContext): Model[M]

  def trainEvaluate(flowData: FlowData, trainingQuota: Double = 0.7)
                   (implicit sc: SparkContext): (Model[M], MulticlassMetrix) = {

    val splits = flowData.data.randomSplit(Array(trainingQuota, 1 - trainingQuota))
    val (trainingData, testData) = (splits(0), splits(1))

    val (model, runtime) = timeCode {
      train(flowData.setData(trainingData))
    }

    val predictionAndLabels = testData.map { point =>
      val prediction = model.predict(point.features)
      (prediction, point.label)
    }
    val metrics = MulticlassMetrix(new MulticlassMetrics(predictionAndLabels), flowData, runtime)

    (model, metrics)

  }
}

object Trainer {

  type Runtime = Long

}
