package com.hq.blemeshdemo.bean

import com.hq.blemeshdemo.Utils.ConcatByteToInt
import com.hq.blemeshdemo.Utils.ConcatIntToLong
import com.hq.blemeshdemo.Utils.bytesToHexString

data class UnprovisionDevice(
    val mac: String
){
    var pid: Long = -1

    private var scanRecord = ByteArray(0)
    private var uuidArr = ByteArray(16)
    private var dataIsMesh = false

    fun setScanRecord(scanRecord: ByteArray){
        this.scanRecord = scanRecord
        parseScanRecord()
    }

    fun getScanRecord(): ByteArray{
        return this.scanRecord
    }

    fun getUuidArr(): ByteArray{
        return this.uuidArr
    }

    fun getDataIsMesh(): Boolean{
        return dataIsMesh
    }

    private fun parseScanRecord(){

        //从11位置解析数据，是按照晶讯的格式

        if(this.scanRecord.isNotEmpty()){

            //PB-ADV的广播包
            if(this.scanRecord[1] == 0x2B.toByte()){

                System.arraycopy(this.scanRecord, 3, uuidArr, 0, 16)

            }else if( this.scanRecord.size > 27 ){

                System.arraycopy(this.scanRecord, 11, uuidArr, 0, 16)

            }


            if(uuidArr.isNotEmpty()){

                //0 -> 00
                // 1, 2, 3, 4  --> pid
                val pidArr = ByteArray(4)
                System.arraycopy(uuidArr, 1, pidArr, 0, pidArr.size)

                pid = ConcatIntToLong(ConcatByteToInt(pidArr[3], pidArr[2]), ConcatByteToInt(pidArr[1], pidArr[0]))


                if(pid > 0) dataIsMesh = true

                //5 -  feature
                //6 -  model

                //13, 14  -- rfu
            }


        }




    }

    override fun toString(): String {

        return "mac : $mac  -- dataIsMesh: $dataIsMesh -- pid : $pid  -- uuidArr : ${bytesToHexString(uuidArr, ":")}"

    }


}