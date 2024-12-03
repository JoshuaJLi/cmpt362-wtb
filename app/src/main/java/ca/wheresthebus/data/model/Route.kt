package ca.wheresthebus.data.model

import ca.wheresthebus.data.RouteId

// Represents a bus route
data class Route(
    val id : RouteId,
    val shortName : String, // e.g. R5, 33, 145, 16
    val longName : String // e.g. SFU/Production Station
)
