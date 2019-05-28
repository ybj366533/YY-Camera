package com.ybj366533.yycamera.base.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * 输入法 Utils
 * Created by summer on 2018/1/15.
 */

fun View.showSoftInput() = run {
    val inputMethodManager = context.inputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideSoftInput() {
    context.hideSoftInput()
}

fun Context.hideSoftInput() {
    inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
}

val Context.inputMethodManager get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager