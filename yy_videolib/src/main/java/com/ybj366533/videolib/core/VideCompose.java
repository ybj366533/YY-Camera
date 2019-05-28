package com.ybj366533.videolib.core;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioTrack;
import android.net.Uri;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

import com.ybj366533.videolib.editor.EditCallback;
import com.ybj366533.videolib.editor.IVideoEditor;
import com.ybj366533.videolib.impl.YYMP4Help;
import com.ybj366533.videolib.impl.recorder.gles.EglCore;
import com.ybj366533.videolib.impl.recorder.gles.WindowSurface;
import com.ybj366533.videolib.impl.setting.AVStreamSetting;
import com.ybj366533.videolib.impl.texture.BaseTexture;
import com.ybj366533.videolib.impl.texture.SurfaceTexture;
import com.ybj366533.videolib.impl.utils.DispRect;
import com.ybj366533.videolib.impl.utils.YYFileUtils;
import com.ybj366533.videolib.stream.VideoComposeStreamer;
import com.ybj366533.videolib.utils.LogUtils;
import com.ybj366533.videolib.videoplayer.VideoDemuxer;
import com.ybj366533.videolib.videoplayer.VideoPlayer;
import com.ybj366533.gtvimage.gtvfilter.filter.base.GTVImageFilter;
import com.ybj366533.gtvimage.gtvfilter.utils.TextureRotationUtil;

import net.surina.soundtouch.SoundTouch;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.ybj366533.videolib.videoplayer.VideoPlayer.YY_PLAYER_STREAM_EOF;


public class VideCompose implements BaseTexture.TextureListener {

    private static final String TAG = "VideCompose";

    private SurfaceTexture surfaceTexture;


    private String inVideoFilePath;
    private String outVideoFilePath;
    //private VideoPlayer gtVideoPlayer;
    private AudioTrack audioTrack;

    private String musicPath;
    long startTimeMili;
    private VideoPlayer gtMusicPlayer;    // 不是严格意义的播放器，只为了获取音乐数据
    private boolean musicPrepared = false;

    private Surface mSf;

    VideoDemuxer videoDemuxer;

    // 源图像高度和宽度
    protected int imageWidth = 0;
    protected int imageHeight = 0;

    private EGLContext mSharingContext;
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private int mTextureId;

    private FloatBuffer gLCubeBuffer;
    private FloatBuffer gLTextureBuffer;

    protected boolean videoIsRendered;

    private SoundTouch mSoundTouch;

    private boolean composeCancelFlag = false;

    private EditCallback editCallback;

    public void setEditCallback(EditCallback editCallback) {
        this.editCallback = editCallback;
    }

    private IPCMAudioCallback rawAudioCallback;

    public void setRawAudioCallback(IPCMAudioCallback c) {
        this.rawAudioCallback = c;
    }

//    public interface PlayCallback {
//        void onPrepared();
//        void onCompletion();
//    }

    public interface ComposeCallback {
        void onPrepared();

        void onProgress(int duration);

        void onCompletion(int reason);      // 0:normal  -1: cancel
    }

    private ComposeCallback composeCallback;
    //private PlayCallback playCallback;
//    public void setPlayCallback(PlayCallback playCallback) {
//        this.playCallback = playCallback;
//    }


    //private Context mAppContext;

    EffectFilterTypeManage effectFilterTypeManage;

    VideoComposeVideoProessor videoComposeVideoProessor;  // 负责从surfacetexutre获取视频，特效处理，log处理 和  PlayerVideoProcessor 有较高重合度。

    private long mFrameCount = 0;

    public VideCompose(EGLContext context) {


        gLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        gLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        gLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        mSoundTouch = new SoundTouch();
        mSoundTouch.setTempo(0.5f);

    }

