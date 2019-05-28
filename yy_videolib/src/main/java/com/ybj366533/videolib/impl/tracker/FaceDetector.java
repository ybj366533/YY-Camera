package com.ybj366533.videolib.impl.tracker;

import android.graphics.Rect;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gtv on 2018/6/5.
 */

public class FaceDetector {

    private static final String TAG = "GTVREC";

    static final int STS_FACE_UNKNOW = -1;          // no face
    static final int STS_FACE_GOTONE = 0;           // detect first face
    static final int STS_FACE_TRACKING = 1;         // tracking face

    private int status = STS_FACE_UNKNOW;
    private long lastDetectTimestamp = 0;

    private float sdmDegree = 0;
    private float[] sdmTrackList = new float[3];    // center_x, center_y, angle

    AFT_FSDKVersion version = null;
    AFT_FSDKEngine engine = null;
    AFT_FSDKEngine portraitEngine = null;
    List<AFT_FSDKFace> result = null;
    private int mFaceDegree = 0;
    private boolean mFaceFlag = false;
    private int mBigJumpCnt = 0;
    private int mLostCnt = 0;

    private Rect mFaceRect = new Rect();

    private int[] eye_pos = new int[4];
    private int[] small_face_pos = new int[4];
    protected float[] outputFaceData = new float[136];

    public FaceDetector() {

        engine = new AFT_FSDKEngine();
        portraitEngine = new AFT_FSDKEngine();
        result = new ArrayList<>();
        version = new AFT_FSDKVersion();

        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine("FSFyi5sEbCLxoCpEkdK6mX9iG1vgSeBx9PAQRNq5DEqF", "2NrtMQesjKrh1Ns42EEnZtSt1PYiQogB1wWVCJpBsuUR", AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 1);
        //Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        //Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        err = portraitEngine.AFT_FSDK_InitialFaceEngine("FSFyi5sEbCLxoCpEkdK6mX9iG1vgSeBx9PAQRNq5DEqF", "2NrtMQesjKrh1Ns42EEnZtSt1PYiQogB1wWVCJpBsuUR", AFT_FSDKEngine.AFT_OPF_0_ONLY, 16, 1);
        //Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = portraitEngine.AFT_FSDK_GetVersion(version);
        //Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        return;
    }

    public boolean isTrackingFace() {

        long now = System.currentTimeMillis();

        if( now - lastDetectTimestamp > 1000 ) {
            //Log.e(TAG, "isTrackingFace timeout ");
            status = STS_FACE_UNKNOW;
        }

        if( this.status == STS_FACE_TRACKING ) {
            return true;
        }

        return false;
    }

    public boolean getAllFacePos(float[] p) {

        if( mFaceFlag == true && status == STS_FACE_TRACKING ) {

            for( int i=0; i<136; i++ ) {
                p[i] = outputFaceData[i];
            }

            return true;
        }

        return false;
    }

    public boolean getSmallFacePos(int[] p) {

        if( mFaceFlag == true && status == STS_FACE_TRACKING ) {

            p[0] = small_face_pos[0];
            p[1] = small_face_pos[1];
            p[2] = small_face_pos[2];
            p[3] = small_face_pos[3];

            return true;
        }

        return false;
    }

    public boolean getEyePos(int[] p) {

        if( mFaceFlag == true && status == STS_FACE_TRACKING ) {

            p[0] = eye_pos[0];
            p[1] = eye_pos[1];
            p[2] = eye_pos[2];
            p[3] = eye_pos[3];

            return true;
        }

        return false;
    }

    public float[] rotationInfoBeforeDetect() {

        if( status == STS_FACE_UNKNOW ) {
            sdmTrackList[0] = sdmTrackList[1] = sdmTrackList[2] = 0.0f;
            this.mFaceFlag = false;
        }

        float[] a = new float[3];

        a[0] = sdmTrackList[0];
        a[1] = sdmTrackList[1];
        a[2] = sdmTrackList[2];

        return a;
    }

    public float[] imageVetexInfoBeforeDetect(int width, int height) {

        float imageVertices[] = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f,  1.0f,
                1.0f,  1.0f,
        };

        long now = System.currentTimeMillis();

        if( now - lastDetectTimestamp > 1000 ) {
            status = STS_FACE_UNKNOW;
            //Log.e(TAG, "STS_FACE_UNKNOW imageVetexInfoBeforeDetect timeout ");
        }

        lastDetectTimestamp = now;

        if( status == STS_FACE_UNKNOW ) {
            return imageVertices;
        }

