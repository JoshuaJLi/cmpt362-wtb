package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject

// Represents stop_times and trips
class MongoArrivalTime : RealmObject {
    var hour: Int = 0
    var minute: Int = 0

    constructor() : this(0, 0, ) {
    }

    constructor(
        hour: Int = 0,
        minute: Int = 0,
    ) {
        this.hour = hour
        this.minute = minute
    }
}
