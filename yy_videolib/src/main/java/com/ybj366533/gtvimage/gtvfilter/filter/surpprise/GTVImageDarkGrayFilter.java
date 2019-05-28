package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.content.Context;
import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gtv on 2018/4/27.
 */

public class GTVImageDarkGrayFilter extends GTVImageFilter {

    private GTVGroupFilter grpFilter;
    private GTVImageHeZeFilter hezeFilter;
    private GTVImageGrayscaleFilter grayFilter;
    private GTVImageContrastFilter contrastFilter;

    public GTVImageDarkGrayFilter(Context context) {

        super(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);

        grayFilter = new GTVImageGrayscaleFilter();
        hezeFilter = new GTVImageHeZeFilter();
        contrastFilter = new GTVImageContrastFilter();

        List<GTVImageFilter> list = new ArrayList<GTVImageFilter>();
        list.add(grayFilter);
        list.add(contrastFilter);
        list.add(hezeFilter);

        grpFilter = new GTVGroupFilter(list);
    }

    public void onInit() {

        super.onInit();

        grpFilter.init();

        contrastFilter.setContrast(1.15f);
    }

    public void onDestroy() {

        grpFilter.destroy();
    }

    public void onInputSizeChanged(final int width, final int height) {

        mIntputWidth = width;
        mIntputHeight = height;

        grpFilter.onInputSizeChanged(width, height);
        grpFilter.onDisplaySizeChanged(width, height);
    }

    public int onDrawFrame(final int inputTextureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {

        IntBuffer fboId = IntBuffer.allocate(1);
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, fboId);
        int textureId = grpFilter.onDrawFrame(inputTextureId);

        int oldFramebufferId = fboId.get(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, oldFramebufferId);

        GLES20.glUseProgram(mGLProgId);

        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return OpenGlUtils.NOT_INIT;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return OpenGlUtils.ON_DRAWN;
    }

    public int onDrawFrame(final int inputTextureId) {

        IntBuffer fboId = IntBuffer.allocate(1);
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, fboId);
        int textureId = grpFilter.onDrawFrame(inputTextureId);

        int oldFramebufferId = fboId.get(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, oldFramebufferId);

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized)
            return OpenGlUtils.NOT_INIT;

        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return OpenGlUtils.ON_DRAWN;
    }
}

