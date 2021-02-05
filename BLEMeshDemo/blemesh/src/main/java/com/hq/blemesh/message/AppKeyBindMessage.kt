package com.hq.blemesh.message

import com.hq.blemesh.bean.OpCode

class AppKeyBindMessage(
     val AppKeyIndex: Int,
     val NetKeyIndex: Int,
     val AppKey: ByteArray,
     source: Int,
     destination: Int,
     accessType: Byte,
     accessKey: ByteArray
): MeshMessage(source, destination, accessType, accessKey) {

    init {
        opCode = OpCode.APPKEY_ADD
        params = buildParams()
    }

    /**
     *
     * NetKeyIndex  as the first key index  , AppKeyIndex as the second key index
     * first key index (NetKeyIndex) packed into the first octets ( lower octets )
     * 第二个字节 ， NetKeyIndex , AppKeyIndex 一半一半
     * second key index (AppKeyIndex) packed into the third octets ( higher octets )
     * NetKey , AppKey -- 12bits
     *
     *
     */
    private fun buildParams(): ByteArray{
        val AppAndNetKeyIndex = NetKeyIndex.and(0x0FFF).or((AppKeyIndex.and(0x0FFF).shl(12)))


        return byteArrayOf()
    }

}