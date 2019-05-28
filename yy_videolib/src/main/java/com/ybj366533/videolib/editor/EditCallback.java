package com.ybj366533.videolib.editor;

public interface EditCallback {

    void onInitReady();  //prepared
    void onPlayComplete();

    //void onComposeFinish(String outputPath);

    //void onProgress(final long duration);

    //void onMaxDuration();

    void onError(int errorCode);

    //void onInitReady();

    //void onDrawReady();

    //void onPictureBack(Bitmap var1);

    //void onPictureDataBack(byte[] var1);

    // 合成部分的callback 要和编辑部分的分开吗？

    void onProgress(int progress);   // 0--100
    void onComposeFinish(int reason);   // 0 正常结束， 非0 非正常结束（包括用户取消）


}
