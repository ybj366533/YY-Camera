package com.ybj366533.videolib.impl.texture;

/**
 * Created by YY on 16/4/12.
 */
public abstract class BaseTexture {

    public class TEXTURE_TYPE {

        public static final int SURFACE_TEXTURE = 1;
        public static final int GLES_TEXTURE = 2;
    }

    public interface TextureListener {

        public abstract void onTextureReady(int type, BaseTexture obj);
    }

    public abstract boolean isReady();

    public abstract void init();

    public abstract void destroy();

    public abstract Object getTexture();

    public abstract int getType();

    public abstract int updateTexture();

    public abstract long getTimestamp();

    public abstract void addListener(TextureListener listener);
}
