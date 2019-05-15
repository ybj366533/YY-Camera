package com.gtv.cloud.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.gtv.cloud.impl.camera.CameraEngine;
import com.gtv.cloud.impl.camera.utils.CameraInfo;
import com.gtv.cloud.impl.texture.GTVBaseTexture;
import com.gtv.cloud.impl.texture.GTVSurfaceTexture;
import com.gtv.cloud.impl.tracker.GTVAnimationDrawFilter;
import com.gtv.cloud.impl.tracker.GTVEffectTracker;
import com.gtv.cloud.impl.tracker.GTVFaceTrackFilter;
import com.gtv.cloud.impl.utils.DispRect;
import com.gtv.cloud.impl.utils.GTVFileUtils;
import com.gtv.cloud.recorder.IGTVVideoRecorder;
import com.gtv.cloud.stream.GTVVideoRecordStreamer;
import com.gtv.cloud.utils.LogUtils;
import com.gtv.cloud.widget.GTVRecorderViewRender;
import com.gtv.gtvimage.gtvfilter.filter.advanced.GTVBeautyBFilter;
import com.gtv.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter;
import com.gtv.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.gtv.gtvimage.gtvfilter.filter.base.GTVOESInputFilter;
import com.gtv.gtvimage.gtvfilter.filter.instagram.FilterHelper;
import com.gtv.gtvimage.gtvfilter.filter.lookupfilter.GTVRiXiRenXiangFilter;
import com.gtv.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.gtv.gtvimage.gtvfilter.utils.Rotation;
import com.gtv.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


public class GTVCameraVideoProcessorST extends GTVVideoObject {

    private static final String TAG = "CamOB";

    //protected GTVImageFilter gtvDummyFilter;

    private GTVAnimationDrawFilter animationDrawFilter;

    protected GTVGroupFilter animationgrpFilter;
    protected int lastTextureId = -1;
    protected int mirrorTextureId = -1;

    private long lastTextureTimestamp = 0;

    protected FloatBuffer gLCubeBuffer;
    protected FloatBuffer gLTextureBuffer;
    protected FloatBuffer gLTextureBufferFlip;


    protected GTVGroupFilter copyFilter;
    protected GTVGroupFilter flipFilter;

    String animationFolder =  null;


    EGLContext eglContext;
    public void setEglContext(EGLContext eglContext) {
        this.eglContext = eglContext;
    }


    public void startAnimationGroup(List<String> l) {

        if( l.size() > 0 ) {

            animationFolder = l.get(0);

            if(animationDrawFilter != null) {
                animationDrawFilter.setStickerFolderGroup(l);
            }
        }
    }

    public void startAnimationGroupKeepLast(List<String> l) {

        if( l.size() > 0 ) {

            animationFolder = l.get(0);

            if(animationDrawFilter != null) {
                animationDrawFilter.setStickerFolderGroupKeepLast(l);
            }
        }
    }

    public void stopAnimationGroup() {
        animationFolder = null;

        if(animationDrawFilter != null) {
            animationDrawFilter.setStickerFolderGroup(null);
        }
    }
    public GTVCameraVideoProcessorST() {

        super(null);

        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        gLTextureBufferFlip = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBufferFlip.put(TextureRotationUtil.getRotation(Rotation.NORMAL,false, true)).position(0);

        // 不管咋样，先clear，防止context被销毁
        GTVEffectTracker.getInstance().clearTexture();

    }

    private int cameraOpenFailLog =0;
    @Override
    public void init() {
        LogUtils.LOGI(TAG, "init");

        return;
    }

    public int getLastTextureId() {

        return this.lastTextureId;
    }

    protected int generateTexture() {

        mirrorTextureId = copyFilter.onDrawFrame(lastTextureId);

        return this.lastTextureId;
    }

    private int getMirrorTextureId() {

        return this.mirrorTextureId;
    }

