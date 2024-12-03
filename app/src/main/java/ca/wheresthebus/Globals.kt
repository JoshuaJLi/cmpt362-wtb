package ca.wheresthebus

object Globals {
    const val BUS_RETRIEVAL_MAX = 3
    const val SERVICE_ID_MON_TO_FRI = 1
    const val SERVICE_ID_SAT = 2
    const val SERVICE_ID_SUN = 3

    const val LAT_LNG_DEGREE_BUFFER = 0.003 // this is around 300m
    const val NEARBY_DISTANCE_THRESHOLD = 300.0
    const val NEARBY_ZOOM_LEVEL = 16f

    const val LOCATION_UPDATE_MINIMUM_INTERVAL = 20L
    const val LOCATION_UPDATE_MAXIMUM_AGE = 500L

}