package com.ybj366533.videolib.editor;

/**
 * Created by ken on 2018/4/14.
 */

public class VideoEffectInfo {

    public VideoEffectInfo() {
        effectType = IVideoEditor.EffectType.EFFECT_NO;
        startTime = 0;
        endTime = -1;
    }

    public IVideoEditor.EffectType getEffectType() {
        return effectType;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }


    public void setEffectType(IVideoEditor.EffectType effectType) {
        this.effectType = effectType;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    IVideoEditor.EffectType effectType;

    int startTime;      // >=0
    int endTime;        // -1 到结尾 (一般用于设定中的特效)
}
