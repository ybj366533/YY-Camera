package com.ybj366533.gtvimage.gtvfilter.filter.surpprise;

import android.opengl.GLES20;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

import java.nio.FloatBuffer;

/**
 * Created by gtv on 2017/9/26.
 */

public class GTVImageCrosshatchFilter extends GTVImageFilter {

    public static final String CROSSHATCH_VERTEX_SHADER = "" +
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

    public static final String CROSSHATCH_FRAGMENT_SHADER = "precision highp float;\n" +
            "\n" +
            "uniform vec3                iResolution;\n" +
            "uniform float               iGlobalTime;\n" +
            "uniform sampler2D           inputImageTexture;\n" +
            "varying vec2                textureCoordinate;\n" +
            "\n" +
            "// The brightnesses at which different hatch lines appear\n" +
            "float hatch_1 = 0.8;\n" +
            "float hatch_2 = 0.6;\n" +
            "float hatch_3 = 0.3;\n" +
            "float hatch_4 = 0.15;\n" +
            "\n" +
            "// How close together hatch lines should be placed\n" +
            "float density = 10.0;\n" +
            "\n" +
            "// How wide hatch lines are drawn.\n" +
            "float width = 1.0;\n" +
            "\n" +
            "// enable GREY_HATCHES for greyscale hatch lines\n" +
            "#define GREY_HATCHES\n" +
            "\n" +
            "// enable COLOUR_HATCHES for coloured hatch lines\n" +
            "#define COLOUR_HATCHES\n" +
            "\n" +
            "#ifdef GREY_HATCHES\n" +
            "float hatch_1_brightness = 0.8;\n" +
            "float hatch_2_brightness = 0.6;\n" +
            "float hatch_3_brightness = 0.3;\n" +
            "float hatch_4_brightness = 0.0;\n" +
            "#else\n" +
            "float hatch_1_brightness = 0.0;\n" +
            "float hatch_2_brightness = 0.0;\n" +
            "float hatch_3_brightness = 0.0;\n" +
            "float hatch_4_brightness = 0.0;\n" +
            "#endif\n" +
            "\n" +
            "float d = 1.0; // kernel offset\n" +
            "\n" +
            "float lookup(vec2 p, float dx, float dy)\n" +
            "{\n" +
            "    vec2 uv = (p.xy + vec2(dx * d, dy * d)) / iResolution.xy;\n" +
            "    vec4 c = texture2D(inputImageTexture, uv.xy);\n" +
            "\n" +
            "\t// return as luma\n" +
            "    return 0.2126*c.r + 0.7152*c.g + 0.0722*c.b;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void mainImage( out vec4 fragColor, in vec2 fragCoord )\n" +
            "{\n" +
            "\t//\n" +
            "\t// Inspired by the technique illustrated at\n" +
            "\t// http://www.geeks3d.com/20110219/shader-library-crosshatching-glsl-filter/\n" +
            "\t//\n" +
            "\tfloat ratio = iResolution.y / iResolution.x;\n" +
            "\tfloat coordX = fragCoord.x / iResolution.x;\n" +
            "\tfloat coordY = fragCoord.y / iResolution.x;\n" +
            "\tvec2 dstCoord = vec2(coordX, coordY);\n" +
            "\tvec2 srcCoord = vec2(coordX, coordY / ratio);\n" +
            "\tvec2 uv = srcCoord.xy;\n" +
            "\n" +
            "\tvec3 res = vec3(1.0, 1.0, 1.0);\n" +
            "    vec4 tex = texture2D(inputImageTexture, uv);\n" +
            "    float brightness = (0.2126*tex.x) + (0.7152*tex.y) + (0.0722*tex.z);\n" +
            "#ifdef COLOUR_HATCHES\n" +
            "\t// check whether we have enough of a hue to warrant coloring our\n" +
            "\t// hatch strokes.  If not, just use greyscale for our hatch color.\n" +
            "\tfloat dimmestChannel = min( min( tex.r, tex.g ), tex.b );\n" +
            "\tfloat brightestChannel = max( max( tex.r, tex.g ), tex.b );\n" +
            "\tfloat delta = brightestChannel - dimmestChannel;\n" +
            "\tif ( delta > 0.1 )\n" +
            "\t\ttex = tex * ( 1.0 / brightestChannel );\n" +
            "\telse\n" +
            "\t\ttex.rgb = vec3(1.0,1.0,1.0);\n" +
            "#endif // COLOUR_HATCHES\n" +
            "\n" +
            "    if (brightness < hatch_1)\n" +
            "    {\n" +
            "\t\tif (mod(fragCoord.x + fragCoord.y, density) <= width)\n" +
            "\t\t{\n" +
            "#ifdef COLOUR_HATCHES\n" +
            "\t\t\tres = vec3(tex.rgb * hatch_1_brightness);\n" +
            "#else\n" +
            "\t\t\tres = vec3(hatch_1_brightness);\n" +
            "#endif\n" +
            "\t\t}\n" +
            "    }\n" +
            "\n" +
            "    if (brightness < hatch_2)\n" +
            "    {\n" +
            "\t\tif (mod(fragCoord.x - fragCoord.y, density) <= width)\n" +
            "\t\t{\n" +
            "#ifdef COLOUR_HATCHES\n" +
            "\t\t\tres = vec3(tex.rgb * hatch_2_brightness);\n" +
            "#else\n" +
            "\t\t\tres = vec3(hatch_2_brightness);\n" +
            "#endif\n" +
            "\t\t}\n" +
            "    }\n" +
            "\n" +
            "    if (brightness < hatch_3)\n" +
            "    {\n" +
            "\t\tif (mod(fragCoord.x + fragCoord.y - (density*0.5), density) <= width)\n" +
            "\t\t{\n" +
            "#ifdef COLOUR_HATCHES\n" +
            "\t\t\tres = vec3(tex.rgb * hatch_3_brightness);\n" +
            "#else\n" +
            "\t\t\tres = vec3(hatch_3_brightness);\n" +
            "#endif\n" +
            "\t\t}\n" +
            "    }\n" +
            "\n" +
            "    if (brightness < hatch_4)\n" +
            "    {\n" +
            "\t\tif (mod(fragCoord.x - fragCoord.y - (density*0.5), density) <= width)\n" +
            "\t\t{\n" +
            "#ifdef COLOUR_HATCHES\n" +
            "\t\t\tres = vec3(tex.rgb * hatch_4_brightness);\n" +
            "#else\n" +
            "\t\t\tres = vec3(hatch_4_brightness);\n" +
            "#endif\n" +
            "\t\t}\n" +
            "    }\n" +
            "\n" +
            "\tvec2 p = fragCoord.xy;\n" +
            "\n" +
            "\t// simple sobel edge detection,\n" +
            "\t// borrowed and tweaked from jmk's \"edge glow\" filter, here:\n" +
            "\t// https://www.shadertoy.com/view/Mdf3zr\n" +
            "    float gx = 0.0;\n" +
            "    gx += -1.0 * lookup(p, -1.0, -1.0);\n" +
            "    gx += -2.0 * lookup(p, -1.0,  0.0);\n" +
            "    gx += -1.0 * lookup(p, -1.0,  1.0);\n" +
            "    gx +=  1.0 * lookup(p,  1.0, -1.0);\n" +
            "    gx +=  2.0 * lookup(p,  1.0,  0.0);\n" +
            "    gx +=  1.0 * lookup(p,  1.0,  1.0);\n" +
            "\n" +
            "    float gy = 0.0;\n" +
            "    gy += -1.0 * lookup(p, -1.0, -1.0);\n" +
            "    gy += -2.0 * lookup(p,  0.0, -1.0);\n" +
            "    gy += -1.0 * lookup(p,  1.0, -1.0);\n" +
            "    gy +=  1.0 * lookup(p, -1.0,  1.0);\n" +
            "    gy +=  2.0 * lookup(p,  0.0,  1.0);\n" +
            "    gy +=  1.0 * lookup(p,  1.0,  1.0);\n" +
            "\n" +
            "\t// hack: use g^2 to conceal noise in the video\n" +
            "    float g = gx*gx + gy*gy;\n" +
            "\tres *= (1.0-g);\n" +
            "\n" +
            "\tfragColor = vec4(res, 1.0);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "\tmainImage(gl_FragColor, textureCoordinate * iResolution.xy);\n" +
            "}";

    public GTVImageCrosshatchFilter() {
        super(CROSSHATCH_VERTEX_SHADER, CROSSHATCH_FRAGMENT_SHADER);
    }

    protected void onDrawArraysPre() {

        int[] iResolution = null;

        if( mIntputWidth > 0 && mIntputHeight > 0 ) {
            iResolution = new int[]{mIntputWidth, mIntputHeight};
        }
        else {
            iResolution = new int[]{480, 640};
        }

        int iResolutionLocation = GLES20.glGetUniformLocation(mGLProgId, "iResolution");
        GLES20.glUniform3fv(iResolutionLocation, 1,
                FloatBuffer.wrap(new float[]{(float) iResolution[0], (float) iResolution[1], 1.0f}));
    }

}
