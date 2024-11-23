package ca.wheresthebus.data.json

// Represents a JSON object for the static data in stops.json
data class JsonStop(
    val id: Int,
    val code: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val route_ids: List<Int>
)