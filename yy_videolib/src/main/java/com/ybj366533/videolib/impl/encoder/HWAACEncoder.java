package com.ybj366533.videolib.impl.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.ybj366533.videolib.impl.YYRes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ybj366533.videolib.utils.LogUtils;


public class HWAACEncoder implements IAudioEncoder {

    private static final String TAG = "YYREC";

    private MediaCodec mEncoder;

    private MediaCodec.BufferInfo bufferInfo;

    private IADTCallback mCallback;

    private MediaFormat goodFormat = null;

    private AtomicBoolean mQuit = new AtomicBoolean(true);

    public void setCallback(IADTCallback callback) {

        this.mCallback = callback;
    }

    public int startEncoder(int sampleRate, int channels, int bitrate) {

        try {
            mEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.LOGE(TAG, "aac encoder createEncoderByType failed." + e.getMessage());
        }

        try {

            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);//AAC-HE 64kbps
//            if( bitrate > 32000 ) {
//                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
//            }
//            else {
//                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            }
            // 默认使用LC
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            this.bufferInfo = new MediaCodec.BufferInfo();

            this.mEncoder.start();
        }
        catch (Exception ex) {

            LogUtils.LOGE(TAG, "aac encoder start failed." + ex.getMessage());
            this.mEncoder = null;
            mQuit.set(true);

            return YYRes.RESULT_ERR_UNKNOW;
        }

        mQuit.set(false);

        return YYRes.RESULT_OK;
    }

    public int encodeFrame(byte[] data, long presentationTimeUs) {
        synchronized (this) {

            if (mQuit.get() == true) {
                return -1;
            }

            try {
                byte[] read_buffer = data;

                ByteBuffer[] inputBuffers;
                ByteBuffer[] outputBuffers;

                ByteBuffer inputBuffer;
                ByteBuffer outputBuffer;

                int inputBufferIndex;
                int outputBufferIndex;

                byte[] outData;

                ///////////////////////
                inputBuffers = mEncoder.getInputBuffers();
                outputBuffers = mEncoder.getOutputBuffers();
                //////////////////////

                int offset = 0;
                while (offset < read_buffer.length) {

                    inputBufferIndex = mEncoder.dequeueInputBuffer(0);

                    if (inputBufferIndex >= 0) {
                        inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();

                        int availableSize = (inputBuffer.capacity() >= (read_buffer.length - offset)) ? (read_buffer.length - offset) : inputBuffer.capacity();
                        byte[] input = new byte[availableSize];

                        System.arraycopy(read_buffer, offset, input, 0, availableSize);
                        long timestamp = presentationTimeUs + (long)offset * 1000 * 1000/(44100*4);
                        offset += availableSize;

                        inputBuffer.put(input);

                        //long timestamp = System.nanoTime() / 1000;
                        // 44100(Hz) x 16(bit) x 1(Monoral)
                        mEncoder.queueInputBuffer(inputBufferIndex, 0, input.length, timestamp, 0);
                    }

                }

                outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0);

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {


                    {
                        try {
                            MediaFormat newFormat = mEncoder.getOutputFormat();
                            LogUtils.LOGS("audio format change :" + newFormat.getByteBuffer("csd-0").position() +
                                    " " + newFormat.getByteBuffer("csd-0").limit()
                                    + " " + newFormat.getString(MediaFormat.KEY_MIME));
                        } catch (Exception e) {
                            LogUtils.LOGS("video format get failed");
                            LogUtils.LOGS(e.getMessage());
                        }

                    }

                    if (goodFormat == null) {
                        goodFormat = mEncoder.getOutputFormat();
                    }

                }

                while (outputBufferIndex >= 0) {
                    outputBuffer = outputBuffers[outputBufferIndex];

                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);

                    //-------------
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

                        final byte[] meta = outData;

                        if (mCallback != null) {
                            mCallback.onAudioMetaInfo(meta);
                        }
                    } else if (outData.length > 7) {

                        // 跳过前7个字符
                        //final byte[] packet = new byte[outData.length-7];
                        //System.arraycopy(outData, 7, packet, 0, outData.length-7);
                        final byte[] packet = new byte[outData.length];
                        System.arraycopy(outData, 0, packet, 0, outData.length);

                        if (mCallback != null) {
                            mCallback.onAudioFrame(packet,bufferInfo.presentationTimeUs);
                        }

                    } else {
                        Log.e(TAG, "audio aac size invalid .");
                    }
                    //------------
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
                }

            } catch (Exception ex) {
                LogUtils.LOGE(TAG, "encodeFrame exception " + ex.getMessage());
            }

        }


        return YYRes.RESULT_OK;
    }

    public void stopEncoder() {

        LogUtils.LOGI(TAG, "stopEncoder");

        synchronized (this) {
            mQuit.set(true);

            if( mEncoder != null ) {
                mEncoder.stop();
                mEncoder = null;
            }
        }

    }

    public MediaFormat getOutputFormat()
    {
//        if(mEncoder != null)
//        {
//            return mEncoder.getOutputFormat();
//        }

        if(mEncoder != null && goodFormat != null)
        {
            return goodFormat;
        }
        return null;
    }

}
