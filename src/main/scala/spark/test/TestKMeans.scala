package spark.test

import breeze.linalg.DenseVector
import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.stat.Statistics

object TestKMeans extends SparkContextSupport {

  def main(args: Array[String]) = {
    assert(args.length > 0)
    val _from = args(0)

    // Load and parse the data
    val data = sc.textFile(_from)
    val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble))).cache()

    // Cluster the data into two classes using KMeans
    val numClusters = 2
    val numIterations = 20
    val clusters = KMeans.train(parsedData, numClusters, numIterations)

    // Evaluate clustering by computing Within Set Sum of Squared Errors
    val WSSSE = clusters.computeCost(parsedData)
    println("Within Set Sum of Squared Errors = " + WSSSE)

    val colSummary = Statistics.colStats(parsedData)
    val k_mean: DenseVector[Double] = DenseVector(colSummary.mean.toArray)
    val centers = clusters.clusterCenters
    val J3: Double = centers.map {
      x => {
        val xx = DenseVector(x.toArray) - k_mean
        xx.dot(xx)
      }
    }.sum
    println("J3 = %f".format(J3))

    val points = centers.zipWithIndex.map {
      case (vec, label) => LabeledPoint(label, vec)
    }
    println(points mkString "\n")
  }
}
