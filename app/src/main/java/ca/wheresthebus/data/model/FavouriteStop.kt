package ca.wheresthebus.data.model

import org.mongodb.kbson.ObjectId

data class FavouriteStop(
    val _id: ObjectId = ObjectId(),
    val nickname: String,
    val busStop: BusStop,
    val route: Route
) {

}
