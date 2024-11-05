package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

// Each realm object has to have its own EMPTY constructor.
class MongoBusStop : RealmObject {
    //default required for realm objects
    @PrimaryKey var _id: ObjectId = ObjectId()
    var id: String = ""
    var code: String = ""
    var name: String = ""
    //var location: Location? = null,
    var lat: Double = 0.0
    var lng: Double = 0.0
    var nextBuses: RealmList<MongoStopTime> = realmListOf()
    var routes: RealmList<Route> = realmListOf()

    //Primary (EMPTY) constructor
    constructor() : this("", "", "", 0.0, 0.0, realmListOf(), realmListOf()) {

    }
    //Secondary constructor
    constructor(
        id: String = "",
        code: String = "",
        name: String = "",
        //var location: Location? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        nextBuses: RealmList<MongoStopTime> = realmListOf(),
        routes: RealmList<Route> = realmListOf()
    ) {
        this.id = id
        this.code = code
        this.name = name
        this.lat = lat
        this.lng = lng
        this.nextBuses = nextBuses
        this.routes = routes
    }
}