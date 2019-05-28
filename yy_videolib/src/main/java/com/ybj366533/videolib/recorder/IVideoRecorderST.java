package com.ybj366533.videolib.recorder;


import android.opengl.EGLContext;

import java.util.List;

/**
 * Created by YY on 2018/1/26.
 */

public interface IVideoRecorderST {
    enum SpeedType{
        STANDARD,
        SLOW,
        FAST
    }

    enum CameraType{
        BACK,
        FRONT
    }

    // 录制初始化
    // glSurfaceView app侧自己定义的用于展示视频画面
    // recWorFolder 录制是临时文件保存用的工作目录
    // recordCallback 回调函数
    //void init(GLSurfaceView glSurfaceView, String recWorFolder, RecordCallback recordCallback);

    void init(String recWorFolder, RecordCallback recordCallback);

    // 设置视频分辨率（默认480*640）,必须是16的倍数
    void setVideoSize(int width, int height);
    int getVideoWidth();
    int getVideoHeight();


    // // 设置录制配乐（从头开始）
    void setMusicPath(String musicPath);

    // 设置录制配乐
    void setMusicPath(String musicPath, int startTimeMili);
    String getMusicPath();
    int getMusicStartTimeMili();

    // 设置录制速度（默认标准速度）
    void setRecordSpeed(SpeedType speedType);
    SpeedType getRecordSpeed();

    // 设置最长录制时间（默认：15秒）
    void setMaxDuration(int maxDurationMili);

    // 获取最长录制时间
    int getMaxDuration();

//    // 设置logo水印
//    boolean setLogoBitmapAtRect(Bitmap bmp, Rect r);
//    boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r);

    // 显示视频帧(必须在opengl线程调用)
    int inputVideoFrame(int textureId, int textureWidth, int textureHeight, final boolean flip, long timestampNanos);

    // 设置EGL上下文（）
    void setEGLContext(EGLContext eglContext, int textureWidth, int textureHeight);

//    // 开始预览
//    void startPreview();
//    // 结束预览
//    void stopPreview();
    // 释放资源
    void destroy();


    // 准备录制(?)
    //void prepareRecord();
    // 开始录制
    void startRecord(final EGLContext currentContext);
    // 暂停录制（断点录制时）
    void pauseRecord();
    // 重开录制（断点录制时）
    void resumeRecord(final EGLContext currentContext);
    // 结束录制
    void stopRecord();

    // 导出录制文件
    // 这个函数可能花费数秒，app侧需要自行做异步处理
    void exportTopath(String path, String pathRev);


    // 获取视频片段信息
    VideoInfo getVideoInfo();
    // 删除最新录制的一个片段
    void deleteLastVideoClip();

    // 删除工作目录下的所有文件（视频片段+管理文件）
    void deleteAllVideoClips();

    int getDuration();




////    int getZoom();
////    void setZoom(int zoom);
//
//    void setFilter(final int filterType);
//    void setFilterByName(String filterName);
//    void setStickerPath(String folder);
//    Boolean isBeautyOn();
//    void setBeautyParams(int index, float percent);
//    float[] getBeautyParams();
//    float[] resetBeautyParams();
//    float[] switchBeauty(boolean flag);
//
//    void setBigEye(int value);
//    int getBigEye();
//    void setThinFace(int value);
//    int getThinFace();
//
//
//    int currentCameraId();
//    void switchCamera(int cameraId);
//    boolean supportLightOn();
//    boolean isLightOn();
//    void setLightStatus(boolean flag);
//    public interface OnFocusListener{
//        void onFocusSucess();
//        void onFocusFail();
//    }
//    void setFocus(float x, float y, OnFocusListener listener);
//
//    public interface OnTextureListener {
//        int onTextureAvailable(int textureId, int textureWidth, int textureHeight);
//    }
//    void setTexutreListener(OnTextureListener listener);
//
//    // 拍照接口(必须在startPreview之后调用)
//    public interface ITakePictureCallback {
//        public abstract void onBitmapReady(Bitmap bmp);
//    }
//    boolean takePicture(ITakePictureCallback cb);
//
//    public interface OnYUVDataListener {
//        void onYUVDataAvailable(byte[] data, int width, int height);
//    }
//
//    void setYUVDataListener(OnYUVDataListener listener);

    void startAnimation(String folder);
    void stopAnimation();

    void startAnimationGroup(List<String> l);
    void stopAnimationGroup();

    void startAnimationGroupKeepLast(List<String> l);
}
