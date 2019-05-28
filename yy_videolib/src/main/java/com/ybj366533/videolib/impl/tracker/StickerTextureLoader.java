/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ybj366533.videolib.impl.tracker;

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.ybj366533.videolib.impl.recorder.gles.EglCore;
import com.ybj366533.videolib.impl.recorder.gles.WindowSurface;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import java.lang.ref.WeakReference;

public class StickerTextureLoader implements Runnable {

    private static final String TAG = "GTVREC";

    private static final int MSG_LOAD_STICKER = 0;
    private static final int MSG_QUIT = 5;

    private static final int MAX_GLES_ERR_COUNT = 20;
    private int glesErrCount = 0;

    private EGLContext mSharingContext;

    // ----- accessed by multiple threads -----
    private volatile StickerLoaderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private String mStickerNameOrdered = null;
    private String mStickerNameCurrent = null;
    private boolean mQuit = false;


    private EglCore mEglCore;
    private WindowSurface mInputWindowSurface;

    public StickerTextureLoader(EGLContext context) {

        mSharingContext = context;
    }

    public boolean isStickerReady(String name) {

        if( name == null )
            return true;

        if( name.equalsIgnoreCase(this.mStickerNameCurrent) )
            return true;

        return false;
    }

    public void loadSticker(String name) {

        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        mStickerNameOrdered = name;

        mHandler.sendMessage(mHandler.obtainMessage(MSG_LOAD_STICKER,
                (int)0, (int)0, name));
    }

    public void startLoad() {

        synchronized (mReadyFence) {
            if (mRunning) {
                LogUtils.LOGW(TAG, "sticker loader thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "stickerloader").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    public void stopLoad() {

        mQuit = true;

        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT,
                (int)0, (int)0, null));
    }

    @Override
    public void run() {

        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new StickerLoaderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }

        prepareEGL();

        if( mQuit == false )
            Looper.loop();

        destroyEGL();

        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    private void prepareEGL() {

        mEglCore = new EglCore(mSharingContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, new Surface(new SurfaceTexture(OpenGlUtils.getExternalOESTextureID())), true);
        mInputWindowSurface.makeCurrent();
    }

    private void destroyEGL() {

        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    private static class StickerLoaderHandler extends Handler {

        private WeakReference<StickerTextureLoader> mWeakEncoder;

        public StickerLoaderHandler(StickerTextureLoader encoder) {
            mWeakEncoder = new WeakReference<StickerTextureLoader>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            StickerTextureLoader encoder = mWeakEncoder.get();
            if (encoder == null) {
                LogUtils.LOGW(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            try {

                switch (what) {
                    case MSG_LOAD_STICKER:
                        String name = (String)inputMessage.obj;
                        encoder.handleLoadSticker(name);
                        break;
                    case MSG_QUIT:
                        encoder.handleLoadSticker(null);
                        Looper.myLooper().quit();
                        break;
                    default:
                        throw new RuntimeException("Unhandled msg what=" + what);
                }
            }
            catch (Exception ex) {

                LogUtils.LOGE(TAG, "StickerLoaderHandler.handleMessage: exception." + ex.getMessage());
            }
        }
    }

    private void handleLoadSticker(String name) {

        if( name == null ) {
            EffectTracker.getInstance().clearTexture();
            return;
        }

        if( name.equalsIgnoreCase(mStickerNameCurrent) ) {
            return;
        }

        EffectTracker.getInstance().clearTexture();

        int i=0;
        while(i < 200) {

            if( i > 20 )
                EffectTracker.getInstance().prepareTextureFrames(i, 1);
            else
                EffectTracker.getInstance().prepareTextureFrames(i, 0);

//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            // sticker change when loading
            if( name.equalsIgnoreCase(mStickerNameOrdered) == false ) {
                i = -1;
                break;
            }

            i += 10;
        }

        mStickerNameCurrent = name;
    }
}
