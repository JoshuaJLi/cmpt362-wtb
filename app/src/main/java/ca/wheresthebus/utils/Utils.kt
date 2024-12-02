package ca.wheresthebus.utils

object Utils {
    fun getHourPart(minutes: Int): Int {
        return minutes / 60
    }

    fun getMinPart(time: Int): Int {
        return time % 60
    }
}