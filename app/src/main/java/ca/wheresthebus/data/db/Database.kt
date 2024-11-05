package ca.wheresthebus.data.db

import ca.wheresthebus.data.mongo_model.BusStop

interface Database {
    fun getStops() : List<BusStop>

    fun addFav()
}