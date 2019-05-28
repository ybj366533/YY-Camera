
package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.OpenGlUtils;

public class GTVShakeFilter extends GTVImageFilter {

//    public static final String SHAKE_VERTEX_SHADER = "" +
//            "attribute vec4 position;\n" +
//            "attribute vec4 inputTextureCoordinate;\n" +
//            " \n" +
//            "varying vec2 textureCoordinate;\n" +
//            " \n" +
//            "uniform vec2 singleStepOffset; \n" +
//            "\n" +
//            "void main()\n" +
//            "{\n" +
//            "    gl_Position = position * 0.9;\n" +
//            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
//            "}";

//    public static final String SHAKE_VERTEX_SHADER = "" +
//            "attribute vec4 position;\n" +
//            "attribute vec4 inputTextureCoordinate;\n" +
//            " \n" +
//            "varying vec2 textureCoordinate;\n" +
//            " \n" +
//            "uniform vec2 singleStepOffset; \n" +
//            "\n" +
//            "void main()\n" +
//            "{\n" +
//            "    gl_Position = position;\n" +
//            "    textureCoordinate = vec2(0.5f,0.5f) + (inputTextureCoordinate.xy - vec2(0.5f,0.5f)) * singleStepOffset;\n" +
//            "}";

    public static final String SHAKE_FRAGMENT_SHADER = " precision highp float;\n" +
            " \n" +
            " varying vec2 textureCoordinate;\n" +
            " varying vec2 textureCoordinate2;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " \n" +
            "uniform highp vec4 params; \n" +
            " void main()\n" +
            " {\n" +
            "     vec2 redCoordinate = vec2(textureCoordinate.x, textureCoordinate.y);\n" +
            "     vec2 blueCoordinate = vec2(textureCoordinate.x+params.r, textureCoordinate.y+params.g);\n" +
            "     vec2 greenCoordinate = vec2(textureCoordinate.x+params.b, textureCoordinate.y+params.a);\n" +
            "     \n" +
            "     vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     \n" +
            "     vec4 greenTextureColor = texture2D(inputImageTexture2, greenCoordinate);\n" +
            "     vec4 blueTextureColor = texture2D(inputImageTexture2, blueCoordinate);\n" +
            "     vec4 redTextureColor = texture2D(inputImageTexture2, redCoordinate);\n" +
            "     \n" +
            "     vec4 greenColor = vec4(0.0, greenTextureColor.g, 0.0, greenTextureColor.a);\n" +
            "     vec4 blueColor = vec4(0.0, 0.0, blueTextureColor.b, blueTextureColor.a);\n" +
            "     vec4 redColor = vec4(redTextureColor.r, 0.0, 0.0, redTextureColor.a);\n" +
            "     \n" +
            "     gl_FragColor = vec4(redColor.r, greenColor.g, blueColor.b, textureColor.a);\n" +
            " }";

    //private int mSingleStepOffsetLocation;
    private int mParamsLocation;
    private float[] mParamsData = new float[4];
    //private float[] mSingleStepOffset = new float[2];

    private int frameCount = 0;
    private float[] mTextSize = new float[2];

    java.util.Random random=new java.util.Random();

    public GTVShakeFilter() {
        super(NO_FILTER_VERTEX_SHADER, SHAKE_FRAGMENT_SHADER);
        frameCount = 0;
        setTexelSize(480, 640);
    }

    protected void onInit() {
        super.onInit();
        //mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void setTexelSize(final float w, final float h) {
        //setFloatVec2(mSingleStepOffsetLocation, new float[] {2.0f / w, 2.0f / h});
        mTextSize[0] = 2.0f/w;
        mTextSize[1] = 2.0f/h;
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        setTexelSize(width, height);
    }

    public int onDrawFrame(final int textureId) {
        //updateShakeParam();
        frameCount++;
        float ratio = 1.0f;
        if(frameCount % 15 < 9) {
            // 无特效
            mParamsData[0] = 0;
            mParamsData[1] = 0;
            mParamsData[2] = 0;
            mParamsData[3] = 0;

//            mSingleStepOffset[0] = 1.0f;
//            mSingleStepOffset[1] = 1.0f;
        } else {
            int intensity = frameCount%15 - 9;

            mParamsData[0] = mTextSize[0] * (float)intensity;
            mParamsData[1] = mTextSize[1] *  (float)intensity;
            mParamsData[2] = mTextSize[0] * 2 * (float)intensity;
            mParamsData[3] = mTextSize[1] * 2 * (float)intensity;

//            mSingleStepOffset[0] = (float)Math.pow(0.95, (double) intensity);
//            mSingleStepOffset[1] = (float)Math.pow(0.95, (double) intensity);

//            mSingleStepOffset[0] = 1.0f - 0.05f * intensity;
//            mSingleStepOffset[1] = 1.0f - 0.05f * intensity;

            ratio = 1.0f +  0.05f * intensity;

        }

        setFloatVec4(mParamsLocation, mParamsData);
        //setFloatVec2(mSingleStepOffsetLocation, mSingleStepOffset);

        OpenGlUtils.fitCubeMagnify(mGLCubeBuffer,ratio);

        return super.onDrawFrame(textureId);
    }

    public void updateBeautyParam() {

        setFloatVec4(mParamsLocation, mParamsData);
    }

    public void updateShakeParam() {

        mParamsData[0] = random.nextFloat();
        mParamsData[1] = random.nextFloat();
        mParamsData[2] = random.nextFloat();
        mParamsData[3] = random.nextFloat();

        setFloatVec4(mParamsLocation, mParamsData);
    }

}
