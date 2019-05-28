package com.ybj366533.yycamera.base.utils

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager

/**
 * 网络工具类
 * Created by summer on 2017/11/27.
 */

// 是否有网络连接
val Context.isNetWorkConnected
    get() = run {
        connectivityManager.activeNetworkInfo?.run { isAvailable } ?: false
    }

// Wifi 网络
val Context.isWifiConnected
    get() = run {
        connectivityManager.activeNetworkInfo?.run {
            type == ConnectivityManager.TYPE_WIFI && isAvailable
        } ?: false
    }

// 移动 网络
val Context.isMobilConnected
    get() = run {
        connectivityManager.activeNetworkInfo?.run {
            type == ConnectivityManager.TYPE_MOBILE && isAvailable
        } ?: false
    }

val Context.is4GConnected
    get() = run {
        connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_LTE
                && !telephonyManager.isNetworkRoaming
    }

val Context.is3GConnected
    get() = run {
        connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_UMTS
                || connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_HSDPA
                || connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_EVDO_0
                && !telephonyManager.isNetworkRoaming
    }

val Context.is2GConnected
    get() = run {
        connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_GPRS
                || connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_EDGE
                || connectivityManager.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_CDMA
                && !telephonyManager.isNetworkRoaming
    }


val Context.connectivityManager get() = run { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

val Context.telephonyManager
    get() = run { getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }