package ca.wheresthebus.data.db

import android.app.Application
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import ca.wheresthebus.data.model.Schedule
import ca.wheresthebus.data.model.StopTime
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class MyMongoDBApp: Application() {
    companion object {
        lateinit var realm: Realm
    }

    override fun onCreate() {
        super.onCreate()
        realm = Realm.open(
            configuration = RealmConfiguration.create(
                schema = setOf(
                    BusStop::class,
                    FavouriteStop::class,
                    Route::class,
                    Schedule::class,
                    StopTime::class
                )
            )
        )
    }
}