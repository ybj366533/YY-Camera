package com.ybj366533.videolib.stream;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.view.Surface;

import com.ybj366533.videolib.core.VideoObject;
import com.ybj366533.videolib.impl.YYRes;
import com.ybj366533.videolib.impl.encoder.HWAACEncoder;
import com.ybj366533.videolib.impl.encoder.HWSurfaceEncoder;
import com.ybj366533.videolib.impl.encoder.IAudioEncoder;
import com.ybj366533.videolib.impl.encoder.IVideoEncoder;
import com.ybj366533.videolib.impl.recorder.EGLTextureRecorder;
import com.ybj366533.videolib.impl.recorder.AudioRecorder;
import com.ybj366533.videolib.impl.setting.AVStreamSetting;
import com.ybj366533.videolib.impl.utils.YYMediaMuxer;
import com.ybj366533.videolib.utils.LogUtils;

import net.surina.soundtouch.SoundTouch;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by YY on 17/1/20.
 */

public class VideoComposeStreamer implements IAudioEncoder.IADTCallback, IVideoEncoder.IAVCCallback {

    private RecordCallback recordCallback;

    private static final String TAG = "ComposeStreamer";

    private String videoPath;
    private AVStreamSetting setting;


    private long lastUpdateTimestamp;
    private long lastVideoFrameTimestamp;
    private long lastAudioFrameTimestamp;


    private IAudioEncoder mAudioEncoder;
    private IVideoEncoder mVideoEncoder;
    private SoundTouch mSoundTouch;

    private EGLContext eglContext;
    private EGLTextureRecorder mEGLTextureRecorder;
    private AudioRecorder AudioRecorder;
    private VideoObject mVideoSource;

    private long lastProfileTimestamp;

    // 排他用
    private AtomicBoolean mQuit = new AtomicBoolean(true);


    //private MediaMuxer mMuxer;
    private YYMediaMuxer YYMediaMuxer;

    private String audioFilePath;
    private String videoFilePath;

    private MediaMuxer audioMuxer;
    private MediaMuxer videoMuxer;


    //private boolean muxerStarted = false;
    //private MediaMuxer mMuxerAudio;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    private long start_mills = 0;

    private double recordSpeed = 1;

    private boolean record_pause_flag = false;

    private boolean bgMusicFlag = false;
    private boolean musicRecordToFile = false;

    private boolean videoFirstFrameRecorded = false;

    public interface RecordCallback {
        //void onPrepared();
        void onProgress(final int duration);
        void onComplete(final int duration);
        void onError(int errorCode);
    }

    public VideoComposeStreamer(String videoPath, AVStreamSetting setting, EGLContext context) {

        this.videoPath = videoPath;
        this.setting = setting;
        this.eglContext = context;

        start_mills = 0;
        videoFirstFrameRecorded = false;

        return;
    }

    public VideoComposeStreamer(String audioFilePath, String videoFilePath, AVStreamSetting setting, EGLContext context) {

        //this.videoPath = videoPath;
        this.audioFilePath = audioFilePath;
        this.videoFilePath = videoFilePath;
        this.setting = setting;
        this.eglContext = context;

        start_mills = 0;
        videoFirstFrameRecorded = false;

        return;
    }


    public int start() {

        LogUtils.LOGI(TAG,"startWork url: " + this.videoPath);


        int ret;

        ret = this.initEncoder();
        if( ret != YYRes.RESULT_OK ) {
            return ret;
        }

        //YYMediaMuxer = new YYMediaMuxer();

       // mQuit.set(false);

        return YYRes.RESULT_OK;
    }

    public void closeStream() {

        LogUtils.LOGI(TAG,"close stream");
        // 取出 所有编码器未出来的帧
//        while(true) {
//            if(mVideoEncoder != null) {
//                mVideoEncoder.encodeBitmap(null);
//            }
//            if(tickList.size() < 1 ) {
//                break;
//            } else {
//                try{
//                    Thread.sleep(20);
//                } catch (Exception e) {
//
//                }
//            }
//        }

        mVideoEncoder.encodeBitmap(null,200*1000);


        synchronized (this) {


            this.destroyEncoder();

            // 关闭两个muxer
            if (audioMuxer != null) {
                LogUtils.LOGI(TAG,"mux stop step 1");
                try {
                    audioMuxer.stop();
                    audioMuxer.release();
                    audioMuxer = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    audioMuxer = null;
                }
                LogUtils.LOGI(TAG,"mux stop step 2");

            }

            if (videoMuxer != null) {
                LogUtils.LOGI(TAG,"mux stop step 1");
                try {
                    videoMuxer.stop();
                    videoMuxer.release();
                    videoMuxer = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    videoMuxer = null;
                }
                LogUtils.LOGI(TAG,"mux stop step 2");

            }

        }

        return;
    }


