package ca.wheresthebus.data.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.DayOfWeek
import java.time.LocalTime

class Schedule : RealmObject {
    @PrimaryKey val _id: ObjectId = ObjectId()
    val day : DayOfWeek? = null
    val time : LocalTime? = null
}
