package com.ybj366533.yycamera;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by qqche_000 on 2017/8/6.
 */

@SuppressLint("Registered")
public class Application extends android.app.Application {
    private static Context mContext;
    public static int screenWidth;
    public static int screenHeight;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        DisplayMetrics mDisplayMetrics = getApplicationContext().getResources()
                .getDisplayMetrics();
        screenWidth = mDisplayMetrics.widthPixels;
        screenHeight = mDisplayMetrics.heightPixels;
    }

    public static Context getContext() {
        return mContext;
    }
}
