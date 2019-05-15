package com.gtv.cloud.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;

import com.gtv.cloud.core.GTVEffectFilterTypeManage;
import com.gtv.cloud.core.GTVPlayerMerge;
import com.gtv.cloud.core.GTVPlayerPreview;
import com.gtv.cloud.impl.GTVMP4Help;
import com.gtv.cloud.impl.setting.AVStreamSetting;
import com.gtv.cloud.impl.utils.GTVFileUtils;
import com.gtv.cloud.stream.GTVVideoRecordStreamer;
import com.gtv.cloud.impl.utils.DispRect;
import com.gtv.cloud.utils.LogUtils;
import com.gtv.cloud.widget.GTVEditorViewRender;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class GTVVideoEditor implements IGTVVideoEditor {

    private static final String TAG = "Editor";

    private static final String SETTING_JSON = "setting.json";

    private GLSurfaceView glSurfaceView;
    private EditCallback editCallback;
    private GTVEditorViewRender gtvEditorViewRender;

    GTVVideoRecordStreamer videoRecordStreamer;

    private String videoPath;

    private double recordSpeed;

    private int bitrate;
    private int gop;

    private int width;
    private int height;

    String tagFile = null;

    GTVPlayerPreview gtvPlayerPreview;
    GTVPlayerMerge gtvPlayerMerge;

    // 合成用参数
    private boolean slowMotionEnable = false;
    private int slowMotionStartTime = 0;
    private int slowMotionEndTime = 0;
    private String musicPath = null;
    private int musicStartTimeMili = 0;
    GTVEffectFilterTypeManage effectFilterTypeManage = new GTVEffectFilterTypeManage();

    private int cropRangeStartTime = 0;
    private int cropRangeEndTime = 0;

    private boolean x264Mode = true;        // 因为需要输出封面设定用的全I帧视频，这个已不能设置为false了。

    private boolean composeCancelFlag = false;  // 用于合成取消

    private String editWorkFolder;     // 现在用于保存编辑设定的json文件的目录
    private boolean reloadSettingFlag;

    public GTVVideoEditor(){
        this.bitrate = 4000000;
        this.width = 368;
        this.height = 640;
        recordSpeed = 1.0;
        this.gop = 15;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getBitrate() {
        return this.bitrate;
    }

    public void setGOP(int gop){
        this.gop = gop;
    }
    public int getGOP() {
        return this.gop;
    }


    public void init(final GLSurfaceView glSurfaceView, final String videoPath, EditCallback editCallback){

        init(glSurfaceView, videoPath, null, false, editCallback);

    }

    public void init(final GLSurfaceView glSurfaceView, final String videoPath, final String editWorkFolder, boolean reloadSettingFlag, EditCallback editCallback){

        LogUtils.LOGI(TAG, "init " + videoPath + " " + reloadSettingFlag);
        this.glSurfaceView =glSurfaceView;
        this.videoPath = videoPath;

        this.editCallback = editCallback;

        gtvEditorViewRender = new GTVEditorViewRender(glSurfaceView, videoPath);

        this.editWorkFolder = editWorkFolder;
        this.reloadSettingFlag = reloadSettingFlag;

        setVideoPath(videoPath);

    }

    public void setVideoPath(String videoPath) {

        LogUtils.LOGI(TAG, "setVideoPath " + videoPath);

        this.videoPath = videoPath;
        gtvEditorViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {
                if(gtvPlayerPreview != null) {
                    //gtvPlayerPreview.closeVideo();
                    gtvPlayerPreview.destroy();
                }

                if(editWorkFolder != null)
                {
                    if(!editWorkFolder.endsWith(""+File.separator)) {
                        editWorkFolder += ""+File.separator;
                    }
                    GTVFileUtils.createDirectoryIfNeed(editWorkFolder);
                }

                // 载入编辑设定值
                loadSetting();

                gtvPlayerPreview = new GTVPlayerPreview(glSurfaceView, gtvEditorViewRender, GTVVideoEditor.this.videoPath);
                gtvPlayerPreview.setEffectFilterSetting(effectFilterTypeManage);
                gtvPlayerPreview.setMusicVolume(_musicV);
                gtvPlayerPreview.setOrigAudioVolume(_originV);
                gtvPlayerPreview.init(gtvEditorViewRender.getPlayerVideoObject(),gtvEditorViewRender.getPlayerVideoObject(), new GTVPlayerPreview.PlayCallback() {
                    @Override
                    public void onPrepared() {
                        if (gtvPlayerPreview.getVideoWidth() !=0 &&
                                gtvPlayerPreview.getVideoHeight() != 0) {
                            width = gtvPlayerPreview.getVideoWidth();
                            height = gtvPlayerPreview.getVideoHeight();
                        }
                        GTVVideoEditor.this.editCallback.onInitReady();
                        gtvEditorViewRender.getPlayerVideoObject().updateVideoSizeFromStream(width, height);

                        LogUtils.LOGI(TAG, "onPrepared " + width + " " + height);
                    }

                    @Override
                    public void onCompletion() {
                        GTVVideoEditor.this.editCallback.onPlayComplete();
                    }
                });
                if(musicPath != null) {
                    gtvPlayerPreview.setMusicPath(GTVVideoEditor.this.musicPath, GTVVideoEditor.this.musicStartTimeMili);
                }
                if(cropRangeStartTime!=0 || cropRangeEndTime !=0) {
                    gtvPlayerPreview.setVideoCropRange(cropRangeStartTime,cropRangeEndTime);
                }
                if(slowMotionEnable == true) {
                    gtvPlayerPreview.setSlowMotionEnable(slowMotionEnable);
                    gtvPlayerPreview.setSlowMotionStartTime(cropRangeStartTime);
                    gtvPlayerPreview.setSlowMotionEndTime(cropRangeEndTime);
                }
                if(muteFlag == true) {
                    gtvPlayerPreview.setOrigAudioVolume(GTVVideoEditor.this.muteFlag == true ? 0 : 1.0f);
                }
            }
        });
    }

    public void startPreview(){ //onresume

        LogUtils.LOGI(TAG, "startPreview ");

        if (gtvEditorViewRender != null) {
            gtvEditorViewRender.myQueueEvent(new Runnable() {
                @Override
                public void run() {
                    gtvEditorViewRender.onResume();

                    if(gtvPlayerPreview != null) {
                        //gtvPlayerPreview.resumeVideo();
                        //gtvPlayerPreview.playStart();     // 不主动打开
                    }
                }
            });

        }
    }

    public void stopPreview(){  //onpause

        LogUtils.LOGI(TAG, "stopPreview ");

        if (gtvEditorViewRender != null) {
            gtvEditorViewRender.myQueueEvent(new Runnable() {
                @Override
                public void run() {
                    gtvEditorViewRender.onPause();
                    if(gtvPlayerPreview != null) {
                        //gtvPlayerPreview.pauseVideo();
                        gtvPlayerPreview.playPause();
                    }
                }
            });

        }
    }

    public void destroy(){

        LogUtils.LOGI(TAG, "destroy ");

        if (gtvEditorViewRender != null) {
            gtvEditorViewRender.onDestroy();
        }
        // 预览播放  合成播放是否需要释放
        if (gtvPlayerPreview != null) {
            gtvPlayerPreview.destroy();
        }

        if(gtvPlayerMerge != null) {
            gtvPlayerMerge.destroy();
        }
    }

