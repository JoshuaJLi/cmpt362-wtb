package ca.wheresthebus.data.model

import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.TripId

// Represents routes
data class Route(
    // route_id
    val id : RouteId,
    val shortName : String,
    val longName : String,
    val tripIds : List<TripId>
)
