package ca.wheresthebus.data

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

@JvmInline
value class IntentRequestCode(val value : Int)

typealias StopRequest = Pair<StopId, RouteId>