    public void startCompose(String filePathFrom, String filePathTo, int imageWidth, int imageHeight, ComposeCallback composeCallback) {

        this.inVideoFilePath = filePathFrom;
        this.outVideoFilePath = filePathTo;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.composeCallback = composeCallback;

        mFrameCount = 0;

        //this.gtvPlayerVideoProcessor = gtvPlayerVideoProcessor;

        // todo 这部分需要放在线程吗？
//        SurfaceTexture s = new SurfaceTexture();
//        s.init();

        this.surfaceTexture = new SurfaceTexture();
        this.surfaceTexture.init();
        //this.surfaceTexture.addListener(listener);
        this.surfaceTexture.addListener(this);

        //temp
        if (this.musicPath != null) {
            openMusic(musicPath);
        }


        mSf = new Surface((android.graphics.SurfaceTexture) this.surfaceTexture.getTexture());
//        this.playCallback = playCallback; //temp
//        openVideo(playUrl, mSf);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                compose_work_thread();
            }
        };
        Thread t = new Thread(r);
        running_flag = true;
        t.start();

    }

    private GTVImageFilter filter;
    VideoComposeStreamer videoComposeStreamer;
    int lastVideoTimeStamp = -1;
    private boolean videoIsEncoding = false;    // 用于判断是否读取下一帧
    long videoPtsUs = -1;       // 微妙

    private void compose_work_thread() {
        videoDemuxer = new VideoDemuxer(this.inVideoFilePath);
        videoDemuxer.open(this.inVideoFilePath, this.mSf);

        if (cropRangeStartTime <= 0 && cropRangeEndTime <= 0) {

        } else {
            if (cropRangeStartTime <= 0) {
                cropRangeStartTime = 0;
            }

//            if (cropRangeEndTime <= 0) {
//                cropRangeEndTime = p.getDuration();
//            }
            videoDemuxer.setRange(cropRangeStartTime, cropRangeEndTime);
        }


        int lastIndex = outVideoFilePath.lastIndexOf(File.separator);
        if (lastIndex < 0) {
            LogUtils.LOGI(TAG, "path error");
            //return;
        }
        String dirPath = outVideoFilePath.substring(0, lastIndex + 1);

        String audioFilePath = dirPath + "a.mp4";
        String videoFilePath = dirPath + "v.mp4";

        boolean onlyIFrame = false;
        AVStreamSetting setting = AVStreamSetting.settingForVideoRecord(onlyIFrame ? 20 * 1000000 : 4500 * 1000, imageWidth, imageHeight, onlyIFrame);
        videoComposeStreamer = new VideoComposeStreamer(audioFilePath, videoFilePath, setting, mSharingContext);
        videoComposeStreamer.start();

        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, videoComposeStreamer.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        videoComposeVideoProessor = new VideoComposeVideoProessor(imageWidth, imageHeight);
        videoComposeVideoProessor.init();

        if (this.logoImage != null && this.logoRect != null) {
            this.videoComposeVideoProessor.loadLogoToRect(0, logoImage, logoRect);
            this.logoImage = null;
            this.logoRect = null;
        }

        if (this.logoImage2 != null && this.logoRect2 != null) {
            this.videoComposeVideoProessor.loadLogoToRect(1, logoImage2, logoRect2);
            this.logoImage = null;
            this.logoRect = null;
        }

        if (this.animationFolder != null && this.animationRect != null) {
            this.videoComposeVideoProessor.setAnimation(this.animationPrefix, this.animationFolder, this.animImageInterval, this.animationRect);
        }

        filter = new GTVImageFilter();
        if (filter != null) {
            filter.init();
            filter.onInputSizeChanged(imageWidth, imageWidth);
            filter.onDisplaySizeChanged(imageWidth, imageWidth);        //todo
        }


        int count = 0;
        while (videoDemuxer.checkEof() == 0 && composeCancelFlag == false) {

            prepareMusicFrame();
            //demuxAudioFrame();

            int dumuxAudioLen = demuxAudioFrame();

            dumuxVideoFrame();

            // todo 避免过度循环，如果什么处理都没情况下，sleep？

        }
        //videoDemuxer.close();
        videoComposeStreamer.closeStream();

        if (composeCancelFlag == false) {
            YYMP4Help.Mp4AudioVideoMerge(audioFilePath, videoFilePath, outVideoFilePath);
        }

        YYFileUtils.deleteFile(audioFilePath);
        YYFileUtils.deleteFile(videoFilePath);

        videoComposeVideoProessor.destroy();

        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (filter != null) {
            filter.destroy();
            filter = null;
            //type = MagicFilterType.NONE;
        }

        if (this.composeCallback != null) {
            this.composeCallback.onCompletion(composeCancelFlag == false ? 0 : -1);
        }
    }

    // 预先从文件读取音乐
    private void prepareMusicFrame(){
        if (musicPath != null && (gtMusicPlayer == null || musicPrepared == false)) {
            LogUtils.LOGI(TAG, "music is not prepared");
            int waitCount = 0;
            while((musicPrepared == false) && (waitCount < 10)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                waitCount++;
            }
        }
        if (gtMusicPlayer != null && musicPrepared == true) {
            while (music_audio_buffer_list.size() < masuci_buffer_count) {

                byte[] d = new byte[audio_read_size];
                int musicLen = gtMusicPlayer.pullAudioData(d);
                if (musicLen > 0) {
                    byte[] e = d;
                    if (musicLen < audio_read_size) {
                        e = new byte[musicLen];
                        System.arraycopy(d, 0, e, 0, musicLen);
                    }

                    music_audio_buffer_list.add(d);
                } else {
                    // 2018/3/25 音乐保持循环播放
                    //break;
                    if (gtMusicPlayer.checkStreamStatus() == YY_PLAYER_STREAM_EOF) {
                        //循环播放
                        gtMusicPlayer.seekTo(0);
                    } else {
                        try {
                            Thread.sleep(20);
                        } catch (Exception e) {

                        }
                    }
                    //break;      // 担心seek的不够快，会在这边死循环，seek后下次再取数据，音乐数据的缓存应该足够的。
                }
            }

        }
    }

    private void dumuxVideoFrame() {
        // 第一次或者读取的视频已处理
        if (videoIsEncoding == false) {
            int[] size = new int[2];
            int timestamp = videoDemuxer.getNextVideoTimestamp(size);
            if (timestamp >= 0) {
                videoIsEncoding = true;
                videoDemuxer.peekNextVideo();

                if (this.composeCallback != null) {
                    this.composeCallback.onProgress(videoPtsUs > 0 ? (int) (videoPtsUs / 1000) : 0);
                }

                //videoDemuxer.removeNextVideo();
            } else {
            }
        } else {
        }

        runPendingOnDrawTasks();
    }

    private int readAudioBytes = 0;
    private final int audio_read_size = 4096;
    private short[] audio_final_buffer_short = new short[audio_read_size / 2];
    private byte[] audio_final_buffer_byte = new byte[audio_read_size];         //存放慢动作，配乐，禁音乐等处理过的声音

    private int demuxAudioFrame() {
        int len = videoDemuxer.pullAudioData(audio_buffer);
        if (len > 0) {

            // 落入慢动作区间
            if (isSlowMotion(readAudioBytes * 1000 / (44100 * 4))) {
                if (tempo_out_data == null) {
                    tempo_out_data = new short[8192];
                }

                if (dataShort == null || dataShort.length < len) {
                    dataShort = new short[len];     // 实际一半就够，但是多点安全点
                }

                ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(dataShort, 0, len / 2);
                int tempo_out_len = mSoundTouch.processMemory(dataShort, len / 2, tempo_out_data, 8192);

                if (tempo_out_len > 0) {

                    if (audio_final_buffer_short == null || audio_final_buffer_short.length != tempo_out_len / 2) {
                        audio_final_buffer_short = new short[tempo_out_len / 2];
                    }
                    System.arraycopy(tempo_out_data, 0, audio_final_buffer_short, 0, audio_final_buffer_short.length);


                } else {
                    // todo sleep?
                    audio_final_buffer_short = null; //mSoundTouch 没有数据输出 todo
                }
            } else {
                if (audio_final_buffer_short == null || audio_final_buffer_short.length != len / 2) {
                    audio_final_buffer_short = new short[len / 2];
                }
                ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audio_final_buffer_short, 0, len / 2);
            }

            if (audio_final_buffer_short != null) {
                if (music_audio_buffer.length < audio_final_buffer_short.length * 2) {
                    music_audio_buffer = new byte[audio_final_buffer_short.length * 2];
                }

                if (music_audio_buffer_short.length < audio_final_buffer_short.length) {
                    music_audio_buffer_short = new short[audio_final_buffer_short.length];
                }
                int readLen = readMusicDataFromBuffer(music_audio_buffer, audio_final_buffer_short.length * 2);

                if (readLen >= audio_final_buffer_short.length * 2) {
                    // 音乐不够的时候就不合成
                    // ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audio_buffer_short, 0, len/2);
                    ByteBuffer.wrap(music_audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music_audio_buffer_short, 0, audio_final_buffer_short.length);
                    for (int i = 0; i < audio_final_buffer_short.length; ++i) {
                        //int tmp = audio_buffer_short[i] + music_audio_buffer_short[i];
                        int tmp = (int) (audio_final_buffer_short[i] * origAudioVolume + music_audio_buffer_short[i] * musicVolume);
                        if (tmp > 32767)
                            tmp = 32767;
                        else if (tmp < -32768)
                            tmp = -32768;

                        audio_final_buffer_short[i] = (short) tmp;
                    }

                    //ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audio_buffer_short, 0, len/2);
                } else {
                    // 因为音乐是循环播放的，可以判定没有音乐
                    //ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audio_buffer_short, 0, len/2);
                    //ByteBuffer.wrap(music_audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music_audio_buffer_short, 0, len/2);
                    for (int i = 0; i < audio_final_buffer_short.length; ++i) {
                        //int tmp = audio_buffer_short[i] + music_audio_buffer_short[i];
                        int tmp = (int) (audio_final_buffer_short[i] * origAudioVolume);
                        if (tmp > 32767)
                            tmp = 32767;
                        else if (tmp < -32768)
                            tmp = -32768;

                        audio_final_buffer_short[i] = (short) tmp;
                    }

                    //ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audio_buffer_short, 0, len/2);
                }

                if (audio_final_buffer_byte == null || audio_final_buffer_byte.length != audio_final_buffer_short.length * 2) {
                    audio_final_buffer_byte = new byte[audio_final_buffer_short.length * 2];
                }

                ByteBuffer.wrap(audio_final_buffer_byte).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audio_final_buffer_short, 0, audio_final_buffer_short.length);

                videoComposeStreamer.writeAudioData(audio_final_buffer_byte);
            }

            readAudioBytes += len;
        }

        return len;
    }

    private boolean isSlowMotion(int timeStamp) {
        //int audioTimestamp = readAudioBytes*1000/(44100*4);
        if ((slowMotionEnable == true) && (slowMotionStartTime <= timeStamp) && (timeStamp <= slowMotionEndTime)) {
            return true;
        } else {
            return false;
        }
    }


    public void destroy() {


        videoDemuxer.close();
        videoDemuxer = null;

        if (this.surfaceTexture != null) {
            this.surfaceTexture.destroy();
            this.surfaceTexture = null;
        }

        closeVideo();
    }


    public void closeVideo() {

    }


    //private final int audio_read_size = 4096;
    protected boolean running_flag = false;
    private boolean pause_flag = false;
    protected byte[] audio_buffer = new byte[audio_read_size];
    protected byte[] music_audio_buffer = new byte[audio_read_size];
    private List<byte[]> music_audio_buffer_list = new ArrayList<>();
    private int masuci_buffer_count = 8; // 4096*8;


    protected short[] audio_buffer_short = new short[audio_read_size / 2];
    protected short[] music_audio_buffer_short = new short[audio_read_size / 2];

    private short[] dataShort;
    private short[] tempo_out_data;
    private byte[] tempo_out_data_byte = null;

    private int writeCount = 0;
    private int no_data_wait_count = 0; // 因为时间戳的问题，有可能结束不了，如果连续10次没数据，等同于结束

    private int readMusicDataFromBuffer(byte[] readData, int len) {
        if (len > readData.length) {
            return 0;
        }
        int readCnt = 0;
        int left = len;//readData.length;
//int start_size = playedAudioList.size();
        while (music_audio_buffer_list.size() > 0 && left > 0) {

            byte[] d = music_audio_buffer_list.remove(0);

            if (d.length <= left) {
                System.arraycopy(d, 0, readData, readCnt, d.length);
                readCnt += d.length;

                left -= d.length;
            } else {
                System.arraycopy(d, 0, readData, readCnt, left);
                readCnt += left;

                // 没有用完，将剩下的数据塞回队列
                byte[] newD = new byte[d.length - left];
                System.arraycopy(d, left, newD, 0, d.length - left);
                music_audio_buffer_list.add(0, newD);

                left = 0;
            }
        }

        return readCnt;
    }


    //private IVideoEditor.EffectType magicFilterType = IVideoEditor.EffectType.EFFECT_NO;
    //private int filterStartTime = 0;


    private boolean slowMotionEnable = false;
    private long slowMotionStartTime = 0;
    private long slowMotionEndTime = 0;

    private int cropRangeStartTime = 0;
    private int cropRangeEndTime = 0;

    private Bitmap logoImage;
    private DispRect logoRect;

    private Bitmap logoImage2;
    private DispRect logoRect2;

