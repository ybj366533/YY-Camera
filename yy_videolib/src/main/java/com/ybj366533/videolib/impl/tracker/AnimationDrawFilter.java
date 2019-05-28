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

import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;

public class AnimationDrawFilter extends GTVImageFilter {

    public static final int MAX_STICKER_CNT = 5;

//    private String newStickerFolder = null;
//    private EffectTracker gtvEffectTracker;

    private List<String> stickerList;
    private List<EffectTracker> trackerList;
    private List<AnimationTextureLoader> loadersList;
    private EGLContext mGLContext;

    public AnimationDrawFilter(EGLContext sharedContext) {
        super(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        mGLContext = sharedContext;
    }

    protected void onInit() {
        super.onInit();
//        gtvEffectTracker = new EffectTracker();
        trackerList = new ArrayList<>();
        stickerList = new ArrayList<>();
        loadersList = new ArrayList<>();
        for( int i=0; i<MAX_STICKER_CNT; i++ ) {
            EffectTracker tracker = new EffectTracker();
            trackerList.add(tracker);
            AnimationTextureLoader l = new AnimationTextureLoader(mGLContext, tracker);
            loadersList.add(l);
            l.startLoad();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        //EffectTracker.getInstance().clearTexture();
        for( int i=0; i<MAX_STICKER_CNT; i++ ) {
            EffectTracker tracker = trackerList.get(i);
            tracker.clearTexture();
            tracker.destroy();
            AnimationTextureLoader l = loadersList.get(i);
            l.stopLoad();
        }
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
    }

    public void setStickerFolderGroupKeepLast(List<String> g) {

        stickerList.clear();

        for( int i=0; i<trackerList.size(); i++ ) {
            EffectTracker t = trackerList.get(i);
            t.stopPlaySticker();
        }

        if( g != null && g.size() > 0 ) {
            for( int i=0; i<MAX_STICKER_CNT && i<g.size(); i++ ) {
                stickerList.add(g.get(i));
            }
        }

        // 直接播放新动画
        for( int i=0; i<stickerList.size(); i++ ) {
            EffectTracker t = trackerList.get(i);
            t.startPlaySticker(stickerList.get(i), -9999);
            AnimationTextureLoader l = loadersList.get(i);
            l.loadSticker(g.get(i));
        }

        return;
    }

    public void setStickerFolderGroup(List<String> g) {

        stickerList.clear();

        for( int i=0; i<trackerList.size(); i++ ) {
            EffectTracker t = trackerList.get(i);
            t.stopPlaySticker();
        }

        if( g != null && g.size() > 0 ) {
            for( int i=0; i<MAX_STICKER_CNT && i<g.size(); i++ ) {
                stickerList.add(g.get(i));
            }
        }

        // 直接播放新动画
        for( int i=0; i<stickerList.size(); i++ ) {
            EffectTracker t = trackerList.get(i);
            t.startPlaySticker(stickerList.get(i), 0);
            AnimationTextureLoader l = loadersList.get(i);
            l.loadSticker(g.get(i));
        }

        return;
    }

    public void setStickerFolder(String f) {

        if( f != null && f.length() > 0 ) {
            //EffectTracker.getInstance().startPlaySticker(f);
//            gtvEffectTracker.startPlaySticker(f,0);
//            newStickerFolder = f;
        }
        else {
            //EffectTracker.getInstance().stopPlaySticker();
//            gtvEffectTracker.stopPlaySticker();
        }

        // TODO:clearTexture();没有调用，必须在gles上下文调用

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
        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": glError " + error);
        }
/*
        if( stickerList.size() > 0 ) {

            for( int i=0; i<stickerList.size() && i<trackerList.size(); i++ ) {

                EffectTracker t = trackerList.get(i);
                String s = stickerList.get(i);

                if( s.equalsIgnoreCase(t.getLastStickerFolderName()) == false ) {
                    Log.e("GTVANIM", "t.clearTexture -- is called ");
                    t.clearTexture();
                }
                if( s.equalsIgnoreCase(t.getCurrentStickerFolderName()) == false ) {
                    Log.e("GTVANIM", "t.clearTexture -- is called ");
                    t.startPlaySticker(s, 0);
                }
            }
        }
*/
//        if( newStickerFolder != null ) {
//            // 防止多个贴纸切换的时候，内存一直增长
//            //EffectTracker.getInstance().clearTexture();
//            gtvEffectTracker.clearTexture();
//            newStickerFolder = null;
//        }

        GLES20.glEnable(GL_BLEND);
        GLES20.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // TODO:暂时不做异步
       // EffectTracker.getInstance().prepareTexture();
        //EffectTracker.getInstance().udpateCanvasSize(this.getIntputWidth(), this.getIntputHeight());
        for( int i=0; i<stickerList.size() && i<trackerList.size(); i++ ) {
            EffectTracker t = trackerList.get(i);
//            AnimationTextureLoader loader = loadersList.get(i);
//            if( loader.isStickerReady(stickerList.get(i)) == true ) {
                t.udpateCanvasSize(this.getIntputWidth(), this.getIntputHeight());
                t.drawWithUniform(mGLUniformTexture, mGLAttribPosition, mGLAttribTextureCoordinate);
//            }
//            t.prepareTexture();
//            t.udpateCanvasSize(this.getIntputWidth(), this.getIntputHeight());
//            t.drawWithUniform(mGLUniformTexture, mGLAttribPosition, mGLAttribTextureCoordinate);
        }
//        gtvEffectTracker.prepareTexture();
//        gtvEffectTracker.udpateCanvasSize(this.getIntputWidth(), this.getIntputHeight());

//        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
//        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        //EffectTracker.getInstance().drawWithUniform(mGLUniformTexture, mGLAttribPosition, mGLAttribTextureCoordinate);
//        gtvEffectTracker.drawWithUniform(mGLUniformTexture, mGLAttribPosition, mGLAttribTextureCoordinate);

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
