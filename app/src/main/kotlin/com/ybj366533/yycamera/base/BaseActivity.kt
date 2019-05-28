package com.ybj366533.yycamera.base

import android.os.Bundle
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity



/**
 * BaseActivity
 * Created by Summer on 2019/05/13.
 */
open class BaseActivity : RxAppCompatActivity(), IEventBus {
    override val eventBusList: MutableSet<Any> = mutableSetOf()

    val activity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearEventBus()
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }
}