package com.hq.blemeshdemo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.marginBottom
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
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

}
