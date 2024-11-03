package ca.wheresthebus.data

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime


// Requires some sort of coodinates
data class BusStop(
    val code : StopCode,
    val id : StopId,
    val name: String,
    val location : Location,
    val schedules : List<Schedule>) {

   @RequiresApi(Build.VERSION_CODES.O)
   //TODO: proof of concept
   fun getNextSchedules(time : LocalDateTime) : List<Schedule> {
        return schedules.filter {s -> s.day >= time.dayOfWeek}
    }
}
