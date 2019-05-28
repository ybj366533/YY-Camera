package com.ybj366533.videolib.editor;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.ybj366533.videolib.core.EffectFilterTypeManage;
import com.ybj366533.videolib.core.VideCompose;
import com.ybj366533.videolib.impl.YYMP4Help;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.impl.utils.YYFileUtils;
import com.ybj366533.videolib.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;

/**
 * Created by YY on 2018/6/3.
 */

public class VideoComposer implements IVideoComposer {
    private static final String TAG = "Composer";

    private static final String SETTING_JSON = "setting.json";

    private String videoPath;
    private String editWorkFolder;     // 现在用于保存编辑设定的json文件的目录

    private EditCallback editCallback;

    private boolean x264Mode = true;        // 因为需要输出封面设定用的全I帧视频，这个已不能设置为false了。

    private boolean composeCancelFlag = false;  // 用于合成取消

    private boolean reloadSettingFlag = true;
    EffectFilterTypeManage effectFilterTypeManage = new EffectFilterTypeManage();
    private String musicPath = null;
    private int musicStartTimeMili = 0;
    private boolean slowMotionEnable = false;
    private int slowMotionStartTime = 0;
    private int slowMotionEndTime = 0;
    private int cropRangeStartTime = 0;
    private int cropRangeEndTime = 0;
    private int width;
    private int height;
    private int duration = 0;
    boolean muteFlag = false;

    // 水印没有保存在设定文件
    private Bitmap logoImage;
    private DispRect logoRect;

    private Bitmap logoImage2;
    private DispRect logoRect2;

    VideCompose videCompose;

    public VideoComposer() {

        width = 576;
        height = 1024;
    }

    public void init(final String videoPath, final String editWorkFolder, EditCallback editCallback) {


        LogUtils.LOGI(TAG, "init " + videoPath);

        this.videoPath = videoPath;

        this.editCallback = editCallback;


        this.editWorkFolder = editWorkFolder;

        setVideoPath(videoPath);

    }

    public void setVideoPath(String videoPath) {
        if (editWorkFolder != null) {
            if (!editWorkFolder.endsWith("" + File.separator)) {
                editWorkFolder += "" + File.separator;
            }
            YYFileUtils.createDirectoryIfNeed(editWorkFolder);
        }

        // 载入编辑设定值
        loadSetting();

    }

    private boolean isCompsing = false;
    private String keyFrameVideoPath = null;

    public void startCompose(final String filePath) {

        LogUtils.LOGI(TAG, "startCompose");

        int lastIndex = filePath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            return;
        }
        String dirPath = filePath.substring(0, lastIndex + 1);
        YYFileUtils.createDirectoryIfNeed(dirPath);

