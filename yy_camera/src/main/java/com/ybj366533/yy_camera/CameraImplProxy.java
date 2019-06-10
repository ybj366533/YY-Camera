package com.ybj366533.yy_camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.migucc.effects.listener.OnFocusListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

public class CameraImplProxy {

    private static final String TAG = "CameraImplProxy";

    /**
     * 后置摄像头.
     */
    public static final int FACING_BACK = Constants.FACING_BACK;

    /**
     * 前置摄像头.
     */
    public static final int FACING_FRONT = Constants.FACING_FRONT;

    public List<Integer> getSupporttedPreviewFormats() {
        return mImpl.getSupporttedPreviewFormats();
    }

    /**
     * 相机相对于设备屏幕的方向.
     */
    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /**
     * Flash will not be fired.
     */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /**
     * Flash will always be fired during snapshot.
     */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /**
     * Constant emission of light during preview, auto-focus and snapshot.
     */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /**
     * Flash will be fired automatically when required.
     */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /**
     * Flash will be fired in red-eye reduction mode.
     */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    /**
     * The mode for for the camera device's flash control
     */
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash {
    }

    private Context mContext;
    private CameraImpl mImpl;
    private SurfaceTexture mSurfaceTexture;
    private CameraImpl.Callback mCallback;

    //加速度传感器控制
    private SensorControler mSensorController;

    public CameraImplProxy(Context context) {
        this.mContext = context;

//        initSensorControler();

//        if (Build.VERSION.SDK_INT < 21) {
//            mImpl = new CameraEngine(mContext);
//        } else if (Build.VERSION.SDK_INT < 23) {
//            mImpl = new Camera2Engine(mContext);
//        } else {
//            mImpl = new Camera2Engine23(mContext);
//        }

        //FIXME 因为carame2卡顿问题，camera1目前达到效果，先强制使用camera1
        mImpl = new CameraEngine(mContext);
        mImpl.setFacing(FACING_FRONT);
    }

    /**
     * Desc:初始化加速度传感器
     * <p>
     * Author: [李豫]
     * Date: 2018-12-04
     */
    private void initSensorControler() {
//        mSensorController = new SensorControler(mContext);
//        //设置传感器停止移动的聚焦监听
//        mSensorController.setCameraFocusListener(new SensorControler.CameraFocusListener() {
//            @Override
//            public void onFocus() {
//                if (mImpl != null){
//                    Point point = new Point(ScreenUtils.getScreenWidth(mContext) / 2, ScreenUtils.getScreenHeight(mContext) / 2);
//                    mImpl.onFocus(point, new CameraImpl.AutoFocusCallback() {
//                        @Override
//                        public void onAutoFocus(boolean success) {
//                            //相机聚焦成功的回调 success
//                        }
//                    });
//                }
//            }
//        });
    }

    /**
     * Desc:开启加速度传感器
     * <p>
     * Author: [李豫]
     * Date: 2018-12-04
     */
    public void startSensor(){
        if (mSensorController != null){
            mSensorController.onStart();
        }
    }

    /**
     * Desc:停止加速度传感器
     * <p>
     * Author: [李豫]
     * Date: 2018-12-04
     */
    public void stopSensor(){
        if (mSensorController != null){
            mSensorController.onStop();
        }
    }

    public boolean openCamera(int cameraId) {
        mImpl.setFacing(cameraId);
        return true;
    }

    //释放相机
    public void releaseCamera() {
//        stopSensor();
        mImpl.onStop();
    }

    public boolean isFlipHorizontal() {
        if (mImpl == null) {
            return false;
        }
        return mImpl.getFacing() == CameraInfo.CAMERA_FACING_FRONT;
    }

    public void startPreview(SurfaceTexture surfaceTexture, CameraImpl.Callback callback) {
        this.mSurfaceTexture = surfaceTexture;
        this.mCallback = callback;
        mImpl.init(new CameraImpl.Callback() {
            @Override
            public void onCameraOpened() {
                if (mCallback != null){
                    mCallback.onCameraOpened();
                }
//                startSensor();
            }

            @Override
            public void onCameraClosed() {
                if (mCallback != null){
                    mCallback.onCameraClosed();
                }
            }

            @Override
            public void onPreviewFrame(byte[] data) {
                if (mCallback != null){
                    mCallback.onPreviewFrame(data);
                }
            }
        }, mSurfaceTexture);

        setAspectRatio(Constants.DEFAULT_ASPECT_RATIO);
        start();
    }

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * {@link Activity#onResume()}.
     */
    public void start() {
        if (!mImpl.onStart()) {//如果某些设置不支持camera2，转用camera1
            int facing = mImpl.getFacing();
            // Camera2 uses legacy hardware layer; fall back to Camera1
            mImpl = new CameraEngine(mContext);
            mImpl.init(mCallback, mSurfaceTexture);
            mImpl.setFacing(facing);
            mImpl.onStart();
        }
    }


    public void setPreviewSize(int width, int height) {
        mImpl.setSize(width, height);
    }

    public int getOrientation() {
        return mImpl.getOrientation();
    }


    public void onSurfaceChanged() {
        mImpl.setSurfaceChanged();
    }

    /**
     * @return {@code true} if the camera is opened.
     */
    public boolean isCameraOpen() {
        return mImpl.isCameraOpened();
    }

    public CameraImpl getCamera() {
        return mImpl;
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or
     *               {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        mImpl.setFacing(facing);
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
        //noinspection WrongConstant
        return mImpl.getFacing();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (mImpl.setAspectRatio(ratio)) {
//            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        mImpl.setFlash(flash);
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    public int getFlash() {
        //noinspection WrongConstant
        return mImpl.getFlash();
    }

    public void onFocus(Point point, OnFocusListener listener) {
        if (mImpl != null) {
            mImpl.onFocus(point, new CameraImpl.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success) {
                    if (listener != null) {
                        if (success) {
                            listener.onFocusSucess();
                        } else {
                            listener.onFocusFail();
                        }
                    }
                }
            });
        }
    }
}
