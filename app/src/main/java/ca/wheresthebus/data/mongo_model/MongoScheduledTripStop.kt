package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

// needed so favourite stop from trips don't get mixed up with regular favourite stops
class MongoScheduledTripStop: RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var nickname: String = ""
    var mongoBusStop: MongoBusStop? = null
    var mongoRoute: MongoRoute? = null
}