package com.ybj366533.yycamera.base.utils

import java.io.File

/**
 * 设备信息
 * Created by summer on 2017/10/31.
 */

/**
 * 获取CPU核心个数
 */
val cpuCoresNumber = run {
    val defNumber = 2;
    var cores = defNumber
    try {
        cores = File("/sys/devices/system/cpu/")
                .listFiles { pathname ->
                    val path = pathname.name
                    if (path.startsWith("cpu")) {
                        for (i in 3 until path.length) {
                            if (path[i] < '0' || path[i] > '9') {
                                return@listFiles false
                            }
                        }
                        return@listFiles true
                    }
                    return@listFiles false
                }
                .size
        cores = if (cores < defNumber) defNumber else cores
    } catch (e: Exception) {
    }
    cores
}