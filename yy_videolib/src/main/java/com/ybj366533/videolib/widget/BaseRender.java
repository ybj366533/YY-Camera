package com.ybj366533.videolib.widget;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;
import com.ybj366533.gtvimage.gtvfilter.utils.Rotation;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by why8222 on 2016/2/25.
 */
//public abstract class BaseRender extends GLSurfaceView implements GLSurfaceView.Renderer{
public abstract class BaseRender implements GLSurfaceView.Renderer{
    /**
     * 所选择的滤镜，类型为MagicBaseGroupFilter
     * 1.mCameraInputFilter将SurfaceTexture中YUV数据绘制到FrameBuffer
     * 2.filter将FrameBuffer中的纹理绘制到屏幕中
     */
    protected GTVImageFilter filter;

    /**
     * SurfaceTexure纹理id
     */
    protected int textureId = OpenGlUtils.NO_TEXTURE;

    /**
     * 顶点坐标
     */
    protected final FloatBuffer gLCubeBuffer;

    /**
     * 纹理坐标
     */
    protected final FloatBuffer gLTextureBuffer;
    protected final FloatBuffer gLTextureFlipBuffer;        //前置摄像头录画数据反转用

    /**
     * GLSurfaceView的宽高
     */
    protected int surfaceWidth, surfaceHeight;

    /**
     * 图像宽高
     */
    protected int imageWidth, imageHeight;

    protected GLSurfaceView glSurfaceView;

    //public BaseRender(Context context) {
    //    this(context, null);
    //}

    //public BaseRender(Context context, AttributeSet attrs) {
    public BaseRender(GLSurfaceView glSurfaceView) {
        //super(context, attrs);
        this.glSurfaceView = glSurfaceView;
        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        gLTextureFlipBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureFlipBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, true, true)).position(0);

        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0,0, 0, 0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        MagicFilterParam.initMagicFilterParam(gl);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width, height);
        surfaceWidth = width;
        surfaceHeight = height;
        onFilterChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    protected void onFilterChanged(){
        if(filter != null) {
            filter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
            filter.onInputSizeChanged(imageWidth, imageHeight);
        }
    }

    public void onResume(){

    }

    public void onPause(){

    }

    public void onDestroy(){

    }

//    public void setFilter(final MagicFilterType type){
//        glSurfaceView.queueEvent(new Runnable() {
//            @Override
//            public void run() {
//                if (filter != null)
//                    filter.destroy();
//                filter = null;
//                filter = MagicFilterFactory.initFilters(type);
//                if (filter != null)
//                    filter.init();
//                onFilterChanged();
//            }
//        });
//        glSurfaceView.requestRender();
//    }

    protected void deleteTextures() {
        if(textureId != OpenGlUtils.NO_TEXTURE && glSurfaceView != null){
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[]{
                            textureId
                    }, 0);
                    textureId = OpenGlUtils.NO_TEXTURE;
                }
            });
        }
    }

//    public void savePicture(SavePictureTask savePictureTask) {
//
//    }
}
