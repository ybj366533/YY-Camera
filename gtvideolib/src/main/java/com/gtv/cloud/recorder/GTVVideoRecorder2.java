package com.gtv.cloud.recorder;

import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;

import com.gtv.cloud.impl.GTVMP4Help;
import com.gtv.cloud.impl.GTVVideoSegments;
import com.gtv.cloud.impl.setting.AVStreamSetting;
import com.gtv.cloud.impl.utils.GTVFileUtils;
import com.gtv.cloud.stream.GTVVideoRecordStreamer;
import com.gtv.cloud.utils.GTVMusicHandler;
import com.gtv.cloud.utils.LogUtils;

import java.io.File;

/**
 * Created by gtv on 2018/1/26.
 */

public class GTVVideoRecorder2 implements IGTVVideoRecorder2 {

    private static final String TAG = "Recorder2";

    private static final int MAX_RECORD_DURATION = 15 * 1000;

    private GLSurfaceView glSurfaceView;


    //private GTVRecorderViewRender gtvRecorderViewRender;

    private String workFolder;

    private int bitrate;
    private int width;
    private int height;

    private int maxDuration;

    GTVVideoRecordStreamer videoRecordStreamer;

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

    private GTVVideoSegments gtvVideoSegments;
    private GTVVideoInfo gtvVideoInfo;

    private String currentRecFileName;

    HandlerThread handlerThread;
    private Handler handler = null;

    // 有配乐时，配乐是否录制到文件
    // true 配乐作为声音源 直接录制到文件
    // false 录制声音为0，配乐文件作为设定传递给编辑画面（等同于编辑画面的配乐选择）
    private boolean musicRecordToFile = false;

    private boolean onMaxDurationSent = false;      // 一次录制 只发送一次

    GTVVideoRecordStreamer.RecordCallback callback = new GTVVideoRecordStreamer.RecordCallback() {
        //private boolean onMaxDurationSent = false;        // 因为可能多次删除，多次录制，改为全体变量。
        @Override
        public void onProgress(int duration) {

            if (GTVVideoRecorder2.this.recordCallback != null) {
                GTVVideoRecorder2.this.recordCallback.onProgress(gtvVideoInfo.getTotalDuration()+duration, gtvVideoInfo);
                if ((gtvVideoInfo.getTotalDuration()+duration >= maxDuration) && (onMaxDurationSent == false)) {
                    LogUtils.LOGI(TAG, "on max duration ");
                    GTVVideoRecorder2.this.recordCallback.onMaxDuration();
                    onMaxDurationSent = true;
                }
            }
        }

        @Override
        public void onComplete(int duration) {
            LogUtils.LOGI(TAG, "record complete, duration: " + duration);
            if (duration < 100) {
                if (GTVVideoRecorder2.this.recordCallback != null) {
                    GTVVideoRecorder2.this.recordCallback.onRecordComplete(false, duration);
                }
            } else {
                if (GTVVideoRecorder2.this.recordCallback != null) {
                    GTVVideoRecorder2.this.recordCallback.onRecordComplete(true, duration);
                }

                gtvVideoSegments.addVideoSegInfo(currentRecFileName, (int)duration, (float)GTVVideoRecorder2.this.recordSpeed);
                gtvVideoInfo = gtvVideoSegments.getVideoSegInfo();
                gtvVideoInfo.dump();
            }

        }

        @Override
        public void onError(int errorCode) {

        }
    };

    public GTVVideoRecorder2() {
        this.bitrate = 20*1000000;
        this.width = 576;
        this.height = 1024;
        recordSpeed = 1.0;
        recordSpeedType = SpeedType.STANDARD;
        maxDuration = MAX_RECORD_DURATION;

        musicReseekFlag = false;

        HandlerThread handlerThread = new HandlerThread("recordtask");
        handlerThread.start();

//这里获取到HandlerThread的runloop
        handler = new Handler(handlerThread.getLooper());
    }

//    public void init(GLSurfaceView glSurfaceView, String workFolder, RecordCallback recordCallback){
//        init(glSurfaceView, workFolder, CameraType.BACK, recordCallback);
//
//    }

