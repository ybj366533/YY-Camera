package com.gtv.cloud.impl;


import com.gtv.cloud.recorder.GTVVideoClipInfo;
import com.gtv.cloud.recorder.GTVVideoInfo;

import java.util.ArrayList;

/**
 * Created by gtv on 2018/2/2.
 */

public class GTVVideoSegments {

    private static boolean _libraryLoaded = false;
    private String videoListFilePath;

    private long mNativeSegments;

    public GTVVideoSegments(String videoListFilePath) {
        synchronized(GTVVideoSegments.class) {

            if( _libraryLoaded == false ) {

                try {
                    System.loadLibrary("gtvideo");
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                _libraryLoaded = true;
            }
        }

        this.videoListFilePath = videoListFilePath;
        mNativeSegments = 0;
    }

    private void open() {

//        LogUtils.DebugLog("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", "test open " + videoListFilePath);
        if (this.videoListFilePath == null) {
            return;
        }
        mNativeSegments = _open(this.videoListFilePath);
        //_close(mNativeSegments);

//        LogUtils.DebugLog("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", "test open " + mNativeSegments);
        return;
    }

    private int getSegmentCount() {
        if (mNativeSegments == 0) {
            return 0;
        }
        return _getSegmentCount(mNativeSegments);
    }

//    public int getVideoSegInfo(int index, String fileName, int duration) {
//        return 0;
//    }

    public GTVVideoInfo getVideoSegInfo() {

        GTVVideoInfo gtvVideoInfo = new GTVVideoInfo();
        open();
        int count = getSegmentCount();

        ArrayList<GTVVideoClipInfo> videoClipList = new ArrayList<>();

        for ( int i = 0; i < count; ++i) {
            GTVVideoClipInfo clipInfo = new GTVVideoClipInfo();
            _getVideo(mNativeSegments, i, clipInfo);
            videoClipList.add(clipInfo);
        }

        close();

        int totalDuration = 0;
        for (int i = 0; i < videoClipList.size(); ++i) {
            totalDuration += videoClipList.get(i).getDuration();
        }

        gtvVideoInfo.setCount(count);
        gtvVideoInfo.setTotalDuration(totalDuration);
        gtvVideoInfo.setVideoClipList(videoClipList);

        return gtvVideoInfo;
    }

    public int addVideoSegInfo(String fileName, int duration, float speed) {

        open();
        if (mNativeSegments == 0 ) {
            return 0;
        }
        _addVideo(mNativeSegments, fileName, duration, speed);

        close();

        return 0;
    }

    public int removeLastVideoSeg() {
        open();
        if (mNativeSegments != 0) {
            _removeLast(mNativeSegments);
        }
        close();
        return 0;
    }

    private void close() {
        if (mNativeSegments != 0) {
            _close(mNativeSegments);
        }

        mNativeSegments = 0;
    }

    private native long _open(String path);
    private native void  _close(long mNativeSegments);
    private native int _getSegmentCount(long mNativeSegments);
    private native int _addVideo(long mNativeSegments, String fileName, int duration, float speed);
    private native int _removeLast(long mNativeSegments);
    private native int _getVideo(long mNativeSegments, int index, GTVVideoClipInfo clipInfo);
}
