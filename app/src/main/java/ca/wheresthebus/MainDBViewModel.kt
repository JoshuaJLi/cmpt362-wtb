package ca.wheresthebus

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainDBViewModel : ViewModel() {

    private val realm = MyMongoDBApp.realm
    private val modelFactory = ModelFactory()
    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

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

    fun showFavStopDetails(favouriteStop: MongoFavouriteStop) {
        favStopDetails = favouriteStop
    }

    fun hideCourseDetails() {
        favStopDetails = null
    }

    // function that creates the sample entries
    private fun createDummyEntries() {
        viewModelScope.launch {
            realm.write {
                // populate the viewmodel with the loaded in BusStops/FavStops/wtv when needed?
            }
        }
    }

    fun deleteMongoFavStop() {
        viewModelScope.launch {
            realm.write {
                //TODO: adapt later
//                val course = courseDetails ?: return@write
//                val latestCourse = findLatest(course) ?: return@write
//                delete(latestCourse)
//
//                courseDetails = null
                val favStop = favStopDetails ?: return@write
                delete(favStop)
                favStopDetails = null
            }
        }
    }

    fun insertMongoFavStop(mongoFavouriteStop: MongoFavouriteStop) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(mongoFavouriteStop, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }
    // TODO @Jonathan: have this function take in a normal bus stop and then convert it accordingly
    fun insertMongoBusStop(newStop: MongoBusStop) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(newStop, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    fun insertBusStop(newStop: BusStop) {
        viewModelScope.launch {
//            var mongoStopToInsert = MongoBusStop()
//            val stopId = newStop.id
//            Log.d("stopid?", stopId.toString())
//            mongoStopToInsert.id = newStop.id.value
//            mongoStopToInsert.code = newStop.code.value
//            mongoStopToInsert.name = newStop.name
//            mongoStopToInsert.lat = newStop.location.latitude
//            mongoStopToInsert.lng = newStop.location.longitude
//            val realmListOfMongoRoutes = realmListOf<MongoRoute>()
//            val realmListOfTripIds = realmListOf<String>()
            realm.write {
//                newStop.routes.forEach { route ->
//                    val mongoRoute = MongoRoute().apply {
//                        id = route.id.toString()
//                        shortName = route.shortName
//                        longName = route.longName
//                        for (id in route.tripIds) {
//                            realmListOfTripIds.add(id.value)
//                        }
//                        tripIds = realmListOfTripIds
//                    }
//                    realmListOfMongoRoutes.add(mongoRoute)
//                    realmListOfTripIds.clear()
//                }
//                mongoStopToInsert.mongoRoutes = realmListOfMongoRoutes
//                copyToRealm(mongoStopToInsert, updatePolicy = UpdatePolicy.ALL)
                copyToRealm(modelFactory.toMongoBusStop(newStop), updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    // function to return bus stops by entering the stop code
    // TODO @Jonathan: have this function return a normal bus stop instead maybe?
    fun getBusStopByCode(stopCode: String) : MongoBusStop? {
        return realm.query<MongoBusStop>("code == $0", stopCode).find().firstOrNull()
    }

    // function to add a favourite stop (add route/mongo route parameter later??)
    // TODO @Jonathan: have this function take in a normal bus stop and then convert it accordingly
    fun addFavouriteStop(mongoBusStop: MongoBusStop, nickname: String) {
        viewModelScope.launch {
            //convert here: ...
            val newFavouriteStop = MongoFavouriteStop(nickname, mongoBusStop, null)
            realm.write {
                copyToRealm(newFavouriteStop, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }
}