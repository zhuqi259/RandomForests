package cn.edu.jlu.ccst.randomforests.solution

import breeze.linalg.{DenseMatrix => DM, sum, trace}
import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.mllib.feature.{IDF, ZQHashingTF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.{EnhancedRandomForest, EnhancedRandomForestModel}
import org.apache.spark.rdd.RDD

/**
  * @author zhuqi259
  *
  */
object EnhancedSolution2 extends SparkContextSupport {

  //评估性能指标
  def evaluate(data: RDD[(Double, Double)], numClasses: Int) = {
    // 1. 多类分类混淆矩阵
    val k = numClasses
    val cs = data.map((_, 1)).reduceByKey(_ + _).collect()
    val confuseMatrix: DM[Int] = DM.zeros[Int](k, k) // 初始为全0矩阵
    cs.foreach {
      case ((label, prediction), count) =>
        val (i, j) = (prediction.toInt, label.toInt)
        confuseMatrix(i, j) = count
    }
    println("多类分类混淆矩阵 => ")
    println(confuseMatrix)
    val Accuracy = data.filter(r => r._1 == r._2).count().toDouble / data.count()
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
  }

  def main(args: Array[String]) {
    if (args.length > 1) {
      sc.addJar(args(1))
    }
    assert(args.length > 0)
    val _from = args(0)
    val labeledDocuments = sc.textFile(_from).map {
      x => {
        val data = x.split("\t")
        (data(1).toDouble, data(0).split(" ").toSeq)
      }
    }
    val ys = labeledDocuments.keys
    ys.foreach(println)
    val documents: RDD[Seq[String]] = labeledDocuments.values
    val hashingTF = new ZQHashingTF(1 << 18)
    val tf: RDD[Vector] = hashingTF.transform(documents)
    tf.cache()
    val idf = new IDF().fit(tf)
    val tf_idf: RDD[Vector] = idf.transform(tf)
    val data = ys.zip(tf_idf).map {
      case (label, vec) => LabeledPoint(label, vec)
    }

    // Split the data into training and test sets (30% held out for testing)
    val splits = data.randomSplit(Array(0.7, 0.3))
    val (trainingData, testData) = (splits(0), splits(1))

    // Train a RandomForest model.
    //  Empty categoricalFeaturesInfo indicates all features are continuous.
    val numClasses = 9
    val categoricalFeaturesInfo = Map[Int, Int]()
    val numTrees = 250 // Use more in practice.
    val featureSubsetStrategy = "sqrt" // Let the algorithm choose.
    val impurity = "gini"
    val maxDepth = 10
    val maxBins = 100

    val model: EnhancedRandomForestModel = EnhancedRandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins, useWeights = true, oob = true, varImport = false)

    // Evaluate model on train instances and compute train error
    val train = trainingData.map { point =>
      val prediction = model.forest.predict(point.features)
      (point.label, prediction)
    }

    val train2 = trainingData.map { point =>
      val prediction = model.enhancedPredict(point.features)
      (point.label, prediction)
    }
    train.cache()
    train2.cache()

    val trainErr = train.filter(r => r._1 != r._2).count.toDouble / trainingData.count()
    val trainErr2 = train2.filter(r => r._1 != r._2).count.toDouble / trainingData.count()

    //加权前
    evaluate(train, numClasses)
    //加权后
    evaluate(train2, numClasses)

    // Evaluate model on test instances and compute test error
    val test = testData.map { point =>
      val prediction = model.forest.predict(point.features)
      (point.label, prediction)
    }
    val test2 = testData.map { point =>
      val prediction = model.enhancedPredict(point.features)
      (point.label, prediction)
    }

    val testErr = test.filter(r => r._1 != r._2).count.toDouble / testData.count()
    val testErr2 = test2.filter(r => r._1 != r._2).count.toDouble / testData.count()

    //    println("Learned classification forest model:\n" + model.toDebugString)
    //    Save and load model
    //    model.save(sc, "myModelPath")
    //    val sameModel = RandomForestModel.load(sc, "myModelPath")

    println("Learned classification forest model:\n" + model.toString)
    println("Train Error = " + trainErr)
    println("Train Error2 = " + trainErr2)
    println("Test Error = " + testErr)
    println("Test Error2 = " + testErr2)

    sc.stop()
  }
}
