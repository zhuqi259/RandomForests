package org.apache.spark.mllib.feature

import org.apache.spark.annotation.Since
import org.apache.spark.mllib.linalg.{Vectors, Vector}

import scala.collection.mutable

/**
  * Created by zhuqi259 on 2015/12/1.
  */
class ZQHashingTF(override val numFeatures: Int) extends HashingTF {

  /**
    * Transforms the input document into a sparse term frequency vector.
    */
  @Since("1.1.0")
  override def transform(document: Iterable[_]): Vector = {
    val termFrequencies = mutable.HashMap.empty[Int, Double]
    val sum = document.size
    // println(" doc len => " + sum)
    document.foreach { term =>
      val i = indexOf(term)
      termFrequencies.put(i, termFrequencies.getOrElse(i, 0.0) + 1.0)
    }
    val termRegularFrequencies = termFrequencies.map {
      case (i, count) => (i, count / sum)
    }.toSeq
    Vectors.sparse(numFeatures, termRegularFrequencies)
  }

}
