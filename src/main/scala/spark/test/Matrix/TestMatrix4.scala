package spark.test.Matrix

import breeze.linalg.{DenseMatrix => DM, DenseVector => DV, Transpose, Axis}
import breeze.math.Complex
import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport

/**
  * @author zhuqi259
  *         测试Matrix
  */
object TestMatrix4 extends SparkContextSupport {

  def gradient1(t: DM[Complex]): DM[Complex] = {
    val numRows: Int = t.rows
    val matrixA: DM[Complex] = t.delete(0, Axis._0)
    val matrixB: DM[Complex] = t.delete(numRows - 1, Axis._0)
    val matrixC: DM[Complex] = matrixA - matrixB
    matrixC
  }

  def gradient2(t: DM[Complex]): DM[Complex] = {
    val numRows: Int = t.rows
    val matrixA: DM[Complex] = t(1 until numRows, ::)
    val matrixB: DM[Complex] = t(0 until numRows - 1, ::)
    val matrixC: DM[Complex] = matrixA - matrixB
    matrixC
  }

  def gradient3(t: DM[Int]): DM[Int] = {
    val numRows: Int = t.rows
    for (i <- (numRows - 1).until(0, -1)) {
      t(i, ::).t := t(i, ::).t - t(i - 1, ::).t //  i - (i-1)
    }
    t(1 until numRows, ::)
  }

  def gradient4(t: DM[Complex]): DM[Complex] = {
    val numRows: Int = t.rows
    for (i <- (numRows - 1).until(0, -1)) {
      t(i, ::).t := t(i, ::).t - t(i - 1, ::).t //  i - (i-1)
    }
    t(1 until numRows, ::)
  }

  def show(t: DM[Complex]): Vector[String] = {
    val numRows: Int = t.rows
    val ret = for (i <- 0 until numRows) yield {
      t(i, ::).t.toArray mkString "\t"
    }
    ret.toVector
  }

  def main(args: Array[String]) = {
    val data: Array[Complex] = {
      for (i <- 0 until 30) yield {
        Complex(i, i + 1)
      }
    }.toArray
    val dmData1: DM[Complex] = new DM(5, 6, data, 0, 6, true)
    println(dmData1)
    val dmData2: DM[Complex] = new DM(6, 5, data)
    println(dmData2)
    val dmData3: DM[Complex] = dmData1 * dmData2
    println(dmData3)
    val dmData4: DM[Complex] = gradient1(dmData2)
    println(dmData4)
    val dmData5: DM[Complex] = gradient2(dmData2)
    println(dmData5)

    val m = DM.ones[Int](5, 4)
    m(4, ::) := DV(1, 2, 3, 4).t
    println(m)

    val m1 = DM.ones[Int](5, 4)
    m1(4, ::).t := DV(1, 2, 3, 4)
    println(m1)

    val n = DM.ones[Complex](5, 4)
    n(4, ::).t := DV(Complex(1, 1), Complex(2, 2), Complex(3, 3), Complex(4, 4))
    println(n)

    //    val n1 = DM.ones[Complex](5, 4)
    //    n1(4, ::) := DV(Complex(1, 1), Complex(2, 2), Complex(3, 3), Complex(4, 4)).t
    //    println(n1)

    val dmData6: DM[Int] = gradient3(m)
    println(dmData6)
    val dmData7: DM[Complex] = gradient4(dmData2)
    println(dmData7)

    assert(args.length > 0)
    val _to = args(0)
    val data2: Vector[String] = show(dmData7)
    println(data2)
    sc.parallelize(data2).saveAsTextFile(_to)

    println(DV(1, 2, 3, 4).t)
    val dv1: DV[Complex] = DV(Complex(1, 1), Complex(2, 2), Complex(3, 3), Complex(4, 4))
    println(dv1)
    val dd2: DM[Complex] = dv1.t
    println(dd2)
    val dd3: DM[Complex] = dd2.t
    println(dd3)
    val dd4: Transpose[DV[Complex]] = dmData7(0, ::)
    println(dd4)
    val dd5: DV[Complex] = dd4.t
    println(dd5)
    sc.stop()
  }
}