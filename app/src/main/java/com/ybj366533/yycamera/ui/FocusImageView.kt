package com.ybj366533.yycamera.ui


import android.content.Context
import android.content.res.TypedArray
import android.graphics.Point
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import com.ybj366533.yycamera.R


/**
 * @ClassName: FocusImageView
 * @Description:聚焦时显示的ImagView
 * @author LinJ
 * @date 2015-1-4 下午2:55:34
 */
class FocusImageView : ImageView {
    private var mFocusImg = NO_ID
    private var mFocusSucceedImg = NO_ID
    private var mFocusFailedImg = NO_ID
    private var mAnimation: Animation? = null
    private var mHandler: Handler? = null

    constructor(context: Context) : super(context) {
        mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.focusview_show)
        visibility = View.GONE
        mHandler = Handler()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.focusview_show)
        mHandler = Handler()

        val a = context.obtainStyledAttributes(attrs, R.styleable.FocusImageView)
        mFocusImg = a.getResourceId(R.styleable.FocusImageView_focus_focusing_id, NO_ID)
        mFocusSucceedImg = a.getResourceId(R.styleable.FocusImageView_focus_success_id, NO_ID)
        mFocusFailedImg = a.getResourceId(R.styleable.FocusImageView_focus_fail_id, NO_ID)
        a.recycle()

        //聚焦图片不能为空
        if (mFocusImg == NO_ID || mFocusSucceedImg == NO_ID || mFocusFailedImg == NO_ID)
            throw RuntimeException("mFocusImg,mFocusSucceedImg,mFocusFailedImg is null")
    }

    /**
     * 显示聚焦图案
     * @param  point
     */
    fun startFocus(point: Point) {
        if (mFocusImg == NO_ID || mFocusSucceedImg == NO_ID || mFocusFailedImg == NO_ID)
            throw RuntimeException("focus image is null")
        //根据触摸的坐标设置聚焦图案的位置
        val params = layoutParams as FrameLayout.LayoutParams
        params.topMargin = point.y - measuredHeight / 2
        params.leftMargin = point.x - measuredWidth / 2
        layoutParams = params
        //设置控件可见，并开始动画
        visibility = View.VISIBLE
        setImageResource(mFocusImg)
        startAnimation(mAnimation)
        //3秒后隐藏View。在此处设置是由于可能聚焦事件可能不触发。
        mHandler!!.postDelayed({
            // TODO Auto-generated method stub
            visibility = View.GONE
        }, 3500)
    }

    /**
     * 聚焦成功回调
     */
    fun onFocusSuccess() {
        setImageResource(mFocusSucceedImg)
        //移除在startFocus中设置的callback，1秒后隐藏该控件
        mHandler!!.removeCallbacks(null, null)
        mHandler!!.postDelayed({
            // TODO Auto-generated method stub
            visibility = View.GONE
        }, 1000)

    }

    /**
     * 聚焦失败回调
     */
    fun onFocusFailed() {
        setImageResource(mFocusFailedImg)
        //移除在startFocus中设置的callback，1秒后隐藏该控件
        mHandler!!.removeCallbacks(null, null)
        mHandler!!.postDelayed({
            // TODO Auto-generated method stub
            visibility = View.GONE
        }, 1000)
    }

    /**
     * 设置开始聚焦时的图片
     * @param focus
     */
    fun setFocusImg(focus: Int) {
        this.mFocusImg = focus
    }

    /**
     * 设置聚焦成功显示的图片
     * @param focusSucceed
     */
    fun setFocusSucceedImg(focusSucceed: Int) {
        this.mFocusSucceedImg = focusSucceed
    }

    companion object {
        val TAG = "FocusImageView"
        private val NO_ID = -1
    }
}
