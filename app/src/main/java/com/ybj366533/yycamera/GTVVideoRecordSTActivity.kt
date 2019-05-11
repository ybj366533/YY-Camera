package com.ybj366533.yycamera

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaScannerConnection
import android.opengl.EGLContext
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ZoomControls

import com.gtv.cloud.gtvideo.ui.CameraGLSurfaceViewST
import com.gtv.cloud.gtvideo.ui.FocusImageView
import com.gtv.cloud.gtvideo.ui.MusicSelectController
import com.gtv.cloud.gtvideo.ui.RecordSettingControllerST
import com.gtv.cloud.gtvideo.ui.RecordSettingRecyclerAdapterST
import com.gtv.cloud.gtvideo.ui.RecordTimelineView
import com.gtv.cloud.gtvideo.ui.SpeedRecyclerAdapter
import com.gtv.cloud.gtvideo.utils.ToolUtils
import com.gtv.cloud.recorder.GTVRecordSTCreator
import com.gtv.cloud.recorder.GTVVideoInfo
import com.gtv.cloud.recorder.IGTVVideoRecorderST
import com.gtv.cloud.recorder.RecordCallback
import com.gtv.cloud.utils.GTVMusicHandler
import com.gtv.cloud.utils.LogUtils

import java.io.File
import java.util.ArrayList

class GTVVideoRecordSTActivity : AppCompatActivity(), View.OnTouchListener, View.OnClickListener {

    private var glSurfaceView: CameraGLSurfaceViewST? = null
    private var eglContext: EGLContext? = null

    internal var mNextBtn: TextView
    internal var btnCancel: ImageView
    internal var btnRecordStart: TextView
    internal var btnRecordDeleteLast: ImageView

    private var mSwitchBtn: ImageView? = null
    private var mLightBtn: ImageView? = null
    private var zoomControls: ZoomControls? = null

    private var mRecordTimeTxt: TextView? = null
    private var recordTimelineView: RecordTimelineView? = null

    private var msc: MediaScannerConnection? = null

    private var btnTakePicture: TextView? = null
    private var btnPlayAnimation: TextView? = null

    private var focusImageView: FocusImageView? = null

    private var mSettingList: RecyclerView? = null
    private var recordSettingRecyclerAdapter: RecordSettingRecyclerAdapterST? = null


    private var recordingFlag = false

    private var mRecorder: IGTVVideoRecorderST? = null
    private var firstClipRecord = true

    private var recWorkFolder: String? = null
    private var outputPath: String? = null
    private var outputPathRev: String? = null
    private var musicPath: String? = null
    private var musicStartTime: Int = 0

    private var startFromDraft = false //是否从草稿箱打开

    private var stopingFlag = false


    private var recordSettingContainer: View? = null
    private var recordSettingController: RecordSettingControllerST? = null

    private var mMusicSelectContainer: View? = null
    private var musicSelectController: MusicSelectController? = null

    private var dialog: ProgressDialog? = null

    private fun copyAssets() {
        object : AsyncTask() {

            protected override fun doInBackground(params: Array<Any>): Any? {
                // todo 只能拷贝吗
                // 启动个线程， asynctask？
                var dest = Environment.getExternalStorageDirectory().toString() + "/VideoRecorderTest/music"
                var file = File(dest)
                if (!file.exists()) {
                    file.mkdir()
                }
                val s = System.currentTimeMillis()
                AssetsHandler.instance.copyFilesFassets(this@GTVVideoRecordSTActivity, "music", dest, ".mp3")

                dest = Environment.getExternalStorageDirectory().toString() + "/VideoRecorderTest/cover"
                file = File(dest)
                if (!file.exists()) {
                    file.mkdir()
                }

                AssetsHandler.instance.copyFilesFassets(this@GTVVideoRecordSTActivity, "cover", dest, ".png")

                Log.e(TAG, "copyFilesFassets cost " + (System.currentTimeMillis() - s))
                return null
            }

            protected override fun onPostExecute(o: Any) {
                //                resCopy.setVisibility(View.GONE);
            }
        }.execute()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        LogUtils.LOGI(TAG, "onCreate")

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record_st)

