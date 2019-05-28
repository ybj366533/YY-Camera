package com.ybj366533.videolib.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;

import com.ybj366533.videolib.core.EffectFilterTypeManage;
import com.ybj366533.videolib.core.PlayerMerge;
import com.ybj366533.videolib.core.PlayerPreview;
import com.ybj366533.videolib.impl.YYMP4Help;
import com.ybj366533.videolib.impl.setting.AVStreamSetting;
import com.ybj366533.videolib.impl.utils.YYFileUtils;
import com.ybj366533.videolib.stream.VideoRecordStreamer;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.videolib.widget.EditorViewRender;

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


public class VideoEditor implements IVideoEditor {

    private static final String TAG = "Editor";

    private static final String SETTING_JSON = "setting.json";

    private GLSurfaceView glSurfaceView;
    private EditCallback editCallback;
    private EditorViewRender YYEditorViewRender;

    VideoRecordStreamer videoRecordStreamer;

    private String videoPath;

    private double recordSpeed;

    private int bitrate;
    private int gop;

    private int width;
    private int height;

    String tagFile = null;

    PlayerPreview playerPreview;
    PlayerMerge playerMerge;

    // 合成用参数
    private boolean slowMotionEnable = false;
    private int slowMotionStartTime = 0;
    private int slowMotionEndTime = 0;
    private String musicPath = null;
    private int musicStartTimeMili = 0;
    EffectFilterTypeManage effectFilterTypeManage = new EffectFilterTypeManage();

    private int cropRangeStartTime = 0;
    private int cropRangeEndTime = 0;

    private boolean x264Mode = true;        // 因为需要输出封面设定用的全I帧视频，这个已不能设置为false了。

    private boolean composeCancelFlag = false;  // 用于合成取消

    private String editWorkFolder;     // 现在用于保存编辑设定的json文件的目录
    private boolean reloadSettingFlag;

    public VideoEditor(){
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

        YYEditorViewRender = new EditorViewRender(glSurfaceView, videoPath);

        this.editWorkFolder = editWorkFolder;
        this.reloadSettingFlag = reloadSettingFlag;

        setVideoPath(videoPath);

    }

    public void setVideoPath(String videoPath) {

        LogUtils.LOGI(TAG, "setVideoPath " + videoPath);

        this.videoPath = videoPath;
        YYEditorViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {
                if(playerPreview != null) {
                    //playerPreview.closeVideo();
                    playerPreview.destroy();
                }

                if(editWorkFolder != null)
                {
                    if(!editWorkFolder.endsWith(""+File.separator)) {
                        editWorkFolder += ""+File.separator;
                    }
                    YYFileUtils.createDirectoryIfNeed(editWorkFolder);
                }

                // 载入编辑设定值
                loadSetting();

                playerPreview = new PlayerPreview(glSurfaceView, YYEditorViewRender, VideoEditor.this.videoPath);
                playerPreview.setEffectFilterSetting(effectFilterTypeManage);
                playerPreview.setMusicVolume(_musicV);
                playerPreview.setOrigAudioVolume(_originV);
                playerPreview.init(YYEditorViewRender.getPlayerVideoObject(),YYEditorViewRender.getPlayerVideoObject(), new PlayerPreview.PlayCallback() {
                    @Override
                    public void onPrepared() {
                        if (playerPreview.getVideoWidth() !=0 &&
                                playerPreview.getVideoHeight() != 0) {
                            width = playerPreview.getVideoWidth();
                            height = playerPreview.getVideoHeight();
                        }
                        VideoEditor.this.editCallback.onInitReady();
                        YYEditorViewRender.getPlayerVideoObject().updateVideoSizeFromStream(width, height);

                        LogUtils.LOGI(TAG, "onPrepared " + width + " " + height);
                    }

                    @Override
                    public void onCompletion() {
                        VideoEditor.this.editCallback.onPlayComplete();
                    }
                });
                if(musicPath != null) {
                    playerPreview.setMusicPath(VideoEditor.this.musicPath, VideoEditor.this.musicStartTimeMili);
                }
                if(cropRangeStartTime!=0 || cropRangeEndTime !=0) {
                    playerPreview.setVideoCropRange(cropRangeStartTime,cropRangeEndTime);
                }
                if(slowMotionEnable == true) {
                    playerPreview.setSlowMotionEnable(slowMotionEnable);
                    playerPreview.setSlowMotionStartTime(cropRangeStartTime);
                    playerPreview.setSlowMotionEndTime(cropRangeEndTime);
                }
                if(muteFlag == true) {
                    playerPreview.setOrigAudioVolume(VideoEditor.this.muteFlag == true ? 0 : 1.0f);
                }
            }
        });
    }

    public void startPreview(){ //onresume

        LogUtils.LOGI(TAG, "startPreview ");

        if (YYEditorViewRender != null) {
            YYEditorViewRender.myQueueEvent(new Runnable() {
                @Override
                public void run() {
                    YYEditorViewRender.onResume();

                    if(playerPreview != null) {
                        //playerPreview.resumeVideo();
                        //playerPreview.playStart();     // 不主动打开
                    }
                }
            });

        }
    }

    public void stopPreview(){  //onpause

        LogUtils.LOGI(TAG, "stopPreview ");

        if (YYEditorViewRender != null) {
            YYEditorViewRender.myQueueEvent(new Runnable() {
                @Override
                public void run() {
                    YYEditorViewRender.onPause();
                    if(playerPreview != null) {
                        //playerPreview.pauseVideo();
                        playerPreview.playPause();
                    }
                }
            });

        }
    }

    public void destroy(){

        LogUtils.LOGI(TAG, "destroy ");

        if (YYEditorViewRender != null) {
            YYEditorViewRender.onDestroy();
        }
        // 预览播放  合成播放是否需要释放
        if (playerPreview != null) {
            playerPreview.destroy();
        }

        if(playerMerge != null) {
            playerMerge.destroy();
        }
    }

