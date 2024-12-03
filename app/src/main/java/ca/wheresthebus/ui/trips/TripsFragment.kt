package ca.wheresthebus.ui.trips

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.adapter.TripAdapter
import ca.wheresthebus.adapter.TripAdapter.ActiveTripViewHolder
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.UpcomingTime
import ca.wheresthebus.data.model.FavouriteStop
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
    private val tripsViewModel: TripsViewModel by viewModels<TripsViewModel>()
    private val mainDBViewModel: MainDBViewModel by viewModels<MainDBViewModel>()

    private lateinit var activeTripAdapter: TripAdapter
    private lateinit var upcomingTripAdapter: TripAdapter
    private lateinit var inactiveTripAdapter: TripAdapter

    private lateinit var activeTripsView: RecyclerView
    private lateinit var upcomingTripsView: RecyclerView
    private lateinit var inactiveTripsView: RecyclerView

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
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setUpAdapter()
        listenForChanges()
        setUpSwipeRefresh()
        setUpSwipeToDelete()
        setUpFab()

        return root
    }

    private fun setUpAdapter() {
        activeTripsView = binding.recyclerActiveTrips
        inactiveTripsView = binding.recyclerInactiveTrips
        upcomingTripsView = binding.recyclerUpcomingTrips

        activeTripAdapter =
            TripAdapter(onDeleteSwipe = ::deleteTrip, onMoreOptionsClick = ::showPopupMenu)
        activeTripsView.apply {
            layoutManager = object : LinearLayoutManager(context) {
                override fun canScrollVertically() = false
            }
            adapter = activeTripAdapter
        }

        upcomingTripAdapter =
            TripAdapter(onDeleteSwipe = ::deleteTrip, onMoreOptionsClick = ::showPopupMenu)
        upcomingTripsView.apply {
            layoutManager = object : LinearLayoutManager(context) {
                override fun canScrollVertically() = false
            }
            adapter = upcomingTripAdapter
        }

        inactiveTripAdapter =
            TripAdapter(onDeleteSwipe = ::deleteTrip, onMoreOptionsClick = ::showPopupMenu)
        inactiveTripsView.apply {
            layoutManager = object : LinearLayoutManager(context) {
                override fun canScrollVertically() = false
            }
            adapter = inactiveTripAdapter
        }

    }

    private fun showPopupMenu(view: View, trip: ScheduledTrip) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.inflate(R.menu.trip_overflow_menu)
        popupMenu.gravity = Gravity.END

        val deleteItem = popupMenu.menu.findItem(R.id.option_delete)
        val spannableTitle = SpannableString(deleteItem.title)
        spannableTitle.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    view.context,
                    android.R.color.holo_red_dark
                )
            ),
            0,
            spannableTitle.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        deleteItem.title = spannableTitle

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.option_delete -> {
                    deleteTrip(trip)
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun listenForChanges() {
        mainDBViewModel._allTripsList.observe(viewLifecycleOwner) { data ->
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
                activeTripAdapter.updateData(it)
                activeTripsList.clear()
                activeTripsList.addAll(it)
                if (it.isEmpty()) {
                    binding.labelActive.visibility = View.GONE
                } else {
                    binding.labelActive.visibility = View.VISIBLE
                }
                refreshBusTimes()
                AlarmService.startTripNow(it, requireContext())
            }

            trips[TripType.TODAY].orEmpty().let {
                upcomingTripAdapter.updateData(it)
                if (it.isEmpty()) {
                    binding.labelUpcomingTrips.visibility = View.GONE
                } else {
                    binding.labelUpcomingTrips.visibility = View.VISIBLE
                }
            }

            trips[TripType.INACTIVE].orEmpty().let {
                inactiveTripAdapter.updateData(it)
                if (it.isEmpty()) {
                    binding.labelAllTrips.visibility = View.GONE
                } else {
                    binding.labelAllTrips.visibility = View.VISIBLE
                }
            }

            if (data.size > 0) {
                binding.layoutTripEmpty.visibility = View.GONE
            } else {
                binding.layoutTripEmpty.visibility = View.VISIBLE
            }
        }

        tripsViewModel.busTimes.observe(requireActivity()) {
            updateBusTimes(it)
        }
    }

    private fun updateBusTimes(busTimes: MutableMap<StopRequest, List<UpcomingTime>>) {
        for (i in 0..activeTripAdapter.itemCount) {
            val viewHolder =
                activeTripsView.findViewHolderForAdapterPosition(i) as? ActiveTripViewHolder
            viewHolder?.updateBusTimes(busTimes)
        }
    }

    private fun setUpFab() {
        binding.fabNewTrip.setOnClickListener {
            val intent = Intent(context, AddTripsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setUpSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getDimensionPixelSize(R.dimen.swipe_refresh_trip_trigger_distance))
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
        ItemTouchHelper(activeTripAdapter.getSwipeHandler(swipeRefreshLayout))
            .attachToRecyclerView(
                activeTripsView
            )
        ItemTouchHelper(upcomingTripAdapter.getSwipeHandler(swipeRefreshLayout))
            .attachToRecyclerView(
                upcomingTripsView
            )
        ItemTouchHelper(inactiveTripAdapter.getSwipeHandler(swipeRefreshLayout))
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