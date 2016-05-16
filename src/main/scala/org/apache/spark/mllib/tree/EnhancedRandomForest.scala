package org.apache.spark.mllib.tree

import scala.collection.mutable
import scala.collection.JavaConverters._
import org.apache.spark.Logging
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.configuration.Strategy
import org.apache.spark.mllib.tree.configuration.Algo.{Classification, Regression}
import org.apache.spark.mllib.tree.configuration.QuantileStrategy._
import org.apache.spark.mllib.tree.impl.{BaggedPoint, DecisionTreeMetadata, NodeIdCache, TimeTracker, TreePoint}
import org.apache.spark.mllib.tree.impurity.Impurities
import org.apache.spark.mllib.tree.model._
import org.apache.spark.mllib.tree.model.Bin
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.Utils
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import scala.util.Random

/**
  * Put the random forest model with oob error and variable importance
  *
  * @param forest     RandomForestModel
  * @param importance variable importance, format: index -> importance score
  * @param oobError   out-of-bag error measure
  */
case class EnhancedRandomForestModel(
                                      forest: RandomForestModel,
                                      treeWeights: Option[Array[Double]],
                                      importance: Option[Array[Double]],
                                      oobError: Option[Double]) extends Serializable {

  def enhancedPredict(features: Vector): Double = {
    val votes = mutable.Map.empty[Int, Double]
    forest.trees.view.zip(treeWeights.getOrElse(Array())).foreach { case (tree, weight) =>
      val prediction = tree.predict(features).toInt
      votes(prediction) = votes.getOrElse(prediction, 0.0) + weight
    }
    votes.maxBy(_._2)._1
  }


  override def toString: String = {
    """
Forest Model
%s

treeWeights
%s

Out-of-bag Error: %f

Variable Importance
%s
    """.format(forest.toString,
      treeWeights.getOrElse(Array())
        .zipWithIndex
        .map(x => "Tree Index %d , weight %.4f".format(x._2, x._1)).mkString("\n"),
      oobError.getOrElse("NA"),
      importance
        .getOrElse(Array())
        .zipWithIndex
        .sortBy(_._1)
        .reverse
        .map(x => "Feature Index %d , Importance %.4f".format(x._2, x._1)).mkString("\n"))
  }
}

private[tree] case object EnhancedTreePoint {

  /**
    * Convert one TreePoint into its labeled point for out-of-bag error measure
    *
    * @param treePoint Internal representation of the original data
    * @param bins      Feature bins generated from the sample
    * @return approximate labeled point for out-of-bag error
    */
  def treePointToLabeledPoint(
                               treePoint: TreePoint,
                               bins: EnhancedRandomForest.BinList,
                               strategy: Strategy): LabeledPoint = {

    assert(bins.size == treePoint.binnedFeatures.length,
      "bin list count (%d) is not equal to binned feature count (%d)"
        .format(bins.size, treePoint.binnedFeatures.length))

    val values = treePoint.binnedFeatures.zipWithIndex.map {
      case (binIndex, featureIndex) =>
        if (strategy.categoricalFeaturesInfo.contains(featureIndex)) {
          // category feature
          binIndex // for category feature, bin Index is the feature value
        } else {
          // continuous feature
          assert(binIndex < bins(featureIndex).size,
            "binIndex %d out of bound for %d".format(binIndex, bins(featureIndex).size))
          val bin = bins(featureIndex)(binIndex)
          (bin.highSplit.threshold + bin.lowSplit.threshold) / 2.0
        }
    }
    new LabeledPoint(treePoint.label, Vectors.dense(values))
  }
}

/**
  * 改进随机森林
  *
  */
