package com.ybj366533.yycamera.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

import com.gtv.cloud.editor.GTVideoEffectInfo
import com.gtv.cloud.editor.IGTVVideoEditor
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.widget.RecordedLayout

import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * Created by Ivy on 2018/4/13.
 */

class VideoEffectEditControlView : LinearLayout, View.OnClickListener {

    private var mContext: Context? = null
    private var contentView: View? = null

    private var btn_live_start: ImageButton? = null
    private var video_play_layout: View? = null

    private var mLiveTimeText: TextView? = null
    private var mLiveTotalTimeText: TextView? = null

    //    private SeekBar mSeekBar;
    //    private SeekBar.OnSeekBarChangeListener mDragListener;
    private var mRecordedLayout: RecordedLayout? = null

    private var mStickersRecycleView: RecyclerView? = null
    private var mAdapter: VisualEffectRecyclerAdapter? = null

    private var effectCancelBtn: TextView? = null
    private var effectSaveBtn: TextView? = null

    private var myHandler: MyHandler? = null

    private var mEditor: IGTVVideoEditor? = null
    private var onFinishListener: OnFinishListener? = null

    private var btnDeleteMode: Button? = null

    private var filterStartTime = -1

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    private fun initView(context: Context) {
        mContext = context
        val layoutInflater = LayoutInflater.from(context)
        contentView = layoutInflater.inflate(R.layout.layout_video_effect_edit, this, true)

        mStickersRecycleView = findViewById(R.id.rv_stickers) as RecyclerView
        mStickersRecycleView!!.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        mStickersRecycleView!!.setAdapter(mAdapter = VisualEffectRecyclerAdapter(mContext))

        mStickersRecycleView!!.visibility = View.VISIBLE
        mAdapter!!.setVisualEffectCheckListener(object : VisualEffectRecyclerAdapter.OnVisualEffectCheckListener {
            override fun onVisualEffectStart(pos: Int, type: IGTVVideoEditor.EffectType): Boolean {
                if (mEditor != null) {
                    Log.e("mAdapter", "startVideoEffect")
                    //                    mRecordedLayout.setSplit();
                    mEditor!!.startVideoEffect(type)
                    mEditor!!.playStart()
                    mRecordedLayout!!.setEffectInfoList(mEditor!!.getVideoEffectList(), true)
                    //mRecordedLayout.setTypeInfo(pos, true);
                }
                return false
            }

            override fun onVisualEffectStop(pos: Int, type: IGTVVideoEditor.EffectType): Boolean {
                Log.e("mAdapter", "stopVideoEffect")
                //mRecordedLayout.setTypeInfo(pos, false);

                mEditor!!.stopVideoEffect(type)
                mEditor!!.playPause()
                mRecordedLayout!!.setEffectInfoList(mEditor!!.getVideoEffectList(), false)

                return false
            }
        })

        btn_live_start = contentView!!.findViewById(R.id.btn_live_start) as ImageButton
        btn_live_start!!.setOnClickListener(this)

        video_play_layout = contentView!!.findViewById(R.id.video_play_layout)
        video_play_layout!!.setOnClickListener(this)

        mLiveTimeText = findViewById(R.id.text_live_time) as TextView
        mLiveTotalTimeText = findViewById(R.id.text_live_total_time) as TextView

        mRecordedLayout = findViewById(R.id.ll_recoded) as RecordedLayout

        btnDeleteMode = findViewById(R.id.btn_delete_mode) as Button
        btnDeleteMode!!.setOnClickListener(this)
        //mSeekBar = (SeekBar) findViewById(R.id.progressBar);
        //        mDragListener = new SeekBar.OnSeekBarChangeListener() {
        //            int duration;
        //            int newposition;
        //
        //            @Override
        //            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //                if (fromUser && mEditor != null) {
        //                    duration = mEditor.getDuration();
        //                    long durationProgress = 1L * duration * progress;
        //                    newposition = (int) (durationProgress / 1000);
        //                    updateProgressText(duration, newposition);
        //                } else {
        //                }
        //            }
        //
        //            @Override
        //            public void onStartTrackingTouch(SeekBar seekBar) {
        //
        //
        //                if (myHandler != null) {
        //                    myHandler.removeMessages(MSG_UPDATE_PROGRESS);
        //                }
        //
        //                btn_live_start.setVisibility(View.VISIBLE);
        //
        //                if (mEditor != null) {;
        //                    mEditor.playPause();
        //                }
        //            }
        //
        //            @Override
        //            public void onStopTrackingTouch(SeekBar seekBar) {
        //                if (mEditor != null && myHandler != null) {
        //                    mEditor.seekTo(newposition);
        //                    myHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 100);
        //                    mEditor.playPause();
        //
        //                }
        //            }
        //        };
        //
        //        mSeekBar.setOnSeekBarChangeListener(mDragListener);

        effectCancelBtn = findViewById(R.id.effect_cancel_btn) as TextView
        effectCancelBtn!!.setOnClickListener(this)
        effectSaveBtn = findViewById(R.id.effect_save_btn) as TextView
        effectSaveBtn!!.setOnClickListener(this)

        myHandler = MyHandler(this)

        mRecordedLayout!!.setOnGestureListener(object : RecordedLayout.OnGestureListener {
            override fun onStop(endType: GTVideoEffectInfo) {
                mEditor!!.stopVideoEffect(endType.getEffectType())
                //updateSeekTo(mEditor.getDuration());
                mRecordedLayout!!.setEffectInfoList(mEditor!!.getVideoEffectList(), false)
            }

            override fun onClick(mCursor: Float) {
                val progress = (mEditor!!.duration * mCursor).toInt()
                Log.e("updateSeekTo", "updateSeekTo：$progress")
                updateSeekTo(progress)
            }

            override fun onEnd() {
                //updateSeekTo(0);
            }

        })

    }

