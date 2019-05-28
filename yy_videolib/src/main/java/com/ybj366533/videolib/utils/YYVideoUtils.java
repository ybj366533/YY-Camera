package com.ybj366533.videolib.utils;

import com.ybj366533.videolib.editor.ExtractFrameInfo;
import com.ybj366533.videolib.editor.IVideoEditor;
import com.ybj366533.videolib.impl.YYMP4Help;
import com.ybj366533.videolib.impl.utils.YYFileUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by YY on 2018/4/23.
 */

public class YYVideoUtils {

    private static final String TAG = "YYVideoUtils";

    public interface OnProgressCallback {
        // 返回值0，正常
        // 返回值1，停止合成
        int onProgress(int progress);
    }

    // 导入转换视频（调整成全I帧模式） destPath需要指定全路径 /xxx/xxx/xxx.mp4
    // fromPath 输入文件路径
    // toPath 输出文件路径
    // onProgressCallback 转换进度
    static public boolean importMovie(String fromPath, String toPath, final OnProgressCallback onProgressCallback) {

        int lastIndex = toPath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            return false;
        }

        String dirPath = toPath.substring(0, lastIndex + 1);
        YYFileUtils.createDirectoryIfNeed(dirPath);

        String videoPath = dirPath + "video.mp4";

        // 只有视频转码
        int ret = YYMP4Help.MP4FileImportVideoByX264_video(fromPath, videoPath, 1, new YYMP4Help.OnProgressListener() {
            @Override
            public int onProgress(int progress) {
                if (onProgressCallback != null) {
                    return onProgressCallback.onProgress(progress >= 99 ? 99 : progress);
                }
                return 0;
            }
        }, 576, 1024, false, 0, 0);

        LogUtils.LOGI(TAG, "video transcode finish: " + ret);
        if (ret != 0) {
            YYFileUtils.deleteFile(videoPath);
            return false;
        }
        ret = YYMP4Help.Mp4AudioVideoMerge(fromPath, videoPath, toPath);

        YYFileUtils.deleteFile(videoPath);

        if (ret != 0) {
            return false;
        }
        // 外面需要个进度条百分百的通知
        if (onProgressCallback != null) {
            onProgressCallback.onProgress(100);
        }
        return true;
    }

    // 导入转换视频（调整成全I帧模式） destPath需要指定全路径 /xxx/xxx/xxx.mp4
    // fromPath 输入文件路径
    // toPath 输出文件路径
    // onProgressCallback 转换进度
    static public boolean importMovie(String fromPath, String toPath, int startTimeMs, int endTimeMs, final OnProgressCallback onProgressCallback ){

        int lastIndex = toPath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            return false;
        }

        String dirPath = toPath.substring(0, lastIndex + 1);
        YYFileUtils.createDirectoryIfNeed(dirPath);

        int [] video_d = new int[1];
        int [] audio_d = new int[1];
        int [] size = new int[2];
        YYMP4Help.GetVideoInfo(fromPath, video_d, audio_d,size);
        if(video_d[0] <= 0) {
            LogUtils.LOGE(TAG, "import failed, no video stream ");
        }

        String videoPath = dirPath + "video.mp4";
        String audioPath = dirPath + "audio.mp4";

        LogUtils.LOGI(TAG, "video file import start: video_duration " + video_d[0] + " audio_duration " + audio_d[0]);


        // 只有视频转码
        int ret = YYMP4Help.MP4FileImportVideoByX264_video(fromPath, videoPath, 1, new YYMP4Help.OnProgressListener() {
            @Override
            public int onProgress(int progress) {
                if (onProgressCallback != null) {
                    return onProgressCallback.onProgress(progress >= 99 ? 99 : progress);
                }
                return 0;
            }
        }, 576, 1024, true, startTimeMs, endTimeMs);

        LogUtils.LOGI(TAG, "video transcode finish: " + ret);
        if (ret != 0) {
            YYFileUtils.deleteFile(videoPath);
            return false;
        }

        if(audio_d[0] > 0) {
            ret = YYMP4Help.MP4FileImportVideoByX264_audio(fromPath,audioPath,true,startTimeMs,endTimeMs );
            LogUtils.LOGI(TAG, "audio transcode finish: " + ret);

            if (ret != 0) {
                YYFileUtils.deleteFile(videoPath);
                YYFileUtils.deleteFile(audioPath);
                return false;
            }

            ret = YYMP4Help.Mp4AudioVideoMerge(audioPath, videoPath, toPath);
        } else {
            // 如果没有音频
            LogUtils.LOGI(TAG, "audio transcode finish: no audio stream ");
            File srcFile = new File(videoPath);
            File dstFile = new File(toPath);
            srcFile.renameTo(dstFile);
        }

        YYFileUtils.deleteFile(videoPath);
        YYFileUtils.deleteFile(audioPath);

        if (ret != 0) {
            return false;
        }
        // 外面需要个进度条百分百的通知
        if (onProgressCallback != null) {
            onProgressCallback.onProgress(100);
        }
        LogUtils.LOGI(TAG, "video file import  end: ");
        return true;
    }

    // 视频帧抽取
    // ImageFormat 抽取的图片格式Jpeg/YUV (YUV仅供制作动态封面webp时使用)
    // outDirPath 视频帧存放目录（app侧维护这个目录）
    // startTime/endTime 抽取的视频期间（同为0，则为全视频）
    // dataNum 抽取的帧数（等间隔抽取）
    // ArrayList<ExtractFrameInfo> frameInfoList 抽取结果（图片路径，以及对应的时间戳）
    static public void extractVideoFrame(String videoPath, IVideoEditor.ImageFormat imageFormat, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList) {

        if (!outDirPath.endsWith("" + File.separator)) {
            outDirPath += "" + File.separator;
        }
        YYFileUtils.createDirectoryIfNeed(outDirPath);

        if (imageFormat == IVideoEditor.ImageFormat.IMAGE_JPEG) {
            YYMP4Help.extractVideoFrameToJpg(videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList);
        } else if (imageFormat == IVideoEditor.ImageFormat.IMAGE_YUV) {
            YYMP4Help.extractVideoFrameToYuv(videoPath, outDirPath, startTime, endTime, dataNum, frameInfoList);
        }

//        for(int i = 0; i < frameInfoList.size(); ++i) {
//            LogUtils.LOGI("DDDDDDD", " " + frameInfoList.get(i).getFilePath() + " " + frameInfoList.get(i).getTimeStampMili());
//        }
    }

    // webp动态图制作
    // frameInfoList 输入图片列表（视频帧抽取的YUV）
    // outputPath 动态图保存位置
    static public boolean createImageWebp(ArrayList<ExtractFrameInfo> frameInfoList, String outputPath) {
        int lastIndex = outputPath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            return false;
        }

        if (frameInfoList == null || frameInfoList.size() < 1) {
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
}
