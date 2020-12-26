package com.hq.blemeshdemo.model

import android.bluetooth.*
import android.content.Context
import android.os.Build
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
import com.hq.blemeshdemo.Utils.initSecurity
import com.hq.blemeshdemo.bean.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.spongycastle.jce.ECNamedCurveTable
import java.lang.Exception
import java.lang.ref.WeakReference
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import kotlin.experimental.and
import kotlin.experimental.or

class ProvisionViewModel  : ViewModel() {

    private val TAG = "ProvisionViewModel"
    private var gatt: BluetoothGatt? = null

    private val attention_time: Byte = 0x00.toByte()

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val resultList: ArrayList<ProvisionResult> = ArrayList()

    private var reconnectCount = 0
    private var deviceTemp : BluetoothDevice? = null
    private var weakRef: WeakReference<Context>? = null

    private val provisionResultLiveData: MutableLiveData<ProvisionResult> = MutableLiveData<ProvisionResult>(
        ProvisionResult(ProvisionStatus.None)
    )

    init {
        initSecurity()

        provisionResultLiveData.observeForever {
            handleNext(it)
            resultList.add(it)
        }

    }

    private fun handleNext(provisionResult: ProvisionResult){
        GlobalScope.launch(Dispatchers.IO){
            delay(50)

            when(provisionResult.status){
                ProvisionStatus.Capabilities -> {
                    provisionStart()

                    delay(100)

                    provisionSendPublicKey()
                }

            }

        }
    }

    fun close(){
        gatt?.disconnect()
        clearCache()
    }

    private fun clearCache(){
        resultList.clear()
    }

    fun connectDevice(context: Context, device: BluetoothDevice?){
        if(device == null) return
        deviceTemp = device
        weakRef = WeakReference(context)

        Log.d(TAG, " connectDevice device mac : ${device.address}")
        GlobalScope.launch(Dispatchers.IO){
            delay(1000)

            gatt = device.connectGatt(context, false, gattCallback)
        }
    }

    fun reconnectDevice(){
        if(reconnectCount < 3 ){
            reconnectCount += 1
            this.weakRef?.get()?.let {
                Log.d(TAG, " reconnectDevice reconnectCount : $reconnectCount")
                connectDevice(it, this.deviceTemp)
            }
        }
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
            if(characteristric.uuid.equals(UUID.PROVISION_OUT_CHARACTERISTIC_UUID)){
                setCharacteristicsNotification(characteristric)
            }
        }

        clearCache()

