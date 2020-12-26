package com.hq.blemeshdemo.activity

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.hq.blemeshdemo.R
import com.hq.blemeshdemo.Utils.UUID
import com.hq.blemeshdemo.model.ProvisionViewModel
import kotlinx.android.synthetic.main.activity_provision.*

class ProvisionActivity : AppCompatActivity() {
    private val TAG = "ProvisionActivity"

    private var deviceMac: String = ""
    private var device: BluetoothDevice? = null
    private var viewModel: ProvisionViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provision)

        viewModel = ViewModelProvider(this).get(ProvisionViewModel::class.java)

        deviceMac = intent.getStringExtra("deviceMac")
        device = intent.getParcelableExtra("bluetoothDevice")
        provison_tv.text = deviceMac

        viewModel?.connectDevice(this, device)
    }

    override fun onStop() {
        viewModel?.close()
        super.onStop()
    }


}