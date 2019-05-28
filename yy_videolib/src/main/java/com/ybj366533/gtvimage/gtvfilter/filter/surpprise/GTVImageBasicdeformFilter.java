package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

import java.nio.FloatBuffer;

/**
 * Created by gtv on 2017/9/26.
 */

public class GTVImageBasicdeformFilter extends GTVImageFilter {

    public static final String BASICDEFORM_VERTEX_SHADER = "" +
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

    public static final String BASICDEFORM_FRAGMENT_SHADER = "precision highp float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform float               iGlobalTime;\n" +
            "uniform sampler2D           inputImageTexture;\n" +
            "varying vec2                textureCoordinate;\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "{\t\n" +
            "\tfloat stongth = 0.3;\n" +
            "\tvec2 uv = fragCoord.xy;\n" +
            "\tfloat waveu = sin((uv.y + iGlobalTime) * 20.0) * 0.5 * 0.05 * stongth;\n" +
            "\tfragColor = texture2D(inputImageTexture, uv + vec2(waveu, 0));\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "\tmainImage(gl_FragColor, textureCoordinate);\n" +
            "}";

    final long START_TIME = System.currentTimeMillis();

    public GTVImageBasicdeformFilter() {
        super(BASICDEFORM_VERTEX_SHADER, BASICDEFORM_FRAGMENT_SHADER);
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
