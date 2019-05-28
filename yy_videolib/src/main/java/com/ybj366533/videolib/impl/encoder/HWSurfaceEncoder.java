
package com.ybj366533.videolib.impl.encoder;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.ybj366533.videolib.impl.YYRes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ybj366533.videolib.utils.LogUtils;

public class HWSurfaceEncoder implements IVideoEncoder {

    private static final String TAG = "YYREC";

    private byte[] sps_data = null;
    private byte[] pps_data = null;

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private boolean onlyIFrame;


    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 20; // 30 fps
    private static final int IFRAME_INTERVAL = 1; // 10 seconds between I-frames

    private static final int FRAME_RATE_ONLY_IFRAME = 20;//5;//15;//15; // 30 fps
    private static final int IFRAME_INTERVAL_ONLY_IFRAME = 0;//1; // 10 seconds between I-frames

    private static final int TIMEOUT_MS = 30;

    private MediaCodec mEncoder;
    private Surface mInputSurface;

    private MediaFormat goodFormat = null;

    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    ///////////////////////////////////////////////////////
    private Bitmap lastBitmap = null;
    private long lastTimestamp = 0;
    //private int[] rgbaBuff = null;
    ///////////////////////////////////////////////////////

    private IAVCCallback mCallback;

    public HWSurfaceEncoder() {
    }

    public void setCallback(IAVCCallback callback) {

        this.mCallback = callback;
    }

    public Surface getInputSurface() {

        return this.mInputSurface;
    }

    public int startEncoder(int width, int height, int bitrate, boolean onlyIFrame) {

        mWidth = width;
        mHeight = height;
        mBitRate = bitrate;
        this.onlyIFrame = onlyIFrame;

        try {
            prepareEncoder();
        } catch (IOException e) {
            e.printStackTrace();
            return YYRes.RESULT_ERR_UNKNOW;
        }

        if( mEncoder == null ) {
            return YYRes.RESULT_ERR_UNKNOW;
        }

        return YYRes.RESULT_OK;
    }

    public int encodeFrame(byte[] data, int yuvWidth, int yuvHeight, int degree) {

        return 0;
    }

    public int getCurrentBitrate() {

        return this.mBitRate;
    }

    public int encodeBitmap(Bitmap bmp) {
        return encodeBitmap(bmp, 0);
    }

