package com.ybj366533.videolib.impl.tracker;

import android.graphics.Bitmap;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.util.Log;

import com.ybj366533.videolib.utils.YYStickerMusicPlayer;
import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gtv on 2018/2/5.
 */

public class FaceTrackFilter extends GTVImageFilter {

    private static final String TAG = "GTVREC";

    protected int[] trackFrameBuffers = null;
    protected int[] trackFrameBufferTextures = null;
    private int trackWidth = -1;
    private int trackHeight = -1;

    protected int[] processFrameBuffers = null;
    protected int[] processFrameBufferTextures = null;
    private int processWidth = -1;
    private int processHeight = -1;

    protected int mIntputWidth;
    protected int mIntputHeight;

    private FaceDetector mDetector = new FaceDetector();
//    AFT_FSDKVersion version = null;
//    AFT_FSDKEngine engine = null;
//    List<AFT_FSDKFace> result = null;

//    private Rect mFaceRect = new Rect();
//    private int mFaceDegree = 0;
//    private boolean mFaceFlag = false;

    private FaceDrawFilter debugFilter;
    private DrawYuvFilter yuvFilter;
    private GTVImageFilter scaleFilter;

    private int mCacheIndex = 0;
    private GTVGroupFilter[] cacheFilters = new GTVGroupFilter[DrawYuvFilter.PBO_MAX];
    private GTVGroupFilter grpFilter;
    private int[] eye_pos = new int[4];
    private EyeBeautyFilter bigEyeFilter;

//    private int[] thin_face_pos = new int[8];
//    private FaceSlimFilter thinFaceFilter;

    private int[] small_face_pos = new int[4];
    private SmallFaceFilter smallFaceFilter;

    private StickerDrawFilter stickerDrawFilter;

    public Bitmap outputBmp = null;


    public int eyeBeautyIntensity = 0;//90;
    public int faceBeautyIntensity = 0;//90;

    private String _currStickerFolder = null;
    String _currStickerMusic = null;

    boolean pauseFlag = false;      // 音乐播放

    public void setStickerFolder(String sf) {

        if(_currStickerFolder!= null && _currStickerFolder.equals(sf)){
            return;
        }

        _currStickerFolder = sf;

        stickerDrawFilter.setStickerFolder(sf);

        try
        {
            String p = sf + "/music.mp3";
            File f=new File(p);
            if(!f.exists()) {
                _currStickerMusic = null;
                YYStickerMusicPlayer.getInstance().close();
            }
            else {
                _currStickerMusic = p;
                YYStickerMusicPlayer.getInstance().open(p);
            }
        }
        catch (Exception e)
        {
        }
    }

    public FaceTrackFilter(EGLContext sharedContext) {

//        engine = new AFT_FSDKEngine();
//        result = new ArrayList<>();
//        version = new AFT_FSDKVersion();
//
//        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine("FSFyi5sEbCLxoCpEkdK6mX9DcQsxpCsCMPYWz92cceLK", "FVczoczqc2WxkbShzcXB1MYpWLg9UuRpnsivEnPZundt", AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
//        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
//        err = engine.AFT_FSDK_GetVersion(version);
//        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        debugFilter = new FaceDrawFilter();
        yuvFilter = new DrawYuvFilter();
        scaleFilter = new GTVImageFilter();
        bigEyeFilter = new EyeBeautyFilter();
//        thinFaceFilter = new FaceSlimFilter();
        smallFaceFilter = new SmallFaceFilter();
        stickerDrawFilter = new StickerDrawFilter(sharedContext);

        List<GTVImageFilter> l = new ArrayList<GTVImageFilter>();
        l.add(bigEyeFilter);
        l.add(smallFaceFilter);
        l.add(stickerDrawFilter);
        grpFilter = new GTVGroupFilter(l);

        List<GTVImageFilter> a = new ArrayList<GTVImageFilter>();
        a.add(new GTVImageFilter());
        a.add(new GTVImageFilter());
        for(int i = 0; i< DrawYuvFilter.PBO_MAX; i++ ) {
            cacheFilters[i] = new GTVGroupFilter(a);
        }

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);

