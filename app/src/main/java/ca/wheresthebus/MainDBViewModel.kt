package ca.wheresthebus

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.Globals.LAT_LNG_DEGREE_BUFFER
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import ca.wheresthebus.data.model.Schedule
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import ca.wheresthebus.data.mongo_model.MongoScheduledTrip
import ca.wheresthebus.data.mongo_model.MongoStopTime
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import java.time.DayOfWeek
import java.time.LocalTime

class MainDBViewModel : ViewModel() {
    private val realm = MyMongoDBApp.realm
    private val modelFactory = ModelFactory()

    val _favouriteBusStopsList = MutableLiveData<MutableList<FavouriteStop>>()

    // change to LiveData instead of MutableLiveDataLater, since we will only be accessing this part of the DB?
    val _allBusStopsList = MutableLiveData<MutableList<BusStop>>()

    val allTripsList = realm.query<MongoScheduledTrip>().find()

    init {
        loadAllStops()
        loadAllFavoriteStops()
    }

    private fun loadAllFavoriteStops() {
        _favouriteBusStopsList.postValue(mutableListOf())
        val updatedList = mutableListOf<FavouriteStop>()
        val allMongoFavStops = realm.query<MongoFavouriteStop>().find()
        println("Found ${allMongoFavStops.size} fav stops in db to load...")
        for (mongoBusStop in allMongoFavStops) {
            updatedList.add(modelFactory.toFavouriteBusStop(mongoBusStop))
        }
        _favouriteBusStopsList.postValue(updatedList)
    }

    // NOTE: _allBusStopsList will be empty if tables are not filled on initial app start
    private fun loadAllStops() {
        _allBusStopsList.postValue(mutableListOf())
        val convertedBusStopList = mutableListOf<BusStop>()
        val allMongoBusStops = realm.query<MongoBusStop>().find()
        for (mongoBusStop in allMongoBusStops) {
            convertedBusStopList.add(modelFactory.toBusStop(mongoBusStop))
        }
        _allBusStopsList.postValue(convertedBusStopList)
    }

    // NOTE: ONLY use when populating database on initial app load in MainActivity
    fun getRealm(): Realm {
        return realm
    }

    // Returns true if both MongoRoutes and MongoBusStops have already been initialized
    fun isStaticDataLoaded(): Boolean {
        return !(realm.query<MongoRoute>().find().isEmpty() && realm.query<MongoBusStop>().find()
            .isEmpty() && realm.query<MongoStopTime>().find().isEmpty())
    }

    fun getAllStops(): List<BusStop>? {
        // Load stops if needed
        if (_allBusStopsList.value.isNullOrEmpty()) {
            loadAllStops()
        }
        return _allBusStopsList.value
    }

    fun getBusStopByCode(stopCode: String) : BusStop? {
        return realm.query<MongoBusStop>("code == $0", stopCode).find().firstOrNull()
            ?.let { modelFactory.toBusStop(it) }
    }

    fun getBusStopWithinRange(userLocation: Location, distanceThreshold: Double): List<BusStop> {
        val allBusStops = getAllStops() ?: return emptyList()
        return allBusStops.filter { busStop ->
            val stopLocation = Location("")
            stopLocation.latitude = busStop.location.latitude
            stopLocation.longitude = busStop.location.longitude
            userLocation.distanceTo(stopLocation) < distanceThreshold
        }
    }

    // copied and modified from BusNotifierService.kt
    fun getNearbyStops(currentLocation: Location): List<BusStop> {
        val nearbyQuery = "(lat >= $0 AND lat <= $1) AND (lng >= $2 AND lng <= $3)"

        val latitude = currentLocation.latitude
        val longitude = currentLocation.longitude

        val result = realm.query<MongoBusStop>(
            nearbyQuery,
            latitude - LAT_LNG_DEGREE_BUFFER,
            latitude + LAT_LNG_DEGREE_BUFFER,
            longitude - LAT_LNG_DEGREE_BUFFER,
            longitude + LAT_LNG_DEGREE_BUFFER
        )

        return result.find().map { modelFactory.toBusStop(it) }
    }

    // function to add a favourite stop
    fun insertFavouriteStop(favouriteStop: FavouriteStop) {
        // Add new stop to the viewmodel first
        val favList = _favouriteBusStopsList.value?.toMutableList() ?: mutableListOf()
        favList.add(favouriteStop)
        _favouriteBusStopsList.postValue(favList)

        // Write to database in the bg
        viewModelScope.launch(Dispatchers.IO) {
            realm.write {
                copyToRealm(
                    modelFactory.toMongoFavouriteStop(favouriteStop),
                    updatePolicy = UpdatePolicy.ALL
                )
            }
        }
    }

