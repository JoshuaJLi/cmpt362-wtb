package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class MongoFavouriteStop : RealmObject {
    //@PrimaryKey var _id: ObjectId = ObjectId()
    //todo: do the inheritance thingy from tutorial i watched to connect; favourite stops have to have a monngo bus stop
    @PrimaryKey var nickname: String = ""
    var mongoBusStop: MongoBusStop? = null
    var mongoRoute: MongoRoute? = null

    //Primary (EMPTY) constructor
    constructor() : this("", null, null) {
    }

    constructor(
        nickname: String = "",
        mongoBusStop: MongoBusStop? = null,
        mongoRoute: MongoRoute? = null
    ) {
        this.nickname = nickname
        this.mongoBusStop = mongoBusStop
        this.mongoRoute = mongoRoute
    }
}