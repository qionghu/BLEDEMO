package com.hq.blemeshdemo.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.hq.blemeshdemo.R
import com.hq.blemeshdemo.Utils.bytesToHexString
import com.hq.blemeshdemo.bean.AdvertingDevice
import com.hq.blemeshdemo.model.ALL_DISPLAY
import com.hq.blemeshdemo.model.DISPLAY_ALL_MESH
import com.hq.blemeshdemo.model.JUST_DISPLAY_BIND
import com.hq.blemeshdemo.model.getDisplayString
import kotlinx.android.synthetic.main.unprovision_device_header.view.*
import kotlinx.android.synthetic.main.unprovision_device_item.view.*
import kotlinx.android.synthetic.main.unprovision_device_item.view.title

//private val dataList: ArrayList<Device>,
class ScanDeviceListAdapter(val fragmentActivity: FragmentActivity, liveData: LiveData<List<AdvertingDevice>>) : RecyclerView.Adapter<ScanDeviceListAdapter.DeviceViewHolder>() {

    private val TAG = "ScanDeviceListAdapter"

    private val ITEM_TYPE_HEADER = 0
    private val ITEM_TYPE_CONTENT = 1

    val dataList: ArrayList<AdvertingDevice> = arrayListOf()
    private var filterType: Int = ALL_DISPLAY

    init {
        val listObserver = Observer<List<AdvertingDevice>>{
            dataList.clear()
            dataList.addAll(it)
            sortList()
        }

        liveData.observe(fragmentActivity, listObserver)
    }

    class DeviceViewHolder(itemView: View, val viewType: Int): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = if(viewType == ITEM_TYPE_HEADER){
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.unprovision_device_header, parent, false)
        }else{
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.unprovision_device_item, parent, false)
        }

        return DeviceViewHolder(itemView = itemView, viewType = viewType)
    }

    override fun getItemViewType(position: Int): Int {
        if(position == 0){
            return ITEM_TYPE_HEADER
        }else{
            return ITEM_TYPE_CONTENT
        }
    }

    override fun getItemCount(): Int {
        return dataList.size + 1
    }


    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        if(holder.viewType == ITEM_TYPE_HEADER){
            (holder.itemView.filter as TextView).text = getDisplayString(filterType)

            holder.itemView.filter.setOnClickListener {
                if(filterType == JUST_DISPLAY_BIND){
                    filterType = ALL_DISPLAY
                }else{
                    filterType += 1
                }

                notifyItemChanged(position)

                this.filterSwitchListener?.invoke(filterType)
            }
        }else{
            val device = dataList[position - 1]
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
        }


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

    private var filterSwitchListener: ((filterType : Int) -> Unit)? = null
    fun addFilterListener(filterSwitchListener: ((filterType : Int) -> Unit)){
        this.filterSwitchListener = filterSwitchListener
    }

    private fun sortList(){
        this.notifyDataSetChanged()
    }

}