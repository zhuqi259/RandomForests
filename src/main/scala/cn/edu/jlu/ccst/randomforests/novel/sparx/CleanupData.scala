package cn.edu.jlu.ccst.randomforests.novel.sparx

import cn.edu.jlu.ccst.randomforests.novel.removeHdfsFile
import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

object CleanupData extends SparkRunnable {

  def main(args: Array[String]) = {

    DefaultSparkRunner(this.getClass.getSimpleName, args).run(this)

  }

  def run(implicit sc: SparkContext, configuration: Configuration) = {

    // import the recipes
    val recipes = RecipesImporter.importFrom(configuration.inputTrainingData, configuration.inputDataType)

    // Normally we should keep this an RDD, but we have a small list here
    val cuisines = recipes.map(r => r.cuisine)
      .map((_, 1)).reduceByKey(_ + _).sortBy(_._2, false) // sort the cuisines by the number of recipes descending
      .collect
    val cuisinesList = cuisines.map(_._1).toSeq
    val cuisineToIndex = cuisinesList
      .zipWithIndex.toMap


    val xlabels = recipes.map(r => cuisineToIndex(r.cuisine))
    val xfeatures = recipes.map(r => r.ingredients)

    val transformer0 = new HashingTF()
    val features0 = transformer0.transform(xfeatures).cache
    val data0 = xlabels.zip(features0).map(x => LabeledPoint(x._1, x._2))

    //    val transformer1 = new ChiSqSelector(5000).fit(data0)
    //    val features1 = transformer1.transform(features0)
    //    val data1 = xlabels.zip(features1).map(x => LabeledPoint(x._1, x._2))

    val transformer2 = new IDF(minDocFreq = 2).fit(features0)
    //    val features2 = transformer2.transform(features1)
    //    val data2 = xlabels.zip(features2).map(x => LabeledPoint(x._1, x._2))

    //    data2.take(10).foreach(println)

    val transformers: List[XTransformer[TransformerType]] = List(
      //      new XTransformer(transformer1),
      new XTransformer(transformer2))

    val rez0 = transformers.foldLeft(features0) {
      case (rez: RDD[Vector], tr: XTransformer[_]) =>
        tr.transform(rez)
    }
    //    val rez0 = transformers.head.transform(features0)

    // Create the HDFS file system handle
    //    val hdfs = FileSystem.get(new org.apache.hadoop.conf.Configuration())

    //    val path = new Path("/tmp/transformers")
    //    val oos = new ObjectOutputStream(hdfs.create(path, true))
    //    oos.write(transformers)


    removeHdfsFile("/tmp/transformers")
    sc.parallelize(transformers).saveAsObjectFile("/tmp/transformers")

    val newTransformers = sc.objectFile[XTransformer[TransformerType]]("/tmp/transformers").collect

    //    val rez1 = newTransformers.foldLeft(features0){case (rez, tr) => tr.transform(rez)}
    val rez1 = newTransformers.head.transform(features0)
    val xrez1 = rez1.take(10)

    println("--------")
    rez0.take(10).foreach(println)
    println("--------")
    rez1.take(10).foreach(println)
    println("--------")
  }

  type TransformerType = {def transform(dataset: RDD[Vector]): RDD[Vector]}

  case class XTransformer[T <: TransformerType](transformer: T) {
    def transform(dataSet: RDD[Vector]): RDD[Vector] = {
      transformer.transform(dataSet)
    }

    //    def transform(labels: RDD[Int], dataSet: RDD[Vector]): RDD[LabeledPoint] = {
    //      labels.zip(t.transform(dataSet)).map(x => LabeledPoint(x._1, x._2))
    //    }
    //    def transformLPs(lps: RDD[LabeledPoint]): RDD[LabeledPoint] = {
    //      val labels = lps.map(lp => lp.label)
    //      val features = lps.map(lp => lp.features)
    //      labels.zip(t.transform(features)).map(x => LabeledPoint(x._1, x._2))
    //    }
  }


}
