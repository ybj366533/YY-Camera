package com.ybj366533.yy_camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Camera 实现
 */
public class CameraEngine extends CameraImpl {
    private static final String TAG = "CameraEngine";
    private static final int INVALID_CAMERA_ID = -1;

    private Callback mCallback;//数据状态监听回调
    private SurfaceTexture surfaceTexture;//实现渲染容器
    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            PrintLog.d(TAG,"onPreviewFrame 开始 《--");
            if (mCallback != null) {
                mCallback.onPreviewFrame(data);
//                PrintLog.d(TAG,"onPreviewFrame 结束 --》 ");
            }

        }
    };
    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();

    static {
        FLASH_MODES.put(Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private int mCameraId;

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);

    Camera mCamera;

    private AutoFocusCallback mFocusCallback;

    private int mWidth = Constants.MAX_PREVIEW_WIDTH_1280;

    private int mHeight = Constants.MAX_PREVIEW_HEIGHT_720;

    private Camera.Parameters mCameraParameters;

    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private AspectRatio mAspectRatio;

    private boolean mShowingPreview;

    //camera1默认自动聚焦
    private boolean mAutoFocus = true;//默认自动聚焦

    private int mFacing;

    private int mFlash;

    private int mDisplayOrientation;


    CameraEngine(Context context) {
        super(context);
    }

    /**
     * 初始化
     *
     * @param callback
     * @param surfaceTexture
     */
    @Override
    void init(Callback callback, SurfaceTexture surfaceTexture) {
//        super.onInit(callback, surfaceTexture);
        this.surfaceTexture = surfaceTexture;
        this.mCallback = callback;
//        setUpPreview();
//        adjustCameraParameters();
    }


    @Override
    boolean start() {
        chooseCamera();
        openCamera();

        setUpPreview();
        mShowingPreview = true;
        mCamera.startPreview();
        return true;
    }

    @Override
    void stop() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mShowingPreview = false;
        releaseCamera();
    }

    @Override
    void display() {

    }

    // Suppresses Camera#setPreviewTexture
    void setUpPreview() {
        try {

            if (mCamera == null) {
                return;
            }
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.setPreviewCallback(mPreviewCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
//            start();
        }
    }

    @Override
    int getFacing() {
        return mFacing;
    }

    @Override
    Set<AspectRatio> getSupportedAspectRatios() {
        SizeMap idealAspectRatios = mPreviewSizes;
        for (AspectRatio aspectRatio : idealAspectRatios.ratios()) {
            if (mPictureSizes.sizes(aspectRatio) == null) {
                idealAspectRatios.remove(aspectRatio);
            }
        }
        return idealAspectRatios.ratios();
    }

    @Override
    int getOrientation() {
        if (mCameraInfo == null) {
            return 0;
        }
        return mCameraInfo.orientation;
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio) {
        if (mAspectRatio == null || !isCameraOpened()) {
            // Handle this later when camera is opened
            mAspectRatio = ratio;
            return true;
        } else if (!mAspectRatio.equals(ratio)) {
            final Set<Size> sizes = mPreviewSizes.sizes(ratio);
            if (sizes == null) {
                throw new UnsupportedOperationException(ratio + " is not supported");
            } else {
                mAspectRatio = ratio;
                adjustCameraParameters();

                return true;
            }
        }
        return false;
    }

    @Override
    AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        if (setAutoFocusInternal(autoFocus)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    boolean getAutoFocus() {
        if (!isCameraOpened()) {
            return mAutoFocus;
        }
        String focusMode = mCameraParameters.getFocusMode();
        return focusMode != null && focusMode.contains("continuous");
    }

    @Override
    void setFlash(int flash) {
        if (flash == mFlash) {
            return;
        }
        if (setFlashInternal(flash)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    int getFlash() {
        return mFlash;
    }

    @Override
    void takePicture() {
        if (!isCameraOpened()) {
            throw new IllegalStateException(
                    "Camera is not ready. Call start() before takePicture().");
        }
        if (getAutoFocus()) {
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePictureInternal();
                }
            });
        } else {
            takePictureInternal();
        }
    }

    void takePictureInternal() {
        if (!isPictureCaptureInProgress.getAndSet(true)) {
            mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isPictureCaptureInProgress.set(false);
                    if (mCallback != null) {
                        mCallback.onPreviewFrame(data);
                    }
                    camera.cancelAutoFocus();
                    camera.startPreview();
                }
            });
        }
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        if (mDisplayOrientation == displayOrientation) {
            return;
        }
        mDisplayOrientation = displayOrientation;
        if (isCameraOpened()) {

            mCameraParameters.setRotation(calcCameraRotation(displayOrientation));
            mCamera.setParameters(mCameraParameters);
            final boolean needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
            if (needsToStopPreview) {
                mCamera.stopPreview();
            }
            mCamera.setDisplayOrientation(calcDisplayOrientation(displayOrientation));
            if (needsToStopPreview) {
                mCamera.startPreview();
            }
        }
    }

    @Override
    void setSurfaceChanged() {

    }

    @Override
    void setBufferSize(int width, int height) {

    }

    @Override
    void
    setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
