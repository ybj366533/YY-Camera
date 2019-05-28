package com.ybj366533.videolib.core;

import com.ybj366533.videolib.editor.VideoEffectInfo;
import com.ybj366533.videolib.editor.IVideoEditor;
import com.ybj366533.videolib.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YY on 2017/11/10.
 */

// 管理特效设定
// 开始时间：特效类型
// 结束时间：就是下一个特效的开始时间
public class EffectFilterTypeManage {
    //List<EffectFitlerItem> typeList;

    private static final String TAG = "VideoEffect";

    public List<VideoEffectInfo> getVideoEffectInfoList() {
        return videoEffectInfoList;
    }

    public void setVideoEffectInfoList(List<VideoEffectInfo> list) {
        videoEffectInfoList = list;
    }

    List<VideoEffectInfo> videoEffectInfoList;

    public EffectFilterTypeManage(){

        videoEffectInfoList = new ArrayList<>();
    }

    public void startVideoEffect(IVideoEditor.EffectType effectType, int startTime){
        VideoEffectInfo videoEffectInfo = new VideoEffectInfo();
        videoEffectInfo.setEffectType(effectType);
        videoEffectInfo.setStartTime(startTime);
        videoEffectInfo.setEndTime(-1);
        videoEffectInfoList.add(videoEffectInfo);

    }

    public void stopVideoEffect(IVideoEditor.EffectType effectType, int endTime){
        if(videoEffectInfoList!= null && videoEffectInfoList.size() > 0) {
            VideoEffectInfo videoEffectInfo = videoEffectInfoList.get(videoEffectInfoList.size() -1);
            if (videoEffectInfo.getEndTime() == -1 && videoEffectInfo.getEffectType() == effectType) {
                videoEffectInfo.setEndTime(endTime);
            }else {
                LogUtils.LOGE(TAG, "wrong effect to stop");
            }
        } else {
            LogUtils.LOGE(TAG, "no effect can stop");
        }

    }

    public void removeLastVideoEffect(){
        if(videoEffectInfoList != null && videoEffectInfoList.size() > 0) {
            videoEffectInfoList.remove(videoEffectInfoList.size()-1);
        } else {
            LogUtils.LOGW(TAG, "no effect to remove");
        }
    }

    public void clearAllVideoEffect(){
        if(videoEffectInfoList != null) {
            videoEffectInfoList.clear();
        }
    }


    public IVideoEditor.EffectType getEffectFilterType(int time) {
        if (videoEffectInfoList != null && videoEffectInfoList.size() > 0) {
            for (int i = videoEffectInfoList.size() - 1; i >= 0; --i) {
                int startTime = videoEffectInfoList.get(i).getStartTime();
                int endTime = videoEffectInfoList.get(i).getEndTime();
                if ((startTime <= time) && ((time <= endTime) || (endTime == -1))) {
                    return videoEffectInfoList.get(i).getEffectType();
                }
            }
        }



        return IVideoEditor.EffectType.EFFECT_NO;
    }


}
