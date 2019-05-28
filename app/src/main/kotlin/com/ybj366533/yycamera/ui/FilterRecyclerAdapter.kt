package com.ybj366533.yycamera.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ybj366533.videolib.utils.YYFilterHelp
import com.ybj366533.yycamera.R


class FilterRecyclerAdapter(//    private ArrayList<MenuBean> mMenuDatas;
        private val context: Context) : RecyclerView.Adapter<ImageHolder>(), View.OnClickListener {

    var effectIcons = intArrayOf(R.drawable.if_normal, // 原图
            // 电影系
            R.drawable.if_1977, R.drawable.if_hefe, R.drawable.if_inkwell, R.drawable.if_lordkelvin, R.drawable.if_nashville, R.drawable.if_earlybird, R.drawable.if_valencia, R.drawable.if_brannan,

            //            R.drawable.if_amaro, R.drawable.if_rise, R.drawable.if_hudson, R.drawable.if_xproii, R.drawable.if_sierra,
            //            R.drawable.if_lomofi, R.drawable.if_sutro, R.drawable.if_toaster,
            //             R.drawable.if_walden,

            // 日系
            R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal,

            // 海
            R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal,

            // 其他
            R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal, R.drawable.if_normal)

    // 如果不是使用所有的滤镜，或者不是按原始顺序排列
    // 这边记录对应的顺序
    // 在构造函数中，直接通过获取的滤镜列表 设定
    private val filterType: IntArray
    //= new int[] {0,
    //        1,2,3,4,5, 6,7,8,9,10, 11,12,13,14,15, 16,17};

    // 也可以通过名字来设定
    private val filterName: Array<String?>
    //    = new String[] {"Normal",
    //            "Amaro","Rise","Hudson","XproII","Sierra",
    //            "Lomofi","Earlybird", "Sutro", "Toaster","Brannan",
    //            "Inkwell", "Walden", "Hefe", "Valencia", "Nashville",
    //            "1977", "LordKelvin"};

    // 用于界面显示
    private val filterTitle: Array<String?>


    private var listener: OnFilterCheckListener? = null

    private var selectPos = 0

    init {

        // 2018/4/15 滤镜修改
        // 因为滤镜比较多，而且考虑到 app会有选择的使用，
        // 增加了获取滤镜列表的接口， 以及通过 滤镜名字 来设置 滤镜
        // 原来通过filtertype（index） 还是可以设置
        // 增加可读性，建议改为 通过滤镜名字设置
        // YYFilterInfo 中的 fillterGroup 用于以后 滤镜分组

        val filterList = YYFilterHelp.getFilterList()
        filterType = IntArray(filterList.size)
        filterName = arrayOfNulls(filterList.size)
        filterTitle = arrayOfNulls(filterList.size)

        for (i in filterList.indices) {
            filterType[i] = filterList[i].filterIndex
            filterName[i] = filterList[i].filterName
            filterTitle[i] = filterList[i].filterTitle

        }


    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ImageHolder {
        return ImageHolder(LayoutInflater.from(context).inflate(R.layout.item_image, viewGroup, false), this)
    }

    override fun onBindViewHolder(effectHolder: ImageHolder, i: Int) {
        effectHolder.effect.setImageResource(effectIcons[i])
        effectHolder.textView.text = filterTitle[i]
        effectHolder.effect.tag = i
        //effectHolder.effect.setSelected(selectPos == i && selectPos != 0);
        effectHolder.effect.isSelected = selectPos == i
    }


    override fun getItemCount(): Int {
        return effectIcons.size
    }

    fun setFilterCheckListener(listener: OnFilterCheckListener) {
        this.listener = listener
    }

    override fun onClick(v: View) {

        selectPos = v.tag as Int
        if (listener != null) {

            // 如果不是使用所有的滤镜，或者不是按原始顺序排列,
            // 则不要 直接使用 控件列表的 序列号，
            // 而是需要使用 转换数组
            // 现在好滤镜序号设定，强烈建议按 滤镜名 设定
            //listener.onFilterChecked(selectPos, "");
            listener!!.onFilterChecked(filterType[selectPos], "")
        }

        notifyDataSetChanged()
    }

    interface OnFilterCheckListener {
        fun onFilterChecked(pos: Int, path: String): Boolean
    }

}
