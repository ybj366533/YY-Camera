package com.ybj366533.videolib.editor;

/**
 * Created by YY on 2018/2/6.
 */

public class ComposerCreator {

    public static IVideoComposer getInstance() {

        return new VideoComposer();
    }
}
