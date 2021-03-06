package com.hq.blemeshdemo.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hq.blemeshdemo.bean.AdvertingDevice
import com.hq.blemeshdemo.parser.ParseUnprovision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

const val ALL_DISPLAY = 1
const val DISPLAY_ALL_MESH = 2
const val JUST_DISPLAY_UNPROVISION = 3
const val JUST_DISPLAY_BIND = 4

fun getDisplayString(displayMode: Int): String{
    when(displayMode){
        ALL_DISPLAY -> {
            return "ALL_DISPLAY"
        }
        DISPLAY_ALL_MESH -> {
            return "ALL_MESH"
        }
        JUST_DISPLAY_UNPROVISION -> {
            return "UNPROVISION_DEVICE"
        }
        JUST_DISPLAY_BIND -> {
            return "BINDED_DEVICE"
        }
        else -> {
            return "NONE"
        }
    }
}

class ScanDeviceListViewModel : ViewModel() {
    val TAG = "ScanDeviceListViewModel"

    private val OPEN_BLUETOOTH_REQUEST_CODE = 1001

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var mScanning = false
    private var scanner : BluetoothLeScanner? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val scanSetting: ScanSettings by lazy { buildScanSettings() }
    private val mScanCallback: ScanCallback by lazy { buildScanCallBack() }
    private var displayMode: Int = ALL_DISPLAY
    private val scan_time_out = 10*1000L

    fun setDisplayMode(mode: Int){
        displayMode = mode

        if(mScanning){
            GlobalScope.launch(Dispatchers.IO){
                stopScan()

                delay(2*1000L)

                startScan()
            }
        }else{
            startScan()
        }
    }

    val deviceListLiveData: MutableLiveData<List<AdvertingDevice>> by lazy {
        MutableLiveData<List<AdvertingDevice>>()
    }

    private val dataMap : HashSet<String> = HashSet()

    init {

        deviceListLiveData.observeForever{
            it.forEach {
                if(!dataMap.contains(it.mac)){
                    dataMap.add(it.mac)
                }
            }
        }

        initBlueTooth()
    }

    fun initBlueTooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled){
           // val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
           // this.startActivityForResult(intent, OPEN_BLUETOOTH_REQUEST_CODE)
            Log.d(TAG, "bluetoothAdapter 不正常 ！")
        }else{
            startScan()
        }
    }

    private fun startScan(){
        //TODO 暂时只适配5.0以上的
        if(android.os.Build.VERSION.SDK_INT >= 21){
            if(mScanning){
                Log.d(TAG, "正在扫描蓝牙中...!")
            }else{

                if(scanner == null){
                    scanner = bluetoothAdapter?.bluetoothLeScanner
                }
                scanner?.let {
                    Log.d(TAG, " start scan!")
                    try {
                        deviceListLiveData.postValue(emptyList())
                        it.startScan(null, scanSetting, mScanCallback)
                        mScanning = true
                    }catch (exception: Exception){
                        exception.printStackTrace()
                    }
                    startScanTimer()
                }
            }
        }else{
            Log.d(TAG, "版本低于5.0暂时未适配!")
        }

    }

    fun stopScan(){
        if(android.os.Build.VERSION.SDK_INT >= 21){
            if(mScanning){
                scanner?.let {
                    mScanning = false
                    it.stopScan(mScanCallback)
                    Log.d(TAG, "停止扫描!")
                }
            }
        }else{
            Log.d(TAG, "版本低于5.0暂时未适配!")
        }
    }

    private fun startScanTimer(){
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            stopScan()
        }, scan_time_out)
    }



    private fun buildScanSettings(): ScanSettings {
        val builder =  ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //高功耗

        if(android.os.Build.VERSION.SDK_INT >= 23){
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY)
        }

        return builder.build()
    }

    private fun buildScanCallBack(): ScanCallback {
        return object: ScanCallback(){

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                result?.let {
                    val unprovisionDevice = ParseUnprovision(result)
                    addNewDevice(unprovisionDevice)
                }

            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d(TAG, " onScanFailed errorCode ： $errorCode ")
            }

        }
    }

    private fun addNewDevice(advertingDevice: AdvertingDevice){
        val devices = arrayListOf<AdvertingDevice>()
        devices.addAll(deviceListLiveData.value ?: emptyList())
        val index = devices.indexOf(advertingDevice)
        if(index < 0){
            // add
            devices.add(advertingDevice)
        }else{
            //update
            devices.remove(advertingDevice)
            devices.add(index, advertingDevice)
        }

        val realDevices = devices.filter { device ->
            var flag = true
            when(displayMode){
                ALL_DISPLAY -> flag = true
                DISPLAY_ALL_MESH -> flag = device.isMeshDevice
                JUST_DISPLAY_UNPROVISION -> flag = device.isMeshDevice && device.isUnprovisionDevice
                JUST_DISPLAY_BIND -> flag = device.isMeshDevice && !device.isUnprovisionDevice
            }
            flag
        }

        deviceListLiveData.value = realDevices
    }

    fun refreshDeviceList(){
        loadDevices()
    }

    private fun loadDevices(){
//        GlobalScope.launch(Dispatchers.IO){
//            val resource =  Iot.apiEntry.get<DeviceManager>()?.loadDevices(null) ?: Resource.error(StatusCodes.UnknownError())
//
//            if(resource.success && !resource.data.isNullOrEmpty()){
//                GlobalScope.launch(Dispatchers.Main){
//                    deviceListLiveData.value = resource.data
//                }
//            }
//
//        }
    }

    fun removeDevice(deviceId: String){
        //没有实现删除设备后 ，刷新数据和界面
//        val removeDevice = deviceListLiveData.value?.find { device -> device.deviceId == deviceId }
//        logd(TAG," removeDevice : ${Gson().toJson(removeDevice)}")
//        val devices = arrayListOf<Device>()
//        devices.addAll(deviceListLiveData.value!!)
//        devices.remove(removeDevice)
//        GlobalScope.launch(Dispatchers.Main){
//            deviceListLiveData.value = devices
//        }
    }



}