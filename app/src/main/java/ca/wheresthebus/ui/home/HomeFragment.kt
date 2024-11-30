package ca.wheresthebus.ui.home

import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.databinding.FragmentHomeBinding
import ca.wheresthebus.service.GtfsRealtimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var stopAdapter: FavStopAdapter
    private lateinit var stopsView : RecyclerView

    private lateinit var mainDBViewModel: MainDBViewModel

    private val favouriteStopsList : ArrayList<FavouriteStop> = arrayListOf()
    private lateinit var modelFactory: ModelFactory

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainDBViewModel = ViewModelProvider(requireActivity()).get(MainDBViewModel::class.java)
        modelFactory = ModelFactory()
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setUpAdapter()
        setUpFab()
        setUpObservers()
        setUpSwipeRefresh()
        setUpSwipeToDelete()
        return root
    }

    private fun setUpObservers() {
        // Handles data sync between viewmodel and view
        mainDBViewModel._favouriteBusStopsList.observe(requireActivity()) { newFavStopsList ->
            // Initial data load -> notify entire dataset is new
            if (newFavStopsList.size > 0 && favouriteStopsList.size == 0) {
                favouriteStopsList.addAll(newFavStopsList)
                stopAdapter.notifyDataSetChanged()
                refreshBusTimes()
            }
            // A new stop was added -> only notify last index
            else if (newFavStopsList.size == favouriteStopsList.size + 1) {
                favouriteStopsList.add(newFavStopsList.last())
                stopAdapter.notifyItemInserted(favouriteStopsList.size)
                refreshBusTimes()
            }
            // Avoid refreshing bus times or notifying adapter view changes otherwise
            // Updates to adapter for deletion is handled in onSwiped()
        }

        homeViewModel.busTimes.observe(requireActivity()){
            stopAdapter.updateBusTimes(it)
        }
    }

    private fun setUpFab() {
        binding.fabNewFav.setOnClickListener {
            AddFavBottomSheet().show(parentFragmentManager, AddFavBottomSheet.TAG)
        }
    }

    private fun setUpAdapter() {
        stopAdapter = FavStopAdapter(favouriteStopsList)
        stopsView = binding.recyclerFavStops

        stopsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopAdapter
        }
    }

    private fun setUpSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setDistanceToTriggerSync(resources.getDimensionPixelSize(R.dimen.swipe_refresh_trigger_distance))
        swipeRefreshLayout.setOnRefreshListener {
            refreshBusTimes()
        }
    }

    private fun setUpSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                swipeRefreshLayout.isEnabled = false
                return false
            }

            // Delete on swipe left of card
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT){
                    val position = viewHolder.adapterPosition
                    val stopToDelete = favouriteStopsList[position]
                    deleteFavouriteStop(stopToDelete)
                    favouriteStopsList.removeAt(position)
                    stopAdapter.notifyItemRemoved(position)
                    swipeRefreshLayout.isEnabled = true
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // only move the foreground?
                val itemView = viewHolder.itemView.findViewById<View>(R.id.fav_card_view)
                itemView.translationX = dX
            }

            // user has to swipe 75% the width of the view to delete
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.75f
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(stopsView)
    }

    private fun refreshBusTimes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Create a map of favourite stops to their next bus times
                val busTimesMap = mutableMapOf<StopCode, List<Duration>>()
                favouriteStopsList.forEach { stop ->
                    val nextBusTimes = GtfsRealtimeHelper.getBusTimes(
                        StopId(stop.busStop.id.value),
                        RouteId(stop.route.id.value))
                    busTimesMap[stop.busStop.code] = nextBusTimes
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    homeViewModel.busTimes.value = busTimesMap
                }

                swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error refreshing bus times", e)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun deleteFavouriteStop(favStop: FavouriteStop) {
        mainDBViewModel.deleteFavouriteStop(favStop._id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}