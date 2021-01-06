package com.hq.blemeshdemo.Utils

import java.nio.ByteOrder

fun ConcatIntToLong(high: Int, low: Int): Long{
    return high.toLong().and(0xFFFF).shl(16).or(low.toLong().and(0xFFFF))
}

fun ConcatByteToInt(high: Byte, low: Byte): Int{
    return high.toInt().and(0xFF).shl(8).or(low.toInt().and(0xFF))
}

// 1 << bitPos   --  比如： 1 << 5 : 0x00100000  -- 用于获取该位置的值是 1 还是 0
fun BitSentry(bitPos: Int): Byte{
    return 1.toInt().shl(bitPos).toByte()
}

fun integer2Bytes(i: Int, sizeTemp: Int, order: ByteOrder): ByteArray {
    var size = sizeTemp
    if (size > 4) size = 4
    val re = ByteArray(size)
    for (j in 0 until size) {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            re[j] = (i shr 8 * j).toByte()
        } else {
            re[size - j - 1] = (i shr 8 * j).toByte()
        }
    }
    return re
}