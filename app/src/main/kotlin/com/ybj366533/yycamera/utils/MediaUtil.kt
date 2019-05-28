package com.ybj366533.yycamera.utils

import android.media.MediaMetadataRetriever
import android.text.TextUtils
import android.util.Log

/**
 * Created by Administrator on 2018/3/27 0027.
 */

object MediaUtil {

    fun getMediaDuration(url: String): Int {

        var duration: String? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(url)
            duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        } catch (ex: Exception) {

            Log.e("MediaUtil", "获取音频时长失败")
        } finally {
            try {
                retriever.release()
            } catch (ex: RuntimeException) {
                Log.d("MediaUtil", "释放MediaMetadataRetriever资源失败")
            }

        }
        return if (!TextUtils.isEmpty(duration)) {
            Integer.parseInt(duration!!)
        } else {
            0
        }

    }


}