//        adjustCameraParameters();
    }

    /**
     * 手动聚焦
     *
     * @param point 触屏坐标
     */
    @Override
    boolean onFocus(Point point, AutoFocusCallback callback) {
        this.mFocusCallback = callback;
        if (mCamera == null) {
            return false;
        }
        try {
            Camera.Parameters localParameters = mCamera.getParameters();
            if (localParameters.getMaxNumFocusAreas() > 0) {
                mCamera.cancelAutoFocus();
                localParameters.setFocusMode("auto");

                ArrayList areas = new ArrayList();
                int x1 = viewXToCameraX(point.x, mWidth);
                int y1 = viewYToCameraY(point.y, mHeight);

                Rect localRect = new Rect(x1 - 100, y1 - 100, x1 + 100, y1 + 100);
                limitRect(localRect);
                areas.add(new Camera.Area(localRect, 1000));
                localParameters.setFocusAreas(areas);
                //localParameters.setMeteringAreas(localArrayList);
                mCamera.setParameters(localParameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (mFocusCallback != null) {
                            mFocusCallback.onAutoFocus(success);
                        }
                    }
                });
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    List<Integer> getSupporttedPreviewFormats() {
        if (mCamera != null) {
            return mCamera.getParameters().getSupportedPictureFormats();
        }
        return new ArrayList<>();
    }

    /**
     * 触碰区域越界判断
     *
     * @param paramRect
     */
    private static void limitRect(Rect paramRect) {
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

    // 触摸对焦
    private static int viewXToCameraX(float viewX, int viewWidth) {
        return (int) (2000.0F * viewX / viewWidth) - 1000;
    }

    private static int viewYToCameraY(float viewY, int viewHeight) {
        return (int) (2000.0F * viewY / viewHeight) - 1000;
    }

    /**
     * This rewrites {@link #mCameraId} and {@link #mCameraInfo}.
     */
    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraId = i;
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
    }


    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;

        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constants.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    void adjustCameraParameters() {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
        if (sizes == null) { // Not supported
            mAspectRatio = chooseAspectRatio();
            sizes = mPreviewSizes.sizes(mAspectRatio);
        }
//        Size size = chooseOptimalSize(sizes);//这里获取的宽高数据是错误的
        Size sizeCamera2 = chooseOptimalSize();//获取正确的宽高
//        PrintLog.d("flag--", "adjustCameraParameters(CameraEngine.java:455)-->>" +size.getWidth()+">>"+size.getHeight()+">>>camera2:"+sizeCamera2.getWidth()+">>>"+sizeCamera2.getHeight() );

        // Always re-apply camera parameters
        // Largest picture size in this ratio
        final Size pictureSize = mPictureSizes.sizes(mAspectRatio).last();
        if (mShowingPreview) {
            mCamera.stopPreview();
        }
        // TODO: 2019/1/5 直播推流添加的，camera优化时，使用策略模式，代码已经调整，先放在这里待验证
//        Integer iNV21Flag = 0;
//        Integer iYV12Flag = 0;
//        for (Integer yuvFormat : mCameraParameters.getSupportedPreviewFormats()) {
//            Log.i(TAG, "preview formats:" + yuvFormat);
//            if (yuvFormat == ImageFormat.YV12) {
//                iYV12Flag = ImageFormat.YV12;
//            }
//            if (yuvFormat == ImageFormat.NV21) {
//                iNV21Flag = ImageFormat.NV21;
//            }
//        }
//
//        int codeType = ImageFormat.NV21;
//        if (iNV21Flag != 0) {
//            codeType = iNV21Flag;
//        } else if (iYV12Flag != 0) {
//            codeType = iYV12Flag;
//        }
//
//        mCameraParameters.setPreviewFormat(codeType);

        //这里不能删除，去除会导致部分手机会卡死
        mCameraParameters.setPreviewSize(sizeCamera2.getWidth(), sizeCamera2.getHeight());
        mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));

        setAutoFocusInternal(mAutoFocus);
        setFlashInternal(mFlash);
        mCamera.setParameters(mCameraParameters);
        if (mShowingPreview) {

            mCamera.startPreview();
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        //TODO 设置画布大小参数
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mWidth;
        final int surfaceHeight = mHeight;

        if (isLandscape(mDisplayOrientation)) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) { // Iterate from small to large
            if (desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()) {
                return size;

            }
            result = size;
        }
        return result;
    }


    /**
     * Chooses the optimal preview size based on {@link #mPreviewSizes} and the surface size.
     *
     * @return The picked size for camera preview.
     */
    private Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mWidth;
        final int surfaceHeight = mHeight;
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            if (mCallback != null) {
                mCallback.onCameraClosed();
            }
        }
    }

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }
        mCamera = Camera.open(mCameraId);
        mCameraParameters = mCamera.getParameters();

        // Supported preview sizes
        mPreviewSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
            mPreviewSizes.add(new Size(size.width, size.height));
        }
        // Supported picture sizes;
        mPictureSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
            mPictureSizes.add(new Size(size.width, size.height));
        }
        // AspectRatio
        if (mAspectRatio == null) {
            mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
        }
        adjustCameraParameters();
        mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));

        if (mCallback != null) {
            mCallback.onCameraOpened();
        }
    }

    public boolean openCamera(Activity activity) {
        synchronized (CameraEngine.class) {
            // 如果别人打开了摄像头，则关闭
            if (mCamera != null && cameraOwnerActivity != activity) {
                Log.e(TAG, "close the camarea opened by others ");
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            if (mCamera == null) {
                try {
                    openCamera();
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }
            return false;
        }

    }

    public Activity cameraOwnerActivity = null;

    public void releaseCamera(Activity activity) {
        synchronized (CameraEngine.class) {
            // 试图关闭别的activity打开的摄像头
            if (cameraOwnerActivity == null || cameraOwnerActivity != activity) {
                return;
            }
            try {

                if (mCamera != null) {
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    cameraOwnerActivity = null;
                }
            } catch (Exception ex) {
            }
        }

    }

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     * <p>
     * This calculation is used for orienting the preview
     * <p>
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     * <p>
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     * <p>
     * Note: This is not the same calculation as the display orientation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calcCameraRotation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == Constants.LANDSCAPE_90 ||
                orientationDegrees == Constants.LANDSCAPE_270);
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
        if (isCameraOpened()) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashInternal(int flash) {
        if (isCameraOpened()) {
            List<String> modes = mCameraParameters.getSupportedFlashModes();
            String mode = FLASH_MODES.get(flash);
            if (modes != null && modes.contains(mode)) {
                mCameraParameters.setFlashMode(mode);
                mFlash = flash;
                return true;
            }
            String currentMode = FLASH_MODES.get(mFlash);
            if (modes == null || !modes.contains(currentMode)) {
                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mFlash = Constants.FLASH_OFF;
                return true;
            }
            return false;
        } else {
            mFlash = flash;
            return false;
        }
    }


}

