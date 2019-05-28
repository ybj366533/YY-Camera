package com.ybj366533.yycamera.widget

import com.ybj366533.videolib.editor.EditCallback

/**
 * describ:
 * author:libq
 * date: 2018/06/13.
 */

abstract class SimpleEditCallback : EditCallback {
    override fun onInitReady() {
        onPrepared()
    }

    override fun onPlayComplete() {
        onPlayEnd()
    }

    override fun onError(i: Int) {

    }

    override fun onProgress(i: Int) {

    }

    override fun onComposeFinish(i: Int) {

    }

    abstract fun onPrepared()

    abstract fun onPlayEnd()

}
