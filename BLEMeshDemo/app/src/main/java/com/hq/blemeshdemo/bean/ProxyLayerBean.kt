package com.hq.blemeshdemo.bean

import com.hq.blemeshdemo.Utils.aesCcm
import com.hq.blemeshdemo.Utils.bytesToHexString
import org.spongycastle.pqc.crypto.rainbow.Layer
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class LayerBean(){
    abstract fun toBytes(): ByteArray
}

val UNSEGMENT_ACCESS_PDU_MAX_LENGTH = 11
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
    val key: ByteArray,
    val nonce: ByteArray,
    val accessPayload: ByteArray
): LayerBean(){


    val value: ByteArray? by lazy {
        val mSize = 4   // 4 或 8 ， 可在后续传入
        aesCcm(accessPayload, key , nonce, mSize, true)
    }

    fun needSegment(): Boolean {
        return accessPayload.size > UNSEGMENT_ACCESS_PDU_MAX_LENGTH
    }

    override fun toBytes(): ByteArray {
        return value ?: byteArrayOf()
    }

    override fun toString(): String {
        return " key : ${bytesToHexString(key, ":")} " +
                "\n  nonce : ${bytesToHexString(nonce, ":")}  " +
                "\n  accessPayload : ${bytesToHexString(accessPayload, ":")} "
    }
}

class LowerLayerBena(
    val akf: Int,
    val aid: Int,
    val upperPayload: ByteArray,
    val needSegment: Boolean
): LayerBean(){

    private val unsegment_seg: Int = 0x00
    private val segment_seg: Int = 0x01

    val order: ByteOrder = ByteOrder.BIG_ENDIAN
    val segmentList: ArrayList<ByteArray> = arrayListOf()

    init {
        if(needSegment){
            //TODO 将比较大的upperPayLoad进行分包， 分别放入到segmentList中

        }
    }

    /**
     * unsegment :
     * seg , 1bit,  0
     * akf , 1bit,
     * aid , 6bit,
     * upperPayload  ,  40 - 120bit ,
     *
     * segment :
     *
     */
    override fun toBytes(): ByteArray {
        if(needSegment){
            return byteArrayOf()
        }else{
            val oct0 = unsegment_seg.shl(7).or(akf.shl(6)).or(aid).toByte()

            return ByteBuffer.allocate(upperPayload.size + 1)
                    .order(order)
                    .put(oct0)
                    .put(upperPayload)
                    .array()
        }
    }

    fun getSegmentList(): List<ByteArray>{
        return emptyList()
    }

}

//class NetworkLayerBean(
//
//): LayerBean(){
//
//}