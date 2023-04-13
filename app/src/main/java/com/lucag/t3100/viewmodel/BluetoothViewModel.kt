package com.lucag.t3100.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lucag.t3100.data.Plant

class BluetoothViewModel : ViewModel()  {
    var bluetoothDevices : MutableList<BluetoothDevice> = mutableListOf()
    var plantList : List<Plant> = listOf()
}