private class EnhancedRandomForest(
                                    private val strategy: Strategy,
                                    private val numTrees: Int,
                                    featureSubsetStrategy: String,
                                    private val useWeights: Boolean = false,
                                    private val hasOobError: Boolean = false,
                                    private val hasVariableImportance: Boolean = false,
                                    private val seed: Int)
  extends Serializable with Logging {

  strategy.assertValid()
  require(numTrees > 0, s"RandomForest requires numTrees > 0, but was given numTrees = $numTrees.")
  require(RandomForest.supportedFeatureSubsetStrategies.contains(featureSubsetStrategy),
    s"RandomForest given invalid featureSubsetStrategy: $featureSubsetStrategy." +
      s" Supported values: ${RandomForest.supportedFeatureSubsetStrategies.mkString(", ")}.")

  /**
    * Method to train a decision tree model over an RDD
    *
    * @param input Training data: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]]
    * @return a random forest model that can be used for prediction
    */
  def run(input: RDD[LabeledPoint]): EnhancedRandomForestModel = {

    val timer = new TimeTracker()

    timer.start("total")

    timer.start("init")

    val retaggedInput = input.retag(classOf[LabeledPoint])
    val metadata =
      DecisionTreeMetadata.buildMetadata(retaggedInput, strategy, numTrees, featureSubsetStrategy)
    logDebug("algo = " + strategy.algo)
    logDebug("numTrees = " + numTrees)
    logDebug("seed = " + seed)
    logDebug("maxBins = " + metadata.maxBins)
    logDebug("featureSubsetStrategy = " + featureSubsetStrategy)
    logDebug("numFeaturesPerNode = " + metadata.numFeaturesPerNode)

    // Find the splits and the corresponding bins (interval between the splits) using a sample
    // of the input data.
    timer.start("findSplitsBins")
    val (splits, bins) = DecisionTree.findSplitsBins(retaggedInput, metadata)
    timer.stop("findSplitsBins")
    logDebug("numBins: feature: number of bins")
    logDebug(Range(0, metadata.numFeatures).map { featureIndex =>
      s"\t$featureIndex\t${metadata.numBins(featureIndex)}"
    }.mkString("\n"))

    println(s"features count => ${bins.length}")
    //    println("bins dimension =========================")
    //    bins.zipWithIndex.foreach(x => println("index: %d, size: %d".format(x._2, x._1.size)))

    // Bin feature values (TreePoint representation).
    // Cache input RDD for speedup during multiple passes.
    val treeInput = TreePoint.convertToTreeRDD(retaggedInput, bins, metadata)

    val (subsample, withReplacement) = {
      // TODO: Have a stricter check for RF in the strategy
      val isRandomForest = numTrees > 1
      if (isRandomForest) {
        (1.0, true)
      } else {
        (strategy.subsamplingRate, false)
      }
    }

    val baggedInput = BaggedPoint.convertToBaggedRDD(treeInput, subsample, numTrees, withReplacement, seed)
      .persist(StorageLevel.MEMORY_AND_DISK)

    // depth of the decision tree
    val maxDepth = strategy.maxDepth
    require(maxDepth <= 30,
      s"DecisionTree currently only supports maxDepth <= 30, but was given maxDepth = $maxDepth.")

    // Max memory usage for aggregates
    // TODO: Calculate memory usage more precisely.
    val maxMemoryUsage: Long = strategy.maxMemoryInMB * 1024L * 1024L
    logDebug("max memory usage for aggregates = " + maxMemoryUsage + " bytes.")
    val maxMemoryPerNode = {
      val featureSubset: Option[Array[Int]] = if (metadata.subsamplingFeatures) {
        // Find numFeaturesPerNode largest bins to get an upper bound on memory usage.
        Some(metadata.numBins.zipWithIndex.sortBy(-_._1)
          .take(metadata.numFeaturesPerNode).map(_._2))
      } else {
        None
      }
      RandomForest.aggregateSizeForNode(metadata, featureSubset) * 8L
    }
    require(maxMemoryPerNode <= maxMemoryUsage,
      s"RandomForest/DecisionTree given maxMemoryInMB = ${strategy.maxMemoryInMB}," +
        " which is too small for the given features." +
        s"  Minimum value = ${maxMemoryPerNode / (1024L * 1024L)}")

    timer.stop("init")

    /*
     * The main idea here is to perform group-wise training of the decision tree nodes thus
     * reducing the passes over the data from (# nodes) to (# nodes / maxNumberOfNodesPerGroup).
     * Each data sample is handled by a particular node (or it reaches a leaf and is not used
     * in lower levels).
     */

    // Create an RDD of node Id cache.
    // At first, all the rows belong to the root nodes (node Id == 1).
    val nodeIdCache = if (strategy.useNodeIdCache) {
      Some(NodeIdCache.init(
        data = baggedInput,
        numTrees = numTrees,
        checkpointInterval = strategy.checkpointInterval,
        initVal = 1))
    } else {
      None
    }

    // FIFO queue of nodes to train: (treeIndex, node)
    val nodeQueue = new mutable.Queue[(Int, Node)]()

    val rng = new scala.util.Random()
    rng.setSeed(seed)

    // Allocate and queue root nodes.
    val topNodes: Array[Node] = Array.fill[Node](numTrees)(Node.emptyNode(nodeIndex = 1))
    Range(0, numTrees).foreach(treeIndex => nodeQueue.enqueue((treeIndex, topNodes(treeIndex))))

    while (nodeQueue.nonEmpty) {
      // Collect some nodes to split, and choose features for each node (if subsampling).
      // Each group of nodes may come from one or multiple trees, and at multiple levels.
      val (nodesForGroup, treeToNodeToIndexInfo) =
        RandomForest.selectNodesToSplit(nodeQueue, maxMemoryUsage, metadata, rng)
      // Sanity check (should never occur):
      assert(nodesForGroup.nonEmpty,
        s"RandomForest selected empty nodesForGroup.  Error for unknown reason.")

      // Choose node splits, and enqueue new nodes as needed.
      timer.start("findBestSplits")
      DecisionTree.findBestSplits(baggedInput, metadata, topNodes, nodesForGroup,
        treeToNodeToIndexInfo, splits, bins, nodeQueue, timer, nodeIdCache = nodeIdCache)
      timer.stop("findBestSplits")
    }

    timer.stop("total")

    logInfo("Internal timing for DecisionTree:")
    logInfo(s"$timer")

    // Delete any remaining checkpoints used for node Id cache.
    if (nodeIdCache.nonEmpty) {
      nodeIdCache.get.deleteAllCheckpoints()
    }

    val trees = topNodes.map(topNode => new DecisionTreeModel(topNode, strategy.algo))
    val forest = new RandomForestModel(strategy.algo, trees)

    val (oob, varImport) = if (hasVariableImportance) {
      val oobErrorMeasure = EnhancedRandomForest.computeOobError(strategy, baggedInput, bins, forest)
      val varImport = EnhancedRandomForest.computeVariableImportance(strategy, baggedInput, bins, forest, oobErrorMeasure)
      (Some(varImport), Some(oobErrorMeasure))
    } else if (hasOobError) {
      val oobErrorMeasure = EnhancedRandomForest.computeOobError(strategy, baggedInput, bins, forest)
      (None, Some(oobErrorMeasure))
    } else {
      (None, None)
    }

    val weights = if (useWeights) {
      // TODO  计算加权
      //      val n = baggedInput.count().toInt
      //      val ratingMatrix: DM[Int] = DM.zeros[Int](n, numTrees)
      //      val OOBMatrix: DM[Int] = DM.zeros[Int](n, numTrees)

      val actualPairs: Array[(Int, Int, Int)] = baggedInput.flatMap {
        case x =>
          val labeledPoint = EnhancedTreePoint.treePointToLabeledPoint(
            x.datum, bins, strategy)
          // Out-of-Bag数据下标
          val oobIndices: Array[Int] = x.subsampleWeights.zipWithIndex.filter(_._1 == 0).map(_._2)
          for {
            i <- 0 until numTrees
            tree = forest.trees(i)
            prediction = tree.predict(labeledPoint.features)
            score = if (prediction == labeledPoint.label) 1 else 0
            oobScore = if (oobIndices.contains(i)) 0 else 1
          } yield (i, score, oobScore)
      }.collect()

      val allScores = actualPairs.map(_._2).sum + 1
      val weights: Array[Double] = {
        for (i <- 0 until numTrees) yield {
          println(s"i=========> $i")
          val data = actualPairs.filter(_._1 == i)
          val score = data.map(_._2).sum + 1
          val oobi_all = data.map(_._3).sum + 1
          val oobi = data.map(x => x._3 * (if (x._2 == 0) 1 else 0)).sum + 1
          val a = score.toDouble / allScores
          val b = oobi.toDouble / oobi_all
          println(s"\tscore=========> $score")
          println(s"\toobi_all=========> $oobi_all")
          println(s"\toobi=========> $oobi")
          println(s"\ta=========> $a")
          println(s"\tb=========> $b")
          1.01 * (1 - b) * a / (0.01 * (1 - b) + a)
        }
      }.toArray

      //      forest.trees.zipWithIndex.zip(actualPair).foreach{
      //        case ((tree, treeIndex),(dataIndex, oobIndices, labeledPoint))=>
      //          val prediction = tree.predict(labeledPoint.features)
      //          val score = if (prediction == labeledPoint.label) 1 else 0
      //          val oobScore = if (oobIndices.contains(treeIndex)) 0 else 1
      //          ratingMatrix(dataIndex.toInt, treeIndex.toInt) = score
      //          OOBMatrix(dataIndex.toInt, treeIndex.toInt) = oobScore
      //      }

      //      baggedInput.zipWithIndex.foreach {
      //        case (x, dataIndex) =>
      //          val labeledPoint = EnhancedTreePoint.treePointToLabeledPoint(
      //            x.datum, bins, strategy)
      //          val oobIndices: Array[Int] = x.subsampleWeights.zipWithIndex.filter(_._1 == 0).map(_._2)
      //
      //          forest.trees.zipWithIndex.foreach {
      //            case (tree, treeIndex) =>
      //              val prediction = tree.predict(labeledPoint.features)
      //              val score = if (prediction == labeledPoint.label) 1 else 0
      //              val oobScore = if (oobIndices.contains(treeIndex)) 0 else 1
      //              ratingMatrix(dataIndex.toInt, treeIndex.toInt) = score
      //              OOBMatrix(dataIndex.toInt, treeIndex.toInt) = oobScore
      //          }
      //      }


      //      val weights: Array[Double] = {
      //        val allScores = sum(ratingMatrix) + 1
      //        for (i <- 0 until numTrees) yield {
      //          val ratingData: DV[Int] = ratingMatrix(::, i)
      //          val OOBData: DV[Int] = OOBMatrix(::, i)
      //          val score = sum(ratingData) + 1
      //          val oobi_all = sum(OOBData) + 1
      //          val re: DV[Int] = ratingData.map(x => if (x == 0) 1 else 0)
      //          val oobi = OOBData.t * re + 1
      //          val a = score.toDouble / allScores
      //          val b = oobi.toDouble / oobi_all
      //          2 * (1 - b) * a / (1 - b + a)
      //        }
      //      }.toArray

      Some(weights)
    } else {
      Some(Array.fill(numTrees)(1.0))
    }

    baggedInput.unpersist()

    new EnhancedRandomForestModel(forest, weights, oob, varImport)
  }

}

