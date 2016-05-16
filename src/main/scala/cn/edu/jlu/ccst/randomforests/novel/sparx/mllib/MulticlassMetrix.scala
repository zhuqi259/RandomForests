package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.trainers.Trainer
import org.apache.spark.mllib.evaluation.MulticlassMetrics

/**
  * We need this info for the final result.
  *
  * Do we need something smarter? Always! but now? Probably not.
  *
  * This is meant to be a Human Readable kind of class,
  * using the actual labels instead of machine friendly doubles.
  */
case class MulticlassMetrix(precision: Double, weightedPrecision: Double,
                            weightedTruePositiveRate: Double, weightedFalsePositiveRate: Double,
                            metricsByLabel: Map[String, ClassMetrics], trainingRuntime: Trainer.Runtime)

object MulticlassMetrix {

  def apply(metrics: MulticlassMetrics,
            flowData: FlowData,
            trainingRuntime: Trainer.Runtime): MulticlassMetrix = {

    val metricsByLabel = flowData.indexToLabel.map { case (label, labelString) =>
      val tpr = metrics.truePositiveRate(label)
      val fpr = metrics.falsePositiveRate(label)
      val pr = metrics.precision(label)
      (labelString -> ClassMetrics(labelString, pr, tpr, fpr))
    }

    MulticlassMetrix(metrics.precision, metrics.weightedPrecision,
      metrics.weightedTruePositiveRate, metrics.weightedFalsePositiveRate,
      metricsByLabel, trainingRuntime
    )
  }

}

case class ClassMetrics(label: String, precision: Double, truePositiveRate: Double, falsePositiveRate: Double)

