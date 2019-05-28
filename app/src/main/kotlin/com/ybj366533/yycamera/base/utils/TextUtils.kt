package com.ybj366533.yycamera.base.utils

import android.support.annotation.ColorInt
import android.text.*
import android.text.style.ClickableSpan
import android.view.View
import java.nio.charset.Charset

/**
 * 文字处理工具类
 * Created by summer on 2017/11/8.
 */

/**
 * 获取字符串字节长度
 * cn = 2 byte, en = 1 byte
 */
inline val String.byteLength get() = toByteArray(CharsetsCN.GB18030).size

/**
 * 中文 = 2字节
 */
class ByteLengthFilter(
        val maxByte: Int) : InputFilter {

    override fun filter(source: CharSequence, start: Int, end: Int,
                        dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        /**
         * source 输入内容
         * start 输入开始位置
         * end 输入结束位置
         * dest 原始内容
         * dstart 当前焦点开始位置 如果结束位置代表删除或替换
         * dend 当前焦点结束位置
         */
        //log("$source, $start, $end, $dest, $dstart, $dend")
        val destLength = dest.toString()
                .apply {
                    if (dstart < dend)
                        removeRange(dstart, dend)
                }.byteLength
        var keep = maxByte - destLength

        val srcStr = source.toString()
                .apply {
                    if (start != 0)
                        removeRange(0, start)
                }
        val srcLength = srcStr.byteLength

        //log("$destLength, $srcLength")
        if (keep <= 0) {
            return ""
        } else if (keep >= srcLength) {
            return null // keep original
        } else {
            var subLength = 0
            var subIndex = start
            srcStr.forEach {
                if (subLength < keep) {
                    //log("${it.toInt()}, ${it.toString().byteLength}")
                    //subLength += it.toString().byteLength
                    if (it.toInt() < 128) {
                        subLength += 1
                    } else {
                        subLength += 2
                    }
                    ++subIndex
                } else {
                    return@forEach
                }
            }
            if (subLength > keep)
                --subIndex

            if (subIndex > 0 && Character.isHighSurrogate(source[subIndex - 1])) {
                --subIndex
                if (subIndex == start) {
                    return ""
                }
            }
            return source.subSequence(start, subIndex)
        }
    }
}

/**
 * 中文编码格式
 */
object CharsetsCN {
    val GB2312 by lazy { Charset.forName("GB2312") } //cn = 2 byte, en = 1 byte
    val GBK by lazy { Charset.forName("GBK") } //cn = 2byte, en = 1 byte
    val GB18030 by lazy { Charset.forName("GB18030") } //cn = 2 byte, en = 1 byte
    val UTF_8 by lazy { Charset.forName("UTF-8") } //cn = 3 byte, en = 1 byte
    val UTF_16 by lazy { Charset.forName("UTF-16") } // all = 4 byte
    val UTF_32 by lazy { Charset.forName("UTF-32") } // all = 8 byte
    val Unicode by lazy { Charset.forName("Unicode") } // all = 4byte
}


// 可点击文本
fun SpannableStringBuilder.append(
        text: CharSequence, @ColorInt color: Int,
        click: (widget: View?) -> Unit) {
    val start = length
    append(text)
    setSpan(object : ClickableSpan() {

        override fun onClick(widget: View?) {
            click.invoke(widget)
        }

        override fun updateDrawState(ds: TextPaint?) {
            super.updateDrawState(ds)
            ds?.color = color
            ds?.isUnderlineText = false
        }
    }, start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun CharSequence.toSpannable(
        @ColorInt color: Int, click: (widget: View?) -> Unit)
        = SpannableString(this).apply {
    setSpan(object : ClickableSpan() {

        override fun onClick(widget: View?) {
            click.invoke(widget)
        }

        override fun updateDrawState(ds: TextPaint?) {
            super.updateDrawState(ds)
            ds?.color = color
            ds?.isUnderlineText = false
        }
    }, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}