package com.ybj366533.videolib.stream;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.opengl.EGLContext;

import com.ybj366533.videolib.core.VideoObject;
import com.ybj366533.videolib.core.IPCMAudioCallback;
import com.ybj366533.videolib.core.IVideoTextureOutput;
import com.ybj366533.videolib.impl.YYRes;
import com.ybj366533.videolib.impl.recorder.AudioRecorder;
import com.ybj366533.videolib.impl.utils.YYMediaMuxer;
import com.ybj366533.videolib.impl.recorder.EGLTextureRecorder;
import com.ybj366533.videolib.impl.setting.AVStreamSetting;
import com.ybj366533.videolib.impl.encoder.HWAACEncoder;
import com.ybj366533.videolib.impl.encoder.HWSurfaceEncoder;
import com.ybj366533.videolib.impl.encoder.IAudioEncoder;
import com.ybj366533.videolib.impl.encoder.IVideoEncoder;

import net.surina.soundtouch.SoundTouch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ybj366533.videolib.utils.LogUtils;


public class VideoRecordStreamer implements IAudioEncoder.IADTCallback, IVideoEncoder.IAVCCallback, IPCMAudioCallback, IVideoTextureOutput {

    private RecordCallback recordCallback;

    private static final String TAG = "Streamer";

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

    //private boolean muxerStarted = false;
    //private MediaMuxer mMuxerAudio;
    //private int videoTrackIndex = -1;
    //private int audioTrackIndex = -1;

    private long start_mills = 0;

    private double recordSpeed = 1;

    private boolean record_pause_flag = false;

    private boolean bgMusicFlag = false;
    private boolean musicRecordToFile = false;

    private boolean videoFirstFrameRecorded = false;

    private ArrayList<byte[]> cacheAudioDataList = new ArrayList<>();
    private ArrayList<Long> cacheAudioTickList = new ArrayList<>();

    public interface RecordCallback {
        //void onPrepared();  //todo?
        void onProgress(final int duration);
        void onComplete(final int duration);
        void onError(int errorCode);
    }

    public VideoRecordStreamer(String videoPath, AVStreamSetting setting, EGLContext context) {

        this.videoPath = videoPath;
        this.setting = setting;
        this.eglContext = context;

        start_mills = 0;
        videoFirstFrameRecorded = false;

        return;
    }

    public void setInputVideoSource(VideoObject vs) {

        if( this.mVideoSource != null ) {
            this.mVideoSource.removeOutput(this);
        }

        this.mVideoSource = vs;
    }

//    public void startRecord(String videoPath) {
//        this.videoPath = videoPath;
//        openStream(null);
//
//
//    }

    public int openStream(boolean bgMusicFlag, double recordSpeed, boolean musicRecordToFile, RecordCallback callback) {

        LogUtils.LOGI(TAG,"startWork url: " + this.videoPath);

        this.recordCallback = callback;
        this.bgMusicFlag = bgMusicFlag;
        this.musicRecordToFile = musicRecordToFile;
        this.recordSpeed = recordSpeed;

        //this.monitorSentTimestamp = 0;
        this.lastVideoFrameTimestamp = 0;
        this.lastAudioFrameTimestamp = 0;
        this.lastUpdateTimestamp = 0;

        this.lastProfileTimestamp = 0;

        initEncoderAll();

        YYMediaMuxer = new YYMediaMuxer();

        mQuit.set(false);

        return YYRes.RESULT_OK;
    }

    public void pauseRecord() {
        LogUtils.LOGI(TAG," pauseRecord");      //其实不允许的
        record_pause_flag = true;
    }

    public void resumeRecord() {
        LogUtils.LOGI(TAG," resumeRecord");
        if (this.recordSpeed > 1) {
//            mSoundTouch.setTempo(2.0f);
            mSoundTouch.setSonicSpeed(2.0f);
        } else if (this.recordSpeed < 1) {
//            mSoundTouch.setTempo(0.5f);
            mSoundTouch.setSonicSpeed(0.5f);
        }

        record_pause_flag = false;
    }

