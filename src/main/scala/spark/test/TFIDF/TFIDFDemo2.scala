package spark.test.TFIDF

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.mllib.feature.{IDF, ZQHashingTF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.rdd.RDD

/**
  * @author zhuqi259
  *         TF-IDF Demo
  */
object TFIDFDemo2 extends SparkContextSupport {
  def main(args: Array[String]) {
    if (args.length > 1) {
      sc.addJar(args(1))
    }
    assert(args.length > 0)
    val _from = args(0)
    //  val documents: RDD[Seq[String]] = sc.textFile("hdfs://192.168.1.98:9000/user/hadoop/data/AfterWordSegmentation/SogouC.mini/Sample/*").map(_.split(" ").toSeq)
    val documents: RDD[Seq[String]] = sc.textFile(_from).map(_.split(" ").toSeq)

    documents.foreach(s => {
      println(s mkString " ") // Doc 一行
    })
    val hashingTF = new ZQHashingTF(1 << 18)
    val tf: RDD[Vector] = hashingTF.transform(documents)
    // println("TF===========================")
    // tf => SparseVector(size, indices, values)
    // tf是一个稀疏矩阵
    // indices => word的hash值 val i = indexOf(term) Utils.nonNegativeMod(term.##, numFeatures)
    // values => word计数（word在当前文档中的数目）
    // println(tf.collect().mkString("\n"))
    tf.cache()
    val idf = new IDF().fit(tf) // 这里是一个IDFModel
    val realIDF: Vector = idf.idf // 真正的idf是一个Vector
    println(realIDF.size)
    val tfidf: RDD[Vector] = idf.transform(tf)
    // println("TF-IDF===========================")
    // tfidf => SparseVector(size, indices, values) 也是一个稀疏矩阵
    // indices => word的hash值
    // values => tfidf值
    println(tfidf.collect().mkString("\n"))

    sc.stop()
  }
}
