package cn.edu.jlu.ccst.randomforests.novel.sparx

/**
  * Some basic paths configuration
  */
case class Configuration(args: Array[String]) {

  // TODO: Refactor and improve

  val argsMap = args.map(_.split("=")).map(x => (x(0), x(1))).toMap

  val inputDataType = argsMap.getOrElse("app.input.file.type", "cuisines")

  val inputTrainingData = argsMap.getOrElse("app.input.file.training", "data/cuisines/train.json")

  val inputTestingData = argsMap.getOrElse("app.input.file.test", "data/cuisines/test.json")

  val outputPredictionsPath = argsMap.getOrElse("app.output.file.predictions", "/tmp/predictions")

  private val modelRootPath = argsMap.getOrElse("app.model.dir", "working_model")

  val dataPath = s"$modelRootPath/flow_data"

  val recipesPath = s"$modelRootPath/recipes"

  private val trainingDataRoot = s"$modelRootPath/training/"

  val randomForestPath = trainingDataRoot + "random_forest.model"

  val enhancedRandomForestPath = trainingDataRoot + "enhanced_random_forest.model"

}
