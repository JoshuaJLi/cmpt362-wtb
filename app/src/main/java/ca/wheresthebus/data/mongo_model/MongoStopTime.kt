package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

// Represents stop_times and trips
class MongoStopTime : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    @Index var stopId: Int = 0
    @Index var routeId : Int = 0
    @Index var serviceId : Int = 0
    var arrivalTimes: RealmList<MongoArrivalTime> = realmListOf()

    constructor() : this(ObjectId(), 0, 0, 0, realmListOf()) {
    }

    constructor(
        _id: ObjectId = ObjectId(),
        stopId: Int = 0,
        routeId: Int = 0,
        serviceId: Int = 0,
        arrivalTimes: RealmList<MongoArrivalTime> = realmListOf()
    ) {
        this._id = _id
        this.stopId = stopId
        this.routeId = routeId
        this.serviceId = serviceId
        this.arrivalTimes = arrivalTimes
    }
}
