package com.gtv.cloud.core;


public interface IPCMAudioCallback {
    void onRawAudioData(byte[] data, int offset, int length);
}
