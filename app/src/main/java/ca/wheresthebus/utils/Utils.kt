package ca.wheresthebus.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
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
        }
    }

    fun checkLocationPermission(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}