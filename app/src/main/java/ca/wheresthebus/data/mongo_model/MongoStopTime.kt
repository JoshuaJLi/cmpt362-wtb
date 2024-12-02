package ca.wheresthebus.data.mongo_model

import ca.wheresthebus.data.StopTimestamp
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index

// Represents stop_times and trips
class MongoStopTime : RealmObject {
    @Index var stopId: Int = 0
    @Index var routeId : Int = 0
    @Index var serviceId : Int = 0
    var arrivalTimes: RealmList<StopTimestamp> = realmListOf() // timestamp of minutes since midnight (00:00)

    constructor() : this( 0, 0, 0, realmListOf()) {
    }

    constructor(
        stopId: Int = 0,
        routeId: Int = 0,
        serviceId: Int = 0,
        arrivalTimes: RealmList<StopTimestamp> = realmListOf()
    ) {
        this.stopId = stopId
        this.routeId = routeId
        this.serviceId = serviceId
        this.arrivalTimes = arrivalTimes
    }
}
