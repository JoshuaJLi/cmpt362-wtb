package ca.wheresthebus.data.model

import java.time.DayOfWeek
import java.time.LocalTime

data class Schedule(
    val day : DayOfWeek,
    val time : LocalTime
)
