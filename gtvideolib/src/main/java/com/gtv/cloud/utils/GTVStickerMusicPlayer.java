package com.gtv.cloud.utils;

import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by gtv on 2018/5/22.
 */

public class GTVStickerMusicPlayer extends Object {

    private String currentMusicPath = null;
    private MediaPlayer currentplayer = null;

    private static GTVStickerMusicPlayer _instance = null;

    public static GTVStickerMusicPlayer getInstance() {

        if( _instance == null ) {

            synchronized (GTVStickerMusicPlayer.class) {

                if( _instance == null ) {
                    _instance = new GTVStickerMusicPlayer();
                }
            }
        }

        return _instance;
    }

    public void open(String p) {
        synchronized (GTVStickerMusicPlayer.class) {

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
        synchronized (GTVStickerMusicPlayer.class) {
            try {
                if( this.currentplayer != null && this.currentplayer.isPlaying() == false )
                    this.currentplayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void pause() {
        synchronized (GTVStickerMusicPlayer.class) {
            try {
                if( this.currentplayer != null && this.currentplayer.isPlaying() == true )
                    this.currentplayer.pause();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void close() {
        synchronized (GTVStickerMusicPlayer.class) {
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
