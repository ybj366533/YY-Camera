package com.ybj366533.yycamera.base

import com.trello.rxlifecycle2.components.support.RxFragment

/**
 * BaseFragment
 * Created by Summer on 2019/05/13.
 */
open class BaseFragment: RxFragment(), IEventBus {

    override val eventBusList: MutableSet<Any> = mutableSetOf()


    override fun onDestroy() {
        super.onDestroy()
        clearEventBus()
    }
}