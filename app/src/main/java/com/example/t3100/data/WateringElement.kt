package com.example.t3100.data

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

//Datenklasse für Bewässerungszeitpunkte
//Durch @SerializedName werden Header für Zuordnung im Mikrocontroller angefügt
data class WateringElement (
    @SerializedName("w")var water: Double,
    @SerializedName("h")var hour: Int,
    @SerializedName("m")var minute: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(water)
        parcel.writeInt(hour)
        parcel.writeInt(minute)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WateringElement> {
        override fun createFromParcel(parcel: Parcel): WateringElement {
            return WateringElement(parcel)
        }

        override fun newArray(size: Int): Array<WateringElement?> {
            return arrayOfNulls(size)
        }
    }
}


