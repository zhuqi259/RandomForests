package spark.test.Matrix

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.mllib.linalg.{Matrix, Matrices}

/**
  * @author zhuqi259
  *         测试Matrix
  */
object TestMatrix1 extends SparkContextSupport {

  def main(args: Array[String]) {
    val dm: Matrix = Matrices.dense(3, 4, Array(1, 4, 8, 2, 5, 9, 3, 6, 1, 4, 7, 2))
    println(dm)
    val numRows = dm.numRows
    val numCols = dm.numCols
    val oldArray = dm.toArray
    val newArray = new Array[Double](numRows * numCols)
    for (i <- 0 until numRows) {
      for (j <- 0 until numCols) {
        newArray(j * numRows + i) = oldArray(j * numRows + i) - oldArray(j * numRows)
      }
    }
    val newDm: Matrix = Matrices.dense(3, 4, newArray)
    println(newDm)
    sc.stop()
  }
}
