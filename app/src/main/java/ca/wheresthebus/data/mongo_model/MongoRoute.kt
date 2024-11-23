package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

// Represents routes
class MongoRoute : RealmObject {
    // route_id
    @PrimaryKey var id: String = ""
    var shortName : String = "John"
    var longName : String = "Doe"
}