package cn.edu.jlu.ccst.randomforests.novel.sparx.model

import cn.edu.jlu.ccst.randomforests.novel.sparx.mllib.ClassMetrics


case class PredictedRecipe(id: Int, ingredients: Seq[String], predictions: Seq[PredictionData])

case class PredictionData(model: String, prediction: String, metrics: ClassMetrics)
