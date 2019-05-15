package com.gtv.cloud.editor;


public class GTVExtractFrameInfo {
    String filePath;

    public GTVExtractFrameInfo(String filePath, int timeStampMili) {
        this.filePath = filePath;
        this.timeStampMili = timeStampMili;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getTimeStampMili() {
        return timeStampMili;
    }

    int timeStampMili;
}
