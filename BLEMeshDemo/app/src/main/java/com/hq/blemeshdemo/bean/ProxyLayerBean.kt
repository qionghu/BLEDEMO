package com.hq.blemeshdemo.bean

import com.hq.blemeshdemo.Utils.aesCcm
import com.hq.blemeshdemo.Utils.bytesToHexString
import com.hq.blemeshdemo.Utils.integer2Bytes
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

class NetworkLayerBean(
    val IVI: Byte,
    val NID: Byte,
    val CTL: Byte,
    val TTL: Byte,
    val SEQ: Int,
    val SRC: Int,
    val DST: Int,
    val encryptionKey: ByteArray,
    val privacyKey: ByteArray,
    val lowerLayerPayload: ByteArray
): LayerBean(){

    val order: ByteOrder = ByteOrder.BIG_ENDIAN

    private var iviNid: Byte = 0x00
    private var ctlTTL: Byte = 0x00

    init {
        iviNid = IVI.toInt().shl(7).or(NID.toInt()).toByte()
        ctlTTL = CTL.toInt().shl(7).or(TTL.toInt()).toByte()
    }

    /**
     * 对于 encryptionkey , privacykey 的生成是通过NetworkKey生成的
     *  NID || encryptionKey || privacyKey == k2 ( networkKey , 0x00 )
     */

    /**
     *  1. 通过 IVI , NID 生成 iviNid , 通过 CTL , TTL 生成 ctlTTL
     *  2. 通过 lowerPDU 和 DST 加密生成 transport 和 NetMIC
     *      network nonce : nonce Type 0x00 - CTL and TTL 1octet - SEQ 3octet - SRC 2octet - Pad 0x0000 - IV Index 4octet 总共13octet
     *      unencrypterPdu : big-endian , dst + lowerPdu
     *      micLen : ctl == 0 ? 4 : 8
     *      key : encryptionKey 具体怎么生成的 ？？？
     *      ccm ( unencrypterPdu , key , nonce , miclen , true)
     *   3. 混淆 CTL TTL SEQ SRC 生成 obfuscate data
     *      privacy random : encrypterData[0, 6]前7位值
     *      plaintext : 0x0000000000 || IV Index || privacy random
     *      pecb = aes ( plaintext , privacyKey )
     *      temp = ctlTTL || SEQ || SRC
     *      obfuscate data =  [  temp xor pecb  ] [0, 5] 前6位值
     *   4. data = iviNid || obfuscate data || encrypterData
     *
     *  IVI : 1bit :  least significant bit of IV index
     *  NID : 7bit :
     *  CTL : 1bit :
     *  TTL : 7bit : Time to live
     *  SEQ : 24bit : sequence number
     *  SRC : 16bit : source address
     *  DST : 16bit : destinaiton address
     *  Transport PDU : 8 -  128 bit  :
     *  NetMIC : 32 or 64 bit : Message integrity check for network
     */
    override fun toBytes(): ByteArray {
        return byteArrayOf()
    }

    //network nonce : nonce Type 0x00 - CTL and TTL 1octet - SEQ 3octet - SRC 2octet - Pad 0x0000 - IV Index 4octet 总共13octet
    private fun getNonce(): ByteArray{
        val seqByteArray = integer2Bytes(SEQ, 3, order)
        val Pad = byteArrayOf(0x00, 0x00)
        val nonce = ByteBuffer.allocate(13).order(order).put(0x00).put(ctlTTL).put(seqByteArray).putShort(SRC.toShort()).put(Pad).put()

    }


}