package ca.wheresthebus.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.firstOrNull

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopAdapter: FavStopAdapter
    private lateinit var stopsView : RecyclerView

    private lateinit var mainDBViewModel: MainDBViewModel

    private val favouriteStopsList : ArrayList<FavouriteStop> = arrayListOf()
    //private val allBusStops : ArrayList<BusStop> = arrayListOf()
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
        return root
    }

    private fun setUpObservers() {
        // when the app opens, the favourite stops list is updated.
        mainDBViewModel._favouriteBusStopsList.observe(requireActivity()) { favouriteStops ->
            Log.d("favStopsListUpdated", favouriteStops.toString())
            favouriteStopsList.clear()
            favouriteStopsList.addAll(favouriteStops)
            stopAdapter.notifyDataSetChanged()
        }
    }

    suspend fun getFavStopsList(): List<MongoFavouriteStop> {
        return mainDBViewModel.mongoFavouriteStops.firstOrNull() ?: emptyList()
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
        swipeRefreshLayout.setOnRefreshListener {
            // do gtfs realtime api call
            refreshData()
        }
    }

    private fun refreshData() {
        // Simulate an API call
        swipeRefreshLayout.postDelayed({
            swipeRefreshLayout.isRefreshing = false
            Log.d("SwipeRefresh", "Data refresh complete")
        }, 500) // Simulated delay of .5 seconds
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}