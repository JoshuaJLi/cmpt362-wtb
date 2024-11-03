package ca.wheresthebus.ui.home

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.BusStop
import ca.wheresthebus.data.Schedule
import ca.wheresthebus.data.StopId
import ca.wheresthebus.databinding.FragmentHomeBinding
import java.time.DayOfWeek
import java.time.LocalTime

class HomeFragment : Fragment() {

    // TODO: remove when there's actual data
    @RequiresApi(Build.VERSION_CODES.O)
    val busStops = arrayOf(
        BusStop(
            code = StopCode("ABC123"),
            id = StopId("stop_123"),
            name = "Main Street Stop",
            location = Location("Main Street Stop").apply { // Using android.location.Location
                latitude = 37.7749
                longitude = -122.4194
            },
            schedules = listOf(
                Schedule(day = DayOfWeek.MONDAY, time = LocalTime.of(10, 0)),
                Schedule(day = DayOfWeek.MONDAY, time = LocalTime.of(10, 30)),
                Schedule(day = DayOfWeek.MONDAY, time = LocalTime.of(11, 0)),
            )
        ),
        BusStop(
            code = StopCode("DEF456"),
            id = StopId("stop_456"),
            name = "Park Avenue Stop",
            location = Location("Park Avenue Stop").apply { // Using android.location.Location
                latitude = 34.0522
                longitude = -118.2437
            },
            schedules = listOf(
                Schedule(day = DayOfWeek.MONDAY, time = LocalTime.of(10, 0)),
                Schedule(day = DayOfWeek.MONDAY, time = LocalTime.of(10, 30)),
                Schedule(day = DayOfWeek.MONDAY, time = LocalTime.of(11, 0)),
            )
        ),
        // ... add more bus stops with schedules
    )

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopAdapter: FavStopAdapter
    private lateinit var stopsView : RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

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