        float centerX = sdmTrackList[0];
        float centerY = sdmTrackList[1];
        float angle = sdmTrackList[2];
        //Log.e(TAG, "rotate params " + centerX + "," + centerY + "," + angle);
        EffectTracker.getInstance().getGLESImageVertex(centerX, centerY, angle, width, height, imageVertices);

        return imageVertices;
    }

    public void detect(byte[] data, int width, int height, float icenterx, float icentery, float iangle) {

        // 因为pbo的原因，可能在track lost后，还会出现有旋转的数据
        if( status == STS_FACE_UNKNOW ) {
            if( icenterx > 0.0f && icentery > 0.0f && iangle > 0.0f ) {
                return;
            }
        }

        // 因为pbo的原因，可能在track got one后，还会出现未旋转的数据
//        if( status == STS_FACE_GOTONE ) {
//            if( sdmTrackList[0] > 0.0f && sdmTrackList[1] > 0.0f && sdmTrackList[2] > 0.0f ) {
//                if( icenterx <= 0.0f && icentery <= 0.0f && iangle <= 0.0f ) {
//                    return;
//                }
//            }
//        }

        // use arc get base degree
        if( status == STS_FACE_TRACKING ) {

            AFT_FSDKError err = portraitEngine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
            if( err.getCode() != AFT_FSDKError.MOK ) {
                //Log.e(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
            }
        }
        else {

            AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
            if( err.getCode() != AFT_FSDKError.MOK ) {
                //Log.e(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
            }
        }

        if( result.size() > 0 ) {

            mLostCnt = 0;
            lastDetectTimestamp = System.currentTimeMillis();

            for (AFT_FSDKFace face : result) {
                this.mFaceRect = face.getRect();
                this.mFaceDegree = face.getDegree();
                int w = this.mFaceRect.right - this.mFaceRect.left;
                int h = this.mFaceRect.bottom - this.mFaceRect.top;
//                Log.e(TAG, "AFT_FSDK_FaceFeatureDetect =" + this.mFaceDegree + " rect "
//                        + this.mFaceRect.left + "," + this.mFaceRect.top + "," + w + "," + h);
                break;
            }

            // first time
            if( this.status == STS_FACE_UNKNOW ) {

                float center_x = (this.mFaceRect.left + this.mFaceRect.right) / 2.0f;
                float center_y = (this.mFaceRect.top + this.mFaceRect.bottom) / 2.0f;
                float angle = 0.0f;

                switch(this.mFaceDegree) {
                    case 1:
                        angle = 0.0f;
                        break;
                    case 2:
                        angle = 270.0f;
                        break;
                    case 3:
                        angle = 90.0f;
                        break;
                    case 4:
                        angle = 180.0f;
                        break;
                    default:
                        break;
                }

                sdmTrackList[0] = center_x;
                sdmTrackList[1] = center_y;
                sdmTrackList[2] = angle;
//Log.e(TAG, "STS_FACE_GOTONE " + sdmTrackList[0] + "," + sdmTrackList[1] + "," + sdmTrackList[2]);
                this.status = STS_FACE_GOTONE;
            }
            else if( this.status == STS_FACE_GOTONE ) {

                // last time, roate center
                float center_x = sdmTrackList[0];
                float center_y = sdmTrackList[1];
                float angle = sdmTrackList[2];

                if( icenterx > 0.0f && icentery > 0.0f ) {
                    center_x = icenterx;
                    center_y = icentery;
                    angle = iangle;
                }

                this.mFaceFlag = true;
                EffectTracker.getInstance().updateFaceRect(this.mFaceRect.left, this.mFaceRect.top, this.mFaceRect.right-this.mFaceRect.left, this.mFaceRect.bottom-this.mFaceRect.top);
                int res = EffectTracker.getInstance().trackImageAJNI(data, width*height, width, height, (int)center_x, (int)center_y, angle, outputFaceData);
                if (res < 0) {
                    // sdmTracker失败，目前主要原因是model还没load
                    this.mFaceFlag = false;
                }

                EffectTracker.getInstance().getBigEyePositionJNI(eye_pos);
                EffectTracker.getInstance().getSmallFacePositionJNI(small_face_pos);

                float[] t = new float[3];
                EffectTracker.getInstance().getRotationParams(t);

//                int d = (int)t[2];
//                d = (d / 10) * 10;
//                t[2] = d * 1.0f;

//                if( t[2] < 10.0f && t[2] > -10.0f ) {
//                    t[2] = 0.0f;
//                }

                sdmTrackList[0] = t[0];
                sdmTrackList[1] = t[1];
                sdmTrackList[2] = t[2];
                this.mBigJumpCnt = 0;
                //Log.e(TAG, "STS_FACE_TRACKING " + sdmTrackList[0] + "," + sdmTrackList[1] + "," + sdmTrackList[2]);
                this.status = STS_FACE_TRACKING;
            }
            else {

                // last time, roate center
                float center_x = sdmTrackList[0];
                float center_y = sdmTrackList[1];
                float angle = sdmTrackList[2];

                if( icenterx > 0.0f && icentery > 0.0f ) {
                    center_x = icenterx;
                    center_y = icentery;
                    angle = iangle;
                }

                this.mFaceFlag = true;
                EffectTracker.getInstance().updateFaceRect(this.mFaceRect.left, this.mFaceRect.top, this.mFaceRect.right-this.mFaceRect.left, this.mFaceRect.bottom-this.mFaceRect.top);
                int res = EffectTracker.getInstance().trackImageAJNI(data, width*height, width, height, (int)center_x, (int)center_y, angle, outputFaceData);
                if (res < 0) {
                    // sdmTracker失败，目前主要原因是model还没load
                    this.mFaceFlag = false;
                    this.status = STS_FACE_UNKNOW;
                    //Log.e(TAG, "STS_FACE_UNKNOW trackImageAJNI failed ");
                }
                else {

                    EffectTracker.getInstance().getBigEyePositionJNI(eye_pos);
                    EffectTracker.getInstance().getSmallFacePositionJNI(small_face_pos);

                    float distance = (eye_pos[0] - eye_pos[2]) * (eye_pos[0] - eye_pos[2]) + (eye_pos[1] - eye_pos[3]) * (eye_pos[1] - eye_pos[3]);
                    distance = (float)Math.sqrt(distance);
                    if( distance >= this.mFaceRect.width() ) {

                        this.mFaceFlag = false;
                        this.status = STS_FACE_UNKNOW;
                        //Log.e(TAG, "STS_FACE_UNKNOW since eye distance " + distance);
                    }
                    else {

                        float[] t = new float[3];
                        EffectTracker.getInstance().getRotationParams(t);

//                    int d = (int)t[2];
//                    d = (d / 10) * 10;
//                    t[2] = d * 1.0f;

//                    if( t[2] < 10.0f && t[2] > -10.0f ) {
//                        t[2] = 0.0f;
//                    }
                        float angle1 = t[2];
                        float angle2 = sdmTrackList[2];

                        float diff = Math.abs(angle1 - angle2);
                        if( diff > 180.0f ) {
                            diff = 360.0f - diff;
                        }

                        if( diff > 45 ) {
                            //Log.e(TAG, "big jump " + t[2] + "," + sdmTrackList[2] + ":" + angle1 + "," + angle2);
                            this.mBigJumpCnt ++;

                            if( this.mBigJumpCnt > 5 ) {
                                this.mFaceFlag = false;
                                this.status = STS_FACE_UNKNOW;
                                //Log.e(TAG, "STS_FACE_UNKNOW since big jump ");
                            }
                        }
                        else {
                            this.mBigJumpCnt = 0;

                            if( diff > 15 ) {
                                sdmTrackList[2] = t[2];
                            }

                            sdmTrackList[0] = t[0];
                            sdmTrackList[1] = t[1];
                        }
                    }
                }
            }
            result.clear();
            //Log.e(TAG, "this.status " + this.status + " -- " + this.mFaceDegree + " rotation " + sdmTrackList[2]);
        }
        else {

            //Log.e(TAG, "track lost ");
/*
            boolean try_track_ret = true;

            float center_x = sdmTrackList[0];
            float center_y = sdmTrackList[1];
            float angle = sdmTrackList[2];

            if( icenterx > 0.0f && icentery > 0.0f ) {
                center_x = icenterx;
                center_y = icentery;
                angle = iangle;
            }

            // 没有检测到人脸，尝试依靠之前的区域回归
            float[] box_bef = new float[4];
            int ret = EffectTracker.getInstance().getEnclosingBox(box_bef);
            if( ret < 0 ) {
                try_track_ret = false;
            }
            else {

                int left = (int)box_bef[0];
                int top = (int)box_bef[1];
                int right = (int)box_bef[2];
                int bottom = (int)box_bef[3];

                EffectTracker.getInstance().updateFaceRect(left, top, right-left, bottom-top);
                int res = EffectTracker.getInstance().trackImageAJNI(data, width*height, width, height, (int)center_x, (int)center_y, angle, outputFaceData);
                if (res < 0) {
                    // sdmTracker失败，目前主要原因是model还没load
                    try_track_ret = false;
                }
                else {

                    float[] box_aft = new float[4];
                    ret = EffectTracker.getInstance().getEnclosingBox(box_aft);
                    if( ret < 0 ) {
                        try_track_ret = false;
                    }
                    else {

                        float width_aft = (box_aft[2] - box_aft[0]);
                        float height_aft = (box_aft[3] - box_aft[1]);
//                        float center_x_aft = (box_aft[0] + box_aft[2]) / 2.0f;
//                        float center_y_aft = (box_aft[1] + box_aft[3]) / 2.0f;

                        float width_bef = (box_bef[2] - box_bef[0]);
                        float height_bef = (box_bef[3] - box_bef[1]);
//                        float center_x_bef = (box_bef[0] + box_bef[2]) / 2.0f;
//                        float center_y_bef = (box_bef[1] + box_bef[3]) / 2.0f;

                        float w_diff = Math.abs(width_aft-width_bef);
                        float h_diff = Math.abs(height_aft-height_bef);
                        if( w_diff > width_bef / 3.0f || h_diff > height_bef / 3.0f ) {
                            try_track_ret = false;
                            Log.e(TAG, "STS_FACE_UNKNOW since w_diff lost " +
                                    box_aft[0] + "," + box_aft[1] + "," + box_aft[2] + "," + box_aft[3] +
                                    box_bef[0] + "," + box_bef[1] + "," + box_bef[2] + "," + box_bef[3]);
                        }
                        else {

                            EffectTracker.getInstance().getBigEyePositionJNI(eye_pos);
                            EffectTracker.getInstance().getSmallFacePositionJNI(small_face_pos);

                            float distance = (eye_pos[0] - eye_pos[2]) * (eye_pos[0] - eye_pos[2]) + (eye_pos[1] - eye_pos[3]) * (eye_pos[1] - eye_pos[3]);
                            distance = (float)Math.sqrt(distance);
                            if( distance >= this.mFaceRect.width() ) {

                                this.mFaceFlag = false;
                                this.status = STS_FACE_UNKNOW;
                                Log.e(TAG, "STS_FACE_UNKNOW since eye distance " + distance);
                            }
                            else {

                                float[] t = new float[3];
                                EffectTracker.getInstance().getRotationParams(t);

                                float angle1 = (float) Math.cos(Math.toRadians(t[2]));
                                float angle2 = (float) Math.cos(Math.toRadians(sdmTrackList[2]));

                                float diff = angle1 - angle2;

                                if( diff > 0.5f || diff < -0.5f ) {
                                    Log.e(TAG, "big jump " + t[2] + "," + sdmTrackList[2] + ":" + angle1 + "," + angle2);
                                    this.mBigJumpCnt ++;

                                    if( this.mBigJumpCnt > 5 ) {
                                        this.mFaceFlag = false;
                                        this.status = STS_FACE_UNKNOW;
                                        Log.e(TAG, "STS_FACE_UNKNOW since big jump ");
                                    }
                                }
                                else {
                                    this.mBigJumpCnt = 0;

                                    if( sdmTrackList[2] - t[2] > 5.0f || sdmTrackList[2] - t[2] < -5.0f ) {
                                        sdmTrackList[2] = t[2];
                                    }

                                    sdmTrackList[0] = t[0];
                                    sdmTrackList[1] = t[1];
                                }
                            }
                        }
                    }
                }
            }

            if( try_track_ret == false ) {

                sdmTrackList[0] = sdmTrackList[1] = sdmTrackList[2] = 0.0f;
                this.mFaceFlag = false;
                this.status = STS_FACE_UNKNOW;
                Log.e(TAG, "STS_FACE_UNKNOW since try_track_ret lost ");
            }
*/

            if( this.status == STS_FACE_UNKNOW ) {
                return;
            }

            this.mLostCnt ++;

            if( this.mLostCnt > 3 ) {

                sdmTrackList[0] = sdmTrackList[1] = sdmTrackList[2] = 0.0f;
                this.mFaceFlag = false;
                this.status = STS_FACE_UNKNOW;
                //Log.e(TAG, "STS_FACE_UNKNOW since track lost ");
            }
        }

        return;
    }

    void destroy() {
        if(engine != null) {
            engine.AFT_FSDK_UninitialFaceEngine();
        }

        if(portraitEngine != null) {
            portraitEngine.AFT_FSDK_UninitialFaceEngine();
        }
    }
}
