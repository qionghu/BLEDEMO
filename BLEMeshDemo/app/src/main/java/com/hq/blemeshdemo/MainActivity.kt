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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.hq.blemesh.BleMesh
import com.hq.blemeshdemo.Utils.toast
import com.hq.blemeshdemo.activity.BleScanActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import kotlin.math.log


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()

        //initReceiver()

//        BleMesh.getInstance().test()
        //test add some content
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_top_right_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.main_item_add -> {
                //toast(this, "点击menu - add !")
                startActivity(Intent(this, BleScanActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
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
