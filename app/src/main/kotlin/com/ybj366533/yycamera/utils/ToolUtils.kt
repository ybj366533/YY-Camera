package com.ybj366533.yycamera.utils

import android.content.Context
import android.os.Environment
import android.util.Log

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Formatter
import java.util.Locale

/**
 * Created by Ivy on 2017/11/10.
 */

object ToolUtils {

    // 录制的中间文件不对外公开
    // 并请根据业务需要，择机删除
    //String pathdir = ToolUtils.getSDPath() + "/VideoRecorderTest";
    val exportVideoPath: String
        get() {
            val pathdir = Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DCIM + File.separator

            val dir = File(pathdir)
            if (!dir.exists()) {
                dir.mkdir()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(Date())
            return "$dir/YY_$timeStamp.mp4"

        }

    //String videoDirPath = Environment.getExternalStorageDirectory()  + "/VideoRecorderTest";
    // 用于比较时间
    // 对当前目录下的所有文件遍历
    //playVideoPath=Environment.getExternalStorageDirectory()  + "/VideoRecorderTest/testa";
    val newestVideo: String
        get() {

            var playVideoPath: String? = null
            try {
                val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
                val videoDirPath = Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DCIM
                val videoDir = File(videoDirPath)
                val files = videoDir.listFiles()
                var newestDate = Calendar.getInstance()
                newestDate.set(2017, 1, 1)

                val count = 0
                if (files != null) {
                    for (videoFile in files) {

                        if (videoFile.exists()) {
                            Log.d("YYdemo", "fileName:" + videoFile.name)

                            val index = videoFile.name.indexOf("SV_")
                            if (index != -1 && index + "yyyyMMdd_HHmmss".length < videoFile.name.length) {
                                val name = videoFile.name.substring(index + 3, index + 18)
                                val date = fileNameFormat.parse(name)

                                val filedC = Calendar.getInstance()
                                filedC.time = date
                                if (filedC.after(newestDate)) {
                                    newestDate = filedC
                                    playVideoPath = videoFile.absolutePath
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            playVideoPath = "/storage/emulated/0/DCIM/1.mp4"

            return playVideoPath

        }

    // 获取年份
    // 获取月份
    // 获取日
    // 分
    // 小时
    // 秒
    //Log.d(TAG, "date:" + date);
    val date: String
        get() {

            val ca = Calendar.getInstance()
            val year = ca.get(Calendar.YEAR)
            val month = ca.get(Calendar.MONTH)
            val day = ca.get(Calendar.DATE)
            val minute = ca.get(Calendar.MINUTE)
            val hour = ca.get(Calendar.HOUR)
            val second = ca.get(Calendar.SECOND)

            return "" + year + (month + 1) + day + hour + minute + second
        }

    // 判断sd卡是否存在
    // 获取外部存储的根目录
    val sdPath: String?
        get() {
            val sdDir: File
            val sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory()
                return sdDir.toString()
            }

            return null
        }

    // 获取一个新文件地址
    fun getNewVideoPath(context: Context): String {
        //String pathdir = ToolUtils.getSDPath() + "/VideoRecorderTest";
        //String pathdir = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
        val pathdir = ToolUtils.getExternalFilesPath(context)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(Date())
        return "$pathdir/SV_$timeStamp.mp4"

    }

    //    public static String getRecWorkFolder(){
    //        String pathdir = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
    //
    //        File dir = new File(pathdir);
    //        if (!dir.exists()) {
    //            dir.mkdir();
    //        }
    //
    //        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
    //        //String path = dir + "/SV_" + timeStamp;
    //        String path = dir + "/SV_rec_folder";
    //
    //
    //        dir = new File(pathdir);
    //        if (!dir.exists()) {
    //            dir.mkdir();
    //        }
    //        return path+ File.separator;
    //    }

    // 录制的中间文件不对外公开
    // 并请根据业务需要，择机删除
    fun getExternalFilesPath(context: Context): String? {
        //String pathdir = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
        var pathdir: String? = null
        val dataDir = context.applicationContext.getExternalFilesDir(null)
        if (dataDir != null) {
            pathdir = dataDir.absolutePath + File.separator
        }

        return pathdir
    }

    // 格式化时间 HH:MM:SS 或者 MM:SS
    fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000

        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600

        val timeFormat = StringBuilder()
        val timeFormatter = Formatter(timeFormat, Locale.getDefault())

        timeFormat.setLength(0)
        return if (hours > 0) {
            timeFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            timeFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    fun DebugLog(tag: String, msg: String) {
        //Log.e(tag, msg);
    }

    fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            //递归删除目录中的子目录下
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


    // 把原文件夹的视频文件等 全部拷贝到目标文件夹（草稿箱）（不支持文件夹递归）
    fun moveFileToDraft(srcDir: String, dstDir: String): Boolean {

        val src = File(srcDir)
        val dst = File(dstDir)
        if (!src.exists() && !src.isDirectory) {
            return false
        }
        if (!dst.exists()) {
            dst.mkdirs()
        }

        for (srcFile in src.listFiles()) {
            if (srcFile.isFile) {
                val dstFile = File(dstDir, srcFile.name)
                srcFile.renameTo(dstFile)
            }

        }

        return true
    }

    //获取某个目录下的文件数量
    fun getFilesCount(url: String): Int {
        val dir = File(url)

        if (dir.exists() && dir.isDirectory) {
            val subFiles = dir.list()
            if (subFiles != null) {
                return subFiles.size
            }
        }

        return 0
    }

    fun getDirsFilesPath(dirUrl: String): Array<Array<String?>>? {
        val dir = File(dirUrl)
        if (dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null && files.size != 0) {
                val filePath = Array<Array<String?>>(files.size) { arrayOfNulls(2) }
                for (i in files.indices) {
                    val f = files[i]
                    if (f.isDirectory) {
                        filePath[i][0] = f.absolutePath
                        filePath[i][1] = getLastLevelFloder(filePath[i][0]!!)

                    }
                }
                return filePath
            }
        }
        return null
    }

    private fun getLastLevelFloder(path: String): String {
        return path.substring(path.lastIndexOf(File.separator), path.length)
    }



}
