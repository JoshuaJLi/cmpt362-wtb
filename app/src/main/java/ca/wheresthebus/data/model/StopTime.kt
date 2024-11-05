package ca.wheresthebus.data.model

import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.ServiceId
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.TripId
import java.time.LocalDateTime

// Represents stop_times and trips
data class StopTime(
    val arrivalTime: LocalDateTime,
    //stop_id
    val id : StopId,
    val routeId: RouteId,
    val serviceId : ServiceId,
    val tripId : TripId
)