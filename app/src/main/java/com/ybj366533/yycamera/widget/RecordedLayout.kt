package com.ybj366533.yycamera.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import com.ybj366533.yycamera.R

import java.util.ArrayList

/**
 * Created by Summer on 18/4/13.
 */

class RecordedLayout : View {

    private var measuredWidth = -1
    private var measuredHeight = -1
    private var paintCursor: Paint? = null
    private var dp6: Int = 0
    private var dp10: Int = 0
    private var rectF: RectF? = null
    private var radius: Float = 0.toFloat()
    private var paintProgress: Paint? = null
    private var paintProgressBg: Paint? = null
    /**
     * 当前进度 以角度为单位
     */
    private var mCursor: Float = 0.toFloat()
    private var maxTime: Int = 0
    private var onGestureListener: OnGestureListener? = null

    private var progress: Float = 0.toFloat()
    private var effectInfoList: List<GTVideoEffectInfo> = ArrayList<GTVideoEffectInfo>()
    //是否设置类型
    private var isInfoType = false

    private var downX: Float = 0.toFloat()
    private var isScroll = false
    private var scrollChange = false


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

        dp6 = resources.getDimension(R.dimen.dp6).toInt()
        dp10 = resources.getDimension(R.dimen.dp10).toInt()
        paintCursor = Paint()
        paintCursor!!.isAntiAlias = true
        paintCursor!!.strokeWidth = dp6.toFloat()


        paintProgress = Paint()
        paintProgress!!.isAntiAlias = true
        paintProgress!!.strokeWidth = dp6.toFloat()
        paintProgress!!.style = Paint.Style.STROKE

