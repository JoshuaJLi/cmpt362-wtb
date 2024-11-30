package ca.wheresthebus.data.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class Schedule(
    val day : DayOfWeek,
    val time : LocalTime
) {
    fun getNextTime(currentTime : LocalDateTime): LocalDateTime {
        return currentTime
        .with(TemporalAdjusters.nextOrSame(day))
        .toLocalDate().atTime(this.time)
    }
}
