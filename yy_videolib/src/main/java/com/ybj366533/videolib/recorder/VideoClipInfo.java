package com.ybj366533.videolib.recorder;

/**
 * Created by YY on 2018/2/5.
 */

public class VideoClipInfo {

    String fileName;
    int duration;
    float speed;

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }


}
