package ca.wheresthebus.data

// Trying out tiny types idiom here to use less primitives
// see https://kotlinlang.org/docs/inline-classes.html

@JvmInline
value class StopCode(val id : String)

@JvmInline
value class StopId(val id : String)