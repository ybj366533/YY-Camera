package com.ybj366533.yycamera.base.utils

import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * 沉浸式 工具类
 * Created by YBJ on 17-6-28.
 */

object TranslucentUtils {


    fun TRANSLUCENT_STATUS(window: Window) {
        //透明状态栏
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }

    fun TRANSLUCENT_NAVIGATION(window: Window) {
        //透明导航栏
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }

    fun INPUT_ADJUST_PAN(window: Window) {
        //软键盘设置 把整个Layout顶上去露出获得焦点的EditText,不压缩多余空间
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }
    fun INPUT_ADJUST_RESIZE(window: Window) {
        //软键盘设置 整个Layout重新编排,重新分配多余空间
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    /**
     * 使用STICKY的沉浸模式
     * http://www.jcodecraeer.com/a/anzhuokaifa/developer/2014/1117/1997.html
     *
     * @param window
     */
    fun showSystemUI(window: Window) {
        //隐藏虚拟按键，并且全屏

        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /**
     * 使用非STICKY的沉浸模式
     *
     * @param mDecorView
     */

    // This snippet hides the system bars.
    fun hideSystemUI(window: Window) {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        val mDecorView = window.decorView
        mDecorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    fun showSystemUI(mDecorView: View) {
        mDecorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}
