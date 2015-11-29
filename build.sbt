import sbtassembly.MergeStrategy

name := "RandomForests"

version := "1.1"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4.1",
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.14"
)

javacOptions ++= Seq("-source", "1.7", "-encoding", "UTF-8", "-target", "1.7")

mainClass in assembly := Some("akka.tutorials.scala.WordSegmenter")

assemblyMergeStrategy in assembly := {
  case "META-INF\\MANIFEST.MF" => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


    