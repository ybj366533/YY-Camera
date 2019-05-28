package com.ybj366533.yycamera.view

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.ybj366533.videolib.editor.EditCallback
import com.ybj366533.videolib.editor.ComposerCreator
import com.ybj366533.videolib.editor.IVideoComposer
import com.ybj366533.yycamera.utils.Constants
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.utils.FileUtils
import com.ybj366533.yycamera.utils.JsonUtil
import com.ybj366533.yycamera.utils.SPUtils
import com.ybj366533.yycamera.utils.ToolUtils

import java.io.File

/**
 * 发布页面
 */
class PublishActivity : AppCompatActivity() {

    private val btnFinish: TextView? = null
    private val btnCancel: TextView? = null
    private val btnBack: TextView? = null
    private val btnDarft: Button? = null
    private val btnPublish: Button? = null
    private var fromDraft: Boolean = false//是否从草稿箱跳转过来
    private var msc: MediaScannerConnection? = null
    private var draftFloderName: String? = null//草稿箱 草稿文件夹名称
    private var isClickeDrafted: Boolean = false//是否已经点击了草稿按钮
    private var editWorkFloder = ""

    private var DRAFT_DIR_PATH = ""
    private var currentDraftFloder: String? = null//当前草稿箱文件名称

    private val etInfo: EditText? = null

    private var alertDialog: Dialog? = null


