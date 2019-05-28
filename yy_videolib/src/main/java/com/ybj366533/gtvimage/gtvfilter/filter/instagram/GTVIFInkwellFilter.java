package com.ybj366533.gtvimage.gtvfilter.filter.instagram;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.ybj366533.videolib.videoplayer.R;

public class GTVIFInkwellFilter extends GTVInstaFilter {

    public static final String SHADER = "precision lowp float;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     vec3 origin = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     texel = vec3(dot(vec3(0.3, 0.6, 0.1), texel));\n" +
            "     texel = vec3(texture2D(inputImageTexture2, vec2(texel.r, .16666)).r);\n" +
            //"     gl_FragColor = vec4(texel, 1.0);\n" +
            "     gl_FragColor = vec4(mix(texel, origin, 0.5), 1.0);\n" +
            " }";

    public GTVIFInkwellFilter(Context context) {
        super(SHADER, 1);
        bitmaps[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.inkwell_map);
    }

}
