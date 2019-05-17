package com.ybj366533.yycamera.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.ybj366533.yycamera.R

/**
 * Created by zhaoshuang on 17/8/22.
 */

class ThumbnailView : View {

    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mPaint: Paint? = null
    private var rectF: RectF? = null
    private var rectF2: RectF? = null
    private var rectWidth: Int = 0
    private val bitmap: Bitmap? = null
    private var onScrollBorderListener: OnScrollBorderListener? = null
    private var minPx: Int = 0

    val leftInterval: Float
        get() = rectF!!.left

    val rightInterval: Float
        get() = rectF2!!.right

    private var downX: Float = 0.toFloat()
    private var isScroll: Boolean = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {

        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        val dp5 = resources.getDimension(R.dimen.dp5).toInt()
        mPaint!!.strokeWidth = dp5.toFloat()

        //bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.video_thumbnail);

        rectWidth = resources.getDimension(R.dimen.dp10).toInt()
        minPx = resources.getDimension(R.dimen.dp50).toInt()
    }

    fun setMinInterval(minPx: Int) {
        var minPx = minPx
        if (mWidth > 0 && minPx > mWidth) {
            minPx = mWidth
        }
        this.minPx = minPx

    }

    interface OnScrollBorderListener {
        fun OnScrollBorder(start: Float, end: Float)

        fun onScrollStateChange()
    }

    fun setOnScrollBorderListener(listener: OnScrollBorderListener) {
        this.onScrollBorderListener = listener
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (mWidth == 0) {
            mWidth = width
            mHeight = height

            rectF = RectF()
            rectF!!.left = 0f
            rectF!!.top = 0f
            rectF!!.right = rectWidth.toFloat()
            rectF!!.bottom = mHeight.toFloat()

            rectF2 = RectF()
            rectF2!!.left = (minPx + rectWidth).toFloat()
            rectF2!!.top = 0f
            rectF2!!.right = (rectWidth * 2 + minPx).toFloat()
            rectF2!!.bottom = mHeight.toFloat()
            //            Log.e("yyyy","rectF.left:"+rectF.left+"rectF.right:"+rectF.right
            //                    +"##rectF2.left:"+rectF2.left+"rectF2.right:"+rectF2.right);
        }
    }
    //    private boolean scrollLeft;
    //    private boolean scrollRight;

    override fun onTouchEvent(event: MotionEvent): Boolean {

        move(event)
        return isScroll
    }

    private fun move(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                Log.e("MotionEvent", "MotionEvent.ACTION_DOWN")

                if (downX > rectF!!.left && downX < rectF2!!.right) {
                    isScroll = true
                } else {
                    Log.e("downX", "downX = " + downX + "  left*" + rectF!!.left + "  right*" + rectF2!!.right)
                    rectF!!.left = downX - minPx / 2 - rectWidth
                    rectF!!.right = downX - minPx / 2
                    rectF2!!.left = downX + minPx / 2
                    rectF2!!.right = downX + minPx / 2 + rectWidth

                    //滑动到左边最大值
                    if (rectF!!.left < 0) {
                        rectF!!.left = 0f
                        rectF!!.right = rectWidth.toFloat()
                        rectF2!!.left = (minPx + rectWidth).toFloat()
                        rectF2!!.right = (rectWidth * 2 + minPx).toFloat()

                    }

                    //滑动至右边最大值
                    if (rectF2!!.right > mWidth) {
                        rectF2!!.right = mWidth.toFloat()
                        rectF2!!.left = (mWidth - rectWidth).toFloat()
                        rectF!!.right = (mWidth - minPx - rectWidth).toFloat()
                        rectF!!.left = (mWidth - minPx - rectWidth * 2).toFloat()
                    }
                    isScroll = true
                    invalidate()

                    if (onScrollBorderListener != null) {
                        onScrollBorderListener!!.OnScrollBorder(rectF!!.left, rectF2!!.right)
                    }

                }
            }
            MotionEvent.ACTION_MOVE -> {
                Log.e("MotionEvent", "MotionEvent.ACTION_MOVE")
                val moveX = event.x

                val scrollX = moveX - downX
                if (isScroll) {
                    rectF!!.left = rectF!!.left + scrollX
                    rectF!!.right = rectF!!.right + scrollX

                    rectF2!!.left = rectF2!!.left + scrollX
                    rectF2!!.right = rectF2!!.right + scrollX
                    //滑动到左边最大值
                    if (rectF!!.left < 0) {
                        rectF!!.left = 0f
                        rectF!!.right = rectWidth.toFloat()
                        rectF2!!.left = (minPx + rectWidth).toFloat()
                        rectF2!!.right = (rectWidth * 2 + minPx).toFloat()

                    }

                    //滑动至右边最大值
                    if (rectF2!!.right > mWidth) {
                        rectF2!!.right = mWidth.toFloat()
                        rectF2!!.left = (mWidth - rectWidth).toFloat()
                        rectF!!.right = (mWidth - minPx - rectWidth).toFloat()
                        rectF!!.left = (mWidth - minPx - rectWidth * 2).toFloat()
                    }
                    invalidate()

                    if (onScrollBorderListener != null) {
                        onScrollBorderListener!!.OnScrollBorder(rectF!!.left, rectF2!!.right)
                    }
                }


                downX = moveX
            }
            MotionEvent.ACTION_CANCEL -> Log.e("MotionEvent", "MotionEvent.ACTION_CANCEL")
            MotionEvent.ACTION_UP -> {
                Log.e("MotionEvent", "MotionEvent.ACTION_UP")
                downX = 0f
                isScroll = false
                //invalidate();
                if (onScrollBorderListener != null) {
                    onScrollBorderListener!!.onScrollStateChange()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        //TODO 动图托框 绘制
        mPaint!!.color = Color.RED


        canvas.drawLine(rectF!!.left, rectF!!.top, rectF!!.left, rectF!!.bottom, mPaint!!)

        canvas.drawLine(rectF2!!.right, rectF2!!.top, rectF2!!.right, rectF2!!.bottom, mPaint!!)


        canvas.drawLine(rectF!!.left, 0f, rectF2!!.right, 0f, mPaint!!)
        canvas.drawLine(rectF!!.left, mHeight.toFloat(), rectF2!!.right, mHeight.toFloat(), mPaint!!)

        // mPaint.setColor(Color.parseColor("#99313133"));

        //        RectF rectF3 = new RectF();
        //        rectF3.left = 0;
        //        rectF3.top = 0;
        //        rectF3.right = rectF.left;
        //        rectF3.bottom = mHeight;
        //        canvas.drawRect(rectF3, mPaint);
        //
        //        RectF rectF4 = new RectF();
        //        rectF4.left = rectF2.right;
        //        rectF4.top = 0;
        //        rectF4.right = mWidth;
        //        rectF4.bottom = mHeight;
        //        canvas.drawRect(rectF4, mPaint);
    }
}