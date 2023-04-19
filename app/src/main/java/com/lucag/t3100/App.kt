package com.lucag.t3100

import android.app.Application
import android.content.Context

class App : Application() {
    var calibrationValue1: Double = 0.013333 //Wert laut Datenblatt
    var calibrationValue2: Double = 0.013333 //Wert laut Datenblatt
    var calibrationValue3: Double = 0.013333 //Wert laut Datenblatt

    val DEFAULT_PREF = "Default"

    override fun onCreate() {
        super.onCreate()
        getSavedCalibrationValue1()
        getSavedCalibrationValue2()
        getSavedCalibrationValue3()
    }

    fun setNewCalibrationValue1(newCalibrationValue: Double) {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        //Wert auf dem Handy speichern
        sharedPref?.let { storage ->
            with(storage.edit()) {
                putString("calibrationValue1", newCalibrationValue.toString())
                apply()
            }
        }
        //Globalen Wert anpassen
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
        //Wert von Handyspeicher laden
        calibrationValue1 =
            sharedPref?.getString("calibrationValue1", "")?.toDoubleOrNull() ?: 0.013333
    }

    private fun getSavedCalibrationValue2() {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        calibrationValue2 =
            sharedPref?.getString("calibrationValue2", "")?.toDoubleOrNull() ?: 0.013333
    }

    private fun getSavedCalibrationValue3() {
        val sharedPref =
            applicationContext?.getSharedPreferences(DEFAULT_PREF, Context.MODE_PRIVATE)
        calibrationValue3 =
            sharedPref?.getString("calibrationValue3", "")?.toDoubleOrNull() ?: 0.013333
    }
}