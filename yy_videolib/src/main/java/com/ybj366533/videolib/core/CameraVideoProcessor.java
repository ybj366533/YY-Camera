package com.ybj366533.videolib.core;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.ybj366533.videolib.impl.texture.BaseTexture;
import com.ybj366533.videolib.impl.texture.SurfaceTexture;
import com.ybj366533.videolib.impl.tracker.AnimationDrawFilter;
import com.ybj366533.videolib.impl.tracker.EffectTracker;
import com.ybj366533.videolib.impl.tracker.FaceTrackFilter;
import com.ybj366533.videolib.impl.utils.YYFileUtils;
import com.ybj366533.videolib.recorder.IVideoRecorder;
import com.ybj366533.videolib.stream.VideoRecordStreamer;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.videolib.widget.RecorderViewRender;
import com.ybj366533.videolib.impl.camera.CameraEngine;
import com.ybj366533.videolib.impl.camera.utils.CameraInfo;
import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVBeautyBFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVOESInputFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.instagram.FilterHelper;
import com.ybj366533.gtvimage.gtvfilter.filter.lookupfilter.GTVRiXiRenXiangFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


public class CameraVideoProcessor extends VideoObject implements BaseTexture.TextureListener, Camera.PreviewCallback {

    private static final String TAG = "CamOB";

    private final int LOGO_NUM = 2;

    private SurfaceTexture surfaceTexture;

    protected GTVOESInputFilter oesInputFilter;
    protected GTVBeautyBFilter gtvBeautyFilter;
    protected GTVImageFilter gtvDummyFilter;
    protected FaceTrackFilter gtvStickerFilter;
    private AnimationDrawFilter animationDrawFilter;

    protected GTVGroupFilter grpFilter;
    protected GTVGroupFilter grpFilter2;
    protected GTVGroupFilter magicGrpFilter;
    protected GTVGroupFilter animationgrpFilter;
    protected int lastTextureId = -1;
    protected int mirrorTextureId = -1;

    private long lastTextureTimestamp = 0;

    protected FloatBuffer gLCubeBuffer;
    protected FloatBuffer gLTextureBuffer;
    protected FloatBuffer gLTextureBufferCam;
    protected FloatBuffer gLFlipTextureBuffer;


    protected GTVGroupFilter copyFilter;

    RecorderViewRender gtvRecorderViewRender;

    String stickerFolder = null;
    String animationFolder = null;

    private int myCameraId = 0;

    public void setStickerFolder(String sf) {
        stickerFolder = sf;

        //gtvStickerFilter.setStickerFolder(sf);
        if (gtvStickerFilter != null) {
            gtvStickerFilter.setStickerFolder(sf);
        }
    }


    public void startAnimationGroup(List<String> l) {

        if (l.size() > 0) {

            animationFolder = l.get(0);

            if (animationDrawFilter != null) {
                animationDrawFilter.setStickerFolderGroup(l);
            }
        }
    }

    public void startAnimationGroupKeepLast(List<String> l) {

        if (l.size() > 0) {

            animationFolder = l.get(0);

            if (animationDrawFilter != null) {
                animationDrawFilter.setStickerFolderGroupKeepLast(l);
            }
        }
    }

    public void stopAnimationGroup() {
        animationFolder = null;

        if (animationDrawFilter != null) {
            animationDrawFilter.setStickerFolderGroup(null);
        }
    }

    public CameraVideoProcessor(GLSurfaceView glSurfaceView, RecorderViewRender gtvRecorderViewRender, int cameraId) {

        super(glSurfaceView);
        OpenGlUtils.setGlVer(glSurfaceView.getContext());
        this.gtvRecorderViewRender = gtvRecorderViewRender;

        setInited(false);

        this.logoTextures = new int[LOGO_NUM];
        this.logoWidth = new int[LOGO_NUM];
        this.logoHeight = new int[LOGO_NUM];
        this.logoRect = new DispRect[LOGO_NUM];
        for (int i = 0; i < LOGO_NUM; ++i) {
            this.logoTextures[i] = -1;
            this.logoWidth[i] = 0;
            this.logoHeight[i] = 0;
            this.logoRect[i] = null;
        }


        final GLSurfaceView view = glSurfaceView;
        Thread b = new Thread(new Runnable() {
            @Override
            public void run() {
                YYFileUtils.copyFileIfNeed(view.getContext(), "gtvft.bin");
                String path = YYFileUtils.getFilePath(view.getContext(), "gtvft.bin");
                EffectTracker.getInstance().loadModel(path);
            }
        });
        b.start();

        // 不管咋样，先clear，防止context被销毁
        EffectTracker.getInstance().clearTexture();

        // 每次重启activity
        myCameraId = cameraId;
        //CameraEngine.cameraID = 0;
    }

