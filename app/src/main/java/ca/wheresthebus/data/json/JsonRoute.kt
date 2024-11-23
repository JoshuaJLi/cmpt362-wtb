package ca.wheresthebus.data.json

// Represents a JSON object for the static data in routes.json
data class JsonRoute(
    val route_id: Int,
    val route_short_name: String,
    val route_long_name: String
)