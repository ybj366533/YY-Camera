package com.ybj366533.yycamera.widget

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class CameraGLSurfaceView : GLSurfaceView {

    private var onCameraGLViewListener: OnCameraGLViewListener? = null


    internal var downX: Float = 0.toFloat()
    internal var downY: Float = 0.toFloat()

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                //判断是否支持对焦模式
                onCameraGLViewListener!!.onClick(downX, downY)
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        return true
    }

    interface OnCameraGLViewListener {
        fun onLongClick()

        fun onClick(downX: Float, downY: Float)
    }

    fun setOnCameraGLViewListener(onCameraGLViewListener: OnCameraGLViewListener) {
        this.onCameraGLViewListener = onCameraGLViewListener
    }
}
