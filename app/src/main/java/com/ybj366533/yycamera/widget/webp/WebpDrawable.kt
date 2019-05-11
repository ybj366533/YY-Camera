package com.ybj366533.yycamera.widget.webp

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.support.annotation.VisibleForTesting
import android.view.Gravity

import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.util.Preconditions

import java.nio.ByteBuffer

/**
 * Created by Summer on 17/7/16.
 */
class WebpDrawable internal constructor(state: GifState) : Drawable(), WebpFrameLoader.FrameCallback, Animatable {
    private val state: GifState
    private var isRunning: Boolean = false
    private var isStarted: Boolean = false
    internal var isRecycled: Boolean = false
        private set
    private var isVisible: Boolean = false
    private var loopCount: Int = 0
    private var maxLoopCount: Int = 0
    private var applyGravity: Boolean = false
    private var paint: Paint? = null
    private var destRect: Rect? = null

    val size: Int
        get() = this.state.frameLoader.size

    val firstFrame: Bitmap
        get() = this.state.frameLoader.firstFrame

    val frameTransformation: Transformation<Bitmap>
        get() = this.state.frameLoader.frameTransformation

    val buffer: ByteBuffer
        get() = this.state.frameLoader.buffer

    val frameCount: Int
        get() = this.state.frameLoader.frameCount

    val frameIndex: Int
        get() = this.state.frameLoader.currentIndex

    constructor(context: Context, gifDecoder: GifDecoder, bitmapPool: BitmapPool, frameTransformation: Transformation<Bitmap>, targetFrameWidth: Int, targetFrameHeight: Int, firstFrame: Bitmap) : this(GifState(bitmapPool, WebpFrameLoader(Glide.get(context), gifDecoder, targetFrameWidth, targetFrameHeight, frameTransformation, firstFrame))) {}

    init {
        this.isVisible = true
        this.maxLoopCount = -1
        this.state = Preconditions.checkNotNull(state)
    }

    @VisibleForTesting
    internal constructor(frameLoader: WebpFrameLoader, bitmapPool: BitmapPool, paint: Paint) : this(GifState(bitmapPool, frameLoader)) {
        this.paint = paint
    }

    fun setFrameTransformation(frameTransformation: Transformation<Bitmap>, firstFrame: Bitmap) {
        this.state.frameLoader.setFrameTransformation(frameTransformation, firstFrame)
    }

    private fun resetLoopCount() {
        this.loopCount = 0
    }

    fun startFromFirstFrame() {
        Preconditions.checkArgument(!this.isRunning, "You cannot restart a currently running animation.")
        this.state.frameLoader.setNextStartFromFirstFrame()
        this.start()
    }

    override fun start() {
        this.isStarted = true
        this.resetLoopCount()
        if (this.isVisible) {
            this.startRunning()
        }

    }

    override fun stop() {
        this.isStarted = false
        this.stopRunning()
    }

    private fun startRunning() {
        Preconditions.checkArgument(!this.isRecycled, "You cannot start a recycled Drawable. Ensure thatyou clear any references to the Drawable when clearing the corresponding request.")
        if (this.state.frameLoader.frameCount == 1) {
            this.invalidateSelf()
        } else if (!this.isRunning) {
            this.isRunning = true
            this.state.frameLoader.subscribe(this)
            this.invalidateSelf()
        }

    }

    private fun stopRunning() {
        this.isRunning = false
        this.state.frameLoader.unsubscribe(this)
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        Preconditions.checkArgument(!this.isRecycled, "Cannot change the visibility of a recycled resource. Ensure that you unset the Drawable from your View before changing the View\'s visibility.")
        this.isVisible = visible
        if (!visible) {
            this.stopRunning()
        } else if (this.isStarted) {
            this.startRunning()
        }

        return super.setVisible(visible, restart)
    }

    override fun getIntrinsicWidth(): Int {
        return this.state.frameLoader.width
    }

    override fun getIntrinsicHeight(): Int {
        return this.state.frameLoader.height
    }

    override fun isRunning(): Boolean {
        return this.isRunning
    }

    internal fun setIsRunning(isRunning: Boolean) {
        this.isRunning = isRunning
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        this.applyGravity = true
    }

    override fun draw(canvas: Canvas) {
        if (!this.isRecycled) {
            if (this.applyGravity) {
                Gravity.apply(GifState.GRAVITY, this.intrinsicWidth, this.intrinsicHeight, this.bounds, this.getDestRect())
                this.applyGravity = false
            }

            val currentFrame = this.state.frameLoader.currentFrame
            canvas.drawBitmap(currentFrame, null as Rect?, this.getDestRect(), this.getPaint())
        }
    }

    override fun setAlpha(i: Int) {
        this.getPaint().alpha = i
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        this.getPaint().colorFilter = colorFilter
    }

    private fun getDestRect(): Rect {
        if (this.destRect == null) {
            this.destRect = Rect()
        }

        return this.destRect
    }

    private fun getPaint(): Paint {
        if (this.paint == null) {
            this.paint = Paint(2)
        }

        return this.paint
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun onFrameReady() {
        if (this.callback == null) {
            this.stop()
            this.invalidateSelf()
        } else {
            this.invalidateSelf()
            if (this.frameIndex == this.frameCount - 1) {
                ++this.loopCount
            }

            if (this.maxLoopCount != LOOP_FOREVER && this.loopCount >= this.maxLoopCount) {
                this.stop()
            }

        }
    }

    override fun getConstantState(): Drawable.ConstantState? {
        return this.state
    }

    fun recycle() {
        this.isRecycled = true
        this.state.frameLoader.clear()
    }

    fun setLoopCount(loopCount: Int) {
        if (loopCount <= 0 && loopCount != LOOP_FOREVER && loopCount != LOOP_INTRINSIC) {
            throw IllegalArgumentException("Loop count must be greater than 0, or equal to LOOP_FOREVER, or equal to LOOP_INTRINSIC")
        } else {
            if (loopCount == LOOP_INTRINSIC) {
                val intrinsicCount = this.state.frameLoader.loopCount
                this.maxLoopCount = if (intrinsicCount == LOOP_INTRINSIC) LOOP_FOREVER else intrinsicCount
            } else {
                this.maxLoopCount = loopCount
            }

        }
    }

    internal class GifState(val bitmapPool: BitmapPool, val frameLoader: WebpFrameLoader) : Drawable.ConstantState() {

        override fun newDrawable(res: Resources?): Drawable {
            return this.newDrawable()
        }

        override fun newDrawable(): Drawable {
            return WebpDrawable(this)
        }

        override fun getChangingConfigurations(): Int {
            return 0
        }

        companion object {
            val GRAVITY = Gravity.FILL
        }
    }

    companion object {
        val LOOP_FOREVER = -1
        val LOOP_INTRINSIC = 0
    }
}
