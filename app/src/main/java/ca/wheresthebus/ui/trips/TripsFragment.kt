package ca.wheresthebus.ui.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

    private lateinit var tripAdapter: TripAdapter
    private lateinit var tripsView : RecyclerView

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

        setUpAdapter()
        return root
    }

    private fun setUpAdapter() {
        val currentTime = LocalDateTime.now()

        val trips = mainDBViewModel.getTrips()
            .sortedBy { it.getClosestTime(currentTime) }
            .partition { it.isActive(currentTime) }
            .let { (active, inactive) -> active + inactive }

        tripAdapter = TripAdapter(trips)
        tripsView = binding.recyclerTrips

        tripsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tripAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}