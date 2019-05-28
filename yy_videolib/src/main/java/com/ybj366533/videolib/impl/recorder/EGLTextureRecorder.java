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

package com.ybj366533.videolib.impl.recorder;

import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ybj366533.videolib.impl.encoder.IVideoEncoder;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;
import com.ybj366533.videolib.impl.recorder.gles.EglCore;
import com.ybj366533.videolib.impl.recorder.gles.WindowSurface;

import com.ybj366533.videolib.utils.LogUtils;


import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 *     call TextureMovieEncoder#frameAvailable().
 * </ul>
 *
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
public class EGLTextureRecorder implements Runnable {
    private static final String TAG = "GTVREC";
    private static final boolean VERBOSE = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    private static final int MAX_GLES_ERR_COUNT = 20;
    private int glesErrCount = 0;

    // input from outside
    private IVideoEncoder mVideoEncoder;
    private EGLContext mSharingContext;

    private int mPreviewWidth = -1;
    private int mPreviewHeight = -1;
    private int mVideoWidth = -1;
    private int mVideoHeight = -1;
    private boolean mFitWithBar = false;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private int mTextureId;
    private long mFrameCount = 0;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private GTVImageFilter filter;
    private FloatBuffer gLCubeBuffer;
    private FloatBuffer gLTextureBuffer;
    private FloatBuffer gLTextureBufferFlip;

    private List<String> encodingQueue = new ArrayList<>();

    public EGLTextureRecorder(EGLContext context, int width, int height) {

        mSharingContext = context;
        mPreviewWidth = mVideoWidth = width;
        mPreviewHeight = mVideoHeight = height;

        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        gLTextureBufferFlip = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBufferFlip.put(TextureRotationUtil.getRotation(Rotation.NORMAL,false, true)).position(0);
//        float[] textureCords = TextureRotationUtil.getRotation(Rotation.NORMAL,
//                false, false);
//        gLTextureBuffer.clear();
//        gLTextureBuffer.put(textureCords).position(0);

        // TODO:暂定逻辑，横屏的编码不切边，竖屏的编码切边
        if( width > height ) {
            this.mFitWithBar = true;
        }
        else {
            this.mFitWithBar = false;
        }

        LogUtils.LOGW(TAG, "EGLTextureRecorder inited:" + mVideoWidth + "x" + mVideoHeight);
    }

    public void setVideoEncoder(IVideoEncoder enc) {

        this.mVideoEncoder = enc;
    }