    public int encodeBitmap(Bitmap bmp, long timeoutUs) {

        synchronized (HWSurfaceEncoder.this) {

            if( mEncoder == null ) {
                return -1;
            }

            try {


                while(true) {

                    int index = mEncoder.dequeueOutputBuffer(mBufferInfo, timeoutUs);

                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        resetOutputFormat();
                        formatAvailable = true;
                    } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    } else if (index >= 0) {

                        encodeToVideoTrack(index);
                        mEncoder.releaseOutputBuffer(index, false);
                    }
                    else {
                        LogUtils.LOGE(TAG, "unknow status !" + index);
                        break;
                    }
                }

            }
            catch (Exception ex) {

                LogUtils.LOGE(TAG, "encodeBitmap exception " + ex.getMessage());
            }
        }


        return YYRes.RESULT_OK;
    }

    public void stopEncoder() {

        synchronized (HWSurfaceEncoder.this) {

            if( mEncoder == null ) {
                return;
            }

            this.release();
        }
    }

    private void encodeToVideoTrack(int encoderStatus) {

        ByteBuffer encodedData = null;

        if( Build.VERSION.SDK_INT > 20 ) {
            encodedData = mEncoder.getOutputBuffer(encoderStatus);
        }
        else {
            ByteBuffer[] buffers = mEncoder.getOutputBuffers();
            encodedData = buffers[encoderStatus];
        }

        if (encodedData == null) {
            throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
        }

        boolean isMeta = false;
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
            //if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            Log.e(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            //bufferInfo.size = 0;

            // 获取sps和pps
            encodedData.position(mBufferInfo.offset);
            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

            byte[] data = new byte[mBufferInfo.size];
            encodedData.get(data);

            int sps_offset,sps_len,pps_offset,pps_len;
            sps_offset = pps_offset = -1;

            for( int i=0; i<data.length-4; i++ ) {

                if( data[i] == 0x00 && data[i+1] == 0x00 && data[i+2] == 0x00 && data[i+3] == 0x01 ) {
                    if( sps_offset == -1 ) {
                        sps_offset = i+4;
                        continue;
                    }
                    if( pps_offset == -1 ) {
                        pps_offset = i+4;
                        continue;
                    }
                }
            }

            pps_len = data.length - pps_offset;
            sps_len = data.length - pps_len - 8;

            sps_data = new byte[sps_len];
            pps_data = new byte[pps_len];

            System.arraycopy(data, sps_offset, sps_data, 0, sps_len);
            System.arraycopy(data, pps_offset, pps_data, 0, pps_len);

            isMeta = true;

            if( mCallback != null ) {
                mCallback.onVideoMetaInfo(sps_data, pps_data);
            }
        }

        if( !isMeta ) {

//            encodedData.position(mBufferInfo.offset);
//            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
//
//            byte[] data = new byte[mBufferInfo.size];
//            encodedData.get(data);
//
//            final int data_offset,isKey;
//
//            // if data begin with start code
//            if( data[0] == 0 && data[1] == 0 && data[2] == 0 && data[3] == 1 ) {
//
//                data_offset = 4;
//            }
//            else {
//
//                data_offset = 0;
//            }
//
//
//            byte flag = data[data_offset];
//            flag &= 0x1F;
//
//            if( flag == 5 ) {
//                isKey = 1;
//            }
//            else {
//                isKey = 0;
//            }


            if( mCallback != null ) {
                //mCallback.onVideoFrame(data, isKey,mBufferInfo.presentationTimeUs);
                mCallback.onVideoFrame(encodedData, mBufferInfo);
            }
        }
    }

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen once
        MediaFormat newFormat = mEncoder.getOutputFormat();
        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());

        {
            try {
                //MediaFormat newFormat = mEncoder.getOutputFormat();
                LogUtils.LOGS("video format change :" + newFormat.getByteBuffer("csd-0").position()
                        + " " +newFormat.getByteBuffer("csd-0").limit()
                        + " " + newFormat.getString(MediaFormat.KEY_MIME)
                        + " " + newFormat.getInteger(MediaFormat.KEY_WIDTH)
                        + " " + newFormat.getInteger(MediaFormat.KEY_HEIGHT));
            }catch (Exception e) {
                LogUtils.LOGS("video format get failed");
                LogUtils.LOGS(e.getMessage());
            }

        }

        if (goodFormat == null) {
            goodFormat = mEncoder.getOutputFormat();
        }
    }

    private void prepareEncoder() throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        if (this.onlyIFrame == true) {
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE_ONLY_IFRAME);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL_ONLY_IFRAME);
        } else {
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        }

        //format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        //format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        //format.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel3);


        String badName =null;

        // 首先尝试用type来创建(保留原有逻辑？)
        try {

            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);

            MediaCodecInfo.CodecCapabilities capabilities = mEncoder.getCodecInfo().getCapabilitiesForType(MIME_TYPE);
            int support_level = MediaCodecInfo.CodecProfileLevel.AVCLevel1;
            for (MediaCodecInfo.CodecProfileLevel profileLevel : capabilities.profileLevels) {
                if( profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline ) {
                    if( profileLevel.level > support_level ) {
                        support_level = profileLevel.level;
                    }
                }
            }
            // 根据mEncoder的信息设置level，挑选最大的level
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
            format.setInteger("level", support_level);

            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            LogUtils.LOGD(TAG, "created video format: " + format);
            mInputSurface = mEncoder.createInputSurface();

            mEncoder.start();
        }
        catch (Exception e) {

            LogUtils.LOGE(TAG, "### createEncoderByType failed : " + e.getMessage());

            if( mEncoder != null ) {
                badName = mEncoder.getName();
//                String badName = mEncoder.getName();
//                if( validMediaCodecMap.containsKey(badName) ) {
//                    Object o = validMediaCodecMap.get(badName);
//                    validMediaCodecMap.remove(o);
//                }
                try {
                    mEncoder.stop();
                    mEncoder.release();
                }catch (Exception e1 ) {
                    e1.printStackTrace();
                }
                mEncoder = null;
            }
        }


        // 通过createEncoderByType创建失败，尝试其他候选
        if( mEncoder == null ) {

            Map<String, MediaCodecInfo> validMediaCodecMap = new HashMap<>();
            {
                int codecSize = MediaCodecList.getCodecCount();

                for (int i = 0; i < codecSize; i++) {

                    MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
                    if (!mediaCodecInfo.isEncoder()) {
                        continue;
                    }

                    String[] types = mediaCodecInfo.getSupportedTypes();
                    boolean typeValid = false;
                    for (String ty : types) {
                        if (MIME_TYPE.equalsIgnoreCase(ty)) {
                            typeValid = true;
                            break;
                        }
                    }

                    if (typeValid == false)
                        continue;

                    MediaCodecInfo.CodecCapabilities capabilities = null;
                    try {
                        capabilities = mediaCodecInfo.getCapabilitiesForType(MIME_TYPE);
                    }
                    catch (Exception e1) {
                        capabilities = null;
                        // 有可能有些编码器无法获取
                        LogUtils.LOGE(TAG, "getCapabilitiesForType failed: " + mediaCodecInfo.toString() + " -- " + e1.getMessage());
                    }
                    if( capabilities == null ) {
                        continue;
                    }

                    int codec_color_format = 0;
                    for (int color_format : capabilities.colorFormats) {
                        if (color_format == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                            codec_color_format = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
                            break;
                        }
                    }

                    if (codec_color_format != MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                        continue;
                    }

                    int support_baseline = 0;
                    for (MediaCodecInfo.CodecProfileLevel profileLevel : capabilities.profileLevels) {
                        if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline ) {
                            support_baseline = 1;
                            break;
                        }
                    }

                    if( support_baseline > 0 ) {
                        validMediaCodecMap.put(mediaCodecInfo.getName(), mediaCodecInfo);
                    }
                }
            }

            if(badName != null && validMediaCodecMap.containsKey(badName) ) {
                Object o = validMediaCodecMap.get(badName);
                validMediaCodecMap.remove(o);
            }

            while(validMediaCodecMap.isEmpty() == false && mEncoder == null) {

//                List<MediaCodecInfo> v = new ArrayList<>();
//                v.add(validMediaCodecMap.get("OMX.google.h264.encoder"));
//                v.add(validMediaCodecMap.get("OMX.qcom.video.encoder.avc"));

                Collection<MediaCodecInfo> v =  validMediaCodecMap.values();
                for( MediaCodecInfo info:v) {

                    validMediaCodecMap.remove(info);
                    MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(MIME_TYPE);

                    // 任意挑选一个baseline的level
                    int support_level = MediaCodecInfo.CodecProfileLevel.AVCLevel1;
                    for (MediaCodecInfo.CodecProfileLevel profileLevel : capabilities.profileLevels) {
                        if( profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline ) {
                            if( profileLevel.level > support_level ) {
                                support_level = profileLevel.level;
                            }
                        }
                    }

                    format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
                    format.setInteger("level", support_level);

                    try {

                        LogUtils.LOGE(TAG, "trying createByCodecName." + info.getName() + " lvl " + support_level);

                        mEncoder = MediaCodec.createByCodecName(info.getName());
                        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

                        LogUtils.LOGD(TAG, "created video format: " + format);
                        mInputSurface = mEncoder.createInputSurface();

                        mEncoder.start();

                        // 没有抛异常，就算成功。。。
                        if( mEncoder != null ) {
                            break;
                        }
                    }
                    catch (Exception e) {

                        LogUtils.LOGE(TAG, "### createByCodecName failed : " + info.getName() + " --> " + e.getMessage());

                        if( mEncoder != null ) {
                            try {
                                mEncoder.stop();
                                mEncoder.release();
                            }catch (Exception e1 ) {
                                e1.printStackTrace();
                            }
                            mEncoder = null;
                        }
                    }
                }
            }
        }


        if( mEncoder == null ) {
            LogUtils.LOGE(TAG, "create video encoder failed.");
            return;
        }

        //mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        //mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private void release() {

        if (mEncoder != null) {
            try {
                mEncoder.stop();
                mEncoder.release();
            }catch (Exception e1 ) {
                e1.printStackTrace();
            }
            mEncoder = null;
        }
    }

    private boolean formatAvailable = false;
    public MediaFormat getOutputFormat()
    {
//        if((mEncoder != null) && (formatAvailable == true))
//        {
//            return mEncoder.getOutputFormat();
//        }

        if((mEncoder != null) && (goodFormat != null))
        {
            return goodFormat;
        }
        return null;
    }

}
