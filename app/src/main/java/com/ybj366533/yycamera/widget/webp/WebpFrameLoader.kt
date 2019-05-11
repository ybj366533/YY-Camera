package com.ybj366533.yycamera.widget.webp

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock

import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Util

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.ArrayList
import java.util.UUID

/**
 * Created by Summer on 17/7/19.
 */

class WebpFrameLoader internal constructor(private val bitmapPool: BitmapPool, internal val requestManager: RequestManager, private val gifDecoder: GifDecoder, handler: Handler?, private var requestBuilder: RequestBuilder<Bitmap>?, transformation: Transformation<Bitmap>, firstFrame: Bitmap) {
    private val handler: Handler
    private val callbacks: MutableList<FrameCallback>
    private var isRunning: Boolean = false
    private var isLoadPending: Boolean = false
    private var startFromFirstFrame: Boolean = false
    private var current: DelayTarget? = null
    private var isCleared: Boolean = false
    private var next: DelayTarget? = null
    internal var firstFrame: Bitmap? = null
        private set
    internal var frameTransformation: Transformation<Bitmap>? = null
        private set

    internal val width: Int
        get() = this.currentFrame!!.width

    internal val height: Int
        get() = this.currentFrame!!.height

    internal val size: Int
        get() = this.gifDecoder.byteSize + this.frameSize

    internal val currentIndex: Int
        get() = if (this.current != null) this.current!!.index else -1

    private val frameSize: Int
        get() = Util.getBitmapByteSize(this.currentFrame!!.width, this.currentFrame!!.height, this.currentFrame!!.config)

    internal val buffer: ByteBuffer
        get() = this.gifDecoder.data.asReadOnlyBuffer()

    internal val frameCount: Int
        get() = this.gifDecoder.frameCount

    internal val loopCount: Int
        get() = this.gifDecoder.totalIterationCount

    internal val currentFrame: Bitmap?
        get() = if (this.current != null) this.current!!.resource else this.firstFrame

    constructor(glide: Glide, gifDecoder: GifDecoder, width: Int, height: Int, transformation: Transformation<Bitmap>, firstFrame: Bitmap) : this(glide.bitmapPool, Glide.with(glide.context), gifDecoder, null as Handler?, getRequestBuilder(Glide.with(glide.context), width, height), transformation, firstFrame) {}

    init {
        var handler = handler
        this.callbacks = ArrayList()
        this.isRunning = false
        this.isLoadPending = false
        this.startFromFirstFrame = false
        if (handler == null) {
            handler = Handler(Looper.getMainLooper(), FrameLoaderCallback())
        }
        this.handler = handler
        this.setFrameTransformation(transformation, firstFrame)
    }

    internal fun setFrameTransformation(transformation: Transformation<Bitmap>, firstFrame: Bitmap) {
        this.frameTransformation = Preconditions.checkNotNull(transformation) as Transformation<*>
        this.firstFrame = Preconditions.checkNotNull(firstFrame) as Bitmap
        this.requestBuilder = this.requestBuilder!!.apply(RequestOptions().transform(transformation))
    }

    internal fun subscribe(frameCallback: FrameCallback) {
        if (this.isCleared) {
            throw IllegalStateException("Cannot subscribe to a cleared frame loader")
        } else {
            val start = this.callbacks.isEmpty()
            if (this.callbacks.contains(frameCallback)) {
                throw IllegalStateException("Cannot subscribe twice in a row")
            } else {
                this.callbacks.add(frameCallback)
                if (start) {
                    this.start()
                }

            }
        }
    }

    internal fun unsubscribe(frameCallback: FrameCallback) {
        this.callbacks.remove(frameCallback)
        if (this.callbacks.isEmpty()) {
            this.stop()
        }

    }

    private fun start() {
        if (!this.isRunning) {
            this.isRunning = true
            this.isCleared = false
            this.loadNextFrame()
        }
    }

    private fun stop() {
        this.isRunning = false
    }

    internal fun clear() {
        this.callbacks.clear()
        this.recycleFirstFrame()
        this.stop()
        if (this.current != null) {
            this.requestManager.clear(this.current)
            this.current = null
        }

        if (this.next != null) {
            this.requestManager.clear(this.next)
            this.next = null
        }

        this.gifDecoder.clear()
        this.isCleared = true
    }

    private fun loadNextFrame() {
        if (this.isRunning && !this.isLoadPending) {
            if (this.startFromFirstFrame) {
                this.gifDecoder.resetFrameIndex()
                this.startFromFirstFrame = false
            }

            this.isLoadPending = true
            val delay = this.gifDecoder.nextDelay
            val targetTime = SystemClock.uptimeMillis() + delay.toLong()
            this.gifDecoder.advance()
            this.next = DelayTarget(this.handler, this.gifDecoder.currentFrameIndex, targetTime)
            this.requestBuilder!!.clone().apply(RequestOptions.signatureOf(FrameSignature())).load(this.gifDecoder).into(this.next!!)
        }
    }

    private fun recycleFirstFrame() {
        if (this.firstFrame != null) {
            this.bitmapPool.put(this.firstFrame)
            this.firstFrame = null
        }

    }

    internal fun setNextStartFromFirstFrame() {
        Preconditions.checkArgument(!this.isRunning, "Can\'t restart a running animation")
        this.startFromFirstFrame = true
    }

    internal fun onFrameReady(delayTarget: DelayTarget) {
        if (this.isCleared) {
            this.handler.obtainMessage(2, delayTarget).sendToTarget()
        } else {
            if (delayTarget.resource != null) {
                this.recycleFirstFrame()
                val previous = this.current
                this.current = delayTarget

                for (i in this.callbacks.indices.reversed()) {
                    this.callbacks[i].onFrameReady()
                }

                if (previous != null) {
                    this.handler.obtainMessage(2, previous).sendToTarget()
                }
            }

            this.isLoadPending = false
            this.loadNextFrame()
        }
    }

    private fun getRequestBuilder(requestManager: RequestManager, width: Int, height: Int): RequestBuilder<Bitmap> {
        return requestManager.asBitmap().apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE).skipMemoryCache(true).override(width, height))
    }

    internal class FrameSignature(private val uuid: UUID) : Key {

        constructor() : this(UUID.randomUUID()) {}

        override fun equals(o: Any?): Boolean {
            if (o is FrameSignature) {
                val other = o as FrameSignature?
                return other!!.uuid == this.uuid
            } else {
                return false
            }
        }

        override fun hashCode(): Int {
            return this.uuid.hashCode()
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            throw UnsupportedOperationException("Not implemented")
        }
    }

    internal class DelayTarget(private val handler: Handler, val index: Int, private val targetTime: Long) : SimpleTarget<Bitmap>() {
        var resource: Bitmap? = null
            private set

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
            this.resource = resource
            val msg = this.handler.obtainMessage(1, this)
            this.handler.sendMessageAtTime(msg, this.targetTime)
        }
    }

    private inner class FrameLoaderCallback internal constructor() : Handler.Callback {

        override fun handleMessage(msg: Message): Boolean {
            val target: DelayTarget
            if (msg.what == 1) {
                target = msg.obj as DelayTarget
                this@WebpFrameLoader.onFrameReady(target)
                return true
            } else {
                if (msg.what == 2) {
                    target = msg.obj as DelayTarget
                    this@WebpFrameLoader.requestManager.clear(target)
                }

                return false
            }
        }

        companion object {
            val MSG_DELAY = 1
            val MSG_CLEAR = 2
        }
    }

    interface FrameCallback {
        fun onFrameReady()
    }
}
