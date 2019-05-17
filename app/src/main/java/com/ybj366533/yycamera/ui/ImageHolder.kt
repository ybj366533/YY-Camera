package com.ybj366533.yycamera.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.ybj366533.yycamera.R

/**
 * Created by aiya on 2017/7/27.
 */

class ImageHolder(itemView: View, clickListener: View.OnClickListener) : RecyclerView.ViewHolder(itemView) {

    var effect: ImageView = itemView.findViewById(R.id.mImage) as ImageView
    var textView: TextView

    init {
        effect.setOnClickListener(clickListener)
        textView = itemView.findViewById(R.id.mText) as TextView
    }

}
