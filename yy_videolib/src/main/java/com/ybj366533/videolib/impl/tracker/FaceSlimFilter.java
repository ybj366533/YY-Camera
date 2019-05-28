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

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

public class FaceSlimFilter extends GTVImageFilter {

    public static final String GTV_THINFACE_FRAGMENT_SHADER = "\n" +
            " precision highp float;\n" +
            " \n" +
            " varying highp vec2 textureCoordinate;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " \n" +
            " uniform highp float radius;\n" +
            " uniform highp float aspectRatio;\n" +
            " \n" +
            " uniform float leftContourPoints[2];\n" +
            " uniform float rightContourPoints[2];\n" +
            " \n" +
            " uniform float deltaArray[1];\n" +
            " uniform int arraySize;\n" +
            " \n" +
            " uniform highp float twoRadius;\n" +
            " uniform float twoDeltaArray[1];\n" +
            " uniform float twoLeftContourPoints[2];\n" +
            " uniform float twoRightContourPoints[2];\n" +
            " \n" +
            " highp vec2 warpPositionToUse(vec2 currentPoint, vec2 contourPointA,  vec2 contourPointB, float radius, float delta, float aspectRatio)\n" +
            "{\n" +
            "    vec2 positionToUse = currentPoint;\n" +
            "    \n" +
            "    vec2 currentPointToUse = vec2(currentPoint.x, currentPoint.y * aspectRatio + 0.5 - 0.5 * aspectRatio);\n" +
            "    vec2 contourPointAToUse = vec2(contourPointA.x, contourPointA.y * aspectRatio + 0.5 - 0.5 * aspectRatio);\n" +
            "    \n" +
            "    float r = distance(currentPointToUse, contourPointAToUse);\n" +
            "    if(r < radius)\n" +
            "    {\n" +
            "        vec2 dir = normalize(contourPointB - contourPointA);\n" +
            "        float dist = radius * radius - r * r;\n" +
            "        float alpha = dist / (dist + (r-delta) * (r-delta));\n" +
            "        alpha = alpha * alpha;\n" +
            "        \n" +
            "        positionToUse = positionToUse - alpha * delta * dir;\n" +
            "    }\n" +
            "    return positionToUse;\n" +
            "}\n" +
            " \n" +
            " void main()\n" +
            "{\n" +
            "    vec2 positionToUse = textureCoordinate;\n" +
            "    \n" +
            "    for(int i = 0; i < arraySize; i++)\n" +
            "    {\n" +
            "        positionToUse = warpPositionToUse(positionToUse, vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), radius, deltaArray[i], aspectRatio);\n" +
            "        positionToUse = warpPositionToUse(positionToUse, vec2(twoLeftContourPoints[i * 2], twoLeftContourPoints[i * 2 + 1]), vec2(twoRightContourPoints[i * 2], twoRightContourPoints[i * 2 + 1]), twoRadius, twoDeltaArray[i], aspectRatio);\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = texture2D(inputImageTexture, positionToUse);\n" +
            "}";

    private int radiusUniform;
    private int aspectRatioUniform;

    private int arraySizeUniform;
    private int deltaArrayUniform;

    private int leftContourPointsUniform;
    private int rightContourPointsUniform;

    private int twoRadiusUniform;
    private int twoDeltaArrayUniform;

    private int twoLeftContourPointsUniform;
    private int twoRightContourPointsUniform;

    private float intensity = 0.3f;
    private float[] left = new float[] {0.25f, 0.25f};
    private float[] right = new float[] {0.5f, 0.5f};

    private float[] twoLeft = new float[] {0.25f, 0.25f};
    private float[] twoRight = new float[] {0.5f, 0.5f};

    public FaceSlimFilter() {
        super(NO_FILTER_VERTEX_SHADER, GTV_THINFACE_FRAGMENT_SHADER);
    }

    protected void onInit() {
        super.onInit();

        radiusUniform = GLES20.glGetUniformLocation(getProgram(), "radius");
        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");

        arraySizeUniform = GLES20.glGetUniformLocation(getProgram(), "arraySize");
        deltaArrayUniform = GLES20.glGetUniformLocation(getProgram(), "deltaArray");

        leftContourPointsUniform = GLES20.glGetUniformLocation(getProgram(), "leftContourPoints");
        rightContourPointsUniform = GLES20.glGetUniformLocation(getProgram(), "rightContourPoints");

        twoRadiusUniform = GLES20.glGetUniformLocation(getProgram(), "twoRadius");
        twoDeltaArrayUniform = GLES20.glGetUniformLocation(getProgram(), "twoDeltaArray");
        twoLeftContourPointsUniform = GLES20.glGetUniformLocation(getProgram(), "twoLeftContourPoints");
        twoRightContourPointsUniform = GLES20.glGetUniformLocation(getProgram(), "twoRightContourPoints");

        float ratio = this.mIntputHeight*1.0f/this.mIntputWidth*1.0f;
        setFloat(aspectRatioUniform, ratio);
        setFloat(radiusUniform, 0.0f);

        setInteger(arraySizeUniform, 1);
        setFloatArray(deltaArrayUniform, new float[]{intensity});

        setFloatArray(leftContourPointsUniform, left);
        setFloatArray(rightContourPointsUniform, right);

        ////////////////////////////////////////////
        setFloat(twoRadiusUniform, 0.0f);
        setFloatArray(twoDeltaArrayUniform, new float[]{intensity});

        setFloatArray(twoLeftContourPointsUniform, left);
        setFloatArray(twoRightContourPointsUniform, right);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
    }

    public void setIntensity(float f) {

        //setFloat(scaleRatioUniform, f);
        intensity = f;
    }

    public void setLeftFacePosition(float[] a, float[] b) {

//        setFloatVec2(leftEyeUniform, a);
//        setFloatVec2(rightEyeUniform, b);
        // TODO:这里做了上下颠倒！！！
        left[0] = a[0];
        left[1] = 1.0f-a[1];

        right[0] = b[0];
        right[1] = 1.0f-b[1];
    }

    public void setRightFacePosition(float[] a, float[] b) {

//        setFloatVec2(leftEyeUniform, a);
//        setFloatVec2(rightEyeUniform, b);
        // TODO:这里做了上下颠倒！！！
        twoLeft[0] = a[0];
        twoLeft[1] = 1.0f-a[1];

        twoRight[0] = b[0];
        twoRight[1] = 1.0f-b[1];
    }

    protected void onDrawArraysPre() {

        float ratio = this.mIntputHeight*1.0f/this.mIntputWidth*1.0f;
        setFloat(aspectRatioUniform, ratio);


        float r = (float) Math.sqrt( (left[0]-right[0])*(left[0]-right[0]) + (left[1]-right[1])*(left[1]-right[1]) );
        setFloat(radiusUniform, r);

        setFloatArray(deltaArrayUniform, new float[]{r/5.0f*intensity});

        setFloatArray(leftContourPointsUniform, left);
        setFloatArray(rightContourPointsUniform, right);

        ////////////////////////////////////////////
        r = (float) Math.sqrt( (twoLeft[0]-twoRight[0])*(twoLeft[0]-twoRight[0]) + (twoLeft[1]-twoRight[1])*(twoLeft[1]-twoRight[1]) );
        setFloat(twoRadiusUniform, r);
        setFloatArray(twoDeltaArrayUniform, new float[]{r/5.0f*intensity});

        setFloatArray(twoLeftContourPointsUniform, twoLeft);
        setFloatArray(twoRightContourPointsUniform, twoRight);
    }

    protected void onDrawArraysAfter() {

    }
}
