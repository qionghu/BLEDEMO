package com.hq.blemeshdemo.bean

import android.util.Log
import com.hq.blemesh.bean.OpCode
import com.hq.blemesh.message.MeshMessage
import com.hq.blemeshdemo.Utils.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.xor

val UNSEGMENT_ACCESS_PDU_MAX_LENGTH = 11
const val APPLICATION_KEY_TYPE =0x01.toByte()
const val DEVICE_KEY_TYPE =0x02.toByte()
val seqNumber = AtomicInteger(1)
const val ASZMICAndPad = 0x00.toByte()
const val NONCE_LENGTH = 13 // 1 + 1 + 3 + 2 + 2 + 4
const val TAG = "ProxyLayerBean"



/**
 * TODO 用责任链模式改写 从 Access - Upper - Low - Network 这几层数据的封装
 */
fun transMessageToNetworkPayLoad(message: MeshMessage): ByteArray{
    seqNumber.incrementAndGet()
    val accessPayload = AccessLayerBean(message.opCode, message.params).toBytes()

    val accessNonce = getAccessNonce(message.accessType, message.source, message.destination )
    val accessKey = message.accessKey
    val upperLayerBean = UpperLayerBean(accessKey, accessNonce, accessPayload)
    val upperPayload = upperLayerBean.toBytes()

    Log.d(TAG, "sendMessage upperLayerBean data size : ${upperPayload.size} ")
    if(upperLayerBean.needSegment()){
        Log.d(TAG, "sendMessage needSegment ! ")
    }else if(upperPayload.isNotEmpty()){
        val akf = getAKF(message.accessType)
        val aid = getAID(message.accessType)
        val lowerLayerPayload = LowerLayerBena(akf, aid, upperPayload, false).toBytes()
        val CTL = getCTL()
        val tempKeyBean = getTempKeyBean()
        val networkLayerBean =
            NetworkLayerBean(
                NID = tempKeyBean.NID ,
                CTL = CTL ,
                SRC = message.source,
                DST = message.destination,
                encryptionKey = tempKeyBean.encryptKey,
                privacyKey = tempKeyBean.privacyKey,
                lowerLayerPayload = lowerLayerPayload
                )
        val networkPayload = networkLayerBean.toBytes()
        Log.d(TAG, "transMessageToNetworkPayLoad ${bytesToHexString(networkPayload, ":")}")
        return networkPayload
    }else{
        Log.d(TAG, "sendMessage upperPayload is Empty  ! ")
    }

    return byteArrayOf()
}




/**
 * Nonce Type - 1 -- accessType -- APPLICATION_NONCE_TYPE(0x01) or DEVICE_NONCE_TYPE(0x02)
 * ASZMIC and Pad - 1  -- 默认用 0x00
 * SEQ -- 3
 * SRC -- 2
 * DST -- 2
 * IV Index --
 *
 */
private fun getAccessNonce(accessType: Byte, src: Int, dst: Int): ByteArray{
    val order = ByteOrder.BIG_ENDIAN
    val SEQ = getSeqByteArray(order)

    return ByteBuffer.allocate(NONCE_LENGTH).order(order)
        .put(accessType)
        .put(ASZMICAndPad)
        .put(SEQ)
        .putShort(src.toShort())
        .putShort(dst.toShort())
        .put(ivIndex)
        .array()
}




private fun getAKF(accessType: Byte): Int{
    return if(accessType == APPLICATION_KEY_TYPE) 1 else 0
}

private fun getAID(accessType: Byte): Int{
    return if(accessType == APPLICATION_KEY_TYPE){
        //TODO app key identifier
        1
    } else 0
}

private fun getCTL(isAccessMessage: Boolean = true ): Byte{
    return if(isAccessMessage) 0 else 1
}

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
    val key: ByteArray,
    val nonce: ByteArray,
    val accessPayload: ByteArray
): LayerBean(){


    val value: ByteArray by lazy {
        val mSize = 4   // 4 或 8 ， 可在后续传入
        aesCcm(accessPayload, key , nonce, mSize, true) ?: byteArrayOf()
    }

    fun needSegment(): Boolean {
        return value.size > UNSEGMENT_ACCESS_PDU_MAX_LENGTH
    }

    override fun toBytes(): ByteArray {
        return value
    }

    override fun toString(): String {
        return " key : ${bytesToHexString(key, ":")} " +
                "\n  nonce : ${bytesToHexString(nonce, ":")}  " +
                "\n  accessPayload : ${bytesToHexString(accessPayload, ":")} "
    }
}

