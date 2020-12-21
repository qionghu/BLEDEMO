package com.hq.blemeshdemo.parser

import android.bluetooth.le.ScanResult
import android.util.Log
import com.hq.blemeshdemo.Utils.bytesToHexString
import com.hq.blemeshdemo.bean.AdvertingDevice


fun ParseUnprovision(result : ScanResult): AdvertingDevice{
    val TAG = "DeviceParser"
    val device = result.device

    val recordStr = bytesToHexString(result.scanRecord.bytes, ":")
    Log.d(TAG, "parseUnprovision info  name :  ${device?.name}  -- address : ${device?.address}  -- scanRecord : ${recordStr}")
    //Log.d(TAG, "parseUnprovision full device info ： ${Gson().toJson(device)} ")

    val byteArray = result.scanRecord.bytes
    val len = byteArray[0].toInt().and(0xFF)
    val type = byteArray[1]
    val isMeshDevice = (type.toInt().and(0xFF) == 0x2B)
    val beaconType = byteArray[2]
    val isUnprovisionDevice = (beaconType.toInt().and(0xFF) == 0x00)
    val uuidArr = ByteArray(16)
    System.arraycopy(byteArray, 3, uuidArr, 0, uuidArr.size)
    val uuidStr = bytesToHexString(uuidArr, ":")
    val oobInfo = ByteArray(2)
    val uriHash = ByteArray(4)
    System.arraycopy(byteArray, 19, oobInfo, 0, oobInfo.size)
    System.arraycopy(byteArray, 21, uriHash, 0, uriHash.size)

    return AdvertingDevice(device.address, device,  isMeshDevice, isUnprovisionDevice, type, beaconType, uuidStr, oobInfo, uriHash,  result.scanRecord.bytes)
}