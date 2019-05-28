package com.ybj366533.yycamera.ui

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ybj366533.yycamera.utils.Constants
import com.ybj366533.yycamera.R
import com.ybj366533.yycamera.view.RecordFinishActivity

import java.util.ArrayList

/**
 * Created by libq on 18/5/21.
 */

class DraftRecyclerAdapter(private val context: Context, private val datas: ArrayList<DraftItemInfo>?) : RecyclerView.Adapter<DraftRecyclerAdapter.ImageItemHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemHolder {
        val item = LayoutInflater.from(context).inflate(R.layout.item_draft_list, parent, false)
        return ImageItemHolder(item)
    }

    override fun onBindViewHolder(holder: DraftRecyclerAdapter.ImageItemHolder, position: Int) {
        if (datas == null)
            return

        val info = datas[position]
        holder.tv.text = info.url
        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecordFinishActivity::class.java)
            intent.putExtra(Constants.EDIT_WORK_FOLDER, info.url)
            intent.putExtra(Constants.KEY_IS_FROM_DRAFT, true)
            intent.putExtra(Constants.CURRENT_DRAFT_FLODER, info.floderName)//当前草稿文件夹名称
            context.startActivity(intent)
            Log.e("DRA", "#PATH-草稿箱：EDIT_WORK_FLODER =" + info.url + "  floder=" + info.floderName)
        }
    }

    override fun getItemCount(): Int {
        return datas?.size ?: 0
    }

    inner class ImageItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var iv: ImageView
        var tv: TextView

        init {
            iv = itemView.findViewById(R.id.iv_video) as ImageView
            tv = itemView.findViewById(R.id.tv_str) as TextView
        }
    }
}
