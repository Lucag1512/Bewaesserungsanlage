package com.example.t3100

import android.app.Application
import android.content.Context

class App : Application() {
    val DEFAULT_PREF = "Default"
    var calibrationValue1: Double = 13.33 //Wert laut Datenblatt
    var calibrationValue2: Double = 13.33 //Wert laut Datenblatt
    var calibrationValue3: Double = 13.33 //Wert laut Datenblatt

    override fun onCreate() {
        super.onCreate()
        getSavedCalibrationValue1()
        getSavedCalibrationValue2()
        getSavedCalibrationValue3()
    }

    fun setNewCalibrationValue1(newCalibrationValue: Double) {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with(storage.edit()) {
                putString("calibrationValue1", newCalibrationValue.toString())
                apply()
            }
        }
        calibrationValue1 = newCalibrationValue
    }

    fun setNewCalibrationValue2(newCalibrationValue: Double) {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with(storage.edit()) {
                putString("calibrationValue2", newCalibrationValue.toString())
                apply()
            }
        }
        calibrationValue2 = newCalibrationValue
    }

    fun setNewCalibrationValue3(newCalibrationValue: Double) {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        sharedPref?.let { storage ->
            with(storage.edit()) {
                putString("calibrationValue3", newCalibrationValue.toString())
                apply()
            }
        }
        calibrationValue3 = newCalibrationValue
    }

    private fun getSavedCalibrationValue1() {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        calibrationValue1 =
            sharedPref?.getString("calibrationValue1", "")?.toDoubleOrNull() ?: 13.33
    }

    private fun getSavedCalibrationValue2() {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        calibrationValue2 =
            sharedPref?.getString("calibrationValue2", "")?.toDoubleOrNull() ?: 13.33
    }

    private fun getSavedCalibrationValue3() {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        calibrationValue3 =
            sharedPref?.getString("calibrationValue3", "")?.toDoubleOrNull() ?: 13.33
    }
}