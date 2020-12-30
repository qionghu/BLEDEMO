package com.hq.blemeshdemo.bean

import org.spongycastle.pqc.crypto.rainbow.Layer
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class LayerBean(){
    abstract fun toBytes(): ByteArray
}

class AccessLayerBean(
    val opCode: OpCode,
    val params: ByteArray
): LayerBean(){

    val order: ByteOrder = ByteOrder.LITTLE_ENDIAN

    override fun toBytes(): ByteArray {
        val len = opCode.getLength() + params.size
        return ByteBuffer.allocate(len)
                .order(order)
                .put(opCode.toBytes())
                .put(params)
                .array()
    }
}

class UpperLayerBean(

): LayerBean(){
    val order: ByteOrder = ByteOrder.BIG_ENDIAN

    override fun toBytes(): ByteArray {

    }
}