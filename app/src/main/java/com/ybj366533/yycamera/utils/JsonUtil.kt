package com.ybj366533.yycamera.utils

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by libq on 18/5/23.
 */

class JsonUtil(jString: String) {
    private var json: JSONObject? = null


    val jsonString: String
        get() = json!!.toString()

    init {
        try {
            json = JSONObject(jString)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }


    /**
     * 获取属性
     * @param key
     * @return
     */
    fun getParam(key: String): String? {
        var param: String? = null
        if (null == json)
            return param

        try {
            param = json!!.getString(key)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return param
    }


    fun putParam(key: String, value: String): JsonUtil {
        try {
            json!!.put(key, value)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return this
    }


    fun close() {
        json = null

    }



}
