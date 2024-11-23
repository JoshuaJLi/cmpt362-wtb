package ca.wheresthebus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainDBViewModel : ViewModel() {
    private val realm = MyMongoDBApp.realm
    private val modelFactory = ModelFactory()

    val _favouriteBusStopsList = MutableLiveData<MutableList<FavouriteStop>>()

    // change to LiveData instead of MutableLiveDataLater, since we will only be accessing this part of the DB?
    val _allBusStopsList = MutableLiveData<MutableList<BusStop>>()

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
    fun getBusStopByCode(stopCode: String) : BusStop? {
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
        return !(realm.query<MongoRoute>().find().isEmpty() && realm.query<MongoBusStop>().find().isEmpty())
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
                copyToRealm(modelFactory.toMongoFavouriteStop(favouriteStop), updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    fun searchByCode(code: String): ArrayList<BusStop> {
        val updatedList = ArrayList<BusStop>()
        val listOfMongoStops = realm.query<MongoBusStop>("code CONTAINS[c] $0", code).find().take(5)
        for (mongoStop in listOfMongoStops) {
            updatedList.add(modelFactory.toBusStop(mongoStop))
        }
        return updatedList
    }

    fun searchForRouteByShortName(shortName: String) : Route? {
        val route = realm.query<MongoRoute>("shortName == $0", shortName).find().take(1)
        if (route.isEmpty()) {
            return null
        }
        return modelFactory.toRoute(route[0]) ?: null
    }
}