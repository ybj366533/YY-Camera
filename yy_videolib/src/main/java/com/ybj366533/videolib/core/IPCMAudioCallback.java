package com.ybj366533.videolib.core;


public interface IPCMAudioCallback {
    void onRawAudioData(byte[] data, int offset, int length);
}
