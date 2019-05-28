@file:Suppress("NOTHING_TO_INLINE")

package com.ybj366533.yycamera.base.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.RequestOptions.*
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import me.iwf.photopicker.PhotoPicker
import java.io.File
import java.util.*

/**
 * 图片处理
 * Created by wittyneko on 2017/11/10.
 */

val REQUEST_CODE = PhotoPicker.REQUEST_CODE
val REQUEST_CROP_CODE = PhotoPicker.REQUEST_CODE + 1
private val CROP_SIZE = 1000

inline fun Context.imageDownload(url: Any, crossinline block: (file: File?) -> Unit) = run {

    Glide.with(this)
            .download(url)
            .into(object : SimpleTarget<File>() {
                override fun onResourceReady(resource: File?, transition: Transition<in File>?) {
                    block(resource)
                }
            })
}

@SuppressLint("CheckResult")
inline fun Context.imageLoad(url: Any, vararg transformations: Transformation<Bitmap>) = run {
    imageLoad(url).apply {
            if (transformations.isNotEmpty()) {
                apply(RequestOptions().transform(MultiTransformation(*transformations)))
            }
    }
}

@SuppressLint("CheckResult")
inline fun Context.imageLoad(url: Any) = run {
    Glide.with(this)
            .load(url)
}

@SuppressLint("CheckResult")
inline fun Context.imageLoad(url: Any, imgView: ImageView, vararg transformations: Transformation<Bitmap>) = run {
    imageLoad(url, imgView, {
        if (transformations.isNotEmpty()) {
            apply(RequestOptions().transform(MultiTransformation(*transformations)))
        }
    })
}

inline fun Context.imageLoad(url: Any, imgView: ImageView, block: RequestBuilder<Drawable>.() -> Unit) = run {
    Glide.with(this)
            .load(url)
            .also {
                it.block()
            }
            .into(imgView)
}



/**
 * 图片加载 缩略图
 */
@SuppressLint("CheckResult")
inline fun Context.imageLoadThumbnail(url: Any, imgView: ImageView) = run {
    imageLoad(url, imgView) {
        val options = RequestOptions().transform(RoundedCorners(25))
        apply(options)
        apply(overrideOf(300, 200))

    }
}

/**
 * 图片加载 圆角
 */
@SuppressLint("CheckResult")
inline fun Context.imageLoadRoundedCorners(url: Any, imgView: ImageView) = run {
    imageLoad(url, imgView) {
        val options = RequestOptions().transform(RoundedCorners(30))
        apply(options)
    }
}

/**
 * 图片加载 圆形图片
 */
@SuppressLint("CheckResult")
inline fun Context.imageLoadCircleCrop(url: Any, imgView: ImageView) = run {
    imageLoad(url, imgView) {
        apply(circleCropTransform())
    }
}

@SuppressLint("CheckResult")
inline fun Context.imageLoad(url: Any, target: DrawableImageViewTarget, vararg transformations: Transformation<Bitmap>) = run {
    imageLoad(url, target, {
        if (transformations.isNotEmpty()) {
            apply(RequestOptions().transform(MultiTransformation(*transformations)))
        }
    })
}

inline fun Context.imageLoad(url: Any, target: DrawableImageViewTarget, block: RequestBuilder<Drawable>.() -> Unit) = run {
    Glide.with(this)
            .load(url)
            .also {
                it.block()
            }
            .into(target)
}

inline fun RequestOptions.multiTransform(vararg transformations: Transformation<Bitmap>) = this.transform(MultiTransformation(*transformations))

fun Activity.imageSelect(count: Int = 1, camera: Boolean = true, preview: Boolean = true,
                         selected: ArrayList<String> = arrayListOf(), request: Int = REQUEST_CODE) {
    PhotoPicker.builder()
            .setPhotoCount(count)       // 最大数量
            .setShowCamera(camera)      // 显示拍照
            .setPreviewEnabled(preview) // 可预览大图
            .setSelected(selected)      // 已选中列表
            .start(this, request)
}

fun Activity.imageSelectResult(requestCode: Int, resultCode: Int, data: Intent?, block: (imageList: ArrayList<String>) -> Unit) {
    if (resultCode == Activity.RESULT_OK) {
        when (requestCode) {
            REQUEST_CODE -> {
                val list = data?.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS)
                        ?: arrayListOf()
                block(list)
            }
        }
    }
}


// 获取图片宽高
fun getImageBounds(path: String) = run {
    try {
        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = Bitmap.Config.RGB_565
        opts.inJustDecodeBounds = true // 只读边界不读内容
        BitmapFactory.decodeFile(path, opts)
        //log("bounds: ${opts.outWidth}, ${opts.outHeight}")
        Point(opts.outWidth, opts.outHeight)
    } catch (e: Throwable) {
        Point()
    }
}
