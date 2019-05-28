package com.ybj366533.videolib.impl;


import com.ybj366533.videolib.recorder.VideoClipInfo;
import com.ybj366533.videolib.recorder.VideoInfo;

import java.util.ArrayList;

/**
 * Created by YY on 2018/2/2.
 */

public class YYVideoSegments {

    private static boolean _libraryLoaded = false;
    private String videoListFilePath;

    private long mNativeSegments;

    public YYVideoSegments(String videoListFilePath) {
        synchronized (YYVideoSegments.class) {

            if (!_libraryLoaded) {

                try {
                    System.loadLibrary("yyvideo");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                _libraryLoaded = true;
            }
        }

        this.videoListFilePath = videoListFilePath;
        mNativeSegments = 0;
    }

    private void open() {

        if (this.videoListFilePath == null) {
            return;
        }
        mNativeSegments = _open(this.videoListFilePath);
        //_close(mNativeSegments);

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

    public VideoInfo getVideoSegInfo() {

        VideoInfo videoInfo = new VideoInfo();
        open();
        int count = getSegmentCount();

        ArrayList<VideoClipInfo> videoClipList = new ArrayList<>();

        for (int i = 0; i < count; ++i) {
            VideoClipInfo clipInfo = new VideoClipInfo();
            _getVideo(mNativeSegments, i, clipInfo);
            videoClipList.add(clipInfo);
        }

        close();

        int totalDuration = 0;
        for (int i = 0; i < videoClipList.size(); ++i) {
            totalDuration += videoClipList.get(i).getDuration();
        }

        videoInfo.setCount(count);
        videoInfo.setTotalDuration(totalDuration);
        videoInfo.setVideoClipList(videoClipList);

        return videoInfo;
    }

    public int addVideoSegInfo(String fileName, int duration, float speed) {

        open();
        if (mNativeSegments == 0) {
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

    private native void _close(long mNativeSegments);

    private native int _getSegmentCount(long mNativeSegments);

    private native int _addVideo(long mNativeSegments, String fileName, int duration, float speed);

    private native int _removeLast(long mNativeSegments);

    private native int _getVideo(long mNativeSegments, int index, VideoClipInfo clipInfo);
}
