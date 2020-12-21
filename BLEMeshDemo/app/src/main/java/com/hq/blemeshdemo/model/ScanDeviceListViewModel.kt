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
import java.lang.Exception

class ScanDeviceListViewModel : ViewModel() {
    val TAG = "ScanDeviceListViewModel"

    private val OPEN_BLUETOOTH_REQUEST_CODE = 1001

    companion object{
        val ALL_DISPLAY = 0x0001
        val DISPLAY_ALL_MESH = 0x0010
        val JUST_DISPLAY_UNPROVISION = 0x0011
        val JUST_DISPLAY_BIND = 0x0100
    }

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
        if(!dataMap.contains(advertingDevice.mac)){
            var flag = true
            when(displayMode){
                ALL_DISPLAY -> flag = true
                DISPLAY_ALL_MESH -> flag = advertingDevice.isMeshDevice
                JUST_DISPLAY_UNPROVISION -> flag = advertingDevice.isMeshDevice && advertingDevice.isUnprovisionDevice
                JUST_DISPLAY_BIND -> flag = advertingDevice.isMeshDevice && !advertingDevice.isUnprovisionDevice
            }

            if(flag){
                val devices = arrayListOf<AdvertingDevice>()
                devices.addAll(deviceListLiveData.value ?: emptyList())
                devices.add(advertingDevice)
                deviceListLiveData.value = devices
            }
        }
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