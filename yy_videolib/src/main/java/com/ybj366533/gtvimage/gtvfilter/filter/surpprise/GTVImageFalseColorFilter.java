
package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

public class GTVImageFalseColorFilter extends GTVImageFilter {
    public static final String FALSECOLOR_FRAGMENT_SHADER = "" +
            "precision lowp float;\n" +
            "\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform float intensity;\n" +
            "uniform vec3 firstColor;\n" +
            "uniform vec3 secondColor;\n" +
            "\n" +
            "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
            "\n" +
            "gl_FragColor = vec4( mix(firstColor.rgb, secondColor.rgb, luminance), textureColor.a);\n" +
            "}\n";

    private float[] mFirstColor;
    private int mFirstColorLocation;
    private float[] mSecondColor;
    private int mSecondColorLocation;

    public GTVImageFalseColorFilter() {
        this(0f, 0f, 0.5f, 1f, 0f, 0f);
    }

    public GTVImageFalseColorFilter(float firstRed, float firstGreen, float firstBlue, float secondRed, float secondGreen, float secondBlue) {
        this(new float[]{firstRed, firstGreen, firstBlue}, new float[]{secondRed, secondGreen, secondBlue});
    }

    public GTVImageFalseColorFilter(float[] firstColor, float[] secondColor) {
        super(NO_FILTER_VERTEX_SHADER, FALSECOLOR_FRAGMENT_SHADER);
        mFirstColor = firstColor;
        mSecondColor = secondColor;
    }

    @Override
    public void onInit() {
        super.onInit();
        mFirstColorLocation = GLES20.glGetUniformLocation(getProgram(), "firstColor");
        mSecondColorLocation = GLES20.glGetUniformLocation(getProgram(), "secondColor");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setFirstColor(mFirstColor);
        setSecondColor(mSecondColor);
    }

    public void setFirstColor(final float[] firstColor) {
        mFirstColor = firstColor;
        setFloatVec3(mFirstColorLocation, firstColor);
    }

    public void setSecondColor(final float[] secondColor) {
        mSecondColor = secondColor;
        setFloatVec3(mSecondColorLocation, secondColor);
    }
}
