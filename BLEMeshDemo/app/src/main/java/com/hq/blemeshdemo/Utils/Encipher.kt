package com.hq.blemeshdemo.Utils

import org.spongycastle.crypto.CipherParameters
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.engines.AESLightEngine
import org.spongycastle.crypto.macs.CMac
import org.spongycastle.crypto.modes.CCMBlockCipher
import org.spongycastle.crypto.params.AEADParameters
import org.spongycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer

val ZERO_16BIT = ByteArray(16){
    0x00.toByte()
}

fun s1(data: ByteArray): ByteArray{
    val ZERO_KEY = ZERO_16BIT
    return aesCmac(data, ZERO_KEY)
}

fun k1(N: ByteArray, salt:ByteArray, text: String = "prck"): ByteArray{
    val T = aesCmac(N, salt)
    return aesCmac(text.toByteArray(), T)
}

/**
 *  input params N , P
 *  N is 128bits data
 *  P is 1 or more octets
 *  salt = s1("smk2".toByteArray())
 *  T = aesCmac(N , salt )
 *  T0 = empty string( zero length )
 *  T1 = aesCmac( T0||P||0x01 , T)
 *  T2 = aesCmac( T1||P||0x02 , T)
 *  T3 = aesCmac( T2||P||0x03 , T)
 *
 *  k2(N, P) = (T1||T2||T3) mod 2 ^ 263
 */
fun k2(N: ByteArray, p: ByteArray): List<ByteArray>{
    val salt = s1("smk2".toByteArray())
    val T = aesCmac(N, salt)
    val T0 = byteArrayOf()
    val tempN1 = ByteBuffer.allocate(p.size + 1).put(T0).put(p).put(0x01.toByte()).array()
    val T1 = aesCmac(tempN1, T)
    val tempN2 = ByteBuffer.allocate(T1.size + p.size + 1).put(T1).put(p).put(0x02.toByte()).array()
    val T2 = aesCmac(tempN2, T)
    val tempN3 = ByteBuffer.allocate(T2.size + p.size + 1).put(T2).put(p).put(0x03.toByte()).array()
    val T3 = aesCmac(tempN3, T)

    return listOf(T1, T2, T3)
}

fun e(key: ByteArray?, data: ByteArray): ByteArray? {
    val encrypted = ByteArray(data.size)
    val cipherParameters: CipherParameters = KeyParameter(key)
    val engine = AESLightEngine()
    engine.init(true, cipherParameters)
    engine.processBlock(data, 0, encrypted, 0)
    return encrypted
}

fun aesCmac(data: ByteArray, key: ByteArray): ByteArray{
    val keySpec = KeyParameter(key)
    val aesEngine = AESEngine()
    val cmac = CMac(aesEngine)

    cmac.init(keySpec)
    cmac.update(data, 0, data.size)

    val re = ByteArray(16)
    cmac.doFinal(re, 0)
    return re
}

fun aesCcm(data: ByteArray, key: ByteArray?, nonce: ByteArray?, micSize: Int, encrypt: Boolean): ByteArray? {
    val result = ByteArray(data.size + if (encrypt) micSize else -micSize)
    val ccmBlockCipher = CCMBlockCipher(AESEngine())
    val aeadParameters = AEADParameters(KeyParameter(key), micSize * 8, nonce)
    ccmBlockCipher.init(encrypt, aeadParameters)
    ccmBlockCipher.processBytes(data, 0, data.size, result, data.size)
    return try {
        ccmBlockCipher.doFinal(result, 0)
        result
    } catch (e: InvalidCipherTextException) {
        null
    }
}