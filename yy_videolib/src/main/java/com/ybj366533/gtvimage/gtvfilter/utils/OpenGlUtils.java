package com.ybj366533.gtvimage.gtvfilter.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class OpenGlUtils {
	public static final int NO_TEXTURE = -1;
	public static final int NOT_INIT = -1;	
	public static final int ON_DRAWN = 1;
	public static float glVer = 2.0f;

	public static boolean supportGL3() {

		// 暂不用pbo
//		if( glVer >= 3.0f )
//			return true;

		return false;
	}

	public static void setGlVer(Context ctx) {

		try {

			ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
			ConfigurationInfo info = am.getDeviceConfigurationInfo();
			String v = info.getGlEsVersion(); //判断是否为3.0 ，一般4.4就开始支持3.0版本了。

			Float f = new Float(v);
			if (f >= 3.0f ) {
				Log.e("GTVREC", "glesver support 3.0 " + v);
			} else {
				Log.e("GTVREC", "glesver support 2.0 " + v);
			}
			glVer = f;
		}
		catch (Exception ex) {

		}
	}

	public static int loadTexture(final Bitmap img, final int usedTexId) {
		return loadTexture(img, usedTexId, false);
    }
	
	public static int loadTexture(final Bitmap img, final int usedTexId, boolean recyled) {
		if(img == null)
			return NO_TEXTURE; 
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if(recyled)
        	img.recycle();
        return textures[0];
    }
	
	public static int loadTexture(final Buffer data, final int width,final int height, final int usedTexId) {
		if(data == null)
			return NO_TEXTURE;
	    int textures[] = new int[1];
	    if (usedTexId == NO_TEXTURE) {
	        GLES20.glGenTextures(1, textures, 0);
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
	                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
	    } else {
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
	        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
	                height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data);
	        textures[0] = usedTexId;
	    }
	    return textures[0];
    }
    
	public static int loadTexture(final Buffer data, final int width,final int height, final int usedTexId,final int type) {
		if(data == null)
			return NO_TEXTURE;
	    int textures[] = new int[1];
	    if (usedTexId == NO_TEXTURE) {
	        GLES20.glGenTextures(1, textures, 0);
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
	                0, GLES20.GL_RGBA, type, data);
	    } else {
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
	        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
	                height, GLES20.GL_RGBA, type, data);
	        textures[0] = usedTexId;
	    }
	    return textures[0];
    }
    
    public static int loadTexture(final Context context, final String name){
		final int[] textureHandle = new int[1];
		
		GLES20.glGenTextures(1, textureHandle, 0);
		
		if (textureHandle[0] != 0){

			// Read in the resource
			final Bitmap bitmap = getImageFromAssetsFile(context,name);
						
			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
			
			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			
			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();						
		}
		
		if (textureHandle[0] == 0){
			throw new RuntimeException("Error loading texture.");
		}
		
		return textureHandle[0];
	}
	
	private static Bitmap getImageFromAssetsFile(Context context,String fileName){  
		Bitmap image = null;  
	    AssetManager am = context.getResources().getAssets();
	    try{  
			InputStream is = am.open(fileName);
			image = BitmapFactory.decodeStream(is);
			is.close();
          	}catch (IOException e){  
	          e.printStackTrace();  
	      }  	  
	      return image;  	  
	}  
    
	public static int loadProgram(final String strVSource, final String strFSource) {
        int iVShader;
        int iFShader;
        int iProgId;
        int[] link = new int[1];
        iVShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
        if (iVShader == 0) {
            Log.d("Load Program", "Vertex Shader Failed");
            return 0;
        }
        iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER);
        if (iFShader == 0) {
            Log.d("Load Program", "Fragment Shader Failed");
            return 0;
        }

        iProgId = GLES20.glCreateProgram();
        GLES20.glAttachShader(iProgId, iVShader);
        GLES20.glAttachShader(iProgId, iFShader);
        GLES20.glLinkProgram(iProgId);
        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            Log.d("Load Program", "Linking Failed");
            return 0;
        }
        GLES20.glDeleteShader(iVShader);
        GLES20.glDeleteShader(iFShader);
        return iProgId;
    }
	
	private static int loadShader(final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }
	
	public static int getExternalOESTextureID(){		
		int[] texture = new int[1];
		GLES20.glGenTextures(1, texture, 0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
		GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);        
		GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		return texture[0];
	}
	
