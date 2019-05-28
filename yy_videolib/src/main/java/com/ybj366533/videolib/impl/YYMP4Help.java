package com.ybj366533.videolib.impl;

import android.support.annotation.Keep;

import com.ybj366533.videolib.editor.ExtractFrameInfo;

import java.util.ArrayList;

/**
 * Created by YY on 2018/2/5.
 */

public class YYMP4Help {
    final public static  int IMG_FORMAT_JPEG = 1;
    final public static int IMG_FORMAT_YUV = 2;

    // todo so 重复load
    private static boolean _libraryLoaded = false;

    static {
        if( !_libraryLoaded ) {

            try {
                System.loadLibrary("yyvideo");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            _libraryLoaded = true;
        }
    }

    public interface OnProgressListener{
        // 被NDK访问，需要keep
        // 返回值0，正常
        // 返回值1，停止合成
        @Keep
        int onProgress(int progress);
    }

    // 到这里时，so应该已经laod了。

    public static int Mp4VideoClipsMerge(String[] filstList, String outputFilePath, String outputFilePathRev, String tempFolder) {
        // 0 正常
        // 其他 合成失败
        return _Mp4VideoClipsMerge(filstList,outputFilePath, outputFilePathRev, tempFolder);

        // 不导出 倒序视频
        // 可把倒序文件路径和临时文件路径设置为null
        //return _Mp4VideoClipsMerge(filstList,outputFilePath, null, tempFolder);
    }

    public static int Mp4AudioVideoMerge(String audioFilePath, String videoFilePath, String outputFilePath) {
        // 0 正常
        // 其他 合成失败
        return  _Mp4AudioVideoMerge( audioFilePath, videoFilePath, outputFilePath);
    }

    public static void setMp4VideoCover(String fileNameFrom,String fileNameTo, String fileNameTag){
        _MP4FileSetCoverJNI(fileNameFrom, fileNameTo, fileNameTag);
    }

    public static float[] getAudioWaveForm(String filePath, int startTime, int endTime, int dataNum ) {
        float[] audioData = new float[dataNum];
        int num = _AudioExtractWaveForm(filePath, startTime, endTime, audioData, dataNum);
        if(num > 0) {
            float[] out_audioData = new float[num];
            for (int i = 0; i< num; ++i) {
                out_audioData[i] = audioData[i];
            }
            return out_audioData;
        } else {
            return null;
        }
    }

    public static void extractAndScaleVideoFrameToJpg(String inFilePath, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList, float scale){
        if (frameInfoList == null) {
            return;
        }
        frameInfoList.clear();
        String outFilePrefix = "F";
        int[] dataTimeStamp = new int[dataNum];
        int num = _Mp4VideoExtractFrame(inFilePath, outDirPath, outFilePrefix, startTime, endTime, dataTimeStamp, dataNum, IMG_FORMAT_JPEG, scale);
        for(int i = 0; i < num; ++i) {
            frameInfoList.add(new ExtractFrameInfo(outDirPath+outFilePrefix+"_"+i+".jpg", dataTimeStamp[i]));
        }

    }

    public static void extractVideoFrameToJpg(String inFilePath, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList){
        if (frameInfoList == null) {
            return;
        }
        frameInfoList.clear();
        String outFilePrefix = "F";
        int[] dataTimeStamp = new int[dataNum];
        int num = _Mp4VideoExtractFrame(inFilePath, outDirPath, outFilePrefix, startTime, endTime, dataTimeStamp, dataNum, IMG_FORMAT_JPEG, 1.0f);
        for(int i = 0; i < num; ++i) {
            frameInfoList.add(new ExtractFrameInfo(outDirPath+outFilePrefix+"_"+i+".jpg", dataTimeStamp[i]));
        }

    }

    public static void extractAndScaleVideoFrameToYuv(String inFilePath, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList, float scale){
        if (frameInfoList == null) {
            return;
        }
        frameInfoList.clear();
        String outFilePrefix = "F";
        int[] dataTimeStamp = new int[dataNum];
        int num = _Mp4VideoExtractFrame(inFilePath, outDirPath, outFilePrefix, startTime, endTime, dataTimeStamp, dataNum, IMG_FORMAT_YUV, scale);
        // todo 还是ndk把路径返回回来？
        for(int i = 0; i < num; ++i) {

            frameInfoList.add(new ExtractFrameInfo(outDirPath+outFilePrefix+"_"+i+".yuv", dataTimeStamp[i]));
        }

    }

    public static void extractVideoFrameToYuv(String inFilePath, String outDirPath, int startTime, int endTime, int dataNum, ArrayList<ExtractFrameInfo> frameInfoList){
        if (frameInfoList == null) {
            return;
        }
        frameInfoList.clear();
        String outFilePrefix = "F";
        int[] dataTimeStamp = new int[dataNum];
        int num = _Mp4VideoExtractFrame(inFilePath, outDirPath, outFilePrefix, startTime, endTime, dataTimeStamp, dataNum, IMG_FORMAT_YUV, 1.0f);
        // todo 还是ndk把路径返回回来？
        for(int i = 0; i < num; ++i) {

            frameInfoList.add(new ExtractFrameInfo(outDirPath+outFilePrefix+"_"+i+".yuv", dataTimeStamp[i]));
        }

    }

    public static void createWebpFromYuv(ArrayList<ExtractFrameInfo> frameInfoList, String outputFilePath) {
        if(frameInfoList == null || frameInfoList.size() < 1) {
            return;
        }
        // jpeg to png
        // 因为可能用yuv，暂时 todo
        String[] filstList = new String[frameInfoList.size()];
        for (int i = 0; i < frameInfoList.size(); ++i) {
            filstList[i] = frameInfoList.get(i).getFilePath();
        }
        //String[] filstList = {"/storage/emulated/0/DCIM/keyframe4/F_0.yuv", "/storage/emulated/0/DCIM/keyframe4/F_8.yuv", "/storage/emulated/0/DCIM/keyframe4/F_16.yuv"};
        //String outputFilePath = "/storage/emulated/0/DCIM/fff.webp";
        _ImgToWebp(filstList, outputFilePath);
    }

    public static int MP4FileTranscodeByX264(String fileNameFrom,String fileNameTo,int gopsize,OnProgressListener listener){
        return _MP4FileTranscodeByX264(fileNameFrom, fileNameTo, gopsize,listener);
    }

    // 只有视频转码(可裁剪)
    public static int MP4FileImportVideoByX264_video(String fileNameFrom,String fileNameTo,int gopsize,OnProgressListener listener, int width, int height, boolean cropFlag, int startTimeMs, int endTimeMs){
        return _MP4FileImportVideoByX264_video(fileNameFrom, fileNameTo, gopsize,listener,width, height, (cropFlag==true?1:0), startTimeMs, endTimeMs);
    }

    // 音频裁剪
    public static int MP4FileImportVideoByX264_audio(String fileNameFrom,String fileNameTo,boolean cropFlag, int startTimeMs, int endTimeMs){
        return _MP4FileImportVideoByX264_audio(fileNameFrom, fileNameTo, (cropFlag==true?1:0), startTimeMs, endTimeMs);
    }

    public static int GetVideoInfo(String fileName, int[] video_duration, int[] audio_duration, int[] videoSize){
        int[] d = new int[2];   ////d[0] 视频duration   d[1] 音频duration
        int ret = _GetVideoInfo(fileName, d, videoSize);
        video_duration[0] = d[0];
        audio_duration[0] = d[1];
        return ret;
    }

    @Keep
    public static native int _Mp4VideoClipsMerge(String[] filstList, String outputFilePath, String outputFilePathRev, String tempFolder);
    @Keep
    public static native int _Mp4AudioVideoMerge( String audioFilePath, String videoFilePath, String outputFilePath);
    @Keep
    public native static void _MP4FileSetCoverJNI(String fileNameFrom, String fileNameTo, String fileTag);

    @Keep
    public native static int _Mp4VideoExtractFrame(String inFilePath, String outDirPath, String outFilePrefix, int startTime, int endTime,
                          int[] dataTimeStamp, int dataNum, int outImgFormat, float scale);
    @Keep
    public native static int _AudioExtractWaveForm(String inFilePath, int startTime, int endTime, float[] audioData, int dataNum);

    @Keep
    public static native int _ImgToWebp(String[] filstList, String outputFilePath);

    @Keep
    public native static int _MP4FileTranscodeByX264(String fileNameFrom, String fileNameTo, int gopsize, Object object);
    @Keep
    public native static int _MP4FileImportVideoByX264_video(String fileNameFrom, String fileNameTo, int gopsize, Object object, int width, int height,int cropFlag, int startTimeMs, int endTimeMs);
    @Keep
    public native static int _MP4FileImportVideoByX264_audio(String fileNameFrom, String fileNameTo, int cropFlag, int startTimeMs, int endTimeMs);


    @Keep
    public native static int _GetVideoInfo(String fileNameFrom, int[] duration, int[] videoSize);
}
