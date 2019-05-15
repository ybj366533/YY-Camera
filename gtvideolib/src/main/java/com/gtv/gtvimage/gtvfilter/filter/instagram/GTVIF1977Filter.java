package com.gtv.gtvimage.gtvfilter.filter.instagram;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.gtv.cloud.videoplayer.R;


public class GTVIF1977Filter extends GTVInstaFilter {

    public static final String SHADER = "precision lowp float;\n" +
        " varying highp vec2 textureCoordinate;\n" +
        " \n" +
        " uniform sampler2D inputImageTexture;\n" +
        " uniform sampler2D inputImageTexture2;\n" +
        " \n" +
        " void main()\n" +
        " {\n" +
        "     \n" +
        "     vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     vec3 origin = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
        "     \n" +
        "     texel = vec3(\n" +
        "                  texture2D(inputImageTexture2, vec2(texel.r, .16666)).r,\n" +
        "                  texture2D(inputImageTexture2, vec2(texel.g, .5)).g,\n" +
        "                  texture2D(inputImageTexture2, vec2(texel.b, .83333)).b);\n" +
        "     \n" +
        //"     gl_FragColor = vec4(texel, 1.0);\n" +
            "     gl_FragColor = vec4(mix(texel, origin, 0.5), 1.0);\n" +
        " }";

    public GTVIF1977Filter(Context context) {
        super(SHADER, 2);
        bitmaps[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.map_1977);
        bitmaps[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.blowout_1977);
    }

}
