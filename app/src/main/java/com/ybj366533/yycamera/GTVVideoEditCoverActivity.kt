package com.ybj366533.yycamera

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaScannerConnection
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.gtv.cloud.editor.EditCallback
import com.gtv.cloud.editor.GTVEditorCreator
import com.gtv.cloud.editor.GTVExtractFrameInfo
import com.gtv.cloud.editor.IGTVVideoEditor
import com.gtv.cloud.gtvideo.utils.FileUtils
import com.gtv.cloud.gtvideo.widget.SimpleEditCallback
import com.gtv.cloud.gtvideo.widget.ThumbnailView
import com.gtv.cloud.utils.GTVVideoUtils
import com.gtv.cloud.utils.LogUtils

import java.io.File
import java.util.ArrayList
import java.util.UUID

import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers

/**
 * Created by Summer on 18/3/29.
 * 裁剪生成 webp 封面
 */

class GTVVideoEditCoverActivity : AppCompatActivity(), View.OnClickListener {
    private val FRAME_PATH = "/storage/emulated/0/DCIM/GTVCoverframe"//抽帧存放路径

    private var glSurfaceView: GLSurfaceView? = null
    private var imageView: ImageView? = null

    //private SimpleDraweeView mWebpView;·
    //    private LinearLayout llWebpView;

    private var btn_commit: Button? = null
    private var btn_finish: Button? = null

    private var mDuration: Int = 0
    private var ll_thumbnail: LinearLayout? = null
    private var thumbnailView: ThumbnailView? = null
    private var mCursor = 0//游标
    internal var videoPath: String? = ""

    private var msc: MediaScannerConnection? = null

    private val filterStartTime = -1
    private var isFromDraft: Boolean = false//是否从草稿箱跳转
    private var draftFloder: String? = null

    private var mEditor: IGTVVideoEditor? = null
    private var editCallback: EditCallback? = null


    /**
     * 生成 文件保存路径
     *
     * @return
     */
    private//新建一个File，传入文件夹目录
    //判断文件夹是否存在，如果不存在就创建，否则不创建
    //通过file的mkdirs()方法创建<span style="color:#FF0000;">目录中包含却不存在</span>的文件夹
    val outPutURL: String
        get() {
            val file = File(FRAME_PATH)
            if (!file.exists()) {
                file.mkdirs()
            }
            return FRAME_PATH
        }

    /**
     * 获取抽帧图片地址
     *
     * @return
     */
    private val frameUrls: Observable<String>
        get() = Observable.create<ArrayList<GTVExtractFrameInfo>>(object : Observable.OnSubscribe<ArrayList<GTVExtractFrameInfo>> {
            internal var size = mDuration / 1000
            internal var frameInfoList = ArrayList<GTVExtractFrameInfo>()

            override fun call(subscriber: Subscriber<in ArrayList<GTVExtractFrameInfo>>) {

                GTVVideoUtils.extractVideoFrame(videoPath, IGTVVideoEditor.ImageFormat.IMAGE_JPEG, outPutURL, 0, mDuration, size, frameInfoList)

                if (frameInfoList.size == size) {
                    subscriber.onNext(frameInfoList)
                    subscriber.onCompleted()
                }
            }
        }).flatMap<GTVExtractFrameInfo> { lists -> Observable.from<GTVExtractFrameInfo>(lists) }.flatMap(Func1<Any, Observable<String>> { gtvExtractFrameInfo -> Observable.just(gtvExtractFrameInfo.getFilePath()) })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    override fun onCreate(savedInstanceState: Bundle?) {
        //去除title
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        //去掉Activity上面的状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_edit_cover)


        val params = intent
        videoPath = params.getStringExtra(Constants.VIDEO_FILE)
        isFromDraft = params.getBooleanExtra(Constants.KEY_IS_FROM_DRAFT, false)
        draftFloder = params.getStringExtra(Constants.EDIT_WORK_FOLDER)//草稿箱路径
        initView()
        initGTVSDK()
        Log.e("VIDEO_FILE", videoPath)

        //videoPath = videoPath.replace("gtv.mp4","final.mp4");

        if (videoPath == null) {
            //            videoPath = "/storage/emulated/0/DCIM/aaa.mp4";
            //showSelectFile();
            videoPath = "/storage/emulated/0/Android/data/com.gtv.cloud.gtvideo/files//" + Constants.REC_WORK_FOLDER + "/gtv.mp4"       //debug, 如果不是从录制界面过来
        }
        //        videoPath = "/storage/emulated/0/DCIM/aaa.mp4";
        msc = MediaScannerConnection(this, null)
        msc!!.connect()
        //        glSurfaceView.setVisibility(View.INVISIBLE);
    }

