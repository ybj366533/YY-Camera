package com.ybj366533.yycamera.utils

import android.content.Context

import java.io.File
import java.io.FileOutputStream


class AssetsHandler {

    /**
     * 从assets目录中复制整个文件夹内容
     * @param  context  Context 使用CopyFiles类的Activity
     * @param  oldPath  String  原文件路径  如：/aa
     * @param  newPath  String  复制后路径  如：xx:/bb/cc
     */
    fun copyFilesFassets(context: Context, oldPath: String, newPath: String, suffix: String?) {
        try {
            val fileNames = context.assets.list(oldPath)//获取assets目录下的所有文件及目录名
            if (fileNames!!.isNotEmpty()) {
                //如果是目录
                val file = File(newPath)
                file.mkdirs()//如果文件夹不存在，则递归
                for (fileName in fileNames) {
                    copyFilesFassets(context, "$oldPath/$fileName", "$newPath/$fileName", suffix)
                }
            } else {
                //如果是文件
                //如果有后缀，则只copy指定后缀的数据
                if (suffix != null && suffix.isNotEmpty() && !oldPath.contains(suffix) && !suffix.equals("*", ignoreCase = true)) {
                    return
                }

                val mis = context.assets.open(oldPath)
                val fos = FileOutputStream(File(newPath))
                val buffer = ByteArray(1024)
                var byteCount = 0
                while (( mis.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount)//将读取的输入流写入到输出流
                }
                fos.flush()//刷新缓冲区
                mis.close()
                fos.close()
            }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    companion object {


        private var mInstance: AssetsHandler? = null

        val instance: AssetsHandler
            get() {

                if (mInstance == null) {
                    synchronized(AssetsHandler::class.java) {
                        if (mInstance == null) {
                            mInstance = AssetsHandler()
                        }
                    }
                }

                return mInstance!!
            }
    }
}
