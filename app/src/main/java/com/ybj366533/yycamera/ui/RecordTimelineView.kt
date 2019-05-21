/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.ybj366533.yycamera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.ybj366533.yycamera.R

class RecordTimelineView : View {
    private var maxDuration: Int = 0
    private var duration: Int = 0
    //private int minDuration;
    //private CopyOnWriteArrayList<DrawInfo> clipDurationList = new CopyOnWriteArrayList<>();
    //private DrawInfo currentClipDuration = new DrawInfo();
    private val paint = Paint()
    private var durationColor: Int = 0
    //private int selectColor;
    //private int offsetColor;
    private var bgColor: Int = 0

    //private boolean isSelected = false;
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
        paint.isAntiAlias = true

        this.durationColor = resources.getColor(R.color.kklive_record_fill_progress)
        this.bgColor = resources.getColor(R.color.kklive_timeline_backgound_color)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bgColor != 0) {
            canvas.drawColor(bgColor)
        }

        paint.color = durationColor

        canvas.drawRect(0f, 0f, duration / maxDuration.toFloat() * width, height.toFloat(), paint)

    }

    fun setMaxDuration(maxDuration: Int) {
        this.maxDuration = maxDuration
        this.duration = 0
    }


    fun setDuration(duration: Int) {

        if (duration > maxDuration || duration < 0) {
            return
        }

        this.duration = duration
        invalidate()
    }


}
