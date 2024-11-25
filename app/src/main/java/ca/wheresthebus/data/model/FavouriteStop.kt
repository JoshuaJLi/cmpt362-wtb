package ca.wheresthebus.data.model

data class FavouriteStop(
    val nickname: String,
    val busStop: BusStop,
    val route: Route
) {

}
