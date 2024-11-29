package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class MongoSchedule : RealmObject {
    @PrimaryKey var id: String = String()
    var day : Int = 0
    //var time : LocalTime? = null
    var hour : Int = 0
    var minute : Int = 0
}
