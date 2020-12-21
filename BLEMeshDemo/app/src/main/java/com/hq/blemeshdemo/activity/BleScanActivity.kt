package com.hq.blemeshdemo.activity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hq.blemeshdemo.R
import com.hq.blemeshdemo.Utils.toastLong
import com.hq.blemeshdemo.adapter.ScanDeviceListAdapter
import com.hq.blemeshdemo.model.ScanDeviceListViewModel
import kotlinx.android.synthetic.main.activity_ble_scan.*
import java.lang.Exception

class BleScanActivity : AppCompatActivity() {

    private val TAG = "BleScanActivity"

    private var viewModel: ScanDeviceListViewModel? = null

    private var deviceAdapter: ScanDeviceListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_scan)

        viewModel = ViewModelProvider(this).get(ScanDeviceListViewModel::class.java)
        viewModel?.setDisplayMode(ScanDeviceListViewModel.JUST_DISPLAY_UNPROVISION)

        deviceAdapter = ScanDeviceListAdapter(this, viewModel?.deviceListLiveData!!)

        device_recycler_view.layoutManager = LinearLayoutManager(this)
        device_recycler_view.adapter = deviceAdapter

        deviceAdapter?.addItemClickListener {
            //toastLong(this, it.mac)
            viewModel?.stopScan()
            val intent = Intent(this, ProvisionActivity::class.java)
            intent.putExtra("deviceMac", it.mac)
            intent.putExtra("bluetoothDevice", it.bluetoothDevice)
            startActivity(intent)

        }

    }

    override fun onStop() {
        viewModel?.stopScan()
        super.onStop()
    }

//    fun initBlueTooth(){
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//
//        if(bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled){
//            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            this.startActivityForResult(intent, OPEN_BLUETOOTH_REQUEST_CODE)
//        }else{
//            startScan()
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(requestCode == OPEN_BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_OK){
//            startScan()
//        }else if(requestCode == OPEN_BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED){
//            Toast.makeText(this, "Please Enable BlueTooth !", Toast.LENGTH_LONG).show()
//        }
//    }


}