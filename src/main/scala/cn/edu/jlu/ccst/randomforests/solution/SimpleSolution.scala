package cn.edu.jlu.ccst.randomforests.solution

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.mllib.feature.{IDF, ZQHashingTF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.rdd.RDD

/**
  * @author zhuqi259
  *         随机森林的基本用法
  *         1. wordsegmenter 分词
  *         2. SogouFileCombination 组合文件
  *         3. TF-IDF feature vectorization
  *         4. Feature selection <未实现>
  *         5. Random Forests 分类 => 训练结果很好，测试结果不好
  *         原因：
  *         【1】数据量少
  *         【2】采样数据不均衡
  *         【3】没有第4步
  *         【4】基分类器待测试 （训练准确率不是100%）
  */
object SimpleSolution extends SparkContextSupport {
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
    val numClasses = 10
    val categoricalFeaturesInfo = Map[Int, Int]()
    val numTrees = 200 // Use more in practice.
    val featureSubsetStrategy = "auto" // Let the algorithm choose.
    val impurity = "gini"
    val maxDepth = 10
    val maxBins = 32

    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)

    // Evaluate model on train instances and compute train error
    val train = trainingData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val trainErr = train.filter(r => r._1 != r._2).count.toDouble / trainingData.count()
    println("Train Error = " + trainErr)

    // Evaluate model on test instances and compute test error
    val labelAndPreds = testData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val testErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testData.count()
    println("Test Error = " + testErr)

    // println("Learned classification forest model:\n" + model.toDebugString)
    // Save and load model
    //    model.save(sc, "myModelPath")
    //    val sameModel = RandomForestModel.load(sc, "myModelPath")

    sc.stop()
  }
}
