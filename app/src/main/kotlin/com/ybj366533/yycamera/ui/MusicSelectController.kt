package com.ybj366533.yycamera.ui

import android.app.Activity
import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.ybj366533.videolib.utils.YYMusicHandler
import com.ybj366533.yycamera.R


/**
 * Created by YY on 2017/7/27.
 */

class MusicSelectController(act: Activity, private val contentView: View) {

    private val mMusicSelectList: RecyclerView

    private val startRecord: TextView

    private val musicSelectRecyclerAdapter: MusicSelectRecyclerAdapter

    internal var listener: OnMusicSelectListener? = null


    init {

        mMusicSelectList = `$`(com.ybj366533.yycamera.R.id.music_select_list)
        mMusicSelectList.layoutManager = LinearLayoutManager(act.applicationContext, LinearLayoutManager.VERTICAL, false)
        mMusicSelectList.addItemDecoration(SpaceItemDecoration(2))
        musicSelectRecyclerAdapter = MusicSelectRecyclerAdapter(act)
        musicSelectRecyclerAdapter.setOnMusicSelectListener(object : MusicSelectRecyclerAdapter.OnMusicSelectListener {
            override fun onMusicSelect(pos: Int, fileName: String?): Boolean {
                //act.setMusicPathFromMusicController(fileName);
                if (listener != null) {
                    listener!!.onMusicSelected(fileName!!)
                }
                return false
            }
        })
        mMusicSelectList.adapter = musicSelectRecyclerAdapter

        startRecord = `$`<Any>(R.id.music_start_record) as TextView
        startRecord.setOnClickListener {
            YYMusicHandler.getInstance().stopPlay()
            this@MusicSelectController.contentView.visibility = View.INVISIBLE
        }

    }

    fun <T> `$`(id: Int): T {
        return contentView.findViewById<View>(id) as T
    }


    internal inner class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
            super.getItemOffsets(outRect, view, parent, state)
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.top = space
            }
        }
    }

    fun SetOnMusicSelectListener(listener: OnMusicSelectListener) {
        this.listener = listener
    }

    interface OnMusicSelectListener {
        fun onMusicSelected(path: String): Boolean
    }

}
