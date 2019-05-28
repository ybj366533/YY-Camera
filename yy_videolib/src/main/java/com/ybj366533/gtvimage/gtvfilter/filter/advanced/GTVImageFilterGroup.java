

package com.ybj366533.gtvimage.gtvfilter.filter.advanced;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil.CUBE;
import static com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil.TEXTURE_NO_ROTATION;


public class GTVImageFilterGroup extends GTVImageFilter {

    protected List<GTVImageFilter> mFilters;
    protected List<GTVImageFilter> mMergedFilters;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGLTextureFlipBuffer;

    /**
     * Instantiates a new GTVImageFilterGroup with no filters.
     */
    public GTVImageFilterGroup() {
        this(null);
    }

    /**
     * Instantiates a new GTVImageFilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    public GTVImageFilterGroup(List<GTVImageFilter> filters) {
        mFilters = filters;
        if (mFilters == null) {
            mFilters = new ArrayList<GTVImageFilter>();
        } else {
            updateMergedFilters();
        }

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

        float[] flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
        mGLTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureFlipBuffer.put(flipTexture).position(0);
    }

    public void addFilter(GTVImageFilter aFilter) {
        if (aFilter == null) {
            return;
        }
        mFilters.add(aFilter);
        //Log.e("testtest77","----------------------gpuimage filter add " + mFilters.size());
        updateMergedFilters();
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.GTVImageFilter#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();
        for (GTVImageFilter filter : mFilters) {
            //Log.e("testtest77","----------------------gpuimage filter init ");
            filter.init();
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.GTVImageFilter#onDestroy()
     */
    @Override
    public void onDestroy() {
        destroyFramebuffers();
        for (GTVImageFilter filter : mFilters) {
            filter.destroy();
        }
        super.onDestroy();
    }

    private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * jp.co.cyberagent.android.gpuimage.GTVImageFilter#onOutputSizeChanged(int,
     * int)
     */
    @Override
    public void onDisplaySizeChanged(final int width, final int height) {
        super.onDisplaySizeChanged(width, height);
//        if (mFrameBuffers != null) {
//            destroyFramebuffers();
//        }

        int size = mFilters.size();
        for (int i = 0; i < size; i++) {
            mFilters.get(i).onDisplaySizeChanged(width, height);
        }

//        if (mMergedFilters != null && mMergedFilters.size() > 0) {
//            size = mMergedFilters.size();
//            mFrameBuffers = new int[size - 1];
//            mFrameBufferTextures = new int[size - 1];
//
//            for (int i = 0; i < size - 1; i++) {
//                GLES20.glGenFramebuffers(1, mFrameBuffers, i);
//                GLES20.glGenTextures(1, mFrameBufferTextures, i);
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
//                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
//                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
//                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
//                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
//                        GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);
//
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//            }
//        }
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        int size = mFilters.size();
        for (int i = 0; i < size; i++) {
            mFilters.get(i).onInputSizeChanged(width, height);
        }
        if(mFrameBuffers != null){
            destroyFramebuffers();
//            frameWidth = width;
//            frameHeight = height;
        }

        //Log.e("testtest77","----------------------onInputSizeChanged" + " " + width + " " + height);

        if (mFrameBuffers == null) {
            mFrameBuffers = new int[size];
            mFrameBufferTextures = new int[size];

            for (int i = 0; i < size; i++) {
                GLES20.glGenFramebuffers(1, mFrameBuffers, i);

                GLES20.glGenTextures(1, mFrameBufferTextures, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
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

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);

                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.GTVImageFilter#onDraw(int,
     * java.nio.FloatBuffer, java.nio.FloatBuffer)
     */
    @SuppressLint("WrongCall")    
    @Override
    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }
        int previousTexture = textureId;
        if (mMergedFilters != null) {
            int size = mMergedFilters.size();

            for (int i = 0; i < size; i++) {
                GTVImageFilter filter = mMergedFilters.get(i);
                boolean isNotLast = i < size - 1;
                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                    GLES20.glClearColor(0, 0, 0, 0);
                }

                if (i == 0) {
                    filter.onDrawFrame(previousTexture, cubeBuffer, textureBuffer);
                } else if (i == size - 1) {
                    filter.onDrawFrame(previousTexture, mGLCubeBuffer, (size % 2 == 0) ? mGLTextureFlipBuffer : mGLTextureBuffer);
                } else {
                    filter.onDrawFrame(previousTexture, mGLCubeBuffer, mGLTextureBuffer);
                }

                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    previousTexture = mFrameBufferTextures[i];
                }
            }
        }
        return previousTexture;
     }

    @Override
    public int onDrawFrame(final int textureId) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }
        int previousTexture = textureId;

        if (mMergedFilters != null) {
            int size = mMergedFilters.size();
//            for (int i = 0; i < size; i++) {
//                GTVImageFilter filter = mMergedFilters.get(i);
//                boolean isNotLast = i < size - 1;
//                if (isNotLast) {
//                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
//                    GLES20.glClearColor(0, 0, 0, 0);
//                }
//
//                if (i == 0) {
//                    filter.onDrawFrame(previousTexture, cubeBuffer, textureBuffer);
//                } else if (i == size - 1) {
//                    filter.onDrawFrame(previousTexture, mGLCubeBuffer, (size % 2 == 0) ? mGLTextureFlipBuffer : mGLTextureBuffer);
//                } else {
//                    filter.onDrawFrame(previousTexture, mGLCubeBuffer, mGLTextureBuffer);
//                }
//
//                if (isNotLast) {
//                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//                    previousTexture = mFrameBufferTextures[i];
//                }
//            }

            //Log.e("testtest44","----------------------gpuimage filter grp start");
            for (int i = 0; i < size; i++) {
                //Log.e("testtest44","----------------------" + i + " " + mIntputWidth + " " + mIntputHeight);
                GTVImageFilter filter = mMergedFilters.get(i);

                boolean isNotLast = i < size - 1;


                //GLES20.glViewport(0, 0, mIntputWidth, mIntputHeight);
                //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                    GLES20.glClearColor(0, 0, 0, 0);
                }

                filter.onDrawFrame(previousTexture);
                //previousTexture = mFrameBufferTextures[i];

                //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    previousTexture = mFrameBufferTextures[i];
                }
                //Log.e("testtest44","----------------------" + i);
            }

            //Log.e("testtest44","----------------------gpuimage filter grp end");
        }
        return previousTexture;
    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    public List<GTVImageFilter> getFilters() {
        return mFilters;
    }

    public List<GTVImageFilter> getMergedFilters() {
        return mMergedFilters;
    }

    public void updateMergedFilters() {
        if (mFilters == null) {
            return;
        }

        if (mMergedFilters == null) {
            mMergedFilters = new ArrayList<GTVImageFilter>();
        } else {
            mMergedFilters.clear();
        }

        List<GTVImageFilter> filters;
        for (GTVImageFilter filter : mFilters) {
            if (filter instanceof GTVImageFilterGroup) {
                ((GTVImageFilterGroup) filter).updateMergedFilters();
                filters = ((GTVImageFilterGroup) filter).getMergedFilters();
                if (filters == null || filters.isEmpty())
                    continue;
                mMergedFilters.addAll(filters);
                continue;
            }
            mMergedFilters.add(filter);
        }
    }
}