    public void startRecord() {

        synchronized (mReadyFence) {
            if (mRunning) {
                LogUtils.LOGW(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "EGLTextureRecorder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
        mFrameCount = 0;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, new Object()));
    }

    public void stopRecord() {
        synchronized (mReadyFence) {
            if (!mRunning) {
                LogUtils.LOGW(TAG, "Encoder thread not started");
                return;
            }
        }
        if( mHandler == null ) {
            return;
        }
        mFrameCount = 0;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(int width, int height, boolean flip, long timestampNanos) {

        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        if( android.os.Build.MODEL.equalsIgnoreCase("OPPO A33") || Build.VERSION.SDK_INT < 22 ) {

            synchronized (encodingQueue) {
                if( encodingQueue.size() > 1 ) {
                    Log.e(TAG, "encodingQueue too big");
                    return;
                }
            }
            encodingQueue.add("encodingQueue");
        }
//        long timestamp = System.nanoTime();
//        if (timestamp == 0) {
//            LogUtils.LOGE(TAG, "HEY: got System.nanoTime with timestamp of zero");
//            timestamp = System.currentTimeMillis()*1000*1000;
//            //return;
//        }
        //long timestamp = mFrameCount * 1000 * 1000 * 1000;
        //mFrameCount ++;
        long timestamp = timestampNanos;

        TextureParams params = new TextureParams();
        //params.cubeBuf = cubeBuf;
        //params.textureBuf = textureBuf;
        params.width = width;
        params.height = height;
        if(flip == true) {
            params.textureBuf = gLTextureBufferFlip;
        } else {
            params.textureBuf = gLTextureBuffer;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, params));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        LogUtils.LOGD(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<EGLTextureRecorder> mWeakEncoder;

        public EncoderHandler(EGLTextureRecorder encoder) {
            mWeakEncoder = new WeakReference<EGLTextureRecorder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            EGLTextureRecorder encoder = mWeakEncoder.get();
            if (encoder == null) {
                LogUtils.LOGW(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            try {

                switch (what) {
                    case MSG_START_RECORDING:
                        LogUtils.LOGW(TAG, "EncoderHandler.handleMessage: MSG_START_RECORDING.");
                        encoder.handleStartRecording();
                        break;
                    case MSG_STOP_RECORDING:
                        LogUtils.LOGW(TAG, "EncoderHandler.handleMessage: MSG_STOP_RECORDING.");
                        encoder.handleStopRecording();
                        break;
                    case MSG_FRAME_AVAILABLE:
                        long timestamp = (((long) inputMessage.arg1) << 32) |
                                (((long) inputMessage.arg2) & 0xffffffffL);
                        encoder.handleFrameAvailable((TextureParams) obj, timestamp);
                        break;
                    case MSG_SET_TEXTURE_ID:
                        encoder.handleSetTexture(inputMessage.arg1);
                        break;
                    case MSG_UPDATE_SHARED_CONTEXT:
                        encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                        break;
                    case MSG_QUIT:
                        LogUtils.LOGW(TAG, "EncoderHandler.handleMessage: MSG_QUIT.");
                        Looper.myLooper().quit();
                        break;
                    default:
                        throw new RuntimeException("Unhandled msg what=" + what);
                }
            }
            catch (Exception ex) {

                LogUtils.LOGE(TAG, "EncoderHandler.handleMessage: exception." + ex.getMessage());
            }
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording() {
        LogUtils.LOGD(TAG, "handleStartRecording ");
        prepareEncoder(mSharingContext, mVideoWidth, mVideoHeight);

        glesErrCount = 0;
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     * @param transform The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(TextureParams params, long timestampNanos) {

        int error = 0;

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR && glesErrCount++ <= MAX_GLES_ERR_COUNT ){
            LogUtils.LOGE("ES20_ERROR", "handleFrameAvailable: befoure glError -> " + error);
        }

        // 不同的线程，一个write mTextureId, 一个read mTextureId ok ?? TODO:
        mInputWindowSurface.makeCurrent();
        GLES20.glViewport(0, 0, mVideoWidth, mVideoHeight);
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR && glesErrCount++ <= MAX_GLES_ERR_COUNT ){
            LogUtils.LOGE("ES20_ERROR", "handleFrameAvailable: glClear red glError -> " + error);
        }

        if( this.mFitWithBar == true ) {
            OpenGlUtils.fitCubeWithBar(params.width, params.height, this.mVideoWidth, this.mVideoHeight, gLCubeBuffer);
        }
        else {
            OpenGlUtils.fitCube(params.width, params.height, this.mVideoWidth, this.mVideoHeight, gLCubeBuffer);
        }
        //filter.onDrawFrame(mTextureId, gLCubeBuffer, gLTextureBuffer);
        filter.onDrawFrame(mTextureId, gLCubeBuffer, params.textureBuf);

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR && glesErrCount++ <= MAX_GLES_ERR_COUNT ){
            LogUtils.LOGE("ES20_ERROR", "handleFrameAvailable: onDrawFrame glError -> " + error);
        }

        GLES20.glFinish();

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();

        // try to encode data
        if( mVideoEncoder.encodeBitmap(null) == -999) {
            // 说明需要encode重新创建了
            releaseEncoder();
            prepareEncoder(mSharingContext, mVideoWidth, mVideoHeight);
        }

        //Log.e(TAG, "LOGCAT encoded " + timestampNanos + " at " + System.currentTimeMillis() + " ---- ");
        if( encodingQueue.size() > 0 )
            encodingQueue.remove(0);
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        LogUtils.LOGD(TAG, "handleStopRecording");
        releaseEncoder();

        glesErrCount = 0;
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        //Log.d(TAG, "handleSetTexture " + id);
        mTextureId = id;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        LogUtils.LOGD(TAG, "handleUpdatedSharedContext " + newSharedContext);
        mSharingContext = newSharedContext;

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mEglCore.release();

        prepareEncoder(newSharedContext, mVideoWidth, mVideoHeight);
        /*
        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        filter = new GTVImageFilter();
        if(filter != null){
            filter.init();
            filter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
            filter.onDisplaySizeChanged(mVideoWidth, mVideoHeight);
        }
       */
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height) {

        int error;
        mVideoWidth = width;
        mVideoHeight = height;

        mSharingContext = sharedContext;

        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            LogUtils.LOGE("ES20_ERROR", ":prepareEncoder 1 glError " + error);
        }

        filter = new GTVImageFilter();
        if(filter != null){
            filter.init();
            filter.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
            filter.onDisplaySizeChanged(mVideoWidth, mVideoHeight);
        }
        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            LogUtils.LOGE("ES20_ERROR", ":prepareEncoder 2 glError " + error);
        }

    }

    private void releaseEncoder() {

        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if(filter != null){
            filter.destroy();
            filter = null;
            //type = MagicFilterType.NONE;
        }
    }
//    private MagicFilterType type = MagicFilterType.NONE;
//    public void setFilter(MagicFilterType type) {
//        this.type = type;
//    }

    public void setPreviewSize(int width, int height){
        mPreviewWidth = width;
        mPreviewHeight = height;
    }
}
