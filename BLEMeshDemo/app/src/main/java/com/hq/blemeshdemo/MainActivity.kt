package com.hq.blemeshdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.log


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val OPEN_BLUETOOTH_REQUEST_CODE = 1001

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val scanSetting: ScanSettings by lazy {
        val builder =  ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //高功耗

        if(android.os.Build.VERSION.SDK_INT >= 23){
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY)
        }

        builder.build()
    }

    private var mScanning = false
    private var scanner : BluetoothLeScanner? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        initBlueTooth()
    }

    private fun initReceiver(){
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, intentFilter)
    }

    fun initView(){

        val test_tv: TextView = TextView(this)
        test_tv.text = "HelloFAB"

        menu_LL.addView(test_tv)


    }

    //往操作菜单中添加按钮， 用于显示其他的界面
    @SuppressLint("ResourceAsColor")
    fun addMenuItem(resId: Int, onclick: (v: View) -> Unit){
        val test_FAB: FloatingActionButton =  FloatingActionButton(this)
        test_FAB.setBackgroundColor(R.color.colorAccent)
        test_FAB.setImageResource(resId)
        val layoutParamsTemp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        Log.d(TAG, " layoutParamsTemp is LinearLayout.LayoutParams ")
        layoutParamsTemp.setMargins(0, 0, 0, 44)
        test_FAB.layoutParams = layoutParamsTemp
        test_FAB.setOnClickListener {
            onclick(it)
        }
        menu_LL.addView(test_FAB)
    }

    fun initBlueTooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled){
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            this.startActivityForResult(intent, OPEN_BLUETOOTH_REQUEST_CODE)
        }else{
            startScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == OPEN_BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            startScan()
        }else if(requestCode == OPEN_BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED){
            Toast.makeText(this, "Please Enable BlueTooth !", Toast.LENGTH_LONG).show()
        }
    }

    private fun startScan(){
        //TODO 暂时只适配5.0以上的
        if(android.os.Build.VERSION.SDK_INT >= 21){
            if(mScanning){
                Toast.makeText(this, "正在扫描蓝牙中...!", Toast.LENGTH_LONG).show()
            }else{

                if(scanner == null){
                    scanner = bluetoothAdapter?.bluetoothLeScanner
                }
                scanner?.let {
                    it.startScan(null, scanSetting, mScanCallback)
                    startScanTimer()
                }
            }
        }else{
            Toast.makeText(this, "版本低于5.0暂时未适配!", Toast.LENGTH_LONG).show()
        }

    }

    private fun startScanTimer(){
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if(android.os.Build.VERSION.SDK_INT >= 21){
                if(mScanning){
                    scanner?.let {
                        mScanning = false
                        it.stopScan(mScanCallback)
                    }
                }
            }else{
                Toast.makeText(this, "版本低于5.0暂时未适配!", Toast.LENGTH_LONG).show()
            }
        }, 30*1000)
    }

    private val mScanCallback: ScanCallback = object: ScanCallback(){

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }

    }

    /**
     * 这个发现广播是什么原理， 是系统什么时候去扫描的， 扫描时机是否受用户控制， 是否很耗费资源
     */
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if(BluetoothDevice.ACTION_FOUND == action){
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, " onReceive brief device info  name :  ${device.name}  -- address : ${device.address}  -- uuids : ${device.uuids}")
                Log.d(TAG, " onReceive full device info ： ${Gson().toJson(device)} ")
            }

        }
    }
}
