package com.ybj366533.yycamera.base.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Window
import android.view.WindowManager

/**
 * DialogUtils
 * Created by summer on 2017/11/2.
 */

// 无标题栏
inline fun Dialog.noTitle() = apply {
    window.addFlags(Window.FEATURE_NO_TITLE)
}

// 透明状态栏
inline fun Dialog.translucentStatus() = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
}

// 透明导航栏
inline fun Dialog.translucentNavigation() = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
}

// 透明窗口
inline fun Dialog.translucentWindows() = apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        with(window){
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }
}

// 透明背景
inline fun Dialog.transparentBackground() = apply {
    with(window){
        setDimAmount(0f) //阴影

        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) //透明背景
    }
}

// 全屏
inline fun Dialog.fillWindow() = apply {
    with(window) {
        decorView.setPadding(0, 0, 0, 0)
        attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        //attributes = attributes
    }
}