package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

import java.nio.FloatBuffer;

/**
 * Created by gtv on 2017/9/26.
 */

public class GTVImageChromaAberrationFilter extends GTVImageFilter {

    public static final String CHROMAABERRATION_VERTEX_SHADER = "" +
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

    public static final String CHROMAABERRATION_FRAGMENT_SHADER = "precision mediump float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform float               iGlobalTime;\n" +
            "uniform sampler2D           inputImageTexture;\n" +
            "varying vec2                textureCoordinate;\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "{\n" +
            "    vec2 uv = fragCoord.xy;\n" +
            "\n" +
            "\tfloat amount = 0.0;\n" +
            "\t\n" +
            "\tamount = (1.0 + sin(iGlobalTime*6.0)) * 0.5;\n" +
            "\tamount *= 1.0 + sin(iGlobalTime*16.0) * 0.5;\n" +
            "\tamount *= 1.0 + sin(iGlobalTime*19.0) * 0.5;\n" +
            "\tamount *= 1.0 + sin(iGlobalTime*27.0) * 0.5;\n" +
            "\tamount = pow(amount, 3.0);\n" +
            "\n" +
            "\tamount *= 0.05;\n" +
            "\t\n" +
            "    vec3 col;\n" +
            "    col.r = texture2D( inputImageTexture, vec2(uv.x+amount,uv.y) ).r;\n" +
            "    col.g = texture2D( inputImageTexture, uv ).g;\n" +
            "    col.b = texture2D( inputImageTexture, vec2(uv.x-amount,uv.y) ).b;\n" +
            "\n" +
            "\tcol *= (1.0 - amount * 0.5);\n" +
            "\t\n" +
            "    fragColor = vec4(col,1.0);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "\tmainImage(gl_FragColor, textureCoordinate);\n" +
            "}";

    final long START_TIME = System.currentTimeMillis();

    public GTVImageChromaAberrationFilter() {
        super(CHROMAABERRATION_VERTEX_SHADER, CHROMAABERRATION_FRAGMENT_SHADER);
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
