package com.hq.blemeshdemo.parser

import android.bluetooth.le.ScanResult
import android.util.Log
import com.google.gson.Gson
import com.hq.blemeshdemo.Utils.bytesToHexString
import com.hq.blemeshdemo.bean.UnprovisionDevice


fun ParseUnprovision(result : ScanResult): UnprovisionDevice{
    val TAG = "DeviceParser"
    val device = result.device

    val recordStr = bytesToHexString(result.scanRecord.bytes, ":")
    Log.d(TAG, "parseUnprovision info  name :  ${device?.name}  -- address : ${device?.address}  -- scanRecord : ${recordStr}")
    //Log.d(TAG, "parseUnprovision full device info ： ${Gson().toJson(device)} ")



    return UnprovisionDevice(device.address, result.scanRecord.bytes)
}