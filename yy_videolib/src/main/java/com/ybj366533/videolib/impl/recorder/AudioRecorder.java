package com.ybj366533.videolib.impl.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.ybj366533.videolib.core.IPCMAudioCallback;
import com.ybj366533.videolib.impl.setting.AVStreamSetting;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ybj366533.videolib.utils.LogUtils;


public class AudioRecorder extends Thread {

    private static final String TAG = "YYVideo";

//    private int frameSize = AVStreamSetting.DEF_ECHO_FRAMESIZE;
    private int sampleRateRecord;
    private int sampleRatePlay;
    private int channelFormat;
    private int encodingFormat;

    private IPCMAudioCallback callback;

    private AudioRecord record;
    private AudioTrack track;


    private AtomicBoolean mQuit = new AtomicBoolean(false);

    public AudioRecorder(IPCMAudioCallback c) {

        this.sampleRateRecord = AVStreamSetting.AUDIO_SAMPLERATE;
        this.sampleRatePlay = AVStreamSetting.AUDIO_SAMPLERATE;
        this.channelFormat = AudioFormat.CHANNEL_IN_STEREO;
        this.encodingFormat = AudioFormat.ENCODING_PCM_16BIT;

        this.callback = c;
    }

    public void startRecord() {

        try {

            this.start();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return;
    }

    public void stopRecord() {

        mQuit.set(true);
        try {
            this.join(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            int frameSize_rec = 2048;//4096;

            int bufferSize = AudioRecord.getMinBufferSize(this.sampleRateRecord, this.channelFormat, this.encodingFormat);

			if(bufferSize < this.sampleRateRecord) {
                bufferSize = this.sampleRateRecord;
            }
            record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        this.sampleRateRecord, this.channelFormat,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            short[] read_buffer = new short[frameSize_rec];

            try {
                record.startRecording();
            }
            catch(Exception e) {
                e.printStackTrace();
                LogUtils.LOGE(TAG, e.getMessage());
            }

            while(!mQuit.get()) {

                // record audio
                int left = read_buffer.length;
                int error = 0;
                while (left>0) {
                    int bufferReadResult = record.read(read_buffer, read_buffer.length-left, left);
                    if( bufferReadResult < 0 ) {
                        LogUtils.LOGE(TAG, "audio data size not enough (" + bufferReadResult + ")...");
                        error ++;
                        break;
                    }
                    left -= bufferReadResult;

                    if( mQuit.get() ) {
                        break;
                    }
                }
                if( error > 0 )
                    continue;

                // 采集数据输出给外部
                if( callback != null ) {
                    

                    byte[] output_bytes = new byte[read_buffer.length*2];
                    ByteBuffer.wrap(output_bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(read_buffer);

                    callback.onRawAudioData(output_bytes, 0, output_bytes.length);
                }
            }


            record.release();
            record = null;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            LogUtils.LOGE(TAG, ex.getMessage());
        }

        return;
    }


    public int getChannelFormat() {
        return channelFormat;
    }

    public void setChannelFormat(int channelFormat) {
        this.channelFormat = channelFormat;
    }

    public int getEncodingFormat() {
        return encodingFormat;
    }

    public void setEncodingFormat(int encodingFormat) {
        this.encodingFormat = encodingFormat;
    }
}
