
package com.ybj366533.gtvimage.gtvfilter.filter.advanced;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

public class GTVBeautyBFilter extends GTVImageFilter {

    public static final String GTVBEAUTY_B_FRAGMENT_SHADER = "\n" +
            " precision highp float;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform vec2 singleStepOffset;\n" +
            " uniform highp vec4 params;\n" +
            " \n" +
            "// uniform highp float pinkness;\n" +
            " uniform highp float beautyflag;\n" +
            " \n" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " const highp vec3 W = vec3(0.299,0.587,0.114);\n" +
            " const mat3 saturateMatrix = mat3(\n" +
            "                                  1.1102,-0.0598,-0.061,\n" +
            "                                  -0.0774,1.0826,-0.1186,\n" +
            "                                  -0.0228,-0.0228,1.1772);\n" +
            " \n" +
            " float hardlight(float color)\n" +
            " {\n" +
            "     if(color <= 0.5)\n" +
            "     {\n" +
            "         color = color * color * 2.0;\n" +
            "     }\n" +
            "     else\n" +
            "     {\n" +
            "         color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);\n" +
            "     }\n" +
            "     return color;\n" +
            " }\n" +
            " \n" +
            " void main(){\n" +
            "     \n" +
            "     if( beautyflag == 0.0 ) {\n" +
            "         gl_FragColor.rgb = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     }\n" +
            "     else {\n" +
            "      \n" +
            "         vec2 blurCoordinates[24];\n" +
            "         \n" +
            "         blurCoordinates[0] = textureCoordinate.xy + singleStepOffset * vec2(0.0, -10.0);\n" +
            "         blurCoordinates[1] = textureCoordinate.xy + singleStepOffset * vec2(0.0, 10.0);\n" +
            "         blurCoordinates[2] = textureCoordinate.xy + singleStepOffset * vec2(-10.0, 0.0);\n" +
            "         blurCoordinates[3] = textureCoordinate.xy + singleStepOffset * vec2(10.0, 0.0);\n" +
            "         \n" +
            "         blurCoordinates[4] = textureCoordinate.xy + singleStepOffset * vec2(5.0, -8.0);\n" +
            "         blurCoordinates[5] = textureCoordinate.xy + singleStepOffset * vec2(5.0, 8.0);\n" +
            "         blurCoordinates[6] = textureCoordinate.xy + singleStepOffset * vec2(-5.0, 8.0);\n" +
            "         blurCoordinates[7] = textureCoordinate.xy + singleStepOffset * vec2(-5.0, -8.0);\n" +
            "         \n" +
            "         blurCoordinates[8] = textureCoordinate.xy + singleStepOffset * vec2(8.0, -5.0);\n" +
            "         blurCoordinates[9] = textureCoordinate.xy + singleStepOffset * vec2(8.0, 5.0);\n" +
            "         blurCoordinates[10] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, 5.0);\n" +
            "         blurCoordinates[11] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, -5.0);\n" +
            "         \n" +
            "         blurCoordinates[12] = textureCoordinate.xy + singleStepOffset * vec2(0.0, -6.0);\n" +
            "         blurCoordinates[13] = textureCoordinate.xy + singleStepOffset * vec2(0.0, 6.0);\n" +
            "         blurCoordinates[14] = textureCoordinate.xy + singleStepOffset * vec2(6.0, 0.0);\n" +
            "         blurCoordinates[15] = textureCoordinate.xy + singleStepOffset * vec2(-6.0, 0.0);\n" +
            "         \n" +
            "         blurCoordinates[16] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, -4.0);\n" +
            "         blurCoordinates[17] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, 4.0);\n" +
            "         blurCoordinates[18] = textureCoordinate.xy + singleStepOffset * vec2(4.0, -4.0);\n" +
            "         blurCoordinates[19] = textureCoordinate.xy + singleStepOffset * vec2(4.0, 4.0);\n" +
            "         \n" +
            "         blurCoordinates[20] = textureCoordinate.xy + singleStepOffset * vec2(-2.0, -2.0);\n" +
            "         blurCoordinates[21] = textureCoordinate.xy + singleStepOffset * vec2(-2.0, 2.0);\n" +
            "         blurCoordinates[22] = textureCoordinate.xy + singleStepOffset * vec2(2.0, -2.0);\n" +
            "         blurCoordinates[23] = textureCoordinate.xy + singleStepOffset * vec2(2.0, 2.0);\n" +
            "         \n" +
            "         float sampleColor = texture2D(inputImageTexture, textureCoordinate).g * 22.0;\n" +
            "         \n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[0]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[1]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[2]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[3]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[4]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[5]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[6]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[7]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[8]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[9]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[10]).g;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[11]).g;\n" +
            "         \n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[12]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[13]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[14]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[15]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[16]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[17]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[18]).g * 2.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[19]).g * 2.0;\n" +
            "         \n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[20]).g * 3.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[21]).g * 3.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[22]).g * 3.0;\n" +
            "         sampleColor += texture2D(inputImageTexture, blurCoordinates[23]).g * 3.0;\n" +
            "         \n" +
            "         sampleColor = sampleColor / 62.0;\n" +
            "         \n" +
            "         vec3 centralColor = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "         \n" +
            "         float highpass = centralColor.g - sampleColor + 0.5;\n" +
            "         \n" +
            "         for(int i = 0; i < 5;i++)\n" +
            "         {\n" +
            "             highpass = hardlight(highpass);\n" +
            "         }\n" +
            "         float lumance = dot(centralColor, W);\n" +
            "         \n" +
            "         float alpha = pow(lumance, params.r);\n" +
            "         \n" +
            "         vec3 smoothColor = centralColor + (centralColor-vec3(highpass))*alpha*0.1;\n" +
            "         \n" +
            "         smoothColor.r = clamp(pow(smoothColor.r, params.g),0.0,1.0);\n" +
            "         smoothColor.g = clamp(pow(smoothColor.g, params.g),0.0,1.0);\n" +
            "         smoothColor.b = clamp(pow(smoothColor.b, params.g),0.0,1.0);\n" +
            "         \n" +
            "         vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);\n" +
            "         vec3 bianliang = max(smoothColor, centralColor);\n" +
            "         vec3 rouguang = 2.0*centralColor*smoothColor + centralColor*centralColor - 2.0*centralColor*centralColor*smoothColor;\n" +
            "         \n" +
            "         gl_FragColor = vec4(mix(centralColor, lvse, alpha), 1.0);\n" +
            "         gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, alpha);\n" +
            "         gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, params.b);\n" +
            "         \n" +
            "         vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
            "         \n" +
            "         vec3 black = vec3(0.1, 0.1, 0.1);\n" +
            "         vec3 white = vec3(0.9, 0.9, 0.9);\n" +
            "         vec3 judge = max(black, gl_FragColor.rgb);\n" +
            "         vec3 judgewhite = min(white, gl_FragColor.rgb);\n" +
            "         float whitenflag = 1.0;\n" +
            "         if( (judge.r + judge.g + judge.b) < 0.301 ) {\n" +
            "             whitenflag = 0.0;\n" +
            "         }\n" +
            "         if( (judgewhite.r + judgewhite.g + judgewhite.b) > 2.699 ) {\n" +
            "             whitenflag = 0.0;\n" +
            "         }\n" +
            "         if( whitenflag > 0.0 ) {\n" +
            "             gl_FragColor.rgb = mix(gl_FragColor.rgb, satcolor, params.a);\n" +
            "         }\n" +
            "     }\n" +
            "}";

