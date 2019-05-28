package com.ybj366533.videolib.impl.utils;

/**
 * Created by YY on 2017/1/25.
 */

public class DispRect {

    public int zOrder;
    public int x,y,width,height;
    public boolean visible;

    public DispRect() {
    }

    public DispRect(int zOrder, int x, int y, int width, int height) {

        this.zOrder = zOrder;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
    }
}
