package com.hq.blemeshdemo.Utils

import android.content.Context
import android.content.SharedPreferences


inline fun SharedPreferences.edit( action : SharedPreferences.Editor.() -> Unit ){
    val editor = edit()
    action(editor)
    editor.apply()
}


class SharePre(context: Context){

    private val share: SharedPreferences by lazy {
        context.getSharedPreferences("saveTemp", Context.MODE_PRIVATE)
    }


    fun saveDeviceKey(deviceId: String, deviceKey: String){
        share.edit{
            putString(deviceId, deviceKey)
        }
    }

    fun getDeviceKey(deviceId: String): String{
        return share.getString(deviceId, "") ?: ""
    }

}