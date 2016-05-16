package spark.test.Matrix

import breeze.linalg.{DenseMatrix => DM, sum, trace}
import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport

/**
  * @author zhuqi259
  *         测试Matrix
  */
object TestMatrix5 extends SparkContextSupport {

  def next(start: Int, end: Int): Double = {
    val rnd = new scala.util.Random
    (start + rnd.nextInt((end - start) + 1)).toDouble
  }

  def main(args: Array[String]) = {
    val n = 1000
    val num = 25
    val start = 0
    val end = num - 1
    val train: Array[(Double, Double)] = {
      for (i <- 0 until n) yield {
        (next(start, end), next(start, end))
      }
    }.toArray

    val k = num
    val confuseMatrix: DM[Int] = DM.zeros[Int](k, k) // 初始为全0矩阵
    train.foreach {
      case (label, prediction) =>
        println(s"label: $label, prediction: $prediction")
        val (i, j) = (prediction.toInt, label.toInt)
        confuseMatrix(i, j) += 1
    }
    println(confuseMatrix)
    val Accuracy = train.count(r => r._1 == r._2).toDouble / train.length
    println("Org=> Accuracy = " + Accuracy)

    // 1. confuseMatrix => Accuracy
    val right: Double = trace(confuseMatrix).toDouble // 对角线和
    val all = sum(confuseMatrix)
    println("CM => Accuracy = " + right / all)

    /**
      * 真实值
      * 1	…	i	…	j	…	k
      * 预测值	1	C11	…	C1i	…	C1j	…	C1k
      * …	…	…	…	…	…	…	…
      * i	Ci1	…	Cii	…	Cij	…	Cik
      * …	…	…	…	…	…	…	…
      * j	Cj1	…	Cji	…	Cjj	…	Cjk
      * …	…	…	…	…	…	…	…
      * k	Ck1	…	Cki	…	Ckj	…	Ckk
      *
      */
    // TP(i)表示真实类别为类i且被正确分为类i的数目
    val TP: Array[Int] = {
      for (i <- 0 until k) yield {
        confuseMatrix(i, i)
      }
    }.toArray

    // FN(i)表示真实类别为类i但没有正确被分为类i的数目
    val FN: Array[Int] = {
      for (i <- 0 until k) yield {
        (0 until k).filter(_ != i).map {
          index => confuseMatrix(index, i)
        }.sum
      }
    }.toArray

    // FP(i)表示真实类别不为类i但被错误分为类i的数目
    val FP: Array[Int] = {
      for (i <- 0 until k) yield {
        (0 until k).filter(_ != i).map {
          index => confuseMatrix(i, index)
        }.sum
      }
    }.toArray

    println("CM => Accuracy = " + TP.sum.toDouble / (TP.sum + FN.sum))
    println("CM => Accuracy = " + TP.sum.toDouble / (TP.sum + FP.sum))

    // Recall(i)表示真实类别是类i的样本中被正确分类成类i的样本所占比例

    val Recall: Array[Double] = TP.zip(FN).map {
      case (tp, fn) =>
        if (tp == 0 && fn == 0)
          0
        else
          tp.toDouble / (tp + fn)
    }
    // Precision(i)则表示被分类为类i的样本中真实类别就是类i的正确样本所占的比重。
    val Precision: Array[Double] = TP.zip(FP).map {
      case (tp, fp) =>
        if (tp == 0 && fp == 0)
          0
        else
          tp.toDouble / (tp + fp)
    }

    val Macro_Recall = Recall.sum / k
    val Macro_Precision = Precision.sum / k

    val F1: Array[Double] = Precision.zip(Recall).map {
      case (precision, recall) =>
        if (precision == 0 && recall == 0)
          0
        else
          2 * precision * recall / (precision + recall) // 防止 /0
    }
    val Macro_F1 = F1.sum / k

    println(
      s"""Recall => ${Recall.toVector}
          |Precision => ${Precision.toVector}
          |Macro_Recall => $Macro_Recall
          |Macro_Precision => $Macro_Precision
          |F1 => ${F1.toVector}
          |Macro_F1 => $Macro_F1
       """.stripMargin)
    sc.stop()
  }
}