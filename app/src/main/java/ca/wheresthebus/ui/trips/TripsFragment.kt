package ca.wheresthebus.ui.trips

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.adapter.TripAdapter
import ca.wheresthebus.data.model.Schedule
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.databinding.FragmentTripsBinding
import ca.wheresthebus.service.AlarmService
import ca.wheresthebus.service.LiveNotificationService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private lateinit var tripsViewModel : TripsViewModel
    private lateinit var mainDBViewModel: MainDBViewModel

    private lateinit var activeTripAdapter: TripAdapter
    private lateinit var upcomingTripAdapter: TripAdapter
    private lateinit var inactiveTripAdapter: TripAdapter

    private lateinit var activeTripsView : RecyclerView
    private lateinit var upcomingTripsView : RecyclerView
    private lateinit var inactiveTripsView : RecyclerView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        tripsViewModel = ViewModelProvider(this)[TripsViewModel::class.java]

        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        mainDBViewModel = ViewModelProvider(requireActivity())[MainDBViewModel::class]

        setUpAdapter()

        scheduleTripNotifications(mainDBViewModel.getTrips(), requireContext())

        return root
    }

    private fun setUpAdapter() {
        val currentTime = LocalDateTime.now()

        val trips = mainDBViewModel.getTrips()
            .sortedBy { it.getClosestTime(currentTime) }
            .groupBy { trip ->
                when {
                    trip.isActive(currentTime) -> TripType.ACTIVE
                    trip.isToday(currentTime) -> TripType.TODAY
                    else -> TripType.INACTIVE
                }
            }

        activeTripsView = binding.recyclerActiveTrips
        inactiveTripsView = binding.recyclerInactiveTrips
        upcomingTripsView = binding.recyclerUpcomingTrips

        trips[TripType.ACTIVE].orEmpty().let {
            if (it.isEmpty()) {
                binding.labelActive.visibility = View.GONE
            }
            activeTripAdapter = TripAdapter(it)

            activeTripsView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = activeTripAdapter
            }
        }

        trips[TripType.TODAY].orEmpty().let {
            if (it.isEmpty()) {
                binding.labelUpcomingTrips.visibility = View.GONE
            }
            upcomingTripAdapter = TripAdapter(it)

            upcomingTripsView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = upcomingTripAdapter
            }
        }

        trips[TripType.INACTIVE].orEmpty().let {
            if (it.isEmpty()) {
                binding.labelAllTrips.visibility = View.GONE
            }
            inactiveTripAdapter = TripAdapter(it)

            inactiveTripsView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = inactiveTripAdapter
            }
        }
    }

    private fun scheduleTripNotifications(trips : List<ScheduledTrip>, context: Context) {
        val sharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        var notificationId = sharedPreferences.getInt("notification_id", 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        clearNotifications(alarmManager, notificationId)
        notificationId = 0

        trips.forEach {trip ->
            val intent = Intent(context, AlarmService::class.java).apply {
                putStringArrayListExtra(LiveNotificationService.EXTRA_NICKNAMES, trip.stops.map { it.nickname }.toCollection(ArrayList()))
                putStringArrayListExtra(LiveNotificationService.EXTRA_STOP_IDS, trip.stops.map { it.busStop.id.value }.toCollection(ArrayList()))
                putStringArrayListExtra(LiveNotificationService.EXTRA_ROUTE_IDS, trip.stops.map { it.route.id.value }.toCollection(ArrayList()))
                putExtra(LiveNotificationService.EXTRA_DURATION, trip.duration.toMinutes())
                putExtra(LiveNotificationService.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(LiveNotificationService.EXTRA_TRIP_NICKNAME, trip.nickname)
            }

            val upcomingTimes = trip.activeTimes.map { it.getNextTime(LocalDateTime.now()) }

            upcomingTimes.forEach { time ->
                val pendingIntent =  PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                Log.d("AlarmService", "Alarm set with id $notificationId")
                notificationId++

//                alarmManager.setInexactRepeating(
//                    AlarmManager.RTC_WAKEUP,
//                    time.toEpochSecond(ZoneOffset.of(ZoneId.systemDefault().id)) * 1000,
//                    AlarmManager.INTERVAL_DAY * 7,
//                    pendingIntent
//                    )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + (1000 * 5),
                    pendingIntent
                )
            }
        }
        sharedPreferences.edit().putInt("notification_id", notificationId).apply()
    }

    private fun clearNotifications(alarmManager: AlarmManager, notificationId: Int) {
        for (id in 0 until notificationId) {
            val intent = Intent(context, AlarmService::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            Log.d("AlarmService", "Deleting alarm with id $id")

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        object TripType {
            const val ACTIVE = "active"
            const val INACTIVE = "inactive"
            const val TODAY = "today"
        }
    }
}