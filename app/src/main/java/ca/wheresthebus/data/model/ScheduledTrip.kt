package ca.wheresthebus.data.model

import ca.wheresthebus.data.IntentRequestCode
import ca.wheresthebus.data.ScheduledTripId
import io.realm.kotlin.internal.platform.currentTime
import org.mongodb.kbson.ObjectId
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

data class ScheduledTrip(
    val id: ScheduledTripId,
    val requestCode : IntentRequestCode = IntentRequestCode(currentTime().nanosecondsOfSecond),
    val nickname: String,
    val stops: ArrayList<FavouriteStop>,
    val activeTimes: ArrayList<Schedule>,
    val duration: Duration = Duration.ofMinutes(60)
) {
    fun isActive(currentTime: LocalDateTime): Boolean {
        activeTimes.forEach {
            val begin = it.getNextTime(currentTime)
            val end = begin.plus(duration)

            if (currentTime.isAfter(begin) && currentTime.isBefore(end)) {
                return true
            }

        }

        return false
    }

    fun isToday(currentTime: LocalDateTime): Boolean {
        activeTimes.forEach {
            val nextTime = it.getNextTime(currentTime)
            if (nextTime.dayOfWeek == currentTime.dayOfWeek &&
                currentTime.isBefore(nextTime)) {
                return true
            }
        }

        return false
    }

    fun getClosestTime(currentTime: LocalDateTime): LocalDateTime {
        return activeTimes.minOf {
            var closest = it.getNextTime(currentTime)

            if (currentTime.isAfter(closest)) {
                closest = closest.with(TemporalAdjusters.next(it.day))
            }
            return@minOf closest
        }
    }
}