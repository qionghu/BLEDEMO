package com.hq.blemeshdemo.bean

import android.bluetooth.BluetoothDevice

data class AdvertingDevice(
    val mac: String,
    val bluetoothDevice: BluetoothDevice,
    val isMeshDevice: Boolean,
    val isUnprovisionDevice: Boolean,
    val type: Byte,
    val beaconType: Byte,
    val uuid: String,
    val oobInfo: ByteArray, //2 Octets
    val uriHash: ByteArray, //4 Octets
    val scanRecord: ByteArray
)

enum class ProvisionStatus(val status: Int){
    None(-1),
    Invite(0),
    Capabilities(1),
    Start(2),
    PublicKey(3),
    InputComplete(4),
    Confirmation(5),
    Random(6),
    Data(7),
    Complete(8),
    Failed(9)
}

data class ProvisionResult(
    val status: ProvisionStatus
)