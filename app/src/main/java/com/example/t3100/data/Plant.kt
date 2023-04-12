package com.example.t3100.data

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

//Datenklasse für Pflanzenobjekte
//Durch @SerializedName werden Header für Zuordnung im Mikrocontroller angefügt
data class Plant(

    @SerializedName("n")var name: String?,
    @SerializedName("v")var valve: Int,
    @SerializedName("b")var wateringList: MutableList<WateringElement>

    //Einstellungen für Übergabe ins JSON Format
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readInt(),
        parcel.createTypedArrayList(WateringElement)?.toMutableList()?: mutableListOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(valve)
        parcel.writeTypedList(wateringList)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Plant> {
        override fun createFromParcel(parcel: Parcel): Plant {
            return Plant(parcel)
        }

        override fun newArray(size: Int): Array<Plant?> {
            return arrayOfNulls(size)
        }
    }
}