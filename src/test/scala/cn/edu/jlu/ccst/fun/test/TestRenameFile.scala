package cn.edu.jlu.ccst.fun.test

import cn.edu.jlu.ccst.fun.util.JavaFileUtils
import cn.edu.jlu.ccst.randomforests.util.ScalaFileUtils
import scala.collection.JavaConversions._

/**
  * @author zhuqi259
  *         文件批量重命名测试
  */
object TestRenameFile {
  val word_num_wordPattern = """([^0-9]+)([0-9]+)(.*)""".r
  // 用于匹配 => 第001讲：Scala开发环境搭建和HelloWorld解析.mp4

  def doWork(path: String, key: String) = {
    val set = ScalaFileUtils.getFileMap(path, key).entrySet
    for (entry <- set) {
      val names = entry.getValue
      for (name <- names) {
        name match {
          case word_num_wordPattern(start, num, end) =>
            val newNum = if (num.length == 1) "00" + num else if (num.length == 2) "0" + num else num
            val newName = start + newNum + end
            JavaFileUtils.renameFile(path, name, newName)
          case _ =>
        }
      }
    }
  }

  def main(args: Array[String]) {
    assert(args.length == 2)
    doWork(args(0), args(1))
  }
}
