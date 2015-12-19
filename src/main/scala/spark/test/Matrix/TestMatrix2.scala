package spark.test.Matrix

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport
import breeze.linalg.{DenseMatrix => DM, DenseVector => DV}
import breeze.linalg.*

/**
  * @author zhuqi259
  *         测试Matrix
  */
object TestMatrix2 extends SparkContextSupport {

  def main(args: Array[String]) {
    val dm: DM[Int] = DM((1, 2, 3, 4), (4, 5, 6, 7), (8, 9, 1, 2))
    val firstRow: DV[Int] = dm(0, ::).t
    val res = dm(*, ::) - firstRow
    println(res)
    sc.stop()
  }
}