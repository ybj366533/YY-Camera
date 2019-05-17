package com.ybj366533.yycamera.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import com.gtv.cloud.editor.IGTVVideoEditor
import com.ybj366533.yycamera.R

//import com.aiyaapp.aiya.util.ClickUtils;

/**
 * Created by aiya on 2017/7/27.
 */

class VisualEffectRecyclerAdapter(private val context: Context) : RecyclerView.Adapter<VisualEffectRecyclerAdapter.ImageTextHolder>(), View.OnTouchListener {

    private var checkPos = 0
    private var mListener: VisualEffectRecyclerAdapter.OnVisualEffectCheckListener? = null
    private val isSelectItem = -1
    private val filterName = arrayOf("无", "黑白", "抖动", "灵魂出窍", "异世界", "尖锐", "波动", "透视", "闪烁", "素描", "灵异", "印象派")

    private val filters = arrayOf("", "heibai", "doudong", "linghunchuqiao", "yishijie", "jianrui", "bodong", "toushi", "shanshuo", "sumiao", "lingyi", "yingxiangpai")


    private val effectType = arrayOf<IGTVVideoEditor.EffectType>(IGTVVideoEditor.EffectType.EFFECT_NO, IGTVVideoEditor.EffectType.EFFECT_HEBAI, IGTVVideoEditor.EffectType.EFFECT_DOUDONG, IGTVVideoEditor.EffectType.EFFECT_LINGHUNCHUQIAO, IGTVVideoEditor.EffectType.EFFECT_YISHIJIE, IGTVVideoEditor.EffectType.EFFECT_JIANRUI, IGTVVideoEditor.EffectType.EFFECT_BODONG, IGTVVideoEditor.EffectType.EFFECT_TOUSHI, IGTVVideoEditor.EffectType.EFFECT_SHANGSHUO, IGTVVideoEditor.EffectType.EFFECT_SUMIAO, IGTVVideoEditor.EffectType.EFFECT_LINGYI, IGTVVideoEditor.EffectType.EFFECT_YINXIANPAI)

    var imageRes = intArrayOf(R.mipmap.effect_normal, R.mipmap.effect_heibai, R.mipmap.effect_doudong, R.mipmap.effect_linghunchuqiao, R.mipmap.effect_yishijie, R.mipmap.effect_jianrui, R.mipmap.effect_bodong, R.mipmap.effect_toushi, R.mipmap.effect_shanshuo, R.mipmap.effect_sumiao, R.mipmap.effect_lingyi, R.mipmap.effect_yingxiangpai)


    private var inEffect = false // 是否特效中flag，确保只回调一次

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ImageTextHolder {
        return ImageTextHolder(LayoutInflater.from(context).inflate(R.layout.item_effect, viewGroup, false), this)
    }

    override fun onBindViewHolder(imageHolder: ImageTextHolder, i: Int) {
        imageHolder.setData(filterName[i], i)
        if (isSelectItem == i) {
            imageHolder.setSelect(true)
        } else {
            imageHolder.setSelect(false)
        }
    }

    override fun getItemCount(): Int {
        return filters.size
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {

        checkPos = v.tag as Int
        when (event.action) {
            MotionEvent.ACTION_DOWN       // 开始特效
            ->
                //Log.e("test111","start");
                //v.setBackgroundColor(context.getResources().getColor(R.color.color2));


                if (inEffect == false) {
                    if (mListener != null) {
                        mListener!!.onVisualEffectStart(checkPos, effectType[checkPos])
                        inEffect = true
                    }
                }
            MotionEvent.ACTION_UP         // 结束特效
                ,

            MotionEvent.ACTION_CANCEL ->
                //v.setBackgroundColor(context.getResources().getColor(R.color.color_null));
                //Log.e("test111","stop");
                if (inEffect == true) {
                    if (mListener != null) {
                        mListener!!.onVisualEffectStop(checkPos, effectType[checkPos])
                        inEffect = false
                    }
                }

            else -> {
            }
        }//                if (checkPos != isSelectItem) {
        //                    isSelectItem = checkPos;
        //                    notifyDataSetChanged();
        //                }
        //notifyDataSetChanged();
        return true
    }

    fun setVisualEffectCheckListener(listener: VisualEffectRecyclerAdapter.OnVisualEffectCheckListener) {
        mListener = listener
    }

    //    public class MenuHolder extends RecyclerView.ViewHolder{
    //
    //        TextView tv;
    //
    //        public MenuHolder(View itemView, View.OnClickListener clickListener) {
    //            super(itemView);
    //            tv= (TextView)itemView.findViewById(R.id.mMenu);
    //            tv.setOnClickListener(clickListener);
    //            //tv.setOnTouchListener(VisualEffectRecyclerAdapter.this);
    //        }
    //
    //        public void setData(String name,int pos){
    //            tv.setText(name);
    //            tv.setSelected(pos==checkPos);
    //            //tv.setPressed(pos == checkPos);
    //            tv.setTag(pos);
    //        }
    //
    //        public void select(boolean isSelect){
    //            tv.setSelected(isSelect);
    //        }
    //    }


    inner class ImageTextHolder(itemView: View, onTouchListener: View.OnTouchListener) : RecyclerView.ViewHolder(itemView) {

        var imageView: ImageView
        var textView: TextView
        var mLayout: FrameLayout
        var isSelect = false

        init {
            imageView = itemView.findViewById(R.id.mImage) as ImageView
            textView = itemView.findViewById(R.id.mText) as TextView
            mLayout = itemView.findViewById(R.id.ll_mImage) as FrameLayout
            itemView.setOnTouchListener(onTouchListener)
        }

        fun setData(name: String, pos: Int) {
            imageView.setImageResource(imageRes[pos])
            textView.text = name
            imageView.isSelected = pos == checkPos
            //tv.setPressed(pos == checkPos);
            imageView.tag = pos
            this.itemView.tag = pos
        }

        fun setSelect(isSelect: Boolean) {
            this.isSelect = isSelect
            if (isSelect) {
                mLayout.setBackgroundColor(context.resources.getColor(R.color.color2))
            } else {
                mLayout.setBackgroundColor(context.resources.getColor(R.color.color_null))
            }

        }
    }

    interface OnVisualEffectCheckListener {
        fun onVisualEffectStart(pos: Int, type: IGTVVideoEditor.EffectType): Boolean

        fun onVisualEffectStop(pos: Int, type: IGTVVideoEditor.EffectType): Boolean
    }
}
