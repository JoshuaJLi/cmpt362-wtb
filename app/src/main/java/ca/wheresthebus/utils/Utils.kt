package ca.wheresthebus.utils

import android.content.Context
import ca.wheresthebus.R
import ca.wheresthebus.data.json.JsonRoute
import ca.wheresthebus.data.json.JsonStop
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.toRealmList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Utils {
    // Populates the Realm database with json data from files in resources/raw
    suspend fun populateRealmDatabase(context: Context, realm: Realm) {
        withContext(Dispatchers.IO) {
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
                        id = obj.route_id.toString(), // should ids be ints??
                        shortName = obj.route_short_name,
                        longName = obj.route_long_name
                    )

                    copyToRealm(realmRoute)
                }

                // add Stops
                stops.forEach { obj ->
                    // get all Route objects associated with this stop
                    val stopRoutes = obj.route_id.mapNotNull { id ->
                        query<MongoRoute>(
                            "id == $0",
                            id.toString()
                        ).first().find()
                    }

                    val realmStop = MongoBusStop(
                        id = obj.stop_id.toString(),
                        code = obj.stop_code.toString(),
                        name = obj.stop_name,
                        lat = obj.stop_lat,
                        lng = obj.stop_lon,
                        mongoRoutes = stopRoutes.toRealmList()
                    )

                    copyToRealm(realmStop)
                }
            }
        }
    }
}