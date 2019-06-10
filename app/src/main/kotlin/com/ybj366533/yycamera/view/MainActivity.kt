package com.ybj366533.yycamera.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import com.ybj366533.yycamera.base.utils.singleTasks
import com.ybj366533.yycamera.utils.AssetsHandler
import com.ybj366533.yycamera.utils.Constants
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.base.BaseActivity
import com.ybj366533.yycamera.base.IInitialize
import com.ybj366533.yycamera.base.utils.TranslucentUtils
import com.ybj366533.yycamera.databinding.ActivityMainBinding
import com.ybj366533.yycamera.utils.FileUtils
import com.ybj366533.yycamera.utils.ToolUtils
import com.ybj366533.yycamera.base.utils.onPermissions
import org.jetbrains.anko.newTask

import java.io.File
class MainActivity : BaseActivity(), IInitialize {
    private lateinit var binding: ActivityMainBinding
    companion object {

        private const val TAG = "MainActivity"

        const val MY_RECORD_AUDIO_REQUEST_CODE = 0x0001
        const val MY_CAMERA_REQUEST_CODE = 0x0002
        const val MY_WRITE_EXTERNAL_REQUEST_CODE = 0x0004
        const val MY_READ_PHONE_STATE_REQUEST_CODE = 0x0008
        private const val VIDEO_REQUEST_CODE = 101
        const val KEY_PATH = "video_path"

        fun startAction(context: Context) = Intent(context, MainActivity::class.java).apply {
            singleTasks()
            newTask()
        }
    }

    val models by lazy { Models() }

    private val sleepTime = 2000
    private var start: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
//        TranslucentUtils.TRANSLUCENT_NAVIGATION(window)
        builderInit()


    }

    override fun onStart() {
        super.onStart()
        // auto login mode, make sure all group and conversation is loaed before enter the main screen
        start = System.currentTimeMillis()
//        models.getVersionCfg()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
//        TranslucentUtils.showSystemUI(window)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            Log.e("MainActivity", "urixxx" + data.data!!)

            if (requestCode == VIDEO_REQUEST_CODE) {
                val intent = Intent(this, VideoCropActivity::class.java)
                intent.putExtra(KEY_PATH, FileUtils.getRealFilePath(this, data.data))
                startActivity(intent)
            }


        }
    }

    override fun initStart() {
        val animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 1500
        binding.root.startAnimation(animation)
    }

    override fun initView() {
        binding.tvVersion.text = models.getVersion()
        binding.btnRecord.setOnClickListener {
            val intent = Intent(this@MainActivity, RecordActivity::class.java)
            val recWorkFolder = ToolUtils.getExternalFilesPath(this@MainActivity) + "/" + Constants.REC_WORK_FOLDER
            intent.putExtra(Constants.REC_WORK_FOLDER, recWorkFolder)
            startActivity(intent)
        }

//        binding.btnDraft.setOnClickListener {
//            val intent = Intent(this@MainActivity, DraftListActivity::class.java)
//
//            startActivity(intent)
//        }
//
//        binding.btnPlay.setOnClickListener {
//            /*Intent intent = new Intent(MainActivity.this, YYPlayerDemoActivity.class);
//                        startActivity(intent);
//                        return;*/
//            /*  Intent intent = new Intent(Intent.ACTION_PICK);
//                        intent .setType("video*//*");
//                        startActivityForResult(intent, VIDEO_REQUEST_CODE);*/
//        }
    }

    override fun initEvent() {
        onPermissions(this,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA)
    }

    override fun initFinish() {
        val dest = Environment.getExternalStorageDirectory().toString() + "/YY-Camera"
        val file = File(dest)
        if (!file.exists()) {
            file.mkdir()
        }
        val s = System.currentTimeMillis()
        AssetsHandler.instance.copyFilesFassets(this, "data", dest, ".mp3")
        AssetsHandler.instance.copyFilesFassets(this, "data", dest, ".mp4")




        var path: String? = null
        val dataDir = applicationContext.getExternalFilesDir(null)
        if (dataDir != null) {
            path = dataDir.absolutePath + File.separator + "modelsticker"
        }
        if (path != null)
            AssetsHandler.instance.copyFilesFassets(applicationContext, "modelsticker", path, "*")
    }

    inner class Models {


        /**
         * 获取版本号
         *
         * @return 当前应用的版本号
         */
        fun getVersion(): String {
            return try {
                val manager = packageManager
                val info = manager.getPackageInfo(packageName, 0)
                val version = info.versionName
                "v$version "
            } catch (e: Exception) {
                e.printStackTrace()
                "v2.0.0 "
            }
        }

    }


}
