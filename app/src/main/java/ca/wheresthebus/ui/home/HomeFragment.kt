package ca.wheresthebus.ui.home

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.BusCode
import ca.wheresthebus.data.BusStop
import ca.wheresthebus.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    // TODO: remove when there's actual data
    private val busStops = arrayOf(
        BusStop(
            BusCode("SF-123"),
            "Main Street Station",
            Location("dummyprovider").apply {
                latitude = 37.7749
                longitude = -122.4194
            }
        ),
        BusStop(
            BusCode("SF-456"),
            "Market Street Station",
            Location("dummyprovider").apply {
                latitude = 37.7882
                longitude = -122.4064
            }
        ),
        BusStop(
            BusCode("SF-789"),
            "Civic Center Station",
            Location("dummyprovider").apply {
                latitude = 37.7795
                longitude = -122.4137
            }
        ),
        // ... add more bus stops as needed
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