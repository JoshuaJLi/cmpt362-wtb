package ca.wheresthebus.ui.home

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.TripId
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.data.mongo_model.MongoFavouriteStop
import ca.wheresthebus.databinding.FragmentHomeBinding
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.flow.firstOrNull

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopAdapter: FavStopAdapter
    private lateinit var stopsView : RecyclerView

    private lateinit var mainDBViewModel: MainDBViewModel

    val busStops : Array<BusStop> = arrayOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainDBViewModel = ViewModelProvider(requireActivity()).get(MainDBViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setUpAdapter()
        setUpFab()
        return root
    }

    suspend fun getFavStopsList(): List<MongoFavouriteStop> {
        return mainDBViewModel.mongoFavouriteStops.firstOrNull() ?: emptyList()
    }

    private fun setUpFab() {
        binding.fabNewFav.setOnClickListener {
            AddFavBottomSheet().show(parentFragmentManager, AddFavBottomSheet.TAG)
            val newLocation = Location("passive")
            newLocation.latitude = (49.0123)
            newLocation.longitude = (-123.2354)
            val testListTripIds = arrayListOf<TripId>(TripId("11"), TripId("12"), TripId("14"))
            val route1 = Route(RouteId("1"), "PEE", "Number 1", testListTripIds)
            val route2 = Route(RouteId("2"), "POO", "Number 2", testListTripIds)
            val busStop = BusStop(StopId("12345"), StopCode("34567"), "Pee St @ Poo Ave", newLocation, arrayListOf(route1, route2))
            mainDBViewModel.insertBusStop(busStop)
            val test = mainDBViewModel.getBusStopByCode("34567")?.name.toString()
            Log.d("favStopQueryTest", test)
        }
    }

    private fun setUpAdapter() {
        stopAdapter = FavStopAdapter(busStops)
        stopsView = binding.recyclerFavStops

        stopsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}