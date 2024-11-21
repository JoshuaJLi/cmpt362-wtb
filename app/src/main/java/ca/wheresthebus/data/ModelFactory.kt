package ca.wheresthebus.data

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import ca.wheresthebus.data.model.*
import ca.wheresthebus.data.mongo_model.*
import io.realm.kotlin.ext.toRealmList
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ModelFactory() {
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun toBusStop(mongoBusStop: MongoBusStop): BusStop {
        val location = Location("").apply {
            latitude = mongoBusStop.lat
            longitude = mongoBusStop.lng
        }
        
        return BusStop(
            id = StopId(mongoBusStop.id),
            code = StopCode(mongoBusStop.code),
            name = mongoBusStop.name,
            location = location,
            routes = mongoBusStop.mongoRoutes.map { toRoute(it) }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toMongoBusStop(busStop: BusStop): MongoBusStop {
        return MongoBusStop(
            id = busStop.id.value,
            code = busStop.code.value,
            name = busStop.name,
            lat = busStop.location.latitude,
            lng = busStop.location.longitude,
            mongoRoutes = busStop.routes.map { toMongoRoute(it) }.toRealmList()
        )
    }

    fun toFavouriteBusStop(mongoFavouriteStop: MongoFavouriteStop): FavouriteStop {
        return FavouriteStop(
            nickname = mongoFavouriteStop.nickname,
            busStop = toBusStop(mongoFavouriteStop.mongoBusStop!!),
            route = toRoute(mongoFavouriteStop.mongoRoute!!)
        )
    }

    fun toMongoFavouriteStop(favouriteStop: FavouriteStop) : MongoFavouriteStop {
        return MongoFavouriteStop(
            nickname = favouriteStop.nickname,
            mongoBusStop = toMongoBusStop(favouriteStop.busStop),
            mongoRoute = toMongoRoute(favouriteStop.route)
        )
    }

    private fun toRoute(mongoRoute: MongoRoute): Route {
        return Route(
            id = RouteId(mongoRoute.id),
            shortName = mongoRoute.shortName,
            longName = mongoRoute.longName,
            tripIds = mongoRoute.tripIds.map { TripId(it) }
        )
    }

    fun toMongoRoute(route: Route): MongoRoute {
        return MongoRoute().apply {
            id = route.id.value
            shortName = route.shortName
            longName = route.longName
            tripIds = route.tripIds.map { it.value }.toRealmList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toStopTime(mongoStopTime: MongoStopTime): StopTime {
        return StopTime(
            arrivalTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(mongoStopTime.arrivalTime),
                ZoneId.systemDefault()
            ),
            id = StopId(mongoStopTime.id),
            routeId = RouteId(mongoStopTime.routeId),
            serviceId = ServiceId(mongoStopTime.serviceId),
            tripId = TripId(mongoStopTime.tripId)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toMongoStopTime(stopTime: StopTime): MongoStopTime {
        return MongoStopTime().apply {
            arrivalTime = stopTime.arrivalTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            id = stopTime.id.value
            routeId = stopTime.routeId.value
            serviceId = stopTime.serviceId.value
            tripId = stopTime.tripId.value
        }
    }
}