package com.hq.blemesh.message

import com.hq.blemesh.bean.OpCode

open class MeshMessage (
    val source: Int,
    val destination: Int,
    val accessType: Byte,
    val accessKey: ByteArray,
    var opCode: OpCode = OpCode.NONE,
    var params: ByteArray = byteArrayOf()
)