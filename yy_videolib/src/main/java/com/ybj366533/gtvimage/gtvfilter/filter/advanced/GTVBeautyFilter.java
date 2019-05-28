
package com.ybj366533.gtvimage.gtvfilter.filter.advanced;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

public class GTVBeautyFilter extends GTVImageFilter {

    public static final String GTVBEAUTY_FRAGMENT_SHADER = "precision highp float;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform vec2 singleStepOffset; \n" +
            "uniform highp vec4 params; \n" +
            "\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            "const highp vec3 W = vec3(0.299,0.587,0.114);\n" +
            "\n" +
            "const mat3 saturateMatrix = mat3(\n" +
            "\t\t1.1102,-0.0598,-0.061,\n" +
            "\t\t-0.0774,1.0826,-0.1186,\n" +
            "\t\t-0.0228,-0.0228,1.1772);\n" +
            "\n" +
            "vec2 blurCoordinates[24];\n" +
            "\n" +
            "float hardLight(float color)\n" +
            "{\n" +
            "\tif(color <= 0.5)\n" +
            "\t{\n" +
            "\t\tcolor = color * color * 2.0;\n" +
            "\t}\n" +
            "\telse\n" +
            "\t{\n" +
            "\t\tcolor = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);\n" +
            "\t}\n" +
            "\treturn color;\n" +
            "}\n" +
            "\n" +
            "void main(){\n" +
            "    vec3 centralColor = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "\n" +
            "\tblurCoordinates[0] = textureCoordinate.xy + singleStepOffset * vec2(0.0, -10.0);\n" +
            "\tblurCoordinates[1] = textureCoordinate.xy + singleStepOffset * vec2(0.0, 10.0);\n" +
            "\tblurCoordinates[2] = textureCoordinate.xy + singleStepOffset * vec2(-10.0, 0.0);\n" +
            "\tblurCoordinates[3] = textureCoordinate.xy + singleStepOffset * vec2(10.0, 0.0);\n" +
            "\tblurCoordinates[4] = textureCoordinate.xy + singleStepOffset * vec2(5.0, -8.0);\n" +
            "\tblurCoordinates[5] = textureCoordinate.xy + singleStepOffset * vec2(5.0, 8.0);\n" +
            "\tblurCoordinates[6] = textureCoordinate.xy + singleStepOffset * vec2(-5.0, 8.0);\n" +
            "\tblurCoordinates[7] = textureCoordinate.xy + singleStepOffset * vec2(-5.0, -8.0);\n" +
            "\tblurCoordinates[8] = textureCoordinate.xy + singleStepOffset * vec2(8.0, -5.0);\n" +
            "\tblurCoordinates[9] = textureCoordinate.xy + singleStepOffset * vec2(8.0, 5.0);\n" +
            "\tblurCoordinates[10] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, 5.0);\t\n" +
            "\tblurCoordinates[11] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, -5.0);\n" +
            "\tblurCoordinates[12] = textureCoordinate.xy + singleStepOffset * vec2(0.0, -6.0);\n" +
            "\tblurCoordinates[13] = textureCoordinate.xy + singleStepOffset * vec2(0.0, 6.0);\n" +
            "\tblurCoordinates[14] = textureCoordinate.xy + singleStepOffset * vec2(6.0, 0.0);\n" +
            "\tblurCoordinates[15] = textureCoordinate.xy + singleStepOffset * vec2(-6.0, 0.0);\n" +
            "\tblurCoordinates[16] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, -4.0);\n" +
            "\tblurCoordinates[17] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, 4.0);\n" +
            "\tblurCoordinates[18] = textureCoordinate.xy + singleStepOffset * vec2(4.0, -4.0);\n" +
            "\tblurCoordinates[19] = textureCoordinate.xy + singleStepOffset * vec2(4.0, 4.0);\n" +
            "\tblurCoordinates[20] = textureCoordinate.xy + singleStepOffset * vec2(-2.0, -2.0);\n" +
            "\tblurCoordinates[21] = textureCoordinate.xy + singleStepOffset * vec2(-2.0, 2.0);\n" +
            "\tblurCoordinates[22] = textureCoordinate.xy + singleStepOffset * vec2(2.0, -2.0);\n" +
            "\tblurCoordinates[23] = textureCoordinate.xy + singleStepOffset * vec2(2.0, 2.0);\n" +
            "\n" +
            "\tfloat sampleColor = centralColor.g * 22.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[0]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[1]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[2]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[3]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[4]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[5]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[6]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[7]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[8]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[9]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[10]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[11]).g;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[12]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[13]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[14]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[15]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[16]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[17]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[18]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[19]).g * 2.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[20]).g * 3.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[21]).g * 3.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[22]).g * 3.0;\n" +
            "\tsampleColor += texture2D(inputImageTexture, blurCoordinates[23]).g * 3.0;\n" +
            "\tsampleColor = sampleColor / 62.0;\n" +
            "\t\n" +
            "\tfloat highPass = centralColor.g - sampleColor + 0.5;\n" +
            "\t\n" +
            "\tfor(int i = 0; i < 5;i++)\n" +
            "\t{\n" +
            "\t\thighPass = hardLight(highPass);\n" +
            "\t}\n" +
            "\tfloat luminance = dot(centralColor, W);\n" +
            "\tfloat alpha = pow(luminance, params.r);\n" +
            "\n" +
            "\tvec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;\n" +
            "\t\n" +
            "\tsmoothColor.r = clamp(pow(smoothColor.r, params.g),0.0,1.0);\n" +
            "\tsmoothColor.g = clamp(pow(smoothColor.g, params.g),0.0,1.0);\n" +
            "\tsmoothColor.b = clamp(pow(smoothColor.b, params.g),0.0,1.0);\n" +
            "\t\n" +
            "\tvec3 screen = vec3(1.0) - (vec3(1.0)-smoothColor) * (vec3(1.0)-centralColor);\n" +
            "\tvec3 lighten = max(smoothColor, centralColor);\n" +
            "\tvec3 softLight = 2.0 * centralColor*smoothColor + centralColor*centralColor\n" +
            "\t\t\t\t\t- 2.0 * centralColor*centralColor * smoothColor;\n" +
            "\t\n" +
            "\tgl_FragColor = vec4(mix(centralColor, screen, alpha), 1.0);\n" +
            "\tgl_FragColor.rgb = mix(gl_FragColor.rgb, lighten, alpha);\n" +
            "\tgl_FragColor.rgb = mix(gl_FragColor.rgb, softLight, params.b);\n" +
            "\t\n" +
            "\tvec3 satColor = gl_FragColor.rgb * saturateMatrix;\n" +
            "\tvec3 smooth = mix(gl_FragColor.rgb, satColor, params.a);\n" +
            "\tsmooth.r = log(1.0 + 0.2 * smooth.r)/log(1.2);\n" +
            "\tsmooth.g = log(1.0 + 0.2 * smooth.g)/log(1.2);\n" +
            "\tsmooth.b = log(1.0 + 0.2 * smooth.b)/log(1.2);\n" +
            "\tgl_FragColor.rgb = smooth;\n" +
            "}";

