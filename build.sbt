import sbtassembly.MergeStrategy

name := "RandomForests"

version := "1.1"

scalaVersion := "2.10.6"

libraryDependencies ++= {
  val sparkVersion = "1.5.2"
  val hadoopVersion = "2.6.2"
  Seq(
    "org.apache.spark" % "spark-core_2.10" % sparkVersion % "provided" exclude("org.apache.hadoop", "hadoop-client"),
    "org.apache.spark" % "spark-mllib_2.10" % sparkVersion % "provided",
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "provided",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.4.1",
    "com.typesafe.akka" % "akka-actor_2.10" % "2.3.14"
  )
}

javacOptions ++= Seq("-source", "1.7", "-encoding", "UTF-8", "-target", "1.7")

mainClass in assembly := Some("cn.edu.jlu.ccst.randomforests.solution.SimpleSolution")

assemblyMergeStrategy in assembly := {
  case "META-INF\\MANIFEST.MF" => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


    