    fun init(igtvVideoEditor: IGTVVideoEditor, onFinishListener: OnFinishListener) {
        this.mEditor = igtvVideoEditor
        this.onFinishListener = onFinishListener

    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (mEditor != null) {
            if (visibility == View.VISIBLE) {

                mEditor!!.seekTo(0)
                mEditor!!.playPause()
                updateProgress(mEditor!!.getDuration(), mEditor!!.getCurrentPosition())
                mRecordedLayout!!.setMaxTime(mEditor!!.getDuration())
                myHandler!!.sendEmptyMessage(MSG_UPDATE_PROGRESS)
            } else {

            }

        }

        // 根据需要，获取特效设定列表，来更新界面
        // list的顺序，就是特效的添加顺序，最后一个特效如果endTime是-1， 表明这个特效还没设置结束时间
        //List<GTVideoEffectInfo> effectInfoList = mEditor.getVideoEffectList();

    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.btn_live_start) {
            btn_live_start!!.visibility = View.INVISIBLE
            if (mEditor != null) {
                mEditor!!.playStart()

                filterStartTime = mEditor!!.getCurrentPosition()
            }
        }

        if (id == R.id.video_play_layout) {
            btn_live_start!!.visibility = View.VISIBLE
            if (mEditor != null) {
                mEditor!!.playPause()
                //                if (filterStartTime >= 0) {
                //                    //Log.e(TAG, " " + filterStartTime);
                //                    mEditor.addEffectFilter(filterStartTime, mEditor.getCurrentPosition(), mEditor.getEffectFilterType());
                //                    filterStartTime = -1; // 防止多次stop
                //                    //Log.e(TAG, " " + filterStartTime);
                //                }
            }
        }

        if (id == R.id.effect_cancel_btn) {
            if (mEditor != null && mEditor!!.getVideoEffectList() != null && mEditor!!.getVideoEffectList().size() > 0) {
                val mAlertDialog = AlertDialog.Builder(mContext!!)
                        .setTitle("取消编辑")
                        .setMessage("是否清除已添加的特效？")
                        .setPositiveButton("是") { dialog, which ->
                            if (mEditor != null) {
                                mEditor!!.clearAllVideoEffect()
                            }

                            if (onFinishListener != null) {
                                onFinishListener!!.onFinish(false)
                            }
                        }
                        .setNegativeButton("否") { dialog, which -> }
                        .show()
            } else {
                if (onFinishListener != null) {
                    onFinishListener!!.onFinish(false)
                }
            }


        }

