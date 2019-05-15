package com.gtv.cloud.recorder;

import com.gtv.cloud.utils.LogUtils;

import java.util.ArrayList;

/**
 * Created by gtv on 2018/2/5.
 */

public class GTVVideoInfo {

    private static final String TAG = "GTVVideoInfo";

    int count;

    int totalDuration;
    ArrayList<GTVVideoClipInfo> videoClipList;

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

    public ArrayList<GTVVideoClipInfo> getVideoClipList() {
        return videoClipList;
    }

    public void setVideoClipList(ArrayList<GTVVideoClipInfo> videoClipList) {
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
