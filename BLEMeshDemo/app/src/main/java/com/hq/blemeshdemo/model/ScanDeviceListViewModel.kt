package com.hq.blemeshdemo.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.hq.blemeshdemo.bean.UnprovisionDevice
import com.hq.blemeshdemo.parser.ParseUnprovision
import java.lang.Exception

class ScanDeviceListViewModel : ViewModel() {
    val TAG = "ScanDeviceListViewModel"

    companion object{
        val SCAN_STATUS_SUCCESS = 0
        val SCAN_STATUS_ADAPTER_DISABLE = 1
        val SCAN_STATUS_API_LOW = 2
        val SCAN_STATUS_SCANNING = 3
        val SCAN_STATUS_UNKOWN_ERROR = 4
    }



    private var bluetoothAdapter: BluetoothAdapter? = null

    private var mScanning = false
    private var scanner: BluetoothLeScanner? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val scanSetting: ScanSettings by lazy { buildScanSettings() }
    private val mScanCallback: ScanCallback by lazy { buildScanCallBack() }


    val deviceListLiveData: MutableLiveData<List<UnprovisionDevice>> by lazy {
        MutableLiveData<List<UnprovisionDevice>>()
    }

    private val dataMap: HashSet<String> = HashSet()

    init {

        deviceListLiveData.observeForever {
            it.forEach {
                if (!dataMap.contains(it.mac)) {
                    dataMap.add(it.mac)
                }
            }
        }



        initBlueTooth()

//        loadDevices()

//        Iot.apiEntry.get<DeviceStateManager>()?.addStateChangeListener {
//            val newDeviceMap: HashMap<String, Device> = hashMapOf()
//            it.forEach {
//                Log.d(TAG, " stateChange deviceId : ${it.deviceId} -- activeFlag : ${it.deviceState?.activeFlags}")
//                newDeviceMap.set(it.deviceId, it)
//            }
//
//            val devices = arrayListOf<Device>()
//            devices.addAll(deviceListLiveData.value!!)
//            devices.forEach {
//                val newDevice = newDeviceMap[it.deviceId]
//
//                newDevice?.let {
//                    it.deviceState = newDevice.deviceState
//                }
//            }
//
//            GlobalScope.launch(Dispatchers.Main){
//                deviceListLiveData.value = devices
//            }
//        }

    }

    fun initBlueTooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }


    fun startScan(): Int {

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return SCAN_STATUS_ADAPTER_DISABLE

        //TODO 暂时只适配5.0以上的
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            if (mScanning) {
                Log.d(TAG, "正在扫描蓝牙中...!")
                return SCAN_STATUS_SCANNING
            } else {

                if (scanner == null) {
                    scanner = bluetoothAdapter?.bluetoothLeScanner
                }
                scanner?.let {
                    Log.d(TAG, " start scan!")
                    try {
                        it.startScan(null, scanSetting, mScanCallback)
                        mScanning = true
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                    startScanTimer()
                }

                if (mScanning) {
                    return SCAN_STATUS_SUCCESS
                } else {
                    return SCAN_STATUS_UNKOWN_ERROR
                }
            }
        } else {
            Log.d(TAG, "版本低于5.0暂时未适配!")
            return SCAN_STATUS_API_LOW
        }

    }

    fun stopScan(){
        if(mScanning) scanner?.stopScan(mScanCallback)
    }

    private fun startScanTimer() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                if (mScanning) {
                    scanner?.let {
                        mScanning = false
                        it.stopScan(mScanCallback)
                        Log.d(TAG, "停止扫描!")
                    }
                }
            } else {
                Log.d(TAG, "版本低于5.0暂时未适配!")
            }
        }, 30 * 1000)
    }

    private fun buildScanSettings(): ScanSettings {
        val builder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //高功耗

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY)
        }

        return builder.build()
    }

    private fun buildScanCallBack(): ScanCallback {
        return object : ScanCallback() {

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

    private fun addNewDevice(unprovisionDevice: UnprovisionDevice) {
        if (!dataMap.contains(unprovisionDevice.mac)) {
            val devices = arrayListOf<UnprovisionDevice>()
            devices.addAll(deviceListLiveData.value ?: emptyList())
            devices.add(unprovisionDevice)
            deviceListLiveData.value = devices
        }
    }

    fun refreshDeviceList() {
        loadDevices()
    }

    private fun loadDevices() {
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

    fun removeDevice(deviceId: String) {
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

