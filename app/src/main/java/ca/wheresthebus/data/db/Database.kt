package ca.wheresthebus.data.db

import ca.wheresthebus.data.mongo_model.MongoBusStop

interface Database {
    fun getStops() : List<MongoBusStop>

    fun addFav()
}