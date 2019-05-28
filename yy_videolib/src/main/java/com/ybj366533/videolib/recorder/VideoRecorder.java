package com.ybj366533.videolib.recorder;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;

import com.ybj366533.videolib.impl.YYMP4Help;
import com.ybj366533.videolib.impl.YYVideoSegments;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.impl.utils.YYFileUtils;
import com.ybj366533.videolib.stream.VideoRecordStreamer;
import com.ybj366533.videolib.utils.YYMusicHandler;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.videolib.widget.RecorderViewRender;
import com.ybj366533.videolib.impl.setting.AVStreamSetting;
import com.ybj366533.gtvimage.gtvfilter.filter.instagram.FilterHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Recorder 接口实现类
 */
public class VideoRecorder implements IVideoRecorder {

    private static final String TAG = "Recorder";

    private static final int MAX_RECORD_DURATION = 15 * 1000;

    private GLSurfaceView glSurfaceView;


    private RecorderViewRender gtvRecorderViewRender;

    private String workFolder;

    private int bitrate;
    private int width;
    private int height;

    private int maxDuration;

    VideoRecordStreamer videoRecordStreamer;

    RecordCallback recordCallback;

    private String bgMusicPath;
    private int musicStartTime;
    private boolean musicReseekFlag;

    private double recordSpeed;
    private SpeedType recordSpeedType;
//    public double getRecordSpeed() {
//        return recordSpeed;
//    }

    private int clipNum = 0;    //todo

    private boolean firstTimeRecord = true;         // 第一次拍摄时，需要打开(open)音乐，而后只是重开(resume)音乐

    private YYVideoSegments YYVideoSegments;
    private VideoInfo videoInfo;

    private String currentRecFileName;

    HandlerThread handlerThread;
    private Handler handler = null;

    // 有配乐时，配乐是否录制到文件
    // true 配乐作为声音源 直接录制到文件
    // false 录制声音为0，配乐文件作为设定传递给编辑画面（等同于编辑画面的配乐选择）
    private boolean musicRecordToFile = false;

    private boolean onMaxDurationSent = false;      // 一次录制 只发送一次

    VideoRecordStreamer.RecordCallback callback = new VideoRecordStreamer.RecordCallback() {
        //private boolean onMaxDurationSent = false;        // 因为可能多次删除，多次录制，改为全体变量。
        @Override
        public void onProgress(int duration) {

            if (VideoRecorder.this.recordCallback != null) {
                VideoRecorder.this.recordCallback.onProgress(videoInfo.getTotalDuration() + duration, videoInfo);
                if ((videoInfo.getTotalDuration() + duration >= maxDuration) && (onMaxDurationSent == false)) {
                    LogUtils.LOGI(TAG, "on max duration ");
                    VideoRecorder.this.recordCallback.onMaxDuration();
                    onMaxDurationSent = true;
                }
            }
        }

        @Override
        public void onComplete(int duration) {
            LogUtils.LOGI(TAG, "record complete, duration: " + duration);
            if (duration < 100) {
                if (VideoRecorder.this.recordCallback != null) {
                    VideoRecorder.this.recordCallback.onRecordComplete(false, duration);
                }
            } else {
                if (VideoRecorder.this.recordCallback != null) {
                    VideoRecorder.this.recordCallback.onRecordComplete(true, duration);
                }

                YYVideoSegments.addVideoSegInfo(currentRecFileName, (int) duration, (float) VideoRecorder.this.recordSpeed);
                videoInfo = YYVideoSegments.getVideoSegInfo();
                videoInfo.dump();
            }

        }

        @Override
        public void onError(int errorCode) {

        }
    };

    public VideoRecorder() {
        this.bitrate = 20*1000000;
        this.width = 368;
        this.height = 640;
        recordSpeed = 1.0;
        recordSpeedType = SpeedType.STANDARD;
        maxDuration = MAX_RECORD_DURATION;

        musicReseekFlag = false;

        HandlerThread handlerThread = new HandlerThread("recordtask");
        handlerThread.start();

//这里获取到HandlerThread的runloop
        handler = new Handler(handlerThread.getLooper());
    }

    public void init(GLSurfaceView glSurfaceView, String workFolder, RecordCallback recordCallback) {
        init(glSurfaceView, workFolder, CameraType.BACK, recordCallback);

    }

