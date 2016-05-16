package cn.edu.jlu.ccst.randomforests.util

import java.io.{File, FileOutputStream}
import java.nio.ByteBuffer

import scala.collection.JavaConversions._
import scala.io.Source

/**
  * @author zhuqi259
  *         搜狗数据集分词后组合文件 形成LabeledFile
  */
object SogouFileCombination2 {

  val dirNameDict = Map("C000008" -> 0,
    "C000010" -> 1, "C000013" -> 2, "C000014" -> 3, "C000016" -> 4,
    "C000020" -> 5, "C000022" -> 6, "C000023" -> 7, "C000024" -> 8)

  def main(args: Array[String]) = {
    val len = args.length // len = 3
    assert(len == 3)
    val inputDir = args(0)
    val outputPath = args(1)
    val key = args(2)
    val dir: File = new File(outputPath)
    dir.mkdirs
    val set = ScalaFileUtils.getFileMap(inputDir, key).entrySet
    for (entry <- set) {
      val outDir: File = new File(outputPath + ScalaFileUtils.fileSeparator + entry.getKey )
      outDir.mkdirs
      val names = entry.getValue
      for (name <- names) {
        val source = inputDir + ScalaFileUtils.fileSeparator + entry.getKey + ScalaFileUtils.fileSeparator + name
        val lines = Source.fromFile(source, ScalaFileUtils.UTF8)
        val content = (for (data <- lines.getLines()) yield data.replace("\t", " ")) mkString " "
        val f = new FileOutputStream(outputPath + ScalaFileUtils.fileSeparator + entry.getKey + ScalaFileUtils.fileSeparator + name).getChannel
        val result = content + "\t" + dirNameDict(entry.getKey)
        f write ByteBuffer.wrap(result.getBytes)
        f.close()
      }
    }
  }
}
