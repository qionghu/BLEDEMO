package com.hq.blemesh

// 1 << bitPos   --  比如： 1 << 5 : 0x00100000  -- 用于获取该位置的值是 1 还是 0
fun BitSentry(bitPos: Int): Byte{
    return 1.toInt().shl(bitPos).toByte()
}