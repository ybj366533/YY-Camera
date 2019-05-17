package com.ybj366533.yycamera.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ybj366533.yycamera.R


class RecordSettingRecyclerAdapter(//    private ArrayList<MenuBean> mMenuDatas;
        private val context: Context)//        mMenuDatas = new ArrayList<>();
//        initMenuData("modelsticker/stickers.json");
//        for (int i = 0; i < effectIcons.length && i < mMenuDatas.size(); i++) {
//            mMenuDatas.get(i).icon = effectIcons[i];
//        }
    : RecyclerView.Adapter<RecordSettingRecyclerAdapter.ImageTextHolder>(), View.OnClickListener {

    //    public int[] effectIcons = new int[]{
    //            R.drawable.if_normal,       // 原图
    //            R.drawable.if_amaro, R.drawable.if_rise, R.drawable.if_hudson, R.drawable.if_xproii, R.drawable.if_sierra,
    //            R.drawable.if_lomofi, R.drawable.if_earlybird, R.drawable.if_sutro, R.drawable.if_toaster, R.drawable.if_brannan,
    //            R.drawable.if_inkwell, R.drawable.if_walden, R.drawable.if_hefe, R.drawable.if_valencia, R.drawable.if_nashville,
    //            R.drawable.if_1977, R.drawable.if_lordkelvin
    //    };

    //    public int[] imageRes = new int[] {R.mipmap.record_sticker, R.mipmap.record_filter, R.mipmap.record_beauty, R.mipmap.record_speed, R.mipmap.record_countdown};
    //    public int[] testStr= new int[] {R.string.record_sticker, R.string.record_filter, R.string.record_beauty, R.string.record_speed,R.string.record_countdown};
    //    public String[] settingName = new String[] {"sticker", "filter", "beauty", "speed", "countdown"};

    var imageRes = intArrayOf(R.mipmap.record_sticker, R.mipmap.record_filter, R.mipmap.record_beauty, R.mipmap.record_speed, R.mipmap.record_countdown)
    var testStr = intArrayOf(R.string.record_sticker, R.string.record_filter, R.string.record_beauty, R.string.record_speed, R.string.record_countdown)
    var settingName = arrayOf("sticker", "filter", "beauty", "speed", "countdown")


    private var listener: OnSettingItemCheckListener? = null

    private var selectPos = 0

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ImageTextHolder {
        return ImageTextHolder(LayoutInflater.from(context).inflate(R.layout.item_image_text, viewGroup, false), this)
    }

    override fun onBindViewHolder(imgTextHolder: ImageTextHolder, i: Int) {
        imgTextHolder.imageView.setImageResource(imageRes[i])
        imgTextHolder.itemView.tag = i

        imgTextHolder.textView.setText(testStr[i])
        //imgTextHolder.textView.setTag(i);
        //        effectHolder.effect.setImageResource(effectIcons[i]);
        //        effectHolder.effect.setTag(i);
        //        //effectHolder.effect.setSelected(selectPos == i && selectPos != 0);
        //        effectHolder.effect.setSelected(selectPos == i);
    }


    override fun getItemCount(): Int {
        return imageRes.size
    }

    fun setSettingItemCheckListener(listener: OnSettingItemCheckListener) {
        this.listener = listener
    }

    override fun onClick(v: View) {

        selectPos = v.tag as Int
        if (listener != null) {
            listener!!.onSettingItemChecked(selectPos, settingName[selectPos])
        }

        notifyDataSetChanged()
    }

    interface OnSettingItemCheckListener {
        fun onSettingItemChecked(pos: Int, settingName: String): Boolean
    }


    inner class ImageTextHolder(itemView: View, clickListener: View.OnClickListener) : RecyclerView.ViewHolder(itemView) {

        var imageView: ImageView
        var textView: TextView

        init {
            imageView = itemView.findViewById(R.id.mImage) as ImageView
            textView = itemView.findViewById(R.id.mText) as TextView
            itemView.setOnClickListener(clickListener)
        }

    }
}