    private int cameraOpenFailLog = 0;

    @Override
    public void init() {
        LogUtils.LOGI(TAG, "init");

        //super.init(st);
        BaseTexture st = new SurfaceTexture();
        st.init();

        this.surfaceTexture = (SurfaceTexture) st;
        this.surfaceTexture.addListener(this);

        st.addListener(this);

        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);

        gLTextureBufferCam = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBufferCam.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, false)).position(0);

        gLFlipTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLFlipTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, true, true)).position(0);

        try {

            // get codec size, rotate to fit camera
            if (CameraEngine.getCamera() == null) {
                CameraEngine.cameraID = myCameraId;
                boolean ret = CameraEngine.openCamera((Activity) this.glSurfaceView.getContext());
                if (ret == false && cameraOpenFailLog < 1) {
                    if (gtvRecorderViewRender != null) {
                        gtvRecorderViewRender.onCameraOpenFailed();
                    }
                }
            }

            if (CameraEngine.getCamera() == null) {
                if (cameraOpenFailLog < 3) {
                    LogUtils.LOGE(TAG, "camera open failed");
                }
                cameraOpenFailLog++;

                return;
            }

            CameraEngine.startPreview((android.graphics.SurfaceTexture) st.getTexture(), this);

            CameraInfo info = CameraEngine.getCameraInfo();
            if (info.orientation == 90 || info.orientation == 270) {
                imageWidth = info.previewHeight;
                imageHeight = info.previewWidth;
            } else {
                imageWidth = info.previewWidth;
                imageHeight = info.previewHeight;
            }

        } catch (Exception ex) {

            cameraOpenFailLog++;

            LogUtils.LOGE(TAG, "camera open exception " + ex.getMessage());

            return;
        }

        oesInputFilter = new GTVOESInputFilter();
        gtvBeautyFilter = new GTVBeautyBFilter(2);
        gtvDummyFilter = new GTVImageFilter();

        List<GTVImageFilter> list = new ArrayList<GTVImageFilter>();
        list.add(oesInputFilter);
        list.add(gtvBeautyFilter);
        list.add(gtvDummyFilter);

        grpFilter = new GTVGroupFilter(list);
        grpFilter.init();
        grpFilter.onInputSizeChanged(imageWidth, imageHeight);
        grpFilter.onDisplaySizeChanged(imageWidth, imageHeight);

        grpFilter.removeFilter(gtvDummyFilter);

        // 用于mirror
        List<GTVImageFilter> list2 = new ArrayList<GTVImageFilter>();
        list2.add(gtvDummyFilter);
        grpFilter2 = new GTVGroupFilter(list2);
        grpFilter2.init();
        grpFilter2.onInputSizeChanged(imageWidth, imageHeight);
        grpFilter2.onDisplaySizeChanged(imageWidth, imageHeight);


        List<GTVImageFilter> list4 = new ArrayList<GTVImageFilter>();
        list4.add(gtvDummyFilter);
        copyFilter = new GTVGroupFilter(list4);
        copyFilter.init();
        copyFilter.onInputSizeChanged(imageWidth, imageHeight);
        copyFilter.onDisplaySizeChanged(imageWidth, imageHeight);
        //贴纸初始化
        gtvStickerFilter = new FaceTrackFilter(this.gtvRecorderViewRender.getEGLContext());
        gtvStickerFilter.init();
        gtvStickerFilter.onInputSizeChanged(imageWidth, imageHeight);
        gtvStickerFilter.setEyeBeautyIntensity(0);
        gtvStickerFilter.setFaceBeautyIntensity(0);

        animationDrawFilter = new AnimationDrawFilter(this.gtvRecorderViewRender.getEGLContext());
