package com.gtv.cloud.core;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.gtv.cloud.editor.IGTVVideoEditor;
import com.gtv.cloud.impl.texture.GTVBaseTexture;
import com.gtv.cloud.impl.texture.GTVSurfaceTexture;
import com.gtv.cloud.utils.LogUtils;
import com.gtv.cloud.videoplayer.GTVideoPlayer;
import com.gtv.cloud.widget.GTVEditorViewRender;

import net.surina.soundtouch.SoundTouch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.gtv.cloud.videoplayer.GTVideoPlayer.GTV_PLAYER_STREAM_EOF;


/**
 * Created by gtv on 2017/1/21.
 */

public class GTVPlayerPreview implements GTVBaseTexture.TextureListener {

    private static final String TAG = "PlayerPreview";

    private GTVSurfaceTexture surfaceTexture;


    private String playUrl;
    private GTVideoPlayer gtVideoPlayer;
    private AudioTrack audioTrack;

    private String musicPath;
    int startTimeMili;
    private GTVideoPlayer gtMusicPlayer;    // 不是严格意义的播放器，只为了获取音乐数据
    private boolean musicPrepared = false;

    private Surface mSf;
    private GTVBaseTexture.TextureListener mTextureListener;

    private SoundTouch mSoundTouch;

    private boolean firstFrameRendered = false;     // 首帧即使暂停也显示一下。


    private IPCMAudioCallback rawAudioCallback;
    public void setRawAudioCallback(IPCMAudioCallback c) {
        this.rawAudioCallback = c;
    }

