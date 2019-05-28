package com.ybj366533.videolib.utils;


import android.util.Log;



/**
 * 日志管理
 */

public class LogUtils   {

    private static final String TAG = "YYVideo";

    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;

    private static int level = DEBUG;
    public static void setLogLevel(int logLevel) {
        level = logLevel;
    }

    private static boolean sIsLoggable = true;

    public static void setIsLoggable(boolean isLoggable) {
        sIsLoggable = isLoggable;
    }


    public static void LOGS(String message) {
        if (message == null || sIsLoggable == false) return;
        Log.e(TAG, message);
    }

    public static void LOGD(String tag, String message){
        if (message == null || sIsLoggable == false || level > DEBUG) return;
        Log.d(TAG + "-" +tag, message);
    }

    public static void LOGI(String tag, String message){
        if (message == null || sIsLoggable == false || level > INFO) return;
        Log.w(TAG + "-" +tag, message);
    }

    public static void LOGW(String tag, String message){
        if (message == null || sIsLoggable == false || level > WARN) return;
        Log.w(TAG + "-" +tag, message);
    }

    public static void LOGE(String tag, String message){
        if (message == null || sIsLoggable == false || level > ERROR) return;
        Log.e(TAG + "-" +tag, message);
    }

    public static void DebugLog(String tag, String msg) {
        //Log.e(tag, msg);
        Log.i(TAG + "-" +tag, "[DebugLog] will be deleted, please use LOGI etc. " +msg);
    }

}
