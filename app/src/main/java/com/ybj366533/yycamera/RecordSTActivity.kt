package com.ybj366533.yycamera

import android.app.ProgressDialog
import android.content.Intent
import android.media.MediaScannerConnection
import android.opengl.EGLContext
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast

import com.gtv.cloud.recorder.GTVRecordSTCreator
import com.gtv.cloud.recorder.GTVVideoInfo
import com.gtv.cloud.recorder.IGTVVideoRecorderST
import com.gtv.cloud.recorder.RecordCallback
import com.gtv.cloud.utils.LogUtils
import com.ybj366533.yycamera.ui.CameraGLSurfaceViewST
import com.ybj366533.yycamera.utils.ToolUtils

import java.io.File
import java.util.ArrayList


class RecordSTActivity : AppCompatActivity() {

    internal lateinit var cameraGLSurfaceView: CameraGLSurfaceViewST

    private var mRecorder: IGTVVideoRecorderST? = null

    private var recWorkFolder: String? = null

    private var recording = false
    private var recordBtn: Button? = null
    private var btnPlayAnimation: Button? = null

    private var eglContext: EGLContext? = null
    private var outputPath: String? = null
    private var outputPathRev: String? = null
    private var msc: MediaScannerConnection? = null

    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_st)

        cameraGLSurfaceView = findViewById<View>(R.id.glSurfaceView) as CameraGLSurfaceViewST
        recordBtn = findViewById<View>(R.id.record_btn) as Button
        recordBtn!!.text = "start"
        recording = false
        recordBtn!!.setOnClickListener {
            if (recording == false) {
                mRecorder!!.startRecord(cameraGLSurfaceView.currentContext)
                //                    cameraGLSurfaceView.setTexutreListener(new CameraGLSurfaceView.OnTextureListener() {
                //                        @Override
                //                        public int onTextureAvailable(int textureId, int textureWidth, int textureHeight, long timestampNanos) {
                //                            mRecorder.writeVideoFrame(textureId, textureWidth, textureHeight,false, timestampNanos);
                //                            return 0;
                //                        }
                //                    });
                recordBtn!!.text = "stop"
                recording = true

            } else {
                recordBtn!!.text = "start"
                cameraGLSurfaceView.setTexutreListener(null)
                mRecorder!!.stopRecord()
            }
        }
        btnPlayAnimation = findViewById<View>(R.id.btn_play_animation) as Button
        btnPlayAnimation!!.setOnClickListener {
            if (mRecorder != null) {
                //mRecorder.setEGLContext(cameraGLSurfaceView.currentContext, 720, 1280);
                val l = ArrayList<String>()
                l.add(ToolUtils.getExternalFilesPath(this@RecordSTActivity) + File.separator + "animation" + "/hulu/")
                l.add(ToolUtils.getExternalFilesPath(this@RecordSTActivity) + File.separator + "animation" + "/perfect/")
                mRecorder!!.startAnimationGroup(l)
            }
        }

        initGTVSDK()
        cameraGLSurfaceView.setTexutreListener(object : CameraGLSurfaceViewST.OnTextureListener {
            override fun onTextureAvailable(textureId: Int, textureWidth: Int, textureHeight: Int, timestampNanos: Long): Int {
                return if (mRecorder != null) {
                    if (eglContext == null) {
                        eglContext = cameraGLSurfaceView.currentContext
                        // 在调用播放动画 startAnimation之前，需要设置EGLContext
                        mRecorder!!.setEGLContext(eglContext, textureWidth, textureHeight)
                    }
                    mRecorder!!.inputVideoFrame(textureId, textureWidth, textureHeight, false, timestampNanos)
                } else {
                    textureId
                }
            }
        })

        msc = MediaScannerConnection(this, null)
        msc!!.connect()
    }

    private fun initGTVSDK() {

        val recordCallback = object : RecordCallback {

            override fun onPrepared(gtvVideoInfo: GTVVideoInfo) {

                //                //mRecorder.switchCamera(1);
                //                // 可以获取上次拍摄的录像，根据需要选择是否继续，还是重新开始
                //                // 也可以在进入 录制预览画面之前，对app端负责管理的文件夹内容进行判断，提醒用户是否有拍摄需要继续
                //
                //                if (gtvVideoInfo.getCount()>0 && startFromDraft== false) {  // 如果是从草稿箱开始就不提示了
                //                    AlertDialog mAlertDialog = new AlertDialog.Builder(GTVVideoRecordStreamActivity.this)
                //                            //.setTitle("取消拍摄")
                //                            .setMessage("你有未编辑完成的视频，是否继续？")
                //                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                //                                @Override
                //                                public void onClick(DialogInterface dialog, int which) {
                //                                    runOnUiThread(new Runnable() {
                //                                        @Override
                //                                        public void run() {
                //                                            mMusicSelectContainer.setVisibility(View.INVISIBLE);
                //                                            updateProgress(gtvVideoInfo.getTotalDuration(), MAX_RECORD_DURATION);
                //                                        }
                //                                    });
                //                                }
                //                            })
                //                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                //                                @Override
                //                                public void onClick(DialogInterface dialog, int which) {
                //                                    mRecorder.deleteAllVideoClips();
                //                                }
                //                            })
                //                            .show();
                //                }
                //
                //                // 20180423 编辑前设置logo，会被加特效。
                //                //setLogoImage(50, 50, 240, 50);
                //
                //                //mRecorder.setMusicPath(null);
                //                mRecorder.setMusicPath(musicPath, musicStartTime);
                //                mRecorder.setVideoSize(576, 1024);
                //                mRecorder.setRecordSpeed(IGTVVideoRecorder.SpeedType.STANDARD);
                //                mRecorder.setMaxDuration(MAX_RECORD_DURATION);
                //
                //                runOnUiThread(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        //LogUtils.LOGI("BBBBBBBB", " " + gtvVideoInfo.getTotalDuration());
                //                        recordTimelineView.setMaxDuration((int)MAX_RECORD_DURATION);
                //                        recordTimelineView.setVisibility(View.VISIBLE);
                //                        updateProgress(gtvVideoInfo.getTotalDuration(), MAX_RECORD_DURATION);
                //                    }
                //                });

            }

            // 一个片段录制结束
            override fun onRecordComplete(validClip: Boolean, clipDuration: Long) {

                LogUtils.LOGI("app", "onRecordComplete")

                //要停止拍摄（已经录完最后一个片段）， 进行文件导出
                if (recording == true) {

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
                        dialog = ProgressDialog(this@RecordSTActivity)
                        dialog!!.setMessage("处理中")
                        dialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                        dialog!!.isIndeterminate = true
                        dialog!!.show()

                        mRecorder!!.exportTopath(outputPath, outputPathRev) // 这个是异步返回，正真的结束通过回到函数onExportComplete 通知
                    }


                }


            }


            override fun onProgress(duration: Long, gtvVideoInfo: GTVVideoInfo) {
                //                Log.e(TAG, "onProgress " + gtvVideoInfo.getCount() + " " + gtvVideoInfo.getTotalDuration() + " " + duration );
                for (i in 0 until gtvVideoInfo.videoClipList.size) {
                    //                    Log.e(TAG, "onProgress " + gtvVideoInfo.getVideoClipList().get(i).getFileName() + " " +  gtvVideoInfo.getVideoClipList().get(i).getDuration());
                }
                //                runOnUiThread(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        updateProgress(duration, MAX_RECORD_DURATION);
                //                    }
                //                });
            }


            override fun onMaxDuration() {
                LogUtils.LOGI("app", "onMaxDuration ")
                // 可以停止拍摄了
                //                runOnUiThread(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        stopRecord();
                //                    }
                //                });

            }

            // 导出结束，得到完整的录像文件，可根据需要进入编辑页面
            override fun onExportComplete(ok: Boolean) {
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

                        val intent = Intent(this@RecordSTActivity, GTVVideoEditActivity::class.java)
                        intent.putExtra("VIDEO_FILE", outputPath)
                        intent.putExtra("EDIT_WORK_FOLDER", editWorkFolder)
                        startActivity(intent)

                        // 因为，进入编辑界面后可能还会返回，这边就不会销毁activity，录制状态也回复为可录制状态。 20180418
                        recording = false

                        //finish();
                    } else {
                        recording = false        // 如果再次录制，录制接受后不会自动进入 导出 阶段
                        // 因为没有录制文件等原因失败情况，app侧根据业务流程进行处理
                        Toast.makeText(this@RecordSTActivity, "合成失败，请确认是否拍摄。", Toast.LENGTH_SHORT).show()
                    }
                }


            }

            override fun onError(errorCode: Int) {

            }

            override fun onCameraOpenFailed() {
                //                runOnUiThread(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        // 弹窗
                //                        Toast.makeText(GTVVideoRecordStreamActivity.this, "请检查摄像头权限。", Toast.LENGTH_SHORT).show();
                //                    }
                //                });

            }
        }

        //
        //mRecorder = GTVRecordCreator.getInstance();
        mRecorder = GTVRecordSTCreator.getInstance()

        // 录制初始化
        recWorkFolder = ToolUtils.getExternalFilesPath(this@RecordSTActivity) + "/SV_rec_folder"
        mRecorder!!.init(recWorkFolder, recordCallback)

    }

    private fun scanFile() {
        msc!!.scanFile(outputPath, "video/mp4")
        msc!!.scanFile(outputPathRev, "video/mp4")
    }

    override fun onResume() {
        super.onResume()
        //        mRecorder.startPreview();
    }

    override fun onPause() {
        super.onPause()
        //        if(recordingFlag == true) {
        //            // 录制中，退到后台，需要暂停录制，不然视频文件只有声音
        //            pauseRecord();
        //        }
        //        mRecorder.stopPreview();

    }

    override fun onDestroy() {

        super.onDestroy()
        mRecorder!!.destroy()
        msc!!.disconnect()


    }
}
