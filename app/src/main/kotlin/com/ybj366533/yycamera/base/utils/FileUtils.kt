package com.ybj366533.yycamera.base.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.math.BigDecimal
import java.util.*

/**
 * FileUtils
 * Created by summer on 2018/1/30.
 */
fun File.folderSize(block: ((total: Long, file: File) -> Unit)?): Long {
    var size = 0L
    val file = this
    if (file.exists()) {
        if (file.isDirectory) {
            file.listFiles().forEach {
                size += it.folderSize { total, file ->

                    val subtotal = size + total
                    block?.invoke(subtotal, file)
                }
            }
        } else {
            size += file.length()
            block?.invoke(size, file)
        }
    }
    return size
}

fun File.deleteFolder(delOur: Boolean = false) {
    val file = this
    if (file.exists()) {
        file.listFiles().forEach {
            if (it.isDirectory) {
                it.deleteFolder(true)
            } else {
                it.delete()
            }
        }
        if (delOur) {
            file.delete()
        }
    }
}

fun getFileFormatSize(size: Double): String {

    val kiloByte = size / 1024
    if (kiloByte < 1) {
        return size.toString() + "Byte"
    }

    val megaByte = kiloByte / 1024
    if (megaByte < 1) {
        val result1 = BigDecimal(kiloByte.toString())
        return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "KB"
    }

    val gigaByte = megaByte / 1024
    if (gigaByte < 1) {
        val result2 = BigDecimal(megaByte.toString())
        return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "MB"
    }

    val teraBytes = gigaByte / 1024
    if (teraBytes < 1) {
        val result3 = BigDecimal(gigaByte.toString())
        return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                .toPlainString() + "GB"
    }
    val result4 = BigDecimal(teraBytes)
    return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB"
}



@Suppress("NAME_SHADOWING")
fun Context.newPath(prefix: String = "", suffix: String = "", time: Boolean = true, uuid: Boolean = true) = run {
    val time = if (time) System.currentTimeMillis().toString() else "tamp_cache"
    val uuid = if (uuid) UUID.randomUUID().toString().run { subSequence(length / 2, length) } else ""
    if (hasSdcard) {
        "${externalCacheDir}/${prefix}${time}${uuid}${suffix}"
    } else {
        "${cacheDir}/${prefix}${time}${uuid}${suffix}"
    }
}

val hasSdcard by lazy { Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) }

val sdcardPath by lazy { Environment.getExternalStorageDirectory() }