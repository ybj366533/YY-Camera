package com.ybj366533.yycamera.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ybj366533.yycamera.R

//import com.aiyaapp.aiya.util.ClickUtils;

/**
 * Created by aiya on 2017/7/27.
 */

class SpeedRecyclerAdapter(private val context: Context) : RecyclerView.Adapter<SpeedRecyclerAdapter.MenuHolder>(), View.OnClickListener {

    private var checkPos = 1   //默认标准
    private var mListener: OnSpeedCheckListener? = null

    private val filterName = arrayOf("慢", "普通", "快")

    private val filters = arrayOf("slow", "standard", "fast")

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MenuHolder {
        return MenuHolder(LayoutInflater.from(context).inflate(R.layout.item_speed_menu, viewGroup, false), this)
    }

    override fun onBindViewHolder(imageHolder: MenuHolder, i: Int) {
        imageHolder.setData(filterName[i], i)
    }

    override fun getItemCount(): Int {
        return filters.size
    }

    override fun onClick(v: View) {
        //        checkPos = ClickUtils.getPos(v);
        //        if(mListener!=null){
        //            mListener.onSelect(checkPos,filters[checkPos]);
        //        }

        checkPos = v.tag as Int
        //Log.e("testtest2", "---------------- " + checkPos + filters[checkPos]);
        if (mListener != null) {
            mListener!!.onSpeedChecked(checkPos, filters[checkPos])
        }
        notifyDataSetChanged()
    }

    fun setSpeedCheckListener(listener: OnSpeedCheckListener) {
        mListener = listener
    }

    inner class MenuHolder(itemView: View, clickListener: View.OnClickListener) : RecyclerView.ViewHolder(itemView) {

        internal var tv: TextView

        init {
            tv = itemView.findViewById(R.id.mMenu) as TextView
            tv.setOnClickListener(clickListener)
            //ClickUtils.addClickTo(tv, LookupAdapter.this,R.id.mMenu);
        }

        fun setData(name: String, pos: Int) {
            tv.text = name
            tv.isSelected = pos == checkPos
            tv.tag = pos
        }

        fun select(isSelect: Boolean) {
            tv.isSelected = isSelect
        }
    }

    interface OnSpeedCheckListener {
        fun onSpeedChecked(pos: Int, music: String): Boolean
    }
}
