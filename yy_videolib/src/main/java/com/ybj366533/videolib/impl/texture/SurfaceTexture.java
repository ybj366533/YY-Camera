package com.ybj366533.videolib.impl.texture;

import android.opengl.GLES20;

import com.ybj366533.videolib.utils.LogUtils;

import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

/**
 * Created by gtv on 16/4/12.
 */
public class SurfaceTexture extends BaseTexture {

    private android.graphics.SurfaceTexture surfaceTexture;
    private int oesTextureId = OpenGlUtils.NO_TEXTURE;
    private TextureListener callback = null;
    private boolean isInited = false;
    private int newFrameCame = 0;

    public SurfaceTexture() {

        surfaceTexture = null;
        oesTextureId = OpenGlUtils.NO_TEXTURE;
        callback = null;
    }

    private android.graphics.SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new android.graphics.SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(android.graphics.SurfaceTexture surfaceTexture) {

            synchronized (surfaceTexture) {
                newFrameCame++;
            }

            try {

                if( callback != null ) {
                    callback.onTextureReady(TEXTURE_TYPE.SURFACE_TEXTURE, SurfaceTexture.this);
                }
            }
            catch (Exception ex) {
                // do nothing
                LogUtils.LOGE("KKLIVE", ex.getMessage());
            }
        }
    };

    @Override
    public boolean isReady() {
        return isInited;
    }

    @Override
    public void init() {

        if (oesTextureId == OpenGlUtils.NO_TEXTURE) {
            oesTextureId = OpenGlUtils.getExternalOESTextureID();
            if (oesTextureId != OpenGlUtils.NO_TEXTURE) {
                surfaceTexture = new android.graphics.SurfaceTexture(oesTextureId);
                surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);

                isInited = true;
            }
        }

        return;
    }

    @Override
    public void destroy() {

        if( surfaceTexture != null ) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if( oesTextureId != OpenGlUtils.NO_TEXTURE ) {
            GLES20.glDeleteTextures(1, new int[]{oesTextureId}, 0);
            oesTextureId = OpenGlUtils.NO_TEXTURE;
        }
        callback = null;

        isInited = false;
    }

    @Override
    public Object getTexture() {

        return surfaceTexture;
    }

    @Override
    public int getType() {

        return TEXTURE_TYPE.SURFACE_TEXTURE;
    }

    @Override
    public int updateTexture() {

        if( surfaceTexture == null ) {
            return -1;
        }

        // no data
        synchronized (surfaceTexture) {

            if( newFrameCame <= 0 ) {
                return -1;
            }
            newFrameCame = 0;
        }

        surfaceTexture.updateTexImage();

        return oesTextureId;
    }

    public long getTimestamp(){
        return surfaceTexture.getTimestamp();
    }

    @Override
    public void addListener(TextureListener listener) {

        callback = listener;
    }
}