        String fileForAllIFrame = null;
        if (keyFrameVideoPath == null) {
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

        // 获取视频大小
        {
            int[] video_d = new int[1];
            int[] audio_d = new int[1];
            int[] size = new int[2];
            YYMP4Help.GetVideoInfo(videoPath, video_d, audio_d, size);
            if (video_d[0] == 0 || size[0] == 0) {
                return;
            }
            duration = video_d[0];
            width = size[0];
            height = size[1];
        }
        final int composeDuration = getComposeDuration();
        final String filePathT = fileForAllIFrame;

        if (isCompsing == true) {
            LogUtils.LOGE(TAG, "already in composing");
            return;
        }
        isCompsing = true;
        composeCancelFlag = false;

        videCompose = new VideCompose(null);
        videCompose.setSlowMotionEnable(VideoComposer.this.slowMotionEnable);
        videCompose.setSlowMotionStartTime(VideoComposer.this.slowMotionStartTime);
        videCompose.setSlowMotionEndTime(VideoComposer.this.slowMotionEndTime);
        videCompose.setVideoCropRange(cropRangeStartTime, cropRangeEndTime);
        videCompose.setMusicPath(VideoComposer.this.musicPath, VideoComposer.this.musicStartTimeMili);
        videCompose.setEffectFilterSetting(VideoComposer.this.effectFilterTypeManage);
        //videCompose.setRawAudioCallback(videoRecordStreamer);
        videCompose.setOrigAudioVolume(VideoComposer.this.muteFlag == true ? 0 : this.origAudioVolume);
        videCompose.setMusicVolume(this.musicVolume);
        if (logoImage != null && logoRect != null) {
            videCompose.setLogoBitmapAtRect(logoImage, logoRect);
        }

        if (logoImage2 != null && logoRect2 != null) {
            videCompose.setLogoBitmapAtRect2(logoImage2, logoRect2);
        }

        if (animationFolder != null && animationRect != null) {
            videCompose.setAnimation(animationPrefix, animationFolder, animImageInterval, animationRect);
        }


        //todo shuiin 2

        videCompose.startCompose(videoPath, filePath, width, height, new VideCompose.ComposeCallback() {
            @Override
            public void onPrepared() {
                // do nothing
                LogUtils.LOGI(TAG, "Compose Start");
            }

            @Override
            public void onProgress(int duration) {
                int progress = duration * 100 / composeDuration;//VideoEditor.this.getDuration();
                if (progress > 99) {
                    progress = 99;
                }
//                if(x264Mode == true) {
//                    //progress /= 2;
//                    progress *= 0.7;
//                }
                VideoComposer.this.editCallback.onProgress(progress);
            }

            @Override
            public void onCompletion(int reason) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int ret = 0;
//                        if (x264Mode == true && composeCancelFlag == false) {        //todo reason!=0

//                                    // 原文件重命名为临时文件
//                                    String fileTmep = filePathT.substring(0, filePathT.length()-4) + "_temp2" + ".mp4";
//                                    File from = new File(filePathT);
//                                    File to = new File(fileTmep);
//                                    from.renameTo(to);

                        //Log.e("hhhhhhhhh", "start ");
//                            LogUtils.LOGI(TAG, " x264 start");
//                            ret = YYMP4Help.MP4FileTranscodeByX264(filePathT, filePath, 15, new YYMP4Help.OnProgressListener() {
//                                @Override
//                                public int onProgress(int progress) {
//                                    //Log.e("hhhhhhhhh", " " +progress);
//                                    if (VideoComposer.this.editCallback != null) {
//                                        int progress2 = 70 + (int) (progress * 0.3);
//                                        if (progress2 > 99) {
//                                            progress2 = 99;
//                                        }
//                                        VideoComposer.this.editCallback.onProgress(progress2);
//                                    }
//
//                                    if (composeCancelFlag == true) {
//                                        return 1;
//                                    }
//
//                                    return 0;
//
//                                }
//                            });
//                            LogUtils.LOGI(TAG, " x264 end");


//                            if (keyFrameVideoPath == null) {
//                                File fileDel = new File(filePathT);
//                                if (fileDel.exists()) {
//                                    fileDel.delete();
//                                    Log.e(TAG, " delete file " + filePathT);
//                                }
//                            }
//                        }

//                        Log.e(TAG, "###" + filePath + "!!!"+filePathT);
//                        Log.e(TAG, "!!!!!###" + filePathT);
//                        if (composeCancelFlag == true) {
//                            YYFileUtils.deleteFile(filePath);
//                            YYFileUtils.deleteFile(filePathT);
//                            Log.e(TAG, "aaaaaa###" + filePathT);
//                            ret = 1;
//                        }


                        videCompose.destroy();
                        videCompose = null;

                        isCompsing = false;

                        if (VideoComposer.this.editCallback != null) {
                            VideoComposer.this.editCallback.onComposeFinish(ret == 0 ? 0 : 1);
                        }

                    }
                }).start();
            }
        });

    }

    public void stopCompose() {
        composeCancelFlag = true;
        if (videCompose != null) {
            videCompose.stopCompose();
        }

    }

    public boolean setLogoBitmapAtRect(Bitmap bmp, Rect r) {
        this.logoImage = bmp;
        this.logoRect = new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top);
        return true;
    }

    public boolean setLogoBitmapAtRect2(Bitmap bmp, Rect r) {
        this.logoImage2 = bmp;
        this.logoRect2 = new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top);
        return true;
    }

    private String animationPrefix = null;
    private String animationFolder = null;
    private int animImageInterval = 60;
    private Rect animationRect = null;

    public void setAnimation(String animationFolder, int animImageInterval, Rect r) {
        this.animationPrefix = null;
        this.animationFolder = animationFolder;
        this.animImageInterval = animImageInterval;
        this.animationRect = r;
        //this.animationRect = new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    public void setAnimation(String prefix, String animationFolder, int animImageInterval, Rect r) {
        this.animationPrefix = prefix;
        this.animationFolder = animationFolder;
        this.animImageInterval = animImageInterval;
        this.animationRect = r;
        //this.animationRect = new DispRect(9999, r.left, r.top, r.right - r.left, r.bottom - r.top);
    }

    private int getDuration() {
        if (duration > 0) {
            return duration;
        } else {
            return 15000;
        }
    }

    private int getComposeDuration() {
        int start = cropRangeStartTime;
        int end = cropRangeEndTime;
        if (start <= 0 || start > getDuration()) {
            start = 0;
        }

        if (end <= 0 || end > getDuration()) {
            end = getDuration();
        }
        return (end - start + 1);
    }

    public void loadSetting() {

        if (this.reloadSettingFlag == false || this.editWorkFolder == null) {
            return;
        }

        String path = this.editWorkFolder + File.separator + SETTING_JSON;
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            LogUtils.LOGI(TAG, "setting file is not exists");
            return;
        }

        //String path ="/storage/emulated/0/DCIM/setting.json";
        try {
            FileReader fr = new FileReader(path);
            StringBuilder sb = new StringBuilder();
            char buf[] = new char[4096];
            int size = fr.read(buf);
            while (size > 0) {
                sb.append(buf, 0, size);
                size = fr.read(buf);
            }
            fr.close();
            // LogUtils.LOGI("AAAAA", sb.toString());
            JSONObject settingObj = new JSONObject(sb.toString());

            //特效 设定
            if (settingObj.has("effect")) {
                JSONArray array = settingObj.optJSONArray("effect");
                if (array != null && array.length() > 0) {
                    if (effectFilterTypeManage == null) {
                        effectFilterTypeManage = new EffectFilterTypeManage();
                    }
                    for (int i = 0; i < array.length(); ++i) {
                        //Map<String, String> map = (Map<String, String>) array.getJSONObject();
                        JSONObject obj = array.getJSONObject(i);
                        //LogUtils.LOGI("AAAAA", " " + obj.get("effectType") + " " + obj.get("startTime") + " " +obj.get("endTime"));
                        VideoEffectInfo videoEffectInfo = new VideoEffectInfo();

                        IVideoEditor.EffectType effectType = IVideoEditor.EffectType.values()[obj.getInt("effectType")];

                        videoEffectInfo.setEffectType(effectType);
                        videoEffectInfo.setStartTime(obj.getInt("startTime"));
                        videoEffectInfo.setEndTime(obj.getInt("endTime"));
                        effectFilterTypeManage.getVideoEffectInfoList().add(videoEffectInfo);
                    }
                }
            }

            //debug
            if (effectFilterTypeManage != null) {
                for (int i = 0; i < effectFilterTypeManage.getVideoEffectInfoList().size(); ++i) {
                    VideoEffectInfo videoEffectInfo = effectFilterTypeManage.getVideoEffectInfoList().get(i);
                    //LogUtils.LOGI("AAAAAA", " " + videoEffectInfo.getEffectType() + " " + videoEffectInfo.getStartTime()+ " " + videoEffectInfo.getEndTime());
                }
            }

            // 音乐
            if (settingObj.has("music")) {
                JSONObject obj = settingObj.getJSONObject("music");
                this.musicPath = obj.getString("musicPath");
                this.musicStartTimeMili = obj.getInt("startTime");
                // 可能是空字符串
                if (this.musicPath.length() < 3) {
                    this.musicPath = null;
                }
            }
            //LogUtils.LOGI("AAAAAA", " " + musicPath + " " + musicStartTimeMili);

            // 慢动作
            if (settingObj.has("slowMotion")) {
                JSONObject obj = settingObj.getJSONObject("slowMotion");
                this.slowMotionEnable = obj.getBoolean("enable");
                this.slowMotionStartTime = obj.getInt("startTime");
                this.slowMotionEndTime = obj.getInt("endTime");
                //LogUtils.LOGI("AAAAAA", " " + slowMotionEnable + " " + slowMotionStartTime + " " + slowMotionEndTime);
            }

            // 视频（裁剪）
            if (settingObj.has("videoCrop")) {
                JSONObject obj = settingObj.getJSONObject("videoCrop");
                this.cropRangeStartTime = obj.getInt("startTime");
                this.cropRangeEndTime = obj.getInt("endTime");
                //LogUtils.LOGI("AAAAAA", " " + cropRangeStartTime + " " + cropRangeEndTime);
            }

            //音频（禁止原声）
            if (settingObj.has("audio")) {
                JSONObject obj = settingObj.getJSONObject("audio");
                this.muteFlag = obj.getBoolean("mute");
                this.origAudioVolume = (float) obj.getDouble("originVolume");
                this.musicVolume = (float) obj.getDouble("musicVolume");
                //LogUtils.LOGI("AAAAAA", " " + this.muteFlag);
            }

            if (settingObj.has("video")) {
                JSONObject obj = settingObj.getJSONObject("video");
                this.width = obj.getInt("width");
                this.height = obj.getInt("height");
                //LogUtils.LOGI("AAAAAA", " " + this.muteFlag);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float origAudioVolume = 1.0f;
    private float musicVolume = 1.0f;

    public void setOrigAudioVolume(float origAudioVolume) {
        this.origAudioVolume = origAudioVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

}
