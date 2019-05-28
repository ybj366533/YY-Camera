package com.ybj366533.videolib.impl.camera;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;

import com.ybj366533.videolib.recorder.IVideoRecorder;
import com.ybj366533.videolib.impl.camera.utils.CameraUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ybj366533.videolib.utils.LogUtils;

public class CameraEngine {
    private static final String TAG = "CamEng";
    private static Camera camera = null;
    public static int cameraID = 0;
    public static Activity cameraOwnerActivity = null;

    public static Camera getCamera(){
        return camera;
    }

    public static boolean openCamera(Activity activity, int width, int height){
        if(camera == null){
            try{
                camera = Camera.open(cameraID);
                setDefaultParameters(activity, width, height);
                return true;
            }catch(RuntimeException e){
                return false;
            }
        }
        return false;
    }

    public static boolean openCamera(Activity activity){
        LogUtils.LOGE(TAG, "open camera " + cameraID);
        synchronized (CameraEngine.class) {
            // 如果别人打开了摄像头，则关闭
            if(camera != null && cameraOwnerActivity != activity) {
                LogUtils.LOGE(TAG, "close the camarea opened by others ");
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            if(camera == null){
                try{
                    camera = Camera.open(cameraID);
                    cameraOwnerActivity = activity;
                    Size previewSize = CameraUtils.getSuitablePreviewSize(camera);
                    if( previewSize != null ) {
                        setDefaultParameters(activity, previewSize.width, previewSize.height);
                    }
                    return true;
                }catch(RuntimeException e){
                    return false;
                }
            }
            return false;
        }

    }

    public static void releaseCamera(Activity activity){
        LogUtils.LOGE(TAG, "release camera " + cameraID);
        synchronized (CameraEngine.class){
            // 试图关闭别的activity打开的摄像头
            if(cameraOwnerActivity == null || cameraOwnerActivity != activity) {
                LogUtils.LOGE(TAG, "try to close the cam belong to others ");
                return;
            }
            try {

                if(camera != null){
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    cameraOwnerActivity = null;
                }
            }
            catch (Exception ex) {
                LogUtils.LOGE("YYREC", "releaseCamera err " + ex.getMessage());
            }
        }

    }

    public void resumeCamera(Activity activity){
        openCamera(activity);
    }

    public void setParameters(Parameters parameters){
        camera.setParameters(parameters);
    }

    public Parameters getParameters(){
        if(camera != null)
            camera.getParameters();
        return null;
    }

    private static void setDefaultParameters(Activity activity, int width, int height){
        Parameters parameters = camera.getParameters();
        if( parameters == null )
            return;
        if (parameters.getSupportedFocusModes().contains(
                Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            // FOCUS_MODE_AUTO,FOCUS_MODE_CONTINUOUS_PICTURE
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        if (parameters.isVideoStabilizationSupported() ) {
            parameters.setVideoStabilization(true);
        }

        parameters.setPreviewSize(width, height);
        if( cameraID != 0 ) {
//            parameters.set("iso", "400");
//            int maxEV = parameters.getMaxExposureCompensation();
//            if( maxEV > 1 ) {
//                parameters.setExposureCompensation(maxEV-1);
//            }
            if( parameters.isAutoExposureLockSupported() ) {
                parameters.setAutoExposureLock(false);
            }
            if( parameters.isAutoWhiteBalanceLockSupported() ) {
                parameters.setAutoWhiteBalanceLock(false);
            }
        }
        List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
        int maxRangeIdx = -1;
        int currRangeDistance = 0;
        for( int i=0; i<fpsList.size(); i++ ) {
            int[] range = fpsList.get(i);
            if( range[1] - range[0] > currRangeDistance ) {
                maxRangeIdx = i;
                currRangeDistance = range[1] - range[0]; //todo fps会太高吗？
            }
        }
        if( maxRangeIdx >= 0 && maxRangeIdx <= fpsList.size()-1 ) {
            int[] range = fpsList.get(maxRangeIdx);
            parameters.setPreviewFpsRange(range[0], range[1]);
        }

        //Size pictureSize = CameraUtils.getLargePictureSize(camera);
        //parameters.setPictureSize(pictureSize.width, pictureSize.height);
        //parameters.setPictureSize(previewSize.width, previewSize.height);
        //parameters.setRotation(90);
        //camera.setDisplayOrientation(90);
        setCameraDisplayOrientation(activity);
        camera.setParameters(parameters);
    }

    private static Size getPreviewSize(){

        Parameters parameters = camera.getParameters();
        if( parameters != null ) {
            return parameters.getPreviewSize();
        }

        LogUtils.LOGE("YYREC", "error:try getPreviewSize but parameters is null. ");
        Size siz = CameraUtils.getSuitablePreviewSize(camera);

        return siz;
    }

    private static Size getPictureSize(){
        if( camera == null ) {
            return null;
        }

        if( camera.getParameters() == null )
            return camera.new Size(640, 480);

        return camera.getParameters().getPictureSize();
    }

    public static void setCameraDisplayOrientation(Activity activity) {

        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraID, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public static void startPreview(SurfaceTexture surfaceTexture, Camera.PreviewCallback previewCallback){
        synchronized (CameraEngine.class) {
            if(camera != null)
                try {
                    camera.setPreviewCallback(previewCallback);
                    camera.setPreviewTexture(surfaceTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public static void startPreview(SurfaceTexture surfaceTexture){
        synchronized (CameraEngine.class) {
            if(camera != null)
                try {
                    camera.setPreviewTexture(surfaceTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public static void startPreview(){
        if(camera != null)
            camera.startPreview();
    }

    public static void stopPreview(){
        camera.stopPreview();
    }

    public static void setRotation(int rotation){
        Parameters params = camera.getParameters();
        params.setRotation(rotation);
        camera.setParameters(params);
    }

    public static void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                                   Camera.PictureCallback jpegCallback){
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public static com.ybj366533.videolib.impl.camera.utils.CameraInfo getCameraInfo(){
        com.ybj366533.videolib.impl.camera.utils.CameraInfo info = new com.ybj366533.videolib.impl.camera.utils.CameraInfo();
        Size size = getPreviewSize();
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);
        info.previewWidth = size.width;
        info.previewHeight = size.height;
        info.orientation = cameraInfo.orientation;
        info.isFront = cameraID == 1 ? true : false;
        size = getPictureSize();
        if( size != null ) {
            info.pictureWidth = size.width;
            info.pictureHeight = size.height;
        }
        else {
            info.pictureWidth = info.previewWidth;
            info.pictureHeight = info.previewHeight;
        }
        return info;
    }

    public static int getCameraNumbers() {

        return Camera.getNumberOfCameras();
    }

    public static boolean supportLightOn() {
        synchronized (CameraEngine.class) {
            if (camera == null) {
                return false;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return false;
            }
            List<String> flashModes = parameters.getSupportedFlashModes();
            // Check if camera flash exists
            if (flashModes == null) {
                // Use the screen as a flashlight (next best thing)
                return false;
            }

            if( flashModes.contains(Parameters.FLASH_MODE_TORCH) ) {
                return true;
            }
        }


        return false;
    }

    public static boolean isLightOn() {
        synchronized (CameraEngine.class) {
            if (camera == null) {
                return false;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return false;
            }
            List<String> flashModes = parameters.getSupportedFlashModes();
            // Check if camera flash exists
            if (flashModes == null) {
                // Use the screen as a flashlight (next best thing)
                return false;
            }

            String flashMode = parameters.getFlashMode();
            if (Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                return true;
            }
        }

        return false;
    }

    public static void turnLightOn() {
        synchronized (CameraEngine.class) {
            if (camera == null) {
                return;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }
            List<String> flashModes = parameters.getSupportedFlashModes();
            // Check if camera flash exists
            if (flashModes == null) {
                // Use the screen as a flashlight (next best thing)
                return;
            }

            String flashMode = parameters.getFlashMode();
            if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                // Turn on the flash
                if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                }
            }
        }


    }

    public static void turnLightOff() {
        synchronized (CameraEngine.class){
            if (camera == null) {
                return;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }

            List<String> flashModes = parameters.getSupportedFlashModes();
            String flashMode = parameters.getFlashMode();
            // Check if camera flash exists
            if (flashModes == null) {
                return;
            }
            if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                // Turn off the flash
                if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                }
            }
        }

    }

    // 画面放大功能
    // 检查是否支持画面放大功能
    public static boolean isZoomSupported() {
        synchronized (CameraEngine.class){
            if (camera == null) {
                return false;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return false;
            }

            return parameters.isZoomSupported();
        }

    }

    // 设定扩大倍数
    // 0-100： 0没有扩大，100：扩大到最大
    public static void setZoom(int zoomRatio) {

        synchronized (CameraEngine.class){
            if (camera == null) {
                return;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }


            parameters.setZoom(parameters.getMaxZoom() * zoomRatio / 100);

            camera.setParameters(parameters);
            return;
        }

    }

    public static int getZoom() {
        synchronized (CameraEngine.class){
            if (camera == null) {
                return 0;
            }

            Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return 0;
            }

            return (parameters.getZoom() * 100 / parameters.getMaxZoom());
        }

    }

    // 触摸对焦

    private static int viewXToCameraX(float viewX, int viewWidth)
    {
        return (int)(2000.0F * viewX / viewWidth) - 1000;
    }

    private static int viewYToCameraY(float viewY, int viewHeight)
    {
        return (int)(2000.0F * viewY / viewHeight) - 1000;
    }

    private static void limitRect(Rect paramRect)
    {
        if (paramRect.left < -1000) {
            paramRect.left = -1000;
        }
        if (paramRect.top < -1000) {
            paramRect.top = -1000;
        }
        if (1000 < paramRect.right) {
            paramRect.right = 1000;
        }
        if (1000 < paramRect.bottom) {
            paramRect.bottom = 1000;
        }
    }

    private static void dumpRect(Rect paramRect)
    {
        Log.i("MediaCamera", "rect left: " + paramRect.left);
        Log.i("MediaCamera", "rect top: " + paramRect.top);
        Log.i("MediaCamera", "rect right: " + paramRect.right);
        Log.i("MediaCamera", "rect bottom: " + paramRect.bottom);
    }

    public static void focusOnTouch(float x, float y, int width, int height, final IVideoRecorder.OnFocusListener listener) {
        synchronized (CameraEngine.class){
            if (camera == null) {
                return;
            }

            Parameters localParameters = camera.getParameters();

            try{
                if (localParameters.getMaxNumFocusAreas() > 0)
                {
                    camera.cancelAutoFocus();
                    ArrayList localArrayList = new ArrayList();

                    int x1 = viewXToCameraX(x,width);
                    int y1 = viewYToCameraY(y,height);

                    Rect localRect = new Rect(x1 - 100, y1 - 100, x1 + 100, y1 + 100);
                    limitRect(localRect);
                    localArrayList.add(new Camera.Area(localRect, 1000));
                    localParameters.setFocusMode("auto");
                    localParameters.setFocusAreas(localArrayList);
                    //localParameters.setMeteringAreas(localArrayList);
                    camera.setParameters(localParameters);
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.i("MediaCamera", "focus: success " + success);
                            if (listener != null) {
                                if (success == true) {
                                    listener.onFocusSucess();
                                } else {
                                    listener.onFocusFail();
                                }
                            }
                        }
                    });
                    dumpRect(localRect);
                }
            }catch (Exception ex) {
                LogUtils.LOGE("YYREC", "focusOnTouch err " + ex.getMessage());
            }
        }


        return;
    }
}