    public void init(GLSurfaceView glSurfaceView, String workFolder, CameraType cameraId, RecordCallback recordCallback) {

        this.glSurfaceView = glSurfaceView;
        this.workFolder = workFolder;


        this.recordCallback = recordCallback;

        LogUtils.LOGI(TAG, "init Ver. 1.87");

        int camId = 0;
        if (cameraId == CameraType.FRONT) {
            camId = 1;
        }

        gtvRecorderViewRender = new RecorderViewRender(glSurfaceView, camId, this.recordCallback);

        handler.post(new Runnable() {
            @Override
            public void run() {
                init_impl();
            }
        });


    }

    private void init_impl() {

//        this.glSurfaceView =glSurfaceView;
//        this.workFolder = workFolder;
//
//        this.recordCallback = recordCallback;

        {
            if (!workFolder.endsWith("" + File.separator)) {
                workFolder += "" + File.separator;
            }
            YYFileUtils.createDirectoryIfNeed(workFolder);
        }

//        gtvRecorderViewRender = new RecorderViewRender(glSurfaceView, this.recordCallback);

        YYFileUtils.createDirectoryIfNeed(this.workFolder);

        // todo 创建失败？
        // todo 如果传入的路径没有/

        YYVideoSegments = new YYVideoSegments(this.workFolder + "/data.txt");
        videoInfo = YYVideoSegments.getVideoSegInfo();
        if (recordCallback != null) {
            recordCallback.onPrepared(videoInfo);        // 如果已经执行了destroy，还要发？可能activit已经不存在了。
        }

        LogUtils.LOGI(TAG, "init end");

    }

    public void startPreview() {
        LogUtils.LOGI(TAG, "startPreview");
        if (gtvRecorderViewRender != null) {
            gtvRecorderViewRender.onResume();
        }
    }

    public void stopPreview() {
        LogUtils.LOGI(TAG, "stopPreview");
        if (gtvRecorderViewRender != null) {
            gtvRecorderViewRender.onPause();
        }
    }


    public void setVideoSize(int width, int height) {
        this.width = width;
        this.height = height;

        if (this.width % 16 != 0) {
            this.width += 16;
            this.width -= this.width % 16;
        }

        if (this.height % 16 != 0) {
            this.height += 16;
            this.height -= this.height % 16;
        }

        LogUtils.LOGI(TAG, "Video Size is set to " + this.width + " " + this.height);
    }

    public int getVideoWidth() {
        return this.width;
    }

    public int getVideoHeight() {
        return this.height;
    }

    public void setRecordSpeed(SpeedType speedType) {
        LogUtils.LOGI(TAG, "setRecordSpeed " + speedType);
        this.recordSpeedType = speedType;
        if (speedType == SpeedType.SLOW) {
            this.recordSpeed = 0.5;
        } else if (speedType == SpeedType.FAST) {
            this.recordSpeed = 2;
        } else {
            this.recordSpeed = 1;
        }
    }

    public SpeedType getRecordSpeed() {
        return this.recordSpeedType;
    }

    public void setMaxDuration(int maxDurationMili) {
        LogUtils.LOGI(TAG, "setMaxDuration " + maxDurationMili);
        maxDuration = maxDurationMili;
    }

    public int getMaxDuration() {
        return this.maxDuration;
    }

//    public void prepareRecord(){
//        videoInfo = YYVideoSegments.getVideoSegInfo();
//    }

