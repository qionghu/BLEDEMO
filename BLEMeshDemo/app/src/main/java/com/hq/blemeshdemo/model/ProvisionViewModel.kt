package com.hq.blemeshdemo.model

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.hq.blemeshdemo.Utils.PDUType
import com.hq.blemeshdemo.Utils.UUID
import com.hq.blemeshdemo.Utils.bytesToHexString
import com.hq.blemeshdemo.bean.ProvisionResult
import com.hq.blemeshdemo.bean.ProvisionStatus

class ProvisionViewModel  : ViewModel() {

    private val TAG = "ProvisionViewModel"
    private var gatt: BluetoothGatt? = null

    private val attention_time: Byte = 0x00.toByte()

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val provisionResultLiveData: MutableLiveData<ProvisionResult> = MutableLiveData<ProvisionResult>(
        ProvisionResult(ProvisionStatus.None)
    )

    init {


    }

    fun connectDevice(context: Context, device: BluetoothDevice?){
        if(device == null) return
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private fun readServiceInfo(service : BluetoothGattService){

        val characteristics = service.characteristics

        characteristics.forEach {

            Log.d(TAG, " characteristic uuid : ${it.uuid} -- value : ${it.value}")

            val descriptors = it.descriptors
            descriptors.forEach {
                Log.d(TAG, "  -- descriptor uuid : ${it.uuid}")
            }

            //Log.d(TAG, " characteristic : ${Gson().toJson(it)}")

        }

    }

    private fun startProvision(provisionService: BluetoothGattService){

        val characteristrics = provisionService.characteristics

        for(characteristric in characteristrics){
            if(characteristric.uuid.toString().equals(UUID.PROVISION_OUT_CHARACTERISTIC_UUID)){
                setCharacteristicsNotification(characteristric)
            }
        }

        handler.postDelayed({
            provisionInvite()
        }, 5*1000L)

    }

    private fun setCharacteristicsNotification(characterictic: BluetoothGattCharacteristic){

        if(gatt == null) return

        val enableNotification = gatt?.setCharacteristicNotification(characterictic, true) ?: false
        if(enableNotification){
            val descriptors = characterictic.descriptors

            for(descriptor in descriptors){
                if(descriptor.uuid.toString().equals(UUID.CONFIG_DESCRIPTOR_UUID)){
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    Log.d(TAG, " writeDescriptor uuid : ${descriptor.uuid}")
                    gatt?.writeDescriptor(descriptor)
                }
            }
        }
    }

    private fun provisionInvite(){
        val data = ByteArray(2)
        data[0] = ProvisionStatus.Invite.status.toByte()
        data[1] = attention_time

        Log.d(TAG, " provisionInvite data:  ${bytesToHexString(data, ":")}")
        sendProvisionData(data)
    }

    private fun sendProvisionData(data: ByteArray){
        if(gatt == null) return

        val realData = ByteArray(data.size + 1)
        realData[0] = PDUType.PROVISIONING_PDU.toByte()
        System.arraycopy(data, 0, realData, 1, data.size)

        val provsionService = gatt?.getService(java.util.UUID.fromString(UUID.PROVISION_SERVICE_UUID))
        val provision_in_characterictic = provsionService?.getCharacteristic(java.util.UUID.fromString(UUID.PROVISION_IN_CHARACTERISTRIC_UUID))
        if(provision_in_characterictic == null){
            Log.d(TAG, " PROVISION_IN_CHARACTERISTRIC_UUID not found ")
            return
        }

        Log.d(TAG, " sendProvisionData  realData : ${bytesToHexString(realData, ":")}")
        provision_in_characterictic.setValue(realData)
        provision_in_characterictic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt?.writeCharacteristic(provision_in_characterictic)

    }

    private val gattCallback: BluetoothGattCallback = object: BluetoothGattCallback(){


        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, " onConnectionStateChange status :  $status -- newState : $newState  ")
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, " gatt connect success !")
                gatt?.discoverServices()
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, " onServicesDiscovered status :  $status ")
            if(status == BluetoothGatt.GATT_SUCCESS ){

                gatt?.services?.forEach {
                    Log.d(TAG, " support service uuid :  ${it.uuid.toString()}")

//                    if(it.uuid.toString().equals(UUID.DEVICE_INFO_UUID)) readServiceInfo(it)
                    if(it.uuid.toString().equals(UUID.PROVISION_SERVICE_UUID)){
                        readServiceInfo(it)
                        startProvision(it)
                    }
                }

            }

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.d(TAG, " onCharacteristicRead status :  $status -- uuid : ${characteristic?.uuid}  -- value : ${bytesToHexString(characteristic?.value, ":")}")
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.d(TAG, " onCharacteristicWrite status :  $status -- uuid : ${characteristic?.uuid}  -- value : ${bytesToHexString(characteristic?.value, ":")}")
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.d(TAG, " onCharacteristicChanged  -- uuid : ${characteristic?.uuid}  -- value : ${bytesToHexString(characteristic?.value, ":")}")
            super.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(TAG, " onDescriptorRead status :  $status -- uuid : ${descriptor?.uuid}  -- value : ${bytesToHexString(descriptor?.value, ":")}")
            super.onDescriptorRead(gatt, descriptor, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(TAG, " onDescriptorWrite status :  $status -- uuid : ${descriptor?.uuid}  -- value : ${bytesToHexString(descriptor?.value, ":")}")
            super.onDescriptorWrite(gatt, descriptor, status)
        }



    }

}