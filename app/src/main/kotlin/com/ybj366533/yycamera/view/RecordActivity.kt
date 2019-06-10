package com.ybj366533.yycamera.view

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaScannerConnection
import android.os.*
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ZoomControls
import com.ybj366533.videolib.recorder.RecordCreator
import com.ybj366533.videolib.recorder.VideoInfo
import com.ybj366533.videolib.recorder.IVideoRecorder
import com.ybj366533.videolib.recorder.RecordCallback
import com.ybj366533.videolib.utils.YYMusicHandler
import com.ybj366533.videolib.utils.LogUtils
import com.ybj366533.yycamera.utils.Constants
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.base.BaseActivity
import com.ybj366533.yycamera.base.IInitialize
import com.ybj366533.yycamera.databinding.ActivityVideoRecordBinding
import com.ybj366533.yycamera.ui.*
import com.ybj366533.yycamera.widget.CameraGLSurfaceView
import com.ybj366533.yycamera.widget.RecordedButton

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class RecordActivity : BaseActivity(), IInitialize, StickerRecylerAdapter.OnStickerCheckListener, FilterRecyclerAdapter.OnFilterCheckListener, View.OnClickListener {
    private lateinit var binding: ActivityVideoRecordBinding

    //TODO ～ Models 拍摄组建
    val models by lazy { Models() }

    private var glSurfaceView: CameraGLSurfaceView? = null

    private lateinit var mNextBtn: TextView
    private lateinit var btnCancel: ImageView
    private lateinit var btnRecordStart: RecordedButton
    private lateinit var btnRecordDeleteLast: ImageView

    private var mSwitchBtn: ImageView? = null
    private var mLightBtn: ImageView? = null
    private var zoomControls: ZoomControls? = null

    private var mRecordTimeTxt: TextView? = null
    private var recordTimelineView: RecordTimelineView? = null

    private var msc: MediaScannerConnection? = null

    private var btnTakePicture: TextView? = null

    private var focusImageView: FocusImageView? = null

    private var mSettingList: RecyclerView? = null
    private var recordSettingRecyclerAdapter: RecordSettingRecyclerAdapter? = null


    private var recordingFlag = false

    private var mRecorder: IVideoRecorder? = null
    private var firstClipRecord = true

    private var recWorkFolder: String? = null
    private var outputPath: String? = null
    private var outputPathRev: String? = null
    private var musicPath: String? = null
    private var fromDraft: Boolean = false//是否从草稿箱过来
    private var musicStartTime: Int = 0
    private var currentDraftFloder: String? = null

    private var stopingFlag = false

    private var recordSettingContainer: View? = null
    private var recordSettingController: RecordSettingController? = null

    private var mMusicSelectContainer: View? = null
    private var musicSelectController: MusicSelectController? = null

    private var dialog: ProgressDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        LogUtils.LOGI(TAG, "onCreate")

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_video_record)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_record)
        builderInit()

        initRecordSDK()

        msc = MediaScannerConnection(this, null)
        msc!!.connect()
    }


    private fun startRecord() {

        recordingFlag = true

        mSettingList!!.visibility = View.INVISIBLE

        // 说明第一次开始
        if (firstClipRecord) {

            mRecorder!!.startRecord()

//            recordTimelineView!!.setMaxDuration(MAX_RECORD_DURATION)
//            recordTimelineView!!.visibility = View.VISIBLE

            btnRecordStart.setMax(MAX_RECORD_DURATION)
            firstClipRecord = false

        } else {
            mRecorder!!.resumeRecord()
        }

    }

    private fun stopRecord() {

        // 可能在录制状态（录制中达到最大录制时间） 或者 非录制状态（录制好 手动下一步）进入这个函数

        //如果已经停止了,防止多次触发
        if (stopingFlag) {
            return
        }
        stopingFlag = true

        mSettingList!!.visibility = View.VISIBLE
        mRecorder!!.stopRecord()

    }

    private fun pauseRecord() {

        mRecorder!!.pauseRecord()
        recordingFlag = false
        //btnRecordStart.setText("录制");
        mSettingList!!.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        mRecorder!!.startPreview()
    }

    override fun onPause() {
        super.onPause()
        if (recordingFlag) {
            // 录制中，退到后台，需要暂停录制，不然视频文件只有声音
            pauseRecord()
        }
        mRecorder!!.stopPreview()

    }

    override fun onDestroy() {

        super.onDestroy()
        mRecorder!!.destroy()

        msc!!.disconnect()

        YYMusicHandler.getInstance().stopPlay()

    }


    private fun setMagicFilterType(filterType: Int) {
        if (mRecorder != null) {
            mRecorder!!.setFilter(filterType)
            //根据需要，还可以通过滤镜名字来设定
            //mRecorder.setFilterByName("Toaster");
        }
    }

    private fun initSettingLayout() {

        recordSettingContainer = findViewById(R.id.record_setting_container)
        recordSettingContainer!!.setOnClickListener { recordSettingContainer!!.visibility = View.GONE }

        recordSettingController = RecordSettingController(this, recordSettingContainer!!, this, this)
        recordSettingController!!.setSpeedCheckListener(object : SpeedRecyclerAdapter.OnSpeedCheckListener {
            override fun onSpeedChecked(pos: Int, speed: String): Boolean {
                if (mRecorder != null) {
                    when (speed) {
                        "slow" -> mRecorder!!.recordSpeed = IVideoRecorder.SpeedType.SLOW
                        "fast" -> mRecorder!!.recordSpeed = IVideoRecorder.SpeedType.FAST
                        else -> mRecorder!!.recordSpeed = IVideoRecorder.SpeedType.STANDARD
                    }
                }
                return true
            }

        })


        // music
        mMusicSelectContainer = findViewById(R.id.mMusicContainer)
        musicSelectController = MusicSelectController(this, mMusicSelectContainer!!)

    }

    override fun onStickerChecked(pos: Int, path: String?): Boolean {

        mRecorder!!.setStickerPath(path)

        return true
    }

    override fun onFilterChecked(pos: Int, path: String): Boolean {
        setMagicFilterType(pos)
        return true
    }

    // 更新进度条以,播放时间和聊天内容
    fun updateProgress(duration: Float, maxDuration: Float) {

        val leftDuration = maxDuration - duration
        if (duration < maxDuration) {
            mRecordTimeTxt!!.text = (duration / 1000).toString()
//            recordTimelineView!!.setDuration(duration.toInt())
            btnRecordStart.setProgress(duration)
            if (duration > 5000) {
                mNextBtn.isEnabled = true
                mNextBtn.setTextColor(resources.getColor(R.color.white))
            } else {
                // 因为删除操作等，duration有可能变小
                mNextBtn.isEnabled = false
                mNextBtn.setTextColor(resources.getColor(R.color.text_disable_color))
            }
        } else {
            // 这个事件会多次触发， onMaxDuration事件 只会触发一次
            // 移动到 onMaxDuration事件
            //stopRecord();
        }

    }


    //倒计时拍摄
    internal inner class MyCountDownTimer(millisInFuture: Long, countDownInterval: Long, private val textView: TextView) : CountDownTimer(millisInFuture, countDownInterval) {

        /**
         * 计时完毕时触发
         */
        override fun onFinish() {
            textView.visibility = View.GONE
            this@RecordActivity.startRecord()
            //btnRecordStart.setText("暂停");
            btnRecordStart.visibility = View.VISIBLE
        }

        /**
         * 计时过程显示
         */
        override fun onTick(millisUntilFinished: Long) {
            textView.isEnabled = false
            //每隔一秒修改一次UI
            textView.text = (millisUntilFinished / 1000).toString()

        }
    }


    private fun scanFile() {
        msc!!.scanFile(outputPath, "video/mp4")
        msc!!.scanFile(outputPathRev, "video/mp4")
    }


    override fun onClick(v: View) {
        if (v === mNextBtn) {

            // 可能在录制中，也可能不在录制中
            // 如果确定录制已结束，可直接调用 导出函数
            stopRecord()

        } else if (v === mLightBtn) {


            if (mRecorder!!.supportLightOn() === false) {
                Toast.makeText(this@RecordActivity, "flash not support", Toast.LENGTH_SHORT).show()
                return
            }

            if (mRecorder!!.isLightOn) {
                mRecorder!!.setLightStatus(false)
            } else {
                mRecorder!!.setLightStatus(true)
            }

            if (mRecorder!!.isLightOn) {
                mLightBtn!!.isSelected = true
                mLightBtn!!.isActivated = true
            } else {
                mLightBtn!!.isSelected = true
                mLightBtn!!.isActivated = false
            }
        } else if (v === mSwitchBtn) {
            if (mRecorder!!.currentCameraId() === 0) {
                mRecorder!!.switchCamera(1)
            } else {
                mRecorder!!.switchCamera(0)
            }

            if (mRecorder!!.isLightOn()) {
                mLightBtn!!.isSelected = true
                mLightBtn!!.isActivated = true
            } else {
                mLightBtn!!.isSelected = true
                mLightBtn!!.isActivated = false
            }
        } else if (v === btnCancel) {

            this@RecordActivity.onBackPressed()

        } else if (v === btnRecordDeleteLast) {
            mRecorder!!.deleteLastVideoClip()
        } else if (v === glSurfaceView) {
            recordSettingContainer!!.visibility = View.INVISIBLE
            btnRecordStart.visibility = View.VISIBLE
        } else if (v === btnTakePicture) {
            mRecorder!!.takePicture { bmp ->
                //根据业务需要，设置照片要保存的位置
                val p = Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DCIM + "/pic001.jpg"
                val file = File(p)
                try {
                    file.createNewFile()
                    val os = BufferedOutputStream(FileOutputStream(file))
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, os)
                    os.flush()
                    os.close()
                    Toast.makeText(applicationContext, "图像保存成功", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        } else if (v === focusImageView) {
            // ontouch 处理？
        }
    }

    override fun onBackPressed() {
        if (firstClipRecord) {     // 还没录制过，直接返回
            super@RecordActivity.onBackPressed()
            return
        }
        val mAlertDialog = AlertDialog.Builder(this@RecordActivity)
                .setTitle("取消拍摄")
                .setMessage("拍摄的内容将不会被保存，确认要取消拍摄吗？")
                .setPositiveButton("是") { _, _ ->
                    // 退出拍摄界面
                    super@RecordActivity.onBackPressed()
                }
                .setNegativeButton("否") { _, _ ->
                    // do nothing
                    // 继续拍摄
                }
                .setNeutralButton("重拍") { _, _ ->
                    // 删除数据后 重新开始拍摄
                    mRecorder!!.deleteAllVideoClips()
                }
                .show()
    }

    override fun initStart() {
    }

    override fun initEvent() {
    }

    override fun initFinish() {

    }

    override fun initView() {

        val params = intent
        fromDraft = params.getBooleanExtra(Constants.KEY_IS_FROM_DRAFT, false)
        recWorkFolder = params.getStringExtra(Constants.REC_WORK_FOLDER)
        musicPath = params.getStringExtra(Constants.MUSIC_FILE)
        musicStartTime = params.getIntExtra(Constants.MUSIC_START_TIME, 0)
        currentDraftFloder = params.getStringExtra(Constants.CURRENT_DRAFT_FLODER)


        mNextBtn = findViewById<View>(R.id.start_live_btn) as TextView
        mNextBtn.setOnClickListener(this)
        mNextBtn.isEnabled = false
        mNextBtn.setTextColor(resources.getColor(R.color.text_disable_color))

        btnCancel = findViewById<View>(R.id.kklive_back) as ImageView
        btnCancel.setOnClickListener(this)

        mLightBtn = findViewById<View>(R.id.btn_light) as ImageView
        mLightBtn!!.setOnClickListener(this)

        mSwitchBtn = findViewById<View>(R.id.btn_switch) as ImageView
        mSwitchBtn!!.setOnClickListener(this)

        btnRecordStart = findViewById<View>(R.id.btn_record_start) as RecordedButton
        btnRecordStart.post(Runnable {
            if (fromDraft) {
                btnRecordStart.pause()
            }
        })

        //btnRecordStart.setOnTouchListener(this);
        btnRecordStart.setOnGestureListener(object : RecordedButton.OnGestureListener {
            override fun onClickStart() {
                //开始
                Log.e("btnRecordStart", "onClickStart")
                startRecord()
            }

            override fun onClickPause() {
                //暂停
                Log.e("btnRecordStart", "onClickPause")
                pauseRecord()
            }

            override fun onClickReset() {
                //重置
                Log.e("btnRecordStart", "onClickReset")
                mRecorder!!.deleteAllVideoClips()
            }

            override fun onProgressOver() {
                //结束
                Log.e("btnRecordStart", "onProgressOver")
                //stopRecord();
            }


        })
        btnRecordDeleteLast = findViewById<View>(R.id.btn_record_delete) as ImageView
        btnRecordDeleteLast.setOnClickListener(this)

        btnTakePicture = findViewById<View>(R.id.btn_take_picture) as TextView
        btnTakePicture!!.setOnClickListener(this)

        mRecordTimeTxt = findViewById<View>(R.id.kklive_record_time) as TextView
//        recordTimelineView = findViewById<View>(R.id.kklive_record_timeline) as RecordTimelineView
//        recordTimelineView!!.setMaxDuration(MAX_RECORD_DURATION)
//        recordTimelineView!!.visibility = View.INVISIBLE

        mSettingList = findViewById<View>(R.id.mSettingList) as RecyclerView
        mSettingList!!.layoutManager = LinearLayoutManager(this.applicationContext, LinearLayoutManager.VERTICAL, false)
        recordSettingRecyclerAdapter = RecordSettingRecyclerAdapter(this)
        recordSettingRecyclerAdapter!!.setSettingItemCheckListener(object : RecordSettingRecyclerAdapter.OnSettingItemCheckListener {
            override fun onSettingItemChecked(pos: Int, settingName: String): Boolean {
                if (settingName.equals("countdown", ignoreCase = true)) {
                    val textView = findViewById<View>(R.id.textview_countdown) as TextView
                    textView.visibility = View.VISIBLE
                    mSettingList!!.visibility = View.INVISIBLE
                    btnRecordStart.visibility = View.INVISIBLE
                    recordSettingContainer!!.visibility = View.INVISIBLE
                    val myCountTimer = MyCountDownTimer((3000 + 300).toLong(), 1000, textView)
                    myCountTimer.start()

                } else {
                    recordSettingContainer!!.visibility = View.VISIBLE
                    btnRecordStart.visibility = View.INVISIBLE
                    if (recordSettingController != null) {
                        recordSettingController!!.setDisplayItem(settingName)
                    }
                }
                return false
            }
        })
        mSettingList!!.adapter = recordSettingRecyclerAdapter

        // 视频画面放大缩小限制，设定值范围为0 到100
        // 0： 原始画面（没有放大） 100： 最大放大
        // 虽然设定范围 0 到100， 但是不代表放大 有 100个等级。实际的放大精度依赖于手机型号。
        zoomControls = findViewById<View>(R.id.zoomControls) as ZoomControls
        zoomControls!!.visibility = View.INVISIBLE

        glSurfaceView = findViewById<View>(R.id.sv_camera) as CameraGLSurfaceView
        //glSurfaceView.setOnTouchListener(this);
        glSurfaceView!!.setOnCameraGLViewListener(object : CameraGLSurfaceView.OnCameraGLViewListener {
            override fun onLongClick() {

            }

            override fun onClick(downX: Float, downY: Float) {
                if (recordSettingContainer!!.visibility == View.VISIBLE) {
                    recordSettingContainer!!.visibility = View.INVISIBLE
                    btnRecordStart.visibility = View.VISIBLE
                }
                // 后置摄像头才对焦
                if (mRecorder!!.currentCameraId() === 0) {
                    mRecorder!!.setFocus(downX, downY, object : IVideoRecorder.OnFocusListener {
                        override fun onFocusSucess() {
                            focusImageView!!.onFocusSuccess()
                        }

                        override fun onFocusFail() {
                            focusImageView!!.onFocusFailed()
                        }
                    })
                    focusImageView!!.startFocus(Point(downX.toInt(), downY.toInt()))
                }
            }
        })


        initSettingLayout()

        focusImageView = findViewById<View>(R.id.focusImageView) as FocusImageView
    }

    private fun initRecordSDK() {

        val recordCallback = object : RecordCallback {

            override fun onPrepared(videoInfo: VideoInfo) {

                // 可以获取上次拍摄的录像，根据需要选择是否继续，还是重新开始
                // 也可以在进入 录制预览画面之前，对app端负责管理的文件夹内容进行判断，提醒用户是否有拍摄需要继续

                if (videoInfo.count > 0 && !fromDraft) {  // 如果是从草稿箱开始就不提示了
                    val mAlertDialog = AlertDialog.Builder(this@RecordActivity)
                            //.setTitle("取消拍摄")
                            .setMessage("你有未编辑完成的视频，是否继续？")
                            .setPositiveButton("确定") { _, _ ->
                                runOnUiThread {
                                    mMusicSelectContainer!!.visibility = View.INVISIBLE
                                    updateProgress(videoInfo.totalDuration.toFloat(), MAX_RECORD_DURATION.toFloat())
                                }
                            }
                            .setNegativeButton("取消") { _, _ -> mRecorder!!.deleteAllVideoClips() }
                            .show()
                }

                // 20180423 编辑前设置logo，会被加特效。
                //setLogoImage(50, 50, 240, 50);

                //mRecorder.setMusicPath(null);
                mRecorder!!.setMusicPath(musicPath, musicStartTime)
                //mRecorder.setVideoSize(576, 1024);
                mRecorder!!.setVideoSize(544, 960)
                mRecorder!!.recordSpeed = IVideoRecorder.SpeedType.STANDARD
                mRecorder!!.maxDuration = MAX_RECORD_DURATION

//                runOnUiThread {
//                    recordTimelineView!!.setMaxDuration(MAX_RECORD_DURATION)
//                    recordTimelineView!!.visibility = View.VISIBLE
//                    updateProgress(videoInfo.totalDuration.toFloat(), MAX_RECORD_DURATION.toFloat())
//                }


            }

            // 一个片段录制结束
            override fun onRecordComplete(validClip: Boolean, clipDuration: Long) {

                LogUtils.LOGI("app", "onRecordComplete")

                // 要停止拍摄（已经录完最后一个片段）， 进行文件导出
                if (stopingFlag) {

                    runOnUiThread {
                        // 导出两个文件  拍摄的正常序 以及 倒序
                        // 导出完成之后，可以根据业务需要，删除工作目录中的临时文件

                        // 2018/4/21 为了方便管理，避免中间文件忘记删除，大量留在手机里，
                        // 录制文件合成的文件也保存在 录制工作目录。（app的专用目录里）
                        // 如果 保存在别的地方，要记得删除。
                        outputPath = recWorkFolder + File.separator + Constants.DRAFT_VIDEO_NAME
                        outputPathRev = outputPath!!.substring(0, outputPath!!.length - 4) + "_rev" + ".mp4"
                        // 导出需要一定时间，界面上最好显示等待画面。
                        dialog = ProgressDialog(this@RecordActivity)
                        dialog!!.setMessage("处理中")
                        dialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                        dialog!!.isIndeterminate = true
                        dialog!!.show()

                        //                            mRecorder.exportTopath(outputPath, outputPathRev); // 这个是异步返回，正真的结束通过回到函数onExportComplete 通知
                        mRecorder!!.exportTopath(outputPath, outputPathRev)
                    }


                }


            }


            override fun onProgress(duration: Long, videoInfo: VideoInfo) {

                Log.e("YYRecordStream", "#record progress duration = $duration")
                runOnUiThread { updateProgress(duration.toFloat(), MAX_RECORD_DURATION.toFloat()) }
            }


            override fun onMaxDuration() {
                LogUtils.LOGI("app", "onMaxDuration ")
                // 可以停止拍摄了
                runOnUiThread {
                    //TODO 录制完毕
                    btnRecordStart.ProgressOver()
                    stopRecord()
                }

            }

            // 导出结束，得到完整的录像文件，可根据需要进入编辑页面
            override fun onExportComplete(ok: Boolean) {
                LogUtils.LOGI("app", "onExportComplete $ok")

                runOnUiThread {
                    dialog!!.dismiss()
                    dialog = null
                    if (ok) {

                        scanFile()

                        // 指定编辑工作目录，用于保存 编辑设定文件 （草稿箱功能）
                        val editWorkFolder = recWorkFolder//ToolUtils.getExternalFilesPath(RecordActivity.this) + "/SV_rec_folder/";

                        val intent = Intent(this@RecordActivity, VideoEditActivity::class.java)
                        intent.putExtra(Constants.VIDEO_FILE, outputPath)
                        intent.putExtra(Constants.MUSIC_FILE, musicPath)
                        intent.putExtra(Constants.EDIT_WORK_FOLDER, editWorkFolder)
                        intent.putExtra(Constants.CURRENT_DRAFT_FLODER, currentDraftFloder)
                        startActivity(intent)

                        // 因为，进入编辑界面后可能还会返回，这边就不会销毁activity，录制状态也回复为可录制状态。 20180418
                        stopingFlag = false

                        //finish();
                    } else {
                        stopingFlag = false        // 如果再次录制，录制接受后不会自动进入 导出 阶段
                        // 因为没有录制文件等原因失败情况，app侧根据业务流程进行处理
                        Toast.makeText(this@RecordActivity, "合成失败，请确认是否拍摄。", Toast.LENGTH_SHORT).show()
                    }
                }


            }

            override fun onError(errorCode: Int) {

            }

            override fun onCameraOpenFailed() {
                runOnUiThread {
                    // 弹窗
                    Toast.makeText(this@RecordActivity, "请检查摄像头权限。", Toast.LENGTH_SHORT).show()
                }

            }
        }

        //
        mRecorder = RecordCreator.getInstance()

        // 录制初始化
        // glSurfaceView app侧自己定义的用于展示视频画面
        // recWorFolder 录制是临时文件保存用的工作目录
        // recordCallback 回调函数

        // 2018/4/21 录制的中间文件不对外公开
        // 并请根据业务需要，择机删除
        //recWorkFolder = ToolUtils.getRecWorkFolder();
        //recWorkFolder = ToolUtils.getExternalFilesPath(this) + File.separator+Constants.REC_WORK_FOLDER;
        mRecorder!!.init(glSurfaceView, recWorkFolder, recordCallback)

    }

    fun setMusicPathFromMusicController(music: String) {
        if (mRecorder != null) {
            musicPath = Environment.getExternalStorageDirectory().toString() + "/VideoRecorderTest/music/" + music
            //SDKSetting.setBgMusic(dest);
            mRecorder!!.setMusicPath(musicPath, 5000)
        }
    }

    fun setBeautyParams(index: Int, percent: Float) {
        if (mRecorder != null) {
            mRecorder!!.setBeautyParams(index, percent)
        }
    }

    fun setThinFace(value: Int) {
        if (mRecorder != null) {
            mRecorder!!.thinFace = value
        }
    }

    fun setBigEye(value: Int) {
        if (mRecorder != null) {
            mRecorder!!.bigEye = value
        }
    }

    companion object {

        private const val TAG = "RecordActivity"

        private const val MSG_UPDATE_PROGRESS = 0

        private const val MAX_RECORD_DURATION = 15 * 1000
    }

    inner class Models {


    }
}
