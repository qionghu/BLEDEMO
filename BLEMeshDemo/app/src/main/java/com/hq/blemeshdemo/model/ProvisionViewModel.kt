package com.hq.blemeshdemo.model

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hq.blemeshdemo.Utils.*
import com.hq.blemeshdemo.bean.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.macs.CMac
import org.spongycastle.crypto.modes.CCMBlockCipher
import org.spongycastle.crypto.params.AEADParameters
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.spongycastle.util.Arrays
import org.spongycastle.util.BigIntegers
import org.spongycastle.util.Strings
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.KeyAgreement

/**
 * TODO 用状态模式重写本类
 */
class ProvisionViewModel  : ViewModel() {

    private val TAG = "ProvisionViewModel"

    private val ZERO_16BIT = ByteArray(16){
        0x00.toByte()
    }

    private var gatt: BluetoothGatt? = null

    private val attention_time: Byte = 0x00.toByte()

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val resultList: ArrayList<ProvisionResult> = ArrayList()

    private var reconnectCount = 0
    private var deviceTemp : BluetoothDevice? = null
    private var weakRef: WeakReference<Context>? = null

    private var provisionerKeyPair: KeyPair? = null
    private var ecdhSecret: ByteArray? = null
    private val provisionRandom: ByteArray = ByteArray(16)
    private var confirmationKey: ByteArray? = null

    private var receConfimation: ByteArray? = null
    private var receRandom: ByteArray? = null

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

            if(provisionResult.bean == null){
                Log.d(TAG, "handleNext status： ${provisionResult.status.name}  bean is null !")
                return@launch
            }

            val data = provisionResult.bean.toBytes()
            Log.d(TAG, "handleNext status： ${provisionResult.status.name} bean : ${bytesToHexString(data, ":")}")