    private var dialog: ProgressDialog? = null
    internal var YYVideoComposer: IVideoComposer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_record_finish)


        fromDraft = intent.getBooleanExtra(Constants.KEY_IS_FROM_DRAFT, false)
        editWorkFloder = intent.getStringExtra(Constants.EDIT_WORK_FOLDER)
        currentDraftFloder = intent.getStringExtra(Constants.CURRENT_DRAFT_FLODER)

        //        btnDarft = (Button) findViewById(R.id.btn_draft);
        //        btnCancel = (TextView) findViewById(R.id.btn_cancle);
        //        btnBack = (TextView) findViewById(R.id.publish_back);
        //        btnPublish = (Button) findViewById(R.id.btn_publish);
        //        etInfo = (EditText) findViewById(R.id.etinfo);
        etInfo!!.hint = "input something"

        DRAFT_DIR_PATH = SPUtils[this@PublishActivity, Constants.KEY_DRAFT_DIR, ""].toString()    //uuid?


        //初始化按钮文本

        if (!TextUtils.isEmpty(currentDraftFloder)) {
            btnBack!!.text = "返回编辑"
            btnCancel!!.visibility = View.VISIBLE
        } else {
            btnBack!!.text = ""
            btnCancel!!.visibility = View.INVISIBLE
        }

        msc = MediaScannerConnection(this, null)
        msc!!.connect()

        addListener()

    }

    /**
     * 检测编辑信息是否改变
     */
    private fun checkInfoChanged(): Boolean {
        val info = etInfo!!.text.toString().trim { it <= ' ' }
        return !TextUtils.isEmpty(info)
    }

    private fun showSaveDialog() {
        if (alertDialog == null) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("放弃当前已有更改？")
                    .setNegativeButton("取消") { dialog, which -> dialog.dismiss() }.setPositiveButton("放弃") { dialog, which -> handleBack() }
            alertDialog = builder.create()
        } else {
            alertDialog!!.show()
        }

    }

    private fun addListener() {
        btnPublish!!.setOnClickListener { startCompose() }


        btnDarft!!.setOnClickListener {
            // 把现在的目录 移动到  草稿箱
            if (fromDraft == false && !isClickeDrafted) {

                val recWorkFolder = ToolUtils.getExternalFilesPath(this@PublishActivity) + File.separator + Constants.REC_WORK_FOLDER
                var draftWorkFolder = ""
                if (TextUtils.isEmpty(currentDraftFloder)) {
                    draftFloderName = System.currentTimeMillis().toString() + ""
                    draftWorkFolder = ToolUtils.getExternalFilesPath(this@PublishActivity) + "/" + Constants.VIDEO_DRAFT_DIR_NAME + "/" + draftFloderName
                    ToolUtils.moveFileToDraft(recWorkFolder, draftWorkFolder)
                    //在草稿箱生成参数json文件
                    val jsonPath = draftWorkFolder + "/" + Constants.PARAMS_JSON
                    if (!FileUtils.createFile(draftWorkFolder, Constants.PARAMS_JSON)) {//文件之前不存在
                        val util = JsonUtil("{}")
                        util.putParam("A", "1").putParam("B", "2")
                        FileUtils.writeFileContent(jsonPath, util.jsonString)
                        util.close()
                    }

                }
                // 如果草稿箱有东西，删除
                //ToolUtils.deleteDir(new File(draftWorkFolder));

                isClickeDrafted = true
            }


            val intent = Intent(this@PublishActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP    // 清除上次入口页面到这个页面之间的所有页面
            startActivity(intent)
        }


        btnCancel!!.setOnClickListener { finish() }

        btnBack!!.setOnClickListener { onBackPressed() }
    }

    private fun startCompose() {

        val editCallback = object : EditCallback {
            override fun onInitReady() {

            }

            override fun onPlayComplete() {

            }

            override fun onError(errorCode: Int) {

            }

            override fun onProgress(progress: Int) {
                if (dialog != null) {
                    dialog!!.progress = progress
                }
            }

            override fun onComposeFinish(reason: Int) {
                dialog!!.dismiss()
                dialog = null

                if (reason == 0) {

                    // 上传文件
                    // final.mp4
                    // 转移到DCIM目录下，并删除
                    val dstVideo = ToolUtils.exportVideoPath

                    val srcFile = File("$editWorkFloder/final.mp4")
                    val dstFile = File(dstVideo)
                    srcFile.renameTo(dstFile)


                    msc!!.scanFile(dstVideo, "video/mp4")
                    val intent = Intent(this@PublishActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP    // 清除上次入口页面到这个页面之间的所有页面
                    startActivity(intent)

                    runOnUiThread { Toast.makeText(applicationContext, "合成完成，可到相册查看 或通过【编辑】继续编辑", Toast.LENGTH_LONG).show() }
                } else {
                    runOnUiThread { Toast.makeText(applicationContext, "合成被终止了", Toast.LENGTH_LONG).show() }
                }
            }
        }
        val videoPath = editWorkFloder + "/" + Constants.DRAFT_VIDEO_NAME
        YYVideoComposer = ComposerCreator.getInstance()
        YYVideoComposer!!.init(videoPath, editWorkFloder, editCallback)
        setLogoImage(50, 550, 240, 50)

        dialog = ProgressDialog(this@PublishActivity)
        dialog!!.setTitle("合成中")
        dialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog!!.max = 100
        dialog!!.setButton(DialogInterface.BUTTON_NEGATIVE, "取消") { dialog, which ->
            // 取消合成
            // 这边还是会回调 onComposeFinish(int reason), 但是reason !=0
            if (YYVideoComposer != null) {
                YYVideoComposer!!.stopCompose()
            }
        }
        dialog!!.show()

        val finalVideoPath = "$editWorkFloder/final.mp4"
        Log.e("aaa", "finalVideoPath=$finalVideoPath")
        YYVideoComposer!!.startCompose(finalVideoPath)

    }

    private fun setLogoImage(x: Int, y: Int, width: Int, height: Int) {
        val logoImage = BitmapFactory.decodeResource(resources, R.mipmap.logo)
        YYVideoComposer!!.setLogoBitmapAtRect(logoImage, Rect(x, y, x + width, y + height))
        // 设置第二个水印
        YYVideoComposer!!.setLogoBitmapAtRect2(logoImage, Rect(x, y + 300, x + width, y + height + 300))
    }

    override fun onBackPressed() {
        if (fromDraft) {
            if (checkInfoChanged()) {
                showSaveDialog()
            } else {
                handleBack()
            }
        } else {
            finish()
        }
    }

    private fun handleBack() {

        val intent = Intent(this@PublishActivity, VideoEditActivity::class.java)
        intent.putExtra(Constants.VIDEO_FILE, editWorkFloder + File.separator + Constants.DRAFT_VIDEO_NAME)
        intent.putExtra(Constants.EDIT_WORK_FOLDER, editWorkFloder)
        intent.putExtra(Constants.CURRENT_DRAFT_FLODER, currentDraftFloder)

        intent.putExtra(Constants.KEY_IS_FROM_DRAFT, true)

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        msc!!.disconnect()
    }


}
