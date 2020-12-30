package com.hq.blemeshdemo.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.hq.blemeshdemo.R
import com.hq.blemeshdemo.Utils.bytesToHexString
import com.hq.blemeshdemo.bean.AdvertingDevice
import kotlinx.android.synthetic.main.unprovision_device_item.view.*

//private val dataList: ArrayList<Device>,
class ScanDeviceListAdapter(val fragmentActivity: FragmentActivity, liveData: LiveData<List<AdvertingDevice>>) : RecyclerView.Adapter<ScanDeviceListAdapter.DeviceViewHolder>() {

    private val TAG = "ScanDeviceListAdapter"
    val dataList: ArrayList<AdvertingDevice> = arrayListOf()

    init {
        val listObserver = Observer<List<AdvertingDevice>>{
            dataList.clear()
            dataList.addAll(it)
            sortList()
        }

        liveData.observe(fragmentActivity, listObserver)
    }

    class DeviceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.unprovision_device_item, parent, false)

        return DeviceViewHolder(itemView = itemView)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
//        holder.itemView.card_view
        val device = dataList[position]
        val typeStr = bytesToHexString(byteArrayOf(device.type), ":")
        val beaconStr = bytesToHexString(byteArrayOf(device.beaconType), ":")
        val oobStr = bytesToHexString(device.oobInfo, ":")
        val uriHash = bytesToHexString(device.uriHash, ":")
        val recordStr = bytesToHexString(device.scanRecord, ":")
        val bindStatus = if(device.isUnprovisionDevice) "unprovisioned" else "binded"
        val str = "address : ${device.mac} --  bind Status :  $bindStatus   \n typeStr : $typeStr -- beaconStr : $beaconStr  -- oobStr : $oobStr  -- uriHash : $uriHash  -- uuid : ${device.uuid} "
        holder.itemView.title.text = str
        holder.itemView.setOnClickListener {
            this.onItemClickListener?.invoke(device)
        }


//        val typeStr = " ${device.category } -- ${device.type}"
//        val stateStr = device.deviceState?.let {
//             " activeFlag : ${it.activeFlags} ,  OnOff : ${it.on} \n " +
//                     " brightness : ${it.brightness} , temperature : ${it.colorTemperature} \n " +
//                     " colorHue : ${it.colorHue} , colorSat : ${it.colorSat}"
//        } ?: run {
//            " null "
//        }
//
//        val str = " type: $typeStr , networkFlag : ${device.networkFlags} , " +
//                " version : ${device.swVersion} , wifiSsid : ${device.wifiSsid} , meshAddress: ${device.meshAddress}" +
//                " \n  state : $stateStr "
//
//        holder.itemView.content.text = str
//
//        val activeFlag = device.deviceState?.activeFlags?.toInt() ?: 0;
//        val online = activeFlag > 0
//        val OnOff = device.deviceState?.on ?: false
//        holder.itemView.OnOff.isChecked =  OnOff
//
//        if(online){
//            holder.itemView.card_view.setBackgroundResource(R.color.grey_50)
//        }else{
//            holder.itemView.card_view.setBackgroundResource(R.color.grey_400)
//        }
//
//        holder.itemView.OnOff.setOnClickListener {
//            this.OnOffListener?.invoke(device.deviceId, !OnOff)
//        }
//
//        holder.itemView.onLongClick {
//            this.onItemLongClickListener?.invoke(device.deviceId)
//        }
    }

    private var OnOffListener : ((deviceId: String, OnOff: Boolean) -> Unit)? = null
    fun addOnOffListener(OnOffListener : ((deviceId: String, OnOff: Boolean) -> Unit)){
        this.OnOffListener = OnOffListener
    }

    private var onItemLongClickListener: ((deviceId: String) -> Unit)? = null
    fun addItemLongClickListener(OnItemLongClickListener : ((deviceId: String) -> Unit)){
        this.onItemLongClickListener = OnItemLongClickListener
    }

    private var onItemClickListener: ((device: AdvertingDevice) -> Unit)? = null
    fun addItemClickListener(OnItemClickListener : ((device: AdvertingDevice) -> Unit)){
        this.onItemClickListener = OnItemClickListener
    }


    private fun sortList(){
//        val dataListTemp = ArrayList<Device>(dataList)
//        dataList.clear()
//
//        val onlineDeviceList = dataListTemp.filter { device ->
//            val activeFlag = device.deviceState?.activeFlags?.toInt() ?: 0;
//            val online = activeFlag > 0
//            online
//        }
//
//        val offlineDeviceList = dataListTemp.filter { device ->
//            val activeFlag = device.deviceState?.activeFlags?.toInt() ?: 0;
//            val online = activeFlag == 0
//            online
//        }
//
//        dataList.addAll(ArrayList(onlineDeviceList + offlineDeviceList))
        this.notifyDataSetChanged()
    }

}