            when(provisionResult.status){
                ProvisionStatus.Capabilities -> {
                    provisionStart()

                    delay(100)

                    provisionSendPublicKey()
                }
                ProvisionStatus.RecePublicKey -> {
                    if(provisionResult.bean is ProvisionPublicKeyBean){
                        Log.d(TAG, " receive public key bean keyX : ${bytesToHexString(provisionResult.bean.keyX, ":")}")
                        Log.d(TAG, " receive public key bean keyY : ${bytesToHexString(provisionResult.bean.keyY, ":")}")
                    }

                    ecdhSecret = getECDH(provisionResult.bean.toBytes())

                    provisionSendConfirmation()
                }
                ProvisionStatus.ReceConfirmation -> {
                    receConfimation = data
                    provisionSendRandom()
                }

                ProvisionStatus.ReceRandom -> {
                    receRandom = data
                    val flag = checkConfirmation()
                    Log.d(TAG, "handleNext checkConfirmation $flag !")
                    if(flag){
                        provisionSendProvisioningData()
                    }
                }

                ProvisionStatus.Complete -> {
                    val proxyService = gatt?.getService(UUID.PROXY_SERVICE_UUID)
                    proxyService?.let {
                        proxyServiceInit(proxyService)

                        handler.postDelayed({

                        }, 5*1000L)
                    }
                }

                else -> {}
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

    private var isUnprovisionDevice: Boolean = false
    fun setProvisionStatus(isUnprovisionDevice: Boolean){
        this.isUnprovisionDevice = isUnprovisionDevice
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
        provisionServiceInit(provisionService)

        clearCache()

        handler.postDelayed({
            provisionInvite()
        }, 5*1000L)

    }

    private fun provisionServiceInit(provisionService: BluetoothGattService){
        val characteristrics = provisionService.characteristics

        for(characteristric in characteristrics){
            if(characteristric.uuid.equals(UUID.PROVISION_OUT_CHARACTERISTIC_UUID)){
                setCharacteristicsNotification(characteristric)
            }
        }
    }

    private fun proxyServiceInit(proxyService: BluetoothGattService){
        val characteristrics = proxyService.characteristics

        for(characteristric in characteristrics){
            if(characteristric.uuid.equals(UUID.PROXY_OUT_CHARACTERISTIC_UUID)){
                setCharacteristicsNotification(characteristric)
            }
        }
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
        provisionerKeyPair = getPublicKey()
        provisionerKeyPair?.let {
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
        val confirmation = getConfirmation()
        confirmation?.let {
            val bean = ProvisionConfirmationBean(it)
            val data = buildProvisionData(ProvisionStatus.Confirmation, bean)
            sendProvisionData(data)
        } ?: run {
            Log.d(TAG, "provisionSendConfirmation confirmation is null !")
        }
    }

    private fun getConfirmation(): ByteArray?{
        confirmationKey = getConfirmationKey() ?: return null
        buildProvisionRandom()
        val confirmRawData = getConfirmRawData(provisionRandom)

        return aceCmac(confirmRawData, confirmationKey!!)
    }

    private fun getConfirmationKey(): ByteArray?{
        val salt = getConfirmationSalt() ?: return null
        val confirmationKey = k1(ecdhSecret!!, salt)
        return confirmationKey
    }

    private fun getConfirmationSalt(): ByteArray?{
        val confirmInput = confirmAssembly()
        if(confirmInput == null || ecdhSecret == null) return null

        return s1(confirmInput)
    }


    private fun buildProvisionRandom(){
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(provisionRandom)
    }

    private fun getConfirmRawData(random : ByteArray): ByteArray{
        val authValue = ZERO_16BIT
        val size = random.size + authValue.size
        val confirmRawData = ByteArray(size)
        // random + authValue
        System.arraycopy(random, 0 , confirmRawData, 0, random.size)
        System.arraycopy(authValue, 0 , confirmRawData, random.size, authValue.size)

        return confirmRawData
    }

    private fun confirmAssembly(): ByteArray?{
        var inviteByteArray: ByteArray? = null
        var capabilitiesByteArray: ByteArray? = null
        var startByteArray: ByteArray? = null
        var provisionerPublickeyByteArray : ByteArray? = null
        var recePublicKeyByteArray: ByteArray? = null

        for(bean in resultList){
            when(bean.status){
                ProvisionStatus.Invite -> {
                    inviteByteArray = bean.bean?.toBytes()
                }

                ProvisionStatus.Capabilities -> {
                    capabilitiesByteArray = bean.bean?.toBytes()
                }

                ProvisionStatus.Start -> {
                    startByteArray = bean.bean?.toBytes()
                }

                ProvisionStatus.PublicKey -> {
                    provisionerPublickeyByteArray = bean.bean?.toBytes()
                }

                ProvisionStatus.RecePublicKey -> {
                    recePublicKeyByteArray = bean.bean?.toBytes()
                }
                else -> {}
            }
        }

        if(inviteByteArray == null
                || capabilitiesByteArray == null
                || startByteArray == null
                || provisionerPublickeyByteArray == null
                || recePublicKeyByteArray == null){
            return null
        }

        val len = inviteByteArray.size + capabilitiesByteArray.size + startByteArray.size + provisionerPublickeyByteArray.size + recePublicKeyByteArray.size

        val byteBuffer = ByteBuffer.allocate(len)
        byteBuffer.put(inviteByteArray)
                .put(capabilitiesByteArray)
                .put(startByteArray)
                .put(provisionerPublickeyByteArray)
                .put(recePublicKeyByteArray)

        return byteBuffer.array()
    }

    private fun s1(data: ByteArray): ByteArray{
        val ZERO_KEY = ZERO_16BIT
        return aceCmac(data, ZERO_KEY)
    }

    private fun k1(N: ByteArray, salt:ByteArray, text: String = "prck"): ByteArray{
        val T = aceCmac(N, salt)
        return aceCmac(text.toByteArray(), T)
    }

    private fun aceCmac(data: ByteArray, key: ByteArray): ByteArray{
        val keySpec = KeyParameter(key)
        val aesEngine = AESEngine()
        val cmac = CMac(aesEngine)

        cmac.init(keySpec)
        cmac.update(data, 0, data.size)

        val re = ByteArray(16)
        cmac.doFinal(re, 0)
        return re
    }

    private fun provisionSendRandom(){
        val bean = ProvisionRandomBean(provisionRandom)
        val data = buildProvisionData(ProvisionStatus.Random, bean)
        sendProvisionData(data)
    }

    private fun checkConfirmation(): Boolean{
        if(confirmationKey == null || receRandom == null || receConfimation == null){
            Log.d(TAG, "checkConfirmation confirmationKey : $confirmationKey -- receRandom : $receRandom -- receConfimation : $receConfimation ")
            return false
        }

        val tempReceConfirmation = getConfirmRawData(receRandom!!)
        val calculateConfirmation = aceCmac(tempReceConfirmation, confirmationKey!!)
        return Arrays.areEqual(receConfimation!!, calculateConfirmation)
    }

    /**
     *  networkKey=46EC1BD27B492F491659D937B7C18115,
     *  networkKeyIndex=0,
     *  ivIndex=0x0,
     *  keyRefreshFlag=0,  ivUpdateFlag=0,
     *  unicastAddress=0x3ea
     *  deviceKey=8B5D4E1DB3526736C78E2600C9212779}
     *
     *  ProvisioningSalt = s1(ConfirmationSalt || RandomProvisioner || RandomDevice)
     *  SessionKey = k1(ECDHSecret, ProvisioningSalt, “prsk”)
     *  The nonce shall be the 13 least significant octets of:
     *  SessionNonce = k1(ECDHSecret, ProvisioningSalt, “prsn”)
     *  The provisioning data shall be encrypted and authenticated using:
     *  Provisioning Data = Network Key || Key Index || Flags || IV Index || Unicast Address
     *  Encrypted Provisioning Data, Provisioning Data MIC = AES-CCM (SessionKey, SessionNonce, Provisioning Data)
     *
     */
    private fun provisionSendProvisioningData(){
        val provisioningSalt = getProvisioningSalt()
        if(provisioningSalt == null || ecdhSecret == null ){
            Log.d(TAG, "provisionSendProvisioningData provisioningSalt : $provisioningSalt --   ecdhSecret : $ecdhSecret ")
            return
        }

        val sessionKey  = k1(ecdhSecret!!, provisioningSalt, "prsk")
        val tempNonce = k1(ecdhSecret!!, provisioningSalt, "prsn")
        val sessionNonce = ByteArray(13)
        System.arraycopy(tempNonce, 3, sessionNonce, 0 , 13)

        val rawProvisioningData = getRawProvisioningData()

        val rawData = aesCcm(rawProvisioningData, sessionKey, sessionNonce, 8, true)
        rawData?.let {
            val bean = ProvisioningDataBean(it)
            val data = buildProvisionData(ProvisionStatus.Data, bean)
            sendProvisionData(data)
        }
    }

    fun aesCcm(data: ByteArray, key: ByteArray?, nonce: ByteArray?, micSize: Int, encrypt: Boolean): ByteArray? {
        val result = ByteArray(data.size + if (encrypt) micSize else -micSize)
        val ccmBlockCipher = CCMBlockCipher(AESEngine())
        val aeadParameters = AEADParameters(KeyParameter(key), micSize * 8, nonce)
        ccmBlockCipher.init(encrypt, aeadParameters)
        ccmBlockCipher.processBytes(data, 0, data.size, result, data.size)
        return try {
            ccmBlockCipher.doFinal(result, 0)
            result
        } catch (e: InvalidCipherTextException) {
            null
        }
    }


    private fun getProvisioningSalt(): ByteArray?{
        val confirmationSalt = getConfirmationSalt()

        if(confirmationSalt == null || provisionRandom.isEmpty() || receRandom == null){
            Log.d(TAG, "getSessionKey confirmationSalt : $confirmationSalt -- provisionRandom : $provisionRandom -- receRandom : $receRandom")
            return null
        }

        val saltSize = confirmationSalt.size
        val buffer = ByteBuffer.allocate(saltSize + provisionRandom.size + receRandom!!.size)
        buffer.put(confirmationSalt).put(provisionRandom).put(receRandom)
        return s1(buffer.array())
    }

    private fun getRawProvisioningData(): ByteArray{
        val networkKey = getNetworkKey()
        //Log.d(TAG, "getRawProvisioningData networkKey : ${bytesToHexString(networkKey, ":")} ")
        val buffer = ByteBuffer.allocate(25)
        buffer.put(networkKey).put(netKeyIndex).put(flags).put(ivIndex).put(unicastAddress)
        return buffer.array()
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

    private fun updateProvisionStatus(provisionStatus: ProvisionStatus, bean: ProvisionBean? = null ){
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

    private fun sendProxyData(data: ByteArray){
        if(gatt == null) return

        val proxyService = gatt?.getService(UUID.PROXY_SERVICE_UUID)
        val proxy_in_characterictic = proxyService?.getCharacteristic(UUID.PROXY_IN_CHARACTERISTIC_UUID)
        if(proxy_in_characterictic == null){
            Log.d(TAG, " PROXY_IN_CHARACTERISTIC_UUID not found ")
            return
        }

        Log.d(TAG, " sendProxyData  data : ${bytesToHexString(data, ":")}")
        proxy_in_characterictic.setValue(data)
        proxy_in_characterictic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt?.writeCharacteristic(proxy_in_characterictic)
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
                updateProvisionStatus(ProvisionStatus.Capabilities, ProvisionCapabilitiesBean.parseData(realData))
            }
            ProvisionStatus.PublicKey.status -> {
                updateProvisionStatus(ProvisionStatus.RecePublicKey, ProvisionPublicKeyBean.parseData(realData))
            }
            ProvisionStatus.Confirmation.status -> {
                updateProvisionStatus(ProvisionStatus.ReceConfirmation, ProvisionConfirmationBean(realData))
            }
            ProvisionStatus.Random.status -> {
                updateProvisionStatus(ProvisionStatus.ReceRandom, ProvisionRandomBean(realData))
            }
            ProvisionStatus.Complete.status -> {
                updateProvisionStatus(ProvisionStatus.Complete)
            }
            ProvisionStatus.Failed.status -> {
                updateProvisionStatus(ProvisionStatus.Failed)
            }
        }
    }

    private fun getECDH(recePublicKeyData: ByteArray) : ByteArray?{
        if(provisionerKeyPair == null) return null

        val xPoint: BigInteger = BigIntegers.fromUnsignedByteArray(recePublicKeyData, 0 , 32)
        val yPoint: BigInteger = BigIntegers.fromUnsignedByteArray(recePublicKeyData, 32 , 32)

        val paramSpec   = ECNamedCurveTable.getParameterSpec("P-256")
        val point   = paramSpec.curve.validatePoint(xPoint, yPoint)

        val publicKeySpec = ECPublicKeySpec(point, paramSpec)
        var keyFactory: KeyFactory? = null
        try {
            keyFactory = KeyFactory.getInstance("ECDH", "SC")
            val recePublicKey = keyFactory.generatePublic(publicKeySpec)

            val keyAgreement = KeyAgreement.getInstance("ECDH", "SC")
            keyAgreement.init(provisionerKeyPair!!.private)
            keyAgreement.doPhase(recePublicKey, true)

            return keyAgreement.generateSecret()
        }catch (e: Exception){
            e.printStackTrace()
            return null
        }
    }

    fun requestMTU(){
        GlobalScope.launch(Dispatchers.IO){
            delay(2000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt?.requestMtu(517);
            }
        }

    }

    

    private fun sendCompositionDataGet(){


    }

    /**
     * 查询model绑定appkey的情况
     */
    private fun getAppKeyBindStatus(){

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

                        if(isUnprovisionDevice){
                            startProvision(it)
                        }else{
                            sendCompositionDataGet()
                        }

                    }
                }

                if(status == BluetoothGatt.GATT_SUCCESS){
                    requestMTU()
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