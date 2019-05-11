package com.ybj366533.yycamera

import android.app.Application
import android.graphics.drawable.Drawable

import com.bumptech.glide.Glide
import com.bumptech.glide.load.ResourceDecoder
import com.gtv.cloud.gtvideo.widget.webp.WebpResourceDecoder

import java.io.InputStream

/**
 * Created by Summser on 2018/4/10.
 */

class GTVApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //TODO Webp 支持初始化
        // Glide支持
        val decoder = WebpResourceDecoder(this)
        Glide.get(this).registry.append<InputStream, Drawable>(InputStream::class.java, Drawable::class.java, decoder)

        // 初始化配置Fresco
        //        Fresco.initialize(this);
    }
}
