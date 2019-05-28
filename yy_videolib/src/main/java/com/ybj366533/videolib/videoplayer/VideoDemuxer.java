package com.ybj366533.videolib.videoplayer;

import android.view.Surface;

/**
 * Created by YY on 2018/1/23.
 */

public class VideoDemuxer {

    private static boolean _libraryLoaded = false;

    String playUrl;
    Surface mOutputSurface;

    private long mNativeDemuxer;

    public VideoDemuxer(String url) {

        synchronized(VideoDemuxer.class) {

            if(!_libraryLoaded) {

                try {
                    System.loadLibrary("yyvideo");
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                _libraryLoaded = true;
            }
        }

        playUrl = url;

    }

    public void open(String url, Surface surface) {

        playUrl = url;
        mOutputSurface = surface;

        if (playUrl == null || surface == null) {
            return;
        }

        mNativeDemuxer = _open(playUrl, mOutputSurface);

        if (mNativeDemuxer == 0) {
            return;
        }

        return;
    }

    public void close() {

        if (mNativeDemuxer != 0) {
            _close(mNativeDemuxer);
            mNativeDemuxer = 0;
        }

    }

    public int seekTo(int milli) {

        if (mNativeDemuxer != 0) {
            return _seekTo(mNativeDemuxer, milli);
        }

        return 0;
    }

    public int pullAudioData(byte[] buff) {

        if (mNativeDemuxer != 0) {
            return _pullAudioData(mNativeDemuxer, buff, buff.length);
        }

        return 0;
    }

    public int checkEof() {

        if (mNativeDemuxer != 0) {
            return _checkEof(mNativeDemuxer);
        }

        return -1;
    }

    public int setRange(int s, int e) {

        if (mNativeDemuxer != 0) {
            return _setRange(mNativeDemuxer, s, e);
        }

        return 0;
    }

    public int getNextVideoTimestamp(int[] size) {

        if (mNativeDemuxer != 0) {
            return _nextVideoTimestamp(mNativeDemuxer, size);
        }

        return 0;
    }

    public int peekNextVideo() {

        if (mNativeDemuxer != 0) {
            return _peekNextVideo(mNativeDemuxer);
        }

        return 0;
    }

    public int removeNextVideo() {

        if (mNativeDemuxer != 0) {
            return _removeNextVideo(mNativeDemuxer);
        }

        return 0;
    }

    private native long _open(String url, Object surface);
    private native void _close(long nativeDemuxer);

    private native int _seekTo(long nativeDemuxer, int milli);
    private native int _nextVideoTimestamp(long nativeDemuxer, int[] size);

    private native int _pullAudioData(long nativeDemuxer, byte[] buff, int size);
    private native int _peekNextVideo(long nativeDemuxer);
    private native int _removeNextVideo(long nativeDemuxer);

    private native int _checkEof(long nativeDemuxer);
    private native int _setRange(long nativeDemuxer, int startMilli, int endMilli);
}
