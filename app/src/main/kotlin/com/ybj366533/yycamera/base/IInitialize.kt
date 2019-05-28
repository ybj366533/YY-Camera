package com.ybj366533.yycamera.base

/**
 * 初始化接口
 * Created by  Summer on 2019/05/13.
 */
interface IInitialize {
    fun initStart()
    fun initView()
    fun initEvent()
    fun initFinish()

    fun builderInit(){
        initStart()
        initView()
        initEvent()
        initFinish()
    }
}