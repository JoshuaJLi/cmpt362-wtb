package ca.wheresthebus.data.model

import ca.wheresthebus.data.ScheduledTripId

data class ScheduledTrip(
    val id : ScheduledTripId,
    val nickname : String,
    val stops : ArrayList<FavouriteStop>,
    val activeTimes : ArrayList<Schedule>
) {

}