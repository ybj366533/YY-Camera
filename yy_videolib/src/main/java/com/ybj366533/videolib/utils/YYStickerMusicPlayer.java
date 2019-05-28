package com.ybj366533.videolib.utils;

import android.media.MediaPlayer;

/**
 * Created by YY on 2018/5/22.
 */

public class YYStickerMusicPlayer extends Object {

    private String currentMusicPath = null;
    private MediaPlayer currentplayer = null;

    private static YYStickerMusicPlayer _instance = null;

    public static YYStickerMusicPlayer getInstance() {

        if( _instance == null ) {

            synchronized (YYStickerMusicPlayer.class) {

                if( _instance == null ) {
                    _instance = new YYStickerMusicPlayer();
                }
            }
        }

        return _instance;
    }

    public void open(String p) {
        synchronized (YYStickerMusicPlayer.class) {

            if( p == currentMusicPath )
                return;

            currentMusicPath = p;
            
            if(this.currentplayer == null) {
                this.currentplayer = new MediaPlayer();
            }

            try {
                this.currentplayer.setDataSource(p);
                this.currentplayer.setLooping(true);
                this.currentplayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void play() {
        synchronized (YYStickerMusicPlayer.class) {
            try {
                if( this.currentplayer != null && this.currentplayer.isPlaying() == false )
                    this.currentplayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void pause() {
        synchronized (YYStickerMusicPlayer.class) {
            try {
                if( this.currentplayer != null && this.currentplayer.isPlaying() == true )
                    this.currentplayer.pause();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void close() {
        synchronized (YYStickerMusicPlayer.class) {
            currentMusicPath = null;

            try {
                if(this.currentplayer != null) {
                    this.currentplayer.stop();
                }

                this.currentplayer = null;
                currentMusicPath = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