        paintProgressBg = Paint()
        paintProgressBg!!.isAntiAlias = true
        paintProgressBg!!.strokeWidth = dp6.toFloat()
        paintProgressBg!!.style = Paint.Style.STROKE

    }

    interface OnGestureListener {

        fun onClick(mCursor: Float)

        fun onEnd()

        fun onStop(endType: GTVideoEffectInfo)
    }

    fun setOnGestureListener(onGestureListener: OnGestureListener) {
        this.onGestureListener = onGestureListener
    }

    override fun setOnTouchListener(l: View.OnTouchListener) {
        super.setOnTouchListener(l)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                //Log.e("downX", "#" + downX);
                if (downX > rectF!!.left - dp10 * 2 && downX < rectF!!.right + dp10 * 2) {
                    isScroll = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                //Log.e("moveX", "#" + event.getX());
                val scrollX = event.x - downX
                if (isScroll) {
                    mCursor = mCursor + scrollX
                    if (mCursor < 0) {
                        mCursor = 0f
                        rectF!!.right = 0f
                        rectF!!.left = dp10.toFloat()
                    } else if (mCursor > measuredWidth - dp10) {
                        rectF!!.left = measuredWidth.toFloat()
                        rectF!!.right = (measuredWidth - dp10).toFloat()
                        mCursor = measuredWidth.toFloat()
                    } else {
                        rectF!!.right = rectF!!.right + scrollX
                        rectF!!.left = rectF!!.left + scrollX
                    }
                    scrollChange = true
                    invalidate()
                }
                downX = event.x
                if (onGestureListener != null) {
                    onGestureListener!!.onClick(mCursor / measuredWidth)
                }
            }
            MotionEvent.ACTION_UP -> {
                downX = 0f
                isScroll = false
                if (scrollChange && onGestureListener != null) {
                    onGestureListener!!.onClick(mCursor / measuredWidth)
                }
                scrollChange = false
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return true
    }


    /**
     * 设置最大进度时间
     *
     * @param max
     */
    fun setMaxTime(max: Int) {
        this.maxTime = max
    }

    /**
     * 设置进度
     */
    fun setProgress(progress: Float) {
        //Log.e("typePosition", "typePosition@" + typePosition);
        this.progress = progress
        //选择绘制颜色样式
        this.mCursor = measuredWidth * progress

        if (mCursor < 0) {
            rectF!!.left = 0f
            rectF!!.right = dp10.toFloat()
        } else if (mCursor > measuredWidth - dp10) {
            rectF!!.left = (measuredWidth - dp10).toFloat()
            rectF!!.right = measuredWidth.toFloat()
        } else {
            rectF!!.left = mCursor
            rectF!!.right = dp10 + mCursor
        }
        invalidate()

        if (progress >= 1) {
            if (onGestureListener != null) {
                if (isInfoType) {
                    onGestureListener!!.onStop(effectInfoList[effectInfoList.size - 1])
                } else {
                    onGestureListener!!.onEnd()
                }
            }
        }

    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (measuredWidth == -1) {
            measuredWidth = getMeasuredWidth()
            measuredHeight = getMeasuredHeight()

            radius = dp10.toFloat()

            rectF = RectF()
            rectF!!.left = 0f
            rectF!!.top = measuredHeight.toFloat()
            rectF!!.right = dp10.toFloat()
            rectF!!.bottom = 0f
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)


    }

    override fun onDraw(canvas: Canvas) {
        //绘制进度条背景
        paintProgressBg!!.color = getTypeColor(null)
        canvas.drawLine(0f, (measuredHeight / 2).toFloat(), measuredWidth.toFloat(), (measuredHeight / 2).toFloat(), paintProgressBg!!)
        //根据 EffectInfoList 绘制已设置类型区间
        for (x in effectInfoList.indices) {
            paintProgress!!.color = getTypeColor(effectInfoList[x].getEffectType())
            if (effectInfoList[x].getEndTime() > 0) {
                canvas.drawLine(getTypeInfoSplit(effectInfoList[x].getStartTime()), (measuredHeight / 2).toFloat(),
                        getTypeInfoSplit(effectInfoList[x].getEndTime()), (measuredHeight / 2).toFloat(), paintProgress!!)
            } else {
                if (isInfoType)
                    canvas.drawLine(getTypeInfoSplit(effectInfoList[x].getStartTime()), (measuredHeight / 2).toFloat(), progress * measuredWidth, (measuredHeight / 2).toFloat(), paintProgress!!)
            }
        }
        //绘制游标
        paintCursor!!.color = Color.YELLOW
        //canvas.drawLine(mCursor, measuredHeight - dp6, mCursor, dp6, paintCursor);
        canvas.drawCircle(mCursor, (measuredHeight / 2).toFloat(), radius, paintCursor!!)

    }

    /**
     * 获取对应绘制颜色
     *
     * @param type
     * @return
     */
    private fun getTypeColor(type: IGTVVideoEditor.EffectType?): Int {

        return if (type === IGTVVideoEditor.EffectType.EFFECT_NO) {
            resources.getColor(R.color.color1)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_HEBAI) {
            resources.getColor(R.color.color2)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_DOUDONG) {
            resources.getColor(R.color.color3)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_LINGHUNCHUQIAO) {
            resources.getColor(R.color.color4)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_YISHIJIE) {
            resources.getColor(R.color.color5)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_JIANRUI) {
            resources.getColor(R.color.color6)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_BODONG) {
            resources.getColor(R.color.color7)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_TOUSHI) {
            resources.getColor(R.color.color8)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_SHANGSHUO) {
            resources.getColor(R.color.color9)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_SUMIAO) {
            resources.getColor(R.color.color10)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_LINGYI) {
            resources.getColor(R.color.color11)
        } else if (type === IGTVVideoEditor.EffectType.EFFECT_YINXIANPAI) {
            resources.getColor(R.color.color12)
        } else {
            resources.getColor(R.color.color1)
        }

    }

    /**
     * 设置类型列表
     *
     * @param mList
     */
    fun setEffectInfoList(mList: List<GTVideoEffectInfo>, isInfoType: Boolean) {
        this.effectInfoList = mList
        this.isInfoType = isInfoType
        //invalidate();
    }


    /**
     * 格式化绘制点
     *
     * @param mTime
     * @return
     */
    private fun getTypeInfoSplit(mTime: Float): Float {
        return measuredWidth * (mTime / maxTime)
    }
}