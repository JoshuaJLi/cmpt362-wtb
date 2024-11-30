package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class MongoFavouriteStop : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    var nickname: String = ""
    var mongoBusStop: MongoBusStop? = null
    var mongoRoute: MongoRoute? = null

    //Primary (EMPTY) constructor
    constructor() : this(ObjectId(),"", null, null) {
    }

    constructor(
        _id: ObjectId = ObjectId(),
        nickname: String = "",
        mongoBusStop: MongoBusStop? = null,
        mongoRoute: MongoRoute? = null
    ) {
        this._id = _id
        this.nickname = nickname
        this.mongoBusStop = mongoBusStop
        this.mongoRoute = mongoRoute
    }
}