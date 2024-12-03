package ca.wheresthebus.data.mongo_model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey

class MongoRoute : RealmObject {
    @PrimaryKey var id: String = ""
    @Index var shortName : String = ""
    @Index var longName : String = ""

    //Primary (EMPTY) constructor
    constructor() : this("", "", "") {
    }

    //Secondary constructor
    constructor(
        id: String = "",
        shortName: String = "",
        longName: String = ""
    ) {
        this.id = id
        this.shortName = shortName
        this.longName = longName
    }
}
