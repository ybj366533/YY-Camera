package com.ybj366533.yycamera.base.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.tbruyelle.rxpermissions2.Permission
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.functions.Consumer

/**
 * Created by summer on 17-10-31.
 * RxPermission 权限管理工具类集成
 * http://www.jianshu.com/p/c8a30200e6b2
 * 《官方权限列表》
 * http://developer.android.com/training/permissions/requesting.html
　　http://developer.android.com/guide/topics/security/permissions.html#normal-dangerous
 */

//请求开启权限
@SuppressLint("CheckResult")
fun onPermissions(activity: Activity, vararg permissionSrc: String) {

    val rxPermission = RxPermissions(activity)
    rxPermission.requestEach(*permissionSrc)
            .subscribe(Consumer<Permission> { permission ->
                when {
                    permission.granted -> // 用户已经同意该权限
                        Log.e("RxPermission",permission.name + " is granted.")
                    permission.shouldShowRequestPermissionRationale -> // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                        Log.e("RxPermission",permission.name + " is denied. More info should be provided.")
                    else -> // 用户拒绝了该权限，并且选中『不再询问』
                        Log.e("RxPermission",permission.name + " is denied.")
                }
            })

}

//判断是否获得权限
@SuppressLint("CheckResult")
fun getInfoPermissions(activity: Activity, permissionSrc: String): Boolean {
    var isInfo = false
    val rxPermission = RxPermissions(activity)
    rxPermission.request(permissionSrc)
            .subscribe({ granted ->
                isInfo = granted
            })

    return isInfo
}

// 必须在初始化阶段调用,例如onCreate()方法中
//RxView.clicks(findViewById(R.id.enableCamera))
//.compose(RxPermissions.getInstance(this).ensure(Manifest.permission.CAMERA))
//.subscribe(granted -> {
//    // 当R.id.enableCamera被点击的时候触发获取权限
//});
