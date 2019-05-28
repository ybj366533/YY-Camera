package com.ybj366533.videolib.impl.camera.utils;

import android.hardware.Camera;
import android.os.Build;

import java.util.List;

/**
 * Created by why8222 on 2016/2/25.
 */
public class CameraUtils {

    public static Camera.Size getLargePictureSize(Camera camera){
        if(camera != null ){
            if( camera.getParameters() == null ) {
                return camera.new Size(640, 480);
            }
            List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
            Camera.Size temp = sizes.get(0);
            for(int i = 1;i < sizes.size();i ++){
                float scale = (float)(sizes.get(i).height) / sizes.get(i).width;
                if(temp.width < sizes.get(i).width && scale < 0.6f && scale > 0.5f)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    public static Camera.Size getLargePreviewSize(Camera camera){
        if(camera != null){
            if( camera.getParameters() == null ) {
                return camera.new Size(640, 480);
            }
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
            Camera.Size temp = sizes.get(0);
            for(int i = 1;i < sizes.size();i ++){
                if(temp.width < sizes.get(i).width)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    public static Camera.Size getSuitablePreviewSize(Camera camera){
        // 优先用640x480
        // 其次用1280x720
        if(camera != null ){
            if( camera.getParameters() == null ) {
                return camera.new Size(640, 480);
            }
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();

            // preview size
            int width  = 1280;
            int height = 720;

            // if os < 6.0
            if(Build.VERSION.SDK_INT < 23){
                width = 960;
                height = 540;
            }

            for(int i = 0;i < sizes.size();i ++){
                if( sizes.get(i).width == width && sizes.get(i).height == height ) {
                    return sizes.get(i);
                }
            }

            for(int i = 0;i < sizes.size();i ++){
                if( sizes.get(i).width == width ) {
                    return sizes.get(i);
                }
            }

            for(int i = 0;i < sizes.size();i ++){
                if( sizes.get(i).width == 640 && sizes.get(i).height == 480 ) {
                    return sizes.get(i);
                }
            }


            return getLargePreviewSize(camera);
        }
        return null;
    }
}
