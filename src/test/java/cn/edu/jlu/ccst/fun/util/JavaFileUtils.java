package cn.edu.jlu.ccst.fun.util;

import java.io.File;

/**
 * @author zhuqi259
 *         Java 版本的FileUtils
 */
public class JavaFileUtils {

    private static final String fileSeparator = File.separator;

    /**
     * 文件重命名
     *
     * @param path    文件目录
     * @param oldName 原来的文件名
     * @param newName 新文件名
     * @return 文件重命名 成功/失败
     */
    public static boolean renameFile(String path, String oldName, String newName) {
        if (!oldName.equals(newName)) {
            File oldFile = new File(path + fileSeparator + oldName);
            File newFile = new File(path + fileSeparator + newName);
            if (!oldFile.exists()) {
                return false;
            }
            if (!newFile.exists()) {
                return oldFile.renameTo(newFile);
            }
        }
        return true;
    }
}
