package com.infrastructure.utils;

import java.io.File;
import java.io.IOException;

/**
 * Created by cyc20 on 2018/3/19.
 */

public class FileUtils {

    /*
    * 删除某个文件夹的内容*/
    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files==null){
            throw new IllegalArgumentException("not a directory"+dir);
        }
        for (File file : files) {
            if (file.isDirectory()){
                deleteContents(file);
            }if (!file.delete()){
                throw new IOException("failed to delete file"+file);
            }
        }
    }
}
