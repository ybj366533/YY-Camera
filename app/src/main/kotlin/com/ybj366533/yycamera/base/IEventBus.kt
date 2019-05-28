package com.ybj366533.yycamera.base

import org.greenrobot.eventbus.EventBus

/**
 * EventBus 管理接口
 * Created by  Summer on 2019/05/13.
 */
interface IEventBus {

    val eventBusList: MutableSet<Any>

    fun bindEventBus(vararg subscriber: Any) = run {
        eventBusList.addAll(subscriber)
        subscriber.forEach {
            EventBus.getDefault().register(it)
        }
    }

    fun unbindEventBus(subscriber: Any) = run {
        eventBusList.remove(subscriber)
        EventBus.getDefault().unregister(subscriber)
    }

    fun clearEventBus() {
        val iterator = eventBusList.iterator()
        for (element in iterator) {
            EventBus.getDefault().unregister(element)
            iterator.remove()
        }
    }
}