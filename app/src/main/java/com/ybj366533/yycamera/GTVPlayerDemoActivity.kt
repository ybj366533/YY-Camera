package com.ybj366533.yycamera

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

import com.gtv.cloud.videoplayer.GTVideoPlayer

class GTVPlayerDemoActivity : AppCompatActivity() {

    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(MyView(this))
    }

    internal inner class MyView//private MyThread myThread;
    (context: Context) : SurfaceView(context), SurfaceHolder.Callback {
        private val holder: SurfaceHolder

        var player: GTVideoPlayer? = null

        protected var running_flag = false
        protected var audio_buffer = ByteArray(4096)

        init {
            // TODO Auto-generated constructor stub
            holder = this.getHolder()
            holder.addCallback(this)
            //myThread = new MyThread(holder);//创建一个绘图线程
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // TODO Auto-generated method stub
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            // TODO Auto-generated method stub
            //holder.getSurface();
            val path = Environment.getExternalStorageDirectory().toString() + "/gtvdemo" + "/01.mp4"
            player = GTVideoPlayer(path)

            player!!.setSurface(holder.surface)
            player!!.setLogLevel(0)
            player!!.setPlayerLogOutput(object : GTVideoPlayer.IGTVideoPlayerLogOutput {
                override fun onLogOutput(logMsg: String) {
                    Log.e("GTV", logMsg)
                }
            })

            player!!.setPlayerEventLisenter(object : GTVideoPlayer.IGTVideoPlayerListener {
                override fun onPrepared(p: GTVideoPlayer) {
                    //------------------------------------------
                    Log.e("app", "prepared")

                    val bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
                    audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM)

                    audioTrack!!.play()

                    // 设置一个定时器，拉取声音
                    val r = Runnable { loopAudio() }
                    val t = Thread(r)
                    running_flag = true
                    t.start()
                }

                override fun onCompletion(p: GTVideoPlayer, errorCode: Int) {
                    Log.e("app", "completion")

                    if (audioTrack != null) {
                        audioTrack!!.stop()
                        audioTrack!!.release()
                        audioTrack = null
                    }
                }
            })
            player!!.play()
        }

        protected fun loopAudio() {

            while (running_flag) {
                val len = player!!.pullAudioData(audio_buffer)
                if (len > 0)
                    audioTrack!!.write(audio_buffer, 0, len)
            }

            return
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {

            running_flag = false

            val p = player
            val r = Runnable { p!!.close() }

            val t = Thread(r)
            t.start()

            player = null
        }
    }
}
