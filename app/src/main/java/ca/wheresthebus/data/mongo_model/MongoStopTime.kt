package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

// Represents stop_times and trips
class MongoStopTime : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    //var arrivalTime: LocalDateTime? = null
    var arrivalTime: Long = 0L
    //stop_id
    var id : String = ""
    var routeId: String = ""
    var serviceId : String = ""
    var tripId : String = ""
}