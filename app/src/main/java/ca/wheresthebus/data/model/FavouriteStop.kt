package ca.wheresthebus.data.model

import android.os.Build
import androidx.annotation.RequiresApi

data class FavouriteStop(
    val nickname: String,
    val busStop: BusStop,
    val route: Route
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getStops() : List<StopTime> {
        return busStop.getBusTimes(route.id)
    }
}
