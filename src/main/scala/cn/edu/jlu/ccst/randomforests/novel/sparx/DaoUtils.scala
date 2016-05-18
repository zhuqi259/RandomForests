package cn.edu.jlu.ccst.randomforests.novel.sparx

import cn.edu.jlu.ccst.randomforests.novel.removeHdfsFile
import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.{Model, MulticlassMetrix}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{LogisticRegressionModel, NaiveBayesModel}
import org.apache.spark.mllib.tree.EnhancedRandomForestModel
import org.apache.spark.mllib.tree.model.{DecisionTreeModel, RandomForestModel}

/**
  * VERY UGLY, but for now it works.
  *
  * This might be a good type classes implementation idea.
  */
object DaoUtils {

  def saveMetrix(model: Model[_], metrics: MulticlassMetrix)(implicit sc: SparkContext, configuration: Configuration) = {
    val path = getPath(model) + ".metrics"
    removeHdfsFile(path)
    sc.parallelize(Seq(metrics)).saveAsObjectFile(path)
  }

  def loadMetrix(model: Model[_])(implicit sc: SparkContext, configuration: Configuration):
  Option[MulticlassMetrix] = {
    val path = getPath(model) + ".metrics"
    sc.objectFile[MulticlassMetrix](path).collect.toSeq.headOption
  }

  def getPath(model: Model[_])(implicit configuration: Configuration) = model.self match {
    case m1: RandomForestModel => configuration.randomForestPath
    case m2: EnhancedRandomForestModel => configuration.enhancedRandomForestPath
  }

}
