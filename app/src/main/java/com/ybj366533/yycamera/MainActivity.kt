package com.ybj366533.yycamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.ybj366533.yycamera.utils.FileUtils
import com.ybj366533.yycamera.utils.ToolUtils

import java.io.File
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // TODO 会循环请求？
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED -> requestForPermissions()
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED -> requestForPermissions()
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED -> requestForPermissions()
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED -> requestForPermissions()
        }

        return
    }

    private fun requestForPermissions() {

        if (Build.VERSION.SDK_INT >= 23) {

            var requestCode = 0
            val permissionList = ArrayList<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO)
                requestCode = requestCode or MY_RECORD_AUDIO_REQUEST_CODE
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA)
                requestCode = requestCode or MY_CAMERA_REQUEST_CODE
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestCode = requestCode or MY_WRITE_EXTERNAL_REQUEST_CODE
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_PHONE_STATE)
                requestCode = requestCode or MY_READ_PHONE_STATE_REQUEST_CODE
            }

            if (requestCode > 0) {

                ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), requestCode)
            }
        } else {

            if (checkAudioPermission(this) == false) {
                Toast.makeText(applicationContext, "没有麦克风权限", Toast.LENGTH_SHORT).show()
            }

            if (checkCameraPermission(this) == false) {
                Toast.makeText(applicationContext, "没有摄像头权限", Toast.LENGTH_SHORT).show()
            }
        }
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestForPermissions()


        val dest = Environment.getExternalStorageDirectory().toString() + "/gtvdemo"
        val file = File(dest)
        if (!file.exists()) {
            file.mkdir()
        }
        val s = System.currentTimeMillis()
        AssetsHandler.instance.copyFilesFassets(this, "data", dest, ".mp3")
        AssetsHandler.instance.copyFilesFassets(this, "data", dest, ".mp4")
        Log.e("GTV", "copyFilesFassets cost " + (System.currentTimeMillis() - s))


        val videoCamBtn = findViewById<View>(R.id.main_btn_record) as ImageView
        videoCamBtn.setOnClickListener(
                View.OnClickListener {
                    val intent = Intent(this@MainActivity, GTVVideoRecordStreamActivity::class.java)
                    val recWorkFolder = ToolUtils.getExternalFilesPath(this@MainActivity) + "/" + Constants.REC_WORK_FOLDER
                    intent.putExtra(Constants.REC_WORK_FOLDER, recWorkFolder)
                    startActivity(intent)
                    return@OnClickListener
                })

        val draft = findViewById<View>(R.id.main_draft) as TextView
        draft.setOnClickListener(
                View.OnClickListener {
                    val intent = Intent(this@MainActivity, DraftListActivity::class.java)

                    startActivity(intent)
                    return@OnClickListener
                })
        //videoEditBtn.setVisibility(View.INVISIBLE);

        //final Button videoCropBtn = (Button)findViewById(R.id.video_crop_btn);
        val videoPlayBtn = findViewById<View>(R.id.main_play_btn) as ImageView
        videoPlayBtn.setOnClickListener {
            /*Intent intent = new Intent(MainActivity.this, GTVPlayerDemoActivity.class);
                        startActivity(intent);
                        return;*/
            /*  Intent intent = new Intent(Intent.ACTION_PICK);
                        intent .setType("video*//*");
                        startActivityForResult(intent, VIDEO_REQUEST_CODE);*/
        }

        //videoPlayBtn.setVisibility(View.INVISIBLE);

        var path: String? = null
        val dataDir = applicationContext.getExternalFilesDir(null)
        if (dataDir != null) {
            path = dataDir.absolutePath + File.separator + "modelsticker"
        }
        if (path != null)
            AssetsHandler.instance.copyFilesFassets(applicationContext, "modelsticker", path, "*")

        // 导入视频API测试
        // 适用于导入第三方视频 或者 已经合成的视频 为 全I帧视频
        //        new Thread(new Runnable() {
        //            @Override
        //            public void run() {
        //                String from = "/storage/emulated/0/DCIM/mm.mp4";
        //                String to = "/storage/emulated/0/DCIM/crop.mp4";
        //                boolean success = GTVVideoUtils.importMovie(from, to, new GTVVideoUtils.OnProgressCallback() {
        //                    @Override
        //                    public int onProgress(int i) {
        //                        Log.e("onProgress--->", "onProgress = " + i);
        //                        return 0;
        //                    }
        //                });
        //            }
        //        }).start();
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            Log.e("MainActivity", "urixxx" + data.data!!)

            if (requestCode == VIDEO_REQUEST_CODE) {
                val intent = Intent(this, VideoCropActivity::class.java)
                intent.putExtra(KEY_PATH, FileUtils.getRealFilePath(this, data.data))
                startActivity(intent)
            }


        }
    }

    companion object {

        private val TAG = "GTVDEMO"

        val MY_RECORD_AUDIO_REQUEST_CODE = 0x0001
        val MY_CAMERA_REQUEST_CODE = 0x0002
        val MY_WRITE_EXTERNAL_REQUEST_CODE = 0x0004
        val MY_READ_PHONE_STATE_REQUEST_CODE = 0x0008
        private val VIDEO_REQUEST_CODE = 101
        val KEY_PATH = "video_path"

        fun checkCameraPermission(context: Context): Boolean {

            var canUse = true
            var mCamera: Camera? = null
            try {
                mCamera = Camera.open(0)
                mCamera!!.setDisplayOrientation(90)
            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
                canUse = false
            }

            if (canUse) {
                mCamera!!.release()
                mCamera = null
            }
            return canUse
        }

        /**
         * 判断是是否有录音权限
         */
        fun checkAudioPermission(context: Context): Boolean {

            // 音频获取源
            val audioSource = MediaRecorder.AudioSource.MIC
            // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
            val sampleRateInHz = 44100
            // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
            val channelConfig = AudioFormat.CHANNEL_IN_STEREO
            // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            var bufferSizeInBytes = 0
            //开始录制音频
            try {
                bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
                        channelConfig, audioFormat)
                var audioRecord: AudioRecord? = AudioRecord(audioSource, sampleRateInHz,
                        channelConfig, audioFormat, bufferSizeInBytes)
                // 防止某些手机崩溃，例如联想
                audioRecord!!.startRecording()

                /**
                 * 根据开始录音判断是否有录音权限
                 */
                if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING && audioRecord.recordingState != AudioRecord.RECORDSTATE_STOPPED) {
                    Log.e(TAG, "audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING : " + audioRecord.recordingState)
                    return false
                }

                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                    //如果短时间内频繁检测，会造成audioRecord还未销毁完成，此时检测会返回RECORDSTATE_STOPPED状态，再去read，会读到0的size，所以此时默认权限通过
                    return true
                }

                val bytes = ByteArray(1024)
                val readSize = audioRecord.read(bytes, 0, 1024)
                if (readSize == AudioRecord.ERROR_INVALID_OPERATION || readSize <= 0) {
                    Log.e(TAG, "readSize illegal : $readSize")
                    return false
                }
                audioRecord.stop()
                audioRecord.release()
                audioRecord = null

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, Log.getStackTraceString(e))
                return false
            }

            return true
        }
    }

}
