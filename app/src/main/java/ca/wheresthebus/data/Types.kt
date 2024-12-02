package ca.wheresthebus.data

import java.time.Duration

// Trying out tiny types idiom here to use less primitives
// see https://kotlinlang.org/docs/inline-classes.html

@JvmInline
value class StopCode(val value : String)

@JvmInline
value class StopId(val value : String)

@JvmInline
value class RouteId(val value : String)

@JvmInline
value class ServiceId(val value : String)

@JvmInline
value class TripId(val value : String)

@JvmInline
value class ScheduledTripId(val value : String)

typealias StopRequest = Pair<StopId, RouteId>

typealias StopTimestamp = Int

data class UpcomingTime(val isRealtime: Boolean, val duration: Duration)