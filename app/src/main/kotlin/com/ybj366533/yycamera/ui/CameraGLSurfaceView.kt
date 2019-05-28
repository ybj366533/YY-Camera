package com.ybj366533.yycamera.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.ybj366533.videolib.utils.LogUtils
import com.ybj366533.gtvimage.gtvfilter.filter.advanced.GTVGroupFilter
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVBaseGroupFilter
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVOESInputFilter

import java.util.ArrayList

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Created by gtv on 2018/6/18.
 */

class CameraGLSurfaceView @JvmOverloads constructor(internal var mContext: Context, attrs: AttributeSet? = null) : GLSurfaceView(mContext, attrs), GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private var surfaceTexture: SurfaceTexture? = null

    var currentContext: EGLContext? = null
    private var mTextureID = -1
    private lateinit var mDirectDrawer: DirectDrawer

    private var grpFilter: GTVGroupFilter? = null
    private var oesInputFilter: GTVOESInputFilter? = null
    private var previewFilter: GTVImageFilter? = null

    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0
    private val videoWidth: Int
    private val videoHeigt: Int
    private var onTextureListener: OnTextureListener? = null

    private var mCamera: Camera? = null

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        videoWidth = 720//1280;
        videoHeigt = 1280//720;
    }


    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

        mTextureID = createTextureID()

        surfaceTexture = SurfaceTexture(mTextureID)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        mDirectDrawer = DirectDrawer(mTextureID)
        //CameraInterface.getInstance().doOpenCamera(null)

        oesInputFilter = GTVOESInputFilter()
        val list = ArrayList<GTVImageFilter>()
        list.add(oesInputFilter!!)
        //list.add(gtvBeautyFilter);
        //list.add(gtvDummyFilter);
        grpFilter = GTVGroupFilter(list)
        grpFilter!!.init()
        grpFilter!!.onInputSizeChanged(videoWidth, videoHeigt)
        grpFilter!!.onDisplaySizeChanged(videoWidth, videoHeigt)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
        openCamera()   //open + start preview
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (surfaceTexture == null) {
            return
        }

        if (currentContext == null) {
            currentContext = EGL14.eglGetCurrentContext()
        }
        surfaceTexture!!.updateTexImage()
        val timestampNanos = surfaceTexture!!.timestamp
        //        float[] mtx = new float[16];
        //        surfaceTexture.getTransformMatrix(mtx);
        //        mDirectDrawer.draw(mtx);

        for (i in 0 until grpFilter!!.filterCount) {

            val filter = grpFilter!!.getFilter(i)
            if (filter is GTVOESInputFilter) {
                val mtx = FloatArray(16)
                //                SurfaceTexture st = (SurfaceTexture)surfaceTexture.getTexture();
                surfaceTexture!!.getTransformMatrix(mtx)
                (filter as GTVOESInputFilter).setTextureTransformMatrix(mtx)
            }
        }

        //gLCubeBuffer.position(0);
        //gLTextureBuffer.position(0);
        //gLFlipTextureBuffer.position(0);

        if (previewFilter == null) {

            run {
                val filters = ArrayList<GTVImageFilter>()
                filters.add(GTVImageFilter())
                previewFilter = GTVBaseGroupFilter(filters)
                if (previewFilter != null)
                    previewFilter!!.init()
            }

            previewFilter!!.onInputSizeChanged(videoWidth, videoHeigt)
            previewFilter!!.onDisplaySizeChanged(surfaceWidth, surfaceHeight)
        }

        val lastTextureId = grpFilter!!.onDrawFrame(mTextureID)
        //previewFilter.onDrawFrame(lastTextureId, gLCubeBuffer, gLTextureBuffer);
        previewFilter!!.onDrawFrame(lastTextureId)
        if (onTextureListener != null) {
            onTextureListener!!.onTextureAvailable(lastTextureId, videoWidth, videoHeigt, timestampNanos)
        }
    }

    private fun createTextureID(): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        return texture[0]
    }


    interface OnTextureListener {
        fun onTextureAvailable(textureId: Int, textureWidth: Int, textureHeight: Int, timestampNanos: Long): Int
    }

    fun setTexutreListener(listener: OnTextureListener?) {
        onTextureListener = listener
    }

    private fun openCamera() {
        try {
            mCamera = cameraInstance
            mCamera!!.setPreviewTexture(surfaceTexture)
            mCamera!!.startPreview()
        } catch (e: Exception) {

        }

    }

    companion object {

        // attempt to get a Camera instance
        // FOCUS_MODE_AUTO,FOCUS_MODE_CONTINUOUS_PICTURE
        // Camera is not available (in use or does not exist)
        // returns null if camera is unavailable
        val cameraInstance: Camera?
            get() {
                var c: Camera? = null
                try {
                    c = Camera.open()
                    val parameters = c!!.parameters
                    parameters.setPreviewSize(1280, 720)
                    if (parameters.supportedFocusModes.contains(
                                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    }
                    c.parameters = parameters
                    c.setDisplayOrientation(90)
                    val size = c.parameters.previewSize
                    LogUtils.LOGI("CCCCCCCCC", " " + size.width + " " + size.height)
                } catch (e: Exception) {
                }

                return c
            }
    }

}