        copyAssets()

        // 有2个途径可以打开拍摄页面，用 startFromDraft 区别
        // 1 主页面
        // 2 草稿箱-》编辑页面-》拍摄页面
        val params = intent
        startFromDraft = params.getBooleanExtra("DRAFT", false)
        recWorkFolder = params.getStringExtra("REC_WORK_FOLDER")
        musicPath = params.getStringExtra("MUSIC_FILE")
        musicStartTime = params.getIntExtra("MUSIC_START_TIME", 0)

        initView()
        initGTVSDK()

        glSurfaceView!!.setTexutreListener(object : CameraGLSurfaceViewST.OnTextureListener() {
            fun onTextureAvailable(textureId: Int, textureWidth: Int, textureHeight: Int, timestampNanos: Long): Int {
                if (mRecorder != null) {
                    if (eglContext == null) {
                        eglContext = glSurfaceView!!.currentContext
                        // 在调用播放动画 startAnimation之前，需要设置EGLContext
                        mRecorder!!.setEGLContext(eglContext, textureWidth, textureHeight)
                    }
                    return mRecorder!!.inputVideoFrame(textureId, textureWidth, textureHeight, true, timestampNanos)
                } else {
                    return textureId
                }
                //mRecorder.writeVideoFrame(textureId, textureWidth, textureHeight,false, timestampNanos);
                //return 0;
            }
        })

