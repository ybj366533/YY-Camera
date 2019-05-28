package com.ybj366533.yycamera.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ybj366533.yycamera.R

import java.io.File


/**
 * Created by aiya on 2017/7/27.
 */

class StickerRecylerAdapter(private val context: Context) : RecyclerView.Adapter<StickerRecylerAdapter.ImageTextHolder>(), View.OnClickListener {

    var effectIcons = intArrayOf(R.mipmap.no_eff, R.mipmap.bunny, R.mipmap.rabbiteating)

    var testStr = arrayOf("无", "兔女郎", "萌兔")

    var stickerName = arrayOf("no", "bunny", "rabbiteating")

    private var listener: OnStickerCheckListener? = null

    private var selectPos = 0

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ImageTextHolder {
        return ImageTextHolder(LayoutInflater.from(context).inflate(R.layout.item_sticker, viewGroup, false), this)
    }

    override fun onBindViewHolder(effectHolder: ImageTextHolder, i: Int) {
        //effectHolder.effect.setImageResource(mMenuDatas.get(i).icon);
        effectHolder.effect.setImageResource(effectIcons[i])
        effectHolder.textView.text = testStr[i]
        effectHolder.effect.tag = i
        //effectHolder.effect.setSelected(selectPos == i && selectPos != 0);
        effectHolder.effect.isSelected = selectPos == i
    }


    override fun getItemCount(): Int {
        //return mMenuDatas.size();
        return effectIcons.size
    }

    fun setEffectCheckListener(listener: OnStickerCheckListener) {
        this.listener = listener
    }

    override fun onClick(v: View) {
        //        if(listener!=null&&(int)v.getTag()==effectIcons.length&&listener.onStickerChecked(-1,"")){
        //            return;
        //        }
        //        YYStickerMusicPlayer.getInstance().close();
        selectPos = v.tag as Int

        if (listener != null) {
            var path: String? = null
            val dataDir = this.context.getExternalFilesDir(null)
            if (dataDir != null) {
                if (selectPos == 0) {

                } else {
                    path = dataDir.absolutePath + File.separator + "modelsticker" + File.separator + stickerName[selectPos]
                }
            }
            listener!!.onStickerChecked(selectPos, path)
        }
        notifyDataSetChanged()
    }

    interface OnStickerCheckListener {
        fun onStickerChecked(pos: Int, path: String?): Boolean
    }

    inner class ImageTextHolder(itemView: View, clickListener: View.OnClickListener) : RecyclerView.ViewHolder(itemView) {

        var effect: ImageView
        var textView: TextView

        init {
            effect = itemView.findViewById(R.id.mImage) as ImageView
            textView = itemView.findViewById(R.id.mText) as TextView
            effect.setOnClickListener(clickListener)
        }

    }
}
