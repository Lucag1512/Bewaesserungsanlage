package com.lucag.t3100.data

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

//Datenklasse für Mikrocontroller damit Werte entsprechend zugeordnet werden können
//Anfügen von Headern
data class PlantHeader(

    var p: List<Plant>,
    var t: ParsedDate
)
