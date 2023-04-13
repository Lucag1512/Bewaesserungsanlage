package com.lucag.t3100.data

import com.google.gson.annotations.SerializedName

//Datenklasse für Mikrocontroller damit Werte entsprechend zugeordnet werden können
//Anfügen von Headern
data class ParsedDate (
    var s : Int,
    @SerializedName("m") var mi : Int,
    var h : Int,
    var D : Int,
    @SerializedName("M") var Mo : Int,
    var Y : Int
)


