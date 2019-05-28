package com.ybj366533.videolib.widget;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;

import com.ybj366533.videolib.core.PlayerVideoProcessor;
import com.ybj366533.videolib.core.IVideoTextureOutput;
import com.ybj366533.videolib.editor.EditCallback;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVBaseGroupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by gtv on 2016/4/18.
 */
public class EditorViewRender extends BaseRender implements IVideoTextureOutput {

    private static final String TAG = "GTV";

    protected GTVImageFilter previewFilter;

    public EGLContext currentContext = null;

    private PlayerVideoProcessor gtvPlayerVideoProcessor;

    private Bitmap logoImage;
    private DispRect logoRect;

    private Bitmap logoImage2;
    private DispRect logoRect2;


    private String videoPath;

    public void setFullMode(boolean fullMode) {
        this.fullMode = fullMode;
    }

    public boolean getFullMode() {
        return this.fullMode;
    }

    private boolean fullMode = false;

    public void setDisPlayToScreenFlag(boolean disPlayToScreenFlag) {
        this.disPlayToScreenFlag = disPlayToScreenFlag;
    }

    public boolean isDisPlayToScreenFlag() {
        return disPlayToScreenFlag;
    }

    private boolean disPlayToScreenFlag = true;

    private EditCallback editCallback;
    public void setEditCallback (EditCallback editCallback) {
        this.editCallback = editCallback;
    }

    public EGLContext getEGLContext() {
        return currentContext;
    }

    public PlayerVideoProcessor getPlayerVideoObject() {
        return gtvPlayerVideoProcessor;
    }

    public EditorViewRender(GLSurfaceView glSurfaceView, String videoPath) {

        super(glSurfaceView);

        this.videoPath = videoPath;

        gtvPlayerVideoProcessor = new PlayerVideoProcessor(this.glSurfaceView, this);

        LogUtils.LOGE(TAG, "EditorViewRender EditorViewRender" + this.glSurfaceView.getWidth() + " " + this.glSurfaceView.getHeight());


    }


    // 设置logo图片
    public boolean setLogoBitmapAtRect(Bitmap bmp, DispRect r) {

        if( bmp == null || r == null ) {
            return false;
        }

        this.logoImage = bmp;
        this.logoRect = r;

        return true;
    }

    public boolean setLogoBitmapAtRect2(Bitmap bmp, DispRect r) {

        if( bmp == null || r == null ) {
            return false;
        }

        this.logoImage2 = bmp;
        this.logoRect2 = r;

        return true;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        LogUtils.LOGD(TAG, "EditorViewRender onSurfaceCreated." + this.glSurfaceView.getWidth() + " " + this.glSurfaceView.getHeight());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        LogUtils.LOGD(TAG, "EditorViewRender onSurfaceChanged." + this.glSurfaceView.getWidth() + " " + this.glSurfaceView.getHeight());
    }

