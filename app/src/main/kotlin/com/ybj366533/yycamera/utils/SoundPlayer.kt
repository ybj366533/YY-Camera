package com.ybj366533.yycamera.utils

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * 音频播放
 * Created by Summer on 2017/11/6.
 */
class SoundPlayer {

    companion object {
        val handler by lazy { Handler(Looper.getMainLooper()) }
    }

    enum class State(val state: Int) {
        ERROR(-1), IDLE(0), INITIALIZED(1), PREPARED(2), STARTED(3), PLAYING(4), PAUSED(5), COMPLETED(6), STOPPED(7),
    }

    var mediaPlayer: MediaPlayer? = null

    private var _listener = object : MediaPlayerListener {
        override fun onPrepared(mp: MediaPlayer?) {
            isPrepared = true
            //currentState = State.PREPARED
            if (autoStart && currentState.state != State.STARTED.state) {
                start()
            }
            listener?.onPrepared(this@SoundPlayer)
        }

        override fun onCompletion(mp: MediaPlayer?) {
            currentState = State.COMPLETED
//            if (updateEnable)
//                handler.post { updateListener?.onUpdate(this@SoundPlayer, duration, currentPosition) }
            listener?.onCompletion(this@SoundPlayer)
        }

        //http://www.cnblogs.com/getherBlog/p/3939033.html
        override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean = run {
            currentState = State.ERROR
            listener?.onError(this@SoundPlayer, what, extra)
            false
        }
    }

    private val _update = object : Runnable {
        override fun run() {
            if (currentState.state >= State.STARTED.state &&
                    currentState.state < State.PAUSED.state &&
                    isPrepared) {
                updateListener?.onUpdate(this@SoundPlayer, duration, currentPosition)
                handler.postDelayed(this,
                        updateDelayed - (currentPosition % updateDelayed))
            }
        }
    }

    var isPrepared = false //解析音频
    var currentState = State.IDLE //播放状态
    var playerUrl = "" //当前播放地址
    var autoStart = false //自动开始
    var updateEnable: Boolean = false //启用进度监听
    var updateDelayed = 1000L //进度监听间隔

    var listener: Listener? = null //状态监听
    var updateListener: UpdateListener? = null //进度监听

    fun load(url: String) = apply {

        if (!playerUrl.equals(url)) {
            reset()
            playerUrl = url
            mediaPlayer?.apply {
                setDataSource(url)
                currentState = State.INITIALIZED
                prepareAsync()
            }
        } else {
            _listener.onPrepared(mediaPlayer)
        }
    }

    /**
     * 重置播放器缓存
     */
    fun reset() = apply {
        mediaPlayer?.apply {
            reset()
        } ?: run {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.apply {
                setOnPreparedListener(_listener)
                setOnCompletionListener(_listener)
                setOnErrorListener(_listener)
            }
        }
        playerUrl = ""
        isPrepared = false
        currentState = State.IDLE
    }

    fun start() = apply {
        currentState = State.STARTED
        mediaPlayer?.start()
        listener?.onStart(this)
        if (updateEnable)
            handler.post(_update)
    }

    fun pause() = apply {
        currentState = State.PAUSED
        mediaPlayer?.pause()
        listener?.onPause(this)
    }

    /**
     * 停止播放，停止播放后必须重新prepare
     */
    fun stop() = apply {
        playerUrl = ""
        currentState = State.STOPPED
        mediaPlayer?.stop()
        listener?.onStop(this)
    }

    fun release() = apply {
        reset()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun seekTo(msec: Int) = apply {
        mediaPlayer?.apply {
            seekTo(msec)
            if (updateEnable)
                handler.post { updateListener?.onUpdate(this@SoundPlayer, duration, currentPosition) }
        }
    }

    val isPlaying get() = mediaPlayer?.run { isPlaying } ?: false

    val duration get() = mediaPlayer?.run { if (isPrepared) duration else 0 } ?: 0

    val currentPosition get() = mediaPlayer?.run { if (isPrepared) currentPosition else 0 } ?: 0

    fun autoStart(autoStart: Boolean) = apply {
        this.autoStart = autoStart
    }

    /**
     * 状态监听
     */
    fun listener(listener: Listener) = apply {
        this.listener = listener
    }

    /**
     * 进度监听
     */
    fun updateListener(updateListener: UpdateListener) = apply {
        this.updateListener = updateListener
    }

    fun updateEnable(enable: Boolean) = apply {
        updateEnable = enable
    }

    interface Listener {
        fun onPrepared(player: SoundPlayer)

        fun onCompletion(player: SoundPlayer)

        fun onError(player: SoundPlayer, what: Int, extra: Int): Boolean

        fun onStart(player: SoundPlayer)

        fun onPause(player: SoundPlayer)

        fun onStop(player: SoundPlayer)
    }

    interface UpdateListener {
        fun onUpdate(player: SoundPlayer, duration: Int, currentPosition: Int)
    }

    interface MediaPlayerListener : MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
}