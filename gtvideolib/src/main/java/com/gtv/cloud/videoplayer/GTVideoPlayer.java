package com.gtv.cloud.videoplayer;

import android.util.Log;
import android.view.Surface;

import com.gtv.cloud.utils.LogUtils;

import java.lang.ref.WeakReference;

/**
 * Created by gtv on 2018/1/23.
 */

public class GTVideoPlayer {

    public interface IGTVideoPlayerListener {

        void onPrepared(GTVideoPlayer player);
        void onCompletion(GTVideoPlayer player, int errorCode);
    }

    public interface IGTVideoPlayerLogOutput {
        void onLogOutput(String logMsg);
    }

    private static boolean _libraryLoaded = false;

    public static final int GTV_PLAYER_EVT_INITED  = 0x9000;
    public static final int GTV_PLAYER_EVT_PREPARED = 0x9001;
    public static final int GTV_PLAYER_EVT_FINISHED = 0x9002;

    public static final int GTV_PLAYER_STREAM_OPENED  = 0x5000;
    public static final int GTV_PLAYER_STREAM_STREAMING = 0x5001;
    public static final int GTV_PLAYER_STREAM_PAUSED = 0x5002;
    public static final int GTV_PLAYER_STREAM_EOF = 0x5003;
    public static final int GTV_PLAYER_STREAM_UNKNOWN = 0x5099;

    private IGTVideoPlayerListener iPlayerListener;
    private static IGTVideoPlayerLogOutput iPlayerLogOutput;

    String playUrl;
    Surface mOutputSurface;

    private int videoWidth;
    private int videoHeight;

    private long mNativeMediaPlayer;

    boolean isPaused = false;

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public GTVideoPlayer(String url) {

        synchronized(GTVideoPlayer.class) {

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

        playUrl = url;

    }

    public void setUrl(String url) {
        playUrl = url;
    }

    public void setSurface(Surface surface) {
        mOutputSurface = surface;
    }

    public void play() {

//        if (playUrl == null || mOutputSurface == null) {
//            return;
//        }

        if (playUrl == null) {
            return;
        }

        mNativeMediaPlayer = _play(playUrl,new WeakReference<GTVideoPlayer>(this), mOutputSurface);

        if (mNativeMediaPlayer == 0) {
            return;
        }

        return;
    }

    public void close() {

        if (mNativeMediaPlayer != 0) {
            _close(mNativeMediaPlayer);
            mNativeMediaPlayer = 0;
        }

    }

    public void pauseVideo() {

        isPaused = true;

        if (mNativeMediaPlayer != 0) {
            _pause(mNativeMediaPlayer);
        }

        return;
    }

    public void resumeVideo() {

        if (mNativeMediaPlayer != 0) {
            _resume(mNativeMediaPlayer);
        }

        isPaused = false;
    }

    public int seekTo(int milli) {

        if (mNativeMediaPlayer != 0) {
            int duration = _getDuration(mNativeMediaPlayer);
            if(duration > 0 && milli > duration) {
                milli = milli % duration;
            }
            return _seekTo(mNativeMediaPlayer, milli);
        }

        return 0;
    }

    public int getDuration() {

        if (mNativeMediaPlayer != 0) {
            return _getDuration(mNativeMediaPlayer);
        }

        return 0;
    }

    public int currentTimestamp() {

        if (mNativeMediaPlayer != 0) {
            return _currentTimestamp(mNativeMediaPlayer);
        }

        return 0;
    }

    public int pullAudioData(byte[] buff) {

        if (mNativeMediaPlayer != 0) {
            return _pullAudioData(mNativeMediaPlayer, buff, buff.length);
        }

        return 0;
    }

    public int checkStreamStatus() {

        if (mNativeMediaPlayer != 0) {
            return _checkStreamStatus(mNativeMediaPlayer);
        }

        return GTV_PLAYER_STREAM_UNKNOWN;
    }

    public int setRange(int s, int e) {

        if (mNativeMediaPlayer != 0) {
            return _setRange(mNativeMediaPlayer, s, e);
        }

        return 0;
    }

    private void postEventFromNative(int event, int arg1, int arg2){

        if (iPlayerListener != null) {
            if (event == GTV_PLAYER_EVT_PREPARED) {
                this.videoWidth = arg1;
                this.videoHeight = arg2;
                iPlayerListener.onPrepared(this);
            } else if (event == GTV_PLAYER_EVT_FINISHED) {
                iPlayerListener.onCompletion(this,arg1);
            } else {
                // TODO:
            }
        }

    }

    public void setLogLevel(int level) {
        _setLogLevel(level);
    }

    public void setPlayerEventLisenter(IGTVideoPlayerListener l) {
        this.iPlayerListener = l;
    }

    private native long _play(String url, Object GTVideoPlayer_this, Object surface);

    private native void _close(long nativeMediaPlayer);
    private native void _pause(long nativeMediaPlayer);
    private native void _resume(long nativeMediaPlayer);

    private native int _seekTo(long nativeMediaPlayer, int milli);
    private native int _getDuration(long nativeMediaPlayer);
    private native int _currentTimestamp(long nativeMediaPlayer);
    private native int _pullAudioData(long nativeMediaPlayer, byte[] buff, int size);

    private native int _checkStreamStatus(long nativeMediaPlayer);
    private native int _setRange(long nativeMediaPlayer, int startMilli, int endMilli);

    private native void _setLogLevel(int level);

    private static void postEventFromNative(Object weakThiz, int event, int arg1, int arg2){

        if (weakThiz != null) {
            GTVideoPlayer player = ((WeakReference<GTVideoPlayer>) weakThiz).get();
            if (player != null) {
                player.postEventFromNative(event, arg1, arg2);
            }
        }
    }

    public static void setPlayerLogOutput(IGTVideoPlayerLogOutput l) {
        //GTVideoPlayer.iPlayerLogOutput = l;
        GTVideoPlayer.iPlayerLogOutput = null;              //防止static内存泄露
    }

    private static void postLogFromNative(String logMsg) {
        LogUtils.LOGI("GTV_NDK", logMsg);
//        if(iPlayerLogOutput!= null) {
//            iPlayerLogOutput.onLogOutput(logMsg);
//        }
    }

}
