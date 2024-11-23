package ca.wheresthebus

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class MainDBViewModel : ViewModel() {

    private val realm = MyMongoDBApp.realm
    private val modelFactory = ModelFactory()
    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    val _favouriteBusStopsList = MutableLiveData<MutableList<FavouriteStop>>()

    // change to LiveData instead of MutableLiveDataLater, since we will only be accessing this part of the DB?
    val _allBusStopsList = MutableLiveData<MutableList<BusStop>>()

    init {
        loadStopsIntoDatabase()
        loadFavouriteStopsFromDatabase()
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

    // populate view model here; perhaps have a function to load the favourite stops in the db later?
    init {
        val favStopsInDb = realm.query<MongoFavouriteStop>().find()

        if (favStopsInDb.isEmpty()) {
            createDummyEntries()
        }
    }

    // function that creates the sample entries
    // todo: remove after real data loaded in
    private fun createDummyEntries() {
        viewModelScope.launch {
            val newLocation = Location("passive")
            newLocation.latitude = (49.0123)
            newLocation.longitude = (-123.2354)
            val route1 = Route(RouteId("1"), "PEE", "Number 1")
            val route2 = Route(RouteId("2"), "POO", "Number 2")
            val busStop = BusStop(StopId("12345"), StopCode("34567"), "Pee St @ Poo Ave", newLocation, arrayListOf(route1, route2))
            insertBusStop(busStop)

            val testLocation1 = Location("passive")
            testLocation1.latitude = 49.0111
            testLocation1.longitude = -123.1111
            val testLocation2 = Location("passive")
            testLocation2.latitude = 49.1234
            testLocation2.longitude = -123.5678
            val testLocation3 = Location("passive")
            testLocation3.latitude = 49.2222
            testLocation3.longitude = -123.3333
            val testLocation4 = Location("passive")
            testLocation4.latitude = 49.2002
            testLocation4.longitude = -123.2002
            val testLocation5 = Location("passive")
            testLocation5.latitude = 49.1989
            testLocation5.longitude = -123.1989
            val testRoute1 = Route(RouteId("01"), "013", "taylor swift")
            val testRoute2 = Route(RouteId("02"), "505", "rihanna")
            val testRoute3 = Route(RouteId("03"), "802", "post malone")
            val testRoute4 = Route(RouteId("04"), "999", "the weeknd")
            val testRoute5 = Route(RouteId("05"), "24K", "bruno mars")
            insertBusStop(BusStop(StopId("1"), StopCode("55234"), "1989 St @ TTPD Ave", testLocation1, arrayListOf(testRoute1, testRoute3)))
            insertBusStop(BusStop(StopId("2"), StopCode("23199"), "Disturbia St @ Umbrella Ave", testLocation2, arrayListOf(testRoute2, testRoute4)))
            insertBusStop(BusStop(StopId("3"), StopCode("11111"), "Circles St @ F1-Trillion Blvd", testLocation3, arrayListOf(testRoute1, testRoute3)))
            insertBusStop(BusStop(StopId("4"), StopCode("10001"), "Dancing St @ The Flames Ave", testLocation4, arrayListOf(testRoute4)))
            insertBusStop(BusStop(StopId("5"), StopCode("24000"), "The APT @ Grenade Ave", testLocation5, arrayListOf(testRoute2, testRoute5)))

            // once inserted, the above will not be reinserted.
            insertFavouriteStop(FavouriteStop("hello", busStop, route1))
        }
    }

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun insertBusStop(newStop: BusStop) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(modelFactory.toMongoBusStop(newStop), updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    // function to return bus stops by entering the stop code
    // TODO @Jonathan: have this function return a normal bus stop instead maybe?
    @RequiresApi(Build.VERSION_CODES.O)
    fun getBusStopByCode(stopCode: String) : BusStop? {
        return realm.query<MongoBusStop>("code == $0", stopCode).find().firstOrNull()
            ?.let { modelFactory.toBusStop(it) }
    }

    private fun loadFavouriteStopsFromDatabase() {
        _favouriteBusStopsList.postValue(mutableListOf())
        val updatedList = mutableListOf<FavouriteStop>()
        val allMongoFavStops = realm.query<MongoFavouriteStop>().find()
        for (mongoBusStop in allMongoFavStops) {
            updatedList.add(modelFactory.toFavouriteBusStop(mongoBusStop))
        }
        _favouriteBusStopsList.postValue(updatedList)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadStopsIntoDatabase() {
        // if initially run, we load in all the stops in the db
        if (realm.query<MongoBusStop>().find().isEmpty()) {
            // load all the bus stops into the database
            var stopToAdd = MongoBusStop()
            // stopToAdd.id = ...
            // stopToAdd.code = ...
        }
        _allBusStopsList.postValue(mutableListOf())
        val convertedBusStopList = mutableListOf<BusStop>()
        val allMongoBusStops = realm.query<MongoBusStop>().find()
        for (mongoBusStop in allMongoBusStops) {
            convertedBusStopList.add(modelFactory.toBusStop(mongoBusStop))
        }
        _allBusStopsList.postValue(convertedBusStopList)
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

    @RequiresApi(Build.VERSION_CODES.O)
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