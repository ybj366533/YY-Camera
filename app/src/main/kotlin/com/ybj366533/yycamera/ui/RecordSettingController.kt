package com.ybj366533.yycamera.ui

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import android.widget.RadioGroup
import android.widget.ViewAnimator

import com.xw.repo.BubbleSeekBar
import com.ybj366533.yycamera.view.RecordActivity
import com.ybj366533.yycamera.R


/**
 * Created by aiya on 2017/7/27.
 */

class RecordSettingController(act: RecordActivity, private val contentView: View, listener: StickerRecylerAdapter.OnStickerCheckListener, filterListener: FilterRecyclerAdapter.OnFilterCheckListener) : SelectListener {
    private val mEffectList: RecyclerView
    private val mFilterList: RecyclerView

    private val mSpeedList: RecyclerView
    //private RecyclerView mMusicList;

    private val mBeautyList: RecyclerView? = null
    private var mEffAdapter: StickerRecylerAdapter? = null
    //private LookupAdapter mLooAdapter;
    private var mViewAnim: ViewAnimator? = null
    private val mSelectGroup: RadioGroup
    //private BubbleSeekBar mSeekBarFilter;
    //private BubbleSeekBar mSeekBarBeauty;
    private val mSeekBarDayan: BubbleSeekBar
    private val mSeekBarShoulian: BubbleSeekBar

    private val mSeekBarMeibai: BubbleSeekBar
    private val mSeekBarMopi: BubbleSeekBar
    private val mSeekBarHongrun: BubbleSeekBar? = null

    //    private AiyaController mController;
    //    private CameraView mCameraView;

    private var filterRecyclerAdapter: FilterRecyclerAdapter? = null
    //private MusicRecyclerAdapter musicRecyclerAdapter;
    private var speedRecyclerAdapter: SpeedRecyclerAdapter? = null
    //private SdkBeauty mMeibaiFilter;
    //private SdkBeauty mMopiFilter;
    //private SdkBeauty mHongrunFilter;

    private val selectKey = SparseIntArray()

    init {

        selectKey.append(0, R.id.select_group_0)
        selectKey.append(1, R.id.select_group_1)
        selectKey.append(2, R.id.select_group_2)
        selectKey.append(3, R.id.select_group_3)
        // selectKey.append(3,R.id.select_group_3);
        //selectKey.append(4,R.id.select_group_4);

        mSelectGroup = `$`(R.id.select_group)
        mViewAnim = `$`<ViewAnimator>(R.id.mSelectAnim)

        mEffectList = `$`(R.id.mEffectList)
        mEffectList.layoutManager = GridLayoutManager(act.applicationContext, 5)
        mEffAdapter = StickerRecylerAdapter(act)
        mEffectList.adapter = mEffAdapter
        mEffAdapter!!.setEffectCheckListener(listener)

        // tod $ ?
        mFilterList = `$`(R.id.mFilterList)
        mFilterList.layoutManager = GridLayoutManager(act.applicationContext, 4)
        filterRecyclerAdapter= FilterRecyclerAdapter(act)
        mFilterList.adapter = filterRecyclerAdapter
        filterRecyclerAdapter!!.setFilterCheckListener(filterListener)


        mSpeedList = `$`(R.id.mSpeedList)
        mSpeedList.layoutManager = LinearLayoutManager(act.applicationContext, LinearLayoutManager.HORIZONTAL, false)
        speedRecyclerAdapter = SpeedRecyclerAdapter(act)
        mSpeedList.adapter = speedRecyclerAdapter


        //mSeekBarFilter= $(R.id.mSeekBarFilter);
        //mSeekBarBeauty= $(R.id.mSeekBarMeibai);
        mSeekBarDayan = `$`(R.id.mSeekBarDayan)
        mSeekBarShoulian = `$`(R.id.mSeekBarShoulian)
        mSeekBarMeibai = `$`(R.id.mSeekBarMeibai)
        mSeekBarMopi = `$`(R.id.mSeekBarMopi)
        //mSeekBarHongrun=$(R.id.mSeekBarHongrun);

        mSelectGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.e("doggycoder", "choose checkid:$checkedId")
            mViewAnim!!.displayedChild = selectKey.indexOfValue(checkedId)
        }
        //        //滤镜程度控制
        //        mSeekBarFilter.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
        //            @Override
        //            public void onProgressChanged(int progress, float progressFloat) {
        //                //mLookupFilter.setIntensity(progress/100f);
        //            }
        //
        //            @Override
        //            public void getProgressOnActionUp(int progress, float progressFloat) {
        //
        //            }
        //
        //            @Override
        //            public void getProgressOnFinally(int progress, float progressFloat) {
        //
        //            }
        //        });
        //美颜等级控制，整数0-6,
        //        mSeekBarBeauty.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener(){
        //
        //            @Override
        //            public void onProgressChanged(int progress, float progressFloat) {
        //                AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_TYPE,1);
        //                AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_LEVEL,progress);
        //            }
        //
        //            @Override
        //            public void getProgressOnActionUp(int progress, float progressFloat) {
        //
        //            }
        //
        //            @Override
        //            public void getProgressOnFinally(int progress, float progressFloat) {
        //
        //            }
        //        });
        //大眼程度控制，0-100
        mSeekBarDayan.onProgressChangedListener = object : BubbleSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(progress: Int, progressFloat: Float) {
                act.setBigEye(progress)
            }

