package com.ybj366533.yycamera.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.ybj366533.yycamera.R

import java.util.ArrayList

/**
 * Created by Summer on 18/4/13.
 */

class RecordedButton : View {
    /**
     * 控件初始化设置
     */
    private var aMeasuredWidth = -1
    private var colorWhite: Int = 0
    private var colorRead: Int = 0
    private var radius: Float = 0.toFloat()
    private val zoom = 0.8f//初始化缩放比例
    private var dp5: Int = 0
    /**
     * 控件绘制设置
     */
    private var paintProgress: Paint? = null
    private var paintText: Paint? = null
    private var paint: Paint? = null
    private var girthPro: Float = 0.toFloat()
    private var oval: RectF? = null
    private var rectF: RectF? = null
    private var rectText: Rect? = null
    private var paintSplit: Paint? = null
    private var max: Int = 0
    private val animTime = 200
    private val textReset = "重拍"

    /**
     * 控件状态设置
     */
    private var isOpenMode = true
    private var splitList: MutableList<Float> = ArrayList()
    private var buttonAnim: ValueAnimator? = null
    private var progress: Float = 0.toFloat()
    private var onGestureListener: OnGestureListener? = null
    private var rawX = -1f
    private var rawY = -1f
    /**
     * button 状态
     */
    private var initStatusType = -1//当前状态


    private var cleanResponse: Boolean = false//清除所有响应
    private val firstX: Float = 0.toFloat()
    private val firstY: Float = 0.toFloat()

    private var flag: Boolean = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {

        dp5 = resources.getDimension(R.dimen.dp6).toInt()
        colorWhite = resources.getColor(R.color.color1)
        colorRead = resources.getColor(R.color.color2)

        paint = Paint()
        paint!!.isAntiAlias = true
        paint!!.color = colorRead
        paint!!.style = Paint.Style.FILL


        paintProgress = Paint()
        paintProgress!!.isAntiAlias = true
        paintProgress!!.color = colorRead
        paintProgress!!.strokeWidth = dp5.toFloat()
        paintProgress!!.style = Paint.Style.STROKE

        paintSplit = Paint()
        paintSplit!!.isAntiAlias = true
        paintSplit!!.color = colorWhite
        paintSplit!!.strokeWidth = dp5.toFloat()
        paintSplit!!.style = Paint.Style.STROKE

        paintText = Paint()
        paintText!!.strokeWidth = 3f
        paintText!!.textSize = 40f
        paintText!!.color = colorWhite
        paintText!!.textAlign = Paint.Align.LEFT
        //设置绘制字体
        rectText = Rect()
        paintText!!.getTextBounds(textReset, 0, textReset.length, rectText)

        //设置绘制暂停按钮大小
        rectF = RectF()

        //设置绘制外圆大小
        oval = RectF()


    }

    interface OnGestureListener {

        fun onClickStart()

        fun onClickPause()

        fun onClickReset()

        fun onProgressOver()
    }

