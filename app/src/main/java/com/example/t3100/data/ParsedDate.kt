package com.example.t3100.data

import com.google.gson.annotations.SerializedName

data class ParsedDate (
    var s : Int,
    @SerializedName("m") var mi : Int,
    var h : Int,
    var D : Int,
    @SerializedName("M") var Mo : Int,
    var Y : Int
)


