package com.gtv.gtvimage.gtvfilter.filter.instagram;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.gtv.cloud.videoplayer.R;

public class GTVIFWaldenFilter extends GTVInstaFilter {

    public static final String SHADER = "precision lowp float;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2; //map\n" +
            " uniform sampler2D inputImageTexture3; //vigMap\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     \n" +
            "     vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     \n" +
            "     texel = vec3(\n" +
            "                  texture2D(inputImageTexture2, vec2(texel.r, .16666)).r,\n" +
            "                  texture2D(inputImageTexture2, vec2(texel.g, .5)).g,\n" +
            "                  texture2D(inputImageTexture2, vec2(texel.b, .83333)).b);\n" +
            "     \n" +
            "     vec2 tc = (2.0 * textureCoordinate) - 1.0;\n" +
            "     float d = dot(tc, tc);\n" +
            "     vec2 lookup = vec2(d, texel.r);\n" +
            "     texel.r = texture2D(inputImageTexture3, lookup).r;\n" +
            "     lookup.y = texel.g;\n" +
            "     texel.g = texture2D(inputImageTexture3, lookup).g;\n" +
            "     lookup.y = texel.b;\n" +
            "     texel.b    = texture2D(inputImageTexture3, lookup).b;\n" +
            "     \n" +
            "     gl_FragColor = vec4(texel, 1.0);\n" +
            " }";

    public GTVIFWaldenFilter(Context context) {
        super(SHADER, 2);
        bitmaps[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.walden_map);
        bitmaps[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.vignette_map);
    }

}
