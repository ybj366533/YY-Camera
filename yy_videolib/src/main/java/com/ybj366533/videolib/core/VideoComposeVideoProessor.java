package com.ybj366533.videolib.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import com.ybj366533.videolib.editor.IVideoEditor;
import com.ybj366533.videolib.impl.texture.BaseTexture;
import com.ybj366533.videolib.impl.texture.SurfaceTexture;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVOESInputFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImage3x3TextureSamplingFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageBasicdeformFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageBlueorangeFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageChromaAberrationFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageCrosshatchFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageEMInterferenceFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageFalseColorFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageGrayscaleFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageMoneyFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVImageSharpenFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVShakeFilter;
import com.ybj366533.gtvimage.gtvfilter.filter.surpprise.GTVSoulOutFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by gtv on 2017/1/21.
 */

// 合成阶段的GTVPlayerVideoProcessor，修改需要同步

public class VideoComposeVideoProessor {

    private static final String TAG = "ComposeVideoProessor";

    private final int LOGO_NUM = 2;


    protected GTVOESInputFilter oesInputFilter;
    protected GTVImageFilter gtvDummyFilter;

    protected GTVGroupFilter grpFilter;
    protected GTVGroupFilter grpFilter2;
    protected GTVGroupFilter magicGrpFilter;
    protected int lastTextureId = -1;

    protected GTVGroupFilter copyFilter;



    protected FloatBuffer gLCubeBuffer;
    protected FloatBuffer gLTextureBuffer;


    protected boolean videoIsRendered;

    protected int imageWidth = 0;
    protected int imageHeight = 0;



    private int duration;
    public void setDuration(int duration) {
        this.duration = duration;
    }


    //public VideoComposeVideoProessor(GLSurfaceView glSurfaceView, EditorViewRender gtvEditorViewRender) {
    public VideoComposeVideoProessor(int imageWidth, int imageHeight) {


        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        // logo相关
        this.logoTextures = new int[LOGO_NUM];
        this.logoWidth = new int[LOGO_NUM];
        this.logoHeight = new int[LOGO_NUM];
        this.logoRect =new DispRect[LOGO_NUM];
        for(int i = 0; i < LOGO_NUM; ++i) {
            this.logoTextures[i] = -1;
            this.logoWidth[i] = 0;
            this.logoHeight[i] = 0;
            this.logoRect[i] = null;
        }

        // 动画合成
        this.animationTextures = new int[1];
        this.animationTextures[0] = -1;
        this.animationWidth = 0;
        this.animationHeight = 0;
        this.animationRect = null;
    }

    public void init() {

        LogUtils.LOGI(TAG, "init");

        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);

        oesInputFilter = new GTVOESInputFilter();
        gtvDummyFilter = new GTVImageFilter();

        List<GTVImageFilter> list = new ArrayList<GTVImageFilter>();
        list.add(oesInputFilter);

        grpFilter = new GTVGroupFilter(list);
        grpFilter.init();
        grpFilter.onInputSizeChanged(imageWidth, imageHeight);
        grpFilter.onDisplaySizeChanged(imageWidth, imageHeight);

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


        this.videoIsRendered = false;


        //setInited(true);

