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

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class DrawYuvFilter extends GTVImageFilter {

    public static final int EXPORT_TYPE_I420=1;
    public static final int EXPORT_TYPE_YV12=2;
    public static final int EXPORT_TYPE_NV12=3;
    public static final int EXPORT_TYPE_NV21=4;

    private static class ExportShader {

        private static final String HEAD="precision highp float;\n" +
                "precision highp int;\n" +
                "\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "uniform float uWidth;\n" +
                "uniform float uHeight;\n" +
                "\n" +
                "float cY(float x,float y){\n" +
                "    vec4 c=texture2D(inputImageTexture,vec2(x,y));\n" +
                "    return c.r*0.257+c.g*0.504+c.b*0.098+0.0625;\n" +
                "}\n" +
                "\n" +
                "vec4 cC(float x,float y,float dx,float dy){\n" +
                "    vec4 c0=texture2D(inputImageTexture,vec2(x,y));\n" +
                "    vec4 c1=texture2D(inputImageTexture,vec2(x+dx,y));\n" +
                "    vec4 c2=texture2D(inputImageTexture,vec2(x,y+dy));\n" +
                "    vec4 c3=texture2D(inputImageTexture,vec2(x+dx,y+dy));\n" +
                "    return (c0+c1+c2+c3)/4.;\n" +
                "}\n" +
                "\n" +
                "float cU(float x,float y,float dx,float dy){\n" +
                "    vec4 c=cC(x,y,dx,dy);\n" +
                "    return -0.148*c.r - 0.291*c.g + 0.439*c.b+0.5000;\n" +
                "}\n" +
                "\n" +
                "float cV(float x,float y,float dx,float dy){\n" +
                "    vec4 c=cC(x,y,dx,dy);\n" +
                "    return 0.439*c.r - 0.368*c.g - 0.071*c.b+0.5000;\n" +
                "}\n" +
                "\n" +
                "vec2 cPos(float t,float shiftx,float gy){\n" +
                "    vec2 pos=vec2(floor(uWidth*textureCoordinate.x),floor(uHeight*gy));\n" +
                "    return vec2(mod(pos.x*shiftx,uWidth),(pos.y*shiftx+floor(pos.x*shiftx/uWidth))*t);\n" +
                "}\n" +
                "\n" +
                "vec4 calculateY(){\n" +
                "    vec2 pos=cPos(1.,4.,textureCoordinate.y);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]=cY(pos.x/uWidth,textureYPos);\n" +
                "    oColor[1]=cY((pos.x+1.)/uWidth,textureYPos);\n" +
                "    oColor[2]=cY((pos.x+2.)/uWidth,textureYPos);\n" +
                "    oColor[3]=cY((pos.x+3.)/uWidth,textureYPos);\n" +
                "    return oColor;\n" +
                "}\n" +
                "vec4 calculateU(float gy,float dx,float dy){\n" +
                "    vec2 pos=cPos(2.,8.,textureCoordinate.y-gy);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]= cU(pos.x/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[1]= cU((pos.x+2.)/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[2]= cU((pos.x+4.)/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[3]= cU((pos.x+6.)/uWidth,textureYPos,dx,dy);\n" +
                "    return oColor;\n" +
                "}\n" +
                "vec4 calculateV(float gy,float dx,float dy){\n" +
                "    vec2 pos=cPos(2.,8.,textureCoordinate.y-gy);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]=cV(pos.x/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[1]=cV((pos.x+2.)/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[2]=cV((pos.x+4.)/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[3]=cV((pos.x+6.)/uWidth,textureYPos,dx,dy);\n" +
                "    return oColor;\n" +
                "}\n" +
                "vec4 calculateUV(float dx,float dy){\n" +
                "    vec2 pos=cPos(2.,4.,textureCoordinate.y-0.2500);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]= cU(pos.x/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[1]= cV(pos.x/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[2]= cU((pos.x+2.)/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[3]= cV((pos.x+2.)/uWidth,textureYPos,dx,dy);\n" +
                "    return oColor;\n" +
                "}\n" +
                "vec4 calculateVU(float dx,float dy){\n" +
                "    vec2 pos=cPos(2.,4.,textureCoordinate.y-0.2500);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]= cV(pos.x/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[1]= cU(pos.x/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[2]= cV((pos.x+2.)/uWidth,textureYPos,dx,dy);\n" +
                "    oColor[3]= cU((pos.x+2.)/uWidth,textureYPos,dx,dy);\n" +
                "    return oColor;\n" +
                "}\n";

        public static String getFrag(int type) {

            StringBuilder sb=new StringBuilder();
            sb.append(HEAD);
            switch (type) {
                case DrawYuvFilter.EXPORT_TYPE_I420:
                    sb.append("void main() {\n" +
                            "    if(textureCoordinate.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(textureCoordinate.y<0.3125){\n" +
                            "        gl_FragColor=calculateU(0.2500,1./uWidth,1./uHeight);\n" +
                            "    }else if(textureCoordinate.y<0.3750){\n" +
                            "        gl_FragColor=calculateV(0.3125,1./uWidth,1./uHeight);\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
                case DrawYuvFilter.EXPORT_TYPE_YV12:
                    sb.append("void main() {\n" +
                            "    if(textureCoordinate.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(textureCoordinate.y<0.3125){\n" +
                            "        gl_FragColor=calculateV(0.2500,1./uWidth,1./uHeight);\n" +
                            "    }else if(textureCoordinate.y<0.3750){\n" +
                            "        gl_FragColor=calculateU(0.3125,1./uWidth,1./uHeight);\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
                case DrawYuvFilter.EXPORT_TYPE_NV12:
                    sb.append("void main() {\n" +
                            "    if(textureCoordinate.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(textureCoordinate.y<0.3750){\n" +
                            "        gl_FragColor=calculateUV(1./uWidth,1./uHeight);\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
                case DrawYuvFilter.EXPORT_TYPE_NV21:
                default:
                    sb.append("void main() {\n" +
                            "    if(textureCoordinate.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(textureCoordinate.y<0.3750){\n" +
                            "        gl_FragColor=calculateVU(1./uWidth,1./uHeight);\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
            }
            return sb.toString();
        }
    }

    public static final String YUV_EXPORT_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "gl_Position = position;\n" +
            "textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    private int mGLWidth;
    private int mGLHeight;

    public DrawYuvFilter() {

        super(YUV_EXPORT_VERTEX_SHADER, ExportShader.getFrag(DrawYuvFilter.EXPORT_TYPE_NV21));
    }

    @Override
    public void onInit() {
        super.onInit();

        mGLWidth = GLES20.glGetUniformLocation(mGLProgId,"uWidth");
        mGLHeight = GLES20.glGetUniformLocation(mGLProgId,"uHeight");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);

        mPboCenterX = new float[PBO_MAX];
        mPboCenterY = new float[PBO_MAX];
        mPboAngle = new float[PBO_MAX];
        for( int i=0; i<PBO_MAX; i++ ) {
            mPboCenterX[i] = mPboCenterY[i] = mPboAngle[i] = -1.0f;
        }

        mPboTextureIds = new int[PBO_MAX];
        for( int i=0; i<PBO_MAX; i++ ) {
            mPboTextureIds[i] = -1;
        }

        mPboSize = width * height * 3 / 2;

        opengles30 = OpenGlUtils.supportGL3();
        if (opengles30) {
            initPixelBuffer(width, height);
        }

        mTempBuffer= ByteBuffer.allocate(mPboSize);
    }

    public void prepare(int tid, float[] rotationInfo) {
        this.mPboTextureIds[mPboIndex] = tid;
        this.mPboCenterX[mPboIndex] = rotationInfo[0];
        this.mPboCenterY[mPboIndex] = rotationInfo[1];
        this.mPboAngle[mPboIndex] = rotationInfo[2];
//        Log.e("GTVREC", "prepare " + tid + "," + mPboIndex);
    }

    public int onDrawFrame(final int textureId, float[] rotationInfo) {

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

        rotationInfo[0] = rotationInfo[1] = rotationInfo[2] = -1.0f;
        int res = -1;
        if (opengles30) {
            res = bindPixelBuffer(rotationInfo);
            Log.e("GTVREC", "bindPixelBuffer " + res + "," + mPboIndex + " rotation: " + rotationInfo[0] +","+ rotationInfo[1] +","+ rotationInfo[2]);
        } else {
            GLES20.glFinish();
            GLES20.glReadPixels(0, 0, mIntputWidth, mIntputHeight * 3 / 8, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mTempBuffer);
            res = this.mPboTextureIds[mPboIndex];
        }

        return res;
    }

    int error;
    protected void onDrawArraysPre() {

        GLES20.glUniform1f(mGLWidth,this.mIntputWidth);
        GLES20.glUniform1f(mGLHeight,this.mIntputHeight);
    }

    private ByteBuffer mTempBuffer;

    protected void onDrawArraysAfter() {
//        GLES20.glFinish();
//        GLES20.glReadPixels(0,0,mIntputWidth,mIntputHeight*3/8, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,mTempBuffer);
//        long s = System.currentTimeMillis();
//        if (opengles30) {
//            bindPixelBuffer();
//        } else {
//            GLES20.glFinish();
//            GLES20.glReadPixels(0, 0, mIntputWidth, mIntputHeight * 3 / 8, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mTempBuffer);
//        }
//        long e = System.currentTimeMillis();
//        Log.e("MGREC", "trackcost onDrawArraysAfter " + (opengles30?"use30 ":"useold ") + (e-s));

    }

    public void getOutput(byte[] data,int offset,int length){
        if(mTempBuffer!=null){
            mTempBuffer.get(data,offset,length);
            mTempBuffer.clear();
        }
    }

    private IntBuffer mPboIds;
    private int mPboIndex = 0;
    private int mPboNewIndex = 1;
    private int[] mPboTextureIds;
    private float[] mPboCenterX;
    private float[] mPboCenterY;
    private float[] mPboAngle;

    private int mPboSize;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyPixelBuffers();
    }

    private void destroyPixelBuffers() {
        if (mPboIds != null) {
            GLES30.glDeleteBuffers(PBO_MAX, mPboIds);
            mPboIds = null;
        }
    }

    private boolean opengles30 = false;

//    private final int mPixelStride = 4;//RGBA 4字节
//    private int mRowStride;//对齐4字节

    public static int PBO_MAX = 3;
    private void initPixelBuffer(final int width, final int height) {

        if (mPboIds != null && (mIntputWidth != width || mIntputHeight != height)) {
            destroyPixelBuffers();
        }
        if (mPboIds != null) {
            return;
        }

        mPboIds = IntBuffer.allocate(PBO_MAX);
        //生成2个PBO
        GLES30.glGenBuffers(PBO_MAX, mPboIds);

        for( int i=0; i<PBO_MAX; i++ ) {

            //绑定到第一个PBO
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(i));
            //设置内存大小
            GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboSize, null,GLES30.GL_STATIC_READ);
        }

        //解除绑定PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int bindPixelBuffer(float[] rotationInfo) {

        int res = -1;

        //绑定到第一个PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboIndex));

        EffectTracker.getInstance().glReadPixels(0,0, mIntputWidth, mIntputHeight * 3/ 8,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE);

        //绑定到第二个PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboNewIndex));
        res = this.mPboTextureIds[mPboNewIndex];
        rotationInfo[0] = this.mPboCenterX[mPboNewIndex];
        rotationInfo[1] = this.mPboCenterY[mPboNewIndex];
        rotationInfo[2] = this.mPboAngle[mPboNewIndex];

        //glMapBufferRange会等待DMA传输完成，所以需要交替使用pbo
        //映射内存
        mTempBuffer = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
        if(mTempBuffer!=null) {
            byte[] data = new byte[8];
            mTempBuffer.get(data,0,8);
            mTempBuffer.position(0);
//            Log.e("MGREC", System.identityHashCode(mTempBuffer) + "--" + data[0] + "," + data[1]+ "," + data[2]+ "," + data[3]);
        }
        //解除映射
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);

        unbindPixelBuffer();

        return res;
    }

    //解绑pbo
    private void unbindPixelBuffer() {
        //解除绑定PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);

        //交换索引
        mPboIndex = (mPboIndex + 1) % PBO_MAX;
        mPboNewIndex = (mPboNewIndex + 1) % PBO_MAX;
    }

}
