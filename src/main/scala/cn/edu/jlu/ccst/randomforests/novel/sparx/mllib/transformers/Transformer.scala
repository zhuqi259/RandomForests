package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.transformers

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.FlowData

/**
  * Interface for transforming flow data (e.g. filtering, sorting,
  * applying smart algorithms on it...)
  */
trait Transformer[T] {

  def transform(input: T): FlowData

}
