package com.hq.blemeshdemo.bean

import android.util.Log


abstract class ProvisionBean(){
    abstract fun toBytes(): ByteArray
}

class ProvisionInvitBean(
    val attention_time: Byte
): ProvisionBean(){
    override fun toBytes(): ByteArray {
        return byteArrayOf(attention_time)
    }
}

/**
 * Short代表字段内容两个字节octet
 */
class ProvisionCapabilitiesBean(
    val numberOfElement: Byte,
    val algorithmValue: Short,
    val publicKeyType: Byte,
    val staticOOBType: Byte,
    val outputOOBSize: Byte,
    val outputOOBAction: Short,
    val inputOOBSize: Byte,
    val inputOOBAction: Short,
    val rawData: ByteArray
): ProvisionBean(){

    companion object{
        fun parseData(data: ByteArray): ProvisionCapabilitiesBean? {
            if(data.size < 11){
                Log.d("Capab..Bean", "parseData data size is not 11 , size is ${data.size}")
                return null
            }

            return ProvisionCapabilitiesBean(
                    numberOfElement = data[0],
                    algorithmValue = ( data[1].toInt().and(0xFF).shl(8) ).or(data[2].toInt().and(0xFF)).toShort(),
                    publicKeyType = data[3],
                    staticOOBType = data[4],
                    outputOOBSize = data[5],
                    outputOOBAction = ( data[6].toInt().and(0xFF).shl(8) ).or(data[7].toInt().and(0xFF)).toShort(),
                    inputOOBSize = data[8],
                    inputOOBAction = ( data[9].toInt().and(0xFF).shl(8) ).or(data[10].toInt().and(0xFF)).toShort(),
                    rawData = data
            )
        }
    }

    override fun toBytes(): ByteArray{
        return rawData
    }



    override fun toString(): String {
        return " numberOfElement: ${numberOfElement.toInt()} ,  " +
                " algorithmValue: ${algorithmValue.toInt()} ,  " +
                " publicKeyType: ${publicKeyType.toInt()} ,  " +
                " staticOOBType: ${staticOOBType.toInt()} ,  " +
                " outputOOBSize: ${outputOOBSize.toInt()} ,  " +
                " outputOOBAction: ${outputOOBAction.toInt()} ,  " +
                " inputOOBSize: ${inputOOBSize.toInt()} ,  " +
                " inputOOBAction: ${inputOOBAction.toInt()} ,  "
    }

}

class ProvisionStartBean(
    val algorithm: Byte,
    val publicKey: Byte,
    val authenticationMethod: Byte,
    val authenticationAction: Byte,
    val authenticationSize: Byte
): ProvisionBean(){


    override fun toBytes(): ByteArray {
        return byteArrayOf(algorithm, publicKey, authenticationMethod, authenticationAction, authenticationSize)
    }
}


class ProvisionPublicKeyBean(
    val keyX: ByteArray,
    val keyY: ByteArray
): ProvisionBean(){

    companion object{
        fun parseData(data: ByteArray): ProvisionPublicKeyBean? {
            if(data.size < 32){
                Log.d("PublicKeyBean", "parseData data size is not 32 , size is ${data.size}")
                return null
            }

            val xByteArray = ByteArray(16)
            val yByteArray = ByteArray(16)
            System.arraycopy(data, 0 , xByteArray, 0 , 16)
            System.arraycopy(data, 16 , yByteArray, 0 , 16)

            return ProvisionPublicKeyBean(
                xByteArray,
                yByteArray
            )
        }
    }

    override fun toBytes(): ByteArray {
        val data = ByteArray(keyX.size + keyY.size)
        System.arraycopy(keyX, 0 , data, 0, keyX.size)
        System.arraycopy(keyY, 0 , data , keyX.size, keyY.size)
        return data
    }

}