    private int initEncoder() {


        this.mAudioEncoder = new HWAACEncoder();
        this.mVideoEncoder = new HWSurfaceEncoder();

//        this.mEGLTextureRecorder = new EGLTextureRecorder(this.eglContext, this.setting.getVideoWidth(), this.setting.getVideoHeight());
//        this.mEGLTextureRecorder.setVideoEncoder(this.mVideoEncoder);

        // set encoder callback
        this.mVideoEncoder.setCallback(this);
        this.mAudioEncoder.setCallback(this);

        // start all
        if( YYRes.RESULT_OK != this.mVideoEncoder.startEncoder(this.setting.getVideoWidth(), this.setting.getVideoHeight(), this.setting.getVideoBitrate(), this.setting.isOnlyIFrame())) {
            LogUtils.LOGS("initRecorder：NG ");
            return YYRes.RESULT_ERR_PARAM_NG;
        }
        //this.mEGLTextureRecorder.startRecord();

        int channels = 2;

        if( this.setting.getAudioChannelFormat() == AudioFormat.CHANNEL_IN_MONO ) {
            channels = 1;
        }
        else if(  this.setting.getAudioChannelFormat() == AudioFormat.CHANNEL_IN_STEREO ) {
            channels = 2;
        }

        this.mAudioEncoder.startEncoder(this.setting.getAudioSampleRate(), channels, this.setting.getAudioBitrate());



        LogUtils.LOGS("initRecorder：OK ");
        return YYRes.RESULT_OK;
    }

    private void destroyEncoder() {

//        if( this.mEGLTextureRecorder != null ) {
//            this.mEGLTextureRecorder.stopRecord();
//            this.mEGLTextureRecorder = null;
//        }

        if( this.mVideoEncoder != null ) {
            this.mVideoEncoder.stopEncoder();
            this.mVideoEncoder = null;
        }

//        if (this.AudioRecorder != null) {
//            this.AudioRecorder.stopRecord();
//            this.AudioRecorder = null;
//        }

        if( this.mAudioEncoder != null ) {
            this.mAudioEncoder.stopEncoder();
            this.mAudioEncoder = null;
        }




        return;
    }

    @Override
    public void onAudioMetaInfo(byte[] meta) {
        LogUtils.LOGI(TAG, "onAudioMetaInfo");

    }

    // 接收的声音数据量，用于作为同步的tick

    private long audioDataBytes = 0;

    //private long lastVideoTick = -1;
    //private long lastAudioTick = -1;
    private long lastTick = -1;


    public long currentVideoPTS() {

        //return (System.nanoTime()/1000 - this.start_mills);
        long tick = (audioDataBytes*1000/(44100*4/1000));
        if ( lastTick >= 0 && tick <= lastTick) {
            tick = lastTick + 1 * 1000;
        }
        lastTick = tick;
        return tick;
    }

    public long currentAudioPTS() {

        long tick = (audioDataBytes*1000/(44100*4/1000));
        if ( lastTick >= 0 && tick <= lastTick) {
            tick = lastTick + 1 * 1000;
        }
        lastTick = tick;
        return tick;
    }

    public long getDuration(){
        return (audioDataBytes * 1000 / (44100 * 4));
    }

    private int callbackFreq = 0;
    @Override
    public void onAudioFrame(byte[] data, long presentationTimeUs) {

        // todo 移动到metainfo？ 但是老华为太早不行？
        if(audioMuxer == null) {
            startAudioMuxer();
        }

        //long    ticks = this.currentAudioPTS();
        writeAudioSampleData(data, presentationTimeUs);

    }

    @Override
    public void onVideoMetaInfo(byte[] sps, byte[] pps) {

        //if (mMuxer == null) {
//        if(YYMediaMuxer.isMuxerStarted() == false) {
//            if (this.mVideoEncoder.getOutputFormat() != null) {
//                YYMediaMuxer.startMuxerIfReady(videoPath, mAudioEncoder.getOutputFormat(),mVideoEncoder.getOutputFormat());
//                audioDataBytes = 0; // 把实际录像之前的声音数据（用于计算时间戳）清0， 多线程？
//            }
//        }

        LogUtils.LOGI(TAG, "onVideoMetaInfo");

    }