    //public void init(GLSurfaceView glSurfaceView, String workFolder, CameraType cameraId, RecordCallback recordCallback){
    public void init(String workFolder, RecordCallback recordCallback){

        this.glSurfaceView =glSurfaceView;
        this.workFolder = workFolder;


        this.recordCallback = recordCallback;
        
        LogUtils.LOGI(TAG, "init Ver. 1.87");

//        int camId = 0;
//        if(cameraId == CameraType.FRONT) {
//            camId = 1;
//        }

//        gtvRecorderViewRender = new GTVRecorderViewRender(glSurfaceView, camId,this.recordCallback);

        handler.post(new Runnable() {
            @Override
            public void run() {
                init_impl();
            }
        });



    }
    private void init_impl(){

//        this.glSurfaceView =glSurfaceView;
//        this.workFolder = workFolder;
//
//        this.recordCallback = recordCallback;

        {
            if(!workFolder.endsWith(""+File.separator)) {
                workFolder += ""+File.separator;
            }
            GTVFileUtils.createDirectoryIfNeed(workFolder);
        }

//        gtvRecorderViewRender = new GTVRecorderViewRender(glSurfaceView, this.recordCallback);

        GTVFileUtils.createDirectoryIfNeed(this.workFolder);

        // todo 创建失败？
        // todo 如果传入的路径没有/

        gtvVideoSegments = new GTVVideoSegments(this.workFolder + "/data.txt");
        gtvVideoInfo = gtvVideoSegments.getVideoSegInfo();
        if (recordCallback != null) {
            recordCallback.onPrepared(gtvVideoInfo);        // 如果已经执行了destroy，还要发？可能activit已经不存在了。
        }

        LogUtils.LOGI(TAG, "init end");

    }

//    public void startPreview(){
//        LogUtils.LOGI(TAG, "startPreview");
//        if (gtvRecorderViewRender != null) {
//            gtvRecorderViewRender.onResume();
//        }
//    }
//
//    public void stopPreview(){
//        LogUtils.LOGI(TAG, "stopPreview");
//        if (gtvRecorderViewRender != null) {
//            gtvRecorderViewRender.onPause();
//        }
//    }


    public void setVideoSize(int width, int height) {
        this.width = width;
        this.height = height;

        if(this.width % 16 !=0) {
            this.width += 16;
            this.width -= this.width % 16;
        }

        if(this.height % 16 !=0) {
            this.height += 16;
            this.height -= this.height % 16;
        }

        LogUtils.LOGI(TAG, "Video Size is set to " + this.width + " " + this.height);
    }

    public int getVideoWidth(){
        return this.width;
    }
    public int getVideoHeight(){
        return this.height;
    }

    public void  setRecordSpeed(SpeedType speedType){
        LogUtils.LOGI(TAG, "setRecordSpeed " + speedType);
        this.recordSpeedType = speedType;
        if  (speedType == SpeedType.SLOW) {
            this.recordSpeed = 0.5;
        } else if (speedType == SpeedType.FAST) {
            this.recordSpeed = 2;
        } else {
            this.recordSpeed = 1;
        }
    }

    public SpeedType getRecordSpeed(){
        return this.recordSpeedType;
    }

    public void setMaxDuration(int maxDurationMili) {
        LogUtils.LOGI(TAG, "setMaxDuration " + maxDurationMili);
        maxDuration = maxDurationMili;
    }

    public int getMaxDuration(){
        return this.maxDuration;
    }

//    public void prepareRecord(){
//        gtvVideoInfo = gtvVideoSegments.getVideoSegInfo();
//    }