object EnhancedRandomForest extends Serializable with Logging {

  // Alias for bins
  type BinList = Array[Array[Bin]]

  /**
    * Method to train a decision tree model for binary or multiclass classification.
    *
    * @param input                 Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
    *                              Labels should take values {0, 1, ..., numClasses-1}.
    * @param strategy              Parameters for training each tree in the forest.
    * @param numTrees              Number of trees in the random forest.
    * @param featureSubsetStrategy Number of features to consider for splits at each node.
    *                              Supported: "auto", "all", "sqrt", "log2", "onethird".
    *                              If "auto" is set, this parameter is set based on numTrees:
    *                              if numTrees == 1, set to "all";
    *                              if numTrees > 1 (forest) set to "sqrt".
    * @param seed                  Random seed for bootstrapping and choosing feature subsets.
    * @return a random forest model that can be used for prediction
    */
  def trainClassifier(
                       input: RDD[LabeledPoint],
                       strategy: Strategy,
                       numTrees: Int,
                       featureSubsetStrategy: String,
                       useWeights: Boolean,
                       oob: Boolean,
                       varImport: Boolean,
                       seed: Int): EnhancedRandomForestModel = {
    require(strategy.algo == Classification,
      s"RandomForest.trainClassifier given Strategy with invalid algo: ${strategy.algo}")
    val rf = new EnhancedRandomForest(strategy, numTrees, featureSubsetStrategy, useWeights, oob, varImport, seed)

    rf.run(input)
  }