    @Override
    public void onVideoFrame(byte[] videoData, int isKey, long presentationTimeUs) {

        if(videoMuxer == null) {
            startVideoMuxer();
        }

        if( this.start_mills == 0 ) {
            this.start_mills = System.nanoTime()/1000;//System.currentTimeMillis();
        }

        //long ticks = tickList.get(0).longValue();
        //tickList.remove(0);

        //writeVideoSampleData(videoData, ticks, isKey);
        writeVideoSampleData(videoData, presentationTimeUs, isKey);

    }

    @Override
    public void onVideoFrame(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {

        if(videoMuxer == null) {
            startVideoMuxer();
        }


        if( this.start_mills == 0 ) {
            this.start_mills = System.nanoTime()/1000;//System.currentTimeMillis();
        }

        long ticks = tickList.get(0).longValue();
        tickList.remove(0);
        bufferInfo.presentationTimeUs = ticks;
        writeVideoSampleData(byteBuf, bufferInfo);

    }


    private byte[] dataZero;
    private float tempo = 0;
    private short[] tempo_out_data;
    private short[] dataShort;

    //public void onRawAudioData(byte[] data, int offset, int length) {
    public void writeAudioData(byte[] data) {

        //LogUtils.DebugLog("AAAAAAAA", " + writeAudioData " + data.length );

        long pts = getDuration()*1000;
        audioDataBytes += data.length;
        // 执行编码操作
        if( mAudioEncoder != null ) {
            mAudioEncoder.encodeFrame(data, pts);
        }

    }

    private long frameCount = 0;

    private ArrayList<Long> tickList = new ArrayList<Long>();

    public void writeVideoData(long ticks) {


       // LogUtils.DebugLog("AAAAAAAAbbbbb", " + writeVideoData " + ticks + " " );
        tickList.add(new Long(ticks));

        long nowTimestamp = System.currentTimeMillis();
        long diff = nowTimestamp - lastUpdateTimestamp;

        if(mVideoEncoder != null) {
            long timeoutUs = 0;
            if(tickList.size() > 5 ) {
                timeoutUs = 20 * 1000;
            } else if (tickList.size() > 10) {
                timeoutUs = 50*1000;
            } else if(tickList.size() > 20) {
                timeoutUs = 120 * 1000;
            }
            mVideoEncoder.encodeBitmap(null, timeoutUs);
        }


    }

    private void  startAudioMuxer(){
        try{
            //Log.e("testtest","MediaMuxer start: " + this.videoPath);
            audioMuxer = new MediaMuxer(audioFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //todo 这里取得format对吗？ 还是要等encode里面实际取到的地方？
            // todo 这边还是有这个问题， 老荣耀这边就会失败

            audioTrackIndex = audioMuxer.addTrack(mAudioEncoder.getOutputFormat());

            audioMuxer.start();

            //muxerStarted = true;
            LogUtils.LOGS("mux start success");

        }catch (Exception e)
        {
            audioMuxer = null;
            //muxerStarted = false;
            LogUtils.LOGS("mux start failed");
            e.printStackTrace();
        }
    }

    private void  startVideoMuxer(){
        try{
            //Log.e("testtest","MediaMuxer start: " + this.videoPath);
            videoMuxer = new MediaMuxer(videoFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //todo 这里取得format对吗？ 还是要等encode里面实际取到的地方？
            // todo 这边还是有这个问题， 老荣耀这边就会失败

            videoTrackIndex = videoMuxer.addTrack(mVideoEncoder.getOutputFormat());

            videoMuxer.start();

            //muxerStarted = true;
            LogUtils.LOGS("mux start success");

        }catch (Exception e)
        {
            videoMuxer = null;
            //muxerStarted = false;
            LogUtils.LOGS("mux start failed");
            e.printStackTrace();
        }
    }

    private void writeAudioSampleData(byte[] data, long ticks) {

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
        synchronized (audioMuxer) {
            if(audioMuxer != null) {
                audioMuxer.writeSampleData(audioTrackIndex,buffer, bufferInfo);
            }

        }

    }

    private void writeVideoSampleData(byte[] data, long ticks, int isKey) {

        if(videoTrackIndex < 0) {
            return;
        }

        //addVideoSamples();
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
            if(videoMuxer != null) {
                videoMuxer.writeSampleData(videoTrackIndex,buffer, bufferInfo);
            }

        }
    }

    private void writeVideoSampleData(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {

        if(videoTrackIndex < 0) {
            return;
        }

        synchronized (this) {
            if(videoMuxer != null) {
                videoMuxer.writeSampleData(videoTrackIndex,byteBuf, bufferInfo);
            }

        }
    }


    public Surface getInputSurface() {

        return this.mVideoEncoder.getInputSurface();
    }


}