    public void deleteLastVideoClip(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                deleteLastVideoClip_impl();
            }
        });
    }
    private void deleteLastVideoClip_impl(){

        LogUtils.LOGI(TAG, "deleteLastVideoClip start");

        if(gtvVideoInfo.getCount()< 1) {
            return;
        }

        musicReseekFlag = true;     // 配乐的播放位置需要重新seek

        gtvVideoSegments.removeLastVideoSeg();
        gtvVideoInfo = gtvVideoSegments.getVideoSegInfo();
        gtvVideoInfo.dump();

        if (GTVVideoRecorder2.this.recordCallback != null) {
            GTVVideoRecorder2.this.recordCallback.onProgress(gtvVideoInfo.getTotalDuration(), gtvVideoInfo);
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
    private void deleteAllVideoClips_impl(){

        LogUtils.LOGI(TAG, "deleteAllVideoClips start");

        File dir = new File(this.workFolder);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                File file =new File(dir, children[i]);
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        } else {
            //impossible
        }

        musicReseekFlag = true;     // 配乐的播放位置需要重新seek

        gtvVideoInfo = gtvVideoSegments.getVideoSegInfo();
        gtvVideoInfo.dump();

        if (GTVVideoRecorder2.this.recordCallback != null) {
            GTVVideoRecorder2.this.recordCallback.onProgress(gtvVideoInfo.getTotalDuration(), gtvVideoInfo);
        }

        LogUtils.LOGI(TAG, "deleteAllVideoClips end");
    }

    public int getDuration(){
        // todo 不包含当前片段
        return gtvVideoInfo.getTotalDuration();
    }

    private String getClipName() {
        int clipNum = gtvVideoInfo.getCount();
        clipNum++;
        return "clip_"+clipNum+".mp4";
    }
    // todo  return value ?

    public void startRecord(final EGLContext currentContext){
        handler.post(new Runnable() {
            @Override
            public void run() {
                startRecord_impl(currentContext);
            }
        });
    }
    private void startRecord_impl(EGLContext currentContext){


        LogUtils.LOGI(TAG, "startRecord");

//        if (gtvRecorderViewRender == null || gtvRecorderViewRender.getEGLContext() == null || gtvRecorderViewRender.getVideoResource() == null) {
//            // todo recall?
//            LogUtils.LOGI(TAG, "startRecord failed");
//            return;
//        }

        // 提前检查是否超限，避免生成无效文件。
        if(gtvVideoInfo.getTotalDuration()>= maxDuration) {
            LogUtils.LOGI(TAG, "startRecord failed: max duration");
            if (GTVVideoRecorder2.this.recordCallback != null) {
                GTVVideoRecorder2.this.recordCallback.onMaxDuration();
            }
            return;
        }

        onMaxDurationSent = false;      // 每次拍摄都有机会发送一次onmax

        //clipNum++;
        currentRecFileName = getClipName();         // todo
        String recFileName = this.workFolder + currentRecFileName;
        //videoRecordStreamer = (GTVVideoRecordStreamer) AVStreamFactory.getInstance().createVideoRecordAVStreamer(gtvRecorderViewRender.getEGLContext(), recFileName , this.width, this.height, this.bitrate);


        AVStreamSetting setting = AVStreamSetting.settingForVideoRecord(bitrate, width, height, true);
        videoRecordStreamer = new GTVVideoRecordStreamer(recFileName, setting, currentContext);

        //videoRecordStreamer.setInputVideoSource(gtvRecorderViewRender.getVideoResource());

        videoRecordStreamer.openStream(this.bgMusicPath==null? false:true, this.recordSpeed, musicRecordToFile, callback);

        if (bgMusicPath != null) {
            if (firstTimeRecord == true) {
                if(musicRecordToFile == true) {
                    GTVMusicHandler.getInstance().playMusic(bgMusicPath, videoRecordStreamer);
                } else {
                    GTVMusicHandler.getInstance().playMusic(bgMusicPath, null);
                }

                GTVMusicHandler.getInstance().seekTo(getMusicPosition());
                firstTimeRecord = false;
            } else {
                if(musicReseekFlag == true) {
                    GTVMusicHandler.getInstance().seekTo(getMusicPosition());
                    musicReseekFlag = false;
                }
                if(musicRecordToFile == true) {
                    GTVMusicHandler.getInstance().resumePlay(videoRecordStreamer);
                } else {
                    GTVMusicHandler.getInstance().resumePlay(null);
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
    private void pauseRecord_impl(){

        LogUtils.LOGI(TAG, "pauseRecord");
        GTVMusicHandler.getInstance().pausePlay();
        if (videoRecordStreamer != null) {
            videoRecordStreamer.closeStream();
            videoRecordStreamer = null;
            //gtvVideoSegments.addVideoSegInfo(currentRecFileName, 3000);     //todo

        } else {
            // todo 回调？
        }

        LogUtils.LOGI(TAG, "pauseRecord end");
    }

    public void resumeRecord(final EGLContext currentContext){
        startRecord(currentContext);
    }

    public void stopRecord(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                stopRecord_impl();
            }
        });
    }
    private void stopRecord_impl(){

        LogUtils.LOGI(TAG, "stopRecord");

        // 因为可能 从编辑界面返回重新录制，所以，音乐也只有暂停
        //GTVMusicHandler.getInstance().stopPlay(); // todo 如果声音停的慢，可能中途encoder会已经被停止
        GTVMusicHandler.getInstance().pausePlay();
        if (videoRecordStreamer != null) {
            videoRecordStreamer.closeStream();
            videoRecordStreamer = null;
            //gtvVideoSegments.addVideoSegInfo(currentRecFileName, 3000);     //todo
        } else {
            // 【下一步】按钮时，需要 录像结束的回调来触发 导出。
            if (GTVVideoRecorder2.this.recordCallback != null) {
                GTVVideoRecorder2.this.recordCallback.onRecordComplete(false, 0);
            }
        }

        LogUtils.LOGI(TAG, "stopRecord end");
        //todo 合成

    }

    public int writeVideoFrame(final int textureId, final int textureWidth, final int textureHeight, final boolean flip, final long timestampNanos){
//        if(videoRecordStreamer != null) {
//            videoRecordStreamer.newGLTextureAvailable(null, textureId, textureWidth, textureHeight);
//        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if(videoRecordStreamer != null) {
                    videoRecordStreamer.writeVideoFrame(textureId, textureWidth, textureHeight,flip,timestampNanos);
                }
            }
        });
        return 0;
    }

    public GTVVideoInfo getVideoInfo() {
        return gtvVideoInfo;
    }

    public void exportTopath(final String path, final String pathRev){
        handler.post(new Runnable() {
            @Override
            public void run() {
                // todo 如果路径不存在
                exportTopath_impl(path, pathRev);
            }
        });
    }

    public void exportTopath_impl(final String path, final String pathRev){

        LogUtils.LOGI(TAG, "export start");

        String tempFolder = null;

        {
            int lastIndex = path.lastIndexOf(File.separator);
            if (lastIndex < 0) {
                LogUtils.LOGI(TAG, "path error");
                return;
            }
            String dirPath = path.substring(0, lastIndex + 1);
            GTVFileUtils.createDirectoryIfNeed(dirPath);
            tempFolder = dirPath;
        }

        {
            int lastIndex = pathRev.lastIndexOf(File.separator);
            if (lastIndex < 0) {
                LogUtils.LOGI(TAG, "path error");
                return;
            }
            String dirPath = pathRev.substring(0, lastIndex + 1);
            GTVFileUtils.createDirectoryIfNeed(dirPath);
        }



        GTVVideoInfo gtvVideoInfo = getVideoInfo();
        if (gtvVideoInfo.getCount() > 0 ) {
            int count = gtvVideoInfo.getCount();
            String[] fileList = new String[count];
            for (int i = 0; i < count; ++i) {
                fileList[i] = this.workFolder + gtvVideoInfo.getVideoClipList().get(i).getFileName();
            }
            //int ret = GTVMP4Help.Mp4VideoClipsMerge(fileList, path, pathRev, GTVFileUtils.getTempFolder(glSurfaceView.getContext()));
            int ret = GTVMP4Help.Mp4VideoClipsMerge(fileList, path, pathRev, tempFolder);

            // 为了界面测试
//            try {
//                Thread.sleep(5*1000);
//            } catch (Exception e) {
//
//            }


            if(recordCallback != null) {
                recordCallback.onExportComplete(ret == 0 ? true : false);
            }


        } else {
            if(recordCallback != null) {
                recordCallback.onExportComplete(false);
            }

            LogUtils.LOGI(TAG, "export error: no clip");
        }

        LogUtils.LOGI(TAG, "export end");
//        int count = gtvVideoSegments.getSegmentCount()
//        {
//            //test
//
//            LogUtils.DebugLog("GTVMP4Help_Mp4VideoClipsMerge", "before");
//            String[] fileList = {this.workFolder+"1.mp4", this.workFolder+"2.mp4"};
//            GTVMP4Help.Mp4VideoClipsMerge(fileList, this.workFolder+"test.mp4");
//            LogUtils.DebugLog("GTVMP4Help_Mp4VideoClipsMerge", "after");
//        }
    }

    private void exportTest() {
        String[] fileList = new String[3];
        fileList[0] = "/storage/emulated/0/DCIM/SV_20180208_235507/clip_1.mp4";
        fileList[1] = "/storage/emulated/0/DCIM/SV_20180208_235507/clip_2.mp4";
        fileList[2] = "/storage/emulated/0/DCIM/SV_20180208_235507/clip_3.mp4";
        //GTVMP4Help.Mp4VideoClipsMerge(fileList, "/storage/emulated/0/DCIM/SV_20180208_235507/aa.mp4", "/storage/emulated/0/DCIM/SV_20180208_235507/aar.mp4");
    }

    public void destroy(){

        GTVMusicHandler.getInstance().stopPlay(); // todo 如果声音停的慢，可能中途encoder会已经被停止

//        if (gtvRecorderViewRender != null) {
//            gtvRecorderViewRender.onDestroy();
//        }

        if (handlerThread != null) {
            handlerThread.quit();
        }

//        if (gtvVideoSegments != null) {
//            gtvVideoSegments.close();
//        }
    }




    public void setMusicPath(String musicPath){
        this.setMusicPath(musicPath, 0);

    }

    public void setMusicPath(String musicPath, int startTime){
        this.bgMusicPath = musicPath;
        this.musicStartTime = startTime;
        this.firstTimeRecord = true;    // 要求：录制一个以上片段删除之后可以重新设置音乐。（APP侧需要防止在录制期间多次设置音乐，只有最后一次会被传给编辑页面）
    }

   public String getMusicPath(){
        return this.bgMusicPath;
   }
   public int getMusicStartTimeMili(){
       return this.musicStartTime;
   }



    public int getMusicPosition(){
        int pos = musicStartTime;
        for (int i = 0; i< gtvVideoInfo.getCount(); ++i) {
            pos += (int)(gtvVideoInfo.getVideoClipList().get(i).getDuration() *    gtvVideoInfo.getVideoClipList().get(i).getSpeed());
        }
        return pos;
    }
}
