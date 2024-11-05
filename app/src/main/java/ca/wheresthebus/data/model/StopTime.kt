package ca.wheresthebus.data.model

import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.ServiceId
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.TripId
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId
import java.time.LocalDateTime

// Represents stop_times and trips
class StopTime : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    //var arrivalTime: LocalDateTime? = null
    var arrivalTime: Long = 0L
    //stop_id
    var id : String = ""
    var routeId: String = ""
    var serviceId : String = ""
    var tripId : String = ""
}