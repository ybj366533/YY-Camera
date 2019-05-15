package com.gtv.cloud.recorder;

/**
 * Created by gtv on 2018/2/5.
 */

public class GTVVideoClipInfo {

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