    @Override
    public void onDestroy(){

        super.onDestroy();


        if(this.glSurfaceView != null) {
            this.glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    runDestroy();
                }
            });
        }


    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);

        if( currentContext == null ) {

            currentContext = EGL14.eglGetCurrentContext();
            LogUtils.LOGD(TAG, "EditorViewRender currentContext setting");
        }


        if (gtvPlayerVideoProcessor != null) {
            if( gtvPlayerVideoProcessor.isInited() == false ) {



                gtvPlayerVideoProcessor.init();
                gtvPlayerVideoProcessor.addOutput(this);   //todo 合适？ 还是在 创建的时候？
            }
        }


        // 如果有设置logo图片，则加载图片
        if( this.logoImage != null && this.logoRect != null ) {
            this.gtvPlayerVideoProcessor.loadLogoToRect(0,logoImage, logoRect);
            this.logoImage = null;
            this.logoRect = null;
        }

        if( this.logoImage2 != null && this.logoRect2 != null ) {
            this.gtvPlayerVideoProcessor.loadLogoToRect(1,logoImage2, logoRect2);
            this.logoImage2 = null;
            this.logoRect2 = null;
        }


        Rect outRect = new Rect();
        this.glSurfaceView.getDrawingRect(outRect);

        surfaceWidth = outRect.width();
        surfaceHeight = outRect.height();
        // force to use filter
        if(previewFilter == null){

            {
                List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                filters.add(new GTVImageFilter());
                previewFilter = new GTVBaseGroupFilter(filters);
                if (previewFilter != null)
                    previewFilter.init();
            }

            onFilterChanged();
        }

        runPendingOnDrawTasks();

        int id = gtvPlayerVideoProcessor.getLastTextureId();

        if(disPlayToScreenFlag == true) {
            if(fullMode == true) {
                preparePreviewCube(gtvPlayerVideoProcessor.getImageWidth(), gtvPlayerVideoProcessor.getImageHeight(), surfaceWidth, surfaceHeight);
            } else {
                OpenGlUtils.fitCubePlayMode(gtvPlayerVideoProcessor.getImageWidth(), gtvPlayerVideoProcessor.getImageHeight(), surfaceWidth, surfaceHeight, gLCubeBuffer);
            }
            previewFilter.onDrawFrame(id, gLCubeBuffer, gLTextureBuffer);
        }

    }

    private void landscapePreviewCube(float inputWidth, float inputHeight, float outputWidth, float outputHeight, float[] cube) {

        float scaleWidth = 1.0f;
        float scaleHeight = 1.0f;

        if( outputWidth/outputHeight > inputWidth/inputHeight ) {
            // 输入比输出瘦，宽度对齐
            scaleWidth = outputWidth * inputHeight / inputWidth / outputHeight;
        }
        else if( outputWidth/outputHeight < inputWidth/inputHeight ) {
            // 输入比输出胖，高度对齐
            scaleHeight = outputHeight * inputWidth / inputHeight / outputWidth;
        }

        float vertex[] = {
                cube[0]*scaleHeight, cube[1]*scaleWidth,
                cube[2]*scaleHeight, cube[3]*scaleWidth,
                cube[4]*scaleHeight, cube[5]*scaleWidth,
                cube[6]*scaleHeight, cube[7]*scaleWidth,
        };

        gLCubeBuffer.clear();
        gLCubeBuffer.put(vertex).position(0);

        return;
    }

    private void preparePreviewCube(float inputWidth, float inputHeight, float outputWidth, float outputHeight) {

        float scaleWidth = 1.0f;
        float scaleHeight = 1.0f;

        if( outputWidth/outputHeight > inputWidth/inputHeight ) {
            // 输入比输出瘦，宽度对齐
            scaleHeight = outputWidth * inputHeight / inputWidth / outputHeight;
        }
        else if( outputWidth/outputHeight < inputWidth/inputHeight ) {
            // 输入比输出胖，高度对齐
            scaleWidth = outputHeight * inputWidth / inputHeight / outputWidth;
        }

        //scaleWidth = scaleWidth/2;
        //scaleHeight = scaleHeight/2;

        float vertex[] = {
                -scaleWidth, -scaleHeight,
                scaleWidth, -scaleHeight,
                -scaleWidth, scaleHeight,
                scaleWidth, scaleHeight,
        };

        gLCubeBuffer.clear();
        gLCubeBuffer.put(vertex).position(0);

        return;
    }

//    @Override
//    public void setFilter(MagicFilterType type) {
//        super.setFilter(type);
//    }

    protected void onFilterChanged(){
        super.onFilterChanged();

        if( previewFilter != null ) {

            previewFilter.onInputSizeChanged(imageWidth, imageHeight);
            previewFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
        }
    }

    public void onResume(){
        super.onResume();
        LogUtils.LOGD(TAG, "EditorViewRender:onResume");


        if (gtvPlayerVideoProcessor != null) {
            gtvPlayerVideoProcessor.resumeVideo(); // tod?
        }

    }

    public void onPause(){
        super.onPause();
        LogUtils.LOGD(TAG, "EditorViewRender:onPause");

        if (gtvPlayerVideoProcessor != null) {
            synchronized (gtvPlayerVideoProcessor) {
                gtvPlayerVideoProcessor.pauseVideo();  //todo
            }
        }

    }

    @Override
    public void newGLTextureAvailable(Object src, int textureId, int width, int height, long timestampNanos) {

    }



    private void runDestroy() {

        if (gtvPlayerVideoProcessor != null) {
            gtvPlayerVideoProcessor.destroy();
        }

    }

    private final LinkedList<Runnable> mRunOnDraw = new LinkedList<>();

    protected void runPendingOnDrawTasks() {

        synchronized (mRunOnDraw) {
            while (!mRunOnDraw.isEmpty()) {

                mRunOnDraw.removeLast().run();
                //mRunOnDraw.clear();
            }
        }
    }

    public void myQueueEvent(final Runnable runnable) {

        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }

        glSurfaceView.requestRender();
    }

}
