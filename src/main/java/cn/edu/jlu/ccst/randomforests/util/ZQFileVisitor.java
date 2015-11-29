package cn.edu.jlu.ccst.randomforests.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


/**
 * @author zhuqi259
 * ZQFileVisitor<br>
 *     用于遍历文件[输入预处理]
 */
public class ZQFileVisitor extends SimpleFileVisitor<Path> {
    private String basicKey = "";
    private String _key = "";
    private List<String> list_file = new ArrayList<>();
    private Map<String, List<String>> fileMap = new HashMap<>();

    public ZQFileVisitor(String key) {
        basicKey = key;
        _key = key;
    }


    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exec)
            throws IOException {
        // System.out.println("Just visited " + dir);
        String currentKey = dir.getFileName().toString();
        if (basicKey.equals(currentKey)) {
            // 父文件夹已访问完毕
            fileMap.put(_key, list_file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
        // System.out.println("About to visit " + dir);
        if (!basicKey.equals(_key)) {
            //保存Map
            fileMap.put(_key, list_file);
        }
        //浏览下一个目录
        _key = dir.getFileName().toString();
        list_file = new ArrayList<>();
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {
        // 访问文件后调用
        if (attrs.isRegularFile())
            list_file.add(file.getFileName().toString());    //插入一个List<String>有别的用。
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
            throws IOException {
        // 文件不可访问时调用
        return FileVisitResult.CONTINUE;
    }

    public Map<String, List<String>> getMap() {
        return fileMap;
    }
    
}