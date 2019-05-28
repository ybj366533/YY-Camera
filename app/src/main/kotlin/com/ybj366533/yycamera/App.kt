package com.ybj366533.yycamera

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex


/**
 * APP
 * Created by summer on 2019/05/19.
 */
open class App : Application() {
    val tempCache by lazy { hashMapOf<String, Any>() }
    var url: String = ""

    companion object {
        lateinit var instance: App
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
//
//        val accessToken = app.userPreferences.accessToken
//        val tokenType = app.userPreferences.tokenType
//        log("accessToken:$accessToken" + "tokenType:$tokenType")

    }


}