package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

/**
 * Created by gtv on 2018/4/26.
 */

public class GTVImageHeZeFilter extends GTVImageFilter {

    public static final String HEZE_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " \n" +
            " uniform lowp float hazeDistance;\n" +
            " uniform highp float slope;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "\t//todo reconsider precision modifiers\t \n" +
            "\t highp vec4 color = vec4(1.0);//todo reimplement as a parameter\n" +
            "\t \n" +
            "\t highp float  d = textureCoordinate.y * slope  +  hazeDistance;\n" +
            "\t \n" +
            "\t highp vec4 c = texture2D(inputImageTexture, textureCoordinate) ; // consider using unpremultiply\n" +
            "\t \n" +
            "\t c = (c - d * color) / (1.0 -d);\n" +
            "\t \n" +
            "\t gl_FragColor = c; //consider using premultiply(c);\n" +
            " }";

    private int distanceUniform;
    private int slopeUniform;

    private float distance = 0.2f;
    private float slope = 0.0f;

    public GTVImageHeZeFilter() {
        super(NO_FILTER_VERTEX_SHADER, HEZE_FRAGMENT_SHADER);
    }

    public void onInit() {
        super.onInit();

        distanceUniform = GLES20.glGetUniformLocation(getProgram(), "hazeDistance");
        slopeUniform = GLES20.glGetUniformLocation(getProgram(), "slope");

        this.setDistance(0.2f);
        this.setSlope(0.0f);
    }
    public void setDistance(float distance) {
        this.distance = distance;
        setFloat(distanceUniform, this.distance);
    }

    public void setSlope(float slope) {
        this.slope = slope;
        setFloat(slopeUniform, this.slope);
    }

    @Override
    protected void onDrawArraysPre() {

//        setFloat(distanceUniform, this.distance);
//        setFloat(slopeUniform, this.slope);
    }
}
