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
                   val routes: List<Route>
) {

}
