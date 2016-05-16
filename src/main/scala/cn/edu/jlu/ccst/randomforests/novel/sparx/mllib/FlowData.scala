package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib

import cn.edu.jlu.ccst.randomforests.novel.removeHdfsFile
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD

/** s
  * This is the data that is passed along in the flow.
  *
  * It contains all necessary data for training and essential data for prediction
  * (the index to label and index to feature mappings)
  */
case class FlowData(data: RDD[LabeledPoint],
                    labelToIndex: Map[String, Int],
                    featureToIndex: Map[String, Int]) {

  // TODO: I have some funky ideas for this class... I smell a monad...

  import FlowData._

  val indexToLabel = labelToIndex.map(r => r._2 -> r._1)

  val indexToFeature = featureToIndex.map(r => r._2 -> r._1)

  def map(f: (FlowData) => FlowData): FlowData = f(this)

  // TODO FlowData DAO should be transparent (e.g. dealt with by third party)
  def save(rootPath: String)(implicit sc: SparkContext) = {

    removeHdfsFile(rootPath + dataDir)
    data.saveAsTextFile(rootPath + dataDir)

    removeHdfsFile(rootPath + labelsDir)
    sc.parallelize(labelToIndex.toSeq).saveAsObjectFile(rootPath + labelsDir)

    removeHdfsFile(rootPath + featuresDir)
    sc.parallelize(featureToIndex.toSeq).saveAsObjectFile(rootPath + featuresDir)

  }

  def setData(newData: RDD[LabeledPoint]): FlowData =
    FlowData(newData, labelToIndex, featureToIndex)

  def setLabelToIndex(newLabelToIndex: Map[String, Int]): FlowData =
    FlowData(data, newLabelToIndex, featureToIndex)

  def setFeatureToIndex(newFeatureToIndex: Map[String, Int]): FlowData =
    FlowData(data, labelToIndex, newFeatureToIndex)

}

object FlowData {

  private val dataDir = "/data"
  private val labelsDir = "/labels"
  private val featuresDir = "/features"

  // TODO FlowData DAO should be transparent (e.g. dealt with by third party)
  def load(rootPath: String)(implicit sc: SparkContext): FlowData = {
    FlowData(
      MLUtils.loadLabeledPoints(sc, rootPath + dataDir),
      sc.objectFile[(String, Int)](rootPath + labelsDir).collect().toMap,
      sc.objectFile[(String, Int)](rootPath + featuresDir).collect().toMap
    )
  }

}
