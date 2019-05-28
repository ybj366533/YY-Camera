package com.ybj366533.videolib.core;

import android.opengl.GLSurfaceView;

import com.ybj366533.videolib.impl.utils.DispRect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YY on 2017/1/21.
 */

public abstract class VideoObject {

    protected GLSurfaceView glSurfaceView;

    // 源图像高度和宽度
    protected int imageWidth = 0;

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    protected int imageHeight = 0;

    public DispRect getRect() {
        return rect;
    }

    public void setRect(DispRect rect) {
        this.rect = rect;
    }

    // 渲染位置
    protected DispRect rect;

    protected boolean inited = false;
    protected boolean paused = false;

    protected List<IVideoTextureOutput> oList;

    public VideoObject(GLSurfaceView glView) {

        oList = new ArrayList<>();
        glSurfaceView = glView;

    }

    public void removeOutput(IVideoTextureOutput o) {
        oList.remove(o);
    }

    public void addOutput(IVideoTextureOutput o) {
        if( oList.contains(o) == false )
            oList.add(o);
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isInited() {
        return inited;
    }

    public void setInited(boolean inited) {
        this.inited = inited;
    }

    public void init() {

    }

    public abstract int getLastTextureId();
    protected abstract int generateTexture();


    public void pauseVideo() {

        this.setPaused(true);

    }

    public void resumeVideo() {

        this.setPaused(false);

    }

    public void destroy() {

        glSurfaceView = null;
        oList.clear();
    }

}
