package com.ybj366533.videolib.editor;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by YY on 2018/6/5.
 */

public interface IVideoComposer {
    void init(final String videoPath, final String editWorkFolder, EditCallback editCallback);
    void startCompose(final String filePath);
    void stopCompose();

    // 设置logo水印
    boolean setLogoBitmapAtRect(Bitmap bmp, Rect r);
    boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r);

    // 设定合成时的动画
    void setAnimation(String animationFolder, int animImageInterval, Rect r);
    void setAnimation(String prefix, String animationFolder, int animImageInterval, Rect r);
}
