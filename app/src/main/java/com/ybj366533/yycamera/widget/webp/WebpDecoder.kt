package com.ybj366533.yycamera.widget.webp

import android.graphics.Bitmap

import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeader
import com.facebook.animated.webp.WebPFrame
import com.facebook.animated.webp.WebPImage

import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Created by Summer on 17/7/19.
 */

class WebpDecoder(private val mProvider: GifDecoder.BitmapProvider, private var mWebPImage: WebPImage?, sampleSize: Int) : GifDecoder {
    private var mFramePointer: Int = 0
    private val mFrameDurations: IntArray
    private val downsampledWidth: Int
    private val downsampledHeight: Int


    init {
        mFrameDurations = mWebPImage.getFrameDurations()
        downsampledWidth = mWebPImage.getWidth() / sampleSize
        downsampledHeight = mWebPImage.getHeight() / sampleSize
    }

    override fun getWidth(): Int {
        return mWebPImage!!.width
    }

    override fun getHeight(): Int {
        return mWebPImage!!.height
    }

    override fun getData(): ByteBuffer? {
        return null
    }

    override fun getStatus(): Int {
        return 0
    }

    override fun advance() {
        mFramePointer = (mFramePointer + 1) % mWebPImage!!.frameCount
    }

    override fun getDelay(n: Int): Int {
        var delay = -1
        if (n >= 0 && n < mFrameDurations.size) {
            delay = mFrameDurations[n]
        }
        return delay
    }

    override fun getNextDelay(): Int {
        return if (mFrameDurations.size == 0 || mFramePointer < 0) {
            0
        } else getDelay(mFramePointer)

    }

    override fun getFrameCount(): Int {
        return mWebPImage!!.frameCount
    }

    override fun getCurrentFrameIndex(): Int {
        return mFramePointer
    }

    override fun resetFrameIndex() {
        mFramePointer = -1
    }

    override fun getLoopCount(): Int {
        return mWebPImage!!.loopCount
    }

    override fun getNetscapeLoopCount(): Int {
        return mWebPImage!!.loopCount
    }

    override fun getTotalIterationCount(): Int {
        return if (mWebPImage!!.loopCount == 0) {
            GifDecoder.TOTAL_ITERATION_COUNT_FOREVER
        } else mWebPImage!!.frameCount + 1
    }

    override fun getByteSize(): Int {
        return mWebPImage!!.sizeInBytes
    }

    override fun getNextFrame(): Bitmap {
        val result = mProvider.obtain(downsampledWidth, downsampledHeight, Bitmap.Config.ARGB_8888)
        val frame = mWebPImage!!.getFrame(currentFrameIndex)
        frame.renderFrame(downsampledWidth, downsampledHeight, result)
        return result
    }

    override fun read(inputStream: InputStream, i: Int): Int {
        return 0
    }

    override fun clear() {
        mWebPImage!!.dispose()
        mWebPImage = null
    }

    override fun setData(gifHeader: GifHeader, bytes: ByteArray) {

    }

    override fun setData(gifHeader: GifHeader, byteBuffer: ByteBuffer) {

    }

    override fun setData(gifHeader: GifHeader, byteBuffer: ByteBuffer, i: Int) {

    }

    override fun read(bytes: ByteArray): Int {
        return 0
    }


}
