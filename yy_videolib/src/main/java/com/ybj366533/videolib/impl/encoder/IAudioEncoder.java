package com.ybj366533.videolib.impl.encoder;

import android.media.MediaFormat;

/**
 * Created by YY on 16/4/12.
 */
public interface IAudioEncoder {

    public interface IADTCallback {

        public void onAudioMetaInfo(byte[] meta);
        public void onAudioFrame(byte[] data, long presentationTimeUs);
    }

    public void setCallback(IADTCallback callback);
    public int startEncoder(int sampleRate, int channels, int bitrate);
    public int encodeFrame(byte[] data, long presentationTimeUs);
    public void stopEncoder();

    public MediaFormat getOutputFormat();
}
