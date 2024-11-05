package ca.wheresthebus.data.model

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.LocalDateTime

class BusStop : RealmObject {
    //default required for realm objects
    @PrimaryKey var _id: ObjectId = ObjectId()

    val id: StopId? = null
    val code: StopCode? = null
    val name: String = ""
    val location: Location? = null
    val nextBuses: RealmList<StopTime> = realmListOf()
    val routes: RealmList<Route> = realmListOf()
    //
    @RequiresApi(Build.VERSION_CODES.O)
    fun getBusTimes(routeId: RouteId): List<StopTime> {
        val currentTime = LocalDateTime.now()

        val route = routes.find { it.id == routeId }

        if (route == null) {
            return listOf()
        }

//        val nextBuses = nextBuses.filter { route.tripIds.contains(it.tripId) }
//            .filter { it.arrivalTime > currentTime }
        val nextBuses = nextBuses.filter { route.tripIds.contains(it.tripId) }
            .filter { it.arrivalTime!! > currentTime }
        return nextBuses
    }
}