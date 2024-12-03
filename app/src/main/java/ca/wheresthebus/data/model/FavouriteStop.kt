package ca.wheresthebus.data.model

import org.mongodb.kbson.ObjectId

// Represents a specific bus stop saved by the user
data class FavouriteStop(
    val _id: ObjectId = ObjectId(),
    val nickname: String,
    val busStop: BusStop,
    val route: Route
) {

}
