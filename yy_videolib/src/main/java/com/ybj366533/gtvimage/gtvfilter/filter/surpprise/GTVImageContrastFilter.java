package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

/**
 * Created by gtv on 2018/4/26.
 */

public class GTVImageContrastFilter extends GTVImageFilter {

    public static final String CONTRAST_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
                    " \n" +
                    " uniform sampler2D inputImageTexture;\n" +
                    " uniform lowp float contrast;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "     \n" +
                    "     gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" +
                    " }";

    private int mContrastUniform;
    private float mContrast = 0.5f;

    public GTVImageContrastFilter() {
        super(NO_FILTER_VERTEX_SHADER, CONTRAST_FRAGMENT_SHADER);
    }

    public void onInit() {
        super.onInit();

        mContrastUniform = GLES20.glGetUniformLocation(getProgram(), "contrast");
    }

    public void setContrast(float f) {
        this.mContrast = f;
        setFloat(mContrastUniform, this.mContrast);
    }

    @Override
    protected void onDrawArraysPre() {
        //setFloat(mContrastUniform, this.mContrast);
    }
}
