package com.gtv.cloud.impl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.gtv.cloud.impl.recorder.gles.EglCore;
import com.gtv.cloud.impl.recorder.gles.EglSurfaceBase;
import com.gtv.cloud.impl.recorder.gles.WindowSurface;

// 这个类还没完成

/**
 * Created by gtv on 2018/6/3.
 */

public class GTVGLSurfaceView extends GLSurfaceView {

    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;

    public GTVGLSurfaceView(Context context){
        super(context);
    }

    private Renderer mRenderer;

    private Object mReadyFence = new Object();

    public void setRenderer(Renderer renderer){
        mRenderer = renderer;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                gl_thread();
            }
        };
        Thread t = new Thread(r);
    }

    public void setRenderMode(int renderMode) {
        //
    }

    public void requestRender(){
        mReadyFence.notify();
    }

    protected void gl_thread(){

        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore);
        mInputWindowSurface.makeCurrent();
        mRenderer.onSurfaceCreated(null, null);

        while (true) {
            try {
                mReadyFence.wait();
                mRenderer.onDrawFrame(null);
            }catch (Exception e) {

            }
        }
    }
}
