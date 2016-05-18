package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib

import cn.edu.jlu.ccst.randomforests.novel.removeHdfsFile
import cn.edu.jlu.ccst.randomforests.novel.sparx.Configuration
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.tree.EnhancedRandomForestModel
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.rdd.RDD

/**
  * Interface unification for the MLLib models
  *
  * @tparam T
  */
trait Model[T] extends Serializable {
  def self: T

  def predict(testData: Vector): Double

  def predict(testData: RDD[Vector]): RDD[Double]

  def save(path: String)(implicit sc: SparkContext, configuration: Configuration): Unit

  def name = self.getClass.getSimpleName
}


object Model {
  implicit def randomForestModelToModel(originalModel: RandomForestModel): Model[RandomForestModel] =
    new Model[RandomForestModel] {
      def self = originalModel

      def predict(testData: Vector): Double = {
        originalModel.predict(testData)
      }

      def predict(testData: RDD[Vector]): RDD[Double] = {
        originalModel.predict(testData)
      }

      def save(path: String)(implicit sc: SparkContext, configuration: Configuration) = {
        removeHdfsFile(path)
        originalModel.save(sc, path)
      }
    }

  implicit def enhancedRandomForestModelToModel(originalModel: EnhancedRandomForestModel): Model[EnhancedRandomForestModel] =
    new Model[EnhancedRandomForestModel] {
      def self = originalModel

      def predict(testData: Vector): Double = {
        originalModel.enhancedPredict(testData)
      }

      def predict(testData: RDD[Vector]): RDD[Double] = {
        testData.map(originalModel.enhancedPredict)
      }

      def save(path: String)(implicit sc: SparkContext, configuration: Configuration) = {
        removeHdfsFile(path)
        // TODO 保存weight, I forgot it!
        originalModel.forest.save(sc, path)
      }
    }
}


