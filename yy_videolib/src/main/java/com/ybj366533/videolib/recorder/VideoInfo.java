package com.ybj366533.videolib.recorder;

import com.ybj366533.videolib.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by YY on 2018/2/5.
 */

public class VideoInfo {

    private static final String TAG = "VideoInfo";

    int count;

    int totalDuration;
    ArrayList<VideoClipInfo> videoClipList;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    public ArrayList<VideoClipInfo> getVideoClipList() {
        return videoClipList;
    }

    public void setVideoClipList(ArrayList<VideoClipInfo> videoClipList) {
        this.videoClipList = videoClipList;
    }

    public void dump(){
        String dumpInfo = "count: " + count + " total duration: " + totalDuration;
        for ( int i = 0; i < count; ++i) {
            dumpInfo += " filename: " +videoClipList.get(i).fileName + " duration: " +videoClipList.get(i).duration;
        }
        LogUtils.LOGI(TAG, dumpInfo);
    }

}
