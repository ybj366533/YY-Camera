package com.ybj366533.videolib.editor;


import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YY on 2018/1/26.
 */

public interface IVideoEditor {

    enum EffectType{
        EFFECT_NO,                  // 无
        EFFECT_HEBAI,               // 黑白
        EFFECT_DOUDONG,             // 抖动
        EFFECT_LINGHUNCHUQIAO,     // 灵魂出窍
        EFFECT_YISHIJIE,            // 异世界
        EFFECT_JIANRUI,             // 尖锐
        EFFECT_BODONG,              // 波动
        EFFECT_TOUSHI,              // 透视
        EFFECT_SHANGSHUO,           // 闪烁
        EFFECT_SUMIAO,              // 素描
        EFFECT_LINGYI,              // 灵异
        EFFECT_YINXIANPAI           // 印象派
    }

    enum ImageFormat{
        IMAGE_JPEG,
        IMAGE_YUV
    }


    // 编辑对象初始化
    // glSurfaceView app侧自己定义的用于展示视频画面
    // videoPath 要编辑的视频文件
    // recordCallback 回调函数
    void init(GLSurfaceView glSurfaceView, String videoPath, EditCallback editCallback);
    void init(final GLSurfaceView glSurfaceView, final String videoPath, final String editWorkFolder, boolean reloadSettingFlag, EditCallback editCallback);

    void setBitrate(int bitrate);
    int getBitrate();

    void setGOP(int gop);
    int getGOP();

//    // 设置视频size（默认使用原视频的分辨率）
//    void setVideoSize(int width, int height);

    // 设置配乐
    void setMusicPath(String musicPath);
    void setMusicPath(String musicPath, int startTimeMili);
    String getMusicPath();

    int getMusicStartTime();
    int getMusicStartTimeMili();        // 等同 getMusicStartTime

    // 设置封面
    void setMp4VideoCover(String imgFileName);

    // 开始预览
    void startPreview();

    // 结束预览
    void stopPreview();

    // 释放资源
    void destroy();

    // 设置特效API
//    // 设置特效预览效果
//    //void setEffectFilterType(int filterType);
//    void setEffectFilterType(EffectType effectType);
//    // 获取当前特效
//    EffectType getEffectFilterType();
//
//    // 设置特效以及期间（作用到最终的合成文件中）
//    void addEffectFilter(int startTime, int endTime,EffectType effectType);

    // APP按下某特效的时候，开始设置特效
    boolean startVideoEffect(EffectType effectType);
    // APP松开某特效的时候，停止设置特效
    boolean stopVideoEffect(EffectType effectType);     // 必须和当前特效一致
    // 移除最后iyge特效
    boolean removeLastVideoEffect();
    // 清除所有特效
    boolean clearAllVideoEffect();
    // 获取已设置特效列表
    List<VideoEffectInfo> getVideoEffectList();
    void setVideoEffectList(List<VideoEffectInfo> list);




    // 设置logo水印
    boolean setLogoBitmapAtRect(Bitmap bmp, Rect r);
    boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r);


    // 设置慢动作特效（开始时间和结束时间同为0，则是清除设置）
    void setSlowPlayTime(int startTime, int endTime);

    // 设置剪辑区间
    void setVideoCropRange(int startTime, int endTime);
    int[] getVideoCropRange();

    void setVideoPath(String videoPath);


    // 开始播放
    void playStart();
    // 暂停播放
    void playPause();
    // 跳转到特定位置
    void seekTo(int msec);
    // 获取当前我位置
    int getDuration();
    // 获取编辑文件的总时长（毫秒）
    int getCurrentPosition ();



    // 合成视频
    // 废弃合成接口，请用合成类的接口。(因为拍摄逻辑优化，共用类难以维护)
    //void startCompose(String filePath);
    //void startCompose(String filePath, String keyFrameVideoPath);
    //void stopCompose();

    // 保存合成视频的设定(不生成视频文件)
    void saveSetting();

    // 设定x264
    //void setX264Mode(boolean x264Mode);
    //boolean getX264Mode();

    // 视频帧抽取
    // ImageFormat 抽取的图片格式Jpeg/YUV (YUV仅供制作动态封面webp时使用)
    // outDirPath 视频帧存放目录（app侧维护这个目录）
    // startTime/endTime 抽取的视频期间（同为0，则为全视频）
    // dataNum 抽取的帧数（等间隔抽取）
    // ArrayList<ExtractFrameInfo> frameInfoList 抽取结果（图片路径，以及对应的时间戳）
    void extractVideoFrame(ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList);
    void extractAndScaleVideoFrame(ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList, float scale);

    // webp动态图制作
    // frameInfoList 输入图片列表（视频帧抽取的YUV）
    // outputPath 动态图保存位置
    boolean createImageWebp(ArrayList<ExtractFrameInfo> frameInfoList, String outputPath);

    // 原声禁音
    void setOriginalSoundMute(boolean muteFlag);
    boolean getOriginalSoundMute();
    
    //设置显示模式
    // 视频保持16:9显示在view上
    // fullMode= true 充满全屏（必要是，部分视频被裁剪）
    // fullMode= false 视频不裁剪，但是view上下或者左右留黑边
    void setDisplayMode(boolean fullMode);
    boolean getDisplayMode();

    //String getVideoPathForCoverSetting();

    // 设定合成时的动画
    void setAnimation(String animationFolder, int animImageInterval, Rect r);
    void setAnimation(String prefix, String animationFolder, int animImageInterval, Rect r);

    // 设置原始音量
    void setOriginVolume(float v);
    float getOriginVolume();
    // 设置音乐音量
    void setMusicVolume(float v);
    float getMusicVolume();
}