  /**
    * Method to train a decision tree model for binary or multiclass classification.
    *
    * @param input                   Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
    *                                Labels should take values {0, 1, ..., numClasses-1}.
    * @param numClasses              number of classes for classification.
    * @param categoricalFeaturesInfo Map storing arity of categorical features.
    *                                E.g., an entry (n -> k) indicates that feature n is categorical
    *                                with k categories indexed from 0: {0, 1, ..., k-1}.
    * @param numTrees                Number of trees in the random forest.
    * @param featureSubsetStrategy   Number of features to consider for splits at each node.
    *                                Supported: "auto", "all", "sqrt", "log2", "onethird".
    *                                If "auto" is set, this parameter is set based on numTrees:
    *                                if numTrees == 1, set to "all";
    *                                if numTrees > 1 (forest) set to "sqrt".
    * @param impurity                Criterion used for information gain calculation.
    *                                Supported values: "gini" (recommended) or "entropy".
    * @param maxDepth                Maximum depth of the tree.
    *                                E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
    *                                (suggested value: 4)
    * @param maxBins                 maximum number of bins used for splitting features
    *                                (suggested value: 100)
    * @param seed                    Random seed for bootstrapping and choosing feature subsets.
    * @return a random forest model  that can be used for prediction
    */
  def trainClassifier(
                       input: RDD[LabeledPoint],
                       numClasses: Int,
                       categoricalFeaturesInfo: scala.collection.immutable.Map[Int, Int],
                       numTrees: Int,
                       featureSubsetStrategy: String,
                       impurity: String,
                       maxDepth: Int,
                       maxBins: Int,
                       useWeights: Boolean,
                       oob: Boolean,
                       varImport: Boolean,
                       seed: Int = Utils.random.nextInt()): EnhancedRandomForestModel = {
    val impurityType = Impurities.fromString(impurity)
    val strategy = new Strategy(Classification, impurityType, maxDepth,
      numClasses, maxBins, Sort, categoricalFeaturesInfo)
    trainClassifier(input, strategy, numTrees, featureSubsetStrategy, useWeights, oob, varImport, seed)
  }