        if (id == R.id.effect_save_btn) {
            if (onFinishListener != null) {
                onFinishListener!!.onFinish(true)
            }

        }
        //撤销特效
        if (id == R.id.btn_delete_mode) {
            if (mEditor!!.getVideoEffectList().size() > 0) {
                val infos = ArrayList(mEditor!!.getVideoEffectList())
                Log.e("xxx", "bbb=" + infos.size)
                val size = infos.size
                mEditor!!.removeLastVideoEffect()
                Log.e("xxx", "bbb=" + infos.size + ";" + mEditor!!.getVideoEffectList().size())
                mRecordedLayout!!.setEffectInfoList(mEditor!!.getVideoEffectList(), false)
                updateSeekTo(if (size == 0) 0 else infos.get(size - 1).getStartTime())
            }
        }
    }

    fun updateProgressText(duration: Int, position: Int) {

        if (mLiveTimeText != null) {
            mLiveTimeText!!.setText(ToolUtils.stringForTime(position))
        }

        if (mLiveTotalTimeText != null) {
            mLiveTotalTimeText!!.setText(ToolUtils.stringForTime(duration))
        } else {
            //
        }

    }

    // 更新进度条以,播放时间
    fun updateProgress(duration: Int, position: Float) {
        if (mRecordedLayout != null && duration > 0) {
            //            long positionProgress = 1000L * position;
            //            int pos = (int) (positionProgress / duration);
            val progress = position / duration
            mRecordedLayout!!.setProgress(progress)

            //            if (progress < 1) {
            //                mRecordedLayout.setProgress(progress);
            //            } else {
            //                mRecordedLayout.setProgress(0);
            //                updateSeekTo(0);
            //            }
        }
        updateProgressText(duration, position.toInt())

    }

    fun updateSeekTo(filterStartTime: Int) {
        mEditor!!.seekTo(filterStartTime)
        myHandler!!.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 100)
        mEditor!!.playPause()
        btn_live_start!!.visibility = View.VISIBLE
    }

    internal class MyHandler(VideoEffectEditControlView: VideoEffectEditControlView) : Handler() {
        private val mActivityReference: WeakReference<VideoEffectEditControlView>

        init {
            mActivityReference = WeakReference(VideoEffectEditControlView)
        }

        override fun handleMessage(msg: Message) {
            val videoEffectEditControlView = mActivityReference.get() ?: return
            when (msg.what) {
                MSG_UPDATE_PROGRESS -> {
                    val view = msg.obj as View

                    if (videoEffectEditControlView.mEditor != null) {

                        val duration = videoEffectEditControlView.mEditor!!.getDuration()
                        val currentPosition = videoEffectEditControlView.mEditor!!.getCurrentPosition()

                        if (duration > 0 && currentPosition >= 0) {
                            Log.e("updateProgress", "duration：$currentPosition")
                            videoEffectEditControlView.updateProgress(duration, currentPosition.toFloat())
                        }
                    }

                    this.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 100)
                }
            }
        }
    }

    //    public void setEffectFilterType(IGTVVideoEditor.EffectType filterType) {
    //        if (mEditor != null) {
    //            if (filterStartTime >= 0) {
    //                mEditor.addEffectFilter(filterStartTime, mEditor.getCurrentPosition(), mEditor.getEffectFilterType());
    //                filterStartTime = mEditor.getCurrentPosition();
    //            }
    //            mEditor.setEffectFilterType(filterType);
    //        }
    //    }

    interface OnFinishListener {
        fun onFinish(saveFlag: Boolean): Boolean
    }

    companion object {

        private val MSG_UPDATE_PROGRESS = 0
    }

}
