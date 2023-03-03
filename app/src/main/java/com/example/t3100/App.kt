package com.example.t3100

import android.app.Application
import android.content.Context

class App : Application(){
    val DEFAULT_PREF = "Default"
    var calibrationValue : Double = 13.33 //Wert laut Datenblatt

    override fun onCreate() {
        super.onCreate()
        getSavedCalibrationValue()
    }

    fun setNewCalibrationValue(newCalibrationValue : Double){
        val sharedPref = applicationContext?.getSharedPreferences(DEFAULT_PREF,Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with (storage.edit()) {
                putString("calibrationValue", newCalibrationValue.toString())
                apply()
            }
        }
        calibrationValue = newCalibrationValue
    }

    private fun getSavedCalibrationValue(){
        val sharedPref = applicationContext?.getSharedPreferences(DEFAULT_PREF,Context.MODE_PRIVATE)
        calibrationValue = sharedPref?.getString("calibrationValue", "")?.toDoubleOrNull() ?: 13.33
    }
}