package com.hq.blemeshdemo.Utils

import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.macs.CMac
import org.spongycastle.crypto.modes.CCMBlockCipher
import org.spongycastle.crypto.params.AEADParameters
import org.spongycastle.crypto.params.KeyParameter

val ZERO_16BIT = ByteArray(16){
    0x00.toByte()
}

fun s1(data: ByteArray): ByteArray{
    val ZERO_KEY = ZERO_16BIT
    return aceCmac(data, ZERO_KEY)
}

fun k1(N: ByteArray, salt:ByteArray, text: String = "prck"): ByteArray{
    val T = aceCmac(N, salt)
    return aceCmac(text.toByteArray(), T)
}

fun aceCmac(data: ByteArray, key: ByteArray): ByteArray{
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