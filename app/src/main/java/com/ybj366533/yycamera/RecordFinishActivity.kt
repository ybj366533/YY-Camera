package com.ybj366533.yycamera

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast

import com.gtv.cloud.editor.EditCallback
import com.gtv.cloud.editor.GTVComposerCreator
import com.gtv.cloud.editor.GTVExtractFrameInfo
import com.gtv.cloud.editor.IGTVVideoComposer
import com.gtv.cloud.editor.IGTVVideoEditor
import com.gtv.cloud.gtvideo.utils.ToolUtils
import com.gtv.cloud.utils.GTVVideoUtils

import java.io.File
import java.util.ArrayList

class RecordFinishActivity : AppCompatActivity() {

    private var btnPublish: Button? = null
    private var btnDraft: Button? = null
    private var btnReturn: Button? = null
    private var btnCancel: Button? = null
    private val workFolder: String? = null


    private var msc: MediaScannerConnection? = null

    private var startFromDraft = false //是否从草稿箱打开

    //    @Override
    //    protected void onSaveInstanceState(Bundle outState){
    //        //LogUtils.LOGI("AAAAAAAAAAAA", "outState");
    //    }

    private var dialog: ProgressDialog? = null
    internal var gtvVideoComposer: IGTVVideoComposer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_finish)

        val params = intent
        startFromDraft = params.getBooleanExtra("DRAFT", false)

        btnReturn = findViewById<View>(R.id.btn_return) as Button
        btnCancel = findViewById<View>(R.id.btn_cancel) as Button

        if (startFromDraft == true) {
            btnReturn!!.text = "返回编辑"
            btnCancel!!.visibility = View.VISIBLE
        }
        btnReturn!!.setOnClickListener { this@RecordFinishActivity.onBackPressed() }
        btnCancel!!.setOnClickListener { super@RecordFinishActivity.onBackPressed() }

        btnPublish = findViewById<View>(R.id.btn_publish) as Button
        btnPublish!!.setOnClickListener {
            // 合成并上传文件（demo中上传文件=移动到相册）
            startCompose()
        }

        btnDraft = findViewById<View>(R.id.btn_draft) as Button
        btnDraft!!.setOnClickListener {
            // 把现在的目录 移动到  草稿箱
            if (startFromDraft == false) {
                val recWorkFolder = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/SV_rec_folder"
                val draftWorkFolder = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/draft/1"    //uuid?

                // 如果草稿箱有东西，删除
                ToolUtils.deleteDir(File(draftWorkFolder))

                ToolUtils.moveFileToDraft(recWorkFolder, draftWorkFolder)
            }


            val intent = Intent(this@RecordFinishActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP    // 清除上次入口页面到这个页面之间的所有页面
            startActivity(intent)
        }

        msc = MediaScannerConnection(this, null)
        msc!!.connect()

    }

    override fun onBackPressed() {
        if (startFromDraft == true) {
            val draftWorkFolder = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/draft/1"

            val intent = Intent(this@RecordFinishActivity, GTVVideoEditActivity::class.java)
            val videoFile = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/draft/1/gtv.mp4"
            intent.putExtra("VIDEO_FILE", videoFile)
            intent.putExtra("EDIT_WORK_FOLDER", draftWorkFolder)
            intent.putExtra("DRAFT", true)
            //intent.putExtra("MUSIC_FILE", musicPath);
            startActivity(intent)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        //LogUtils.LOGI("AAAAAAAAAAAA", "destroy");
        super.onDestroy()
        msc!!.disconnect()
    }

    private fun startCompose() {

        val videoPath = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/SV_rec_folder/gtv.mp4"
        val editWorkFolder = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/SV_rec_folder/"
        val finalVideoPath = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/SV_rec_folder/final.mp4"
        val editCallback = object : EditCallback() {
            fun onInitReady() {

            }

            fun onPlayComplete() {

            }

            fun onError(errorCode: Int) {

            }

            fun onProgress(progress: Int) {
                //LogUtils.LOGI("AAAAAAAAA", " " +progress);
                if (dialog != null) {
                    dialog!!.progress = progress
                }
            }

            fun onComposeFinish(reason: Int) {
                dialog!!.dismiss()
                dialog = null
                //isCompsing = false;
                //                if(msc != null) {
                //                    scanFile(outputPath);
                //                }

                // finish 提前是为了保证，再次启动mainactivity的时候，mainactivity在栈顶，这样singletop方式启动不会重复创建mainactivity
                //finish();

                if (reason == 0) {

                    val srcVideo = ToolUtils.getExternalFilesPath(this@RecordFinishActivity) + "/SV_rec_folder/final.mp4"

                    // 根据需求，进行封面设定
                    val frameInfoList = ArrayList<GTVExtractFrameInfo>()
                    GTVVideoUtils.extractVideoFrame(srcVideo, IGTVVideoEditor.ImageFormat.IMAGE_YUV, "/storage/emulated/0/DCIM/keyframe5/", 1000, 1000 * 10, 5, frameInfoList)
                    if (frameInfoList.size > 0) {
                        GTVVideoUtils.createImageWebp(frameInfoList, "/storage/emulated/0/DCIM/keyframe5/ggg.webp")
                    } else {
                        // 抽帧失败
                    }


                    // 上传文件
                    // final.mp4
                    // 转移到DCIM目录下，并删除
                    //String srcVideo = ToolUtils.getExternalFilesPath(RecordFinishActivity.this) + "/SV_rec_folder/final.mp4";
                    val dstVideo = ToolUtils.getExportVideoPath()

                    val srcFile = File(srcVideo)
                    val dstFile = File(dstVideo)
                    srcFile.renameTo(dstFile)


                    msc!!.scanFile(dstVideo, "video/mp4")
                    val intent = Intent(this@RecordFinishActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP    // 清除上次入口页面到这个页面之间的所有页面
                    startActivity(intent)

                    runOnUiThread { Toast.makeText(applicationContext, "合成完成，可到相册查看 或通过【编辑】继续编辑", Toast.LENGTH_LONG).show() }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "合成被终止了", Toast.LENGTH_LONG).show() }
                }
            }
        }
        gtvVideoComposer = GTVComposerCreator.getInstance()
        gtvVideoComposer!!.init(videoPath, editWorkFolder, editCallback)
        setLogoImage(50, 550, 240, 50)
        val r = Rect(100, 100, 100 + 100, 100 + 100)
        gtvVideoComposer!!.setAnimation(ToolUtils.getExternalFilesPath(this) + File.separator + "animation" + "/wudao/", 60, r)
        //gtvVideoComposer.startCompose(finalVideoPath);

        //        if (isCompsing == true) {
        //            return;
        //        }
        //
        //        isCompsing = true;
        //        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        //        //videoPath="/storage/emulated/0/DCIM/1.mp4"; //todo
        //        // 拍摄的中途文件一般用户不可见， 而最终文件暂定为 可见
        //        //outputPath = videoPath.substring(0, videoPath.length()-4) + "_out_" + timeStamp + ".mp4";
        //        //outputPath = ToolUtils.getExportVideoPath();
        //        outputPath = editWorkFolder+"/final.mp4";

        dialog = ProgressDialog(this@RecordFinishActivity)
        dialog!!.setTitle("合成中")
        dialog!!.setCancelable(false)
        dialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog!!.max = 100
        //        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        //            @Override
        //            public void onCancel(DialogInterface dialog) {
        //                //todo 暂不支持
        //            }
        //        });
        dialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { dialog, which ->
            // 取消合成
            // 这边还是会回调 onComposeFinish(int reason), 但是reason !=0
            if (gtvVideoComposer != null) {
                gtvVideoComposer!!.stopCompose()
            }
        }
        dialog!!.show()

        // 20180424，除了原有的startCompose, 除了原有的startCompose，该接口需要2个参数。
        gtvVideoComposer!!.startCompose(finalVideoPath)


    }

    private fun setLogoImage(x: Int, y: Int, width: Int, height: Int) {
        val logoImage = BitmapFactory.decodeResource(resources, R.mipmap.logo)
        gtvVideoComposer!!.setLogoBitmapAtRect(logoImage, Rect(x, y, x + width, y + height))
        // 设置第二个水印
        gtvVideoComposer!!.setLogoBitmapAtRect2(logoImage, Rect(x, y + 300, x + width, y + height + 300))
    }
}
