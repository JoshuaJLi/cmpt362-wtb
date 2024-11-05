package ca.wheresthebus.ui.home

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.ServiceId
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.Schedule
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.TripId
import ca.wheresthebus.data.model.StopTime
import ca.wheresthebus.databinding.FragmentHomeBinding
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopAdapter: FavStopAdapter
    private lateinit var stopsView : RecyclerView

    private val homeViewModel: HomeViewModel by viewModels()

    val busStops : Array<BusStop> = arrayOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        val homeViewModel =
//            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        setUpAdapter()
        setUpFab()
        return root
    }

    private fun setUpFab() {
        binding.fabNewFav.setOnClickListener {
            AddFavBottomSheet().show(parentFragmentManager, AddFavBottomSheet.TAG)
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