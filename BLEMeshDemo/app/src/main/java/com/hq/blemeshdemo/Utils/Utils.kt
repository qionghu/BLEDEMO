package com.hq.blemeshdemo.Utils

import android.content.Context
import android.widget.Toast
import java.util.*

fun toast(context: Context, str: String){
    Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
}

fun toastLong(context: Context, str: String){
    Toast.makeText(context, str, Toast.LENGTH_LONG).show()
}

fun bytesToHexString( array: ByteArray? ,  separator: String): String {
    if (array == null || array.isEmpty())
        return "";

    val sb: StringBuilder = StringBuilder();

    val formatter =  Formatter(sb);
//    formatter.format("%02X", array[0]);

    array.forEach {
        if(sb.isNotEmpty()){
            sb.append(separator);
        }
        formatter.format("%02X", it);
    }

    formatter.flush();
    formatter.close();

    return sb.toString();
}