            override fun getProgressOnActionUp(progress: Int, progressFloat: Float) {

            }

            override fun getProgressOnFinally(progress: Int, progressFloat: Float) {

            }
        }
        //瘦脸程度控制，0-100
        mSeekBarShoulian.onProgressChangedListener = object : BubbleSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(progress: Int, progressFloat: Float) {
                act.setThinFace(progress)
            }

            override fun getProgressOnActionUp(progress: Int, progressFloat: Float) {

            }

            override fun getProgressOnFinally(progress: Int, progressFloat: Float) {

            }
        }
        //美白程度控制，0-100
        mSeekBarMeibai.onProgressChangedListener = object : BubbleSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(progress: Int, progressFloat: Float) {
                //mMeibaiFilter.setLevel(progress);
                //                if (mCamera != null) {
                //                    mCamera.setBeautyParams(2, progress/100.0f);
                //                }
                act.setBeautyParams(2, progress / 100.0f)
            }

            override fun getProgressOnActionUp(progress: Int, progressFloat: Float) {

            }

            override fun getProgressOnFinally(progress: Int, progressFloat: Float) {

            }
        }
        //磨皮程度控制，0-6
        mSeekBarMopi.onProgressChangedListener = object : BubbleSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(progress: Int, progressFloat: Float) {
                //mMopiFilter.setLevel(progress);

                act.setBeautyParams(0, progress / 100.0f)
            }

            override fun getProgressOnActionUp(progress: Int, progressFloat: Float) {

            }

            override fun getProgressOnFinally(progress: Int, progressFloat: Float) {

            }
        }
        //        //红润程度控制，0-100
        //        mSeekBarHongrun.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
        //            @Override
        //            public void onProgressChanged(int progress, float progressFloat) {
        //                //mHongrunFilter.setLevel(progress);
        //            }
        //
        //            @Override
        //            public void getProgressOnActionUp(int progress, float progressFloat) {
        //
        //            }
        //
        //            @Override
        //            public void getProgressOnFinally(int progress, float progressFloat) {
        //
        //            }
        //        });
        filterInit()
    }

    fun <T> `$`(id: Int): T {
        return contentView.findViewById<View>(id) as T
    }

    private fun filterInit() {
        //        //滤镜
        //        mLookupFilter=new LookupFilter(getContentView().getResources());
        //        mLookupFilter.setIntensity(0.5f);
        //        //美白磨皮红润的滤镜
        //        mMeibaiFilter=new SdkBeauty(getContentView().getResources());
        //        mMeibaiFilter.setType(ISdkManager.BEAUTY_WHITEN);
        //        mMopiFilter=new SdkBeauty(getContentView().getResources());
        //        mMopiFilter.setType(ISdkManager.BEAUTY_SMOOTH);
        //        mHongrunFilter=new SdkBeauty(getContentView().getResources());
        //        mHongrunFilter.setType(ISdkManager.BEAUTY_SATURATE);

    }

    override fun onSelect(pos: Int, data: String) {
        //        if(pos==0){
        //            removeFilter(mLookupFilter);
        //        }else{
        //            removeFilter(mLookupFilter);
        //            mLookupFilter.setMaskImage("shader/lookup/"+data);
        //            addFilter(mLookupFilter);
        //        }
    }

    fun release() {
        mViewAnim = null
    }

    fun attachTo(obj: Any) {
        //        if(obj instanceof CameraView){
        //            mCameraView= (CameraView) obj;
        //            mController=null;
        //        }else if(obj instanceof AiyaController){
        //            mController= (AiyaController) obj;
        //            mCameraView=null;
        //        }
        //        addFilter(mMopiFilter);
        //        addFilter(mMeibaiFilter);
        //        addFilter(mHongrunFilter);
    }


    //    private void addFilter(AFilter filter){
    ////        if(mCameraView!=null){
    ////            mCameraView.addFilter(filter,true);
    ////        }
    ////        if(mController!=null){
    ////            mController.addFilter(filter,true);
    ////        }
    //    }

    //    private void removeFilter(AFilter filter){
    ////        if(mCameraView!=null){
    ////            mCameraView.removeFilter(filter);
    ////        }
    ////        if(mController!=null){
    ////            mController.removeFilter(filter);
    ////        }
    //    }

    fun setSpeedCheckListener(listener: SpeedRecyclerAdapter.OnSpeedCheckListener) {
        speedRecyclerAdapter!!.setSpeedCheckListener(listener)
    }

    //    public void setMusicCheckListener(MusicRecyclerAdapter.OnMusicCheckListener listener) {
    //        musicRecyclerAdapter.setMusicCheckListener(listener);
    //    }

    fun setDisplayItem(settingName: String) {
        if (settingName.equals("sticker", ignoreCase = true)) {
            mSelectGroup.check(R.id.select_group_0)
        } else if (settingName.equals("filter", ignoreCase = true)) {
            mSelectGroup.check(R.id.select_group_1)
        } else if (settingName.equals("beauty", ignoreCase = true)) {
            mSelectGroup.check(R.id.select_group_2)
        } else if (settingName.equals("speed", ignoreCase = true)) {
            mSelectGroup.check(R.id.select_group_3)
        }
    }
}
