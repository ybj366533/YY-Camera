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

import android.graphics.Rect;
import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FaceDrawFilter extends GTVImageFilter {

    public static final String FACEDBG_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String FACEDBG_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private static final String LINE_VERTEX_SHADER =
            "attribute vec4 vPosition;\n" +
                    "void main() {\n" +
                    "  gl_Position = vPosition;\n" +
                    "  gl_PointSize = 8.0;\n" +
                    "}";

    private static final String LINE_FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = vec4(1.0, 0.0, 0.0, 0.5);\n" +
                    "}";

    protected int mDebugGLProgId;
    protected int mDebugGLAttribPosition;

    public FaceDrawFilter() {

        super(FACEDBG_VERTEX_SHADER, FACEDBG_FRAGMENT_SHADER);

        mDebugGLProgId = OpenGlUtils.loadProgram(LINE_VERTEX_SHADER, LINE_FRAGMENT_SHADER);
        mDebugGLAttribPosition = GLES20.glGetAttribLocation(mDebugGLProgId, "vPosition");

        ////////////////////////////////
        pointsBuffer = ByteBuffer
                .allocateDirect(136 * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        facepBuffer = ByteBuffer
                .allocateDirect(8 * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
    }
    //float类型的字节数
    private final int BYTES_PER_FLOAT = 4;
    // 数组中每个顶点的坐标数
    private final int COORDS_PER_VERTEX = 2;
    //矩形顶点坐标
    private float squareCoords[] = { -0.5f,  0.5f ,   // top left
            -0.5f, -0.5f ,   // bottom left
            0.5f, -0.5f ,   // bottom right
            0.5f,  0.5f }; // top right

    private FloatBuffer vertexBuffer;

    private FloatBuffer pointsBuffer;
    private FloatBuffer facepBuffer;

    private Rect faceRect;
//    private Point[] facePointList = new Point[68];

    public void updateFaceRect(Rect rect) {

        faceRect = new Rect(rect.left*2, rect.top*2, rect.right*2, rect.bottom*2);
    }

    public void updateFaceThinList(int[] l) {

        facepBuffer.position(0);
        for( int i=0; i<4; i++ ) {
            float x = (l[i*2]*2.0f/mIntputWidth)*2.0f - 1.0f;
            float y = (l[i*2+1]*2.0f/mIntputHeight)*2.0f - 1.0f;
            y = 0.0f - y;
            facepBuffer.put(x);
            facepBuffer.put(y);
        }
    }

    public void updateFacePointList(float[] l) {

        pointsBuffer.position(0);
        for( int i=0; i<68; i++ ) {
            float x = (l[i]*2/mIntputWidth)*2.0f - 1.0f;
            float y = (l[i+68]*2/mIntputHeight)*2.0f - 1.0f;
            y = 0.0f - y;
            pointsBuffer.put(x);
            pointsBuffer.put(y);
        }
//        pointsBuffer.put(l, 0, 136);
    }

    int error;
    /*
    protected void onDrawArraysAfter() {

        Rect rect = faceRect;

        vertexBuffer = ByteBuffer
                .allocateDirect(squareCoords.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        if( rect != null ) {

            float left = ((rect.left * 1.0f) / (mIntputWidth * 1.0f))*2.0f - 1.0f;
            float top = (( (rect.top) * 1.0f) / (mIntputHeight * 1.0f))*2.0f - 1.0f;
            float right = ((rect.right * 1.0f) / (mIntputWidth * 1.0f))*2.0f - 1.0f;
            float bottom = (( (rect.bottom) * 1.0f) / (mIntputHeight * 1.0f))*2.0f - 1.0f;

            top = 0.0f - top;
            bottom = 0.0f - bottom;

            float points[] = { left,  top ,   // top left
                    left, bottom,   // bottom left
                    right, bottom,   // bottom right
                    right, top };

            vertexBuffer.put(points);
        }
        else {

            vertexBuffer.put(squareCoords);
        }
        vertexBuffer.position(0);

        GLES20.glUseProgram(mDebugGLProgId);
        GLES20.glVertexAttribPointer(mDebugGLAttribPosition, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mDebugGLAttribPosition);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4);

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": onDrawArraysAfter glError 1111 " + error);
        }

        pointsBuffer.position(0);
        GLES20.glVertexAttribPointer(mDebugGLAttribPosition, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, pointsBuffer);
        GLES20.glEnableVertexAttribArray(mDebugGLAttribPosition);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 68);

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": onDrawArraysAfter glError 1111 " + error);
        }

        facepBuffer.position(0);
        GLES20.glVertexAttribPointer(mDebugGLAttribPosition, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, facepBuffer);
        GLES20.glEnableVertexAttribArray(mDebugGLAttribPosition);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 4);

        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e("ES20_ERROR", ": onDrawArraysAfter glError 1111 " + error);
        }
    }
*/
    public void onDrawPoints() {

        onDrawArraysAfter();
    }

}
