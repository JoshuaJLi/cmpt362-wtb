package ca.wheresthebus.data.model

import android.location.Location
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId

data class BusStop(val id: StopId,
                   val code: StopCode,
                   val name: String,
                   val location: Location,
                   val routes: List<Route>
) {

}
