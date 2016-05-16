package spark.test.Matrix

import breeze.linalg.{DenseMatrix => DM, DenseVector => DV, sum}
import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport

/**
  * @author zhuqi259
  *         测试Matrix
  */
object TestMatrix6 extends SparkContextSupport {

  def main(args: Array[String]) = {
    val n = 10
    val numTrees = 5

    val ratingMatrix: DM[Int] = DM.zeros[Int](n, numTrees)
    val OOBMatrix: DM[Int] = DM.zeros[Int](n, numTrees)

    val weights: Array[Double] = {
      val allScores = sum(ratingMatrix) + 1
      for (i <- 0 until numTrees) yield {
        val ratingData: DV[Int] = ratingMatrix(::, i)
        val OOBData: DV[Int] = OOBMatrix(::, i)
        val score = sum(ratingData) + 1
        val oobi_all = sum(OOBData) + 1
        val re: DV[Int] = ratingData.map(x => if (x == 0) 1 else 0)
        val oobi = OOBData.t * re + 1
        val a = score.toDouble / allScores
        val b = oobi.toDouble / oobi_all
        2 * (1 - b) * a / (1 - b + a)
      }
    }.toArray
    println(weights.mkString(", "))
    sc.stop()
  }
}