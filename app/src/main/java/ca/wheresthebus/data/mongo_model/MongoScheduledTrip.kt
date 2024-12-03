package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class MongoScheduledTrip : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var requestCode = 0
    var nickname : String = String()
    var stops : RealmList<MongoScheduledTripStop> = realmListOf()
    var activeTimes: RealmList<MongoSchedule> = realmListOf()
    var duration : Long = 0L

}