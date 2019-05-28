package com.ybj366533.videolib.core;

import android.opengl.EGLContext;
import android.opengl.GLES20;

import com.ybj366533.videolib.impl.tracker.AnimationDrawFilter;
import com.ybj366533.videolib.impl.tracker.EffectTracker;
import com.ybj366533.videolib.stream.VideoRecordStreamer;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


public class CameraVideoProcessorST extends VideoObject {

    private static final String TAG = "CamOB";

    //protected GTVImageFilter gtvDummyFilter;

    private AnimationDrawFilter animationDrawFilter;

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
    public CameraVideoProcessorST() {

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
        EffectTracker.getInstance().clearTexture();

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
            animationDrawFilter = new AnimationDrawFilter(eglContext);

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

        final VideoObject self = this;

        try {


            pullTextureFromSurface(textureId, flip);

            //generateTexture();

            int origin = getLastTextureId();
            //int mirror = getMirrorTextureId();

            for( int i=0; i<oList.size(); i++ ) {

                IVideoTextureOutput o = oList.get(i);
                if( o instanceof VideoRecordStreamer) {
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
