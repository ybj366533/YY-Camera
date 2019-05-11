package com.ybj366533.yycamera.utils

import java.text.DecimalFormat

/**
 * Created by Administrator on 2018/3/28 0028.
 */

object ValueFormatUtils {
    fun doubleToString(num: Double): String {
        //使用0.00不足位补0，#.##仅保留有效位
        return DecimalFormat("0.00").format(num)
    }
}
