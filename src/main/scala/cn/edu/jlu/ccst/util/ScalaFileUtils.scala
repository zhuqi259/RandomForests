package cn.edu.jlu.ccst.util

import java.io.IOException
import java.nio.file.{Files, Paths, Path}

import cn.edu.jlu.ccst.randomforests.util.ZQFileVisitor

/**
  * @author zhuqi259
  *         Scala版本的文件工具类
  */
object ScalaFileUtils {

  def htmlDecode(_txt: String): String = {
    var txt = _txt
    txt = txt.replace("&amp;", "&")
    txt = txt.replace("&quot;", "\"")
    txt = txt.replace("&lt;", "<")
    txt = txt.replace("&gt;", ">")
    txt = txt.replace("&nbsp;", " ")
    txt
  }

  @throws(classOf[IOException])
  def getFileMap(fileName: String, key: String) = {
    val fileDir: Path = Paths.get(fileName)
    val visitor: ZQFileVisitor = new ZQFileVisitor(key)
    val start: Long = System.currentTimeMillis
    Files.walkFileTree(fileDir, visitor)
    val end: Long = System.currentTimeMillis
    Console.err.println("遍历文件夹耗时 : " + (end - start) / 1000f + " 秒 ")
    visitor.getMap
  }
}
