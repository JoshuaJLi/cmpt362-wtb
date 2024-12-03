package ca.wheresthebus.data.json

// Represents a JSON object for the static bus schedules in stop_times.json
data class JsonStopTime(
    val stop_id: Int,
    val route_id: Int,
    val service_id: Int,
    val arrival_times: List<JsonArrivalTime>
)
