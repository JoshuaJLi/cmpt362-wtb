package ca.wheresthebus.ui.home

import android.content.Intent
import android.graphics.Canvas
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
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.databinding.FragmentHomeBinding
import ca.wheresthebus.service.GtfsData
import ca.wheresthebus.service.GtfsRealtimeHelper
import ca.wheresthebus.service.GtfsStaticHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                val index = favouriteStopsList.size
                favouriteStopsList.add(index, newFavStopsList.last())
                stopAdapter.notifyItemInserted(index)
                refreshBusTimes()
            }
            // Avoid refreshing bus times or notifying adapter view changes otherwise
            // Updates to adapter for deletion is handled in onSwiped()

            if (_binding == null) return@observe
            if (newFavStopsList.size == 0) {
                binding.layoutFavEmpty.visibility = View.VISIBLE
            } else {
                binding.layoutFavEmpty.visibility = View.GONE
            }
        }

        homeViewModel.busTimes.observe(requireActivity()){
            stopAdapter.updateBusTimes(it)
        }
    }

    private fun setUpFab() {
        val bottomSheet = AddFavBottomSheet()
            .assignAddFavourite { mainDBViewModel.insertFavouriteStop(it) }
        binding.fabNewFav.setOnClickListener {
                bottomSheet.show(parentFragmentManager, AddFavBottomSheet.TAG)
        }
    }

    private fun setUpAdapter() {
        stopAdapter = FavStopAdapter(
            favouriteStopsList,
            onMoreOptionsClick = { view, stop ->
                showPopupMenu(view, stop)
            }
        )
        stopsView = binding.recyclerFavStops

        stopsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopAdapter
        }
    }

    private fun showPopupMenu(view: View, stop: FavouriteStop) {
        val popupMenu = PopupMenu(view.context, view)
        popupMenu.inflate(R.menu.fav_bus_overflow_menu)
        popupMenu.gravity = Gravity.END

        // change delete to red
        val deleteItem = popupMenu.menu.findItem(R.id.option_delete)
        val spannableTitle = SpannableString(deleteItem.title)
        spannableTitle.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(view.context, android.R.color.holo_red_dark)),
            0,
            spannableTitle.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        deleteItem.title = spannableTitle

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.option_open_schedule -> {
                    val url = "https://www.translink.ca/schedules-and-maps/stop/${stop.busStop.code.value}/schedule"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                }
                R.id.option_delete -> {
                    val position = favouriteStopsList.indexOf(stop)
                    if (position != -1) {
                        deleteFavouriteStop(stop)
                        favouriteStopsList.removeAt(position)
                        stopAdapter.notifyItemRemoved(position)
                    }
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
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
                val itemView = viewHolder.itemView.findViewById<View>(R.id.fav_card_foreground)
                itemView.translationX = dX
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(stopsView)
    }

    private fun refreshBusTimes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert all favorite stops to pairs list and do GTFS realtime call
                val stopRoutePairs = favouriteStopsList.map { stop ->
                    StopId(stop.busStop.id.value) to RouteId(stop.route.id.value)
                }

                val realtime = GtfsRealtimeHelper.getBusTimes(stopRoutePairs)
                val static = GtfsStaticHelper.getBusTimes(stopRoutePairs)

                lifecycleScope.launch(Dispatchers.Main) {
                    homeViewModel.busTimes.value = GtfsData.combine(static, realtime)
                }

                swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error refreshing bus times", e)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun deleteFavouriteStop(favStop: FavouriteStop) {
        if (favouriteStopsList.size == 1) {
            Log.d("HomeFragment", "Deleting last favourite stop")
            binding.layoutFavEmpty.visibility = View.VISIBLE
        }
        mainDBViewModel.deleteFavouriteStop(favStop._id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}