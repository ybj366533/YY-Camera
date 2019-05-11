package com.ybj366533.yycamera.widget

import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.RecyclerView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

abstract class OnRecyclerItemClickListener(private val recyclerView: RecyclerView) : RecyclerView.OnItemTouchListener {
    private val mGestureDetector: GestureDetectorCompat

    init {
        mGestureDetector = GestureDetectorCompat(recyclerView.context,
                ItemTouchHelperGestureListener())
    }


    abstract fun onItemClick(viewHolder: RecyclerView.ViewHolder)

    abstract fun onItemLongClick(viewHolder: RecyclerView.ViewHolder)

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        mGestureDetector.onTouchEvent(e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

    }


    private inner class ItemTouchHelperGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            if (child != null) {
                val viewHolder = recyclerView.getChildViewHolder(child)
                onItemClick(viewHolder)
            }
            return true
        }


        override fun onLongPress(e: MotionEvent) {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            if (child != null) {
                val viewHolder = recyclerView.getChildViewHolder(child)
                onItemLongClick(viewHolder)
            }
        }
    }
}
