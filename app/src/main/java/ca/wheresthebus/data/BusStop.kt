package ca.wheresthebus.data

import android.location.Location


// Requires some sort of coodinates
data class BusStop(val code : BusCode, val nickname : String, val location : Location)
