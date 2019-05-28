package com.ybj366533.yycamera.base.utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock.sleep
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

/**
 * 线程管理
 * Created by summer on 2017/10/31.
 */

/**
 * 主线程运行
 */
fun runOnUi(runnable: () -> Unit) = if (isMainThread) runnable() else mainHandler.post(runnable)

fun runOnUi(delayMillis: Long, runnable: () -> Unit) = mainHandler.postDelayed(runnable, delayMillis)

/**
 * 子线程运行
 */
fun runOnBackground(runnable: () -> Unit) = cachedPool.execute(runnable)

fun runOnBackground(delayMillis: Long, runnable: () -> Unit) = cachedPool.execute {
    sleep(delayMillis)
    runnable()
}

/**
 * 仅核心线程池
 * 核心线程池不会回收
 */
val fixedPool by lazy { Executors.newFixedThreadPool(coreSize) as ThreadPoolExecutor }

/**
 * 仅非核心线程池MAX 60秒后回收
 */
val cachedPool by lazy { Executors.newCachedThreadPool() as ThreadPoolExecutor; }

/**
 * 核心线程池 + 非核心线程池MAX 立即回收
 */
val scheduledPool by lazy { Executors.newScheduledThreadPool(coreSize) as ThreadPoolExecutor }

/**
 * 单线程池 顺序执行
 */
val singlePool by lazy { Executors.newSingleThreadExecutor() as ThreadPoolExecutor }

/**
 * 主线程
 */
val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/**
 * 主线程判断
 */
val isMainThread = Looper.myLooper() == Looper.getMainLooper()

val coreSize = run {
    var cores = cpuCoresNumber
    if (cores > 5) cores / 2 else cores
}