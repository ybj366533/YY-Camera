package com.ybj366533.videolib.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.ybj366533.videolib.core.IPCMAudioCallback;
import com.ybj366533.videolib.videoplayer.VideoPlayer;

import static com.ybj366533.videolib.videoplayer.VideoPlayer.YY_PLAYER_STREAM_EOF;

//import tv.danmaku.ijk.media.player.IMediaPlayer;
//import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by YY on 2017/9/3.
 */

public class YYMusicHandler {

    VideoPlayer player;

    //private AudioTrack audioTrack;

    private IPCMAudioCallback callback;

    private static YYMusicHandler _instance = null;

    private boolean pause_flag = false;

    private int seekPosMili = 0;        // 可能在prepare之前调用seek，暂存

    public static YYMusicHandler getInstance() {

        if( _instance == null ) {
            synchronized (YYMusicHandler.class) {
                if( _instance == null ) {
                    _instance = new YYMusicHandler();
                }
            }
        }

        return _instance;
    }

    public YYMusicHandler() {

    }

    public void playMusic(String path, IPCMAudioCallback c){
        this.callback = c;
        playMusic(path);
    }

    public void playMusic(String path) {
        seekPosMili = 0;

        if(player != null) {
            stopPlay();
        }

        player  = new VideoPlayer(path);
        pause_flag = false;

        player.setSurface(null);
        player.setLogLevel(0);
        player.setPlayerLogOutput(new VideoPlayer.IYYVideoPlayerLogOutput() {
            @Override
            public void onLogOutput(String logMsg) {
                Log.e("YY", logMsg);
            }
        });

        player.setPlayerEventLisenter(new VideoPlayer.IYYVideoPlayerListener() {
            @Override
            public void onPrepared(VideoPlayer p) {
                
                //------------------------------------------
                Log.e("app", "prepared");

//                int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
//                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
//
//                audioTrack.play();

                //p.seekTo(10000);
                if (seekPosMili != 0) {
                    p.seekTo(seekPosMili);
                    p.setRange(seekPosMili,p.getDuration() + 1000);
                    seekPosMili = 0;
                }

                // 设置一个定时器，拉取声音
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        loopAudio(player);
                    }
                };
                Thread t = new Thread(r);
                running_flag = true;
                //pause_flag = false;       //0424 音乐开始马上停止，会停不掉，移到开始位置
                t.start();
            }

            @Override
            public void onCompletion(VideoPlayer p, int errorCode) {
                Log.e("app", "completion");
            }
        });
        player.play();

    }

    public VideoPlayer getCurrentPLayer(){
        return player;
    }

    protected boolean running_flag = false;
    protected byte[] audio_buffer = new byte[4096];
    protected void loopAudio(VideoPlayer playingObj) {

        int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        audioTrack.play();

        while(running_flag == true && playingObj == getCurrentPLayer() ) {
            if (pause_flag == true) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                int len = playingObj.pullAudioData(audio_buffer);
                if (len > 0) {
                    if(callback != null) {
                        callback.onRawAudioData(audio_buffer, 0, len);
                    }
                    audioTrack.write(audio_buffer, 0, len);     //todo sleep when no data, and play dummy data?
                } else {
                    if (player.checkStreamStatus() == YY_PLAYER_STREAM_EOF) {
                        //循环播放
                        player.seekTo(0);
                    }
                }

            }

        }

        if( audioTrack != null ) {
            try{
            audioTrack.stop();
            audioTrack.release();
            }catch (Exception e ) {
                e.printStackTrace();
            }
            audioTrack = null;
        }

        playingObj.close();
        playingObj = null;

        return;
    }

    public void stopPlay() {


        running_flag = false;       //todo 互斥
        player = null;

        // while循环如果还没结束还要使用player，这边就不是释放，等下次播放的时候在释放。
//        if(player != null) {
//            player.close();
//            player = null;
//        }

        //this.callback = null;
     }

     public void pausePlay() {

         pause_flag = true;
         this.callback = null;

//        if(player != null) {
//            player.pauseVideo();
//        }
     }

     public boolean resumePlay() {
        return resumePlay(null);
     }

     public boolean resumePlay(IPCMAudioCallback c) {
//        if (player != null) {
//            player.resumeVideo();
//        }
         this.callback = c;

         if (player != null) {
             pause_flag = false;
             return true;
         }
         return false;
     }

    public void seekTo(int msec) {
        if (player != null) {
            seekPosMili = msec;
            player.seekTo(msec);
        }

        return;
    }

    public int getDuration() {
        if (player != null) {
            return player.getDuration();
        }

        return 0;
    }

}
