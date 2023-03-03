package com.example.t3100.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.t3100.R
import com.example.t3100.data.Plant
import com.example.t3100.data.WateringElement

class SharedViewModel : ViewModel() {
    var plantList : MutableList<Plant> = mutableListOf()

    var tempWateringElementList : MutableList<WateringElement> = mutableListOf()

}