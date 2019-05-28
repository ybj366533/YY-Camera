package com.ybj366533.yycamera.view

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaScannerConnection
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.ybj366533.videolib.editor.EditorCreator
import com.ybj366533.videolib.editor.ExtractFrameInfo
import com.ybj366533.videolib.editor.IVideoEditor
import com.ybj366533.videolib.utils.YYVideoUtils
import com.ybj366533.videolib.utils.LogUtils
import com.libq.scrolltrackview.ScrollTrackView
import com.libq.videocutpreview.VideoCutView
import com.libq.videocutpreview.VideoThumbnailView
import com.ybj366533.yycamera.utils.Constants
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.R.id.glSurfaceView_placehodler
import com.ybj366533.yycamera.ui.VideoEffectEditControlView
import com.ybj366533.yycamera.utils.FileUtils
import com.ybj366533.yycamera.utils.MediaUtil
import com.ybj366533.yycamera.utils.ToolUtils
import com.ybj366533.yycamera.widget.SimpleEditCallback

import java.io.File
import java.text.NumberFormat
import java.util.ArrayList

class VideoEditActivity : AppCompatActivity(), View.OnClickListener {

    private var glSurfaceView: GLSurfaceView? = null

    private var btnSave: Button? = null
    private var btnCoverSetting: TextView? = null
    private var btnSlowmotion: TextView? = null
    private var videoCutView: VideoCutView? = null
    private var videoCutTopLayout: View? = null//视频裁剪头titlebar
    private var videoSeekBar: SeekBar? = null
    private var videoIntervalStart: Int = 0
    private var videoIntervalEnd: Int = 0//视频seekbar播放区间

    private var mSlowMotionSeekBar: SeekBar? = null
    private var slowSettingLayout: View? = null
    private var slowmotionFlag = false

    private var btnEffectSetting: TextView? = null        // 特效设定，弹出特效设定画面
    private var isEffectEdit: Boolean = false//当前是否是特效编辑

    private var viewEditLayout: View? = null               // 编辑主设定画面（特效编辑以外的画面）
    internal var videoEffectEditControlView: VideoEffectEditControlView? = null  // 特效编辑画面
    private var mCutAudioLayout: View? = null//音频裁剪视图
    private var mScrollTrackView: ScrollTrackView? = null//音频音轨视图
    private var mAudioCutStartTime: TextView? = null//音频裁剪开始时间
    private var btnBack: TextView? = null

    private var videoPath: String? = null
    private var musicPath: String? = null
    private var outputPath: String? = null

    private var msc: MediaScannerConnection? = null

    private var mVideoStartTime: Int = 0
    private var mVideoEndTime: Int = 0
    private var mVideoDuration: Int = 0
    private var layoutFrame: View? = null//抽帧底部布局


    private var fromDraft: Boolean = false//是否从草稿箱跳转
    private var editWorkFloder = ""

    private var currentDraftFloder: String? = null

    private var alertDialog: Dialog? = null


    //    private IAVStreamer mixStreamer;
    private val isCompsing = false

    private var mEditor: IVideoEditor? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        LogUtils.LOGI(TAG, "onCreate")

        //去除title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_edit)

        val params = intent
        videoPath = params.getStringExtra(Constants.VIDEO_FILE)


        fromDraft = params.getBooleanExtra(Constants.KEY_IS_FROM_DRAFT, false)
        editWorkFloder = params.getStringExtra(Constants.EDIT_WORK_FOLDER)
        currentDraftFloder = params.getStringExtra(Constants.CURRENT_DRAFT_FLODER)

        if (videoPath == null) {
            videoPath = "/storage/emulated/0/DCIM/1.mp4"       //debug, 如果不是从录制界面过来
        }
        //        videoPath = "/storage/emulated/0/DCIM/mm.mp4";
        Log.e("videoPath", videoPath)
        musicPath = params.getStringExtra(Constants.MUSIC_FILE)


        if (musicPath == null) {
            musicPath = "/storage/emulated/0/VideoRecorderTest/music/lvxing.mp3"
        }


        initView()

        initYYSDK()


