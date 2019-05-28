package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

/**
 * Created by gtv on 2017/9/26.
 */

public class GTVImageBlueorangeFilter extends GTVImageFilter {

    public static final String BLUE_ORANGE_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";

    public static final String BLUE_ORANGE_FRAGMENT_SHADER = "precision mediump float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform float               iGlobalTime;\n" +
            "uniform sampler2D           inputImageTexture;\n" +
            "varying vec2                textureCoordinate;\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "{\n" +
            "    vec2 uv = fragCoord.xy;\n" +
            "\t\n" +
            "\tvec3 tex = texture2D( inputImageTexture, uv ).rgb;\n" +
            "\tfloat shade = dot(tex, vec3(0.333333));\n" +
            "\n" +
            "\tvec3 col = mix(vec3(0.1, 0.36, 0.8) * (1.0-2.0*abs(shade-0.5)), vec3(1.06, 0.8, 0.55), 1.0-shade);\n" +
            "\t\n" +
            "    fragColor = vec4(col,1.0);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "\tmainImage(gl_FragColor, textureCoordinate);\n" +
            "}";

    public GTVImageBlueorangeFilter() {
        super(BLUE_ORANGE_VERTEX_SHADER, BLUE_ORANGE_FRAGMENT_SHADER);
    }

    protected void onDrawArraysPre() {

    }

}
