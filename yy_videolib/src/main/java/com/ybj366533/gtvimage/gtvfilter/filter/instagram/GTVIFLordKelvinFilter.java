package com.ybj366533.gtvimage.gtvfilter.filter.instagram;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.ybj366533.videolib.videoplayer.R;

public class GTVIFLordKelvinFilter extends GTVInstaFilter {

    public static final String SHADER = "precision lowp float;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " void main()\n" +
            " {\n" +
            "     vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     vec3 origin = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     vec2 lookup;\n" +
            "     lookup.y = .5;\n" +
            "     lookup.x = texel.r;\n" +
            "     texel.r = texture2D(inputImageTexture2, lookup).r;\n" +
            "     lookup.x = texel.g;\n" +
            "     texel.g = texture2D(inputImageTexture2, lookup).g;\n" +
            "     lookup.x = texel.b;\n" +
            "     texel.b = texture2D(inputImageTexture2, lookup).b;\n" +
            //"     gl_FragColor = vec4(texel, 1.0);\n" +
            "     gl_FragColor = vec4(mix(texel, origin, 0.5), 1.0);\n" +
            " }";

    public GTVIFLordKelvinFilter(Context context) {
        super(SHADER, 1);
        bitmaps[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.kelvin_map);
    }

}