//    public void setAudio_buffer(byte[] audio_buffer) {
//        this.audio_buffer = audio_buffer;
//    }

    public void setSlowMotionEnable(boolean slowMotionEnable) {
        this.slowMotionEnable = slowMotionEnable;
    }

    public void setSlowMotionStartTime(long startTime) {
        slowMotionStartTime = startTime;
    }

    public void setSlowMotionEndTime(long endTime) {
        slowMotionEndTime = endTime;
    }

    public void setVideoCropRange(int startTimeMili, int endTimeMili) {
        this.cropRangeStartTime = startTimeMili;
        this.cropRangeEndTime = endTimeMili;
    }


    public void setMusicPath(String musicPath, int startTimeMili) {
        if (musicPath == null) {
            gtMusicPlayer = null;
        }

        this.musicPath = musicPath;
        this.startTimeMili = startTimeMili;

    }

    // 设置logo图片
    public boolean setLogoBitmapAtRect(Bitmap bmp, DispRect r) {

        if (bmp == null || r == null) {
            return false;
        }

        this.logoImage = bmp;
        this.logoRect = r;

        return true;
    }

    public boolean setLogoBitmapAtRect2(Bitmap bmp, DispRect r) {

        if (bmp == null || r == null) {
            return false;
        }

        this.logoImage2 = bmp;
        this.logoRect2 = r;

        return true;
    }

    private Object musicReadyFence = new Object();

    public void openMusic(String musicPath) {

        if (musicPath == null) {
            return;
        }

        Uri mUri;

        gtMusicPlayer = null;

        mUri = Uri.parse(musicPath);

        try {
            // 可以考虑 这部分代码打包成一个函数，类似createplayer？
            gtMusicPlayer = new VideoPlayer(mUri.toString());

            gtMusicPlayer.setSurface(null);
            gtMusicPlayer.setLogLevel(0);
            gtMusicPlayer.setPlayerLogOutput(new VideoPlayer.IYYVideoPlayerLogOutput() {
                @Override
                public void onLogOutput(String logMsg) {
                    Log.e("GTV", logMsg);
                }
            });

            gtMusicPlayer.setPlayerEventLisenter(new VideoPlayer.IYYVideoPlayerListener() {
                @Override
                public void onPrepared(VideoPlayer p) {
                    //------------------------------------------
                    Log.e(TAG, " music prepared");
                    musicPrepared = true;
                    gtMusicPlayer.seekTo((int) VideCompose.this.startTimeMili);
                    gtMusicPlayer.setRange((int) VideCompose.this.startTimeMili, gtMusicPlayer.getDuration() + 1000);
                    synchronized (musicReadyFence) {
                        musicReadyFence.notify();
                    }

                }

                @Override
                public void onCompletion(VideoPlayer p, int errorCode) {
                    Log.e(TAG, " music completion");


                }
            });
            gtMusicPlayer.play();
            synchronized (musicReadyFence) {
                musicReadyFence.wait();
            }


        } catch (Exception ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }

        //return mMediaPlayer;
        return;
    }

    public void setEffectFilterSetting(EffectFilterTypeManage effectFilterTypeManage) {
        this.effectFilterTypeManage = effectFilterTypeManage;
    }

    private float origAudioVolume = 1.0f;
    private float musicVolume = 1.0f;

    public void setOrigAudioVolume(float origAudioVolume) {
        this.origAudioVolume = origAudioVolume * origAudioVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume * musicVolume;
    }

    @Override
    public void onTextureReady(int type, BaseTexture obj) {
//        if( this.glSurfaceView == null )
//            return;

        this.videoIsRendered = true;

        //final VideoObject self = this;
        final SurfaceTexture gtvSurfaceTexture = (SurfaceTexture) obj;
        android.graphics.SurfaceTexture surfaceTexture = (android.graphics.SurfaceTexture) gtvSurfaceTexture.getTexture();
        this.myQueueEvent(new Runnable() {
            @Override
            public void run() {
                try {
                    //int oesId = gtvSurfaceTexture.updateTexture();

                    int[] size = new int[2];
                    int currTimestamp = videoDemuxer.getNextVideoTimestamp(size);

                    if (effectFilterTypeManage != null) {
                        //检查时间设定filter
                        IVideoEditor.EffectType type = effectFilterTypeManage.getEffectFilterType(currTimestamp);
                        //if (type != magicFilterType) {
                        //Log.e("testtestFilter1", " " + getCurrentPosition() + " " + type + " " +magicFilterType);
                        videoComposeVideoProessor.setMagicFilterType(type);
                        //}
                    }

                    videoComposeVideoProessor.setDuration(currTimestamp);

                    videoComposeVideoProessor.videoProcess(0, gtvSurfaceTexture);
                    int textureid = videoComposeVideoProessor.getLastTextureId();

                    mInputWindowSurface.makeCurrent();

                    filter.onDrawFrame(textureid, gLCubeBuffer, gLTextureBuffer);

//                    if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR && glesErrCount++ <= MAX_GLES_ERR_COUNT ){
//                        LogUtils.LOGE("ES20_ERROR", "handleFrameAvailable: onDrawFrame glError -> " + error);
//                    }

                    if (videoPtsUs < 0) {
                        videoPtsUs = 0; //(long)currTimestamp * 1000;   //考虑到裁剪，第一帧固定为 0
                    } else {
                        if (isSlowMotion(currTimestamp)) {
                            videoPtsUs += (long) (currTimestamp - lastVideoTimeStamp) * 1000 * 2;
                        } else {
                            videoPtsUs += (long) (currTimestamp - lastVideoTimeStamp) * 1000;
                        }
                    }

                    long timestamp = mFrameCount * 1000 * 1000 * 1000;
                    mFrameCount++;

                    //mInputWindowSurface.setPresentationTime(timestamp);
                    mInputWindowSurface.setPresentationTime(videoPtsUs * 1000);
                    mInputWindowSurface.swapBuffers();
                    videoComposeStreamer.writeVideoData(videoPtsUs);

                    videoDemuxer.removeNextVideo();
                    videoIsEncoding = false;
                    lastVideoTimeStamp = currTimestamp;


                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

    }

    private final LinkedList<Runnable> mRunOnDraw = new LinkedList<>();

    protected void runPendingOnDrawTasks() {

        synchronized (mRunOnDraw) {
            while (!mRunOnDraw.isEmpty()) {

                mRunOnDraw.removeLast().run();
                //mRunOnDraw.clear();
            }
        }
    }

    public void myQueueEvent(final Runnable runnable) {

        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }

        //glSurfaceView.requestRender();
    }

    public void stopCompose() {
        composeCancelFlag = true;
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
}
