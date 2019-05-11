package com.ybj366533.yycamera.ui

import android.support.annotation.IdRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import android.widget.RadioGroup
import android.widget.ViewAnimator

import com.gtv.cloud.gtvideo.GTVVideoRecordSTActivity
import com.gtv.cloud.gtvideo.R


/**
 * Created by gtv on 2017/7/27.
 */

class RecordSettingControllerST(act: GTVVideoRecordSTActivity, val contentView: View) : SelectListener {

    private val mSpeedList: RecyclerView

    private var mViewAnim: ViewAnimator? = null
    private val mSelectGroup: RadioGroup

    private var speedRecyclerAdapter: SpeedRecyclerAdapter? = null

    private val selectKey = SparseIntArray()

    init {

        selectKey.append(0, R.id.select_group_0)

        mSelectGroup = `$`(R.id.select_group)
        mViewAnim = `$`(R.id.mSelectAnim)


        mSpeedList = `$`(R.id.mSpeedList)
        mSpeedList.layoutManager = LinearLayoutManager(act.getApplicationContext(), LinearLayoutManager.HORIZONTAL, false)
        mSpeedList.setAdapter(speedRecyclerAdapter = SpeedRecyclerAdapter(act))

        mSelectGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.e("doggycoder", "choose checkid:$checkedId")
            mViewAnim!!.displayedChild = selectKey.indexOfValue(checkedId)
        }

    }

    fun <T> `$`(id: Int): T {
        return contentView.findViewById<View>(id) as T
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


    fun setSpeedCheckListener(listener: SpeedRecyclerAdapter.OnSpeedCheckListener) {
        speedRecyclerAdapter!!.setSpeedCheckListener(listener)
    }


    fun setDisplayItem(settingName: String) {
        if (settingName.equals("speed", ignoreCase = true)) {
            mSelectGroup.check(R.id.select_group_0)
        }
    }
}
