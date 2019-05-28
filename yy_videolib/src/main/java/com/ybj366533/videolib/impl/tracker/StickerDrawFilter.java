/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ybj366533.videolib.impl.tracker;

import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.util.Log;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;

public class StickerDrawFilter extends GTVImageFilter {

    private String newStickerFolder = null;
    private StickerTextureLoader loader = null;

    public StickerDrawFilter(EGLContext sharedContext) {
        super(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        loader = new StickerTextureLoader(sharedContext);
    }

    protected void onInit() {
        super.onInit();
        loader.startLoad();
    }

    protected void onDestroy() {
        super.onDestroy();
        //EffectTracker.getInstance().clearTexture();
        loader.stopLoad();
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
    }

    public void setStickerFolder(String f) {

        if( f != null && f.length() > 0 ) {
            EffectTracker.getInstance().startPlaySticker(f);
            newStickerFolder = f;
        }
        else {
            EffectTracker.getInstance().stopPlaySticker();
        }

        // TODO:clearTexture();没有调用，必须在gles上下文调用
        loader.loadSticker(f);

        return;
    }

    public int onDrawFrame(final int textureId) {

        int error;

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

//        if( newStickerFolder != null ) {
//            // 防止多个贴纸切换的时候，内存一直增长
//            EffectTracker.getInstance().clearTexture();
//            newStickerFolder = null;
//        }

        if( loader.isStickerReady(newStickerFolder) == true ) {

            GLES20.glEnable(GL_BLEND);
            GLES20.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            EffectTracker.getInstance().udpateCanvasSize(this.getIntputWidth(), this.getIntputHeight());

//        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
            EffectTracker.getInstance().drawWithUniform(mGLUniformTexture, mGLAttribPosition, mGLAttribTextureCoordinate);
        }

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": glError " + error);
        }

        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);

        return OpenGlUtils.ON_DRAWN;
    }
}
