package ca.wheresthebus

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.Globals.LAT_LNG_DEGREE_BUFFER
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import ca.wheresthebus.data.mongo_model.MongoScheduledTrip
import ca.wheresthebus.data.mongo_model.MongoStopTime
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.notifications.UpdatedResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

class MainDBViewModel : ViewModel() {
    companion object {
        val realm = MyMongoDBApp.realm
        private val staticModelFactory = ModelFactory()

        fun getTripById(scheduledTripId: ScheduledTripId) : ScheduledTrip? {
            return realm.query<MongoScheduledTrip>("id == $0", ObjectId(scheduledTripId.value)).find().firstOrNull()
                ?.let { staticModelFactory.toScheduledTrip(it) }
        }
    }

    private val realm = MyMongoDBApp.realm
    private val modelFactory = ModelFactory()

    val _favouriteBusStopsList = MutableLiveData<MutableList<FavouriteStop>>()
    val _allBusStopsList = MutableLiveData<MutableList<BusStop>>()
    val _allTripsList = MutableLiveData<MutableList<ScheduledTrip>>()

    init {
        loadAllStops()
        loadAllFavoriteStops()
        loadTrips()
        listenForTripChanges()
    }

    private fun listenForTripChanges() = viewModelScope.launch(Dispatchers.IO) {
        realm.query<MongoScheduledTrip>().asFlow()
            .collect{change : ResultsChange<MongoScheduledTrip> ->
                    when (change) {
                        is UpdatedResults -> {
                            _allTripsList.postValue(change.list
                                .map { modelFactory.toScheduledTrip(it) }
                                .toMutableList())
                        }

                        is InitialResults -> {
                            // do nothing
                        }
                    }
            }
    }

    private fun loadTrips() {
        _allTripsList.postValue(mutableListOf())
        val allMongoScheduledTrip = realm.query<MongoScheduledTrip>().find()
            .map { modelFactory.toScheduledTrip((it)) }
            .toMutableList()
        _allTripsList.postValue(allMongoScheduledTrip)
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

    fun insertScheduledTrip(scheduledTrip: ScheduledTrip) {
        val tripList = _allTripsList.value?.toMutableList() ?: mutableListOf()
        tripList.add(scheduledTrip)

        // Write to database in the bg
        viewModelScope.launch(Dispatchers.IO) {
            realm.write {
                copyToRealm(
                    modelFactory.toMongoScheduledTrip(scheduledTrip),
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

    fun deleteScheduledTrip(id: ScheduledTripId) {
        viewModelScope.launch(Dispatchers.IO) {
            val tripList = _allTripsList.value

            if (tripList?.removeIf { it.id == id} == true) {
                val idToDelete = ObjectId(id.value)
                val toDelete = realm.query<MongoScheduledTrip>("id == $0", idToDelete).find().firstOrNull()

                realm.write {
                    if (toDelete != null) {
                        findLatest(toDelete)?.also { delete(it) }
                    }
                    else {
                        println("deleteScheduledTrip: Could not find the right record to delete for some reason")
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
}
