package com.ybj366533.videolib.recorder;

public interface RecordCallback {

    void onPrepared(final VideoInfo videoInfo);
    // // 一段拍摄结束，文件是否有效
    void onRecordComplete(boolean validClip, long clipDuration);


    void onProgress(final long duration, VideoInfo videoInfo);

    // 录制达到设定的上限时间
    void onMaxDuration();

    // 导出结束
    void onExportComplete(boolean ok);

    // 中途发生错误
    void onError(int errorCode);

    // 摄像头权限
    void onCameraOpenFailed();

}
