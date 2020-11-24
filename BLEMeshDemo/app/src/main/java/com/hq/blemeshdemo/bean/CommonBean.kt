package com.hq.blemeshdemo.bean

data class UnprovisionDevice(
    val mac: String,
    val scanRecord: ByteArray
)