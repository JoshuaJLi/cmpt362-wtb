package ca.wheresthebus.data.json

data class JsonStopTime(
    val stop_id: Int,
    val route_id: Int,
    val service_id: Int,
    val arrival_times: List<JsonArrivalTime>
)