  /**
    * Java-friendly API for [[org.apache.spark.mllib.tree.RandomForest$#trainClassifier]]
    */
  def trainClassifier(
                       input: JavaRDD[LabeledPoint],
                       numClasses: Int,
                       categoricalFeaturesInfo: java.util.Map[java.lang.Integer, java.lang.Integer],
                       numTrees: Int,
                       featureSubsetStrategy: String,
                       impurity: String,
                       maxDepth: Int,
                       maxBins: Int,
                       useWeights: Boolean,
                       oob: Boolean,
                       varImport: Boolean,
                       seed: Int): EnhancedRandomForestModel = {
    trainClassifier(input.rdd, numClasses,
      categoricalFeaturesInfo.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins, useWeights, oob, varImport, seed)
  }

  /**
    * Method to train a decision tree model for regression.
    *
    * @param input                 Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
    *                              Labels are real numbers.
    * @param strategy              Parameters for training each tree in the forest.
    * @param numTrees              Number of trees in the random forest.
    * @param featureSubsetStrategy Number of features to consider for splits at each node.
    *                              Supported: "auto", "all", "sqrt", "log2", "onethird".
    *                              If "auto" is set, this parameter is set based on numTrees:
    *                              if numTrees == 1, set to "all";
    *                              if numTrees > 1 (forest) set to "onethird".
    * @param seed                  Random seed for bootstrapping and choosing feature subsets.
    * @return a random forest model that can be used for prediction
    */
  def trainRegressor(
                      input: RDD[LabeledPoint],
                      strategy: Strategy,
                      numTrees: Int,
                      featureSubsetStrategy: String,
                      useWeights: Boolean,
                      oob: Boolean,
                      varImport: Boolean,
                      seed: Int): EnhancedRandomForestModel = {
    require(strategy.algo == Regression,
      s"RandomForest.trainRegressor given Strategy with invalid algo: ${strategy.algo}")
    val rf = new EnhancedRandomForest(strategy, numTrees, featureSubsetStrategy, useWeights, oob, varImport, seed)
    rf.run(input)
  }

  /**
    * Method to train a decision tree model for regression.
    *
    * @param input                   Training dataset: RDD of [[org.apache.spark.mllib.regression.LabeledPoint]].
    *                                Labels are real numbers.
    * @param categoricalFeaturesInfo Map storing arity of categorical features.
    *                                E.g., an entry (n -> k) indicates that feature n is categorical
    *                                with k categories indexed from 0: {0, 1, ..., k-1}.
    * @param numTrees                Number of trees in the random forest.
    * @param featureSubsetStrategy   Number of features to consider for splits at each node.
    *                                Supported: "auto", "all", "sqrt", "log2", "onethird".
    *                                If "auto" is set, this parameter is set based on numTrees:
    *                                if numTrees == 1, set to "all";
    *                                if numTrees > 1 (forest) set to "onethird".
    * @param impurity                Criterion used for information gain calculation.
    *                                Supported values: "variance".
    * @param maxDepth                Maximum depth of the tree.
    *                                E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.
    *                                (suggested value: 4)
    * @param maxBins                 maximum number of bins used for splitting features
    *                                (suggested value: 100)
    * @param seed                    Random seed for bootstrapping and choosing feature subsets.
    * @return a random forest model that can be used for prediction
    */
  def trainRegressor(
                      input: RDD[LabeledPoint],
                      categoricalFeaturesInfo: Map[Int, Int],
                      numTrees: Int,
                      featureSubsetStrategy: String,
                      impurity: String,
                      maxDepth: Int,
                      maxBins: Int,
                      useWeights: Boolean,
                      oob: Boolean,
                      varImport: Boolean,
                      seed: Int = Utils.random.nextInt()): EnhancedRandomForestModel = {
    val impurityType = Impurities.fromString(impurity)
    val strategy = new Strategy(Regression, impurityType, maxDepth,
      0, maxBins, Sort, categoricalFeaturesInfo)
    trainRegressor(input, strategy, numTrees, featureSubsetStrategy, useWeights, oob, varImport, seed)
  }

