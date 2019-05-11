package com.ybj366533.yycamera

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button

import com.gtv.cloud.gtvideo.ui.CameraGLSurfaceView
import com.gtv.cloud.gtvideo.utils.ToolUtils
import com.gtv.cloud.recorder.GTVRecordCreator2
import com.gtv.cloud.recorder.GTVVideoInfo
import com.gtv.cloud.recorder.IGTVVideoRecorder2
import com.gtv.cloud.recorder.RecordCallback
import com.gtv.cloud.utils.LogUtils

class RecordActivity : AppCompatActivity() {

    internal var cameraGLSurfaceView: CameraGLSurfaceView

    private var mRecorder: IGTVVideoRecorder2? = null

    private var recWorkFolder: String? = null

    private var recording = false
    private var recordBtn: Button? = null

    private val dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        cameraGLSurfaceView = findViewById<View>(R.id.glSurfaceView) as CameraGLSurfaceView
        recordBtn = findViewById<View>(R.id.record_btn) as Button
        recordBtn!!.text = "start"
        recording = false
        recordBtn!!.setOnClickListener {
            if (recording == false) {
                mRecorder!!.startRecord(cameraGLSurfaceView.currentContext)
                cameraGLSurfaceView.setTexutreListener(object : CameraGLSurfaceView.OnTextureListener() {
                    fun onTextureAvailable(textureId: Int, textureWidth: Int, textureHeight: Int, timestampNanos: Long): Int {
                        mRecorder!!.writeVideoFrame(textureId, textureWidth, textureHeight, false, timestampNanos)
                        return 0
                    }
                })
                recordBtn!!.text = "stop"
                recording = true

            } else {
                recordBtn!!.text = "start"
                recording = false
                cameraGLSurfaceView.setTexutreListener(null)
                mRecorder!!.stopRecord()
            }
        }

        initGTVSDK()
    }

    private fun initGTVSDK() {

        val recordCallback = object : RecordCallback() {

            fun onPrepared(gtvVideoInfo: GTVVideoInfo) {

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
            fun onRecordComplete(validClip: Boolean, clipDuration: Long) {

                LogUtils.LOGI("app", "onRecordComplete")

                // 要停止拍摄（已经录完最后一个片段）， 进行文件导出
                //                if(stopingFlag == true) {
                //
                //                    runOnUiThread(new Runnable() {
                //                        @Override
                //                        public void run() {
                //
                //                            // 导出两个文件  拍摄的正常序 以及 倒序
                //                            // 导出完成之后，可以根据业务需要，删除工作目录中的临时文件
                //
                //                            // 2018/4/21 为了方便管理，避免中间文件忘记删除，大量留在手机里，
                //                            // 录制文件合成的文件也保存在 录制工作目录。（app的专用目录里）
                //                            // 如果 保存在别的地方，要记得删除。
                //                            //outputPath = ToolUtils.getNewVideoPath(GTVVideoRecordStreamActivity.this);
                //                            //outputPathRev = outputPath.substring(0, outputPath.length()-4) + "_rev" + ".mp4";
                //                            // 草稿箱：所有文件都放在一个文件夹。
                //                            //outputPath = ToolUtils.getExternalFilesPath(GTVVideoRecordStreamActivity.this) + "/SV_rec_folder" + "/gtv.mp4";
                //                            outputPath = recWorkFolder + "/gtv.mp4";
                //                            outputPathRev = outputPath.substring(0, outputPath.length()-4) + "_rev" + ".mp4";
                //
                //                            // 导出需要一定时间，界面上最好显示等待画面。
                //                            dialog = new ProgressDialog(GTVVideoRecordStreamActivity.this);
                //                            dialog.setMessage("处理中");
                //                            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                //                            dialog.setIndeterminate(true);
                //                            dialog.show();
                //
                //                            mRecorder.exportTopath(outputPath, outputPathRev); // 这个是异步返回，正真的结束通过回到函数onExportComplete 通知
                //                        }
                //                    });
                //
                //
                //                }


            }


            fun onProgress(duration: Long, gtvVideoInfo: GTVVideoInfo) {
                //                Log.e(TAG, "onProgress " + gtvVideoInfo.getCount() + " " + gtvVideoInfo.getTotalDuration() + " " + duration );
                for (i in 0 until gtvVideoInfo.getVideoClipList().size()) {
                    //                    Log.e(TAG, "onProgress " + gtvVideoInfo.getVideoClipList().get(i).getFileName() + " " +  gtvVideoInfo.getVideoClipList().get(i).getDuration());
                }
                //                runOnUiThread(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        updateProgress(duration, MAX_RECORD_DURATION);
                //                    }
                //                });
            }


            fun onMaxDuration() {
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
            fun onExportComplete(ok: Boolean) {
                LogUtils.LOGI("app", "onExportComplete $ok")

                //                runOnUiThread(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        dialog.dismiss();
                //                        dialog = null;
                //                        if (ok == true) {
                //                            // 根据需要，删除工作目录(确保导出目录是不一样)
                //                            // 因为，进入编辑界面后可能还会返回，这边就不删除目录了。 20180418
                ////                            File dir = new File(GTVVideoRecordStreamActivity.this.recWorkFolder);
                ////                            if (dir.exists()) {
                ////                                ToolUtils.deleteDir(dir);
                ////                            }
                //
                //                            scanFile();
                //
                //                            // 指定编辑工作目录，用于保存 编辑设定文件 （草稿箱功能）
                //                            String editWorkFolder = recWorkFolder;//ToolUtils.getExternalFilesPath(GTVVideoRecordStreamActivity.this) + "/SV_rec_folder/";
                //
                //                            Intent intent = new Intent(GTVVideoRecordStreamActivity.this, GTVVideoEditActivity.class);
                //                            intent.putExtra("VIDEO_FILE", outputPath);
                //                            intent.putExtra("MUSIC_FILE", musicPath);
                //                            intent.putExtra("EDIT_WORK_FOLDER",editWorkFolder);
                //                            startActivity(intent);
                //
                //                            // 因为，进入编辑界面后可能还会返回，这边就不会销毁activity，录制状态也回复为可录制状态。 20180418
                //                            stopingFlag = false;
                //
                //                            //finish();
                //                        } else {
                //                            stopingFlag = false;        // 如果再次录制，录制接受后不会自动进入 导出 阶段
                //                            // 因为没有录制文件等原因失败情况，app侧根据业务流程进行处理
                //                            Toast.makeText(GTVVideoRecordStreamActivity.this, "合成失败，请确认是否拍摄。", Toast.LENGTH_SHORT).show();
                //                        }
                //
                //
                //                    }
                //                });


            }

            fun onError(errorCode: Int) {

            }

            fun onCameraOpenFailed() {
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
        mRecorder = GTVRecordCreator2.getInstance()

        // 录制初始化
        recWorkFolder = ToolUtils.getExternalFilesPath(this@RecordActivity) + "/SV_rec_folder"
        mRecorder!!.init(recWorkFolder, recordCallback)

    }
}
