package com.hq.blemeshdemo.Utils

fun ConcatIntToLong(high: Int, low: Int): Long{
    return high.toLong().and(0xFFFF).shl(16).or(low.toLong().and(0xFFFF))
}

fun ConcatByteToInt(high: Byte, low: Byte): Int{
    return high.toInt().and(0xFF).shl(8).or(low.toInt().and(0xFF))
}