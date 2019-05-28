package com.ybj366533.videolib.impl.setting;

import android.media.AudioFormat;


public class AVStreamSetting {

    public static final int AUDIO_SAMPLERATE = 44100;
    public static final int AUDIO_BITRATE = 128000;


    private int audioBitrate;
    private int audioSampleRate;
    private int audioChannelFormat;
    private int audioEncodingFormat;

    private int videoBitrate;
    private int videoWidth;
    private int videoHeight;

    private boolean onlyIFrame = false;         // 视频是否只有I帧

    public AVStreamSetting(int audioBitrate, int audioSampleRate, int audioChannelFormat, int audioEncodingFormat,
                           int videoBitrate, int videoWidth, int videoHeight, boolean onlyIFrame) {

        this.audioBitrate = audioBitrate;
        this.audioSampleRate = audioSampleRate;
        this.audioChannelFormat = audioChannelFormat;
        this.audioEncodingFormat = audioEncodingFormat;

        this.videoBitrate = videoBitrate;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;

        this.onlyIFrame = onlyIFrame;
    }


    // 普通 录像
    public static AVStreamSetting settingForVideoRecord(int videoBitrate, int videoWidth, int videoHeight, boolean onlyIFrame) {

        return new AVStreamSetting(AUDIO_BITRATE, AUDIO_SAMPLERATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, videoBitrate, videoWidth, videoHeight, onlyIFrame);
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioChannelFormat() {
        return audioChannelFormat;
    }

    public void setAudioChannelFormat(int audioChannelFormat) {
        this.audioChannelFormat = audioChannelFormat;
    }

    public int getAudioEncodingFormat() {
        return audioEncodingFormat;
    }

    public void setAudioEncodingFormat(int audioEncodingFormat) {
        this.audioEncodingFormat = audioEncodingFormat;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public boolean isOnlyIFrame() {
        return onlyIFrame;
    }

}
