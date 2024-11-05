package ca.wheresthebus.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

class FavouriteStop : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    var nickname: String = ""
    var busStop: BusStop? = null
    var route: Route? = null
    @RequiresApi(Build.VERSION_CODES.O)
    fun getStops() : List<StopTime> {
        //return busStop.getBusTimes(route.id)
        return route?.let { busStop?.getBusTimes(it.id!!) } ?: emptyList()
    }
}