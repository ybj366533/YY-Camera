package com.ybj366533.gtvimage.gtvfilter.filter.advanced;


import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import java.nio.FloatBuffer;
import java.util.List;


public class GTVGroupFilter extends GTVImageFilter {

    protected int[] frameBuffers = null;
    protected int[] frameBufferTextures = null;
    private int frameWidth = -1;
    private int frameHeight = -1;
    protected List<GTVImageFilter> filters;

    public GTVGroupFilter(List<GTVImageFilter> filters) {
        this.filters = filters;
    }

    @Override
    public void onDestroy() {
        for (GTVImageFilter filter : filters) {
            filter.destroy();
        }
        destroyFramebuffers();
    }

    @Override
    public void init() {
        for (GTVImageFilter filter : filters) {
            filter.init();
        }
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        int size = filters.size();
        for (int i = 0; i < size; i++) {
            filters.get(i).onInputSizeChanged(width, height);
        }
        if (frameBuffers != null && (frameWidth != width || frameHeight != height || frameBuffers.length != size)) {
            destroyFramebuffers();
            frameWidth = width;
            frameHeight = height;
        }
        if (frameBuffers == null) {
            frameBuffers = new int[size];
            frameBufferTextures = new int[size];

            for (int i = 0; i < size; i++) {
                GLES20.glGenFramebuffers(1, frameBuffers, i);

                GLES20.glGenTextures(1, frameBufferTextures, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures[i]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, frameBufferTextures[i], 0);

                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
    }

    @Override
    public int onDrawFrame(final int textureId) {

        if (frameBuffers == null || frameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }

        int size = filters.size();
        int previousTexture = textureId;
        for (int i = 0; i < size; i++) {
            GTVImageFilter filter = filters.get(i);
            //Log.e("testtest44","----------------------" + i + " " + mIntputWidth + " " + mIntputHeight);

            GLES20.glViewport(0, 0, mIntputWidth, mIntputHeight);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);

            filter.onDrawFrame(previousTexture);
            previousTexture = frameBufferTextures[i];

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            //Log.e("testtest44","----------------------" + i );
        }

        return previousTexture;
    }

    @Override
    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {
        if (frameBuffers == null || frameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }
        int size = filters.size();
        int previousTexture = textureId;
        for (int i = 0; i < size; i++) {
            GTVImageFilter filter = filters.get(i);

            GLES20.glViewport(0, 0, mIntputWidth, mIntputHeight);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);
            GLES20.glClearColor(0, 0, 0, 0);

            boolean isNotLast = i < size - 1;

            if (isNotLast) {
                filter.onDrawFrame(previousTexture, mGLCubeBuffer, mGLTextureBuffer);
            } else {
                filter.onDrawFrame(previousTexture, cubeBuffer, textureBuffer);
            }

            previousTexture = frameBufferTextures[i];

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
        return previousTexture;
    }

    public int onDrawFrameInRectWithAlpha(final int textureId, final FloatBuffer cubeBuffer,
                                          final FloatBuffer textureBuffer, int x, int y, int width, int height, boolean alpha) {

        if (frameBuffers == null || frameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }
        int size = filters.size();
        int previousTexture = textureId;
        for (int i = 0; i < size; i++) {
            GTVImageFilter filter = filters.get(i);

            GLES20.glViewport(x, y, width, height);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);
            GLES20.glClearColor(0, 0, 0, 0);

            if (alpha) {
                GLES20.glEnable(GLES20.GL_BLEND);
                //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                //png的素材像素已经乘以了alpha
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            }

            boolean isNotLast = i < size - 1;

            if (isNotLast) {
                filter.onDrawFrame(previousTexture, mGLCubeBuffer, mGLTextureBuffer);
            } else {
                filter.onDrawFrame(previousTexture, cubeBuffer, textureBuffer);
            }

            previousTexture = frameBufferTextures[i];

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            if (alpha) {
                GLES20.glDisable(GLES20.GL_BLEND);
                //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
        return previousTexture;
    }

    public int onDrawFrameInRect(final int textureId, final FloatBuffer cubeBuffer,
                                 final FloatBuffer textureBuffer, int x, int y, int width, int height) {

        return onDrawFrameInRectWithAlpha(textureId, cubeBuffer, textureBuffer, x, y, width, height, false);
    }

    private void destroyFramebuffers() {
        if (frameBufferTextures != null) {
            GLES20.glDeleteTextures(frameBufferTextures.length, frameBufferTextures, 0);
            frameBufferTextures = null;
        }
        if (frameBuffers != null) {
            GLES20.glDeleteFramebuffers(frameBuffers.length, frameBuffers, 0);
            frameBuffers = null;
        }
    }

    // f必须是已经init好的
    public void addFilter(GTVImageFilter f) {

        filters.add(f);
    }

    public void removeFilter(GTVImageFilter f) {

        if (f != null && filters != null) {
            filters.remove(f);
        }
    }

    public boolean haveFilter(GTVImageFilter f) {

        if (f != null && filters != null) {
            return filters.contains(f);
        }

        return false;
    }

    public GTVImageFilter getFilter(int index) {

        return filters.get(index);
    }

    public int getFilterCount() {
        return filters.size();
    }

    @Override
    public void onDisplaySizeChanged(final int width, final int height) {
        super.onDisplaySizeChanged(width, height);
        int size = filters.size();
        for (int i = 0; i < size; i++) {
            //Log.e("testtest77", " onDisplaySizeChanged" + i );
            filters.get(i).onDisplaySizeChanged(width, height);
        }
    }
}
