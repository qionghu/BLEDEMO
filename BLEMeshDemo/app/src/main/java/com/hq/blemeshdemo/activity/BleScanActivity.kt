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
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hq.blemeshdemo.R
import com.hq.blemeshdemo.adapter.ScanDeviceListAdapter
import com.hq.blemeshdemo.model.ScanDeviceListViewModel
import kotlinx.android.synthetic.main.activity_ble_scan.*
import java.lang.Exception

class BleScanActivity : AppCompatActivity() {

    private val TAG = "BleScanActivity"

    private val OPEN_BLUETOOTH_REQUEST_CODE = 1001

    private var viewModel: ScanDeviceListViewModel? = null

    private var deviceAdapter: ScanDeviceListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_scan)

        viewModel = ViewModelProvider(this).get(ScanDeviceListViewModel::class.java)

        deviceAdapter = ScanDeviceListAdapter(this, viewModel?.deviceListLiveData!!)

        device_recycler_view.layoutManager = LinearLayoutManager(this)
        device_recycler_view.adapter = deviceAdapter

        setActionBar()

    }

    private fun setActionBar(){
        supportActionBar?.title = "扫描Mesh设备"
        val options = ActionBar.DISPLAY_SHOW_TITLE.or(ActionBar.DISPLAY_HOME_AS_UP)
        Log.d(TAG, "setActionBar options : $options")
        supportActionBar?.displayOptions = options
        supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId){
            android.R.id.home -> {
                viewModel?.stopScan()
                finish()
            }

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        val status = viewModel?.startScan()

        if(status == ScanDeviceListViewModel.SCAN_STATUS_ADAPTER_DISABLE){
            requestBluetoothAdapter()
        }

        super.onResume()
    }

    fun requestBluetoothAdapter(){
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        this.startActivityForResult(intent, OPEN_BLUETOOTH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == OPEN_BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            viewModel?.initBlueTooth()
            viewModel?.startScan()
        }else if(requestCode == OPEN_BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED){
            Toast.makeText(this, "Please Enable BlueTooth !", Toast.LENGTH_LONG).show()
        }
    }


}