package com.hq.blemeshdemo.Utils

import org.spongycastle.util.Strings
import kotlin.experimental.and

fun getNetworkKey(): ByteArray{
    val netKeyStr = "36:FD:8F:9E:73:95:4A:58:DD:C9:29:DC:DF:F7:F5:7D"
    val netKeyStrArr = Strings.split(netKeyStr, ':')
    val networkKey = ByteArray(16){
        Integer.parseInt(netKeyStrArr[it], 16).toByte()
    }
    return networkKey
}

fun getAppKey(): ByteArray{
    return "F26A7D7B878A29AB9E379DFA4FBAB0A3".toByteArray()
}

/**
 * NID -- 7bits
 */
data class TempKeyBean(
    val NID: Byte,
    val encryptKey: ByteArray,
    val privacyKey: ByteArray
)

fun getTempKeyBean(): TempKeyBean{
    val networkKey = getNetworkKey()
    val list = k2(networkKey, byteArrayOf(0x00))
    val NID = list[0][15].and(0x7F)
    val encryptKey = list[1]
    val privacyKey = list[2]

    return TempKeyBean(NID, encryptKey, privacyKey)
}

fun getIVI(): Byte{
    return ivIndex[3].toInt().and(0x01).toByte()
}

val appKeyIndex = 0
val netKeyIndex = byteArrayOf(0x00, 0x00)
val ivIndex = byteArrayOf(0x00, 0x00, 0x00, 0x00)
val flags = 0x00.toByte()
val unicastAddress = byteArrayOf(0x03, 0xea.toByte())