        msc = MediaScannerConnection(this, null)
        msc!!.connect()
    }


    private fun startRecord() {

        recordingFlag = true

        mSettingList!!.visibility = View.INVISIBLE

        // 说明第一次开始
        if (firstClipRecord == true) {

            mRecorder!!.startRecord(glSurfaceView!!.currentContext)

            recordTimelineView!!.setMaxDuration(MAX_RECORD_DURATION)
            recordTimelineView!!.setVisibility(View.VISIBLE)

            firstClipRecord = false


        } else {

            mRecorder!!.resumeRecord(glSurfaceView!!.currentContext)
        }

    }

    private fun stopRecord() {

        // 可能在录制状态（录制中达到最大录制时间） 或者 非录制状态（录制好 手动下一步）进入这个函数

        //如果已经停止了,防止多次触发
        if (stopingFlag == true) {
            return
        }
        stopingFlag = true

        mSettingList!!.visibility = View.VISIBLE
        mRecorder!!.stopRecord()

    }

    private fun pauseRecord() {

        mRecorder!!.pauseRecord()
        recordingFlag = false
        btnRecordStart.text = "录制"
        mSettingList!!.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        //mRecorder.startPreview();
        //关闭打开摄像头（SDK不再负责）
    }

    override fun onPause() {
        super.onPause()
        if (recordingFlag == true) {
            // 录制中，退到后台，需要暂停录制，不然视频文件只有声音
            pauseRecord()
        }
        //mRecorder.stopPreview();
        //关闭打开摄像头（SDK不再负责）

    }

    override fun onDestroy() {

        super.onDestroy()
        mRecorder!!.destroy()

        msc!!.disconnect()

        GTVMusicHandler.getInstance().stopPlay()

    }

    private fun initSettingLayout() {

        recordSettingContainer = findViewById(R.id.record_setting_container)
        recordSettingContainer!!.setOnClickListener { recordSettingContainer!!.visibility = View.GONE }

        recordSettingController = RecordSettingControllerST(this, recordSettingContainer)
        recordSettingController!!.setSpeedCheckListener(object : SpeedRecyclerAdapter.OnSpeedCheckListener() {
            fun onSpeedChecked(pos: Int, speed: String): Boolean {
                if (mRecorder != null) {
                    if (speed == "slow") {
                        mRecorder!!.setRecordSpeed(IGTVVideoRecorderST.SpeedType.SLOW)
                    } else if (speed == "fast") {
                        mRecorder!!.setRecordSpeed(IGTVVideoRecorderST.SpeedType.FAST)
                    } else {
                        mRecorder!!.setRecordSpeed(IGTVVideoRecorderST.SpeedType.STANDARD)
                    }
                }
                return true
            }

        })


        // music
        mMusicSelectContainer = findViewById(R.id.mMusicContainer)
        musicSelectController = MusicSelectController(this, mMusicSelectContainer)
        musicSelectController!!.SetOnMusicSelectListener(object : MusicSelectController.OnMusicSelectListener() {
            fun onMusicSelected(musicPath: String): Boolean {
                setMusicPathFromMusicController(musicPath)
                return false
            }
        })

    }


    // 更新进度条以,播放时间和聊天内容
    fun updateProgress(duration: Long, maxDuration: Long) {

        val leftDuration = maxDuration - duration
        if (duration < maxDuration) {
            mRecordTimeTxt!!.text = (duration / 1000).toString() + ""
            recordTimelineView!!.setDuration(duration.toInt())

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
            this@GTVVideoRecordSTActivity.startRecord()
            btnRecordStart.text = "暂停"
            btnRecordStart.visibility = View.VISIBLE
        }

        /**
         * 计时过程显示
         */
        override fun onTick(millisUntilFinished: Long) {
            textView.isEnabled = false
            //每隔一秒修改一次UI
            textView.text = (millisUntilFinished / 1000).toString() + ""

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
            //摄像头控制不再sdk负责
        } else if (v === mSwitchBtn) {
            //摄像头控制不再sdk负责
        } else if (v === btnCancel) {

            this@GTVVideoRecordSTActivity.onBackPressed()

        } else if (v === btnRecordDeleteLast) {
            mRecorder!!.deleteLastVideoClip()
        } else if (v === glSurfaceView) {
            recordSettingContainer!!.visibility = View.INVISIBLE
            btnRecordStart.visibility = View.VISIBLE
        } else if (v === btnTakePicture) {

        } else if (v === btnPlayAnimation) {
            //mRecorder.startAnimation(ToolUtils.getExternalFilesPath(this) + File.separator + "animation"+"/hulu/");
            val l = ArrayList<String>()
            l.add(ToolUtils.getExternalFilesPath(this) + File.separator + "animation" + "/hulu/")
            l.add(ToolUtils.getExternalFilesPath(this) + File.separator + "animation" + "/perfect/")
            mRecorder!!.startAnimationGroup(l)
        } else if (v === focusImageView) {
            // ontouch 处理？
        }
    }

    override fun onBackPressed() {
        if (firstClipRecord == true) {     // 还没录制过，直接返回
            super@GTVVideoRecordSTActivity.onBackPressed()
            return
        }
        val mAlertDialog = AlertDialog.Builder(this@GTVVideoRecordSTActivity)
                .setTitle("取消拍摄")
                .setMessage("拍摄的内容将不会被保存，确认要取消拍摄吗？")
                .setPositiveButton("是") { dialog, which ->
                    // 退出拍摄界面
                    super@GTVVideoRecordSTActivity.onBackPressed()
                }
                .setNegativeButton("否") { dialog, which ->
                    // do nothing
                    // 继续拍摄
                }
                .setNeutralButton("重拍") { dialog, which ->
                    // 删除数据后 重新开始拍摄
                    mRecorder!!.deleteAllVideoClips()
                }
                .show()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {


        if (v === btnRecordStart) {

            if (event.action == MotionEvent.ACTION_DOWN) {

                if (recordingFlag == false) {
                    startRecord()
                    // 设置缩放渐变动画
                    val scaleAnimation = ScaleAnimation(1.0f, 1.3f, 1.0f, 1.3f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                    scaleAnimation.repeatCount = -1
                    scaleAnimation.duration = 500
                    btnRecordStart.startAnimation(scaleAnimation)
                } else {
                    pauseRecord()
                }

            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                pauseRecord()
                btnRecordStart.clearAnimation()
            }
        } else if (v === glSurfaceView) {      // todo 需要判断？
            if (recordSettingContainer!!.visibility == View.VISIBLE) {
                recordSettingContainer!!.visibility = View.INVISIBLE
                btnRecordStart.visibility = View.VISIBLE
                return false
            }

            //            // 后置摄像头才对焦
            //            if (mRecorder.currentCameraId() == 0) {
            //                mRecorder.setFocus(event.getX(), event.getY(), new IGTVVideoRecorder.OnFocusListener() {
            //                    @Override
            //                    public void onFocusSucess() {
            //                        focusImageView.onFocusSuccess();
            //                    }
            //
            //                    @Override
            //                    public void onFocusFail() {
            //                        focusImageView.onFocusFailed();
            //                    }
            //                });
            //                focusImageView.startFocus(new Point((int)(event.getRawX()), (int)(event.getRawY())));
            //            }

        }

        return true
    }

    private fun initView() {

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

        btnRecordStart = findViewById<View>(R.id.btn_record_start) as TextView
        btnRecordStart.setOnTouchListener(this)

        btnRecordDeleteLast = findViewById<View>(R.id.btn_record_delete) as ImageView
        btnRecordDeleteLast.setOnClickListener(this)

        btnTakePicture = findViewById<View>(R.id.btn_take_picture) as TextView
        btnTakePicture!!.setOnClickListener(this)

        btnPlayAnimation = findViewById<View>(R.id.btn_play_animation) as TextView
        btnPlayAnimation!!.setOnClickListener(this)

        mRecordTimeTxt = findViewById<View>(R.id.kklive_record_time) as TextView
        recordTimelineView = findViewById<View>(R.id.kklive_record_timeline) as RecordTimelineView
        recordTimelineView!!.setMaxDuration(MAX_RECORD_DURATION)
        recordTimelineView!!.setVisibility(View.INVISIBLE)

        mSettingList = findViewById<View>(R.id.mSettingList) as RecyclerView
        mSettingList!!.layoutManager = LinearLayoutManager(this.applicationContext, LinearLayoutManager.VERTICAL, false)
        recordSettingRecyclerAdapter = RecordSettingRecyclerAdapterST(this)
        recordSettingRecyclerAdapter!!.setSettingItemCheckListener(object : RecordSettingRecyclerAdapterST.OnSettingItemCheckListener() {
            fun onSettingItemChecked(pos: Int, settingName: String): Boolean {
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

        //        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
        //
        //            @Override
        //            public void onClick(View v) {
        //
        //                int zoom = mRecorder.getZoom();
        //                zoom += 10;
        //                if(zoom > 100 ) {
        //                    zoom = 100;
        //                }
        //                mRecorder.setZoom(zoom);
        //
        //                return;
        //            }
        //        });
        //
        //        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
        //
        //            @Override
        //            public void onClick(View v) {
        //
        //                int zoom = mRecorder.getZoom();
        //                zoom -= 10;
        //                if(zoom < 0 ) {
        //                    zoom = 0;
        //                }
        //                mRecorder.setZoom(zoom);
        //
        //                return;
        //            }
        //        });
        zoomControls!!.visibility = View.INVISIBLE

        glSurfaceView = findViewById<View>(R.id.mixView) as CameraGLSurfaceViewST
        glSurfaceView!!.setOnTouchListener(this)



        initSettingLayout()

        focusImageView = findViewById<View>(R.id.focusImageView) as FocusImageView
    }

    private fun initGTVSDK() {

        val recordCallback = object : RecordCallback() {

            fun onPrepared(gtvVideoInfo: GTVVideoInfo) {

                //mRecorder.switchCamera(1);
                // 可以获取上次拍摄的录像，根据需要选择是否继续，还是重新开始
                // 也可以在进入 录制预览画面之前，对app端负责管理的文件夹内容进行判断，提醒用户是否有拍摄需要继续

                if (gtvVideoInfo.getCount() > 0 && startFromDraft == false) {  // 如果是从草稿箱开始就不提示了
                    val mAlertDialog = AlertDialog.Builder(this@GTVVideoRecordSTActivity)
                            //.setTitle("取消拍摄")
                            .setMessage("你有未编辑完成的视频，是否继续？")
                            .setPositiveButton("确定") { dialog, which ->
                                runOnUiThread {
                                    mMusicSelectContainer!!.visibility = View.INVISIBLE
                                    updateProgress(gtvVideoInfo.getTotalDuration(), MAX_RECORD_DURATION.toLong())
                                }
                            }
                            .setNegativeButton("取消") { dialog, which -> mRecorder!!.deleteAllVideoClips() }
                            .show()
                }

                // 20180423 编辑前设置logo，会被加特效。
                //setLogoImage(50, 50, 240, 50);

                //mRecorder.setMusicPath(null);
                mRecorder!!.setMusicPath(musicPath, musicStartTime)
                mRecorder!!.setVideoSize(576, 1024)
                mRecorder!!.setRecordSpeed(IGTVVideoRecorderST.SpeedType.STANDARD)
                mRecorder!!.setMaxDuration(MAX_RECORD_DURATION)

                runOnUiThread {
                    //LogUtils.LOGI("BBBBBBBB", " " + gtvVideoInfo.getTotalDuration());
                    recordTimelineView!!.setMaxDuration(MAX_RECORD_DURATION)
                    recordTimelineView!!.setVisibility(View.VISIBLE)
                    updateProgress(gtvVideoInfo.getTotalDuration(), MAX_RECORD_DURATION.toLong())
                }

            }

            // 一个片段录制结束
            fun onRecordComplete(validClip: Boolean, clipDuration: Long) {

                LogUtils.LOGI("app", "onRecordComplete")

                // 要停止拍摄（已经录完最后一个片段）， 进行文件导出
                if (stopingFlag == true) {

                    runOnUiThread {
                        // 导出两个文件  拍摄的正常序 以及 倒序
                        // 导出完成之后，可以根据业务需要，删除工作目录中的临时文件

                        // 2018/4/21 为了方便管理，避免中间文件忘记删除，大量留在手机里，
                        // 录制文件合成的文件也保存在 录制工作目录。（app的专用目录里）
                        // 如果 保存在别的地方，要记得删除。
                        //outputPath = ToolUtils.getNewVideoPath(GTVVideoRecordStreamActivity.this);
                        //outputPathRev = outputPath.substring(0, outputPath.length()-4) + "_rev" + ".mp4";
                        // 草稿箱：所有文件都放在一个文件夹。
                        //outputPath = ToolUtils.getExternalFilesPath(GTVVideoRecordStreamActivity.this) + "/SV_rec_folder" + "/gtv.mp4";
                        outputPath = recWorkFolder!! + "/gtv.mp4"
                        outputPathRev = outputPath!!.substring(0, outputPath!!.length - 4) + "_rev" + ".mp4"

                        // 导出需要一定时间，界面上最好显示等待画面。
                        dialog = ProgressDialog(this@GTVVideoRecordSTActivity)
                        dialog!!.setMessage("处理中")
                        dialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                        dialog!!.isIndeterminate = true
                        dialog!!.show()

                        mRecorder!!.exportTopath(outputPath, outputPathRev) // 这个是异步返回，正真的结束通过回到函数onExportComplete 通知
                    }


                }


            }


            fun onProgress(duration: Long, gtvVideoInfo: GTVVideoInfo) {
                //                Log.e(TAG, "onProgress " + gtvVideoInfo.getCount() + " " + gtvVideoInfo.getTotalDuration() + " " + duration );
                for (i in 0 until gtvVideoInfo.getVideoClipList().size()) {
                    //                    Log.e(TAG, "onProgress " + gtvVideoInfo.getVideoClipList().get(i).getFileName() + " " +  gtvVideoInfo.getVideoClipList().get(i).getDuration());
                }
                runOnUiThread { updateProgress(duration, MAX_RECORD_DURATION.toLong()) }
            }


            fun onMaxDuration() {
                LogUtils.LOGI("app", "onMaxDuration ")
                // 可以停止拍摄了
                runOnUiThread { stopRecord() }

            }

            // 导出结束，得到完整的录像文件，可根据需要进入编辑页面
            fun onExportComplete(ok: Boolean) {
                LogUtils.LOGI("app", "onExportComplete $ok")

                runOnUiThread {
                    dialog!!.dismiss()
                    dialog = null
                    if (ok == true) {
                        // 根据需要，删除工作目录(确保导出目录是不一样)
                        // 因为，进入编辑界面后可能还会返回，这边就不删除目录了。 20180418
                        //                            File dir = new File(GTVVideoRecordStreamActivity.this.recWorkFolder);
                        //                            if (dir.exists()) {
                        //                                ToolUtils.deleteDir(dir);
                        //                            }

                        scanFile()

                        // 指定编辑工作目录，用于保存 编辑设定文件 （草稿箱功能）
                        val editWorkFolder = recWorkFolder//ToolUtils.getExternalFilesPath(GTVVideoRecordStreamActivity.this) + "/SV_rec_folder/";

                        val intent = Intent(this@GTVVideoRecordSTActivity, GTVVideoEditActivity::class.java)
                        intent.putExtra("VIDEO_FILE", outputPath)
                        intent.putExtra("MUSIC_FILE", musicPath)
                        intent.putExtra("EDIT_WORK_FOLDER", editWorkFolder)
                        startActivity(intent)

                        // 因为，进入编辑界面后可能还会返回，这边就不会销毁activity，录制状态也回复为可录制状态。 20180418
                        stopingFlag = false

                        //finish();
                    } else {
                        stopingFlag = false        // 如果再次录制，录制接受后不会自动进入 导出 阶段
                        // 因为没有录制文件等原因失败情况，app侧根据业务流程进行处理
                        Toast.makeText(this@GTVVideoRecordSTActivity, "合成失败，请确认是否拍摄。", Toast.LENGTH_SHORT).show()
                    }
                }


            }

            fun onError(errorCode: Int) {

            }

            fun onCameraOpenFailed() {
                runOnUiThread {
                    // 弹窗
                    Toast.makeText(this@GTVVideoRecordSTActivity, "请检查摄像头权限。", Toast.LENGTH_SHORT).show()
                }

            }
        }

        //
        mRecorder = GTVRecordSTCreator.getInstance()

        // 录制初始化
        // glSurfaceView app侧自己定义的用于展示视频画面
        // recWorFolder 录制是临时文件保存用的工作目录
        // recordCallback 回调函数

        // 2018/4/21 录制的中间文件不对外公开
        // 并请根据业务需要，择机删除
        //recWorkFolder = ToolUtils.getRecWorkFolder();
        // 20180512 有2个不同途径进入拍摄页面，拍摄工作目录会不一样，同一为 启动activity时指定
        //recWorkFolder = ToolUtils.getExternalFilesPath(this) + "/SV_rec_folder";
        //mRecorder.init(glSurfaceView, recWorkFolder, recordCallback);
        mRecorder!!.init(recWorkFolder, recordCallback)


        //            mRecorder.setTexutreListener(new IGTVVideoRecorder.OnTextureListener() {
        //
        //                GTVGroupFilter grpFilter;
        //            @Override
        //            public int onTextureAvailable(int textureId, int textureWidth, int textureHeight) {
        //                if (grpFilter == null) {
        //                    GTVImageGrayscaleFilter gtvImageGrayscaleFilter = new GTVImageGrayscaleFilter();
        //                    List<GTVImageFilter> list = new ArrayList<GTVImageFilter>();
        //                    list.add(gtvImageGrayscaleFilter);
        //
        //                    grpFilter = new GTVGroupFilter(list);
        //                    grpFilter.init();
        //                    grpFilter.onInputSizeChanged(textureWidth, textureHeight);
        //                    grpFilter.onDisplaySizeChanged(textureWidth, textureHeight);
        //                }
        //
        //                int id = grpFilter.onDrawFrame(textureId);
        //                return id;
        //            }
        //        });


    }

    fun setMusicPathFromMusicController(music: String) {
        if (mRecorder != null) {
            musicPath = Environment.getExternalStorageDirectory().toString() + "/VideoRecorderTest/music/" + music
            //SDKSetting.setBgMusic(dest);
            mRecorder!!.setMusicPath(musicPath, 5000)
        }
    }

    companion object {

        private val TAG = "RecActivity"

        private val MSG_UPDATE_PROGRESS = 0

        private val MAX_RECORD_DURATION = 15 * 1000
    }

}
