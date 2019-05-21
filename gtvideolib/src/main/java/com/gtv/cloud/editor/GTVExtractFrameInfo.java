package com.gtv.cloud.editor;


public class GTVExtractFrameInfo {
    private String filePath;
    private int timeStampMili;

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


}
