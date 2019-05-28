package com.ybj366533.gtvimage.gtvfilter.filter.instagram;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.ybj366533.videolib.videoplayer.R;

public class GTVIFValenciaFilter extends GTVInstaFilter {

    public static final String SHADER = "precision lowp float;\n" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2; //map\n" +
            " uniform sampler2D inputImageTexture3; //gradMap\n" +
            " \n" +
            " mat3 saturateMatrix = mat3(\n" +
            "                            1.1402,\n" +
            "                            -0.0598,\n" +
            "                            -0.061,\n" +
            "                            -0.1174,\n" +
            "                            1.0826,\n" +
            "                            -0.1186,\n" +
            "                            -0.0228,\n" +
            "                            -0.0228,\n" +
            "                            1.1772);\n" +
            " \n" +
            " vec3 lumaCoeffs = vec3(.3, .59, .11);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     vec3 origin = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
            "     \n" +
            "     texel = vec3(\n" +
            "                  texture2D(inputImageTexture2, vec2(texel.r, .1666666)).r,\n" +
            "                  texture2D(inputImageTexture2, vec2(texel.g, .5)).g,\n" +
            "                  texture2D(inputImageTexture2, vec2(texel.b, .8333333)).b\n" +
            "                  );\n" +
            "     \n" +
            "     texel = saturateMatrix * texel;\n" +
            "     float luma = dot(lumaCoeffs, texel);\n" +
            "     texel = vec3(\n" +
            "                  texture2D(inputImageTexture3, vec2(luma, texel.r)).r,\n" +
            "                  texture2D(inputImageTexture3, vec2(luma, texel.g)).g,\n" +
            "                  texture2D(inputImageTexture3, vec2(luma, texel.b)).b);\n" +
            "     \n" +
            //"     gl_FragColor = vec4(texel, 1.0);\n" +
            "     gl_FragColor = vec4(mix(texel, origin, 0.5), 1.0);\n" +
            " }";

    public GTVIFValenciaFilter(Context context) {
        super(SHADER, 2);
        bitmaps[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.valencia_map);
        bitmaps[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.valencia_gradient_map);
    }

}
