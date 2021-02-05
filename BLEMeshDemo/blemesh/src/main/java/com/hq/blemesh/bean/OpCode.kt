package com.hq.blemesh.bean

import com.hq.blemesh.BitSentry
import kotlin.experimental.and

enum class OpCode(val value: Int) {

    NONE(-1),

    APPKEY_ADD(0x00),


    COMPOSITION_DATA_GET(0x0880);


    /**
     * opcode的格式
     * 第一个字节 ：
     *  0x0xxxxxxx --> 1个字节
     *  0x10xxxxxx xxxxxxxx  -> 2个字节
     *  0x11xxxxxx xxxxxxxx xxxxxxxx -> 3个字节
     */
    fun getLength(): Int{
        val firstOctet = value.toByte()
        val bitSentry_7 = BitSentry(7)
        val bitSentry_6 = BitSentry(6)
        if(value == NONE.value) return 0

        if(firstOctet.and(bitSentry_7).toInt() == 0){
            return 1
        }else if(firstOctet.and(bitSentry_6).toInt() == 0){
            return 2
        }else{
            return 3
        }
    }

    fun toBytes(): ByteArray{
        when(getLength()){
            1 -> {
                return byteArrayOf(value.toByte())
            }
            2 -> {
                return byteArrayOf(value.toByte(), value.shl(8).toByte())
            }
            3 -> {
                return byteArrayOf(value.toByte(), value.shl(8).toByte(), value.shl(16).toByte())
            }
            else -> {
                return byteArrayOf()
            }
        }

    }

}