    private int pullTextureFromSurface(int texureId, boolean flip) {

        if(animationgrpFilter == null && eglContext != null && imageWidth !=0) {
            animationDrawFilter = new GTVAnimationDrawFilter(eglContext);

            List<GTVImageFilter> list3 = new ArrayList<GTVImageFilter>();
            list3.add(animationDrawFilter);
            animationgrpFilter = new GTVGroupFilter(list3);
            animationgrpFilter.init();
            animationgrpFilter.onInputSizeChanged(imageWidth, imageHeight);
            animationgrpFilter.onDisplaySizeChanged(imageWidth, imageHeight);
        }

        if(flipFilter == null) {
            List<GTVImageFilter> list = new ArrayList<GTVImageFilter>();
            list.add(new GTVImageFilter());
            flipFilter = new GTVGroupFilter(list);
            flipFilter.init();
            flipFilter.onInputSizeChanged(imageWidth, imageHeight);
            flipFilter.onDisplaySizeChanged(imageWidth, imageHeight);
        }

        if( animationgrpFilter!= null && animationFolder != null) {
            if(flip == true) {
                lastTextureId = flipFilter.onDrawFrame(texureId, gLCubeBuffer, gLTextureBufferFlip);
                lastTextureId = animationgrpFilter.onDrawFrame(lastTextureId);
                lastTextureId = flipFilter.onDrawFrame(lastTextureId, gLCubeBuffer, gLTextureBufferFlip);
            } else {
                lastTextureId = animationgrpFilter.onDrawFrame(texureId);
            }

        } else {
            lastTextureId = texureId;
        }

        return lastTextureId;
    }


    public void destroy() {
        LogUtils.LOGI(TAG, "destroy");

        super.destroy();


        if( this.copyFilter != null ) {
            this.copyFilter.onDestroy();
            this.copyFilter = null;
        }

        if( this.flipFilter != null ) {
            this.flipFilter.onDestroy();
            this.flipFilter = null;
        }


        if( this.animationDrawFilter != null ) {
            this.animationDrawFilter.destroy();
            this.animationDrawFilter = null;
        }

        if( this.animationgrpFilter != null ) {
            this.animationgrpFilter.onDestroy();
            this.animationgrpFilter = null;
        }

    }



    //@Override
    public int videoProcess(int textureId, int textureWidth, int textureHeight, final boolean flip, long timestampNanos) {
        imageWidth = textureWidth;
        imageHeight = textureHeight;

        final GTVVideoObject self = this;

        try {


            pullTextureFromSurface(textureId, flip);

            //generateTexture();

            int origin = getLastTextureId();
            //int mirror = getMirrorTextureId();

            for( int i=0; i<oList.size(); i++ ) {

                IVideoTextureOutput o = oList.get(i);
                if( o instanceof GTVVideoRecordStreamer) {
                    //if( mirror >= 0 )
                    if(copyFilter == null) {
                        List<GTVImageFilter> list = new ArrayList<GTVImageFilter>();
                        list.add(new GTVImageFilter());
                        copyFilter = new GTVGroupFilter(list);
                        copyFilter.init();
                        copyFilter.onInputSizeChanged(imageWidth, imageHeight);
                        copyFilter.onDisplaySizeChanged(imageWidth, imageHeight);
                    }

                    int mirror;
                    if(flip == true) {
                        mirror = copyFilter.onDrawFrame(lastTextureId, gLCubeBuffer, gLTextureBufferFlip);
                    } else {
                       mirror = copyFilter.onDrawFrame(lastTextureId);
                    }

                    if( android.os.Build.MODEL.equalsIgnoreCase("OPPO A33")) {
                        GLES20.glFinish();
                    }

                    o.newGLTextureAvailable(self, mirror, imageWidth, imageHeight, timestampNanos);
                }
                else {
//                            if( origin >= 0 )
//                                o.newGLTextureAvailable(self, origin, imageWidth, imageHeight, timestampNanos);
                }
            }
        }
        catch(Exception ex) {

            ex.printStackTrace();
        }

        return lastTextureId;
    }


}
