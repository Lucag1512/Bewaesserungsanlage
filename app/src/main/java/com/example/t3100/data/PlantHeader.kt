package com.example.t3100.data

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class PlantHeader(

    var p: List<Plant>,
    var t: ParsedDate
)