    public void closeStream() {

        LogUtils.LOGI(TAG,"close stream");


        synchronized (this) {

            mQuit.set(true);


            if( this.mVideoSource != null ) {
                this.mVideoSource.removeOutput(this);
                this.mVideoSource = null;
            }

            //mVideoEncoder.encodeBitmap(null,200*1000);

            this.destroyEncoder();

            YYMediaMuxer.close();


            if (recordCallback != null) {
                if(videoFirstFrameRecorded == true) {
                    recordCallback.onComplete((int)getDuration());
                } else {
                    recordCallback.onComplete(0);
                }

            }

            this.recordCallback = null;
        }

        return;
    }

    private int initEncoderAll() {

        int ret;

        ret = this.initEncoder();
        if( ret != YYRes.RESULT_OK ) {
            return ret;
        }

        // get video
        if( this.mVideoSource != null )
            this.mVideoSource.addOutput(this);

        this.mVideoEncoder.getOutputFormat();

        return YYRes.RESULT_OK;
    }

    private void initVideoAndAudioRecorder() {

    }
    private int initEncoder() {

        mSoundTouch = new SoundTouch();

        if (this.recordSpeed > 1) {
//            mSoundTouch.setTempo(2.0f);
            mSoundTouch.setSonicSpeed(2.0f);
        } else if (this.recordSpeed < 1) {
//            mSoundTouch.setTempo(0.5f);
            mSoundTouch.setSonicSpeed(0.5f);
        }

        this.mAudioEncoder = new HWAACEncoder();
        this.mVideoEncoder = new HWSurfaceEncoder();

        this.mEGLTextureRecorder = new EGLTextureRecorder(this.eglContext, this.setting.getVideoWidth(), this.setting.getVideoHeight());
        this.mEGLTextureRecorder.setVideoEncoder(this.mVideoEncoder);

        // set encoder callback
        this.mVideoEncoder.setCallback(this);
        this.mAudioEncoder.setCallback(this);

        // start all
        if( YYRes.RESULT_OK != this.mVideoEncoder.startEncoder(this.setting.getVideoWidth(), this.setting.getVideoHeight(), this.setting.getVideoBitrate(), this.setting.isOnlyIFrame())) {
            LogUtils.LOGS("initRecorder：NG ");
            return YYRes.RESULT_ERR_PARAM_NG;
        }
        this.mEGLTextureRecorder.startRecord();

        int channels = 2;

        if( this.setting.getAudioChannelFormat() == AudioFormat.CHANNEL_IN_MONO ) {
            channels = 1;
        }
        else if(  this.setting.getAudioChannelFormat() == AudioFormat.CHANNEL_IN_STEREO ) {
            channels = 2;
        }

        this.mAudioEncoder.startEncoder(this.setting.getAudioSampleRate(), channels, this.setting.getAudioBitrate());

        //String bgMusic = SDKSetting.getBGMusic();
        if ((this.bgMusicFlag == false) || ((this.bgMusicFlag == true) && (this.musicRecordToFile == false))) {

            this.AudioRecorder = new AudioRecorder(this);
            AudioRecorder.startRecord();
        }


        LogUtils.LOGS("initRecorder：OK ");
        return YYRes.RESULT_OK;
    }

