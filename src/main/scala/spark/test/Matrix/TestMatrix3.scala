package spark.test.Matrix

import breeze.linalg.{DenseMatrix => DM}
import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix

/**
  * @author zhuqi259
  *         测试Matrix
  */
object TestMatrix3 extends SparkContextSupport {

  def main(args: Array[String]) {
    assert(args.length > 0)
    val _from = args(0)
    val data = sc.textFile(_from)
      .map(s => s.split(' ').map(_.toDouble)).map(x => Vectors.dense(x))
    val mat: RowMatrix = new RowMatrix(data)
    val numCols: Long = mat.numCols
    val numRows: Long = mat.numRows
    val dataArray: Array[Double] = mat.rows.map(_.toArray).collect().flatten
    // 行优先 => 列优先
    val dmData: DM[Double] = DM.create(numCols.toInt, numRows.toInt, dataArray).t
    println(dmData)

    val dmData2: DM[Double] = new DM(numRows.toInt, numCols.toInt, dataArray, 0, numCols.toInt, isTranspose = true)
    println(dmData2)
    sc.stop()
  }
}