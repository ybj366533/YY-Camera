package com.ybj366533.videolib.impl.encoder;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Created by YY on 16/4/12.
 */
public interface IVideoEncoder {

    public interface IAVCCallback {

        public void onVideoMetaInfo(byte[] sps, byte[] pps);
        public void onVideoFrame(byte[] data, int isKey, long presentationTimeUs);
        public void onVideoFrame(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo);
    }

    public void setCallback(IAVCCallback callback);
    public int startEncoder(int width, int height, int bitrate, boolean onlyIFrame);
    public int encodeFrame(byte[] data, int yuvWidth, int yuvHeight, int degree);
    public int encodeBitmap(Bitmap bmp);
    public int encodeBitmap(Bitmap bmp, long timeoutUs);
    public int getCurrentBitrate();
    public Surface getInputSurface();
    public void stopEncoder();

    public MediaFormat getOutputFormat();
}
