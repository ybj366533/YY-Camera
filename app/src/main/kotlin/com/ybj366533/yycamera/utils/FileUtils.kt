package com.ybj366533.yycamera.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by Administrator on 2018/3/27 0027.
 */

object FileUtils {
    fun getRealFilePath(context: Context, uri: Uri?): String? {
        if (null == uri) return null
        val scheme = uri.scheme
        var data: String? = null
        if (scheme == null)
            data = uri.path
        else if (ContentResolver.SCHEME_FILE == scheme) {
            data = uri.path
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    if (index > -1) {
                        data = cursor.getString(index)
                    }
                }
                cursor.close()
            }
        }
        return data
    }

    fun getFileName(path: String?): String {
        if (path == null) {
            return ""
        }
        val file = File(path)
        return file.name
    }

    /**
     * 获取文件后缀
     * @param fileName
     */
    fun getFileNameSuffix(fileName: String?): String {
        if (fileName == null) {
            return ""
        }
        val index = fileName.lastIndexOf(".")
        return fileName.substring(index)
    }

    fun getFileNameExcludeSuffix(fileName: String?): String {
        if (fileName == null) {
            return ""
        }
        val index = fileName.lastIndexOf(".")
        return fileName.substring(0, index)
    }

    fun getFilePathExcludeSuffix(filePath: String?): String {
        if (filePath == null) {
            return ""
        }
        val index = filePath.lastIndexOf(".")
        return filePath.substring(0, index)
    }

    /**
     * 获取文件路径(无文件名)
     * @param path 文件全路径(路径+文件名)
     * @return
     */
    fun getFilePathExcludeName(path: String?): String {
        if (path == null) {
            return ""
        }
        val lastIndex = path.lastIndexOf(File.separator)
        return if (lastIndex != -1 && path.length - 1 >= lastIndex + 1) {
            path.substring(0, lastIndex + 1)
        } else ""
    }

    fun delAllFile(path: String): Boolean {
        var flag = false
        val file = File(path)
        if (!file.exists()) {
            return flag
        }
        if (!file.isDirectory) {
            return flag
        }
        val tempList = file.list()
        var temp: File? = null
        for (i in tempList.indices) {
            if (path.endsWith(File.separator)) {
                temp = File(path + tempList[i])
            } else {
                temp = File(path + File.separator + tempList[i])
            }
            if (temp.isFile) {
                temp.delete()
            }
            if (temp.isDirectory) {
                delAllFile(path + "/" + tempList[i])//先删除文件夹里面的文件
                flag = true
            }
        }
        return flag
    }

    fun createDir(path: String) {
        //新建一个File，传入文件夹目录
        val file = File(path)
        //判断文件夹是否存在，如果不存在就创建，否则不创建
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    /**
     * 创建文件
     * @param path
     * @param fileName
     * @return true - file is exits
     */
    fun createFile(path: String, fileName: String): Boolean {
        try {
            createDir(path)
            val jf = File(path + File.separator + fileName)
            if (jf.exists()) {
                return true
            } else {
                jf.createNewFile()
                return false
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * 获取文件内容
     * @param filePath
     * @return
     */
//    fun readFileContent(filePath: String): String {
//        //将json数据变成字符串
//        val stringBuilder = StringBuilder()
//        try {
//            val jsonFile = File(filePath)
//            //通过管理器打开文件并读取
//            val bf = BufferedReader(InputStreamReader(FileInputStream(jsonFile)))
//            var line: String
//            while ((bf.readLine()) != null) {
//                stringBuilder.append(line)
//            }
//            bf.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//
//        return stringBuilder.toString()
//    }


    /**
     * 写文件内容
     * @param filePath
     * @param content
     */
    fun writeFileContent(filePath: String, content: String) {
        try {
            val writer = FileWriter(filePath)
            writer.write(content)
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun deleteFile(path: String) {
        File(path).deleteOnExit()
    }


    fun deleteAllFilesAndFloder(path: String) {
        if (delAllFile(path)) {
            val dir = File(path)
            val subFiles = dir.list()
            for (sub in subFiles) {
                File(sub).delete()
            }
        }

    }

    fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete()
    }


}
