package com.ybj366533.gtvimage.gtvfilter.filter.lookupfilter;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.ybj366533.videolib.videoplayer.R;

/**
 * Created by ken on 2018/4/14.
 */

public class GTVRiXiRenXiangFilter extends GTVImageLookupFilter {

    public static final String RIXIRENXIANG_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2; // TODO: This is not used\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2; // lookup texture\n" +
            " \n" +
            " uniform lowp float intensity;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     \n" +
            "     highp float blueColor = textureColor.b * 63.0;\n" +
            "     \n" +
            "     highp vec2 quad1;\n" +
            "     quad1.y = floor(floor(blueColor) / 8.0);\n" +
            "     quad1.x = floor(blueColor) - (quad1.y * 8.0);\n" +
            "     \n" +
            "     highp vec2 quad2;\n" +
            "     quad2.y = floor(ceil(blueColor) / 8.0);\n" +
            "     quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n" +
            "     \n" +
            "     highp vec2 texPos1;\n" +
            "     texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "     texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "     \n" +
            "     highp vec2 texPos2;\n" +
            "     texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "     texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "     \n" +
            "     lowp vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n" +
            "     lowp vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n" +
            "     \n" +
            "     lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n" +
            "     newColor = vec4(newColor.r*0.975, newColor.g, newColor.b, newColor.w);\n" +
            "     gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);\n" +
            " }";

    private float whiten;
    private float max;

    public GTVRiXiRenXiangFilter(Context context) {
        super(0.101f, RIXIRENXIANG_FRAGMENT_SHADER);
        max = 0.151f;
        whiten = 0.1f;
        setBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.f_rixi_abaose));
    }

    public void setIntensity(final float intensity) {
        whiten = intensity;
        mIntensity = 0.1f + whiten * max;
        setFloat(mIntensityLocation, mIntensity);
    }
}
