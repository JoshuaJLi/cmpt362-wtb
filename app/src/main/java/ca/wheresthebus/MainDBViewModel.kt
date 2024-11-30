package ca.wheresthebus

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // NOTE: each view model for each frag can query for different class objects from the db when required
    val mongoFavouriteStops = realm
        .query<MongoFavouriteStop>(
            // use this to filter entries in the view model; ex:
            //"teacher.address.fullName CONTAINS $0", "John" queries any teachers named John
        )
        .find() // gets all the favourite stops and returns it as a StateFlow<List<MongoFavouriteStop>
        .asFlow()
        .map { results ->
            results.list.toList()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    var favStopDetails: MongoFavouriteStop? by mutableStateOf(null)
        private set

    fun deleteMongoFavStop() {
        viewModelScope.launch {
            realm.write {
                //TODO: adapt later
                val favStop = favStopDetails ?: return@write
                delete(favStop)
                favStopDetails = null
            }
        }
    }

    fun insertBusStop(newStop: BusStop) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(modelFactory.toMongoBusStop(newStop), updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    // function to return bus stops by entering the stop code
    // TODO @Jonathan: have this function return a normal bus stop instead maybe?
    fun getBusStopByCode(stopCode: String): BusStop? {
        return realm.query<MongoBusStop>("code == $0", stopCode).find().firstOrNull()
            ?.let { modelFactory.toBusStop(it) }
    }

    private fun loadAllFavoriteStops() {
        _favouriteBusStopsList.postValue(mutableListOf())
        val updatedList = mutableListOf<FavouriteStop>()
        val allMongoFavStops = realm.query<MongoFavouriteStop>().find()
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
            .isEmpty())
    }

    fun getAllStops(): List<BusStop>? {
        // Load stops if needed
        if (_allBusStopsList.value.isNullOrEmpty()) {
            loadAllStops()
        }
        return _allBusStopsList.value
    }

    // function to add a favourite stop (add route/mongo route parameter later??)
    // TODO @Jonathan: have this function take in a normal bus stop and then convert it accordingly
    fun insertFavouriteStop(favouriteStop: FavouriteStop) {
        viewModelScope.launch {
            val updatedList = _favouriteBusStopsList.value?.toMutableList() ?: mutableListOf()

            // Add the new favouriteStop to the list
            updatedList.add(favouriteStop)

            // Post the updated list back to LiveData
            _favouriteBusStopsList.postValue(updatedList)
            realm.write {
                copyToRealm(
                    modelFactory.toMongoFavouriteStop(favouriteStop),
                    updatePolicy = UpdatePolicy.ALL
                )
            }
        }
    }

    // todo: can improve search by sorting by closest location
    fun searchForStop(input: String): List<BusStop> {
        // search the name parts by any matching string tokens
        val words = input.split(" ").filter { it.isNotEmpty() }
        val fuzzyQuery = List(words.size) { i ->
            "(name CONTAINS[c] $${i + 1} OR ANY mongoRoutes.longName CONTAINS[c] $${i + 1})"
        }.joinToString(" AND ")

        // Strict search by code or route shortname
        val query = "code == $0 OR ANY mongoRoutes.shortName == $0 OR ($fuzzyQuery)"
        val result = realm.query<MongoBusStop>(
            query,
            input,
            *words.toTypedArray() // spread operator to pass string tokens as query params
        ).find().take(10)

        // Return result as a List<BusStops> instead of List<MongoBusStops>
        return result.map { it -> modelFactory.toBusStop(it) }
    }

    fun searchForRouteByShortName(shortName: String): Route? {
        val route = realm.query<MongoRoute>("shortName == $0", shortName).find().take(1)
        if (route.isEmpty()) {
            return null
        }
        return modelFactory.toRoute(route[0]) ?: null
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
        val favouriteStop1 = FavouriteStop("145 @ Production Way", stop, route1)
        val favouriteStop2 = FavouriteStop("R5 @ SFU", stop, route2)
        val favouriteStop3 = FavouriteStop("R5 @ Waterfront", stop, route3)

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
//        return allTripsList.map { modelFactory.toScheduledTrip(it) } as ArrayList<ScheduledTrip>
    }
}