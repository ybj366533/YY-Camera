package com.ybj366533.yycamera.view

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.ybj366533.yycamera.utils.Constants
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.ui.DraftItemInfo
import com.ybj366533.yycamera.ui.DraftRecyclerAdapter
import com.ybj366533.yycamera.utils.FileUtils
import com.ybj366533.yycamera.utils.ToolUtils


import java.io.File
import java.util.ArrayList

class DraftListActivity : AppCompatActivity() {


    private var rcyDraft: RecyclerView? = null
    private var adapter: DraftRecyclerAdapter? = null
    private var datas: ArrayList<DraftItemInfo>? = null
    private var tvBack: TextView? = null
    private var tvClear: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft_list)
        initView()
        addListener()
    }

    private fun addListener() {
        tvBack!!.setOnClickListener { finish() }
        tvClear!!.setOnClickListener {
            FileUtils.deleteDir(File(ToolUtils.getExternalFilesPath(this@DraftListActivity) + File.separator + Constants.VIDEO_DRAFT_DIR_NAME))
            datas!!.clear()
            adapter!!.notifyDataSetChanged()
            //new File(ToolUtils.getExternalFilesPath(DraftListActivity.this)+File.separator+Constants.VIDEO_DRAFT_DIR_NAME)
        }
    }

    private fun initView() {
        tvBack = findViewById<View>(R.id.draft_back) as TextView
        rcyDraft = findViewById<View>(R.id.draft_recycle) as RecyclerView
        tvClear = findViewById<View>(R.id.btn_clear) as TextView
        //获取草稿箱目录下的数据
        val draftPath = ToolUtils.getExternalFilesPath(this) + File.separator + Constants.VIDEO_DRAFT_DIR_NAME
        Log.e("draftPath", "## draftpath = $draftPath")
        val filePaths = ToolUtils.getDirsFilesPath(draftPath)
        datas = ArrayList<DraftItemInfo>()
        if (filePaths != null) {
            for (url in filePaths) {
                datas!!.add(DraftItemInfo(url[0], url[1]))
                Log.e("DLA", "#PATH-draft: path =" + url[0] + "  ,\nfloder=" + url[1])
            }
        }
        adapter = DraftRecyclerAdapter(this, datas)
        rcyDraft!!.layoutManager = LinearLayoutManager(this)
        rcyDraft!!.adapter = adapter


    }
}
