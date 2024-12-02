package ca.wheresthebus.data

import android.location.Location
import ca.wheresthebus.data.model.*
import ca.wheresthebus.data.mongo_model.*
import io.realm.kotlin.ext.toRealmList
import org.mongodb.kbson.ObjectId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ModelFactory() {
    
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
            _id = mongoFavouriteStop._id,
            nickname = mongoFavouriteStop.nickname,
            busStop = toBusStop(mongoFavouriteStop.mongoBusStop!!),
            route = toRoute(mongoFavouriteStop.mongoRoute!!)
        )
    }

    fun toMongoFavouriteStop(favouriteStop: FavouriteStop) : MongoFavouriteStop {
        return MongoFavouriteStop(
            _id = favouriteStop._id,
            nickname = favouriteStop.nickname,
            mongoBusStop = toMongoBusStop(favouriteStop.busStop),
            mongoRoute = toMongoRoute(favouriteStop.route)
        )
    }

    fun toRoute(mongoRoute: MongoRoute): Route {
        return Route(
            id = RouteId(mongoRoute.id),
            shortName = mongoRoute.shortName,
            longName = mongoRoute.longName,
        )
    }

    fun toMongoRoute(route: Route): MongoRoute {
        return MongoRoute().apply {
            id = route.id.value
            shortName = route.shortName
            longName = route.longName
        }
    }

    private fun toSchedule(mongoSchedule: MongoSchedule) : Schedule {
        return Schedule(
            DayOfWeek.of(mongoSchedule.day),
            java.time.LocalTime.of(mongoSchedule.hour, mongoSchedule.minute)
        )
    }

    fun toScheduledTrip(mongoScheduledTrip: MongoScheduledTrip): ScheduledTrip {
        return ScheduledTrip(
            id = ScheduledTripId(mongoScheduledTrip.id.toHexString()) ,
            requestCode = IntentRequestCode(mongoScheduledTrip.requestCode),
            nickname = mongoScheduledTrip.nickname,
            stops = mongoScheduledTrip.stops.map { toFavouriteBusStop(it) } as ArrayList<FavouriteStop>,
            activeTimes = mongoScheduledTrip.activeTimes.map { toSchedule(it) } as ArrayList<Schedule>,
            duration = Duration.ofMinutes(mongoScheduledTrip.duration))
    }

    fun toMongoScheduledTrip(trip: ScheduledTrip): MongoScheduledTrip {
        return MongoScheduledTrip().apply {
            id = ObjectId(trip.id.value)
            requestCode = trip.requestCode.value
            nickname = trip.nickname
            stops = trip.stops.map { toMongoFavouriteStop(it) }.toRealmList()
            activeTimes = trip.activeTimes.map { toMongoSchedule(it) }.toRealmList()
            duration = trip.duration.toMinutes()
        }
    }

    private fun toMongoSchedule(schedule: Schedule) : MongoSchedule {
        return MongoSchedule().apply {
            day = schedule.day.value
            minute = schedule.time.minute
            hour = schedule.time.hour
        }
    }

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