    public void deleteLastVideoClip() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                deleteLastVideoClip_impl();
            }
        });
    }

    private void deleteLastVideoClip_impl() {

        LogUtils.LOGI(TAG, "deleteLastVideoClip start");

        if (videoInfo.getCount() < 1) {
            return;
        }

        musicReseekFlag = true;     // 配乐的播放位置需要重新seek

        YYVideoSegments.removeLastVideoSeg();
        videoInfo = YYVideoSegments.getVideoSegInfo();
        videoInfo.dump();

        if (VideoRecorder.this.recordCallback != null) {
            VideoRecorder.this.recordCallback.onProgress(videoInfo.getTotalDuration(), videoInfo);
        }

        LogUtils.LOGI(TAG, "deleteLastVideoClip end");

    }

    public void deleteAllVideoClips() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                deleteAllVideoClips_impl();
            }
        });
    }

    private void deleteAllVideoClips_impl() {

        LogUtils.LOGI(TAG, "deleteAllVideoClips start");

        File dir = new File(this.workFolder);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                File file = new File(dir, children[i]);
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        } else {
            //impossible
        }

        musicReseekFlag = true;     // 配乐的播放位置需要重新seek

        videoInfo = YYVideoSegments.getVideoSegInfo();
        videoInfo.dump();

        if (VideoRecorder.this.recordCallback != null) {
            VideoRecorder.this.recordCallback.onProgress(videoInfo.getTotalDuration(), videoInfo);
        }

        LogUtils.LOGI(TAG, "deleteAllVideoClips end");
    }

    public int getDuration() {
        // todo 不包含当前片段
        return videoInfo.getTotalDuration();
    }

    private String getClipName() {
        int clipNum = videoInfo.getCount();
        clipNum++;
        return "clip_" + clipNum + ".mp4";
    }
    // todo  return value ?

    public void startRecord() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                startRecord_impl();
            }
        });
    }

    private void startRecord_impl() {

        LogUtils.LOGI(TAG, "startRecord");

        if (gtvRecorderViewRender == null || gtvRecorderViewRender.getEGLContext() == null || gtvRecorderViewRender.getVideoResource() == null) {
            // todo recall?
            LogUtils.LOGI(TAG, "startRecord failed");
            return;
        }

        // 提前检查是否超限，避免生成无效文件。
        if (videoInfo.getTotalDuration() >= maxDuration) {
            LogUtils.LOGI(TAG, "startRecord failed: max duration");
            if (VideoRecorder.this.recordCallback != null) {
                VideoRecorder.this.recordCallback.onMaxDuration();
            }
            return;
        }

        onMaxDurationSent = false;      // 每次拍摄都有机会发送一次onmax

        //clipNum++;
        currentRecFileName = getClipName();         // todo
        String recFileName = this.workFolder + currentRecFileName;

        AVStreamSetting setting = AVStreamSetting.settingForVideoRecord(bitrate, width, height, true);
        videoRecordStreamer = new VideoRecordStreamer(recFileName, setting, gtvRecorderViewRender.getEGLContext());

        videoRecordStreamer.setInputVideoSource(gtvRecorderViewRender.getVideoResource());

        videoRecordStreamer.openStream(this.bgMusicPath == null ? false : true, this.recordSpeed, musicRecordToFile, callback);

        if (bgMusicPath != null) {
            if (firstTimeRecord == true) {
                if (musicRecordToFile == true) {
                    YYMusicHandler.getInstance().playMusic(bgMusicPath, videoRecordStreamer);
                } else {
                    YYMusicHandler.getInstance().playMusic(bgMusicPath, null);
                }

                YYMusicHandler.getInstance().seekTo(getMusicPosition());
                firstTimeRecord = false;
            } else {
                if(musicReseekFlag == true) {
                    YYMusicHandler.getInstance().seekTo(getMusicPosition());
                    musicReseekFlag = false;
                }

                if (musicRecordToFile == true) {
                    YYMusicHandler.getInstance().resumePlay(videoRecordStreamer);
                } else {
                    YYMusicHandler.getInstance().resumePlay(null);
                }

            }
        }
        LogUtils.LOGI(TAG, "startRecord end");

    }

    public void pauseRecord() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                pauseRecord_impl();
            }
        });
    }

    private void pauseRecord_impl() {

        LogUtils.LOGI(TAG, "pauseRecord");
        YYMusicHandler.getInstance().pausePlay();
        if (videoRecordStreamer != null) {
            videoRecordStreamer.closeStream();
            videoRecordStreamer = null;
            //YYVideoSegments.addVideoSegInfo(currentRecFileName, 3000);     //todo

        } else {
            // todo 回调？
        }

        LogUtils.LOGI(TAG, "pauseRecord end");
    }

    public void resumeRecord() {
        startRecord();
    }

    public void stopRecord() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                stopRecord_impl();
            }
        });
    }

    private void stopRecord_impl() {

        LogUtils.LOGI(TAG, "stopRecord");

        // 因为可能 从编辑界面返回重新录制，所以，音乐也只有暂停
        //YYMusicHandler.getInstance().stopPlay(); // todo 如果声音停的慢，可能中途encoder会已经被停止
        YYMusicHandler.getInstance().pausePlay();
        if (videoRecordStreamer != null) {
            videoRecordStreamer.closeStream();
            videoRecordStreamer = null;
            //YYVideoSegments.addVideoSegInfo(currentRecFileName, 3000);     //todo
        } else {
            // 【下一步】按钮时，需要 录像结束的回调来触发 导出。
            if (VideoRecorder.this.recordCallback != null) {
                VideoRecorder.this.recordCallback.onRecordComplete(false, 0);
            }
        }

        LogUtils.LOGI(TAG, "stopRecord end");
        //todo 合成

    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void exportTopath(final String path, final String pathRev) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // todo 如果路径不存在
                exportTopath_impl(path, pathRev);
            }
        });
    }

    public void exportTopath_impl(final String path, final String pathRev) {

        LogUtils.LOGI(TAG, "export start");

        {
            int lastIndex = path.lastIndexOf(File.separator);
            if (lastIndex < 0) {
                LogUtils.LOGI(TAG, "path error");
                return;
            }
            String dirPath = path.substring(0, lastIndex + 1);
            YYFileUtils.createDirectoryIfNeed(dirPath);
        }

        {
            int lastIndex = pathRev.lastIndexOf(File.separator);
            if (lastIndex < 0) {
                LogUtils.LOGI(TAG, "path error");
                return;
            }
            String dirPath = pathRev.substring(0, lastIndex + 1);
            YYFileUtils.createDirectoryIfNeed(dirPath);
        }


        VideoInfo videoInfo = getVideoInfo();
        if (videoInfo.getCount() > 0) {
            int count = videoInfo.getCount();
            String[] fileList = new String[count];
            for (int i = 0; i < count; ++i) {
                fileList[i] = this.workFolder + videoInfo.getVideoClipList().get(i).getFileName();
            }
            int ret = YYMP4Help.Mp4VideoClipsMerge(fileList, path, pathRev, YYFileUtils.getTempFolder(glSurfaceView.getContext()));

            // 为了界面测试
//            try {
//                Thread.sleep(5*1000);
//            } catch (Exception e) {
//
//            }


            if (recordCallback != null) {
                recordCallback.onExportComplete(ret == 0 ? true : false);
            }


        } else {
            if (recordCallback != null) {
                recordCallback.onExportComplete(false);
            }

            LogUtils.LOGI(TAG, "export error: no clip");
        }

        LogUtils.LOGI(TAG, "export end");
//        int count = YYVideoSegments.getSegmentCount()
//        {
//            //test
//
//            LogUtils.DebugLog("GTVMP4Help_Mp4VideoClipsMerge", "before");
//            String[] fileList = {this.workFolder+"1.mp4", this.workFolder+"2.mp4"};
//            YYMP4Help.Mp4VideoClipsMerge(fileList, this.workFolder+"test.mp4");
//            LogUtils.DebugLog("GTVMP4Help_Mp4VideoClipsMerge", "after");
//        }
    }

    private void exportTest() {
        String[] fileList = new String[3];
        fileList[0] = "/storage/emulated/0/DCIM/SV_20180208_235507/clip_1.mp4";
        fileList[1] = "/storage/emulated/0/DCIM/SV_20180208_235507/clip_2.mp4";
        fileList[2] = "/storage/emulated/0/DCIM/SV_20180208_235507/clip_3.mp4";
        //YYMP4Help.Mp4VideoClipsMerge(fileList, "/storage/emulated/0/DCIM/SV_20180208_235507/aa.mp4", "/storage/emulated/0/DCIM/SV_20180208_235507/aar.mp4");
    }

    public void destroy() {

        YYMusicHandler.getInstance().stopPlay(); // todo 如果声音停的慢，可能中途encoder会已经被停止

        if (gtvRecorderViewRender != null) {
            gtvRecorderViewRender.onDestroy();
        }

        if (handlerThread != null) {
            handlerThread.quit();
        }

//        if (YYVideoSegments != null) {
//            YYVideoSegments.close();
//        }
    }


    public void setMusicPath(String musicPath) {
        this.setMusicPath(musicPath, 0);

    }

    public void setMusicPath(String musicPath, int startTime) {
        this.bgMusicPath = musicPath;
        this.musicStartTime = startTime;
        this.firstTimeRecord = true;    // 要求：录制一个以上片段删除之后可以重新设置音乐。（APP侧需要防止在录制期间多次设置音乐，只有最后一次会被传给编辑页面）
    }

    public String getMusicPath() {
        return this.bgMusicPath;
    }

    public int getMusicStartTimeMili() {
        return this.musicStartTime;
    }

    public boolean setLogoBitmapAtRect(Bitmap bmp, Rect r) {
        LogUtils.LOGI(TAG, "set logo.");
        if (gtvRecorderViewRender != null) {
            return gtvRecorderViewRender.setLogoBitmapAtRect(bmp, new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top));
        } else {
            // todo 如果还没准备好，要暂存？
            LogUtils.LOGI(TAG, "set logo fail.");
        }
        return true;
    }

    public boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r) {
        LogUtils.LOGI(TAG, "set logo2.");
        if (gtvRecorderViewRender != null) {
            return gtvRecorderViewRender.setLogoBitmapAtRect2(bmp, new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top));
        } else {
            // todo 如果还没准备好，要暂存？
            LogUtils.LOGI(TAG, "set logo fail.");
        }
        return true;
    }

    public int getZoom() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().getZoom();
        }

        return 0;
    }

    public void setZoom(int zoom) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setZoom(zoom);
        }
    }

    public boolean isZoomSupported() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().isZoomSupported();
        }
        return false;
    }

    public void setFilter(final int filterType) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setMagicFilterType(filterType);
        }
    }

    public void setFilterByName(String filterByName) {
        int filterType = FilterHelper.getFilterIndex(filterByName);
        //Log.e("oooooooo", " " + filterByName +" " + filterType);
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setMagicFilterType(filterType);
        }
    }

    public boolean supportLightOn() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().supportLightOn();
        }
        return false;
    }

    public boolean isLightOn() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().isLightOn();
        }
        return false;
    }

    public void setLightStatus(boolean flag) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setLightStatus(flag);
        }
    }

    public void setFocus(float x, float y, OnFocusListener listener) {
        if (gtvRecorderViewRender != null) {
            gtvRecorderViewRender.focusOnTouch(x, y, listener);
        }
    }

    public int currentCameraId() {

        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().currentCameraId();
        }

        return 0;
    }

    public void switchCamera(int cameraId) {

        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().switchCamera(cameraId);
        }
    }

    public void setStickerPath(String folder) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.setStickerPath(folder);
        }
    }

    public void startAnimationGroup(List<String> l) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().startAnimationGroup(l);
        }
    }

    public void startAnimationGroupKeepLast(List<String> l) {
        if( gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().startAnimationGroupKeepLast(l);
        }

    }

    public void stopAnimationGroup() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().stopAnimationGroup();
        }
    }

    public void startAnimation(String folder) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            List<String> l = new ArrayList<>();
            l.add(folder);
            gtvRecorderViewRender.getCameraVideoObject().startAnimationGroup(l);
        }
    }

    public void stopAnimation() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().stopAnimationGroup();
        }
    }


    public void setBeautyParams(int index, float percent) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setBeautyParams(index, percent);
        }
    }

    public float[] getBeautyParams() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().getBeautyParams();
        } else {
            return null;
        }
    }

    public float[] resetBeautyParams() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().resetBeautyParams();
        } else {
            return null;
        }
    }

    public float[] switchBeauty(boolean flag) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().switchBeauty(flag);
        } else {
            return null;
        }
    }

    public Boolean isBeautyOn() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().isBeautyOn();
        } else {
            return false;
        }
    }

    public void setBigEye(int value) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setBigEye(value);
        } else {
        }
    }

    public int getBigEye() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().getBigEye();
        } else {
            return 0;
        }
    }

    public void setThinFace(int value) {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setThinFace(value);
        } else {
        }
    }

    public int getThinFace() {
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            return gtvRecorderViewRender.getCameraVideoObject().getThinFace();
        } else {
            return 0;
        }
    }

    OnTextureListener onTextureListener;

    public void setTexutreListener(OnTextureListener listener) {
        this.onTextureListener = listener;
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setTexutreListener(listener);
        }
    }

    OnYUVDataListener onYUVDataListener;

    public void setYUVDataListener(OnYUVDataListener listener) {
        this.onYUVDataListener = listener;
        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().setYUVDataListener(listener);
        }
    }

    public int getMusicPosition() {
        int pos = musicStartTime;
        for (int i = 0; i < videoInfo.getCount(); ++i) {
            pos += (int) (videoInfo.getVideoClipList().get(i).getDuration() * videoInfo.getVideoClipList().get(i).getSpeed());
        }
        return pos;
    }

    public boolean takePicture(ITakePictureCallback cb) {

        if (gtvRecorderViewRender != null && gtvRecorderViewRender.getCameraVideoObject() != null) {
            gtvRecorderViewRender.getCameraVideoObject().takePicture(cb);
        } else {
            return false;
        }

        return true;
    }
}