        msc = MediaScannerConnection(this, null)
        msc!!.connect()

    }

    private fun initVideoCrop() {
        //视频裁剪
        findViewById<View>(R.id.cut_video).setOnClickListener(this)
        layoutFrame = findViewById(R.id.layout_video_frame)
        videoCutView = findViewById<View>(R.id.cut_video_view) as VideoCutView

        findViewById<View>(R.id.btn_cut_video_back).setOnClickListener(this)
        videoCutTopLayout = findViewById(R.id.top_video_crop)

        videoSeekBar = findViewById<View>(R.id.video_seek) as SeekBar

        videoSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                if (progress < videoIntervalStart) {
                    seekBar.progress = videoIntervalStart
                }
                if (progress > videoIntervalEnd) {
                    seekBar.progress = videoIntervalEnd
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mEditor!!.seekTo((videoIntervalStart * 1f / 100f * mVideoDuration).toInt())
                mEditor!!.playStart()
            }
        })

    }


    /**
     * 检测视频信息是否改变，比如设置裁剪配乐期间，裁剪区间等等，改变了
     * @return
     */
    private fun checkVideoInfoChanged(): Boolean {
        return true
    }

    private fun showSaveDialog() {
        if (alertDialog == null) {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setMessage("放弃当前已有更改？")
                    .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }.setPositiveButton("放弃") { dialog, which -> handleBack() }
            alertDialog = builder.create()
        } else {
            alertDialog!!.show()
        }

    }


    override fun onResume() {
        super.onResume()
        mEditor!!.startPreview()
        // 因为现在编辑界面分为主界面和特效界面，所以播放器是否重开 归demo管了。
        // 这边简单期间统一重开
        mEditor!!.playStart()

    }

    override fun onPause() {
        super.onPause()
        mEditor!!.stopPreview()

    }

    override fun onDestroy() {
        ToolUtils.DebugLog(TAG, " on destroy edit")
        super.onDestroy()
        mEditor!!.destroy()
        msc!!.disconnect()
    }

    override fun onClick(v: View) {
        val id = v.id

        if (id == R.id.save_btn) {
            doNext()
        }

        if (id == R.id.cover_setting_btn) {

        }


        if (id == R.id.slow_motion_btn) {
            slowmotionFlag = !slowmotionFlag
            slowSettingLayout!!.visibility = if (slowmotionFlag) View.VISIBLE else View.INVISIBLE
        }

        if (id == R.id.effect_setting_btn) {
            //isCanLoopPlayVideo = false;//不循环播放
            isEffectEdit = true
            // 隐藏当前设定按钮
            // 弹出特效编辑界面
            // 根据特效编辑界面的排版 调整 view的位置

            viewEditLayout!!.visibility = View.INVISIBLE
            videoEffectEditControlView!!.visibility = View.VISIBLE
            val frameLayout = findViewById<View>(glSurfaceView_placehodler)
            val height = frameLayout.height
            val width = frameLayout.width

            val params = FrameLayout.LayoutParams(height * 9 / 16, height)
            params.topMargin = frameLayout.top
            params.leftMargin = (width - height * 9 / 16) / 2

            glSurfaceView!!.layoutParams = params
            mEditor!!.setDisplayMode(false)

        }
        //进入视频裁剪视图
        if (id == R.id.cut_video) {
            //isCanLoopPlayVideo = true;
            FileUtils.createDir(FRAME_OUT_PUT_PATH)
            viewEditLayout!!.visibility = View.INVISIBLE
            layoutFrame!!.visibility = View.VISIBLE
            videoCutTopLayout!!.visibility = View.VISIBLE
            //缩小GLSurfaceView
            val frameLayout = findViewById<View>(glSurfaceView_placehodler)
            val height = frameLayout.height
            val width = frameLayout.width

            val params = FrameLayout.LayoutParams(height * 9 / 16, height)
            params.topMargin = frameLayout.top
            params.leftMargin = (width - height * 9 / 16) / 2
            glSurfaceView!!.layoutParams = params

            //设置视频时长
            videoCutView!!.setVideoDuration(mVideoDuration)
            //设置视频最小裁剪时长
            videoCutView!!.setCutMinDuration(3000)
            //获取最适合当前屏幕的图片张数
            videoCutView!!.getSuitImageCount { frameCount ->
                FileUtils.delAllFile(FRAME_OUT_PUT_PATH)
                val infos = ArrayList<ExtractFrameInfo>()
                //抽帧
                YYVideoUtils.extractVideoFrame(videoPath, IVideoEditor.ImageFormat.IMAGE_JPEG, FRAME_OUT_PUT_PATH, 0, mEditor!!.getDuration(), frameCount, infos)
                val urls = ArrayList<String>()
                for (info in infos) {
                    urls.add(info.filePath)
                }
                //设置图片路径
                videoCutView!!.setImageUrls(urls) { imgUrls, ivList ->
                    for (i in imgUrls.indices) {
                        //设置图片，可以替换成自己的图片加载框架api
                        val iv = ivList[i]

                        val ro = RequestOptions()
                        ro.diskCacheStrategy(DiskCacheStrategy.NONE)
                        Glide.with(this@VideoEditActivity)
                                .setDefaultRequestOptions(ro)
                                .load(urls[i])
                                .into(iv)
                        if (i == 0)
                            Log.e("bbb", "id = " + ivList[i].toString())
                    }
                    //Log.e("bbb","list size = "+ivList.size());
                }
            }
            //添加视频播放区间改变监听
            videoCutView!!.setOnVideoPlayIntervalChangeListener { startTime, endTime ->
                videoIntervalStart = (startTime * 1f / (mVideoDuration * 1f) * 100).toInt()
                videoIntervalEnd = (endTime * 1f / (mVideoDuration * 1f) * 100).toInt()
                Log.e("YYVideoEdit", "start = $videoIntervalStart  , end = $videoIntervalEnd")
                //如果视频播放开始位置改变则更新seekbar进度
                if (mVideoStartTime != startTime) {
                    videoSeekBar!!.progress = videoIntervalStart
                    mVideoStartTime = startTime
                }

                mVideoEndTime = endTime
                mEditor!!.setVideoCropRange(startTime, endTime)
            }
            //添加点击裁剪区域监听
            videoCutView!!.setOnTouchCutAreaListener(object : VideoThumbnailView.OnTouchCutAreaListener {
                override fun onTouchUp() {
                    mEditor!!.seekTo(mVideoStartTime)
                    mEditor!!.playStart()
                }

                override fun onTouchDown() {
                    mEditor!!.playPause()
                }
            })

        }

        //裁剪配乐
        if (id == R.id.no_music_btn) {
            initTrack()
        }
        //设置配乐完成
        if (id == R.id.btn_cut_audio_ok) {

            mScrollTrackView!!.stopMove()
            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            params.topMargin = 0
            params.leftMargin = 0
            glSurfaceView!!.layoutParams = params

            mCutAudioLayout!!.visibility = View.INVISIBLE
            viewEditLayout!!.visibility = View.VISIBLE
            //mEditor.playStart();
        }

        //退出视频裁剪
        if (id == R.id.btn_cut_video_back) {
            videoCutView!!.clearAllFrame()
            videoCutTopLayout!!.visibility = View.INVISIBLE
            viewEditLayout!!.visibility = View.VISIBLE
            layoutFrame!!.visibility = View.INVISIBLE
            //isCanLoopPlayVideo = true;
            //全屏
            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            params.topMargin = 0
            params.leftMargin = 0
            glSurfaceView!!.layoutParams = params

        }

        //退出
        if (id == R.id.btn_back) {
            //showBackDialog();
            onBackPressed()
        }

    }

    private fun initTrack() {

        //isCanLoopPlayVideo = true;
        //isLoopPlayThreadOpen = true;
        mAudioCutStartTime!!.text = "从 0 秒开始"
        viewEditLayout!!.visibility = View.INVISIBLE
        mCutAudioLayout!!.visibility = View.VISIBLE
        mScrollTrackView!!.restartMove()
        //mEditor.playStart();

    }

    private fun doNext() {
        if (mEditor != null) {
            mEditor!!.saveSetting()

            val recWorkFolder = ToolUtils.getExternalFilesPath(this@VideoEditActivity) + File.separator + Constants.REC_WORK_FOLDER
            // 演示从编辑结束后画面通过系统返回键回到编辑画面
            val intent = Intent(this@VideoEditActivity, RecordFinishActivity::class.java)
            intent.putExtra(Constants.KEY_IS_FROM_DRAFT, fromDraft)
            intent.putExtra(Constants.CURRENT_DRAFT_FLODER, currentDraftFloder)
            intent.putExtra(Constants.EDIT_WORK_FOLDER, if (fromDraft) editWorkFloder else recWorkFolder)
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //intent.putExtra("VIDEO_FILE", outputPath);
            startActivity(intent)
        }
    }


    override fun onBackPressed() {
        if (fromDraft) {
            // 如果是从草稿箱打开
            if (checkVideoInfoChanged()) {
                showSaveDialog()
            } else {
                handleBack()
            }

        } else {
            val mAlertDialog = AlertDialog.Builder(this@VideoEditActivity)
                    .setTitle("取消编辑")
                    .setMessage("所有的修改将不会被保存，确认要取消编辑吗？")
                    .setPositiveButton("是") { dialog, which ->
                        // 因为，编辑界面按返回按钮，回到上次录制的状态，改为采用系统默认处理。 20180418

                        //                        // 弹出摄像头录制画面
                        //                        Intent intent = new Intent(VideoEditActivity.this, RecordActivity.class);
                        //
                        //
                        //                        startActivity(intent);
                        //                        finish();
                        super@VideoEditActivity.onBackPressed()
                    }
                    .setNegativeButton("否") { dialog, which -> }
                    .show()
        }
    }

    private fun handleBack() {
        val intent = Intent(this@VideoEditActivity, RecordActivity::class.java)
        //String videoFile=  ToolUtils.getExternalFilesPath(PublishActivity.this) + "/draft/1/YY.mp4";
        //intent.putExtra("VIDEO_FILE", videoFile);
        intent.putExtra(Constants.REC_WORK_FOLDER, editWorkFloder)      // demo假设 拍摄工作目录 和 编辑工作目录 是同一个文件夹
        intent.putExtra(Constants.MUSIC_FILE, mEditor!!.getMusicPath())
        intent.putExtra(Constants.MUSIC_START_TIME, mEditor!!.getMusicStartTime())
        intent.putExtra(Constants.KEY_IS_FROM_DRAFT, true)
        intent.putExtra(Constants.CURRENT_DRAFT_FLODER, currentDraftFloder)
        //intent.putExtra("MUSIC_FILE", musicPath);
        startActivity(intent)
        finish()
    }


    private fun scanFile(fileName: String) {
        msc!!.scanFile(fileName, "video/mp4")
    }


    private fun initView() {

        glSurfaceView = findViewById<View>(R.id.glSurfaceView) as GLSurfaceView

        btnSave = findViewById<View>(R.id.save_btn) as Button
        btnSave!!.setOnClickListener(this)

        btnCoverSetting = findViewById<View>(R.id.cover_setting_btn) as TextView
        btnCoverSetting!!.setOnClickListener(this)

        initVideoCrop()

        initAudioCrop()

        //  end 音频处理模块 libq

        mSlowMotionSeekBar = findViewById<View>(R.id.progressBar_slowmotion) as SeekBar
        mSlowMotionSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mEditor != null) {
                    val duration = mEditor!!.duration
                    val startTime = duration * progress / 100

                    mEditor!!.setSlowPlayTime(startTime, startTime + 1000)

                    val textSlowMotionStarTime = findViewById<View>(R.id.text_slowmotion_total_time) as TextView
                    textSlowMotionStarTime.text = ToolUtils.stringForTime(startTime)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })


        btnSlowmotion = findViewById<View>(R.id.slow_motion_btn) as TextView
        btnSlowmotion!!.setOnClickListener(this)
        slowmotionFlag = false
        slowSettingLayout = findViewById(R.id.slow_motion_setting_layout)
        slowSettingLayout!!.visibility = View.INVISIBLE

        btnEffectSetting = findViewById<View>(R.id.effect_setting_btn) as TextView
        btnEffectSetting!!.setOnClickListener(this)

        viewEditLayout = findViewById(R.id.YY_video_edit_layout)

        videoEffectEditControlView = findViewById<View>(R.id.videoEffectEditControlView) as VideoEffectEditControlView

        if (fromDraft) {
            btnBack!!.text = "继续拍摄"
        }


    }

    /**
     * 初始化音频裁剪
     */
    private fun initAudioCrop() {
        //音频处理模块 libq
        mCutAudioLayout = findViewById(R.id.cut_audio_layout)
        mScrollTrackView = findViewById<View>(R.id.scroll_track) as ScrollTrackView
        findViewById<View>(R.id.btn_cut_audio_ok).setOnClickListener(this)
        btnBack = findViewById<View>(R.id.btn_back) as TextView

        btnBack!!.setOnClickListener(this)

        findViewById<View>(R.id.no_music_btn).setOnClickListener(this)
        mAudioCutStartTime = findViewById<View>(R.id.cut_start_time) as TextView
        mScrollTrackView!!.setSpaceSize(6)
        mScrollTrackView!!.setTrackItemWidth(6)
        mScrollTrackView!!.setLoopRun(true)
        mScrollTrackView!!.setDuration(MediaUtil.getMediaDuration(musicPath!!)) // 音频时间
        mScrollTrackView!!.setCutDuration(MediaUtil.getMediaDuration(videoPath!!))//屏幕左边跑到右边持续的时间,以视频长度为准
        mScrollTrackView!!.stopMove()
        mScrollTrackView!!.setOnProgressRunListener(object : ScrollTrackView.OnProgressRunListener {
            override fun onTrackStart(i: Int) {

            }

            override fun onTrackStartTimeChange(i: Int) {
                //保留两位小数
                val nf = NumberFormat.getNumberInstance()
                nf.maximumFractionDigits = 2
                mAudioCutStartTime!!.text = "从 " + nf.format((i * 1f / 1000f).toDouble()) + " 秒开始"
                mEditor!!.setMusicPath(musicPath, i)

            }

            override fun onTrackEnd() {}
        })
    }

    private fun initYYSDK() {
        val editCallback = object : SimpleEditCallback() {
            override fun onPrepared() {
                mEditor!!.displayMode = true
                mVideoDuration = mEditor!!.duration
                //LogUtils.DebugLog(TAG, "  " + mEditor.getDuration() + "    " + mEditor.getCurrentPosition());
                if (videoEffectEditControlView != null) {
                    videoEffectEditControlView!!.init(mEditor!!, object : VideoEffectEditControlView.OnFinishListener{
                        override fun onFinish(saveFlag: Boolean): Boolean {
                            //全屏
                            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            params.topMargin = 0
                            params.leftMargin = 0
                            glSurfaceView!!.layoutParams = params

                            videoEffectEditControlView!!.setVisibility(View.INVISIBLE)
                            viewEditLayout!!.visibility = View.VISIBLE
                            mEditor!!.setDisplayMode(true)
                            mEditor!!.seekTo(0)
                            mEditor!!.playStart()
                            isEffectEdit = false//判断是否是编辑特效状态
                            //isCanLoopPlayVideo = true;//设置可循环播放
                            return true       // 目前这个返回值无所谓
                        }
                    })
                }

                runOnUiThread { mEditor!!.playStart() }
            }

            override fun onPlayEnd() {
                runOnUiThread {
                    // 编辑主画面时,视频裁剪页面 需要连续播放
                    // 特效编辑画面 不需要连续播放
                    if (viewEditLayout!!.visibility == View.VISIBLE || videoCutTopLayout!!.visibility == View.VISIBLE) {
                        mEditor!!.seekTo(mVideoStartTime)
                        mEditor!!.playStart()
                    }
                }

            }
        }

        // 创建编辑对象IYYVideoRecorder
        mEditor = EditorCreator.getInstance()

        // 草稿箱功能：init增加2个参数
        // editWorkFolder: 编辑工作目录， 用于保存编辑设定文件 （以便从草稿箱可以继续编辑）
        // reloadSettingFlag: 是否从 编辑工作目录的编辑设定文件 导入 编辑设定
        // 第一次编辑（从拍摄页面过来）： false
        // 从草稿箱打开：true， 可以读入原有的编辑设定
        val reloadSettingFlag = fromDraft

        mEditor!!.init(glSurfaceView, videoPath, editWorkFloder, reloadSettingFlag, editCallback)
        if (musicPath != null) {
            mEditor!!.setMusicPath(musicPath, 0)
            //mEditor.setMusicPath(musicPath, 5000);        // for test
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //获取图片路径
        if (requestCode == IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage = data.data
            val filePathColumns = arrayOf(MediaStore.Images.Media.DATA)
            val c = contentResolver.query(selectedImage!!, filePathColumns, null, null, null)
            c!!.moveToFirst()
            val columnIndex = c.getColumnIndex(filePathColumns[0])
            val imagePath = c.getString(columnIndex)
            //saveCoverImage(imagePath);
            mEditor!!.setMp4VideoCover(imagePath)
            c.close()

        }
        mEditor!!.playStart()
    }

    companion object {

        private val TAG = "EditorActivity"

        private val IMAGE_REQUEST = 101
        private val REQUEST_CODE = 102
        private val FRAME_OUT_PUT_PATH = (Environment.getExternalStorageDirectory().toString()
                + File.separator + "DCIM" + File.separator + "YYFrameCache")//抽帧存放路径
    }

}
