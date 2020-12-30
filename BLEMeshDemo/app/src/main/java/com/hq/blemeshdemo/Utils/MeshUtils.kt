package com.hq.blemeshdemo.Utils

import org.spongycastle.util.Strings

fun getNetworkKey(): ByteArray{
    val netKeyStr = "36:FD:8F:9E:73:95:4A:58:DD:C9:29:DC:DF:F7:F5:7D"
    val netKeyStrArr = Strings.split(netKeyStr, ':')
    val networkKey = ByteArray(16){
        Integer.parseInt(netKeyStrArr[it], 16).toByte()
    }
    return networkKey
}

val netKeyIndex = byteArrayOf(0x00, 0x00)
val ivIndex = byteArrayOf(0x00, 0x00, 0x00, 0x00)
val flags = 0x00.toByte()
val unicastAddress = byteArrayOf(0x03, 0xea.toByte())