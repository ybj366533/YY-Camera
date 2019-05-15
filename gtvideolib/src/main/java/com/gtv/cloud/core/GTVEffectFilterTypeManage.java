package com.gtv.cloud.core;

import com.gtv.cloud.editor.GTVideoEffectInfo;
import com.gtv.cloud.editor.IGTVVideoEditor;
import com.gtv.cloud.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gtv on 2017/11/10.
 */

// 管理特效设定
// 开始时间：特效类型
// 结束时间：就是下一个特效的开始时间
public class GTVEffectFilterTypeManage {
    //List<EffectFitlerItem> typeList;

    private static final String TAG = "VideoEffect";

    public List<GTVideoEffectInfo> getVideoEffectInfoList() {
        return videoEffectInfoList;
    }

    public void setVideoEffectInfoList(List<GTVideoEffectInfo> list) {
        videoEffectInfoList = list;
    }

    List<GTVideoEffectInfo> videoEffectInfoList;

    public GTVEffectFilterTypeManage(){

        videoEffectInfoList = new ArrayList<>();
    }

    public void startVideoEffect(IGTVVideoEditor.EffectType effectType, int startTime){
        GTVideoEffectInfo gtVideoEffectInfo = new GTVideoEffectInfo();
        gtVideoEffectInfo.setEffectType(effectType);
        gtVideoEffectInfo.setStartTime(startTime);
        gtVideoEffectInfo.setEndTime(-1);
        videoEffectInfoList.add(gtVideoEffectInfo);

    }

    public void stopVideoEffect(IGTVVideoEditor.EffectType effectType, int endTime){
        if(videoEffectInfoList!= null && videoEffectInfoList.size() > 0) {
            GTVideoEffectInfo gtVideoEffectInfo = videoEffectInfoList.get(videoEffectInfoList.size() -1);
            if (gtVideoEffectInfo.getEndTime() == -1 && gtVideoEffectInfo.getEffectType() == effectType) {
                gtVideoEffectInfo.setEndTime(endTime);
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


    public IGTVVideoEditor.EffectType getEffectFilterType(int time) {
        if (videoEffectInfoList != null && videoEffectInfoList.size() > 0) {
            for (int i = videoEffectInfoList.size() - 1; i >= 0; --i) {
                int startTime = videoEffectInfoList.get(i).getStartTime();
                int endTime = videoEffectInfoList.get(i).getEndTime();
                if ((startTime <= time) && ((time <= endTime) || (endTime == -1))) {
                    return videoEffectInfoList.get(i).getEffectType();
                }
            }
        }



        return IGTVVideoEditor.EffectType.EFFECT_NO;
    }


}
