package ca.wheresthebus.data.db

import android.app.Application
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.data.mongo_model.MongoRoute
import ca.wheresthebus.data.mongo_model.MongoSchedule
import ca.wheresthebus.data.mongo_model.MongoScheduledTrip
import ca.wheresthebus.data.mongo_model.MongoStopTime
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class MyMongoDBApp: Application() {
    companion object {
        val realm: Realm by lazy {
            Realm.open(
                configuration = RealmConfiguration.create(
                    schema = setOf(
                        MongoBusStop::class,
                        MongoFavouriteStop::class,
                        MongoRoute::class,
                        MongoSchedule::class,
                        MongoStopTime::class,
                        MongoScheduledTrip::class
                    )
                )
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}