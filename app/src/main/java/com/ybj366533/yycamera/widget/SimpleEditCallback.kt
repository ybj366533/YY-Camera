package com.ybj366533.yycamera.widget

import com.gtv.cloud.editor.EditCallback

/**
 * describ:
 * author:libq
 * date: 2018/06/13.
 */

abstract class SimpleEditCallback : EditCallback {
    fun onInitReady() {
        onPrepared()
    }

    fun onPlayComplete() {
        onPlayEnd()
    }

    fun onError(i: Int) {

    }

    fun onProgress(i: Int) {

    }

    fun onComposeFinish(i: Int) {

    }

    abstract fun onPrepared()

    abstract fun onPlayEnd()

}