/**
 * akf , aid 的值由 UpperLayerPdu 的加密密钥类型决定
 * 如果在UpperLayerPdu层的加密密钥是 application key --  akf = 1 , aid 为 application key identifier (AID)
 * 如果在UpperLayerPdu层的加密密钥是 device key  -- akf = 0 , aid = 0b000000
 */
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

fun getSeqByteArray(order: ByteOrder): ByteArray{
    return integer2Bytes(seqNumber.get(), 3, order)
}

class NetworkLayerBean(
    val NID: Byte,
    val CTL: Byte,
    val SRC: Int,
    val DST: Int,
    val encryptionKey: ByteArray,
    val privacyKey: ByteArray,
    val lowerLayerPayload: ByteArray,
    val TTL: Byte = 3
): LayerBean(){

    val order: ByteOrder = ByteOrder.BIG_ENDIAN

    private var iviNid: Byte = 0x00
    private var ctlTTL: Byte = 0x00
    private val IVI by lazy { getIVI() }

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
     *      key : encryptionKey (NID , encryptionKey , privateKey 都是从 NetKey 通过 k2() 算法算出来的 )
     *      ccm ( unencrypterPdu , key , nonce , miclen , true)
     *   3. 混淆 CTL TTL SEQ SRC 生成 obfuscate data
     *      privacy random : encrypterData[0, 6]前7位值
     *      plaintext : 0x0000000000 || IV Index || privacy random
     *      pecb = aes ( plaintext , privacyKey )
     *      temp = ctlTTL || SEQ || SRC
     *      obfuscate data =  [  temp xor pecb  ] [0, 5] 前6位值
     *   4. data = iviNid || obfuscate data || encrypterData
     *
     *  IVI : 1bit :  least significant bit of IV index ( ivIndex & 0x01 )
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
        val encryptionData = getEncryptionData() ?: byteArrayOf()
        val privacyRandom = ByteArray(7)
        System.arraycopy(encryptionData, 0, privacyRandom, 0, 7)
        val obfuscateData = getObfuscateData(privacyRandom) ?: byteArrayOf()

        if(encryptionData.isEmpty() || obfuscateData.isEmpty()){
            Log.d("NetworkLayerBean", "toBytes encryptionData or obfuscateData is empty !")
            return byteArrayOf()
        }

        val length = 1+encryptionData.size+obfuscateData.size
        return ByteBuffer.allocate(length).order(order)
            .put(iviNid)
            .put(obfuscateData)
            .put(encryptionData)
            .array()
    }

    private fun getObfuscateData(privacyRandom: ByteArray): ByteArray?{
        val temp = 0x00000000
        val plainText = ByteBuffer.allocate(15).order(order).putInt(temp).put(ivIndex).put(privacyRandom).array()
        val pecb = e(privacyKey, plainText) ?: byteArrayOf()
        val temp2 = ByteBuffer.allocate(6).order(order).put(ctlTTL).put(getSeqByteArray(order)).putShort(SRC.toShort()).array()
        val obfuscateArr = ByteArray(6)

        if(pecb.size < 6 || temp2.size < 6){
            Log.d("NetworkLayerBean", "getObfuscateData obfuscateArr or temp2 size less than 6 !")
            return null
        }

        for (index in pecb.indices){
            //只迭代前6个byte内容
            if(index > 5) break

            obfuscateArr[index] = pecb[index].xor(temp2[index])
        }

        return obfuscateArr
    }

    private fun getEncryptionData(): ByteArray? {
        val nonce = getNetWorkNonce()
        val unEncryptionData = ByteBuffer.allocate(2+lowerLayerPayload.size).order(order).putShort(DST.toShort()).put(lowerLayerPayload).array()
        val MICLen = getMicLen()
        return aesCcm(unEncryptionData, encryptionKey , nonce, MICLen, true)
    }

    private fun getMicLen(): Int{
        return if(CTL == 0x00.toByte()) 4 else 8
    }



    //network nonce : nonce Type 0x00 - CTL and TTL 1octet - SEQ 3octet - SRC 2octet - Pad 0x0000 - IV Index 4octet 总共13octet
    private fun getNetWorkNonce(): ByteArray{
        val seqByteArray = getSeqByteArray(order)
        val Pad = byteArrayOf(0x00, 0x00)
        val nonce = ByteBuffer.allocate(13).order(order)
            .put(0x00)
            .put(ctlTTL)
            .put(seqByteArray)
            .putShort(SRC.toShort())
            .put(Pad)
            .put(ivIndex)
            .array()

        return nonce
    }


}