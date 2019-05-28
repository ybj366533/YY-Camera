package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

import java.nio.FloatBuffer;

/**
 * Created by gtv on 2017/9/26.
 */

public class GTVImageEMInterferenceFilter extends GTVImageFilter {

    public static final String EMINTERFERENCE_VERTEX_SHADER = "" +
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

    public static final String EMINTERFERENCE_FRAGMENT_SHADER = "precision highp float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform float               iGlobalTime;\n" +
            "uniform sampler2D           inputImageTexture;\n" +
            "varying vec2                textureCoordinate;\n" +
            "\n" +
            "float rng2(vec2 seed)\n" +
            "{\n" +
            "    return fract(sin(dot(seed * floor(iGlobalTime * 12.), vec2(127.1,311.7))) * 43758.5453123);\n" +
            "}\n" +
            "\n" +
            "float rng(float seed)\n" +
            "{\n" +
            "    return rng2(vec2(seed, 1.0));\n" +
            "}\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "{\n" +
            "\tvec2 uv = fragCoord.xy;\n" +
            "    vec2 blockS = floor(uv * vec2(24., 9.));\n" +
            "    vec2 blockL = floor(uv * vec2(8., 4.));\n" +
            "\n" +
            "    float r = rng2(uv);\n" +
            "    vec3 noise = (vec3(r, 1. - r, r / 2. + 0.5) * 1.0 - 2.0) * 0.08;\n" +
            "\n" +
            "    float lineNoise = pow(rng2(blockS), 8.0) * pow(rng2(blockL), 3.0) - pow(rng(7.2341), 17.0) * 2.;\n" +
            "\n" +
            "    vec4 col1 = texture2D(inputImageTexture, uv);\n" +
            "    vec4 col2 = texture2D(inputImageTexture, uv + vec2(lineNoise * 0.05 * rng(5.0), 0));\n" +
            "    vec4 col3 = texture2D(inputImageTexture, uv - vec2(lineNoise * 0.05 * rng(31.0), 0));\n" +
            "\n" +
            "\tfragColor = vec4(vec3(col1.x, col2.y, col3.z) + noise, 1.0);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "\tmainImage(gl_FragColor, textureCoordinate);\n" +
            "}";

    final long START_TIME = System.currentTimeMillis();

    public GTVImageEMInterferenceFilter() {
        super(EMINTERFERENCE_VERTEX_SHADER, EMINTERFERENCE_FRAGMENT_SHADER);
    }

    protected void onDrawArraysPre() {

        int[] iResolution = null;

        if( mIntputWidth > 0 && mIntputHeight > 0 ) {
            iResolution = new int[]{mIntputWidth, mIntputHeight};
        }
        else {
            iResolution = new int[]{480, 640};
        }

        int iResolutionLocation = GLES20.glGetUniformLocation(mGLProgId, "iResolution");
        GLES20.glUniform3fv(iResolutionLocation, 1,
                FloatBuffer.wrap(new float[]{(float) iResolution[0], (float) iResolution[1], 1.0f}));

        float time = ((float) (System.currentTimeMillis() - START_TIME)) / 1000.0f;
        int iGlobalTimeLocation = GLES20.glGetUniformLocation(mGLProgId, "iGlobalTime");
        GLES20.glUniform1f(iGlobalTimeLocation, time);
    }

}
