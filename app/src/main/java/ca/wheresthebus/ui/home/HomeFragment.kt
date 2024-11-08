package ca.wheresthebus.ui.home

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.databinding.FragmentHomeBinding
import io.realm.kotlin.ext.realmListOf

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopAdapter: FavStopAdapter
    private lateinit var stopsView : RecyclerView

    private lateinit var homeViewModel: HomeViewModel

    val busStops : Array<BusStop> = arrayOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setUpAdapter()
        setUpFab()
        return root
    }

    private fun setUpFab() {
        binding.fabNewFav.setOnClickListener {
            AddFavBottomSheet().show(parentFragmentManager, AddFavBottomSheet.TAG)
            val newLocation = Location("passive")
            newLocation.latitude = (49.0123)
            newLocation.longitude = (-123.2354)
            //val busStop = BusStop(StopId("12345"), StopCode("34567"), "Pee St @ Poo Ave", newLocation, realmListOf(), realmListOf())
            val mongoBusStop = MongoBusStop("12345", "34567", "Pee St @ Poo Ave", 49.0123, -123.2354, realmListOf(), realmListOf())
            homeViewModel.insertBusStop(mongoBusStop)
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