package com.ybj366533.yycamera.view

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.MediaScannerConnection
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Toast

import com.bumptech.glide.Glide
import com.ybj366533.videolib.editor.EditCallback
import com.ybj366533.videolib.editor.EditorCreator
import com.ybj366533.videolib.editor.ExtractFrameInfo
import com.ybj366533.videolib.editor.IVideoEditor
import com.libq.videocutpreview.VideoCutView
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.utils.FileUtils

import java.io.File
import java.util.ArrayList

/**
 * descirb:视频裁剪页面
 * author:libq
 * date:
 */
class VideoCropActivity : AppCompatActivity() {

    private var surfaceView: GLSurfaceView? = null
    private var mEditor: IVideoEditor? = null
    private var videoPath = ""//视频路径
    private var msc: MediaScannerConnection? = null //文件扫描器
    private var videoCutView: VideoCutView? = null
    private var progressBar: ProgressBar? = null
    private var getVideoFrameThread: Thread? = null//获取线程
    private var btnOk: ImageView? = null
    private var btnBack: ImageView? = null
    private var seekBar: SeekBar? = null
    private var videoDuration: Int = 0
    private var videoIntervalStart: Int = 0
    private val videoIntervalEnd: Int = 0//视频seekbar播放区间
    private var mVideoStartTime = -1//视频播放开始时间
    //输出 裁切完的路径
    private val outputPath = Environment.getExternalStorageDirectory().toString() + File.separator + "YYCache"