        return;
    }

    public int getLastTextureId() {

        if( this.videoIsRendered == false ) {
            return -1;
        }

        return this.lastTextureId;
    }

    protected int generateTexture() {

        return this.lastTextureId;
    }

    private int pullTextureFromSurface(SurfaceTexture surfaceTexture) {

        if( surfaceTexture == null ) {
            return -1;
        }

        int oesId = surfaceTexture.updateTexture();
        if( oesId < 0 ) {
            return lastTextureId;
        }

        for( int i=0; i<grpFilter.getFilterCount(); i++ ) {

            GTVImageFilter filter = grpFilter.getFilter(i);
            if( filter instanceof GTVOESInputFilter) {
                float[] mtx = new float[16];
                android.graphics.SurfaceTexture st = (android.graphics.SurfaceTexture)surfaceTexture.getTexture();
                st.getTransformMatrix(mtx);
                ((GTVOESInputFilter)filter).setTextureTransformMatrix(mtx);
            }
        }

        lastTextureId = grpFilter.onDrawFrame(oesId);

        if (magicGrpFilter != null) {
            lastTextureId = magicGrpFilter.onDrawFrame(lastTextureId);
        }


        lastTextureId = grpFilter2.onDrawFrame(lastTextureId);

        if(this.animationFolder != null && duration>=0) {
            int index = this.duration/animImageInterval;
            if(index != animationIndex) {
                loadanimationToRect(index);
            }

            if( this.animationRect != null && this.animationTextures != null && this.animationTextures[0] >= 0 && this.animationHeight > 0 && this.animationWidth > 0 ) {

                int texId = this.animationTextures[0];

                OpenGlUtils.fitCube(this.animationWidth, this.animationHeight, this.animationRect.width, this.animationRect.height, gLCubeBuffer);

                gLCubeBuffer.position(0);
                gLTextureBuffer.position(0);

                DispRect r1 = this.animationRect;


                //lastTextureId = grpFilter2.onDrawFrame(lastTextureId);
                lastTextureId = grpFilter2.onDrawFrameInRectWithAlpha(texId, gLCubeBuffer, gLTextureBuffer, r1.x, imageHeight-(r1.y+r1.height), r1.width, r1.height, true);

            }
        }


        // 只有合成是才需要水印
        {
            // 绘制logo覆盖在最上面
            //lastTextureId = grpFilter2.onDrawFrame(lastTextureId);
            for(int m = 0; m < LOGO_NUM; ++m) {
                if( this.logoRect[m] != null && this.logoTextures != null && this.logoTextures[m] >= 0 && this.logoHeight[m] > 0 && this.logoWidth[m] > 0 ) {

                    int texId = this.logoTextures[m];

                    OpenGlUtils.fitCube(this.logoWidth[m], this.logoHeight[m], this.logoRect[m].width, this.logoRect[m].height, gLCubeBuffer);

                    gLCubeBuffer.position(0);
                    gLTextureBuffer.position(0);

                    DispRect r = this.logoRect[m];


                    lastTextureId = grpFilter2.onDrawFrameInRectWithAlpha(texId, gLCubeBuffer, gLTextureBuffer, r.x, imageHeight-(r.y+r.height), r.width, r.height, true);

                }
            }
        }

        // 为了不闪动

        lastTextureId = copyFilter.onDrawFrame(lastTextureId);

        return lastTextureId;
    }


    public void destroy() {

        //super.destroy();


        if( this.grpFilter != null ) {
            this.grpFilter.onDestroy();
            this.grpFilter = null;
        }

        if( this.grpFilter2 != null ) {
            this.grpFilter2.onDestroy();
            this.grpFilter2 = null;
        }

        if( this.magicGrpFilter != null ) {
            this.magicGrpFilter.onDestroy();
            this.magicGrpFilter = null;
        }

        if(this.copyFilter != null) {
            this.copyFilter.onDestroy();;
            this.copyFilter = null;
        }

    }





    //@Override
    public void videoProcess(int type, BaseTexture obj) {


        this.videoIsRendered = true;

        final SurfaceTexture gtvSurfaceTexture = (SurfaceTexture)obj;

                try {

                    pullTextureFromSurface(gtvSurfaceTexture);


                }
                catch(Exception ex) {

                    ex.printStackTrace();
                }
    }




    private IVideoEditor.EffectType magicFilterType = IVideoEditor.EffectType.EFFECT_NO;

    public IVideoEditor.EffectType getMagicFilterType() {
        return magicFilterType;
    }

    public void setMagicFilterType(IVideoEditor.EffectType type) {


        final IVideoEditor.EffectType filterType = type;

        if (filterType != magicFilterType) {

                    if (magicGrpFilter != null) {
                        magicGrpFilter.destroy();
                        magicGrpFilter = null;
                    }

                    if (filterType == IVideoEditor.EffectType.EFFECT_HEBAI) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        // 边缘检测，两个filter组合使用
                        filters.add(new GTVImageGrayscaleFilter());
                        filters.add(new GTVImage3x3TextureSamplingFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                        //Log.e("testtest77", " init" );
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_DOUDONG) {
                        // 抖动
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVShakeFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_LINGHUNCHUQIAO) {
                        // 抖动
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVSoulOutFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);

                    } else if (filterType == IVideoEditor.EffectType.EFFECT_YISHIJIE) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageFalseColorFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);

                    } else if (filterType == IVideoEditor.EffectType.EFFECT_JIANRUI) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageSharpenFilter(20));
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    }
                    // new filter
                    else if (filterType == IVideoEditor.EffectType.EFFECT_BODONG) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageBasicdeformFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_TOUSHI) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageBlueorangeFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_SHANGSHUO) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageChromaAberrationFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_SUMIAO) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageCrosshatchFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_LINGYI) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageEMInterferenceFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    } else if (filterType == IVideoEditor.EffectType.EFFECT_YINXIANPAI) {
                        List<GTVImageFilter> filters = new ArrayList<GTVImageFilter>();
                        filters.add(new GTVImageMoneyFilter());
                        //filters.add(new GTVImageFilter());
                        magicGrpFilter = new GTVGroupFilter(filters);
                    }


                    if (magicGrpFilter != null) {
                        magicGrpFilter.init();
                        magicGrpFilter.onInputSizeChanged(imageWidth, imageHeight);
                        magicGrpFilter.onDisplaySizeChanged(imageWidth, imageHeight);
                    }

                    magicFilterType =filterType;


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

            if( this.logoTextures[logoIndex] >= 0 ) {
                GLES20.glDeleteTextures(1, logoTextures, logoIndex);
                this.logoTextures[logoIndex] = -1;
            }

            if( r.width == 0 && r.height == 0 ) {
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
            if( bcnt == width*height*4 ) {
                colorFormat = GLES20.GL_RGBA;
            }
            else if( bcnt == width*height*3 ) {
                colorFormat = GLES20.GL_RGB;
            }
            else {
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
        }
        catch(Exception ex) {

            ex.printStackTrace();

            this.logoHeight[logoIndex] = this.logoWidth[logoIndex] = 0;
            this.logoRect[logoIndex] = null;
            this.logoTextures[logoIndex] = -1;
        }

        return;
    }

    protected int[] animationTextures;
    protected int animationWidth;
    protected int animationHeight;
    protected DispRect animationRect;
    public void loadanimationToRect(int index) {

        DecimalFormat df=new DecimalFormat("000");
        String fileName="out_"+df.format(index)+".png";
        if( animationPrefix != null && animationPrefix.length() > 0 ) {
            fileName=animationPrefix +"_"+df.format(index)+".png";
        }

        String path = animationFolder + "/" + fileName;

        //LogUtils.LOGI(TAG, "loadLogoToRect ");

        Bitmap bmp = BitmapFactory.decodeFile(path);
        if(bmp == null) {
            this.animationHeight = this.animationWidth = 0;
            //this.animationRect = null;
            this.animationTextures[0] = -1;
            return;
        }

        //this.animationRect = r;

        try {

            if( this.animationTextures[0] >= 0 ) {
                GLES20.glDeleteTextures(1, animationTextures, 0);
                this.animationTextures[0] = -1;
            }

            if( this.animationRect.width == 0 && this.animationRect.height == 0 ) {
                this.animationHeight = this.animationWidth = 0;
                this.animationRect = null;
                return;
            }

            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int bcnt = bmp.getByteCount();

            this.animationWidth = width;
            this.animationHeight = height;

            int colorFormat = GLES20.GL_RGBA;
            if( bcnt == width*height*4 ) {
                colorFormat = GLES20.GL_RGBA;
            }
            else if( bcnt == width*height*3 ) {
                colorFormat = GLES20.GL_RGB;
            }
            else {
                colorFormat = GLES20.GL_RGB565;
            }

            ByteBuffer byteData = ByteBuffer.allocate(bcnt);
            bmp.copyPixelsToBuffer(byteData);
            byteData.position(0);

            GLES20.glGenTextures(1, animationTextures, 0);

            int textureId = animationTextures[0];
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

            animationIndex = index;
        }
        catch(Exception ex) {

            ex.printStackTrace();

            this.animationHeight = this.animationWidth = 0;
            //this.animationRect = null;
            this.animationTextures[0] = -1;
        }

        return;
    }

    private String animationPrefix = null;
    private String animationFolder = null;
    private int animImageInterval = 60;
    private int animationIndex = -1;

    public void setAnimation(String prefix, String animationFolder, int animImageInterval, DispRect dispRect){
        this.animationPrefix = prefix;
        this.animationFolder = animationFolder;
        this.animImageInterval = animImageInterval;
        animationIndex = -1;
        this.animationRect = dispRect;
    }

}