//    private void setEffectFilterType(EffectType effectType) {
//        if (YYEditorViewRender!= null && YYEditorViewRender.getPlayerVideoObject() != null) {
//            YYEditorViewRender.getPlayerVideoObject().setMagicFilterType(effectType);
//        }
//    }
//
//    private EffectType getEffectFilterType(){
//        if (YYEditorViewRender!= null && YYEditorViewRender.getPlayerVideoObject() != null) {
//            return YYEditorViewRender.getPlayerVideoObject().getMagicFilterType();
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

    public List<VideoEffectInfo> getVideoEffectList(){
        List<VideoEffectInfo> list = new ArrayList<>();
        for(int i = 0; i < effectFilterTypeManage.getVideoEffectInfoList().size(); ++i) {
            VideoEffectInfo e = effectFilterTypeManage.getVideoEffectInfoList().get(i);
            VideoEffectInfo videoEffectInfo = new VideoEffectInfo();

            videoEffectInfo.setEffectType(e.getEffectType());
            videoEffectInfo.setStartTime(e.getStartTime());
            videoEffectInfo.setEndTime(e.getEndTime());
            list.add(videoEffectInfo);
        }
        return list;
    }

    public void setVideoEffectList(List<VideoEffectInfo> videoEffectInfoList){
        List<VideoEffectInfo> list = new ArrayList<>();
        if(videoEffectInfoList != null) {
            for(int i = 0; i < videoEffectInfoList.size(); ++i) {
                VideoEffectInfo e = videoEffectInfoList.get(i);
                VideoEffectInfo videoEffectInfo = new VideoEffectInfo();

                videoEffectInfo.setEffectType(e.getEffectType());
                videoEffectInfo.setStartTime(e.getStartTime());
                videoEffectInfo.setEndTime(e.getEndTime());
                list.add(videoEffectInfo);
            }
        }
        effectFilterTypeManage.setVideoEffectInfoList(list);
    }

    public void setSlowPlayTime(int startTime, int endTime){
        if (startTime == 0 && endTime == 0) {
            if (playerPreview != null) {
                playerPreview.setSlowMotionEnable(false);
            }
            this.slowMotionEnable = false;
        } else {
            if (playerPreview != null) {
                playerPreview.setSlowMotionEnable(true);
                playerPreview.setSlowMotionStartTime(startTime);
                playerPreview.setSlowMotionEndTime(endTime);

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

        if (playerPreview != null) {
            playerPreview.setVideoCropRange(startTime, endTime);
        }
    }

    public int[] getVideoCropRange(){
        int [] range = new int[2];
        range[0] = this.cropRangeStartTime;
        range[1] = this.cropRangeEndTime;
        return range;
    }

    public int getDuration() {

        if (playerPreview != null) {
            return playerPreview.getDuration();
        }

        return 0;
    }

    public int getCurrentPosition () {

        if (playerPreview != null) {
            return playerPreview.getCurrentPosition();
        }

        return 0;
    }

    public void seekTo(int msec) {

        if (playerPreview != null) {
            playerPreview.seekTo(msec);
        }
        return;
    }

    public void playPause() {

        if (playerPreview != null) {
            playerPreview.playPause();
        }
    }

    // tood  = play resume
    public void playStart() {

        if(playerPreview != null) {
            playerPreview.playStart();
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



        YYEditorViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {

                LogUtils.LOGI(TAG, "startCompose");
                if (YYEditorViewRender == null || YYEditorViewRender.getEGLContext() == null || YYEditorViewRender.getPlayerVideoObject() == null) {
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
                YYFileUtils.createDirectoryIfNeed(dirPath);

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
                    YYFileUtils.createDirectoryIfNeed(dirPath);

                    fileForAllIFrame = keyFrameVideoPath;
                }

                if(isCompsing == true) {
                    LogUtils.LOGE(TAG, "already in composing");
                    return;
                }
                isCompsing = true;
                composeCancelFlag = false;

                AVStreamSetting setting = AVStreamSetting.settingForVideoRecord(x264Mode?6000000:bitrate, width, height, x264Mode?true:false);
                videoRecordStreamer = new VideoRecordStreamer(fileForAllIFrame, setting, YYEditorViewRender.getEGLContext());

                videoRecordStreamer.setInputVideoSource(YYEditorViewRender.getPlayerVideoObject());

                final String filePathT = fileForAllIFrame;


                final int composeDuration = getComposeDuration();
                // music=true; 不需要创建录音
                // recordspeed 固定1
                // openstream的callback 要统一成一个独立的
                // todo 有背景音乐改如何处理
                int ret = videoRecordStreamer.openStream(true, VideoEditor.this.recordSpeed, true, new VideoRecordStreamer.RecordCallback() {
                    @Override
                    public void onProgress(int duration) {
                        // compose callback
                        if (VideoEditor.this.editCallback != null) {
                            int progress = duration * 100 / composeDuration;//VideoEditor.this.getDuration();
                            if (progress > 99) {
                                progress = 99;
                            }
                            if(x264Mode == true) {
                                //progress /= 2;
                                progress *= 0.7;
                            }
                            VideoEditor.this.editCallback.onProgress(progress);
                        }
                    }

                    @Override
                    public void onComplete(int duration) {
//                        if(composeCancelFlag == true) {
//                            YYFileUtils.deleteFile(filePathT);
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
                                    ret = YYMP4Help.MP4FileTranscodeByX264(filePathT, filePath, VideoEditor.this.gop,new YYMP4Help.OnProgressListener() {
                                        @Override
                                        public int onProgress(int progress) {
                                            //Log.e("hhhhhhhhh", " " +progress);
                                            if (VideoEditor.this.editCallback != null) {
                                                int progress2 = 70 + (int)(progress *0.3);
                                                if(progress2>99) {
                                                    progress2 = 99;
                                                }
                                                VideoEditor.this.editCallback.onProgress(progress2);
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
                                    YYFileUtils.deleteFile(filePath);
                                    YYFileUtils.deleteFile(filePathT);
                                    ret = 1;
                                }

                                // 恢复预览界面
                                YYEditorViewRender.setDisPlayToScreenFlag(true);
                                playerPreview.seekTo(0);
                                playerPreview.playStart();

                                LogUtils.LOGI(TAG, "compose finish " + (ret == 0? 0: 1));

                                if (VideoEditor.this.editCallback != null) {
                                    VideoEditor.this.editCallback.onComposeFinish(ret == 0? 0: 1);
                                }

                                isCompsing = false;


                            }
                        }).start();


                    }

                    @Override
                    public void onError(int errorCode) {
                        if (VideoEditor.this.editCallback != null) {
                            VideoEditor.this.editCallback.onError(errorCode);
                        }
                    }
                });

                if (ret != 0) {
                    if (VideoEditor.this.editCallback != null) {
                        VideoEditor.this.editCallback.onError(100);
                    }
                    return;
                }

                saveSetting();

                playerPreview.playPause();
                YYEditorViewRender.setDisPlayToScreenFlag(false);
                playerMerge = new PlayerMerge(glSurfaceView, YYEditorViewRender, videoPath);
                playerMerge.setSlowMotionEnable(VideoEditor.this.slowMotionEnable);
                playerMerge.setSlowMotionStartTime(VideoEditor.this.slowMotionStartTime);
                playerMerge.setSlowMotionEndTime(VideoEditor.this.slowMotionEndTime);
                playerMerge.setVideoCropRange(cropRangeStartTime,cropRangeEndTime);
                playerMerge.setMusicPath(VideoEditor.this.musicPath, VideoEditor.this.musicStartTimeMili);
                playerMerge.setEffectFilterSetting(VideoEditor.this.effectFilterTypeManage);
                playerMerge.setRawAudioCallback(videoRecordStreamer);
                //playerMerge.setOrigAudioVolume(VideoEditor.this.muteFlag == true ? 0 : 1.0f);
                playerMerge.setOrigAudioVolume(VideoEditor.this.muteFlag == true ? 0 : _originV);
                playerMerge.setMusicVolume(_musicV);

                // 设定动画
                YYEditorViewRender.getPlayerVideoObject().setAnimation(animationPrefix, animationFolder, animImageInterval,animationRect);

                playerMerge.startMerge(YYEditorViewRender.getPlayerVideoObject(), YYEditorViewRender.getPlayerVideoObject(), new PlayerMerge.PlayCallback() {
                    @Override
                    public void onPrepared() {
                        if (playerMerge.getVideoWidth() !=0 &&
                                playerMerge.getVideoHeight() != 0) {
                            width = playerMerge.getVideoWidth();
                            height = playerMerge.getVideoHeight();
                        }
                        //VideoEditor.this.editCallback.onInitReady();
                        YYEditorViewRender.getPlayerVideoObject().updateVideoSizeFromStream(width, height);
                    }

                    @Override
                    public void onCompletion() {
                        YYEditorViewRender.getPlayerVideoObject().setRawAudioCallback(null);
                        if (videoRecordStreamer != null) {
                            videoRecordStreamer.closeStream();
                            videoRecordStreamer = null;

                        }

                        synchronized (VideoEditor.this) {
                            if(playerMerge != null) {
                                playerMerge.destroy();
                                playerMerge =null;
                            }
                        }

                        // 清除动画设定
                        YYEditorViewRender.getPlayerVideoObject().setAnimation(null,null, animImageInterval,animationRect);

                        // 如果x264mode情况下，还有很多处理，移动到被的地方
                        // 恢复预览界面
                        //YYEditorViewRender.setDisPlayToScreenFlag(true);
                        //playerPreview.seekTo(0);
                    }
                });

                //playerMerge.playStart();
            }
        });


    }
    public void stopCompose() {
        composeCancelFlag = true;
        YYEditorViewRender.myQueueEvent(new Runnable() {
            @Override
            public void run() {
                //20180517 这边强行destroy，还是会导致merge结束回调时再次destroy， 然后黑屏？
                synchronized (VideoEditor.this) {
                    if(playerMerge != null) {
                        playerMerge.stopMerge();
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
        if (playerPreview != null) {
            playerPreview.setMusicPath(musicPath, startTimeMili);
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
        if (YYEditorViewRender != null) {
            return YYEditorViewRender.setLogoBitmapAtRect(bmp, new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top));
        }
        return true;
    }

    public boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r){
        if (YYEditorViewRender != null) {
            return YYEditorViewRender.setLogoBitmapAtRect2(bmp, new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top));
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

        tagFile = YYFileUtils.getFilePath(glSurfaceView.getContext(), tagFileName);

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

    public void extractAndScaleVideoFrame(ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList, float scale) {

        if(!outDirPath.endsWith(""+File.separator)) {
            outDirPath += ""+File.separator;
        }
        YYFileUtils.createDirectoryIfNeed(outDirPath);

        if(imageFormat == ImageFormat.IMAGE_JPEG) {
            YYMP4Help.extractAndScaleVideoFrameToJpg(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList, scale);
        }else if (imageFormat == ImageFormat.IMAGE_YUV) {
            YYMP4Help.extractAndScaleVideoFrameToYuv(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList, scale);
        }
    }

    public void extractVideoFrame(ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList){

        if(!outDirPath.endsWith(""+File.separator)) {
            outDirPath += ""+File.separator;
        }
        YYFileUtils.createDirectoryIfNeed(outDirPath);

        if(imageFormat == ImageFormat.IMAGE_JPEG) {
            YYMP4Help.extractVideoFrameToJpg(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList);
        }else if (imageFormat == ImageFormat.IMAGE_YUV) {
            YYMP4Help.extractVideoFrameToYuv(this.videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList);
        }

//        for(int i = 0; i < frameInfoList.size(); ++i) {
//            LogUtils.LOGI("DDDDDDD", " " + frameInfoList.get(i).getFilePath() + " " + frameInfoList.get(i).getTimeStampMili());
//        }
    }

    public boolean createImageWebp(ArrayList<ExtractFrameInfo> frameInfoList, String outputPath){
        int lastIndex = outputPath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            return false;
        }

        if(frameInfoList==null || frameInfoList.size() < 1) {
            return false;
        }

        String dirPath = outputPath.substring(0, lastIndex + 1);
        YYFileUtils.createDirectoryIfNeed(dirPath);
        YYMP4Help.createWebpFromYuv(frameInfoList, outputPath);

        File file = new File(outputPath);
        if (!file.exists()) {
            return false;
        }

        return true;
    }

    boolean muteFlag = false;
    public void setOriginalSoundMute(boolean muteFlag){
        this.muteFlag = muteFlag;
        if(playerPreview != null) {
            playerPreview.setOrigAudioVolume(muteFlag == true ? 0 : 1.0f);
        }
    }

    public boolean getOriginalSoundMute(){
        return this.muteFlag;
    }

    public void setDisplayMode(boolean fullMode){
        if(YYEditorViewRender != null) {
            YYEditorViewRender.setFullMode(fullMode);
        }
    }

    public boolean getDisplayMode() {
        if(YYEditorViewRender != null) {
            YYEditorViewRender.getFullMode();
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
                    VideoEffectInfo videoEffectInfo = effectFilterTypeManage.getVideoEffectInfoList().get(i);

                    JSONObject obj = new JSONObject();
                    obj.put("effectType", ""+ videoEffectInfo.getEffectType().ordinal());
                    obj.put("startTime", "" + videoEffectInfo.getStartTime());
                    obj.put("endTime", "" + videoEffectInfo.getEndTime());

                    jArray.put(obj);
                    //videoEffectInfo.getEffectType()
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
                        effectFilterTypeManage = new EffectFilterTypeManage();
                    }
                    for(int i = 0; i < array.length(); ++i) {
                        //Map<String, String> map = (Map<String, String>) array.getJSONObject();
                        JSONObject obj = array.getJSONObject(i);
                        //LogUtils.LOGI("AAAAA", " " + obj.get("effectType") + " " + obj.get("startTime") + " " +obj.get("endTime"));
                        VideoEffectInfo videoEffectInfo = new VideoEffectInfo();

                        IVideoEditor.EffectType effectType = EffectType.values()[obj.getInt("effectType")];

                        videoEffectInfo.setEffectType(effectType);
                        videoEffectInfo.setStartTime(obj.getInt("startTime"));
                        videoEffectInfo.setEndTime(obj.getInt("endTime"));
                        effectFilterTypeManage.getVideoEffectInfoList().add(videoEffectInfo);
                    }
                }
            }

            //debug
            if(effectFilterTypeManage!=null) {
                for(int i = 0; i < effectFilterTypeManage.getVideoEffectInfoList().size();++i) {
                    VideoEffectInfo videoEffectInfo = effectFilterTypeManage.getVideoEffectInfoList().get(i);
                    //LogUtils.LOGI("AAAAAA", " " + videoEffectInfo.getEffectType() + " " + videoEffectInfo.getStartTime()+ " " + videoEffectInfo.getEndTime());
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
        if( playerPreview != null ) {
            playerPreview.setOrigAudioVolume(v);
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
        if( playerPreview != null ) {
            playerPreview.setMusicVolume(v);
        }
    }
    public float getMusicVolume() {
        return _musicV;
    }
}
