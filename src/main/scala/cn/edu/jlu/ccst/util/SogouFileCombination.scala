package cn.edu.jlu.ccst.util

import java.io.{File, FileOutputStream}
import java.nio.ByteBuffer
import scala.collection.JavaConversions._

import scala.io.Source

/**
  * @author zhuqi259
  *         搜狗数据集分词后组合文件 形成LabeledFile
  */
object SogouFileCombination {

  val fileSeparator = File.separator
  val lineSeparator = System.getProperty("line.separator")

  val dirNameDict = Map("C000007" -> 0, "C000008" -> 1,
    "C000010" -> 2, "C000013" -> 3, "C000014" -> 4, "C000016" -> 5,
    "C000020" -> 6, "C000022" -> 7, "C000023" -> 8, "C000024" -> 9)

  def main(args: Array[String]) = {
    val len = args.length // len = 3
    assert(len == 3)
    val inputDir = args(0)
    val outputPath = args(1)
    val dir: File = new File(outputPath)
    dir.mkdirs
    val key = args(2)
    val f = new FileOutputStream(outputPath + fileSeparator + key).getChannel
    val set = ScalaFileUtils.getFileMap(inputDir, key).entrySet
    for (entry <- set) {
      val names = entry.getValue
      for (name <- names) {
        val source = inputDir + fileSeparator + entry.getKey + fileSeparator + name
        val lines = Source.fromFile(source, "UTF-8")
        val content = (for (data <- lines.getLines()) yield data.replace("\t", " ")) mkString " "
        val result = content + "\t" + dirNameDict(entry.getKey) + lineSeparator
        f write ByteBuffer.wrap(result.getBytes)
      }
    }
    f.close()
  }
}