    private int mSingleStepOffsetLocation;
    private int mParamsLocation;
    private int mBeautyLevel;
    private float[] mParamsData = new float[4];

    public GTVBeautyFilter(int level) {
        super(NO_FILTER_VERTEX_SHADER, GTVBEAUTY_FRAGMENT_SHADER);
        mBeautyLevel = level;
        resetBeautyParams();
    }

    protected void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mParamsLocation = GLES20.glGetUniformLocation(getProgram(), "params");
        updateBeautyParam();
        //setBeautyLevel(mBeautyLevel);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void setTexelSize(final float w, final float h) {
        setFloatVec2(mSingleStepOffsetLocation, new float[] {2.0f / w, 2.0f / h});
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        setTexelSize(width, height);
    }
    // 磨皮，曝光，美白，饱和
    public float[] locateBeautyRange(int index) {

        float start, end;

        if( index == 0 ) {
            // 1.0 -> 0.1
            start = 1.0f;
            end = 0.0f;//0.1f;
        }
        else if( index == 1 ) {
            // 1.0 -> 0.1
            start = 1.0f;
            end = 0.1f;
        }
        else if( index == 2 ) {
            // 0.0 -> 0.8
            start = 0.89f;
            end = 0.55f;//0.0f;
        }
        else if( index == 3 ) {
            // 0.0 -> 0.8
            start = 0.0f;
            end = 0.8f;
        }
        else {
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
        mParamsData[1] = 0.55f;
        mParamsData[2] = 0.89f;
        mParamsData[3] = 0.29f;

        return getBeautyParam();
    }
    public float[] getBeautyParam() {

        float[] values = new float[4];

        for( int i=0; i<4; i++ ) {
            float[] range = locateBeautyRange(i);
            float start = range[0];
            float end = range[1];
            values[i] = (mParamsData[i]-start)/(end-start);
        }

        return values;
    }
    public void updateBeautyParam() {

        setFloatVec4(mParamsLocation, mParamsData);
    }
    public void setBeautyParam(int index, float percent) {

        if( percent < 0.0f || percent > 1.0f ) {
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

//    public void setBeautyLevel(int level){
//        switch (level) {
//            case 1:
//                setFloatVec4(mParamsLocation, new float[] {1.0f, 1.0f, 0.15f, 0.15f});
//                break;
//            case 2:
//                setFloatVec4(mParamsLocation, new float[] {0.8f, 0.9f, 0.2f, 0.2f});
//                break;
//            case 3:
//                setFloatVec4(mParamsLocation, new float[] {0.6f, 0.8f, 0.25f, 0.25f});
//                break;
//            case 4:
//                setFloatVec4(mParamsLocation, new float[] {0.4f, 0.7f, 0.38f, 0.3f});
//                break;
//            case 5:
//                setFloatVec4(mParamsLocation, new float[] {0.33f, 0.63f, 0.4f, 0.35f});
//                break;
//            default:
//                break;
//        }
//    }
}