    private int mSingleStepOffsetLocation;
    private int mParamsLocation;
    private int mBeautyFlagLocation;
    private int mBeautyLevel;
    private float[] mParamsData = new float[4];

    public GTVBeautyBFilter(int level) {
        super(NO_FILTER_VERTEX_SHADER, GTVBEAUTY_B_FRAGMENT_SHADER);
        mBeautyLevel = level;
        resetBeautyParams();
    }

    protected void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
        mBeautyFlagLocation = GLES20.glGetUniformLocation(getProgram(), "beautyflag");
        setFloat(mBeautyFlagLocation, 1.0f);
        updateBeautyParam();
        //setBeautyLevel(mBeautyLevel);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void setTexelSize(final float w, final float h) {
        setFloatVec2(mSingleStepOffsetLocation, new float[]{2.0f / w, 2.0f / h});
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        setTexelSize(width, height);
    }

    // 磨皮，曝光，美白，饱和
    public float[] locateBeautyRange(int index) {

        float start, end;

        if (index == 0) {
            // 1.0 -> 0.1
            start = 1.0f;
            end = 0.1f;
        } else if (index == 1) {
            // 1.0 -> 0.1
            start = 1.0f;
            end = 0.1f;
        } else if (index == 2) {
            // 0.0 -> 0.8
            start = 0.4f;
            end = 0.1f;//0.0f;
        } else if (index == 3) {
            // 0.0 -> 0.8
            start = 0.0f;
            end = 0.8f;
        } else {
            start = 0.5f;
            end = 0.5f;
        }

        float[] res = new float[2];

        res[0] = start;
        res[1] = end;

        return res;
    }

    public float[] resetBeautyParams() {

        mParamsData[0] = 1.0f;
        mParamsData[1] = 1.0f;
        mParamsData[2] = 0.4f;
        mParamsData[3] = 0.0f;

        return getBeautyParam();
    }

    public float[] getBeautyParam() {

        float[] values = new float[4];

        for (int i = 0; i < 4; i++) {
            float[] range = locateBeautyRange(i);
            float start = range[0];
            float end = range[1];
            values[i] = (mParamsData[i] - start) / (end - start);
        }

        return values;
    }

    public void updateBeautyParam() {

        setFloatVec4(mParamsLocation, mParamsData);
    }

    public void setBeautyParam(int index, float percent) {

        if (percent < 0.0f || percent > 1.0f) {
            return;
        }

        float[] range = locateBeautyRange(index);
        float start, end;

        start = range[0];
        end = range[1];

        float value = (end - start) * percent + start;

        mParamsData[index] = value;

        setFloatVec4(mParamsLocation, mParamsData);

        return;
    }

    public void setBeautyLevel(int level){
        switch (level) {
            case 1:
                setFloatVec4(mParamsLocation, new float[] {1.0f, 1.0f, 0.15f, 0.15f});
                break;
            case 2:
                setFloatVec4(mParamsLocation, new float[] {0.8f, 0.9f, 0.2f, 0.2f});
                break;
            case 3:
                setFloatVec4(mParamsLocation, new float[] {0.6f, 0.8f, 0.25f, 0.25f});
                break;
            case 4:
                setFloatVec4(mParamsLocation, new float[] {0.4f, 0.7f, 0.38f, 0.3f});
                break;
            case 5:
                setFloatVec4(mParamsLocation, new float[] {0.33f, 0.63f, 0.4f, 0.35f});
                break;
            default:
                break;
        }
    }
}
