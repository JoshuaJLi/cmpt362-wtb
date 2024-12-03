package ca.wheresthebus.ui.trips

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.adapter.TripAdapter
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.databinding.FragmentTripsBinding
import ca.wheresthebus.service.AlarmService
import ca.wheresthebus.service.GtfsData
import ca.wheresthebus.service.GtfsRealtimeHelper
import ca.wheresthebus.service.GtfsStaticHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val activeTripsList: MutableList<ScheduledTrip> = mutableListOf()

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
        setUpSwipeRefresh()
        setUpSwipeToDelete()
        setUpFab()

        return root
    }

    private fun setUpAdapter() {
        activeTripsView = binding.recyclerActiveTrips
        inactiveTripsView = binding.recyclerInactiveTrips
        upcomingTripsView = binding.recyclerUpcomingTrips

        activeTripAdapter = TripAdapter(onDeleteSwipe = ::deleteTrip)
        activeTripsView.apply {
            layoutManager = object : LinearLayoutManager(context)
            { override fun canScrollVertically() = false }
            adapter = activeTripAdapter
        }

        upcomingTripAdapter = TripAdapter(onDeleteSwipe = ::deleteTrip)
        upcomingTripsView.apply {
            layoutManager = object : LinearLayoutManager(context)
            { override fun canScrollVertically() = false }
            adapter = upcomingTripAdapter
        }

        inactiveTripAdapter = TripAdapter(onDeleteSwipe = ::deleteTrip)
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
                activeTripsList.clear()
                activeTripsList.addAll(it)
                refreshBusTimes()
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

            AlarmService.scheduleTripNotifications(data, requireContext())
        }

//        tripsViewModel.busTimes.observe(requireActivity()) {
//            activeTripAdapter.
//        }
    }

    private fun setUpFab() {
        binding.fabNewTrip.setOnClickListener {
            val intent = Intent(context, AddTripsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setUpSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getDimensionPixelSize(R.dimen.swipe_refresh_trigger_distance))
        swipeRefreshLayout.setOnRefreshListener {
            refreshBusTimes()
        }
    }

    private fun refreshBusTimes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert all favorite stops to pairs list and do GTFS realtime call
                activeTripsList.flatMap { it.stops }
                val stopRoutePairs = activeTripsList.flatMap { it.stops }.map { stop ->
                    StopId(stop.busStop.id.value) to RouteId(stop.route.id.value)
                }

                val realtime = GtfsRealtimeHelper.getBusTimes(stopRoutePairs)
                val static = GtfsStaticHelper.getBusTimes(stopRoutePairs)

                lifecycleScope.launch(Dispatchers.Main) {
                    tripsViewModel.busTimes.value = GtfsData.combine(static, realtime)
                }

                swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error refreshing bus times", e)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setUpSwipeToDelete() {
        // Add the swipe handlers to each recycler view
        ItemTouchHelper(activeTripAdapter.getSwipeHandler())
            .attachToRecyclerView(
            activeTripsView
        )
        ItemTouchHelper(upcomingTripAdapter.getSwipeHandler())
            .attachToRecyclerView(
            upcomingTripsView
        )
        ItemTouchHelper(inactiveTripAdapter.getSwipeHandler())
            .attachToRecyclerView(
            inactiveTripsView
        )
    }

    private fun deleteTrip(trip: ScheduledTrip) {
        mainDBViewModel.deleteScheduledTrip(trip.id)
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