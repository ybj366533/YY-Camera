package com.ybj366533.videolib.editor;


public class ExtractFrameInfo {
    private String filePath;
    private int timeStampMili;

    public ExtractFrameInfo(String filePath, int timeStampMili) {
        this.filePath = filePath;
        this.timeStampMili = timeStampMili;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getTimeStampMili() {
        return timeStampMili;
    }


}
