package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

import java.nio.FloatBuffer;

/**
 * Created by gtv on 2017/9/26.
 */

public class GTVImageMoneyFilter extends GTVImageFilter {

    public static final String MONEY_VERTEX_SHADER = "" +
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

    public static final String MONEY_FRAGMENT_SHADER = "precision highp float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform float               iGlobalTime;\n" +
            "uniform sampler2D           inputImageTexture;\n" +
            "varying vec2                textureCoordinate;\n" +
            "\n" +
            "\n" +
            "// Money filter by Giacomo Preciado\n" +
            "// Based on: \"Free Engraved Illustration Effect Action for Photoshop\" - http://snip.ly/j0gq\n" +
            "// e-mail: giacomo@kyrie.pe\n" +
            "// website: http://kyrie.pe\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "{\n" +
            "    vec2 xy = fragCoord.xy / iResolution.yy;\n" +
            "\n" +
            "    float amplitud = 0.03;\n" +
            "    float frecuencia = 10.0;\n" +
            "    float gris = 1.0;\n" +
            "    float divisor = 8.0 / iResolution.y;\n" +
            "    float grosorInicial = divisor * 0.2;\n" +
            "\n" +
            "    const int kNumPatrones = 6;\n" +
            "\n" +
            "    // x: seno del angulo, y: coseno del angulo, z: factor de suavizado\n" +
            "\tvec3 datosPatron[kNumPatrones];\n" +
            "    datosPatron[0] = vec3(-0.7071, 0.7071, 3.0); // -45\n" +
            "    datosPatron[1] = vec3(0.0, 1.0, 0.6); // 0\n" +
            "    datosPatron[2] = vec3(0.0, 1.0, 0.5); // 0\n" +
            "    datosPatron[3] = vec3(1.0, 0.0, 0.4); // 90\n" +
            "    datosPatron[4] = vec3(1.0, 0.0, 0.3); // 90\n" +
            "    datosPatron[5] = vec3(0.0, 1.0, 0.2); // 0\n" +
            "\n" +
            "    vec4 color = texture2D(inputImageTexture, vec2(fragCoord.x / iResolution.x, xy.y));\n" +
            "    fragColor = color;\n" +
            "\n" +
            "    for(int i = 0; i < kNumPatrones; i++)\n" +
            "    {\n" +
            "        float coseno = datosPatron[i].x;\n" +
            "        float seno = datosPatron[i].y;\n" +
            "\n" +
            "        vec2 punto = vec2(\n" +
            "            xy.x * coseno - xy.y * seno,\n" +
            "            xy.x * seno + xy.y * coseno\n" +
            "        );\n" +
            "\n" +
            "        float grosor = grosorInicial * float(i + 1);\n" +
            "        float dist = mod(punto.y + grosor * 0.5 - sin(punto.x * frecuencia) * amplitud, divisor);\n" +
            "        float brillo = 0.3 * color.r + 0.4 * color.g + 0.3 * color.b;\n" +
            "\n" +
            "        if(dist < grosor && brillo < 0.75 - 0.12 * float(i))\n" +
            "        {\n" +
            "            // Suavizado\n" +
            "            float k = datosPatron[i].z;\n" +
            "            float x = (grosor - dist) / grosor;\n" +
            "            float fx = abs((x - 0.5) / k) - (0.5 - k) / k;\n" +
            "            gris = min(fx, gris);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    fragColor = vec4(gris, gris, gris, 1.0);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "\tmainImage(gl_FragColor, textureCoordinate*iResolution.xy);\n" +
            "}";

    final long START_TIME = System.currentTimeMillis();
    int iFrame = 0;

    public GTVImageMoneyFilter() {
        super(MONEY_VERTEX_SHADER, MONEY_FRAGMENT_SHADER);
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

        int iFrameLocation = GLES20.glGetUniformLocation(mGLProgId, "iFrame");
        GLES20.glUniform1i(iFrameLocation, iFrame);

        iFrame ++;
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
}
