package com.ybj366533.yycamera.view

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.animation.AlphaAnimation

import android.os.SystemClock.sleep
import com.ybj366533.yycamera.base.utils.TranslucentUtils
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.base.BaseActivity
import com.ybj366533.yycamera.base.IInitialize
import com.ybj366533.yycamera.databinding.ActivitySplashBinding


/**
 * 开屏页
 */
class SplashActivity : BaseActivity(), IInitialize {

    lateinit var binding: ActivitySplashBinding
    val models by lazy { Models() }

    private val sleepTime = 2000
    private var start: Long = 0


    override fun onCreate(arg0: Bundle?) {
        super.onCreate(arg0)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        builderInit()
    }

    override fun onResume() {
        super.onResume()
        Thread(Runnable {
            var costTime = System.currentTimeMillis() - start
            while (sleepTime - costTime > 0) {
                sleep(sleepTime - costTime)
                costTime = System.currentTimeMillis() - start
            }
            startActivity(MainActivity.startAction(activity))
            finish()
        }).start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        TranslucentUtils.showSystemUI(window)
    }

    override fun initStart() {
        TranslucentUtils.TRANSLUCENT_NAVIGATION(window)
        val animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 1500
        binding.root.startAnimation(animation)
    }

    override fun onStart() {
        super.onStart()
        // auto login mode, make sure all group and conversation is loaed before enter the main screen
        start = System.currentTimeMillis()
//        models.getVersionCfg()
    }


    override fun initView() {
        binding.tvVersion.text = models.getVersion()

    }

    override fun initEvent() {
    }

    override fun initFinish() {
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
