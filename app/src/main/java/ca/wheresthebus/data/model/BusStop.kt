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
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.LocalDateTime

// Each realm object has to have its own EMPTY constructor.
class BusStop : RealmObject {
    var id: String = ""
    var code: String = ""
    var name: String = ""
    //var location: Location? = null,
    var lat: Double = 0.0
    var lng: Double = 0.0
    var nextBuses: RealmList<StopTime> = realmListOf()
    var routes: RealmList<Route> = realmListOf()

    //Primary (EMPTY) constructor
    constructor() : this("", "", "", 0.0, 0.0, realmListOf(), realmListOf()) {

    }
    //Secondary constructor
    constructor(
        id: String = "",
        code: String = "",
        name: String = "",
        //var location: Location? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        nextBuses: RealmList<StopTime> = realmListOf(),
        routes: RealmList<Route> = realmListOf()
    ) {
        this.id = id
        this.code = code
        this.name = name
        this.lat = lat
        this.lng = lng
        this.nextBuses = nextBuses
        this.routes = routes
    }

    //default required for realm objects
    //
    @PrimaryKey var _id: ObjectId = ObjectId()
    @RequiresApi(Build.VERSION_CODES.O)
    fun getBusTimes(routeId: String): List<StopTime> {
        val currentTime = LocalDateTime.now()

        val route = routes.find { it.id == routeId }

        if (route == null) {
            return listOf()
        }

//        val nextBuses = nextBuses.filter { route.tripIds.contains(it.tripId) }
//            .filter { it.arrivalTime > currentTime }
//        return nextBuses
        return emptyList()
    }
}