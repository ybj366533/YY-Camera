
package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

public class GTVSoulOutFilter extends GTVImageFilter {

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

    public static final String SOUL_OUT_FRAGMENT_SHADER = " precision highp float;\n" +
            " \n" +
            " varying vec2 textureCoordinate;\n" +
            " varying vec2 textureCoordinate2;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            "\n" +
            "uniform vec2 singleStepOffset; \n" +
            " void main()\n" +
            " {\n" +
            "     vec2 blurCoordinates = vec2(0.5,0.5) + (textureCoordinate.xy - vec2(0.5,0.5)) * singleStepOffset;\n" +
            "     \n" +
            "     vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     \n" +
            "     vec4 textureColor2 = texture2D(inputImageTexture, blurCoordinates);\n" +
            "     \n" +
            "     \n" +
            "     gl_FragColor = vec4(mix(textureColor.rgb, textureColor2.rgb,0.5), 1.0);\n" +
            " }";

    private int mSingleStepOffsetLocation;
    //private int mParamsLocation;
    //private float[] mParamsData = new float[4];
    private float[] mSingleStepOffset = new float[2];

    private int frameCount = 0;
    //private float[] mTextSize = new float[2];

    //java.util.Random random=new java.util.Random();

    public GTVSoulOutFilter() {
        super(NO_FILTER_VERTEX_SHADER, SOUL_OUT_FRAGMENT_SHADER);
        frameCount = 0;
        //setTexelSize(480, 640);
    }

    protected void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        //mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
    }

    protected void onDestroy() {
        super.onDestroy();
    }

//    private void setTexelSize(final float w, final float h) {
//        //setFloatVec2(mSingleStepOffsetLocation, new float[] {2.0f / w, 2.0f / h});
//        mTextSize[0] = 2.0f/w;
//        mTextSize[1] = 2.0f/h;
//    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        //setTexelSize(width, height);
    }

    public int onDrawFrame(final int textureId) {
        //updateShakeParam();
        frameCount++;
        //float ratio = 1.0f;
        if(frameCount % 15 < 8) {

            mSingleStepOffset[0] = 1.0f;
           mSingleStepOffset[1] = 1.0f;
        } else {
            int intensity = frameCount%15 - 7;
            float ratio = (float) Math.pow(1.1, intensity);

            mSingleStepOffset[0] = 1.0f/ratio;
            mSingleStepOffset[1] = 1.0f/ratio;


        }

        setFloatVec2(mSingleStepOffsetLocation, mSingleStepOffset);

        //OpenGlUtils.fitCubeMagnify(mGLCubeBuffer,ratio);

        return super.onDrawFrame(textureId);
    }



}
