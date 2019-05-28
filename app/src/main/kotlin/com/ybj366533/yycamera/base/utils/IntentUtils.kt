package com.ybj366533.yycamera.base.utils

import android.content.Intent

/**
 * Intent 扩展
 * Created by summer on 2017/10/30.
 */

/**
 * 栈顶复用
 * 回调 onNewIntent()
 */
fun Intent.singleTop() = apply {
    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

/**
 * 栈内复用
 * 回调 onNewIntent()
 */
inline fun Intent.singleTasks() = apply {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

/**
 * 清空栈顶、自身
 * 回调 onCreate()
 */
inline fun Intent.clearTops() = apply {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * 清空栈
 * 销毁 singleInstance 模式外的全部 Activity
 * 回调 onCreate()
 */
inline fun Intent.clearTasks() = apply {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * 不执行转场动画
 */
inline fun Intent.noAnim() = apply {
    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
}

/**
 * 跳转桌面(模拟 HOME 键)
 */
val launcherIntent = run {
    Intent(Intent.ACTION_MAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addCategory(Intent.CATEGORY_HOME)
}

/**
 * 桌面主页
 */
val launcherHome = launcherIntent.clearTasks()