package ca.wheresthebus.utils

import android.content.Context
import ca.wheresthebus.R
import ca.wheresthebus.data.json.JsonRoute
import ca.wheresthebus.data.json.JsonStop
import ca.wheresthebus.data.json.JsonStopTime
import ca.wheresthebus.data.mongo_model.MongoArrivalTime
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import ca.wheresthebus.data.mongo_model.MongoStopTime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.toRealmList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StaticDataLoadHelper {
    // Populates the Realm database with json data from files in resources/raw
    suspend fun populateRealmDatabase(context: Context, realm: Realm) {
        loadStopsAndRoutes(context, realm)
        loadStopTimes(context, realm)
    }

    private suspend fun loadStopTimes(
        context: Context,
        realm: Realm
    ) = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        println("MongoStopTime: Starting work")

        val gson = Gson()
        val reader =
            JsonReader(context.resources.openRawResource(R.raw.stop_times).bufferedReader())

        realm.write {
            reader.use { r ->
                r.beginArray()

                while (r.hasNext()) {
                    val jsonStopTime = gson.fromJson<JsonStopTime>(r, JsonStopTime::class.java)

                    // build list of MongoArrivalTimes
                    val arrivalTimes: List<MongoArrivalTime> =
                        jsonStopTime.arrival_times.map { time ->
                            MongoArrivalTime(
                                hour = time.h,
                                minute = time.m,
                            )
                        }

                    // build new MongoStopTime object
                    val mongoStopTime = MongoStopTime(
                        stopId = jsonStopTime.stop_id,
                        routeId = jsonStopTime.route_id,
                        serviceId = jsonStopTime.service_id,
                        arrivalTimes = arrivalTimes.toRealmList()
                    )

                    // push mongoStopTime to a producer/consumer
                    copyToRealm(mongoStopTime)
                }
                r.endArray()
            }
        }
        println("MongoStopTime: Finished to writing stop time data to database")
        val end = System.currentTimeMillis()
        println("Populating database with stop time data took ${(end - start) / 1000} seconds")
    }

    private suspend fun loadStopsAndRoutes(
        context: Context,
        realm: Realm
    ) {
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            // read json file contents
            val gson = Gson()
            val stopsJsonContent = context.resources.openRawResource(R.raw.stops).bufferedReader()
                .use { it.readText() }
            val routesJsonContent = context.resources.openRawResource(R.raw.routes).bufferedReader()
                .use { it.readText() }

            // build JSON objects from file contents
            val stopType = object : TypeToken<List<JsonStop>>() {}.type
            val routeType = object : TypeToken<List<JsonRoute>>() {}.type
            val stops: List<JsonStop> = gson.fromJson(stopsJsonContent, stopType)
            val routes: List<JsonRoute> = gson.fromJson(routesJsonContent, routeType)

            // Add to Realm
            realm.write {
                // add Routes
                routes.forEach { obj ->
                    val realmRoute = MongoRoute(
                        id = obj.id.toString(),
                        shortName = obj.shortName,
                        longName = obj.longName
                    )

                    copyToRealm(realmRoute)
                }

                // add Stops
                stops.forEach { obj ->
                    // get all Route objects associated with this stop
                    val stopRoutes = obj.route_ids.mapNotNull { id ->
                        query<MongoRoute>(
                            "id == $0",
                            id.toString()
                        ).first().find()
                    }

                    val realmStop = MongoBusStop(
                        id = obj.id.toString(),
                        code = obj.code.toString(),
                        name = obj.name,
                        lat = obj.lat,
                        lng = obj.lng,
                        mongoRoutes = stopRoutes.toRealmList()
                    )

                    copyToRealm(realmStop)
                }
            }
            val end = System.currentTimeMillis()
            println("Populating database with stop and route data took ${(end - start) / 1000} seconds")
        }
    }
}