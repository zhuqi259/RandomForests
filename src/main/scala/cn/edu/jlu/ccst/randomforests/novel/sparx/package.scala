package cn.edu.jlu.ccst.randomforests.novel

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.trainers.Trainer
import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.{FlowData, MulticlassMetrix}
import org.apache.spark.mllib.evaluation.MulticlassMetrics

package object sparx {


  def printEvaluationMetrics(metrics: MulticlassMetrics, runtime: Trainer.Runtime, flowData: FlowData) = {

    println(evaluationMetricsText(metrics, runtime, flowData))

  }

  def printEvaluationMetrix(metrics: MulticlassMetrix) = {

    println(evaluationMetrixText(metrics))

  }

  def evaluationMetricsText(metrics: MulticlassMetrics, runtime: Trainer.Runtime, flowData: FlowData): String = {

    val generalStats = "" ::
      ( "| Parameter                    | Value     |") ::
      ( "| :--------------------------- | --------: |") ::
      (f"| Precision                    | ${metrics.precision * 100}%8.4f%% |") ::
      (f"| Error                        | ${(1 - metrics.precision) * 100}%8.4f%% |") ::
      (f"| Weighted Precision           | ${metrics.weightedPrecision * 100}%8.4f%% |") ::
      (f"| Weighted True Positive Rate  | ${metrics.weightedTruePositiveRate * 100}%8.4f%% |") ::
      (f"| Weighted False Positive Rate | ${metrics.weightedFalsePositiveRate * 100}%8.4f%% |") ::
      Nil

    val statsPerCuisineHeader = "" ::
      ("| Cuisine              | TPR       | FPR       | Prec.     | Error     |") ::
      ("| :------------------- | --------: | --------: | --------: | --------: |") ::
      Nil

    val statsPerCuisine = flowData.indexToLabel.map{ case(label, labelString) =>
      val tpr = metrics.truePositiveRate(label)
      val fpr = metrics.falsePositiveRate(label)
      val pr = metrics.precision(label)
      val er = 1 - pr
      (f"| ${labelString}%-20s | ${tpr * 100}%8.4f%% | ${fpr * 100}%8.4f%% | ${pr * 100}%8.4f%% | ${er * 100}%8.4f%% | ")
    }

    val legend = "" ::
      ("| Legend ||") ::
      ("| ------ | ----------------------- |") ::
      ("| TPR    | True Positive Rate      |") ::
      ("| FPR    | False Positive Rate     |") ::
      ("| Prec.  | Precision (TP / LC)     |") ::
      ("| TP     | True Positive by Class  |") ::
      ("| LC     | Label Count By Class    |") ::
      Nil

    val runtimeString = (f"Training was completed in ${runtime/1000/60}%02d:${runtime/1000%60}%02d (minutes:seconds).")

    val text = generalStats ++ statsPerCuisineHeader ++ statsPerCuisine ++ legend :+ runtimeString
    text.mkString("\n")
  }


  def evaluationMetrixText(metrics: MulticlassMetrix): String = {

    val generalStats = "" ::
      ( "| Parameter                    | Value     |") ::
      ( "| :--------------------------- | --------: |") ::
      (f"| Precision                    | ${metrics.precision * 100}%8.4f%% |") ::
      (f"| Error                        | ${(1 - metrics.precision) * 100}%8.4f%% |") ::
      (f"| Weighted Precision           | ${metrics.weightedPrecision * 100}%8.4f%% |") ::
      (f"| Weighted True Positive Rate  | ${metrics.weightedTruePositiveRate * 100}%8.4f%% |") ::
      (f"| Weighted False Positive Rate | ${metrics.weightedFalsePositiveRate * 100}%8.4f%% |") ::
      Nil

    val statsPerCuisineHeader = "" ::
      ("| Cuisine              | TPR       | FPR       | Prec.     | Error     |") ::
      ("| :------------------- | --------: | --------: | --------: | --------: |") ::
      Nil

    val statsPerCuisine = metrics.metricsByLabel.values.map { mbl =>
      val tpr = mbl.truePositiveRate
      val fpr = mbl.falsePositiveRate
      val pr = mbl.precision
      val er = 1 - pr
      (f"| ${mbl.label}%-20s | ${tpr * 100}%8.4f%% | ${fpr * 100}%8.4f%% | ${pr * 100}%8.4f%% | ${er * 100}%8.4f%% | ")
    }

    val legend = "" ::
      ("| Legend ||") ::
      ("| ------ | ----------------------- |") ::
      ("| TPR    | True Positive Rate      |") ::
      ("| FPR    | False Positive Rate     |") ::
      ("| Prec.  | Precision (TP / LC)     |") ::
      ("| TP     | True Positive by Class  |") ::
      ("| LC     | Label Count By Class    |") ::
      Nil

    val runtimeString = (f"Training was completed in ${metrics.trainingRuntime/1000/60}%02d:${metrics.trainingRuntime/1000%60}%02d (minutes:seconds).")

    val text = generalStats ++ statsPerCuisineHeader ++ statsPerCuisine ++ legend :+ runtimeString
    text.mkString("\n")
  }


}