        handler.postDelayed({
            provisionInvite()
        }, 5*1000L)

    }

    private fun setCharacteristicsNotification(characterictic: BluetoothGattCharacteristic){

        if(gatt == null) return

        Log.d(TAG, " setCharacteristicNotification characterictic uuid : ${characterictic.uuid}")
        val enableNotification = gatt?.setCharacteristicNotification(characterictic, true) ?: false
        if(enableNotification){
            val descriptors = characterictic.descriptors

            for(descriptor in descriptors){
                if(descriptor.uuid.equals(UUID.CONFIG_DESCRIPTOR_UUID)){
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    Log.d(TAG, " writeDescriptor uuid : ${descriptor.uuid}")
                    gatt?.writeDescriptor(descriptor)
                }
            }
        }
    }

    private fun provisionInvite(){

        val bean = ProvisionInvitBean(attention_time)
        val data = buildProvisionData(ProvisionStatus.Invite, bean)

        sendProvisionData(data)
    }

    private fun provisionStart(){

        val bean = ProvisionStartBean(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val data = buildProvisionData(ProvisionStatus.Start, bean)

        sendProvisionData(data)
    }

    private fun provisionSendPublicKey(){
        val publicKey = getPublicKey()
        publicKey?.let {
            val publicKey = it.public as BCECPublicKey

            val bean = ProvisionPublicKeyBean(publicKey.q.xCoord.encoded, publicKey.q.yCoord.encoded)
            val data = buildProvisionData(ProvisionStatus.PublicKey, bean)

            sendProvisionData(data)
        }

    }

    private fun getPublicKey(): KeyPair? {
        return try {
            val paramSpec = ECNamedCurveTable.getParameterSpec("P-256")
            val keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "SC")
            keyPairGenerator.initialize(paramSpec)
            keyPairGenerator.generateKeyPair()
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    private fun provisionSendConfirmation(){

    }

    private fun provisionSendRandom(){

    }

    private fun provisionSendProvisioningData(){

    }

    private fun buildProvisionData(provisionStatus: ProvisionStatus, bean: ProvisionBean): ByteArray{
        updateProvisionStatus(provisionStatus, bean)

        val beanData = bean.toBytes()
        val data = ByteArray(beanData.size + 2)
        data[0] = PDUType.PROVISIONING_PDU.toByte()
        data[1] = provisionStatus.status.toByte()
        System.arraycopy(beanData, 0 , data, 2, beanData.size)
        return data
    }

    private fun updateProvisionStatus(provisionStatus: ProvisionStatus, bean: ProvisionBean){
        GlobalScope.launch(Dispatchers.Main){
            val provisionResult = ProvisionResult(provisionStatus, bean)
            provisionResultLiveData.value = provisionResult
            Log.d(TAG, " updateProvisionStatus status :  ${provisionStatus.name}")
        }
    }

    private fun sendProvisionData(data: ByteArray){
        if(gatt == null) return

        val provsionService = gatt?.getService(UUID.PROVISION_SERVICE_UUID)
        val provision_in_characterictic = provsionService?.getCharacteristic(UUID.PROVISION_IN_CHARACTERISTRIC_UUID)
        if(provision_in_characterictic == null){
            Log.d(TAG, " PROVISION_IN_CHARACTERISTRIC_UUID not found ")
            return
        }

        Log.d(TAG, " sendProvisionData  data : ${bytesToHexString(data, ":")}")
        provision_in_characterictic.setValue(data)
        provision_in_characterictic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt?.writeCharacteristic(provision_in_characterictic)

    }


    private fun handleNotify(characteristic: BluetoothGattCharacteristic){
        val uuid = characteristic.uuid
        val data: ByteArray = characteristic.value

        //Provisioning过程中，设备回复
        if(uuid.equals(UUID.PROVISION_OUT_CHARACTERISTIC_UUID)){
            Log.d(TAG, " handleNotify PROVISION_OUT uuid : $uuid data : ${bytesToHexString(data, ":")}")
            parseProvisionPDU(data)
        }

    }

    private fun parseProvisionPDU(data: ByteArray){
        if(data[0] != PDUType.PROVISIONING_PDU.toByte()) return

        val realData = ByteArray(data.size - 2)
        System.arraycopy(data, 2, realData, 0, realData.size)
        when(data[1].toInt().and(0xFF)){
            ProvisionStatus.Capabilities.status -> {
                val provisionCapabilitiesBean = ProvisionCapabilitiesBean.parseData(realData)
                Log.d(TAG, " provisionCapabilitiesBean : ${provisionCapabilitiesBean.toString()}")
                provisionCapabilitiesBean?.let {
                    updateProvisionStatus(ProvisionStatus.Capabilities, it)
                }
            }
            ProvisionStatus.PublicKey.status -> {
                val receiveBean = ProvisionPublicKeyBean.parseData(realData)
                receiveBean?.let {
                    Log.d(TAG, " receive public key bean keyX : ${bytesToHexString(receiveBean.keyX, ":")}")
                    Log.d(TAG, " receive public key bean keyY : ${bytesToHexString(receiveBean.keyY, ":")}")
                }
            }

        }
    }



    private val gattCallback: BluetoothGattCallback = object: BluetoothGattCallback(){

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, " onConnectionStateChange status :  $status -- newState : $newState  ")
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, " gatt connect success !")
                gatt?.discoverServices()
            }else if (status == 133){
                reconnectDevice()
            }

        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d(TAG, " onMtuChanged status :  $status  -- mtu: $mtu ")
            super.onMtuChanged(gatt, mtu, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, " onServicesDiscovered status :  $status ")
            if(status == BluetoothGatt.GATT_SUCCESS ){

                gatt?.services?.forEach {
                    Log.d(TAG, " support service uuid :  ${it.uuid}")

//                    if(it.uuid.toString().equals(UUID.DEVICE_INFO_UUID)) readServiceInfo(it)
                    if(it.uuid.equals(UUID.PROVISION_SERVICE_UUID)){
                        readServiceInfo(it)
                        startProvision(it)
                    }
                }

                if(status == BluetoothGatt.GATT_SUCCESS){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        gatt?.requestMtu(517);
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
            characteristic?.let {
                handleNotify(characteristic)
            }
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