    override fun onResume() {
        super.onResume()
        mEditor!!.startPreview()
        mEditor!!.playStart()
    }

    override fun onPause() {
        super.onPause()
        mEditor!!.stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        mEditor!!.destroy()

        msc!!.disconnect()
    }


    override fun onClick(v: View) {
        val id = v.id

        if (id == R.id.btn_commit) {
            //commit 生成webp
            //TODO 生成webp
            getVideoCover()
        }


        if (id == R.id.btn_finish) {
            //测试 视频抽针 合成 webp
            //getVideoCover();
            setResult(RESULT_CODE)
            finish()
        }

    }


    /**
     * webp 显示方法Glide
     *
     * @param imageView
     * @param filePath
     */
    private fun showGlideWedp(imageView: ImageView, filePath: String) {
        val options = RequestOptions()
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
        Glide.with(this)
                .load(filePath)
                .apply(options).transition(DrawableTransitionOptions().crossFade(200))
                .into(imageView)
    }


    private fun scanFile(fileName: String) {
        msc!!.scanFile(fileName, "video/mp4")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //获取图片路径
        if (resultCode == Activity.RESULT_OK && data != null) {


            if (requestCode == VIDEO_REQUEST_CODE) {
                videoPath = FileUtils.getRealFilePath(this, data.data)
                mEditor!!.setVideoPath(videoPath)
                Log.e("videoPath=", videoPath)
            }
        }
    }


    private fun initView() {

        glSurfaceView = findViewById<View>(R.id.mixView) as GLSurfaceView
        imageView = findViewById<View>(R.id.mImage) as ImageView
        imageView!!.visibility = View.INVISIBLE
        //TODO WEBP 显示控件
        //mWebpView = (SimpleDraweeView) findViewById(R.id.iv_simpleDraweeView);
        //        llWebpView = (LinearLayout) findViewById(R.id.ll_simpleDraweeView);
        //        llWebpView.setVisibility(View.INVISIBLE);

        btn_commit = findViewById<View>(R.id.btn_commit) as Button
        btn_commit!!.setOnClickListener(this)

        btn_finish = findViewById<View>(R.id.btn_finish) as Button
        btn_finish!!.setOnClickListener(this)
        ll_thumbnail = findViewById<View>(R.id.ll_thumbnail) as LinearLayout
        thumbnailView = findViewById<View>(R.id.thumbnailView) as ThumbnailView

        //监听裁剪器滑动
        thumbnailView!!.setOnScrollBorderListener(object : ThumbnailView.OnScrollBorderListener() {
            fun OnScrollBorder(start: Float, end: Float) {

                // 游标变更

                val startTIme = start.toInt()
                val result = startTIme / thumbnailView!!.getWidth() as Float
                mCursor = (result * mDuration).toInt()
                Log.e(TAG, "OnScrollBorder: start=" + startTIme + "mCursor= " + mCursor + "@result * mDuration =" + result * mDuration)

            }

            fun onScrollStateChange() {
                //生成 wedp
                // getVideoCover();
                //TODO 预览
                Log.e(TAG, "onScrollStateChange")
                getVideoCoverPreview()
            }
        })

    }

