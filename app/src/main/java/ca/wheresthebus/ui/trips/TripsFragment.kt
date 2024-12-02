package ca.wheresthebus.ui.trips

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
            layoutManager = LinearLayoutManager(context)
            adapter = activeTripAdapter
        }

        upcomingTripAdapter = TripAdapter()
        upcomingTripsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = upcomingTripAdapter
        }

        inactiveTripAdapter = TripAdapter()
        inactiveTripsView.apply {
            layoutManager = LinearLayoutManager(context)
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

//    private fun setUpSwipeToDelete() {
//        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
//            override fun onMove(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                swipeRefreshLayout.isEnabled = false
//                return false
//            }
//
//            // Delete on swipe left of card
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                if (direction == ItemTouchHelper.LEFT){
//                    val position = viewHolder.adapterPosition
//                    val stopToDelete = favouriteStopsList[position]
//                    deleteFavouriteStop(stopToDelete)
//                    favouriteStopsList.removeAt(position)
//                    stopAdapter.notifyItemRemoved(position)
//                    swipeRefreshLayout.isEnabled = true
//                }
//            }
//
//            override fun onChildDraw(
//                c: Canvas,
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                dX: Float,
//                dY: Float,
//                actionState: Int,
//                isCurrentlyActive: Boolean
//            ) {
//                // only move the foreground?
//                val itemView = viewHolder.itemView.findViewById<View>(R.id.fav_card_view)
//                itemView.translationX = dX
//            }
//
//            // user has to swipe 75% the width of the view to delete
//            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
//                return 0.75f
//            }
//        }
//        val itemTouchHelper = ItemTouchHelper(swipeHandler)
//        itemTouchHelper.attachToRecyclerView(stopsView)
//    }

    companion object {
        object TripType {
            const val ACTIVE = "active"
            const val INACTIVE = "inactive"
            const val TODAY = "today"
        }
    }
}