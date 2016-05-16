package cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.transformers

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.FlowData
import org.apache.spark.mllib.feature.ChiSqSelector
import org.apache.spark.mllib.regression.LabeledPoint

/**
  *
  */
case class ChiSqSelectorTransformer(topFeaturesRatio: Double) extends Transformer[FlowData] {

  require(topFeaturesRatio < 1.0 && topFeaturesRatio > 0)

  override def transform(flowData: FlowData): FlowData = {

    val numTopFeatures = (flowData.indexToFeature.size * topFeaturesRatio).toInt

    // Create ChiSqSelector that will select top numTopFeatures features
    val selector = new ChiSqSelector(numTopFeatures)

    // Create ChiSqSelector model (selecting features)
    val transformer = selector.fit(flowData.data)

    // Filter the top numTopFeatures features from each feature vector
    val filteredData = flowData.data.map { lp =>
      LabeledPoint(lp.label, transformer.transform(lp.features))
    }

    // Filter out the features no longer needed
    val filteredFeaturesIndex = flowData.featureToIndex
      .filter { case (feature, index) =>
        transformer.selectedFeatures.contains(index)
      }

    // Map the old index feature to the new one
    val remappedFeaturesIndex = filteredFeaturesIndex
      .toSeq.sortBy(_._2).map(_._1)
      .zipWithIndex.toMap

    FlowData(filteredData, flowData.labelToIndex, remappedFeaturesIndex)

  }

}
