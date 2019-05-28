package com.ybj366533.videolib.core;

/**
 * Created by YY on 2017/1/21.
 */

public interface IVideoTextureOutput {

    public abstract void newGLTextureAvailable(Object src, int textureId, int width, int height,long timstampNanos);
}