    private var urls: ArrayList<String>? = null
    //抽帧完成异步处理
    private var getVideoFrameHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 1) {
                urls = msg.data.getStringArrayList("URLS")
                getVideoFrameThread!!.interrupt()
                initCutViewFrame()

                Log.e("VideoCropActivity", "img urls = " + urls!!.size)
            }
        }
    }

    internal var isGetVideoFrameFinish = false
    override fun onCreate(savedInstanceState: Bundle?) {
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_crop)
        getExtra()
        initView()
    }

    private fun initView() {

        FileUtils.createDir(outputPath)
        FileUtils.delAllFile(outputPath)

        videoCutView = findViewById<View>(R.id.cutView) as VideoCutView
        surfaceView = findViewById<View>(R.id.surface) as GLSurfaceView
        progressBar = findViewById<View>(R.id.progress) as ProgressBar
        btnOk = findViewById<View>(R.id.btn_ok) as ImageView
        btnBack = findViewById<View>(R.id.btn_back) as ImageView
        seekBar = findViewById<View>(R.id.video_seek) as SeekBar


        mEditor = EditorCreator.getInstance()
        //mEditor 初始化
        mEditor!!.init(surfaceView, videoPath, object : EditCallback {
            override fun onInitReady() {
                runOnUiThread {
                    mEditor!!.startPreview()
                    videoDuration = mEditor!!.getDuration()
                    videoCutView!!.setCutMinDuration(1000)
                    videoCutView!!.setVideoDuration(videoDuration)
                    videoCutView!!.getSuitImageCount { i ->
                        getVideoFrameThread = Thread(GetVideoFrameRunable(i))
                        getVideoFrameThread!!.start()//开启抽真显示线程
                    }
                    mEditor!!.playStart()
                }
            }

            override fun onPlayComplete() {
                //播放完毕，拖动到指定开始位置继续播放
                runOnUiThread {
                    mEditor!!.playPause()
                    mEditor!!.seekTo(0)
                }
            }

            override fun onError(i: Int) {
                runOnUiThread { Toast.makeText(this@VideoCropActivity, "裁切失败", Toast.LENGTH_LONG).show() }
            }

            override fun onProgress(i: Int) {
                runOnUiThread { }
            }

            override fun onComposeFinish(i: Int) {
                runOnUiThread {
                    startScanFile()
                    progressBar!!.visibility = View.INVISIBLE
                }
            }

        })

        btnOk!!.setOnClickListener {
            //TODO 合成方式修改
            //                mEditor.startCompose(outputPath);
        }

        btnBack!!.setOnClickListener {
            /*if(getVideoFrameThread!=null&&!getVideoFrameThread.isInterrupted()){
                    getVideoFrameThread.interrupt();
                    getVideoFrameThread = null;
                }*/
            finish()
        }

        seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                val currProgress = seekBar.progress
                if (currProgress < videoIntervalStart) {
                    seekBar.progress = videoIntervalStart
                }
                if (currProgress > videoIntervalEnd) {
                    seekBar.progress = videoIntervalEnd
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
    }

    private fun getExtra() {
        videoPath = intent.getStringExtra(MainActivity.KEY_PATH)
    }

    /**
     * 扫描文件(裁剪之后要扫描文件才能显示出来)
     */
    private fun startScanFile() {
        if (msc == null) {
            msc = MediaScannerConnection(this, null)
        }
        if (!msc!!.isConnected) {
            msc!!.connect()
        }
        Log.e("", " scan path = $outputPath")
        //开始扫描，扫描后推出
//        msc!!.scanFile(this, arrayOf(outputPath), arrayOf("video/mp4"), MediaScannerConnection.OnScanCompletedListener { path, uri ->
//            Toast.makeText(this@VideoCropActivity, "裁切成功", Toast.LENGTH_LONG).show()
//            startScanFile()
//        })
    }


    /**
     * 释放mediascanner
     */
    private fun releaseScanObj() {
        if (msc != null) {
            msc!!.disconnect()
        }
    }


    private fun initCutViewFrame() {

        //设置帧路径,加载图片
        videoCutView!!.setImageUrls(urls) { imgUrls, ivs ->
            for (i in imgUrls.indices) {
                Glide.with(this@VideoCropActivity).load(imgUrls[i]).into(ivs[i])
                Log.e("VideoCropActivity", " url = " + imgUrls[i] + "  iv = " + ivs[i].id)
            }
        }

        /**
         * 播放区间改变监听
         */
        videoCutView!!.setOnVideoPlayIntervalChangeListener { startTime, endTime ->
            mEditor!!.setVideoCropRange(startTime, endTime)
            videoIntervalStart = (startTime * 1f / (videoDuration * 1f) * 100).toInt()
            videoIntervalStart = (endTime * 1f / (videoDuration * 1f) * 100).toInt()
            //如果视频播放开始位置改变则更新seekbar进度
            if (mVideoStartTime != startTime) {
                seekBar!!.progress = startTime
                mVideoStartTime = startTime
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (getVideoFrameThread != null) {
            getVideoFrameThread = null
        }
        releaseScanObj()
    }

    override fun onResume() {
        super.onResume()
        mEditor!!.startPreview()
    }

    override fun onPause() {
        super.onPause()
        mEditor!!.stopPreview()
    }

    /**
     * 抽幀任务
     */
    internal inner class GetVideoFrameRunable(private val frameCount: Int//幀数
    ) : Runnable {
        override fun run() {
            val infos = ArrayList<ExtractFrameInfo>()
            if (mEditor != null) {
                //抽帧
                mEditor!!.extractVideoFrame(IVideoEditor.ImageFormat.IMAGE_JPEG, outputPath, 0, mEditor!!.getDuration(), frameCount, infos)
                Log.e("VideoCropActivity", "video duration = " + mEditor!!.getDuration())
            }
            //判断如果抽帧完成，返回路径
            while (!isGetVideoFrameFinish) {
                if (!infos.isEmpty()) {
                    val url = ArrayList<String>()
                    for (info in infos) {
                        url.add(info.getFilePath())
                    }
                    val msg = getVideoFrameHandler.obtainMessage(1)
                    val b = Bundle()
                    b.putStringArrayList("URLS", url)
                    msg.data = b
                    getVideoFrameHandler.sendMessage(msg)
                    isGetVideoFrameFinish = true
                }
            }
        }
    }


}
