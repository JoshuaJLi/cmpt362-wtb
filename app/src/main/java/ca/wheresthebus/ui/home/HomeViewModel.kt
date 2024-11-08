package ca.wheresthebus.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val realm = MyMongoDBApp.realm
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

//    fun showCourseDetails(course: Course) {
//        courseDetails = course
//    }
//
//    fun hideCourseDetails() {
//        courseDetails = null
//    }

    // function that creates the sample entries
    private fun createDummyEntries() {
        viewModelScope.launch {
            realm.write {
                // populate the viewmodel with the loaded in BusStops/FavStops/wtv when needed?
            }
        }
    }

    fun deleteFavStop() {
        viewModelScope.launch {
            realm.write {
                //TODO: adapt later
//                val course = courseDetails ?: return@write
//                val latestCourse = findLatest(course) ?: return@write
//                delete(latestCourse)
//
//                courseDetails = null
            }
        }
    }

    fun insertFavStop(mongoFavouriteStop: MongoFavouriteStop) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(mongoFavouriteStop, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    fun insertBusStop(newStop: MongoBusStop) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(newStop, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }
}