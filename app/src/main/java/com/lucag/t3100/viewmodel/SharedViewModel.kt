package com.lucag.t3100.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lucag.t3100.R
import com.lucag.t3100.data.Plant
import com.lucag.t3100.data.WateringElement

class SharedViewModel : ViewModel() {
    var plantList : MutableList<Plant> = mutableListOf()

    var tempWateringElementList : MutableList<WateringElement> = mutableListOf()

}