    private void destroyEncoder() {

        if( this.mEGLTextureRecorder != null ) {
            this.mEGLTextureRecorder.stopRecord();
            this.mEGLTextureRecorder = null;
        }

        if( this.mVideoEncoder != null ) {
            this.mVideoEncoder.stopEncoder();
            this.mVideoEncoder = null;
        }

        if (this.AudioRecorder != null) {
            this.AudioRecorder.stopRecord();
            this.AudioRecorder = null;
        }

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

    private long lastTick = -1;


    public long currentVideoPTS() {

//        //return (System.nanoTime()/1000 - this.start_mills);
//        long tick = (audioDataBytes*1000/(44100*4/1000));
//        if ( lastTick >= 0 && tick <= lastTick) {
//            tick = lastTick + 1 * 1000;
//        }
//        lastTick = tick;

        long tick = 0;
        if(start_mills != 0) {
            tick = (System.nanoTime()/1000 - this.start_mills);
            if(recordSpeed > 1) {
                tick /=2;
            } else if (recordSpeed < 1) {
                tick *= 2;
            }
        } else {
            start_mills = System.nanoTime()/1000;
        }
        return tick;
    }

    public long currentVideoPTS(long timestampNanos) {

        long tick = 0;
        if(start_mills != 0) {
            tick = (timestampNanos/1000 - this.start_mills);
            if(recordSpeed > 1) {
                tick /=2;
            } else if (recordSpeed < 1) {
                tick *= 2;
            }
        } else {
            start_mills = timestampNanos/1000;
        }
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

        //if(mMuxer == null) {
        if(YYMediaMuxer.isMuxerStarted() == false) {     // 改为flag，避免在new 和 start之间 来了onAudioFrame
            cacheAudioDataList.add(data);
            cacheAudioTickList.add(presentationTimeUs);
            return;
        }

        // 视频之前的音频不要，确保音频时间戳在第一帧视频之后开始增长
        if(videoFirstFrameRecorded == false) {
            //保存在临时队列
            cacheAudioDataList.add(data);
            cacheAudioTickList.add(presentationTimeUs);
            return;
        }

        if( this.start_mills == 0 ) {
            this.start_mills = System.nanoTime()/1000;//System.currentTimeMillis();
        }

        int cacheAudioNum = cacheAudioDataList.size();
        if(cacheAudioNum > 0) {
            for(int i = 0; i < cacheAudioNum; ++i) {
                YYMediaMuxer.writeAudioSampleData(cacheAudioDataList.get(i), cacheAudioTickList.get(i));
            }
            cacheAudioDataList.clear();
            cacheAudioTickList.clear();
        }


        long    ticks = presentationTimeUs;//this.currentAudioPTS();

        YYMediaMuxer.writeAudioSampleData(data, ticks);
        if (recordCallback != null && (callbackFreq == 0 || callbackFreq > 2)) {
            recordCallback.onProgress( (int)getDuration());
            callbackFreq = 0;
        }
        callbackFreq++;

    }

    @Override
    public void onVideoMetaInfo(byte[] sps, byte[] pps) {

        if(YYMediaMuxer.isMuxerStarted() == false) {
            if (this.mVideoEncoder.getOutputFormat() != null) {
                YYMediaMuxer.startMuxerIfReady(videoPath, mAudioEncoder.getOutputFormat(),mVideoEncoder.getOutputFormat());
                //audioDataBytes = 0; // 把实际录像之前的声音数据（用于计算时间戳）清0， 多线程？
            }
        }

        LogUtils.LOGI(TAG, "onVideoMetaInfo");

    }

    @Override
    public void onVideoFrame(byte[] videoData, int isKey, long presentationTimeUs) {

        if(YYMediaMuxer.isMuxerStarted() == false) {
            if (this.mVideoEncoder.getOutputFormat() != null) {
                //startMutex();
                YYMediaMuxer.startMuxerIfReady(videoPath, mAudioEncoder.getOutputFormat(),mVideoEncoder.getOutputFormat());
                //audioDataBytes = 0; // 把实际录像之前的声音数据（用于计算时间戳）清0， 多线程？
            } else {
                return;
            }
        }

        // 确保第一帧时间戳是0
        if(videoFirstFrameRecorded == false) {
            //audioDataBytes = 0;
            videoFirstFrameRecorded = true;
        }

        //long    ticks = this.currentVideoPTS();
        //long ticks = videoPtsList.get((int)(presentationTimeUs/1000/1000)); // todo ,有风险吗？ 用map？
        //YYMediaMuxer.writeVideoData(videoData, ticks, isKey);
        YYMediaMuxer.writeVideoData(videoData, presentationTimeUs, isKey);
        //videoRecorded = true;

//        if (recordCallback != null) {
//            //recordCallback.onProgress( (int)getDuration());
//            recordCallback.onProgress( (int)(ticks/1000));
//            //callbackFreq = 0;
//        }
//        //callbackFreq++;
    }

    @Override
    public void onVideoFrame(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {

        if(YYMediaMuxer.isMuxerStarted() == false) {
            if (this.mVideoEncoder.getOutputFormat() != null) {
                //startMutex();
                YYMediaMuxer.startMuxerIfReady(videoPath, mAudioEncoder.getOutputFormat(),mVideoEncoder.getOutputFormat());
                //audioDataBytes = 0; // 把实际录像之前的声音数据（用于计算时间戳）清0， 多线程？
            } else {
                return;
            }
        }

        // 确保第一帧时间戳是0
        if(videoFirstFrameRecorded == false) {
            //audioDataBytes = 0;
            videoFirstFrameRecorded = true;
        }

        //long    ticks = this.currentVideoPTS();
        long presentationTimeUs = bufferInfo.presentationTimeUs;
        long ticks = videoPtsList.get((int)(presentationTimeUs/1000/1000)); // todo ,有风险吗？ 用map？
        bufferInfo.presentationTimeUs = ticks;
        YYMediaMuxer.writeVideoData(byteBuf, bufferInfo);
    }


    private byte[] dataZero;
    private float tempo = 0;
    private short[] tempo_out_data;
    private short[] dataShort;
    @Override
    public void onRawAudioData(byte[] data, int offset, int length) {

        if (record_pause_flag == true) {
            return;
        }

        long pts = getDuration() * 1000;

        if((bgMusicFlag == true) && (this.musicRecordToFile == false)) {
            Arrays.fill(data, (byte)0);
        }

        if (this.recordSpeed == 1) {
            audioDataBytes += data.length;
                // 执行编码操作
                if( mAudioEncoder != null ) {
                    mAudioEncoder.encodeFrame(data, pts);
                }
        } else {

            // 变速时候录音全0，快速，数据长度变为1/2， 慢速则变为2倍
            int dataZeroLen = length;

            // todo 4096 需要调整？
            if (tempo_out_data ==  null) {
                tempo_out_data = new short[8192];
            }

            if (dataShort ==  null || dataShort.length < data.length) {
                dataShort = new short[data.length];     // 实际一半就够，但是多点安全点
            }

            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(dataShort, 0, length/2);
//            int len = mSoundTouch.processMemory(dataShort, length/2, tempo_out_data, 8192);
            int len = mSoundTouch.processSonicMemory(dataShort, length/2, tempo_out_data, 8192);

            if (len > 0) {
                audioDataBytes += len * 2;
                if (dataZero == null || len * 2 != dataZero.length) {
                    dataZero = new byte[len * 2];
                }
                //ShortBuffer.wrap(tempo_out_data).asByteBuffer.get(dataZero);
                ByteBuffer.wrap(dataZero).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(tempo_out_data, 0, len);

                if( mAudioEncoder != null ) {
                    mAudioEncoder.encodeFrame(dataZero, pts);
                }
            }

        }


    }

    private long frameCount = 0;
    private long encodedFrameCount = 0;
    private ArrayList<Long> videoPtsList = new ArrayList<Long>();
    @Override
    public void newGLTextureAvailable(Object src, int textureId, int width, int height, long timestampNanos) {


        if (record_pause_flag == true) {
            return;
        }

        // 风险点，现在是等到声音的format之后才投放 图片
        if(mAudioEncoder.getOutputFormat() == null) {
            return;
        }

        long nowTimestamp = System.currentTimeMillis();
        long diff = nowTimestamp - lastUpdateTimestamp;

        frameCount++;
        if(this.recordSpeed > 1) {
            if(frameCount % 2 == 0) {
                return;
            }
        }

        lastUpdateTimestamp = nowTimestamp;

        if( this.mEGLTextureRecorder != null ) {
            long pts = currentVideoPTS(timestampNanos);
            videoPtsList.add(pts);
            this.mEGLTextureRecorder.setTextureId(textureId);
            this.mEGLTextureRecorder.frameAvailable(width, height,false,(long)encodedFrameCount * 1000 * 1000 * 1000);
            encodedFrameCount++;
        }

    }

    public void writeVideoFrame(final int textureId, final int textureWidth, final int textureHeight, final boolean flip, long timestampNanos) {
        if (record_pause_flag == true) {
            return;
        }

        // 风险点，现在是等到声音的format之后才投放 图片
        if(mAudioEncoder.getOutputFormat() == null) {
            return;
        }

        long nowTimestamp = System.currentTimeMillis();
        long diff = nowTimestamp - lastUpdateTimestamp;

        frameCount++;
        if(this.recordSpeed > 1) {
            if(frameCount % 2 == 0) {
                return;
            }
        }

        lastUpdateTimestamp = nowTimestamp;

        if( this.mEGLTextureRecorder != null ) {
            long pts = currentVideoPTS(timestampNanos);
            videoPtsList.add(pts);
            this.mEGLTextureRecorder.setTextureId(textureId);
            //this.mEGLTextureRecorder.frameAvailable(textureWidth, textureHeight,flip,(long)encodedFrameCount * 1000 * 1000 * 1000);
            this.mEGLTextureRecorder.frameAvailable(textureWidth, textureHeight,flip,pts * 1000);
            encodedFrameCount++;
        }
    }

}
