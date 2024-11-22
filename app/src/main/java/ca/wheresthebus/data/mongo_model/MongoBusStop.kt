package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

// Each realm object has to have its own EMPTY constructor.
class MongoBusStop : RealmObject {
    //default required for realm objects
    //@PrimaryKey var _id: ObjectId = ObjectId()
    @PrimaryKey var id: String = ""
    var code: String = ""
    var name: String = ""
    //var location: Location? = null,
    var lat: Double = 0.0
    var lng: Double = 0.0
    var mongoRoutes: RealmList<MongoRoute> = realmListOf()

    //Primary (EMPTY) constructor
    constructor() : this("", "", "", 0.0, 0.0, realmListOf()) {

    }
    //Secondary constructor
    constructor(
        id: String = "",
        code: String = "",
        name: String = "",
        //var location: Location? = null,
        lat: Double = 0.0,
        lng: Double = 0.0,
        mongoRoutes: RealmList<MongoRoute> = realmListOf()
    ) {
        this.id = id
        this.code = code
        this.name = name
        this.lat = lat
        this.lng = lng
        this.mongoRoutes = mongoRoutes
    }
}