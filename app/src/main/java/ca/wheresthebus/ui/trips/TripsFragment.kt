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
        setUpFab()

//        AlarmService.scheduleTripNotifications(mainDBViewModel.getTrips(), requireContext())

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

    private fun setUpFab() {
        binding.fabNewTrip.setOnClickListener {
            val intent = Intent(context, AddTripsActivity::class.java)
            startActivity(intent)
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