        return;
    }

    protected void onInitialized() {

    }

    protected void onDestroy() {

        YYStickerMusicPlayer.getInstance().close();

        debugFilter.destroy();
        yuvFilter.destroy();
        scaleFilter.destroy();
        grpFilter.destroy();
        if(stickerDrawFilter!=null) {
            stickerDrawFilter.onDestroy();
            stickerDrawFilter = null;
        }
        for(int i = 0; i< DrawYuvFilter.PBO_MAX; i++ ) {
            cacheFilters[i].destroy();
        }
        destroyFramebuffers();

        mDetector.destroy();
    }

    protected void onInit() {

        debugFilter.init();
        yuvFilter.init();
        scaleFilter.init();
//        scaleFilter.vflip();//debug

        grpFilter.init();
        for(int i = 0; i< DrawYuvFilter.PBO_MAX; i++ ) {
            cacheFilters[i].init();
        }
    }

    public void onInputSizeChanged(final int width, final int height) {

        mIntputWidth = width;
        mIntputHeight = height;

        int size = 1;

        int error;
        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": onInputSizeChanged glError 1111 " + error);
        }

        processWidth = width;
        processHeight = height;

        trackWidth = width/2; // TODO:是否需要偶数？4的倍数？？
        trackHeight = height/2;

        debugFilter.onInputSizeChanged(width, height);
        debugFilter.onDisplaySizeChanged(width, height);

        grpFilter.onInputSizeChanged(processWidth, processHeight);
        grpFilter.onDisplaySizeChanged(processWidth, processHeight);

        for(int i = 0; i< DrawYuvFilter.PBO_MAX; i++ ) {
            cacheFilters[i].onInputSizeChanged(processWidth, processHeight);
            cacheFilters[i].onDisplaySizeChanged(processWidth, processHeight);
        }

        yuvFilter.onInputSizeChanged(trackWidth, trackHeight);
        yuvFilter.onDisplaySizeChanged(trackWidth, trackHeight);

        scaleFilter.onInputSizeChanged(trackWidth, trackHeight);
        scaleFilter.onDisplaySizeChanged(trackWidth, trackHeight);

        if(processFrameBuffers != null && (processWidth != width || processHeight != height || processFrameBuffers.length != size)){
            destroyFramebuffers();
        }
        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": onInputSizeChanged glError 1111 " + error);
        }
        if (processFrameBuffers == null) {

            processFrameBuffers = new int[size];
            processFrameBufferTextures = new int[size];

            for (int i = 0; i < size; i++) {

                GLES20.glGenFramebuffers(1, processFrameBuffers, i);

                GLES20.glGenTextures(1, processFrameBufferTextures, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, processFrameBufferTextures[i]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, processWidth, processHeight, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, processFrameBuffers[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, processFrameBufferTextures[i], 0);

                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
        // 一个用于缩小，一个用于提起YUV
        size = 2;
        if( trackFrameBuffers == null ) {

            trackFrameBuffers = new int[size];
            trackFrameBufferTextures = new int[size];

            for (int i = 0; i < size; i++) {

                GLES20.glGenFramebuffers(1, trackFrameBuffers, i);

                GLES20.glGenTextures(1, trackFrameBufferTextures, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, trackFrameBufferTextures[i]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, trackWidth, trackHeight, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, trackFrameBuffers[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, trackFrameBufferTextures[i], 0);

                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }

        this.outputBmp = Bitmap.createBitmap(trackWidth, trackHeight, Bitmap.Config.ARGB_8888);
    }

    protected long lastFaceDectectTimestamp = 0;
    protected byte[] smallYuvData = null;
    protected float[] outputFaceData = null;
    protected int onTrackFace(final int input_textureId) {

        // 先画到缓存里面
        int textureId = this.cacheFilters[this.mCacheIndex].onDrawFrame(input_textureId);
        this.mCacheIndex = (this.mCacheIndex + 1) % DrawYuvFilter.PBO_MAX;

        GLES20.glViewport(0, 0, trackWidth, trackHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, trackFrameBuffers[0]);

        float[] imageVertices = mDetector.imageVetexInfoBeforeDetect(trackWidth, trackHeight);
//        Log.e(TAG, "imageVertices " + imageVertices[0] +","+ imageVertices[1] +","+ imageVertices[2] +","+ imageVertices[3]
//                +","+ imageVertices[4] +","+ imageVertices[5] +","+ imageVertices[6] +","+ imageVertices[7]);

//        scaleFilter.onDrawFrame(textureId);
        mGLCubeBuffer.clear();
        mGLCubeBuffer.position(0);
        mGLCubeBuffer.put(imageVertices).position(0);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        scaleFilter.onDrawFrame(textureId, mGLCubeBuffer, mGLTextureBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glViewport(0, 0, trackWidth, trackHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, trackFrameBuffers[1]);

        float[] rotationInfo = mDetector.rotationInfoBeforeDetect();
        yuvFilter.prepare(textureId, rotationInfo);
        textureId = yuvFilter.onDrawFrame(trackFrameBufferTextures[0], rotationInfo);
//        Log.e(TAG, "yuvFilter rotation info " + rotationInfo[0] +","+ rotationInfo[1] +","+ rotationInfo[2]);
        if( textureId < 0 ) {
            Log.e(TAG, "yuvFilter textureId < 0 ");
            return -1;
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        if( smallYuvData == null ) {
            smallYuvData = new byte[trackWidth*trackHeight*3/2];
            outputFaceData = new float[136];
        }
        yuvFilter.getOutput(smallYuvData, 0, smallYuvData.length);

        mDetector.detect(smallYuvData, trackWidth, trackHeight, rotationInfo[0], rotationInfo[1], rotationInfo[2]);
        if( mDetector.isTrackingFace() ) {

            mDetector.getEyePos(eye_pos);
            mDetector.getSmallFacePos(small_face_pos);
            mDetector.getAllFacePos(outputFaceData);

            if( _currStickerMusic != null && pauseFlag == false) {
                YYStickerMusicPlayer.getInstance().play();
            }
        }
        else {

            if( _currStickerMusic != null ) {
                YYStickerMusicPlayer.getInstance().pause();
            }
        }
        /***
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(smallYuvData, trackWidth, trackHeight, AFT_FSDKEngine.CP_PAF_NV21, result);
        if( err.getCode() != AFT_FSDKError.MOK ) {
            Log.e(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        }
        if( result.size() > 0 ) {
            for (AFT_FSDKFace face : result) {
                this.mFaceRect = face.getRect();
                this.mFaceDegree = face.getDegree();
                Log.e(TAG, "AFT_FSDK_FaceFeatureDetect =" + this.mFaceDegree);
            }

            this.mFaceFlag = true;
            EffectTracker.getInstance().updateFaceRect(this.mFaceRect.left, this.mFaceRect.top, this.mFaceRect.right-this.mFaceRect.left, this.mFaceRect.bottom-this.mFaceRect.top);
            int res = EffectTracker.getInstance().trackImageJNI(smallYuvData, trackWidth*trackHeight, trackWidth, trackHeight, outputFaceData);
            if (res < 0) {
                // sdmTracker失败，目前主要原因是model还没load
                this.mFaceFlag = false;
            }

            EffectTracker.getInstance().getBigEyePositionJNI(eye_pos);
            EffectTracker.getInstance().getSmallFacePositionJNI(small_face_pos);
//            EffectTracker.getInstance().getThinFacePositionJNI(thin_face_pos);

            if( _currStickerMusic != null && pauseFlag == false) {
                YYStickerMusicPlayer.getInstance().play();
            }
        }
        else {
            this.mFaceFlag = false;

            if( _currStickerMusic != null ) {
                YYStickerMusicPlayer.getInstance().pause();
            }
        }
        result.clear();
         **/

        return textureId;
    }

    //@Override
    public int onDrawFrame(final int input_textureId) {

        int textureId = input_textureId;

        if (trackFrameBuffers == null || trackFrameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }
        if (processFrameBuffers == null || processFrameBufferTextures == null) {
            return OpenGlUtils.NOT_INIT;
        }
        textureId = onTrackFace(input_textureId);
        if( textureId < 0 ) {
            return -1;
        }

        int lastTextureId = textureId;
        if( /*this.mFaceFlag*/this.mDetector.isTrackingFace() ) {

//            debugFilter.updateFaceRect(this.mFaceRect);
            debugFilter.updateFacePointList(this.outputFaceData);
//            debugFilter.updateFaceThinList(this.thin_face_pos);

            float[] a = {eye_pos[0]*1.0f/trackWidth, eye_pos[1]*1.0f/trackHeight};
            float[] b = {eye_pos[2]*1.0f/trackWidth, eye_pos[3]*1.0f/trackHeight};
            bigEyeFilter.setEyePosition(a, b);
            bigEyeFilter.setIntensity(eyeBeautyIntensity*1.0f/100.0f/3.20f);// TODO:根据设定来

//            thinFaceFilter.setIntensity(faceBeautyIntensity * 1.0f / 100.0f/2.0f);
//            float[] c = {thin_face_pos[0]*1.0f/trackWidth, thin_face_pos[1]*1.0f/trackHeight};
//            float[] d = {thin_face_pos[2]*1.0f/trackWidth, thin_face_pos[3]*1.0f/trackHeight};
//            thinFaceFilter.setLeftFacePosition(c, d);
//            float[] e = {thin_face_pos[4]*1.0f/trackWidth, thin_face_pos[5]*1.0f/trackHeight};
//            float[] f = {thin_face_pos[6]*1.0f/trackWidth, thin_face_pos[7]*1.0f/trackHeight};
//            thinFaceFilter.setRightFacePosition(e, f);

            smallFaceFilter.setIntensity(faceBeautyIntensity * 1.0f / 100.0f/2.0f);
            float[] c = {small_face_pos[0]*1.0f/trackWidth, small_face_pos[1]*1.0f/trackHeight};
            float[] d = {small_face_pos[2]*1.0f/trackWidth, small_face_pos[3]*1.0f/trackHeight};
            smallFaceFilter.setNoseAndChinPosition(c, d);

            lastTextureId = grpFilter.onDrawFrame(textureId);
        }

        GLES20.glViewport(0, 0, processWidth, processHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, processFrameBuffers[0]);

        debugFilter.onDrawFrame(lastTextureId);

//        GLES20.glViewport(200, 400, 180, 240);
//        debugFilter.onDrawFrame(trackFrameBufferTextures[0]);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return processFrameBufferTextures[0];
    }

    private void destroyFramebuffers() {

        if (processFrameBufferTextures != null) {
            GLES20.glDeleteTextures(processFrameBufferTextures.length, processFrameBufferTextures, 0);
            processFrameBufferTextures = null;
        }
        if (trackFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(trackFrameBuffers.length, trackFrameBuffers, 0);
            trackFrameBuffers = null;
        }

        if (trackFrameBufferTextures != null) {
            GLES20.glDeleteTextures(trackFrameBufferTextures.length, trackFrameBufferTextures, 0);
            trackFrameBufferTextures = null;
        }
        if (trackFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(trackFrameBuffers.length, trackFrameBuffers, 0);
            trackFrameBuffers = null;
        }
    }

    public void setEyeBeautyIntensity(int eyeBeautyIntensity) {
        this.eyeBeautyIntensity = eyeBeautyIntensity;
    }

    public void setFaceBeautyIntensity(int faceBeautyIntensity) {
        this.faceBeautyIntensity = faceBeautyIntensity;
    }

    public void onPause(){
        pauseFlag = true;
        if( _currStickerMusic != null ) {
            YYStickerMusicPlayer.getInstance().pause();
        }
    }

    public void onResume(){
        pauseFlag = false;
    }
}
