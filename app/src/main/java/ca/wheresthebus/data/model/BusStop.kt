package ca.wheresthebus.data.model

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import java.time.LocalDateTime

data class BusStop(val id: StopId,
                   val code: StopCode,
                   val name: String,
                   val location: Location,
                   val nextBuses: List<StopTime>,
                   val routes: List<Route>
) {
    //
    @RequiresApi(Build.VERSION_CODES.O)
    fun getBusTimes(routeId: RouteId): List<StopTime> {
        val currentTime = LocalDateTime.now()
        val route = routes.find { it.id == routeId }
        if (route == null) {
            return listOf()
        }
        val nextBuses = nextBuses.filter { route.tripIds.contains(it.tripId) }
            .filter { it.arrivalTime > currentTime }
        return nextBuses
    }
}
