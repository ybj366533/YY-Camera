package com.ybj366533.yycamera.base.utils

/**
 * 计时器
 * Created by summer on 2018/1/17.
 */
class Timer(val delay: Long = 1000) {

    var isPause = true
        private set
    private var pastTime = 0L
    private var beginTime = 0L

    private var callCount = -1L

    val currentTime get() = System.currentTimeMillis()

    val total
        get() =
            if (isPause)
                pastTime
            else
                currentTime - beginTime + pastTime

    val canCall get() = run {
        val count = total / delay
        val status = callCount < count
        if (status) {
            callCount = count
        }
        status
    }

    fun start() {
        isPause = false
        beginTime = currentTime
    }

    fun pause() {
        pastTime = total
        isPause = true
    }

    fun stop() {
        isPause = true
        pastTime = 0
        beginTime = 0
        callCount = -1L
    }
}