    @Override
    public void onTextureReady(int type, GTVBaseTexture obj) {

        if( (gtvEditorViewRender.isDisPlayToScreenFlag() == true) && this.mTextureListener != null ) {

            firstFrameRendered = true;
            this.mTextureListener.onTextureReady(type, obj);
        } else {
            this.gtvEditorViewRender.myQueueEvent(new Runnable() {
                @Override
                public void run() {
                    try {

                        int oesId = surfaceTexture.updateTexture();
                    }
                    catch(Exception ex) {

                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    public interface PlayCallback {
        void onPrepared();
        void onCompletion();
    }
    private PlayCallback playCallback;
    public void setPlayCallback(PlayCallback playCallback) {
        this.playCallback = playCallback;
    }


    private Context mAppContext;


    GTVEditorViewRender gtvEditorViewRender;

    GLSurfaceView glSurfaceView;

    GTVPlayerVideoProcessor gtvPlayerVideoProcessor;

    private int cropRangeStartTime = 0;
    private int cropRangeEndTime = 0;


    private float origAudioVolume = 1.0f;
    private float musicVolume = 1.0f;


    public GTVPlayerPreview(GLSurfaceView glSurfaceView, GTVEditorViewRender gtvEditorViewRender, String url) {

        this.glSurfaceView = glSurfaceView;
        //super(glSurfaceView);
        this.gtvEditorViewRender = gtvEditorViewRender;
        mAppContext = glSurfaceView.getContext().getApplicationContext();

        this.playUrl = url;

        mSoundTouch = new SoundTouch();
        mSoundTouch.setTempo(0.5f);

    }

    public void init(GTVPlayerVideoProcessor gtvPlayerVideoSource, GTVBaseTexture.TextureListener listener, PlayCallback playCallback) {

        this.gtvPlayerVideoProcessor = gtvPlayerVideoSource;
        GTVSurfaceTexture s = new GTVSurfaceTexture();
        s.init();

        this.surfaceTexture = new GTVSurfaceTexture();
        this.surfaceTexture.init();
        //this.surfaceTexture.addListener(listener);
        this.mTextureListener = listener;
        this.surfaceTexture.addListener(this);


        mSf = new Surface((SurfaceTexture) this.surfaceTexture.getTexture());
        this.playCallback = playCallback;
        openVideo(playUrl, mSf);

        return;
    }




    public void destroy() {

        //super.destroy();

        if( playUrl != null ) {
            LogUtils.LOGW(TAG, "GTVPlayerVideoProcessor destroy --> " + playUrl);
        }


        if( this.surfaceTexture != null ) {
            this.surfaceTexture.destroy();
            this.surfaceTexture = null;
        }

        closeVideo();
    }


//    public void pauseVideo() {
//
//        //super.pauseVideo();
//
//        if (this.gtVideoPlayer != null) {
//
//            this.gtVideoPlayer.setSurface(null);
//        }
//
//    }

    public void resumeVideo() {

        //super.resumeVideo();

        if (this.gtVideoPlayer != null) {

            if( mSf != null )
                this.gtVideoPlayer.setSurface(mSf);
            else
                LogUtils.LOGE(TAG, "mSf is null.");
        }

    }

    public void closeVideo() {

        if (gtVideoPlayer != null) {

            //this.callback = null;

            running_flag = false;       //todo 互斥

            if(gtVideoPlayer != null) {
                // todo 要另起一个线程吗？ 设置setsurface为空有问题吗？
                gtVideoPlayer.setSurface(null);
                gtVideoPlayer.close();
                gtVideoPlayer = null;
            }


            gtVideoPlayer = null;
            mSf = null;

            if(gtMusicPlayer != null) {
                gtMusicPlayer.close();
                gtMusicPlayer = null;
            }

//            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
//            am.abandonAudioFocus(null);
        }
    }


    public GTVideoPlayer openVideo(String uri, Surface sf) {

        if (uri == null ) {
            return null;
        }

        Uri mUri;

        gtVideoPlayer = null;

        mUri = Uri.parse(uri);


        try {
            // 可以考虑 这部分代码打包成一个函数，类似createplayer？
            gtVideoPlayer  = new GTVideoPlayer(mUri.toString());

            gtVideoPlayer.setSurface(sf);
            gtVideoPlayer.setLogLevel(0);
            gtVideoPlayer.setPlayerLogOutput(new GTVideoPlayer.IGTVideoPlayerLogOutput() {
                @Override
                public void onLogOutput(String logMsg) {
                    LogUtils.LOGI(TAG, logMsg);
                }
            });

            gtVideoPlayer.setPlayerEventLisenter(new GTVideoPlayer.IGTVideoPlayerListener() {
                @Override
                public void onPrepared(GTVideoPlayer p) {
                    //------------------------------------------
                    LogUtils.LOGI(TAG, "prepared");

                    //updateVideoSizeFromStream(p.getVideoWidth(), p.getVideoHeight());
                    //p.setRange(3000, 10000);

                    int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

                    audioTrack.play();


                    if (playCallback != null) {
                        playCallback.onPrepared();
                    }

                    if (cropRangeStartTime <=0 && cropRangeEndTime <= 0) {

                    } else {
                        if (cropRangeStartTime <= 0) {
                            cropRangeStartTime = 0;
                        }

                        if (cropRangeEndTime <= 0) {
                            cropRangeEndTime = p.getDuration();
                        }
                        p.setRange(cropRangeStartTime, cropRangeEndTime);
                    }


                    // 设置一个定时器，拉取声音
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            loopAudio();
                        }
                    };
                    Thread t = new Thread(r);
                    running_flag = true;
                    t.start();
                }

                @Override
                public void onCompletion(GTVideoPlayer p, int errorCode) {

                    LogUtils.LOGD(TAG, " onCompletion");

                }
            });
            gtVideoPlayer.play();


            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                LogUtils.LOGD(TAG, "Audio focus request granted for VOICE_CALL streams");
            } else {
                LogUtils.LOGE(TAG, "Audio focus request failed");
            }
            am.setMode(AudioManager.MODE_NORMAL);

        } catch (Exception ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }

        //return mMediaPlayer;
        return gtVideoPlayer;
    }

    private final int audio_read_size = 4096;
    protected boolean running_flag = false;
    private    boolean pause_flag = false;
    protected byte[] audio_buffer = new byte[audio_read_size];
    protected byte[] music_audio_buffer = new byte[audio_read_size];
    private List<byte[]> music_audio_buffer_list  = new ArrayList<>();
    private int masuci_buffer_count = 8; // 4096*8;


    protected short[] audio_buffer_short = new short[audio_read_size/2];
    protected short[] music_audio_buffer_short = new short[audio_read_size/2];

    private short[] dataShort;
    private short[] tempo_out_data;
    private byte[] tempo_out_data_byte = null;

    private int writeCount = 0;
    private int no_data_wait_count = 0; // 因为时间戳的问题，有可能结束不了，如果连续10次没数据，等同于结束

    private boolean seek_clear_data = false;            // seek的时候清除缓存的音乐数据。

    protected void loopAudio() {

        while(running_flag == true) {
            if (pause_flag == true) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                int len = gtVideoPlayer.pullAudioData(audio_buffer);
                {
                    if(effectFilterTypeManage != null) {
                        //检查时间设定filter
                        IGTVVideoEditor.EffectType type = effectFilterTypeManage.getEffectFilterType(gtVideoPlayer.currentTimestamp());
                        //if (type != magicFilterType) {
                        //Log.e("testtestFilter1", " " + getCurrentPosition() + " " + type + " " +magicFilterType);
                        this.gtvPlayerVideoProcessor.setMagicFilterType(type);
                        //}
                    }

                }

                if(seek_clear_data == true) {
                    music_audio_buffer_list.clear();
                    seek_clear_data = false;
                }
                if (len > 0 && gtMusicPlayer != null && musicPrepared == true) {
                    while (music_audio_buffer_list.size() < masuci_buffer_count) {

                        byte[] d = new byte[audio_read_size];
                        int musicLen = gtMusicPlayer.pullAudioData(d);
                        if (musicLen > 0 ) {
                            byte[] e = d;
                            if (musicLen < audio_read_size) {
                                e = new byte[musicLen];
                                System.arraycopy(d, 0, e, 0, musicLen);
                            }

                            music_audio_buffer_list.add(d);
                        } else {
                            // 2018/3/25 音乐保持循环播放
                            //break;
                            if (gtMusicPlayer.checkStreamStatus() == GTV_PLAYER_STREAM_EOF) {
                                //循环播放
                                gtMusicPlayer.seekTo(0);
                            }
                            break;      // 担心seek的不够快，会在这边死循环，seek后下次再取数据，音乐数据的缓存应该足够的。
                        }
                    }


                }
                if( len > 0 ) {
                    no_data_wait_count = 0;
                    if ((slowMotionEnable == true) && (slowMotionStartTime <= gtVideoPlayer.currentTimestamp()) && (gtVideoPlayer.currentTimestamp() <= slowMotionEndTime)) {
                        if (tempo_out_data ==  null) {
                            tempo_out_data = new short[8192];
                        }

                        if (dataShort ==  null || dataShort.length < len) {
                            dataShort = new short[len];     // 实际一半就够，但是多点安全点
                        }

                        ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(dataShort, 0, len/2);
                        int tempo_out_len = mSoundTouch.processMemory(dataShort, len/2, tempo_out_data, 8192);

                        if (tempo_out_len > 0) {

                            int tempo_out_len_byte = tempo_out_len;

                            //if (gtMusicPlayer != null && musicPrepared == true) {
                                if(music_audio_buffer.length < tempo_out_len_byte) {
                                    music_audio_buffer = new byte[tempo_out_len_byte];
                                }

                                if(music_audio_buffer_short.length < tempo_out_len) {
                                    music_audio_buffer_short = new short[tempo_out_len];
                                }

                                int readLen = readMusicDataFromBuffer(music_audio_buffer, tempo_out_len_byte);
                                if(readLen >= tempo_out_len_byte) {
                                    // 音乐不够的时候就不合成
                                    //ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audio_buffer_short, 0, len/2);
                                    ByteBuffer.wrap(music_audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music_audio_buffer_short, 0, tempo_out_len_byte/2);
                                    for(int i = 0; i < tempo_out_len_byte/2; ++i) {
                                       // int tmp = tempo_out_data[i] + music_audio_buffer_short[i];
                                        int tmp = (int)(tempo_out_data[i] * origAudioVolume + music_audio_buffer_short[i] * musicVolume);
                                        if(tmp > 32767)
                                            tmp = 32767;
                                        else if(tmp < -32768)
                                            tmp = -32768;

                                        tempo_out_data[i] = (short)tmp;
                                    }

                                    //ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audio_buffer_short, 0, len/2);
                                }else {
                                    for(int i = 0; i < tempo_out_len_byte/2; ++i) {
                                        // int tmp = tempo_out_data[i] + music_audio_buffer_short[i];
                                        int tmp = (int)(tempo_out_data[i] * origAudioVolume);
                                        if(tmp > 32767)
                                            tmp = 32767;
                                        else if(tmp < -32768)
                                            tmp = -32768;

                                        tempo_out_data[i] = (short)tmp;
                                    }
                            }
                            //}

                            if (tempo_out_data_byte == null || tempo_out_len * 2 != tempo_out_data_byte.length) {
                                tempo_out_data_byte = new byte[tempo_out_len * 2];
                            }

                            ByteBuffer.wrap(tempo_out_data_byte).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(tempo_out_data, 0, tempo_out_len/2);

                            if (this.rawAudioCallback != null) {    // todo better judege;
                                this.rawAudioCallback.onRawAudioData(tempo_out_data_byte, 0, tempo_out_len*2);
                                writeCount += tempo_out_len*2;
                                if (writeCount > 2048) {
                                    writeCount -= 2048;
                                    try {
                                        Thread.sleep(5);
                                    }
                                    catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            } else {
                                audioTrack.write(tempo_out_data_byte, 0, tempo_out_len);
                            }

                        } else {
                            // todo sleep?
                        }
                    } else {
                        //audioTrack.write(audio_buffer, 0, len);
                        int readLen = readMusicDataFromBuffer(music_audio_buffer, len);
                        if(readLen >= len) {
                            // 音乐不够的时候就不合成
                            ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audio_buffer_short, 0, len/2);
                            ByteBuffer.wrap(music_audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music_audio_buffer_short, 0, len/2);
                            for(int i = 0; i < len/2; ++i) {
                                int tmp = (int)(audio_buffer_short[i] * origAudioVolume + music_audio_buffer_short[i] * musicVolume);
                                if(tmp > 32767)
                                    tmp = 32767;
                                else if(tmp < -32768)
                                    tmp = -32768;

                                audio_buffer_short[i] = (short)tmp;
                            }

                            ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audio_buffer_short, 0, len/2);
                        } else {
                            ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audio_buffer_short, 0, len/2);
                            //ByteBuffer.wrap(music_audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music_audio_buffer_short, 0, len/2);
                            for(int i = 0; i < len/2; ++i) {
                                int tmp = (int)(audio_buffer_short[i] * origAudioVolume);
                                if(tmp > 32767)
                                    tmp = 32767;
                                else if(tmp < -32768)
                                    tmp = -32768;

                                audio_buffer_short[i] = (short)tmp;
                            }

                            ByteBuffer.wrap(audio_buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audio_buffer_short, 0, len/2);
                        }
                        if (this.rawAudioCallback != null) {    // todo better judege;
                            this.rawAudioCallback.onRawAudioData(audio_buffer, 0, len);
                            writeCount += len;
                            if (writeCount > 2048) {
                                writeCount -= 2048;
                                try {
                                    Thread.sleep(10);
                                }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        } else {
                            audioTrack.write(audio_buffer, 0, len);
                        }
                    }

                } else {
                    no_data_wait_count ++;
                    //LogUtils.DebugLog("00000000000000a", "oncomplete " + gtVideoPlayer.currentTimestamp() + " " + gtVideoPlayer.getDuration());
                    //if (gtVideoPlayer.currentTimestamp() >= gtVideoPlayer.getDuration() - 1 || no_data_wait_count > 20) {
//                    if ((gtVideoPlayer.checkStreamStatus() == GTV_PLAYER_STREAM_EOF)
//                            ||(gtVideoPlayer.currentTimestamp() >= gtVideoPlayer.getDuration() - 1)
//                            ||((cropRangeEndTime > 0) && (gtVideoPlayer.currentTimestamp() + 1000> cropRangeEndTime) && (no_data_wait_count > 100))) {
                    if (gtVideoPlayer.checkStreamStatus() == GTV_PLAYER_STREAM_EOF){
                        //LogUtils.DebugLog("00000000000000a", "oncomplete " + gtVideoPlayer.currentTimestamp() + " " + gtVideoPlayer.getDuration());
                        pause_flag = true;
                        music_audio_buffer_list.clear();
                        if (this.playCallback != null) {
                            this.playCallback.onCompletion();

                        }
                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        // todo 合适？ 如果要多次播放
        if( audioTrack != null ) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        return;
    }

    private int readMusicDataFromBuffer(byte[] readData, int len) {
        if (len > readData.length) {
            return 0;
        }
        int readCnt = 0;
        int left = len;//readData.length;
//int start_size = playedAudioList.size();
        while(music_audio_buffer_list.size()>0 && left>0) {

            byte[] d = music_audio_buffer_list.remove(0);

            if( d.length <= left ) {
                System.arraycopy(d, 0, readData, readCnt, d.length);
                readCnt += d.length;

                left -= d.length;
            }
            else {
                System.arraycopy(d, 0, readData, readCnt, left);
                readCnt += left;

                // 没有用完，将剩下的数据塞回队列
                byte[] newD = new byte[d.length-left];
                System.arraycopy(d, left, newD, 0, d.length-left);
                music_audio_buffer_list.add(0, newD);

                left = 0;
            }
        }

        return readCnt;
    }


    public int getVideoWidth() {
        if (gtVideoPlayer != null) {
            return gtVideoPlayer.getVideoWidth();
        }
        return 0;
    }

    public int getVideoHeight() {
        if (gtVideoPlayer != null) {
            return gtVideoPlayer.getVideoHeight();
        }
        return 0;
    }

    public int getDuration() {
        if (gtVideoPlayer != null) {
            return (int) gtVideoPlayer.getDuration();
        }

        return -1;
    }

    public int getCurrentPosition () {
        if(gtVideoPlayer != null) {
            return (int) gtVideoPlayer.currentTimestamp();
        }

        return -1;
    }

    public void seekTo(int msec) {
        //music_audio_buffer_list.clear();
        seek_clear_data = true;
        if (gtVideoPlayer != null) {
            gtVideoPlayer.seekTo(msec);

            // seek的时候需要刷新seek后的画面，这时候也需要特效
            if(effectFilterTypeManage != null) {
                //检查时间设定filter
                IGTVVideoEditor.EffectType type = effectFilterTypeManage.getEffectFilterType(gtVideoPlayer.currentTimestamp());
                this.gtvPlayerVideoProcessor.setMagicFilterType(type);
            }
        }

        if (gtMusicPlayer != null) {
            int musicSeek = msec - cropRangeStartTime;      // 算出视频从裁剪的开始位置 跳转了多少
            if(musicSeek < 0) {
                musicSeek = 0;
            }
            int musicLen = gtMusicPlayer.getDuration() - startTimeMili;
            if(musicLen > 0) {
                musicSeek = musicSeek % musicLen;       // 配乐短于视频时

                musicSeek += startTimeMili;         // + 配乐设置的开始位置

                gtMusicPlayer.seekTo(musicSeek);
            }


        }
        return;
    }

    public void playPause() {
        if (gtVideoPlayer != null) {
            pause_flag = true;
            gtVideoPlayer.pauseVideo();
        }
    }

    //   = play resume
    public void playStart() {

        if (gtVideoPlayer != null) {
            pause_flag = false;
            gtVideoPlayer.resumeVideo();
        }
    }

    private IGTVVideoEditor.EffectType magicFilterType = IGTVVideoEditor.EffectType.EFFECT_NO;
    private int filterStartTime = 0;


    private boolean slowMotionEnable = false;
    private long slowMotionStartTime = 0;
    private long slowMotionEndTime = 0;

    public void setAudio_buffer(byte[] audio_buffer) {
        this.audio_buffer = audio_buffer;
    }

    public void setSlowMotionEnable(boolean slowMotionEnable) {
        this.slowMotionEnable =  slowMotionEnable;
    }

    public void setSlowMotionStartTime(long startTime) {
        slowMotionStartTime = startTime;
    }

    public void setSlowMotionEndTime(long endTime) {
        slowMotionEndTime = endTime;
    }

    private GTVideoPlayer.IGTVideoPlayerListener listener;
    public void setIGTVideoPlayerListener(GTVideoPlayer.IGTVideoPlayerListener listener) {
        this.listener = listener;
    }

    public void setMusicPath(String musicPath, int startTimeMili){

        this.musicPath = musicPath;
        this.startTimeMili = startTimeMili;
        // 打开音乐
        openMusic(musicPath);

    }

    public void openMusic(String musicPath) {

        musicPrepared = false;      // 切换音乐

        if(gtMusicPlayer != null) {
            gtMusicPlayer.close();
            gtMusicPlayer = null;
        }

        if (musicPath == null ) {
            return;
        }

        Uri mUri;

        gtMusicPlayer = null;

        mUri = Uri.parse(musicPath);

        try {
            // 可以考虑 这部分代码打包成一个函数，类似createplayer？
            gtMusicPlayer  = new GTVideoPlayer(mUri.toString());

            gtMusicPlayer.setSurface(null);
            gtMusicPlayer.setLogLevel(0);
            gtMusicPlayer.setPlayerLogOutput(new GTVideoPlayer.IGTVideoPlayerLogOutput() {
                @Override
                public void onLogOutput(String logMsg) {
                    Log.e("GTV", logMsg);
                }
            });

            gtMusicPlayer.setPlayerEventLisenter(new GTVideoPlayer.IGTVideoPlayerListener() {
                @Override
                public void onPrepared(GTVideoPlayer p) {
                    //------------------------------------------
                    //Log.e("app", "prepared");
                    musicPrepared = true;
                    gtMusicPlayer.setRange((int)startTimeMili, gtMusicPlayer.getDuration()+1000);
                    seekTo(0);
                }

                @Override
                public void onCompletion(GTVideoPlayer p, int errorCode) {
                    //Log.e("app", "completion");


                }
            });
            seek_clear_data = true;
            gtMusicPlayer.play();


        } catch (Exception ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }

        return;
    }

    public void setVideoCropRange(int startTimeMili, int endTimeMili){
        this.cropRangeStartTime = startTimeMili;
        this.cropRangeEndTime = endTimeMili;

        if(gtVideoPlayer != null) {
            gtVideoPlayer.setRange(startTimeMili, endTimeMili);
        }
    }

    public void setOrigAudioVolume(float origAudioVolume) {
        this.origAudioVolume = origAudioVolume * origAudioVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume * musicVolume;
    }

    GTVEffectFilterTypeManage effectFilterTypeManage;
    public void setEffectFilterSetting(GTVEffectFilterTypeManage effectFilterTypeManage) {
        this.effectFilterTypeManage = effectFilterTypeManage;
    }
}
