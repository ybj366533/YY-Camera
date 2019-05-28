package com.ybj366533.videolib.impl.utils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.ybj366533.videolib.utils.LogUtils;

import java.nio.ByteBuffer;

public class YYMediaMuxer {

    private static final String TAG = "YYMUX";

    private MediaMuxer mMuxer;

    private boolean muxerStarted = false;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    public boolean isMuxerStarted() {
        return muxerStarted;
    }

    public  void startMuxerIfReady(String path, MediaFormat audioMediaFormat, MediaFormat videoMediaFormat) {

        if (muxerStarted || audioMediaFormat == null || videoMediaFormat == null) {
            return;
        }
        synchronized (this) {
            try{
                mMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                audioTrackIndex = mMuxer.addTrack(audioMediaFormat);
                videoTrackIndex = mMuxer.addTrack(videoMediaFormat);
                mMuxer.start();

                muxerStarted = true;
                LogUtils.LOGS("mux start success");

            }catch (Exception e)
            {
                mMuxer = null;
                muxerStarted = false;
                LogUtils.LOGS("mux start failed");
                e.printStackTrace();
            }
        }

    }

    public void writeAudioSampleData(byte[] data, long ticks) {

        if(audioTrackIndex < 0) {
            return;
        }
        //addVideoSamples();
        ByteBuffer buffer= ByteBuffer.wrap(data);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.offset = 0;
        bufferInfo.size = data.length;
        bufferInfo.flags = 0;

        bufferInfo.presentationTimeUs = ticks;//(long)(ticks / recordSpeed);
        synchronized (this) {
            if(mMuxer != null) {
                mMuxer.writeSampleData(audioTrackIndex,buffer, bufferInfo);
            }

        }

    }

    public void writeVideoData(byte[] data, long ticks, int isKey) {

        if(videoTrackIndex < 0) {
            return;
        }

        ByteBuffer buffer= ByteBuffer.wrap(data);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.offset = 0;
        bufferInfo.size = data.length;
        bufferInfo.flags = 0;
        if(isKey == 1)
        {
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
        }
        bufferInfo.presentationTimeUs = ticks;//(long)(ticks/ recordSpeed);
        synchronized (this) {
            if(mMuxer != null) {
                mMuxer.writeSampleData(videoTrackIndex,buffer, bufferInfo);
            }

        }

    }

    public void writeVideoData(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {

        if(videoTrackIndex < 0) {
            return;
        }

        synchronized (this) {
            if(mMuxer != null) {
                mMuxer.writeSampleData(videoTrackIndex,byteBuf, bufferInfo);
            }

        }

    }

    public void close() {
        synchronized (this) {
            if (mMuxer != null) {
                LogUtils.LOGI(TAG,"mux stop step 1");
                try {
                    mMuxer.stop();
                    mMuxer.release();
                    mMuxer = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    mMuxer = null;
                }
                LogUtils.LOGI(TAG,"mux stop step 2");

            }
        }

    }
}