//        animationDrawFilter.init();
//        animationDrawFilter.onInputSizeChanged(imageWidth, imageHeight);
//        animationDrawFilter.onDisplaySizeChanged(imageWidth, imageHeight);

        List<GTVImageFilter> list3 = new ArrayList<GTVImageFilter>();
        list3.add(animationDrawFilter);
        animationgrpFilter = new GTVGroupFilter(list3);
        animationgrpFilter.init();
        animationgrpFilter.onInputSizeChanged(imageWidth, imageHeight);
        animationgrpFilter.onDisplaySizeChanged(imageWidth, imageHeight);

        // init beauty params
        for (int i = 0; i < configedBeautyIntensity.length; i++) {
            configedBeautyIntensity[i] = 0.0f;
        }

        setInited(true);

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

    private int pullTextureFromSurface() {

        if (this.surfaceTexture == null) {
            return -1;
        }

        int oesId = this.surfaceTexture.updateTexture();
        if (oesId < 0) {
            return lastTextureId;
        }

        lastTextureTimestamp = this.surfaceTexture.getTimestamp();

//        if( configedBeautyIndex >= 0 ) {
//            gtvBeautyFilter.setBeautyParam(configedBeautyIndex, configedBeautyPercent);
//            configedBeautyIndex = -1;
//        }

        boolean needbeauty = false;
        for (int i = 0; i < 4 && mBeautyLevel == 1; i++) {
            if (configedBeautyIntensity[i] >= 0.05f) {
                needbeauty = true;
                break;
            }
        }

        if (needbeauty == true) {
            gtvBeautyFilter.setBeautyParam(0, configedBeautyIntensity[0]);//磨皮
            gtvBeautyFilter.setBeautyParam(1, configedBeautyIntensity[1]);
            gtvBeautyFilter.setBeautyParam(2, configedBeautyIntensity[2]);//美白
            gtvBeautyFilter.setBeautyParam(3, configedBeautyIntensity[3]);
        }

        if (needbeauty == true && grpFilter.haveFilter(this.gtvBeautyFilter) == false) {
            grpFilter.removeFilter(this.gtvDummyFilter);
            grpFilter.removeFilter(this.gtvBeautyFilter);
            grpFilter.addFilter(this.gtvBeautyFilter);
        } else if (needbeauty == false && grpFilter.haveFilter(this.gtvBeautyFilter) == true) {
            grpFilter.removeFilter(this.gtvBeautyFilter);
            grpFilter.removeFilter(this.gtvDummyFilter);
            grpFilter.addFilter(this.gtvDummyFilter);
        }

        for (int i = 0; i < grpFilter.getFilterCount(); i++) {

            GTVImageFilter filter = grpFilter.getFilter(i);
            if (filter instanceof GTVOESInputFilter) {
                float[] mtx = new float[16];
                android.graphics.SurfaceTexture st = (android.graphics.SurfaceTexture) surfaceTexture.getTexture();
                st.getTransformMatrix(mtx);
                ((GTVOESInputFilter) filter).setTextureTransformMatrix(mtx);
            }
        }

        gLCubeBuffer.position(0);
        gLTextureBuffer.position(0);
        gLFlipTextureBuffer.position(0);

        lastTextureId = grpFilter.onDrawFrame(oesId, gLCubeBuffer, gLTextureBufferCam);

        // 如果没有滤镜时，实际也有一个滤镜
        if (magicGrpFilter == null) {
            setMagicFilterType(0);
        }

        if (magicGrpFilter != null) {
            GTVImageFilter lvjing = magicGrpFilter.getFilter(0);
            if (lvjing instanceof GTVRiXiRenXiangFilter) {
                ((GTVRiXiRenXiangFilter) lvjing).setIntensity(mSkinWhiten);
            }
            // 特殊处理  gLTextureBuffer --》gLTextureBufferCam
            lastTextureId = magicGrpFilter.onDrawFrame(lastTextureId, gLCubeBuffer, gLTextureBufferCam);
        }

        if (thinFaceValue > 0 || bigEyeValue > 0 || stickerFolder != null || OpenGlUtils.supportGL3()) {
            Log.e("gtvStickerFilter:", "thinFaceValue = " + thinFaceValue + "   ##bigEyeValue=" + bigEyeValue);
            gtvStickerFilter.setFaceBeautyIntensity(thinFaceValue);
            gtvStickerFilter.setEyeBeautyIntensity(bigEyeValue);
            int lid = gtvStickerFilter.onDrawFrame(lastTextureId);
            if (lid >= 0) {
                lastTextureId = lid;
            }
        }

        if (animationgrpFilter != null && animationFolder != null) {
            lastTextureId = animationgrpFilter.onDrawFrame(lastTextureId);
        }

        if (onTextureListener != null) {
            lastTextureId = onTextureListener.onTextureAvailable(lastTextureId, imageWidth, imageHeight);
        }


        // 绘制logo覆盖在最上面
        lastTextureId = grpFilter2.onDrawFrame(lastTextureId);
        for (int m = 0; m < LOGO_NUM; ++m) {
            if (this.logoRect[m] != null && this.logoTextures != null && this.logoTextures[m] >= 0 && this.logoHeight[m] > 0 && this.logoWidth[m] > 0) {

                int texId = this.logoTextures[m];

                OpenGlUtils.fitCube(this.logoWidth[m], this.logoHeight[m], this.logoRect[m].width, this.logoRect[m].height, gLCubeBuffer);

                gLCubeBuffer.position(0);
                gLTextureBuffer.position(0);

                DispRect r = this.logoRect[m];


                lastTextureId = grpFilter2.onDrawFrameInRectWithAlpha(texId, gLCubeBuffer, gLTextureBuffer, r.x, imageHeight - (r.y + r.height), r.width, r.height, true);

            }
        }


        if (this.mGrabBmpCB != null) {

            try {

                final Bitmap bmp = OpenGlUtils.textureToBitmapByFilter(lastTextureId, gtvDummyFilter, imageWidth, imageHeight);

                if (this.glSurfaceView != null) {

                    this.glSurfaceView.post(new Runnable() {
                        @Override
                        public void run() {
                            handleGrabbedBitmap(bmp);
                        }
                    });
                }
            } catch (Exception ex) {
                LogUtils.LOGE(TAG, ex.getMessage());
            }
        }

        return lastTextureId;
    }


    public void destroy() {
        LogUtils.LOGI(TAG, "destroy");

        super.destroy();

        //this.pauseCamera();

        if (this.grpFilter != null) {
            this.grpFilter.onDestroy();
            this.grpFilter = null;
        }
        if (this.grpFilter2 != null) {
            this.grpFilter2.onDestroy();
            this.grpFilter2 = null;
        }

        if (this.copyFilter != null) {
            this.copyFilter.onDestroy();
            this.copyFilter = null;
        }

        if (this.magicGrpFilter != null) {
            this.magicGrpFilter.onDestroy();
            this.magicGrpFilter = null;
        }

        if (this.gtvStickerFilter != null) {
            this.gtvStickerFilter.destroy();
            this.gtvStickerFilter = null;
        }

        if (this.animationDrawFilter != null) {
            this.animationDrawFilter.destroy();
            this.animationDrawFilter = null;
        }

        if (this.animationgrpFilter != null) {
            this.animationgrpFilter.onDestroy();
            this.animationgrpFilter = null;
        }

        if (surfaceTexture != null) {
            this.surfaceTexture.destroy();
        }
    }

    public void pauseVideo() {
        LogUtils.LOGI(TAG, "pauseVideo");

        super.pauseVideo();
        if (gtvStickerFilter != null) {
            gtvStickerFilter.onPause(); // 停止播放音乐贴纸（如果有）
        }
        if (glSurfaceView != null) {
            this.glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {

                    pauseCamera();
                }
            });
        }

    }

    public void resumeVideo() {
        LogUtils.LOGI(TAG, "resumeVideo");

        super.resumeVideo();

        if (gtvStickerFilter != null) {
            gtvStickerFilter.onResume(); // 停止播放音乐贴纸（如果有）
        }

        if (glSurfaceView != null) {
            this.glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {

                    resumeCamera();
                }
            });
        }


        //this.resumeCamera();
    }

    private void resumeCamera() {

        //if(CameraEngine.getCamera() == null) {
        CameraEngine.cameraID = myCameraId;
        boolean ret = CameraEngine.openCamera((Activity) this.glSurfaceView.getContext());
        if (ret == false) {
            if (gtvRecorderViewRender != null) {
                gtvRecorderViewRender.onCameraOpenFailed();
            }
        }
        //}
        if (this.surfaceTexture != null) {
            CameraEngine.startPreview((android.graphics.SurfaceTexture) this.surfaceTexture.getTexture(), this);
        }
    }

    private void pauseCamera() {
        if (glSurfaceView != null) {
            CameraEngine.releaseCamera((Activity) CameraVideoProcessor.this.glSurfaceView.getContext());
        }

    }

    public Boolean isBeautyOn() {

        if (mBeautyLevel > 0) {
            return true;
        }

        return false;
    }

    private int mBeautyLevel = 1;
    private float configedBeautyIntensity[] = new float[4];
    private int configedBeautyIndex = -1;
    private float configedBeautyPercent = 0.0f;

    public float[] getBeautyParams() {
        if (gtvBeautyFilter != null) {
            return gtvBeautyFilter.getBeautyParam();
        } else {
            return new float[]{0.8f, 0.55f, 0.5f, 0.15f};
        }
    }

    private float mSkinWhiten = 0.1f;

    //  美颜参数设置
    public void setBeautyParams(int index, float percent) {
        configedBeautyIndex = index;
        configedBeautyPercent = percent;
        configedBeautyIntensity[index] = percent;
        // 美白滤镜也跟着调
        if (index == 2) {
            mSkinWhiten = percent;
        }
    }

    public void setBeautySkinSmooth(float percent) {
        setBeautyParams(0, percent);
    }

    public void setBeautySkinWhite(float percent) {
        setBeautyParams(2, percent);
    }

    public float[] resetBeautyParams() {
        if (gtvBeautyFilter != null) {
            return gtvBeautyFilter.resetBeautyParams();
        }
        return new float[]{0.8f, 0.55f, 0.5f, 0.15f};
    }

    public float[] switchBeauty(boolean flag) {
        LogUtils.LOGI(TAG, "CameraView:switchBeauty" + flag);

        if (flag == true) {
            mBeautyLevel = 1;
        } else {
            mBeautyLevel = 0;
        }

        if (gtvBeautyFilter != null) {
            return gtvBeautyFilter.getBeautyParam();
        }

        return new float[]{0.8f, 0.55f, 0.5f, 0.15f};
    }

    public boolean supportLightOn() {

        return CameraEngine.supportLightOn();
    }

    public boolean isLightOn() {

        return CameraEngine.isLightOn();
    }

    public void setLightStatus(boolean flag) {

        if (flag) {
            CameraEngine.turnLightOn();
        } else {
            CameraEngine.turnLightOff();
        }
    }

    public int currentCameraId() {

        return CameraEngine.cameraID;
    }

    public void switchCamera(int cameraId) {
        LogUtils.LOGI(TAG, "switch camera " + cameraId);

        if (cameraId == CameraEngine.cameraID) {
            LogUtils.LOGE(TAG, "same cameraId passed !");
            return;
        }
        if (cameraId >= CameraEngine.getCameraNumbers()) {
            LogUtils.LOGE(TAG, "invalid cameraId passed !");
            return;
        }

        // 放到GL线程去执行
        final int nCamId = cameraId;
        if (this.glSurfaceView != null) {
            this.glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {

                    CameraEngine.releaseCamera((Activity) CameraVideoProcessor.this.glSurfaceView.getContext());
                    CameraEngine.cameraID = nCamId;
                    myCameraId = CameraEngine.cameraID;

                    resumeCamera();
                }
            });
        }

    }

    // 画面放大功能
    // 检查是否支持画面放大功能
    public boolean isZoomSupported() {

        return CameraEngine.isZoomSupported();
    }

    // 取得画面放大比率（0：没放大，100：放大到最大）
    public int getZoom() {
        return CameraEngine.getZoom();
    }

    //设定画面放大比率（0：没放大，100：放大到最大）
    public void setZoom(int zoom) {
        CameraEngine.setZoom(zoom);
    }

    @Override
    public void onTextureReady(int type, BaseTexture obj) {

        // 受到通知后尽快updateTexture，否则最小化操作的时候，会fail
        final VideoObject self = this;
        //this.glSurfaceView.queueEvent(new Runnable() {
        this.gtvRecorderViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {

                try {

                    pullTextureFromSurface();

                    generateTexture();

                    int origin = getLastTextureId();
                    int mirror = getMirrorTextureId();

                    if (android.os.Build.MODEL.equalsIgnoreCase("OPPO A33")) {
                        GLES20.glFinish();
                    }

                    for (int i = 0; i < oList.size(); i++) {

                        IVideoTextureOutput o = oList.get(i);
                        if (o instanceof VideoRecordStreamer) {
                            if (mirror >= 0)
                                o.newGLTextureAvailable(self, mirror, imageWidth, imageHeight, lastTextureTimestamp);
                        } else {
                            if (origin >= 0)
                                o.newGLTextureAvailable(self, origin, imageWidth, imageHeight, lastTextureTimestamp);
                        }
                    }
                } catch (Exception ex) {

                    ex.printStackTrace();
                }
            }
        });
    }


    private int magicFilterType = 0;

    /**
     * 添加滤镜
     *
     * @param filterType
     */
    public void setMagicFilterType(final int filterType) {


        final Context context = this.glSurfaceView.getContext();
        if ((filterType != magicFilterType || magicGrpFilter == null) && (glSurfaceView != null)) {
            this.glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {

                    if (magicGrpFilter != null) {
                        magicGrpFilter.destroy();
                        magicGrpFilter = null;
                    }

                    //if (filterType < 10) {
                    List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                    filters.add(FilterHelper.getFilter(context, filterType));
                    magicGrpFilter = new GTVGroupFilter(filters);
                    if (magicGrpFilter != null)
                        magicGrpFilter.init();
                    //}


                    if (magicGrpFilter != null) {
                        magicGrpFilter.onInputSizeChanged(imageWidth, imageHeight);
                        magicGrpFilter.onDisplaySizeChanged(imageWidth, imageHeight);
                    }

                    magicFilterType = filterType;
                }


            });
        }
    }

    protected int[] logoTextures;
    protected int[] logoWidth;
    protected int[] logoHeight;
    protected DispRect[] logoRect;

    // logoIndex < 2
    public void loadLogoToRect(int logoIndex, Bitmap bmp, DispRect r) {

        LogUtils.LOGI(TAG, "loadLogoToRect");

        this.logoRect[logoIndex] = r;

        try {

            if (this.logoTextures[logoIndex] >= 0) {
                GLES20.glDeleteTextures(1, logoTextures, logoIndex);
                this.logoTextures[logoIndex] = -1;
            }

            if (r.width == 0 && r.height == 0) {
                this.logoHeight[logoIndex] = this.logoWidth[logoIndex] = 0;
                this.logoRect[logoIndex] = null;
                return;
            }

            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int bcnt = bmp.getByteCount();

            this.logoWidth[logoIndex] = width;
            this.logoHeight[logoIndex] = height;

            int colorFormat = GLES20.GL_RGBA;
            if (bcnt == width * height * 4) {
                colorFormat = GLES20.GL_RGBA;
            } else if (bcnt == width * height * 3) {
                colorFormat = GLES20.GL_RGB;
            } else {
                colorFormat = GLES20.GL_RGB565;
            }

            ByteBuffer byteData = ByteBuffer.allocate(bcnt);
            bmp.copyPixelsToBuffer(byteData);
            byteData.position(0);

            GLES20.glGenTextures(1, logoTextures, logoIndex);

            int textureId = logoTextures[logoIndex];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, colorFormat, width, height, 0,
                    colorFormat, GLES20.GL_UNSIGNED_BYTE, byteData);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        } catch (Exception ex) {

            ex.printStackTrace();

            this.logoHeight[logoIndex] = this.logoWidth[logoIndex] = 0;
            this.logoRect[logoIndex] = null;
            this.logoTextures[logoIndex] = -1;
        }

        return;
    }

    private int bigEyeValue = 0;
    private int thinFaceValue = 0;

    public void setBigEye(int value) {
        LogUtils.LOGW(TAG, "setBigEye is called " + value);
        bigEyeValue = value / 2;//value/2;
    }

    public int getBigEye() {
        return bigEyeValue;
    }

    public void setThinFace(int value) {
        LogUtils.LOGW(TAG, "setThinFace is called " + value);
        thinFaceValue = value / 2;
    }

    public int getThinFace() {
        return thinFaceValue;
    }

    IVideoRecorder.OnTextureListener onTextureListener;

    public void setTexutreListener(IVideoRecorder.OnTextureListener listener) {
        this.onTextureListener = listener;
    }

    IVideoRecorder.OnYUVDataListener onYUVDataListener;

    public void setYUVDataListener(IVideoRecorder.OnYUVDataListener listener) {
        this.onYUVDataListener = listener;
    }

    private IVideoRecorder.ITakePictureCallback mGrabBmpCB = null;

    public void takePicture(IVideoRecorder.ITakePictureCallback cb) {

        mGrabBmpCB = cb;
    }

    protected void handleGrabbedBitmap(Bitmap bmp) {

        if (this.mGrabBmpCB != null) {

            this.mGrabBmpCB.onBitmapReady(bmp);
            this.mGrabBmpCB = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        if (onYUVDataListener != null) {
            onYUVDataListener.onYUVDataAvailable(bytes, this.imageHeight, this.imageWidth);
        }

        //lastestPreviewFrameData = bytes;
    }
}
