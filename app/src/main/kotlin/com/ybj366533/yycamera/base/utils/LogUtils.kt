package com.ybj366533.yycamera.base.utils

import android.util.Log
import com.ybj366533.yycamera.BuildConfig

/**
 * Log封装
 * Created by summer on 2017/10/30.
 */

enum class LogLevel {
    Verbose, Debug, Info, Warn, Error, WTF, Assert
}

var TAG = "yy"
var space = "->"
var isDebug = BuildConfig.DEBUG

val trace get() = trace(4)

private fun trace(index: Int): String {
    val caller = Throwable().stackTrace[index]

    // 1. 类名 + 方法名
    //val callerClazzName = caller.className
    //val callerClazzName = callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
    //var template = "${callerClazzName}.${caller.methodName}(${caller.fileName}:${caller.lineNumber})"

    // 2. 方法名

    var template = "${caller.methodName}(${caller.fileName}:${caller.lineNumber})"
    return template
}

fun log(msg: String, level: LogLevel = LogLevel.Error, tag: String = TAG) = run {
    if (isDebug) {
        when (level) {
            LogLevel.Verbose -> {
                Log.v(tag, "$trace $space $msg")
            }
            LogLevel.Debug -> {
                Log.d(tag, "$trace $space $msg")
            }
            LogLevel.Info -> {
                Log.i(tag, "$trace $space $msg")
            }
            LogLevel.Warn -> {
                Log.w(tag, "$trace $space $msg")
            }
            LogLevel.Error -> {
                Log.e(tag, "$trace $space $msg")
            }
            LogLevel.WTF -> {
                Log.wtf(tag, "$trace $space $msg")
            }
            else -> {
            }
        }
    }
}

fun log(msg: String, tr: Throwable, level: LogLevel = LogLevel.Error, tag: String = TAG) = run {
    if (isDebug) {
        when (level) {
            LogLevel.Verbose -> {
                Log.v(tag, "$trace $space $msg", tr)
            }
            LogLevel.Debug -> {
                Log.d(tag, "$trace $space $msg", tr)
            }
            LogLevel.Info -> {
                Log.i(tag, "$trace $space $msg", tr)
            }
            LogLevel.Warn -> {
                Log.w(tag, "$trace $space $msg", tr)
            }
            LogLevel.Error -> {
                Log.e(tag, "$trace $space $msg", tr)
            }
            LogLevel.WTF -> {
                Log.wtf(tag, "$trace $space $msg", tr)
            }
            else -> {
            }
        }
    }
}