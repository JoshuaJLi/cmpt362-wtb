package ca.wheresthebus.ui.trips

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.adapter.TripAdapter
import ca.wheresthebus.databinding.FragmentTripsBinding
import java.time.LocalDateTime

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

        listenForChanges()
        setUpAdapter()
        setUpFab()

//        AlarmService.scheduleTripNotifications(mainDBViewModel.getTrips(), requireContext())

        return root
    }

    private fun setUpAdapter() {
        activeTripsView = binding.recyclerActiveTrips
        inactiveTripsView = binding.recyclerInactiveTrips
        upcomingTripsView = binding.recyclerUpcomingTrips

        activeTripAdapter = TripAdapter()
        activeTripsView.apply {
            layoutManager = object : LinearLayoutManager(context)
            { override fun canScrollVertically() = false }
            adapter = activeTripAdapter
        }

        upcomingTripAdapter = TripAdapter()
        upcomingTripsView.apply {
            layoutManager = object : LinearLayoutManager(context)
            { override fun canScrollVertically() = false }
            adapter = upcomingTripAdapter
        }

        inactiveTripAdapter = TripAdapter()
        inactiveTripsView.apply {
            layoutManager = object : LinearLayoutManager(context)
            { override fun canScrollVertically() = false }
            adapter = inactiveTripAdapter
        }

    }

    private fun listenForChanges() {
        mainDBViewModel._allTripsList.observe(viewLifecycleOwner) {data ->
            val currentTime = LocalDateTime.now()

            val trips = data.orEmpty()
                .sortedBy { it.getClosestTime(currentTime) }
                .groupBy { trip ->
                    when {
                        trip.isActive(currentTime) -> TripType.ACTIVE
                        trip.isToday(currentTime) -> TripType.TODAY
                        else -> TripType.INACTIVE
                    }
                }

            trips[TripType.ACTIVE].orEmpty().let {
                if (it.isEmpty()) {
                    binding.labelActive.visibility = View.GONE
                }
                activeTripAdapter.updateData(it)
            }

            trips[TripType.TODAY].orEmpty().let {
                if (it.isEmpty()) {
                    binding.labelUpcomingTrips.visibility = View.GONE
                }
                upcomingTripAdapter.updateData(it)
            }

            trips[TripType.INACTIVE].orEmpty().let {
                if (it.isEmpty()) {
                    binding.labelAllTrips.visibility = View.GONE
                }
                inactiveTripAdapter.updateData(it)
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