//    private void setEffectFilterType(EffectType effectType) {
//        if (gtvEditorViewRender!= null && gtvEditorViewRender.getPlayerVideoObject() != null) {
//            gtvEditorViewRender.getPlayerVideoObject().setMagicFilterType(effectType);
//        }
//    }
//
//    private EffectType getEffectFilterType(){
//        if (gtvEditorViewRender!= null && gtvEditorViewRender.getPlayerVideoObject() != null) {
//            return gtvEditorViewRender.getPlayerVideoObject().getMagicFilterType();
//        }
//        return EffectType.EFFECT_NO;
//    }

//    private void addEffectFilter(int startTime, int endTime,EffectType effectType) {
//
//        if (effectFilterTypeManage != null) {
//            effectFilterTypeManage.addEffectFilter(startTime,endTime,effectType);
//        }
//    }

    public boolean startVideoEffect(EffectType effectType){
        int pos = this.getCurrentPosition();
        effectFilterTypeManage.startVideoEffect(effectType, pos);
        return true;
    }
    public boolean stopVideoEffect(EffectType effectType){
        int pos = this.getCurrentPosition();
        effectFilterTypeManage.stopVideoEffect(effectType, pos);
        return true;
    }
    public boolean removeLastVideoEffect(){
        effectFilterTypeManage.removeLastVideoEffect();
        return true;
    }
    public boolean clearAllVideoEffect(){
        effectFilterTypeManage.clearAllVideoEffect();
        return true;
    }

    public List<GTVideoEffectInfo> getVideoEffectList(){
        List<GTVideoEffectInfo> list = new ArrayList<>();
        for(int i = 0; i < effectFilterTypeManage.getVideoEffectInfoList().size(); ++i) {
            GTVideoEffectInfo e = effectFilterTypeManage.getVideoEffectInfoList().get(i);
            GTVideoEffectInfo gtVideoEffectInfo = new GTVideoEffectInfo();

            gtVideoEffectInfo.setEffectType(e.getEffectType());
            gtVideoEffectInfo.setStartTime(e.getStartTime());
            gtVideoEffectInfo.setEndTime(e.getEndTime());
            list.add(gtVideoEffectInfo);
        }
        return list;
    }

    public void setVideoEffectList(List<GTVideoEffectInfo> gtVideoEffectInfoList){
        List<GTVideoEffectInfo> list = new ArrayList<>();
        if(gtVideoEffectInfoList != null) {
            for(int i = 0; i < gtVideoEffectInfoList.size(); ++i) {
                GTVideoEffectInfo e = gtVideoEffectInfoList.get(i);
                GTVideoEffectInfo gtVideoEffectInfo = new GTVideoEffectInfo();

                gtVideoEffectInfo.setEffectType(e.getEffectType());
                gtVideoEffectInfo.setStartTime(e.getStartTime());
                gtVideoEffectInfo.setEndTime(e.getEndTime());
                list.add(gtVideoEffectInfo);
            }
        }
        effectFilterTypeManage.setVideoEffectInfoList(list);
    }

    public void setSlowPlayTime(int startTime, int endTime){
        if (startTime == 0 && endTime == 0) {
            if (gtvPlayerPreview != null) {
                gtvPlayerPreview.setSlowMotionEnable(false);
            }
            this.slowMotionEnable = false;
        } else {
            if (gtvPlayerPreview != null) {
                gtvPlayerPreview.setSlowMotionEnable(true);
                gtvPlayerPreview.setSlowMotionStartTime(startTime);
                gtvPlayerPreview.setSlowMotionEndTime(endTime);

            }

            this.slowMotionEnable = true;
            this.slowMotionStartTime = startTime;
            this.slowMotionEndTime = endTime;
        }
    }

    public void setVideoCropRange(int startTime, int endTime) {

        LogUtils.LOGI(TAG, "setVideoCropRange " + startTime + " " + endTime);

        this.cropRangeStartTime = startTime;
        this.cropRangeEndTime = endTime;

        if (gtvPlayerPreview != null) {
            gtvPlayerPreview.setVideoCropRange(startTime, endTime);
        }
    }

    public int[] getVideoCropRange(){
        int [] range = new int[2];
        range[0] = this.cropRangeStartTime;
        range[1] = this.cropRangeEndTime;
        return range;
    }

    public int getDuration() {

        if (gtvPlayerPreview != null) {
            return gtvPlayerPreview.getDuration();
        }

        return 0;
    }

    public int getCurrentPosition () {

        if (gtvPlayerPreview != null) {
            return gtvPlayerPreview.getCurrentPosition();
        }

        return 0;
    }

    public void seekTo(int msec) {

        if (gtvPlayerPreview != null) {
            gtvPlayerPreview.seekTo(msec);
        }
        return;
    }

    public void playPause() {

        if (gtvPlayerPreview != null) {
            gtvPlayerPreview.playPause();
        }
    }

    // tood  = play resume
    public void playStart() {

        if(gtvPlayerPreview != null) {
            gtvPlayerPreview.playStart();
        }
    }

    private int getComposeDuration() {
        int start = cropRangeStartTime;
        int end = cropRangeEndTime;
        if(start <=0 || start > getDuration()) {
            start = 0;
        }

        if (end <= 0 || end > getDuration()) {
            end = getDuration();
        }
        return (end - start +1);
    }

    private boolean isCompsing = false;
    public void startCompose(final String filePath){
        startCompose(filePath, null);
    }
    //废弃
    public void startCompose(final String filePath, final String keyFrameVideoPath){



        gtvEditorViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {

                LogUtils.LOGI(TAG, "startCompose");
                if (gtvEditorViewRender == null || gtvEditorViewRender.getEGLContext() == null || gtvEditorViewRender.getPlayerVideoObject() == null) {
                    // todo recall?
                    LogUtils.LOGI(TAG, "startCompose failed."); // 用法错误
                    return;
                }

                int lastIndex = filePath.lastIndexOf(File.separator);
                if (lastIndex < 0) {
                    LogUtils.LOGI(TAG, "path error");
                    return;
                }
                String dirPath = filePath.substring(0, lastIndex + 1);
                GTVFileUtils.createDirectoryIfNeed(dirPath);

                String fileForAllIFrame = null;
                if(keyFrameVideoPath == null) {
                    lastIndex = videoPath.lastIndexOf(File.separator);
                    fileForAllIFrame = videoPath.substring(0, lastIndex + 1) + "for_cover.mp4";
                } else {

                    lastIndex = keyFrameVideoPath.lastIndexOf(File.separator);
                    if (lastIndex < 0) {
                        LogUtils.LOGI(TAG, "path error");
                        return;
                    }
                    dirPath = keyFrameVideoPath.substring(0, lastIndex + 1);
                    GTVFileUtils.createDirectoryIfNeed(dirPath);

                    fileForAllIFrame = keyFrameVideoPath;
                }

                if(isCompsing == true) {
                    LogUtils.LOGE(TAG, "already in composing");
                    return;
                }
                isCompsing = true;
                composeCancelFlag = false;

                AVStreamSetting setting = AVStreamSetting.settingForVideoRecord(x264Mode?6000000:bitrate, width, height, x264Mode?true:false);
                videoRecordStreamer = new GTVVideoRecordStreamer(fileForAllIFrame, setting, gtvEditorViewRender.getEGLContext());

                videoRecordStreamer.setInputVideoSource(gtvEditorViewRender.getPlayerVideoObject());

                final String filePathT = fileForAllIFrame;


                final int composeDuration = getComposeDuration();
                // music=true; 不需要创建录音
                // recordspeed 固定1
                // openstream的callback 要统一成一个独立的
                // todo 有背景音乐改如何处理
                int ret = videoRecordStreamer.openStream(true, GTVVideoEditor.this.recordSpeed, true, new GTVVideoRecordStreamer.RecordCallback() {
                    @Override
                    public void onProgress(int duration) {
                        // compose callback
                        if (GTVVideoEditor.this.editCallback != null) {
                            int progress = duration * 100 / composeDuration;//GTVVideoEditor.this.getDuration();
                            if (progress > 99) {
                                progress = 99;
                            }
                            if(x264Mode == true) {
                                //progress /= 2;
                                progress *= 0.7;
                            }
                            GTVVideoEditor.this.editCallback.onProgress(progress);
                        }
                    }

                    @Override
                    public void onComplete(int duration) {
//                        if(composeCancelFlag == true) {
//                            GTVFileUtils.deleteFile(filePathT);
//                            return;
//                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int ret = 0;
                                if(x264Mode == true && composeCancelFlag == false) {

//                                    // 原文件重命名为临时文件
//                                    String fileTmep = filePathT.substring(0, filePathT.length()-4) + "_temp2" + ".mp4";
//                                    File from = new File(filePathT);
//                                    File to = new File(fileTmep);
//                                    from.renameTo(to);

                                    //Log.e("hhhhhhhhh", "start ");
                                    // 20180506 gop 250-->25, 因为要用这个视频抽帧，避免抽帧结果太难看 （但是和规格不符合）
                                    LogUtils.LOGI(TAG," x264 start");
                                    ret = GTVMP4Help.MP4FileTranscodeByX264(filePathT, filePath, GTVVideoEditor.this.gop,new GTVMP4Help.OnProgressListener() {
                                        @Override
                                        public int onProgress(int progress) {
                                            //Log.e("hhhhhhhhh", " " +progress);
                                            if (GTVVideoEditor.this.editCallback != null) {
                                                int progress2 = 70 + (int)(progress *0.3);
                                                if(progress2>99) {
                                                    progress2 = 99;
                                                }
                                                GTVVideoEditor.this.editCallback.onProgress(progress2);
                                            }

                                            if(composeCancelFlag == true) {
                                                return 1;
                                            }

                                            return 0;

                                        }
                                    });
                                    LogUtils.LOGI(TAG," x264 end");


                                    if(keyFrameVideoPath == null) {
                                        File fileDel = new File(filePathT);
                                        if (fileDel.exists()) {
                                            fileDel.delete();
                                            LogUtils.LOGI(TAG," delete file " + filePathT);
                                        }
                                    }

                                }


                                if(composeCancelFlag == true) {
                                    GTVFileUtils.deleteFile(filePath);
                                    GTVFileUtils.deleteFile(filePathT);
                                    ret = 1;
                                }

                                // 恢复预览界面
                                gtvEditorViewRender.setDisPlayToScreenFlag(true);
                                gtvPlayerPreview.seekTo(0);
                                gtvPlayerPreview.playStart();

                                LogUtils.LOGI(TAG, "compose finish " + (ret == 0? 0: 1));

                                if (GTVVideoEditor.this.editCallback != null) {
                                    GTVVideoEditor.this.editCallback.onComposeFinish(ret == 0? 0: 1);
                                }

                                isCompsing = false;


                            }
                        }).start();


                    }

                    @Override
                    public void onError(int errorCode) {
                        if (GTVVideoEditor.this.editCallback != null) {
                            GTVVideoEditor.this.editCallback.onError(errorCode);
                        }
                    }
                });

                if (ret != 0) {
                    if (GTVVideoEditor.this.editCallback != null) {
                        GTVVideoEditor.this.editCallback.onError(100);
                    }
                    return;
                }

                saveSetting();

                gtvPlayerPreview.playPause();
                gtvEditorViewRender.setDisPlayToScreenFlag(false);
                gtvPlayerMerge = new GTVPlayerMerge(glSurfaceView, gtvEditorViewRender, videoPath);
                gtvPlayerMerge.setSlowMotionEnable(GTVVideoEditor.this.slowMotionEnable);
                gtvPlayerMerge.setSlowMotionStartTime(GTVVideoEditor.this.slowMotionStartTime);
                gtvPlayerMerge.setSlowMotionEndTime(GTVVideoEditor.this.slowMotionEndTime);
                gtvPlayerMerge.setVideoCropRange(cropRangeStartTime,cropRangeEndTime);
                gtvPlayerMerge.setMusicPath(GTVVideoEditor.this.musicPath, GTVVideoEditor.this.musicStartTimeMili);
                gtvPlayerMerge.setEffectFilterSetting(GTVVideoEditor.this.effectFilterTypeManage);
                gtvPlayerMerge.setRawAudioCallback(videoRecordStreamer);
                //gtvPlayerMerge.setOrigAudioVolume(GTVVideoEditor.this.muteFlag == true ? 0 : 1.0f);
                gtvPlayerMerge.setOrigAudioVolume(GTVVideoEditor.this.muteFlag == true ? 0 : _originV);
                gtvPlayerMerge.setMusicVolume(_musicV);

                // 设定动画
                gtvEditorViewRender.getPlayerVideoObject().setAnimation(animationPrefix, animationFolder, animImageInterval,animationRect);

                gtvPlayerMerge.startMerge(gtvEditorViewRender.getPlayerVideoObject(), gtvEditorViewRender.getPlayerVideoObject(), new GTVPlayerMerge.PlayCallback() {
                    @Override
                    public void onPrepared() {
                        if (gtvPlayerMerge.getVideoWidth() !=0 &&
                                gtvPlayerMerge.getVideoHeight() != 0) {
                            width = gtvPlayerMerge.getVideoWidth();
                            height = gtvPlayerMerge.getVideoHeight();
                        }
                        //GTVVideoEditor.this.editCallback.onInitReady();
                        gtvEditorViewRender.getPlayerVideoObject().updateVideoSizeFromStream(width, height);
                    }

                    @Override
                    public void onCompletion() {
                        gtvEditorViewRender.getPlayerVideoObject().setRawAudioCallback(null);
                        if (videoRecordStreamer != null) {
                            videoRecordStreamer.closeStream();
                            videoRecordStreamer = null;

                        }

                        synchronized (GTVVideoEditor.this) {
                            if(gtvPlayerMerge != null) {
                                gtvPlayerMerge.destroy();
                                gtvPlayerMerge=null;
                            }
                        }

                        // 清除动画设定
                        gtvEditorViewRender.getPlayerVideoObject().setAnimation(null,null, animImageInterval,animationRect);

                        // 如果x264mode情况下，还有很多处理，移动到被的地方
                        // 恢复预览界面
                        //gtvEditorViewRender.setDisPlayToScreenFlag(true);
                        //gtvPlayerPreview.seekTo(0);
                    }
                });

                //gtvPlayerMerge.playStart();
            }
        });


    }
    public void stopCompose() {
        composeCancelFlag = true;
        gtvEditorViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {
                //20180517 这边强行destroy，还是会导致merge结束回调时再次destroy， 然后黑屏？
                synchronized (GTVVideoEditor.this) {
                    if(gtvPlayerMerge != null) {
                        gtvPlayerMerge.stopMerge();
                    }
                }

            }
        });

    }

    public void setX264Mode(boolean x264Mode) {
        this.x264Mode = x264Mode;
    }

    public boolean getX264Mode() {
        return this.x264Mode;
    }

    public void setMusicPath(String musicPath){
        setMusicPath(musicPath, 0);
    }

    public void setMusicPath(String musicPath, int startTimeMili){
        if (gtvPlayerPreview != null) {
            gtvPlayerPreview.setMusicPath(musicPath, startTimeMili);
        }

        this.musicPath = musicPath;
        this.musicStartTimeMili = startTimeMili;

        return;
    }

    public String getMusicPath(){
        return musicPath;
    }
    public int getMusicStartTime() {
        return musicStartTimeMili;
    }

    public int getMusicStartTimeMili(){
        return this.musicStartTimeMili;
    }

    public boolean setLogoBitmapAtRect(Bitmap bmp, Rect r){
        if (gtvEditorViewRender != null) {
            return gtvEditorViewRender.setLogoBitmapAtRect(bmp, new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top));
        }
        return true;
    }

    public boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r){
        if (gtvEditorViewRender != null) {
            return gtvEditorViewRender.setLogoBitmapAtRect2(bmp, new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top));
        }
        return true;
    }

    // todo 增加返回值？
    public void setMp4VideoCover(String imgFileName){

        if (imgFileName == null) {
            return;
        }
        Bitmap bm = BitmapFactory.decodeFile(imgFileName);
        Bitmap bmScaled = Bitmap.createScaledBitmap(bm, 170, 160, false);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        String tagFileName = timeStamp + ".png";

        tagFile = GTVFileUtils.getFilePath(glSurfaceView.getContext(), tagFileName);

        try {
            File file = new File(tagFile);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            bmScaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            tagFile = null;
            e.printStackTrace();
        }
    }

    public void extractAndScaleVideoFrame(ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<GTVExtractFrameInfo> frameInfoList, float scale) {

        if(!outDirPath.endsWith(""+File.separator)) {
            outDirPath += ""+File.separator;
        }
        GTVFileUtils.createDirectoryIfNeed(outDirPath);

        if(imageFormat == ImageFormat.IMAGE_JPEG) {
            GTVMP4Help.extractAndScaleVideoFrameToJpg(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList, scale);
        }else if (imageFormat == ImageFormat.IMAGE_YUV) {
            GTVMP4Help.extractAndScaleVideoFrameToYuv(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList, scale);
        }
    }

    public void extractVideoFrame(ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<GTVExtractFrameInfo> frameInfoList){

        if(!outDirPath.endsWith(""+File.separator)) {
            outDirPath += ""+File.separator;
        }
        GTVFileUtils.createDirectoryIfNeed(outDirPath);

        if(imageFormat == ImageFormat.IMAGE_JPEG) {
            GTVMP4Help.extractVideoFrameToJpg(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList);
        }else if (imageFormat == ImageFormat.IMAGE_YUV) {
            GTVMP4Help.extractVideoFrameToYuv(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList);
        }

//        for(int i = 0; i < frameInfoList.size(); ++i) {
//            LogUtils.LOGI("DDDDDDD", " " + frameInfoList.get(i).getFilePath() + " " + frameInfoList.get(i).getTimeStampMili());
//        }
    }

    public boolean createImageWebp(ArrayList<GTVExtractFrameInfo> frameInfoList, String outputPath){
        int lastIndex = outputPath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            return false;
        }

        if(frameInfoList==null || frameInfoList.size() < 1) {
            return false;
        }

        String dirPath = outputPath.substring(0, lastIndex + 1);
        GTVFileUtils.createDirectoryIfNeed(dirPath);
        GTVMP4Help.createWebpFromYuv(frameInfoList, outputPath);

        File file = new File(outputPath);
        if (!file.exists()) {
            return false;
        }

        return true;
    }

    boolean muteFlag = false;
    public void setOriginalSoundMute(boolean muteFlag){
        this.muteFlag = muteFlag;
        if(gtvPlayerPreview != null) {
            gtvPlayerPreview.setOrigAudioVolume(muteFlag == true ? 0 : 1.0f);
        }
    }

    public boolean getOriginalSoundMute(){
        return this.muteFlag;
    }

    public void setDisplayMode(boolean fullMode){
        if(gtvEditorViewRender != null) {
            gtvEditorViewRender.setFullMode(fullMode);
        }
    }

    public boolean getDisplayMode() {
        if(gtvEditorViewRender != null) {
            gtvEditorViewRender.getFullMode();
        }

        return true;
    }

