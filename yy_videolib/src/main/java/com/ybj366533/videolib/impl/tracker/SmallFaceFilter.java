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

public class SmallFaceFilter extends GTVImageFilter {

    public static final String GTV_SMALL_FACE_FRAGMENT_SHADER = "precision highp float;\n" +
            " \n" +
            " varying highp vec2 textureCoordinate;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " \n" +
            " uniform highp float cos_data;\n" +
            " uniform highp float sin_data;\n" +
            " \n" +
            " uniform highp float scaleRatio;\n" +
            " uniform highp float radius;\n" +
            " uniform highp vec2 leftEyeCenterPosition;\n" +
            " uniform float aspectRatio;\n" +
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
            "        // rotate to get new y position\n" +
            "        float new_x = currentPositionToUse.x - centerPostionToUse.x;\n" +
            "        float new_y = currentPositionToUse.y - centerPostionToUse.y;\n" +
            "        \n" +
            "        new_y = sin_data * new_x + cos_data * new_y;\n" +
            "        \n" +
            "        //if( currentPositionToUse.y < centerPostionToUse.y ) {\n" +
            "        if( new_y <= 0.0 ) {\n" +
            "            positionToUse = currentPosition;\n" +
            "        }\n" +
            "        else {\n" +
            "            float alpha = 1.0 + scaleRatio * pow(r*2.0 / radius - 1.0, 2.0);\n" +
            "            alpha = (1.0 + scaleRatio) - (alpha - 1.0);\n" +
            "\n" +
            "            if( r > radius / 2.0 ) {\n" +
            "                float k1 = radius/2.0;\n" +
            "                float k2 = (r - k1) / k1;\n" +
            "                if( alpha > 1.0 )\n" +
            "                    alpha = 1.0 + (alpha-1.0) * (1.0-k2*k2);\n" +
            "            }\n" +
            "            \n" +
            "            float d = new_y;//currentPositionToUse.y - centerPostionToUse.y;\n" +
            "            d = d / radius;\n" +
            "\n" +
            "            // 1.44 -  (2*x-1.2)^2\n" +
            "            //float dd = pow(d * 2.0 - 1.2, 2.0);\n" +
            "            //dd = (1.44 - dd) / 1.5;\n" +
            "            float dd = pow(d * 2.0 - 1.0, 2.0);\n" +
            "            dd = (1.0 - dd) / 1.0;\n" +
            "\n" +
            "            alpha = 1.0 + (alpha - 1.0) * dd;\n" +
            "            \n" +
            "            positionToUse = centerPostion + alpha * (currentPosition - centerPostion);\n" +
            "        }\n" +
            "    }\n" +
            "    return positionToUse;\n" +
            "}\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     vec2 positionToUse = warpPositionToUse(leftEyeCenterPosition, textureCoordinate, radius, scaleRatio, aspectRatio);\n" +
            "     gl_FragColor = texture2D(inputImageTexture, positionToUse);\n" +
            " }";

    private int leftEyeUniform;

    private int aspectRatioUniform;
    private int radiusUniform;
    private int cosUniform;
    private int sinUniform;
    private int scaleRatioUniform;

    private float intensity = 0.0f;
    private float[] left = new float[] {0.25f, 0.5f};
    private float cos_data = 0.1f;
    private float sin_data = 0.1f;
    private float radius = 0.5f;

    public SmallFaceFilter() {
        super(NO_FILTER_VERTEX_SHADER, GTV_SMALL_FACE_FRAGMENT_SHADER);
    }

    protected void onInit() {
        super.onInit();

        leftEyeUniform = GLES20.glGetUniformLocation(getProgram(), "leftEyeCenterPosition");

        aspectRatioUniform = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");
        radiusUniform = GLES20.glGetUniformLocation(getProgram(), "radius");
        scaleRatioUniform = GLES20.glGetUniformLocation(getProgram(), "scaleRatio");
        cosUniform = GLES20.glGetUniformLocation(getProgram(), "cos_data");
        sinUniform = GLES20.glGetUniformLocation(getProgram(), "sin_data");

        //float ratio = this.mIntputWidth*1.0f/this.mIntputHeight;
        setFloat(aspectRatioUniform, 1.0f);
        setFloat(radiusUniform, 0.15f);
        setFloat(scaleRatioUniform, intensity);

        setFloatVec2(leftEyeUniform, left);
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
        intensity = f;//0.3f * f;
        //[self setFloat:(16.0f/9.0f) forUniformName:@"aspectRatio"];
        setFloat(aspectRatioUniform, (getIntputHeight()*1.0f)/(getIntputWidth()*1.0f));
    }

    public void setNoseAndChinPosition(float[] a, float[] b) {

        left[0] = a[0];
        left[1] = 1.0f-a[1];

        float as = (getIntputHeight()*1.0f)/(getIntputWidth()*1.0f);
        float r = (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) * as * as;
        r = (float)Math.sqrt(r);

        this.cos_data = ((1.0f-b[1]) - (1.0f-a[1])) * as / r;
        this.sin_data = (b[0] - a[0]) / r;

        this.radius = r * 2.8f;

        return;
    }

    protected void onDrawArraysPre() {

        setFloat(scaleRatioUniform, intensity * 2.0f * 0.36f * this.radius);

        setFloatVec2(leftEyeUniform, left);

        setFloat(radiusUniform, radius);

        setFloat(cosUniform, cos_data);
        setFloat(sinUniform, sin_data);
    }
}
