package ca.wheresthebus.data

// Trying out tiny types idiom here to use less primitives
// see https://kotlinlang.org/docs/inline-classes.html

@JvmInline
value class BusCode(val id : String)