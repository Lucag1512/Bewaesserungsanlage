package com.example.t3100.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.t3100.data.Plant

class SharedViewModel : ViewModel() {
    var plantList : MutableList<Plant> = mutableListOf()
}