package ca.wheresthebus.data.model

import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.TripId
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

// Represents routes
class Route : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    // route_id
    val id : RouteId? = null
    val shortName : String = "John"
    val longName : String = "Doe"
    val tripIds : RealmList<TripId> = realmListOf()
}