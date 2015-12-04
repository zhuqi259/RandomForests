package spark.test

import cn.edu.jlu.ccst.randomforests.util.SparkContextSupport

/**
  * @author zhuqi259
  *         WordCount SortByValue 排序
  *         练习top/takeOrdered与自定义Ordering
  */
object TestWordCount extends SparkContextSupport {

  def main(args: Array[String]) = {
    implicit val myOrdering = new Ordering[(String, Int)] {
      override def compare(a: (String, Int), b: (String, Int)) =
        a._2.compare(b._2)
    }
    if (args.length > 3) {
      sc.addJar(args(3))
    }
    assert(args.length > 2)
    val _from = args(0)
    val _to1 = args(1)
    val _to2 = args(2)

    val wc = sc.textFile(_from).map {
      line => {
        val s = line.split("\t")
        (s(0), s(1).toInt)
      }
    }
    wc.sortBy(_._2, ascending = false).map(x => x._1 + "\t" + x._2).saveAsTextFile(_to1)
    // top
    sc.parallelize(wc.takeOrdered(10)(myOrdering.reverse)).map(x => x._1 + "\t" + x._2).saveAsTextFile(_to2)
  }

}