//    public String getVideoPathForCoverSetting(){
//        String fileForAllIFrame = null;
//        {
//            int lastIndex = videoPath.lastIndexOf(File.separator);
//            fileForAllIFrame = videoPath.substring(0, lastIndex + 1) + "for_cover.mp4";
//        }
//        File file = new File(fileForAllIFrame);
//        if (!file.exists()) {
//            return null;
//        }
//        Log.e("bbbbbbbb", " " + fileForAllIFrame);
//
//        return fileForAllIFrame;
//    }

    //private String getSettingFilePath() {}

    public void saveSetting(){

        if(editWorkFolder == null) {
            return;
        }

        String jsonPath = this.editWorkFolder+File.separator+ SETTING_JSON;

        try{
            JSONObject settingObj = new JSONObject();

            //特效 设定
            if(effectFilterTypeManage != null && effectFilterTypeManage.getVideoEffectInfoList()!= null && effectFilterTypeManage.getVideoEffectInfoList().size() > 0) {
                JSONArray jArray = new JSONArray();
                for ( int i = 0; i < effectFilterTypeManage.getVideoEffectInfoList().size(); ++i) {
                    GTVideoEffectInfo gtVideoEffectInfo = effectFilterTypeManage.getVideoEffectInfoList().get(i);

                    JSONObject obj = new JSONObject();
                    obj.put("effectType", ""+gtVideoEffectInfo.getEffectType().ordinal());
                    obj.put("startTime", "" + gtVideoEffectInfo.getStartTime());
                    obj.put("endTime", "" + gtVideoEffectInfo.getEndTime());

                    jArray.put(obj);
                    //gtVideoEffectInfo.getEffectType()
                }
                settingObj.put("effect", jArray);
            }

            // 音乐
            if(this.musicPath != null) {
                JSONObject obj = new JSONObject();
                obj.put("musicPath", this.musicPath);
                obj.put("startTime", this.musicStartTimeMili);
                settingObj.put("music", obj);
            }

            // 慢动作
            if(this.slowMotionEnable == true) {
                JSONObject obj = new JSONObject();
                obj.put("enable", this.slowMotionEnable);
                obj.put("startTime", this.slowMotionStartTime);
                obj.put("endTime", this.slowMotionEndTime);
                settingObj.put("slowMotion", obj);
            }

            // 视频（裁剪）
            if(this.cropRangeStartTime !=0 || this.cropRangeEndTime != 0) {
                JSONObject obj = new JSONObject();
                obj.put("startTime", this.cropRangeStartTime);
                obj.put("endTime", this.cropRangeEndTime);
                settingObj.put("videoCrop", obj);
            }

            //音频（禁止原声）
//            if(this.muteFlag == true)
            {
                JSONObject obj = new JSONObject();
                obj.put("mute", this.muteFlag);
                obj.put("originVolume", _originV);
                obj.put("musicVolume", _musicV);
                settingObj.put("audio", obj);
            }

            {
                JSONObject obj = new JSONObject();
                obj.put("width", this.width);
                obj.put("height", this.height);
                settingObj.put("video", obj);
            }


            FileWriter fr = new FileWriter(jsonPath);
            fr.write(settingObj.toString());
            fr.close();
        } catch (Exception e ) {
            e.printStackTrace();
        }

    }

    public void loadSetting(){

        if(this.reloadSettingFlag == false || this.editWorkFolder == null) {
            return;
        }

        String path = this.editWorkFolder+File.separator+ SETTING_JSON;
        File f = new File(path);
        if(!f.exists()|| !f.isFile()) {
            LogUtils.LOGI(TAG, "setting file is not exists");
            return;
        }

        //String path ="/storage/emulated/0/DCIM/setting.json";
        try{
            FileReader fr = new FileReader(path);
            StringBuilder sb = new StringBuilder();
            char buf[] = new char[4096];
            int size = fr.read(buf);
            while (size > 0) {
                sb.append(buf,0,size);
                size = fr.read(buf);
            }
            fr.close();
           // LogUtils.LOGI("AAAAA", sb.toString());
            JSONObject settingObj = new JSONObject(sb.toString());

            //特效 设定
            if(settingObj.has("effect")) {
                JSONArray array = settingObj.optJSONArray("effect");
                if(array!=null && array.length() > 0) {
                    if(effectFilterTypeManage == null) {
                        effectFilterTypeManage = new GTVEffectFilterTypeManage();
                    }
                    for(int i = 0; i < array.length(); ++i) {
                        //Map<String, String> map = (Map<String, String>) array.getJSONObject();
                        JSONObject obj = array.getJSONObject(i);
                        //LogUtils.LOGI("AAAAA", " " + obj.get("effectType") + " " + obj.get("startTime") + " " +obj.get("endTime"));
                        GTVideoEffectInfo gtVideoEffectInfo = new GTVideoEffectInfo();

                        IGTVVideoEditor.EffectType effectType = EffectType.values()[obj.getInt("effectType")];

                        gtVideoEffectInfo.setEffectType(effectType);
                        gtVideoEffectInfo.setStartTime(obj.getInt("startTime"));
                        gtVideoEffectInfo.setEndTime(obj.getInt("endTime"));
                        effectFilterTypeManage.getVideoEffectInfoList().add(gtVideoEffectInfo);
                    }
                }
            }

            //debug
            if(effectFilterTypeManage!=null) {
                for(int i = 0; i < effectFilterTypeManage.getVideoEffectInfoList().size();++i) {
                    GTVideoEffectInfo gtVideoEffectInfo = effectFilterTypeManage.getVideoEffectInfoList().get(i);
                    //LogUtils.LOGI("AAAAAA", " " + gtVideoEffectInfo.getEffectType() + " " + gtVideoEffectInfo.getStartTime()+ " " + gtVideoEffectInfo.getEndTime());
                }
            }

            // 音乐
            if(settingObj.has("music")) {
                JSONObject obj = settingObj.getJSONObject("music");
                this.musicPath = obj.getString("musicPath");
                this.musicStartTimeMili = obj.getInt("startTime");
                // 可能是空字符串
                if(this.musicPath.length() < 3) {
                    this.musicPath = null;
                }
            }
            //LogUtils.LOGI("AAAAAA", " " + musicPath + " " + musicStartTimeMili);

            // 慢动作
            if(settingObj.has("slowMotion")) {
                JSONObject obj = settingObj.getJSONObject("slowMotion");
                this.slowMotionEnable = obj.getBoolean("enable");
                this.slowMotionStartTime = obj.getInt("startTime");
                this.slowMotionEndTime = obj.getInt("endTime");
                //LogUtils.LOGI("AAAAAA", " " + slowMotionEnable + " " + slowMotionStartTime + " " + slowMotionEndTime);
            }

            // 视频（裁剪）
            if(settingObj.has("videoCrop")) {
                JSONObject obj = settingObj.getJSONObject("videoCrop");
                this.cropRangeStartTime = obj.getInt("startTime");
                this.cropRangeEndTime = obj.getInt("endTime");
                //LogUtils.LOGI("AAAAAA", " " + cropRangeStartTime + " " + cropRangeEndTime);
            }

            //音频（禁止原声）
            if(settingObj.has("audio")){
                JSONObject obj = settingObj.getJSONObject("audio");
                this.muteFlag = obj.getBoolean("mute");
                this._originV = (float)obj.getDouble("originVolume");
                this._musicV = (float)obj.getDouble("musicVolume");
                //LogUtils.LOGI("AAAAAA", " " + this.muteFlag);
            }

            if(settingObj.has("video")){
                JSONObject obj = settingObj.getJSONObject("video");
                this.width = obj.getInt("width");
                this.height = obj.getInt("height");
                //LogUtils.LOGI("AAAAAA", " " + this.muteFlag);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String animationPrefix = null;
    private String animationFolder = null;
    private int animImageInterval = 60;
    private DispRect animationRect = null;

    public void setAnimation(String prefix, String animationFolder, int animImageInterval, Rect r) {
        this.animationPrefix = prefix;
        this.animationFolder = animationFolder;
        this.animImageInterval = animImageInterval;
        this.animationRect = new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    public void setAnimation(String animationFolder, int animImageInterval, Rect r) {
        this.animationPrefix = null;
        this.animationFolder = animationFolder;
        this.animImageInterval = animImageInterval;
        this.animationRect = new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    // 设置原始音量
    private float _originV = 1.0f;
    public void setOriginVolume(float v) {
        if( v < 0.0f )
            v = 0.0f;
        else if( v > 1.0f )
            v = 1.0f;
        _originV = v;
        if( gtvPlayerPreview != null ) {
            gtvPlayerPreview.setOrigAudioVolume(v);
        }
    }
    public float getOriginVolume() {
        return _originV;
    }

    // 设置音乐音量
    private float _musicV = 1.0f;
    public void setMusicVolume(float v) {
        if( v < 0.0f )
            v = 0.0f;
        else if( v > 1.0f )
            v = 1.0f;
        _musicV = v;
        if( gtvPlayerPreview != null ) {
            gtvPlayerPreview.setMusicVolume(v);
        }
    }
    public float getMusicVolume() {
        return _musicV;
    }
}
