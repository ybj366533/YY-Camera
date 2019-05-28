package com.ybj366533.yycamera.ui

import android.content.Context
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ybj366533.videolib.utils.YYMusicHandler

import com.ybj366533.yycamera.R


class MusicSelectRecyclerAdapter(//    private ArrayList<MenuBean> mMenuDatas;
        private val context: Context)//        mMenuDatas = new ArrayList<>();
//        initMenuData("modelsticker/stickers.json");
//        for (int i = 0; i < effectIcons.length && i < mMenuDatas.size(); i++) {
//            mMenuDatas.get(i).icon = effectIcons[i];
//        }
    : RecyclerView.Adapter<MusicSelectRecyclerAdapter.ItemMusicHolder>(), View.OnClickListener {

    //    public int[] effectIcons = new int[]{
    //            R.drawable.if_normal,       // 原图
    //            R.drawable.if_amaro, R.drawable.if_rise, R.drawable.if_hudson, R.drawable.if_xproii, R.drawable.if_sierra,
    //            R.drawable.if_lomofi, R.drawable.if_earlybird, R.drawable.if_sutro, R.drawable.if_toaster, R.drawable.if_brannan,
    //            R.drawable.if_inkwell, R.drawable.if_walden, R.drawable.if_hefe, R.drawable.if_valencia, R.drawable.if_nashville,
    //            R.drawable.if_1977, R.drawable.if_lordkelvin
    //    };

    var imageRes = intArrayOf(R.mipmap.music_panama, R.mipmap.music_travel, R.mipmap.music_hongzhaoyuan, R.mipmap.music_love)
    var title = intArrayOf(R.string.music_panama, R.string.music_travel, R.string.music_hongzhaoyuan, R.string.music_love)
    var player = arrayOf("Matteo", "校长", "音阙诗听", "Taylor Swift")
    var duration = arrayOf("00:15", "00:32", "00:17", "00:56")
    var fileName = arrayOf("panama-2.mp3", "lvxing.mp3", "hongzhaoyuan-2.mp3", "love.mp3")


    private var listener: OnMusicSelectListener? = null

    private var selectPos = -1             //选择的位置
    private var playPos = -1               // 播放的位置

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ItemMusicHolder {
        return ItemMusicHolder(LayoutInflater.from(context).inflate(R.layout.item_music, viewGroup, false), this)
    }

    override fun onBindViewHolder(imgTextHolder: ItemMusicHolder, i: Int) {
        imgTextHolder.coverView.setImageResource(imageRes[i])
        imgTextHolder.titleView.setText(title[i])
        imgTextHolder.playerView.text = player[i]
        imgTextHolder.durationView.text = duration[i]
        imgTextHolder.checkView.isSelected = i == selectPos
        imgTextHolder.playBtn.isSelected = i == playPos

        imgTextHolder.itemView.tag = MyTag("item", i)
        imgTextHolder.coverView.tag = MyTag("cover", i)
        imgTextHolder.playBtn.tag = MyTag("play", i)

        //imgTextHolder.textView.setText(testStr[i]);
        //imgTextHolder.textView.setTag(i);
        //        effectHolder.effect.setImageResource(effectIcons[i]);
        //        effectHolder.effect.setTag(i);
        //        //effectHolder.effect.setSelected(selectPos == i && selectPos != 0);
        //        effectHolder.effect.setSelected(selectPos == i);
    }


    override fun getItemCount(): Int {
        return imageRes.size
    }

    fun setOnMusicSelectListener(listener: OnMusicSelectListener) {
        this.listener = listener
    }

    override fun onClick(v: View) {

        val tag = v.tag as MyTag

        //        ToolUtils.DebugLog("BBBBBBBBBBBBBBBB", " " + tag.getTagLabel() + " " + tag.getTagIndex());

        // 单击的是封面或者播放按钮
        if (tag.tagLabel.equals("cover", ignoreCase = true) || tag.tagLabel.equals("play", ignoreCase = true)) {
            if (playPos == tag.tagIndex) {
                playPos = -1
                // 结束播放
                YYMusicHandler.getInstance().stopPlay()
            } else {
                playPos = tag.tagIndex
                //开始播放
                val dest = Environment.getExternalStorageDirectory().toString() + "/VideoRecorderTest/music/" + fileName[playPos]
                YYMusicHandler.getInstance().playMusic(dest)
            }
        } else {
            if (selectPos == tag.tagIndex) {
                selectPos = -1
            } else {
                selectPos = tag.tagIndex
            }

            if (listener != null) {
                listener!!.onMusicSelect(selectPos, if (selectPos == -1) null else fileName[selectPos])
            }
        }

        //        if (selectPos == (int) v.getTag()) {
        //            selectPos = 99;
        //        } else {
        //            selectPos = (int) v.getTag();
        //        }
        //


        notifyDataSetChanged()
    }

    interface OnMusicSelectListener {
        fun onMusicSelect(pos: Int, fileName: String?): Boolean
    }


    inner class ItemMusicHolder(itemView: View, clickListener: View.OnClickListener) : RecyclerView.ViewHolder(itemView) {

        var coverView: ImageView
        var playBtn: ImageView
        var titleView: TextView
        var playerView: TextView
        var durationView: TextView
        var checkView: ImageView

        init {
            coverView = itemView.findViewById<View>(R.id.img_music_cover) as ImageView
            playBtn = itemView.findViewById<View>(R.id.img_music_play) as ImageView
            titleView = itemView.findViewById<View>(R.id.txt_music_title) as TextView
            playerView = itemView.findViewById<View>(R.id.txt_Music_player) as TextView
            durationView = itemView.findViewById<View>(R.id.txt_Music_duration) as TextView
            checkView = itemView.findViewById<View>(R.id.img_music_check) as ImageView

            itemView.setOnClickListener(clickListener)
            coverView.setOnClickListener(clickListener)
            playBtn.setOnClickListener(clickListener)
        }

    }

    private inner class MyTag(val tagLabel: String, val tagIndex: Int)
}
