package ca.wheresthebus.data.json

// Represents a JSON object for the static data in stops.json
data class JsonStop(
    val stop_id: Int,
    val stop_code: Int,
    val stop_name: String,
    val stop_lat: Double,
    val stop_lon: Double,
    val route_id: List<Int>
)