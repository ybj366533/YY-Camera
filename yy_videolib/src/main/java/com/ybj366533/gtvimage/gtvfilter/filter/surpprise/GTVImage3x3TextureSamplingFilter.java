
package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

public class GTVImage3x3TextureSamplingFilter extends GTVImageFilter {
    public static final String THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" + 
            "attribute vec4 inputTextureCoordinate;\n" + 
            "\n" + 
            "uniform highp float texelWidth; \n" + 
            "uniform highp float texelHeight; \n" + 
            "\n" + 
            "varying vec2 textureCoordinate;\n" + 
            "varying vec2 leftTextureCoordinate;\n" + 
            "varying vec2 rightTextureCoordinate;\n" + 
            "\n" + 
            "varying vec2 topTextureCoordinate;\n" + 
            "varying vec2 topLeftTextureCoordinate;\n" + 
            "varying vec2 topRightTextureCoordinate;\n" + 
            "\n" + 
            "varying vec2 bottomTextureCoordinate;\n" + 
            "varying vec2 bottomLeftTextureCoordinate;\n" + 
            "varying vec2 bottomRightTextureCoordinate;\n" + 
            "\n" + 
            "void main()\n" + 
            "{\n" + 
            "    gl_Position = position;\n" + 
            "\n" + 
            "    vec2 widthStep = vec2(texelWidth, 0.0);\n" + 
            "    vec2 heightStep = vec2(0.0, texelHeight);\n" + 
            "    vec2 widthHeightStep = vec2(texelWidth, texelHeight);\n" + 
            "    vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);\n" + 
            "\n" + 
            "    textureCoordinate = inputTextureCoordinate.xy;\n" + 
            "    leftTextureCoordinate = inputTextureCoordinate.xy - widthStep;\n" + 
            "    rightTextureCoordinate = inputTextureCoordinate.xy + widthStep;\n" + 
            "\n" + 
            "    topTextureCoordinate = inputTextureCoordinate.xy - heightStep;\n" + 
            "    topLeftTextureCoordinate = inputTextureCoordinate.xy - widthHeightStep;\n" + 
            "    topRightTextureCoordinate = inputTextureCoordinate.xy + widthNegativeHeightStep;\n" + 
            "\n" + 
            "    bottomTextureCoordinate = inputTextureCoordinate.xy + heightStep;\n" + 
            "    bottomLeftTextureCoordinate = inputTextureCoordinate.xy - widthNegativeHeightStep;\n" + 
            "    bottomRightTextureCoordinate = inputTextureCoordinate.xy + widthHeightStep;\n" + 
            "}";

    public static final String SOBEL_EDGE_DETECTION = "" +
            "precision mediump float;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 topLeftTextureCoordinate;\n" +
            "varying vec2 topRightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 bottomTextureCoordinate;\n" +
            "varying vec2 bottomLeftTextureCoordinate;\n" +
            "varying vec2 bottomRightTextureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    float bottomLeftIntensity = texture2D(inputImageTexture, bottomLeftTextureCoordinate).r;\n" +
            "    float topRightIntensity = texture2D(inputImageTexture, topRightTextureCoordinate).r;\n" +
            "    float topLeftIntensity = texture2D(inputImageTexture, topLeftTextureCoordinate).r;\n" +
            "    float bottomRightIntensity = texture2D(inputImageTexture, bottomRightTextureCoordinate).r;\n" +
            "    float leftIntensity = texture2D(inputImageTexture, leftTextureCoordinate).r;\n" +
            "    float rightIntensity = texture2D(inputImageTexture, rightTextureCoordinate).r;\n" +
            "    float bottomIntensity = texture2D(inputImageTexture, bottomTextureCoordinate).r;\n" +
            "    float topIntensity = texture2D(inputImageTexture, topTextureCoordinate).r;\n" +
            "    float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
            "    float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
            "\n" +
            "    float mag = length(vec2(h, v));\n" +
            "\n" +
            "    gl_FragColor = vec4(vec3(mag), 1.0);\n" +
            "}";

    private int mUniformTexelWidthLocation;
    private int mUniformTexelHeightLocation;

    private boolean mHasOverriddenImageSizeFactor = false;
    private float mTexelWidth; 
    private float mTexelHeight;
    private float mLineSize = 1.0f;

//    public GTVImage3x3TextureSamplingFilter() {
//        this(NO_FILTER_VERTEX_SHADER);
//    }

        public GTVImage3x3TextureSamplingFilter() {
        this(THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER);
    }

    public GTVImage3x3TextureSamplingFilter(final String fragmentShader) {
        super(THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER, SOBEL_EDGE_DETECTION);
    }

    @Override
    public void onInit() {
        super.onInit();
        mUniformTexelWidthLocation = GLES20.glGetUniformLocation(getProgram(), "texelWidth");
        mUniformTexelHeightLocation = GLES20.glGetUniformLocation(getProgram(), "texelHeight");
        if (mTexelWidth != 0) {
            updateTexelValues();
        }
    }

    @Override
    public void onDisplaySizeChanged(final int width, final int height) {
        super.onDisplaySizeChanged(width, height);
        if (!mHasOverriddenImageSizeFactor) {
            setLineSize(mLineSize);
        }
    }

    public void setTexelWidth(final float texelWidth) {
        mHasOverriddenImageSizeFactor = true;
        mTexelWidth = texelWidth;
        setFloat(mUniformTexelWidthLocation, texelWidth);
    }

    public void setTexelHeight(final float texelHeight) {
        mHasOverriddenImageSizeFactor = true;
        mTexelHeight = texelHeight;
        setFloat(mUniformTexelHeightLocation, texelHeight);
    }

    public void setLineSize(final float size) {
        //Log.e("testtest77", " " + getOutputWidth() + " " + getOutputHeight());
        mLineSize = size;
        mTexelWidth = size / getOutputWidth();
        mTexelHeight = size / getOutputHeight();
        updateTexelValues();
    }

    private void updateTexelValues() {
        setFloat(mUniformTexelWidthLocation, mTexelWidth);
        setFloat(mUniformTexelHeightLocation, mTexelHeight);
    }
}