//	public static String readShaderFromRawResource(final int resourceId){
//		final InputStream inputStream = MagicEngine.getContext().getResources().openRawResource(
//				resourceId);
//		final InputStreamReader inputStreamReader = new InputStreamReader(
//				inputStream);
//		final BufferedReader bufferedReader = new BufferedReader(
//				inputStreamReader);
//
//		String nextLine;
//		final StringBuilder body = new StringBuilder();
//
//		try{
//			while ((nextLine = bufferedReader.readLine()) != null){
//				body.append(nextLine);
//				body.append('\n');
//			}
//		}
//		catch (IOException e){
//			return null;
//		}
//		return body.toString();
//	}

	public static Bitmap textureToBitmapByFilter(int textureId, GTVImageFilter filter,
												 int displayWidth, int displayHeight){
		if(filter == null)
			return null;

		FloatBuffer cubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		cubeBuffer.put(TextureRotationUtil.CUBE).position(0);

		FloatBuffer textureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		textureBuffer.put(TextureRotationUtil.TEXTURE_FLIP_VERTICAL).position(0);

		int width = displayWidth;
		int height = displayHeight;

		int[] mFrameBuffers = new int[1];
		int[] mFrameBufferTextures = new int[1];

		GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
		GLES20.glGenTextures(1, mFrameBufferTextures, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
				GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
		GLES20.glViewport(0, 0, width, height);

		filter.onInputSizeChanged(width, height);
		filter.onDisplaySizeChanged(displayWidth, displayHeight);

		filter.onDrawFrame(textureId, cubeBuffer, textureBuffer);

		IntBuffer ib = IntBuffer.allocate(width * height);
		GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));

		GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
		GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
		filter.onInputSizeChanged(displayWidth, displayHeight);

		return result;
	}

	public static Bitmap drawToBitmapByFilter(Bitmap bitmap, GTVImageFilter filter,
            int displayWidth, int displayHeight){
		if(filter == null)
			return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] mFrameBuffers = new int[1];
        int[] mFrameBufferTextures = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
        GLES20.glViewport(0, 0, width, height);
        filter.onInputSizeChanged(width, height);
        filter.onDisplaySizeChanged(displayWidth, displayHeight);
        int textureId = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, true);
        filter.onDrawFrame(textureId);
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(IntBuffer.wrap(ib.array()));
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
        GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
        filter.onInputSizeChanged(displayWidth, displayHeight);
        return result;
	}

	/**
	 * Checks to see if a GLES error has been raised.
	 */
	public static void checkGlError(String op) {
		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e("OpenGlUtils", msg);
			throw new RuntimeException(msg);
		}
	}

	public static void remapCube(float x, float y, float width, float height, float cWidth, float cHeight, FloatBuffer cubeBuffer) {

		float left = (x/cWidth)*2.0f-1.0f;
		float top = ((cHeight-y)/cHeight)*2.0f-1.0f;
		float right = ((x+width)/cWidth)*2.0f-1.0f;
		float bottom = ((cHeight-y-height)/cHeight)*2.0f-1.0f;

		float vertex[] = {
				left, bottom,
				right, bottom,
				left, top,
				right, top,
		};

//		float vertex[] = {
//				-scaleWidth, -scaleHeight,
//				scaleWidth, -scaleHeight,
//				-scaleWidth, scaleHeight,
//				scaleWidth, scaleHeight,
//		};

		cubeBuffer.clear();
		cubeBuffer.put(vertex).position(0);

		return;
	}

	public static void fitCubeWithBar(float inputWidth, float inputHeight, float outputWidth, float outputHeight, FloatBuffer cubeBuffer) {

		float scaleWidth = 1.0f;
		float scaleHeight = 1.0f;

		if( outputWidth/outputHeight > inputWidth/inputHeight ) {
			// 输入比输出瘦，高度对齐
			scaleWidth = outputHeight * inputWidth / inputHeight / outputWidth;
		}
		else if( outputWidth/outputHeight < inputWidth/inputHeight ) {
			// 输入比输出胖，宽度对齐
			scaleHeight = outputWidth * inputHeight / inputWidth / outputHeight;
		}

		float vertex[] = {
				-scaleWidth, -scaleHeight,
				scaleWidth, -scaleHeight,
				-scaleWidth, scaleHeight,
				scaleWidth, scaleHeight,
		};

		cubeBuffer.clear();
		cubeBuffer.put(vertex).position(0);

		return;
	}

	public static void fitCube(float inputWidth, float inputHeight, float outputWidth, float outputHeight, FloatBuffer cubeBuffer) {

		float scaleWidth = 1.0f;
		float scaleHeight = 1.0f;

		if( outputWidth/outputHeight > inputWidth/inputHeight ) {
			// 输入比输出瘦，宽度对齐
			scaleHeight = outputWidth * inputHeight / inputWidth / outputHeight;
		}
		else if( outputWidth/outputHeight < inputWidth/inputHeight ) {
			// 输入比输出胖，高度对齐
			scaleWidth = outputHeight * inputWidth / inputHeight / outputWidth;
		}

		float vertex[] = {
				-scaleWidth, -scaleHeight,
				scaleWidth, -scaleHeight,
				-scaleWidth, scaleHeight,
				scaleWidth, scaleHeight,
		};


		cubeBuffer.clear();
		cubeBuffer.put(vertex).position(0);

		return;
	}

	public static void fitCubePlayMode(float inputWidth, float inputHeight, float outputWidth, float outputHeight, FloatBuffer cubeBuffer) {

		float scaleWidth = 1.0f;
		float scaleHeight = 1.0f;

		if( outputWidth/outputHeight < inputWidth/inputHeight ) {
			// 输入比输出瘦，宽度对齐
			scaleHeight = outputWidth * inputHeight / inputWidth / outputHeight;
		}
		else if( outputWidth/outputHeight > inputWidth/inputHeight ) {
			// 输入比输出胖，高度对齐
			scaleWidth = outputHeight * inputWidth / inputHeight / outputWidth;
		}

		float vertex[] = {
				-scaleWidth, -scaleHeight,
				scaleWidth, -scaleHeight,
				-scaleWidth, scaleHeight,
				scaleWidth, scaleHeight,
		};

		cubeBuffer.clear();
		cubeBuffer.put(vertex).position(0);

		return;
	}

	public static void fitCubeMagnify(FloatBuffer cubeBuffer, float ratio) {

		float scaleWidth = 1.0f * ratio;
		float scaleHeight = 1.0f * ratio;

		float vertex[] = {
				-scaleWidth, -scaleHeight,
				scaleWidth, -scaleHeight,
				-scaleWidth, scaleHeight,
				scaleWidth, scaleHeight,
		};


		cubeBuffer.clear();
		cubeBuffer.put(vertex).position(0);

		return;
	}
}
