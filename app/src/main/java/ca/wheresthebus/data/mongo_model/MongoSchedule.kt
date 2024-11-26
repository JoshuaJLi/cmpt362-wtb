package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class MongoSchedule : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    var day : String = ""
    //var time : LocalTime? = null
    var time: Long = 0L // store this as system.timeinmillis and convert?
}
