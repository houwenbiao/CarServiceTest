package com.jack.cartest.ui

import android.annotation.SuppressLint
import android.app.JackManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.jack.cartest.R
import com.jack.cartest.databinding.ActivityMainBinding
import com.jack.cartest.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG: String = "MainActivity"
    }

    lateinit var mainBinding: ActivityMainBinding
    private val mainViewModel: MainViewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainBinding.mainVM = mainViewModel
        mainBinding.lifecycleOwner = this
        val jackManager:JackManager = getSystemService(Context.JACK_SERVICE) as JackManager
        Log.d(TAG, "JackManager: ${jackManager.plusA(1, 2)}")
    }
}