package ca.wheresthebus.data.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.DayOfWeek
import java.time.LocalTime

class Schedule : RealmObject {
    @PrimaryKey var _id: ObjectId = ObjectId()
    var day : String = ""
    //var time : LocalTime? = null
    var time: Long = 0L // store this as system.timeinmillis and convert?
}