  /**
    * Java-friendly API for [[org.apache.spark.mllib.tree.RandomForest$#trainRegressor]]
    */
  def trainRegressor(
                      input: JavaRDD[LabeledPoint],
                      categoricalFeaturesInfo: java.util.Map[java.lang.Integer, java.lang.Integer],
                      numTrees: Int,
                      featureSubsetStrategy: String,
                      impurity: String,
                      maxDepth: Int,
                      maxBins: Int,
                      useWeights: Boolean,
                      oob: Boolean,
                      varImport: Boolean,
                      seed: Int): EnhancedRandomForestModel = {
    trainRegressor(input.rdd,
      categoricalFeaturesInfo.asInstanceOf[java.util.Map[Int, Int]].asScala.toMap,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins, useWeights, oob, varImport, seed)
  }

  /**
    * Method for compute the variable importance
    *
    * @param strategy    Parameter object for single tree
    * @param baggedInput Bootstrapping sample for training forest
    * @param bins        Bin list for each feature
    * @param forest      Random forest model
    * @param oobError    Out of bag error for the forest
    * @return a list of importance score with the same feature indices, the higher, the more important
    */
  private def computeVariableImportance(
                                         strategy: Strategy,
                                         baggedInput: RDD[BaggedPoint[TreePoint]],
                                         bins: EnhancedRandomForest.BinList,
                                         forest: RandomForestModel,
                                         oobError: Double): Array[Double] = {

    bins.indices.par.map(featureIndex => {

      val binCount = if (strategy.categoricalFeaturesInfo.contains(featureIndex)) {
        // category feature
        strategy.categoricalFeaturesInfo(featureIndex)
      } else {
        // continuous feature
        bins(featureIndex).size
      }
      val shuffleBinFeature = Random.shuffle((0 until binCount).toList) // 每个元素对饮shuffle后的数据
      val shuffleOneFeatureBaggedInput = baggedInput.map(x => {
          val currentFeatureBinIndex = x.datum.binnedFeatures(featureIndex)
          x.datum.binnedFeatures(featureIndex) = shuffleBinFeature(currentFeatureBinIndex)
          x
        })
      computeOobError(strategy, shuffleOneFeatureBaggedInput, bins, forest) - oobError
    }).toArray

  }

  /**
    * Method for compute the out-of-bag error
    *
    * @param strategy    Parameter object for single tree
    * @param baggedInput Bootstrapping sample for training forest
    * @param bins        Bin list for each feature
    * @param forest      Random forest model
    * @return out-of-bag error
    */
  private def computeOobError(
                               strategy: Strategy,
                               baggedInput: RDD[BaggedPoint[TreePoint]],
                               bins: EnhancedRandomForest.BinList,
                               forest: RandomForestModel): Double = {

    val actualPredictPair = baggedInput
      .map(x => {
        val labeledPoint = EnhancedTreePoint.treePointToLabeledPoint(
          x.datum, bins, strategy)
        (x.subsampleWeights.zipWithIndex.filter(_._1 == 0)
          .map(_._2).map(index => forest.trees(index)), labeledPoint)
      })
      .filter(_._1.size > 0) // 过滤掉无树的森林
      .map({
      case (oobTrees, labeledPoint) =>
        val subForest = new RandomForestModel(strategy.algo, oobTrees)
        (labeledPoint.label, subForest.predict(labeledPoint.features))
    })

    val totalCount = actualPredictPair.count
    assert(totalCount > 0,
      s"OOB error measure: data number is zeror")
    strategy.algo match {
      case Regression => actualPredictPair.map(x => Math.pow(x._1 -
        x._2, 2)).reduce(_ + _) / totalCount
      case Classification => actualPredictPair.filter(x => x._1 !=
        x._2).count.toDouble / totalCount
    }
  }
}