    fun setOnGestureListener(onGestureListener: OnGestureListener) {
        this.onGestureListener = onGestureListener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN ->
                //if (isResponseLongTouch) myHandler.sendEmptyMessageDelayed(0, animTime);
                //startAnim(0, 1 - zoom);
                onButtonClick()
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP,

            MotionEvent.ACTION_CANCEL -> cleanResponse = false
        }//startMoveAnim();
        return true
    }

    private fun startMoveAnim() {

        val slideX = rawX - x
        val slideY = rawY - y

        val rX = x
        val rY = y

        val va = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(50)
        va.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            x = rX + slideX * value
            y = rY + slideY * value
        }

        va.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (Math.abs(slideX) > Math.abs(slideY)) {
                    jitterAnim(slideX / 5, true)
                } else {
                    jitterAnim(slideY / 5, false)
                }
            }
        })
        va.start()
    }

    private fun jitterAnim(slide: Float, isX: Boolean) {

        val va = ValueAnimator.ofFloat(slide, 0.0f).setDuration(100)
        va.addUpdateListener { animation ->
            var value = animation.animatedValue as Float
            if (flag) {
                value = -value
            }
            if (isX) {
                x = rawX + value
            } else {
                y = rawY + value
            }
            flag = !flag
        }
        va.start()
    }

    fun closeButton() {
        if (isOpenMode) {
            isOpenMode = false
            startAnim(1 - zoom, 0f)
        }
    }

    private fun startAnim(start: Float, end: Float) {

        if (buttonAnim == null || !buttonAnim!!.isRunning) {
            buttonAnim = ValueAnimator.ofFloat(start, end).setDuration(animTime.toLong())
            buttonAnim!!.addUpdateListener { animation ->
                var value = animation.animatedValue as Float

                radius = aMeasuredWidth * (zoom + value) / 2

                if (girthPro > 0 && girthPro < 360) {
                    //隐藏开始按钮
                    radius = 0f
                }
                value = 1f - zoom - value

                oval!!.left = aMeasuredWidth * value / 2 + dp5 / 2
                oval!!.top = aMeasuredWidth * value / 2 + dp5 / 2
                oval!!.right = aMeasuredWidth * (1 - value / 2) - dp5 / 2
                oval!!.bottom = aMeasuredWidth * (1 - value / 2) - dp5 / 2

                invalidate()
            }
            buttonAnim!!.start()

            //            final ScaleAnimation scaleAnimation =new ScaleAnimation(1.0f, 1.3f, 1.0f,1.3f,
            //                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            //                   scaleAnimation.setRepeatCount(-1);
            //                   scaleAnimation.setDuration(500);
            //                   this.startAnimation(scaleAnimation);
        }
    }

    fun setMax(max: Int) {
        this.max = max
    }

    /**
     * 设置进度
     */
    fun setProgress(progress: Float) {

        this.progress = progress
        val ratio = progress / max
        girthPro = 365 * ratio


        if (ratio >= 1) {
            initStatusType = BUTTON_STATUS_RESET
            if (onGestureListener != null) onGestureListener!!.onProgressOver()
        }
        invalidate()
    }

    /**
     * 设置段点
     */
    private fun setSplit() {
        splitList.add(girthPro)
        invalidate()
    }

    /**
     * 设置断点列表
     * @param splitList
     */
    private fun setSplit(splitList: MutableList<Float>) {
        this.splitList = splitList
        invalidate()
    }

    /**
     * 删除最后一个段点
     */
    private fun deleteSplit() {
        if (splitList.size > 0) {
            splitList.removeAt(splitList.size - 1)
            invalidate()
        }
    }

    /**
     * 清除断点
     */
    private fun cleanSplit() {
        if (splitList.size > 0) {
            splitList.clear()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (aMeasuredWidth == -1) {
            aMeasuredWidth = measuredWidth

            radius = aMeasuredWidth * zoom / 2 - dp5

            oval!!.left = aMeasuredWidth * 0.1f + dp5 / 2
            oval!!.top = aMeasuredWidth * 0.1f + dp5 / 2
            oval!!.right = aMeasuredWidth * (1 - 0.1f) - dp5 / 2
            oval!!.bottom = aMeasuredWidth * (1 - 0.1f) - dp5 / 2

            rectF!!.left = (aMeasuredWidth / 2 - dp5 * 3).toFloat()
            rectF!!.top = (aMeasuredWidth / 2 + dp5 * 3).toFloat()
            rectF!!.right = (aMeasuredWidth / 2 + dp5 * 3).toFloat()
            rectF!!.bottom = (aMeasuredWidth / 2 - dp5 * 3).toFloat()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (rawX == -1f) {
            rawX = x
            rawY = y
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        //绘制状态控件
        when (initStatusType) {
            BUTTON_STATUS_START -> {
                //绘制开始按钮
                paint!!.color = colorRead
                canvas.drawRoundRect(rectF!!, dp5.toFloat(), dp5.toFloat(), paint!!)
            }
            BUTTON_STATUS_PAUSE -> {
                //绘制暂停按钮
                paintProgress!!.color = colorRead
                canvas.drawLines(floatArrayOf(// 绘制一组线 每四数字(两个点的坐标)确定一条线
                        (aMeasuredWidth / 2 + dp5).toFloat(), (aMeasuredWidth / 2 + dp5 * 2).toFloat(), (aMeasuredWidth / 2 + dp5).toFloat(), (aMeasuredWidth / 2 - dp5 * 2).toFloat(), (aMeasuredWidth / 2 - dp5).toFloat(), (aMeasuredWidth / 2 + dp5 * 2).toFloat(), (aMeasuredWidth / 2 - dp5).toFloat(), (aMeasuredWidth / 2 - dp5 * 2).toFloat()), paintProgress!!)

            }
            BUTTON_STATUS_RESET -> {
                //绘制重拍按钮
                paint!!.color = colorWhite
                paintText!!.color = colorRead
                canvas.drawCircle((aMeasuredWidth / 2).toFloat(), (aMeasuredWidth / 2).toFloat(), radius, paint!!)
                canvas.drawText(textReset, (aMeasuredWidth / 2 - rectText!!.width() / 2).toFloat(), (aMeasuredWidth / 2 + dp5).toFloat(), paintText!!)
            }
            else -> {
                paint!!.color = colorRead
                canvas.drawRoundRect(rectF!!, dp5.toFloat(), dp5.toFloat(), paint!!)
            }
        }

        //绘制圆圈，进度条背景
        paintProgress!!.color = colorWhite
        canvas.drawArc(oval!!, 270f, 360f, false, paintProgress!!)
        //绘制进度
        paintProgress!!.color = colorRead
        canvas.drawArc(oval!!, 270f, girthPro, false, paintProgress!!)

        //绘制段点
        for (x in splitList.indices) {
            canvas.drawArc(oval!!, 270 + splitList[x], 1f, false, paintSplit!!)
        }

        //绘制删除模式的段落
        //        if (isDeleteMode && splitList.size() > 0) {
        //            float split = splitList.get(splitList.size() - 1);
        //            canvas.drawArc(oval, 270 + split, girthPro - split, false, paintDelete);
        //        }
    }

    /**
     * 按钮点击处理
     */
    private fun onButtonClick() {
        if (initStatusType == BUTTON_STATUS_START) {
            onButtonStart()
        } else if (initStatusType == BUTTON_STATUS_PAUSE) {
            onButtonPause()
        } else if (initStatusType == BUTTON_STATUS_RESET) {
            onButtonReset()
        } else {
            onButtonStart()
        }
    }

    /**
     * 开始
     */
    private fun onButtonStart() {
        initStatusType = BUTTON_STATUS_PAUSE
        invalidate()
        if (onGestureListener != null) onGestureListener!!.onClickStart()
    }

    /**
     * 暂停
     */
    private fun onButtonPause() {
        initStatusType = BUTTON_STATUS_START
        setSplit()
        invalidate()
        if (onGestureListener != null) onGestureListener!!.onClickPause()
    }

    /**
     * 重置
     */
    private fun onButtonReset() {
        initStatusType = BUTTON_STATUS_START
        cleanSplit()
        if (onGestureListener != null) onGestureListener!!.onClickReset()

    }

    fun ProgressOver() {
        initStatusType = BUTTON_STATUS_RESET
        girthPro = 365f
        invalidate()
        if (onGestureListener != null) onGestureListener!!.onProgressOver()
    }

    /**
     * 设置按钮当前状态
     * @param buttonStatus
     * BUTTON_STATUS_START(开始)
     * BUTTON_STATUS_PAUSE(暂停)
     * BUTTON_STATUS_RESET(重置)
     */
    fun setButtonStatus(buttonStatus: Int) {
        //  设置按钮状态
        initStatusType = buttonStatus
        invalidate()
    }

    /**
     * 设置暂停状态
     */
    fun pause() {
        onButtonPause()
    }

    companion object {
        val BUTTON_STATUS_START = 1001 //开始
        val BUTTON_STATUS_PAUSE = 1002 //暂停
        val BUTTON_STATUS_RESET = 1003 //重置
        val BUTTON_STATUS_OVER = 1004  //完毕
    }
}