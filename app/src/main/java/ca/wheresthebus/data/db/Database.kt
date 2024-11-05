package ca.wheresthebus.data.db

import ca.wheresthebus.data.model.BusStop

interface Database {
    fun getStops() : List<BusStop>

    fun addFav()
}