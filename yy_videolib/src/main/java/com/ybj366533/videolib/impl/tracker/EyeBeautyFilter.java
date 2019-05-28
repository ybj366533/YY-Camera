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

public class EyeBeautyFilter extends GTVImageFilter {

    public static final String GTV_BIGEYE_FRAGMENT_SHADER = "precision highp float;\n" +
            " \n" +
            " varying highp vec2 textureCoordinate;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " \n" +
            " uniform highp float scaleRatio;// 缩放系数，0无缩放，大于0则放大\n" +
            " uniform highp float radius;// 缩放算法的作用域半径\n" +
            " uniform highp vec2 leftEyeCenterPosition; // 左眼控制点，越远变形越小\n" +
            " uniform highp vec2 rightEyeCenterPosition; // 右眼控制点\n" +
            " uniform float aspectRatio; // 所处理图像的宽高比\n" +
            " \n" +
            " highp vec2 warpPositionToUse(vec2 centerPostion, vec2 currentPosition, float radius, float scaleRatio, float aspectRatio)\n" +
            "{\n" +
            "    vec2 positionToUse = currentPosition;\n" +
            "    vec2 currentPositionToUse = vec2(currentPosition.x, currentPosition.y * aspectRatio + 0.5 - 0.5 * aspectRatio);\n" +
            "    vec2 centerPostionToUse = vec2(centerPostion.x, centerPostion.y * aspectRatio + 0.5 - 0.5 * aspectRatio);\n" +
            "    \n" +
            "    float r = distance(currentPositionToUse, centerPostionToUse);\n" +
            "    \n" +
            "    if(r < radius)\n" +
            "    {\n" +
            "        float alpha = 1.0 - scaleRatio * pow(r / radius - 1.0, 2.0);\n" +
            "        positionToUse = centerPostion + alpha * (currentPosition - centerPostion);\n" +
            "    }\n" +
            "    return positionToUse;\n" +
            "}\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     vec2 positionToUse = warpPositionToUse(leftEyeCenterPosition, textureCoordinate, radius, scaleRatio, aspectRatio);\n" +
            "     \n" +
            "     positionToUse = warpPositionToUse(rightEyeCenterPosition, positionToUse, radius, scaleRatio, aspectRatio);\n" +
            "     \n" +
            "     gl_FragColor = texture2D(inputImageTexture, positionToUse);\n" +
            " }";

    private int leftEyeUniform;
    private int rightEyeUniform;

    private int aspectRatioUniform;
    private int radiusUniform;
    private int scaleRatioUniform;

    private float intensity = 0.0f;
    private float[] left = new float[] {0.25f, 0.5f};
    private float[] right = new float[] {0.75f, 0.5f};

    public EyeBeautyFilter() {
        super(NO_FILTER_VERTEX_SHADER, GTV_BIGEYE_FRAGMENT_SHADER);
    }

    protected void onInit() {
        super.onInit();

        leftEyeUniform = GLES20.glGetUniformLocation(getProgram(), "leftEyeCenterPosition");
        rightEyeUniform = GLES20.glGetUniformLocation(getProgram(), "rightEyeCenterPosition");

        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");
        radiusUniform = GLES20.glGetUniformLocation(getProgram(), "radius");
        scaleRatioUniform = GLES20.glGetUniformLocation(getProgram(), "scaleRatio");

        //float ratio = this.mIntputWidth*1.0f/this.mIntputHeight;
        setFloat(aspectRatioUniform, 1.0f);
        setFloat(radiusUniform, 0.15f);
        setFloat(scaleRatioUniform, intensity);

        setFloatVec2(leftEyeUniform, left);
        setFloatVec2(rightEyeUniform, right);
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

    public void setEyePosition(float[] a, float[] b) {

//        setFloatVec2(leftEyeUniform, a);
//        setFloatVec2(rightEyeUniform, b);
        // TODO:这里做了上下颠倒！！！
        left[0] = a[0];
        left[1] = 1.0f-a[1];

        right[0] = b[0];
        right[1] = 1.0f-b[1];
    }

    protected void onDrawArraysPre() {

        setFloat(scaleRatioUniform, intensity);

        setFloatVec2(leftEyeUniform, left);
        setFloatVec2(rightEyeUniform, right);

        float radius = 0.15f;
        float distance = (left[0] - right[0]) * (left[0] - right[0]) + (left[1] - right[1]) * (left[1] - right[1]);
        distance = (float) Math.sqrt(distance);
        radius = radius * distance / 0.5f;
        if( radius >= 0.15f )
            radius = 0.15f;
        setFloat(radiusUniform, radius);
    }
}
