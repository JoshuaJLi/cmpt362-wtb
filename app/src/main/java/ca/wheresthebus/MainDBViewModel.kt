package ca.wheresthebus

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.data.mongo_model.MongoArrivalTime
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import ca.wheresthebus.data.mongo_model.MongoScheduledTrip
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId

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
            .isEmpty() && realm.query<MongoArrivalTime>().find().isEmpty())
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
        return result.map { modelFactory.toBusStop(it) }
    }

    fun getTrips(): ArrayList<ScheduledTrip> {
        return allTripsList.map { modelFactory.toScheduledTrip(it) } as ArrayList<ScheduledTrip>
    }
}