    fun deleteFavouriteStop(_id: ObjectId) {
        viewModelScope.launch(Dispatchers.IO) {
            val favList = _favouriteBusStopsList.value?.toMutableList() ?: mutableListOf()

            // Remove the fav stop from adapter list
            if (favList.removeIf { it._id == _id }) {
                // Post the updated list back to LiveData
                _favouriteBusStopsList.postValue(favList)

                // Find the value to delete
                val toDelete = realm.query<MongoFavouriteStop>("_id == $0", _id).find().firstOrNull()

                // Remove from db too
                realm.write {
                    if (toDelete != null) {
                        findLatest(toDelete)?.also { delete(it) }
                    }
                    else {
                        println("deleteFavouriteStop: Could not find the right record to delete for some reason...")
                    }
                }
            }
        }
    }

    // todo: can improve search by sorting by closest location
    fun searchForStop(input: String): List<BusStop> {
        // search the name parts by any matching string tokens
        val tokens = input.split(" ").filter { it.isNotEmpty() }
        val fuzzyQuery = List(tokens.size) { i ->
            "(code == $${i} OR ANY mongoRoutes.shortName == $${i} OR name CONTAINS[c] $${i})"
        }.joinToString(" AND ")

        val result = realm.query<MongoBusStop>(
            fuzzyQuery,
            *tokens.toTypedArray() // spread operator to pass string tokens as query params
        ).find().takeWhile { true }

        // Return result as a List<BusStops> instead of List<MongoBusStops>
        return result.map { modelFactory.toBusStop(it) }
    }

    fun getTrips(): ArrayList<ScheduledTrip> {
        val trips = ArrayList<ScheduledTrip>()


        // Schedule, Stop, Route setup
        val schedule1 = Schedule(DayOfWeek.SUNDAY, LocalTime.of(23, 0))
        val schedule2 = Schedule(DayOfWeek.SUNDAY, LocalTime.of(23, 30))
        val schedule3 = Schedule(DayOfWeek.MONDAY, LocalTime.of(7, 45))
        val schedule4 = Schedule(DayOfWeek.TUESDAY, LocalTime.of(7, 45))
        val schedule5 = Schedule(DayOfWeek.WEDNESDAY, LocalTime.of(8, 45))
        val stop = BusStop(
            StopId("test"),
            StopCode("test"),
            "Bus Stop",
            Location("Test Location"),
            ArrayList()
        )
        val route1 = Route(RouteId("route1"), "20", "Morning Trip")
        val route2 = Route(RouteId("route2"), "15", "Classes")
        val route3 = Route(RouteId("route3"), "10", "Sunday Classes")

        // Favorite stops setup
        val favouriteStop1 = FavouriteStop(ObjectId(),"145 @ Production Way", stop, route1)
        val favouriteStop2 = FavouriteStop(ObjectId(), "R5 @ SFU", stop, route2)
        val favouriteStop3 = FavouriteStop(ObjectId(), "R5 @ Waterfront", stop, route3)

        // Trip 1
        val stops1 = ArrayList<FavouriteStop>()
        val schedules1 = ArrayList<Schedule>()
        schedules1.add(schedule1)
        stops1.add(favouriteStop1)
        val trip1 = ScheduledTrip(ScheduledTripId("trip1"), "CMPT 362", stops1, schedules1)

        // Trip 2
        val stops2 = ArrayList<FavouriteStop>()
        val schedules2 = ArrayList<Schedule>()
        schedules2.add(schedule2)
        stops2.add(favouriteStop2)
        stops2.add(favouriteStop1)
        val trip2 = ScheduledTrip(ScheduledTripId("trip2"), "Office Hours", stops2, schedules2)

        // Trip 3
        val stops3 = ArrayList<FavouriteStop>()
        val schedules3 = ArrayList<Schedule>()
        schedules3.add(schedule3)
        schedules3.add(schedule4)
        schedules3.add(schedule5)
        stops3.add(favouriteStop3)
        val trip3 = ScheduledTrip(ScheduledTripId("trip3"), "Board Games", stops3, schedules3)

        // Add trips to the list
        trips.add(trip1)
        trips.add(trip2)
        trips.add(trip3)

        return trips
    }
}