    private fun initGTVSDK() {

        editCallback = object : SimpleEditCallback() {
            fun onPrepared() {
                LogUtils.DebugLog(TAG, "  " + mEditor!!.getDuration() + "    " + mEditor!!.getCurrentPosition())
                mEditor!!.startPreview()
                //mEditor.playPause();
                mDuration = mEditor!!.getDuration()
                mEditor!!.playStart()
                getThumbnailList()

            }

            fun onPlayEnd() {
                runOnUiThread {
                    Log.e("getDuration", "#" + mCursor + "#####" + mEditor!!.getDuration())
                    if (mCursor > 0) {
                        mEditor!!.seekTo(mCursor + 1)
                    } else {
                        mEditor!!.seekTo(0)
                    }
                    mEditor!!.playStart()
                    //如果需要连续播放
                }
            }
        }

        mEditor = GTVEditorCreator.getInstance()
        mEditor!!.init(glSurfaceView, videoPath, draftFloder, if (isFromDraft) true else false, editCallback)

    }

    /**
     * 截取预览
     */

    private fun getVideoCoverPreview() {
        //截取预览
        mEditor!!.playPause()
        mEditor!!.setVideoCropRange(mCursor + 1, mCursor + 3000)
        //        mEditor.startPreview();
        mEditor!!.playStart()
        //mEditor.playPause();
    }

    /**
     * 生成 webp
     */
    private fun getVideoCover() {
        // 视频帧抽取 以及 webp 测试
        val frameInfoList = ArrayList<GTVExtractFrameInfo>()
        val a1 = System.nanoTime() / 1000

        GTVVideoUtils.extractVideoFrame(videoPath, IGTVVideoEditor.ImageFormat.IMAGE_YUV, outPutURL, mCursor, mCursor + 3000, 8, frameInfoList)

        //mEditor.extractVideoFrame(IGTVVideoEditor.ImageFormat.IMAGE_YUV, getOutPutURL(), mCursor, mCursor + 3000, 8, frameInfoList);
        //long t1 = System.nanoTime() / 1000 - a1;
        val webpUrl = outPutURL + "/" + UUID.randomUUID().toString() + ".webp"
        val `is` = GTVVideoUtils.createImageWebp(frameInfoList, webpUrl)
        if (`is`)
            Log.e(TAG, "webpUrl $webpUrl")

        GTVVideoUtils.createImageWebp(frameInfoList, webpUrl)
        imageView!!.visibility = View.VISIBLE
        ll_thumbnail!!.visibility = View.GONE
        thumbnailView!!.setVisibility(View.GONE)
        showGlideWedp(imageView, webpUrl)
        //        showFrescoWedp(mWebpView, webpUrl);
    }


    /**
     * 生成缩略图
     */
    private fun getThumbnailList() {
        FileUtils.delAllFile(FRAME_PATH)
        frameUrls.subscribe(object : Subscriber<String>() {
            internal var size = mDuration / 1000
            internal var thumbnailWidth = ll_thumbnail!!.width / size

            override fun onCompleted() {}

            override fun onError(throwable: Throwable) {}

            override fun onNext(imgUrl: String) {

                val imageView = ImageView(this@GTVVideoEditCoverActivity)
                imageView.layoutParams = ViewGroup.LayoutParams(thumbnailWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                imageView.setBackgroundColor(Color.parseColor("#666666"))
                imageView.scaleType = ImageView.ScaleType.CENTER

                val ro = RequestOptions()
                ro.diskCacheStrategy(DiskCacheStrategy.NONE)
                Glide.with(this@GTVVideoEditCoverActivity)
                        .setDefaultRequestOptions(ro)
                        .load(imgUrl)
                        .into(imageView)
                ll_thumbnail!!.addView(imageView)
            }
        })
    }

    companion object {

        private val TAG = "EditorCoverActivity"
        private val RESULT_CODE = 300
        private val VIDEO_REQUEST_CODE = 103
    }

}
