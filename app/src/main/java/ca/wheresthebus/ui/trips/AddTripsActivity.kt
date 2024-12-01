package ca.wheresthebus.ui.trips

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.adapter.AddTripTimeAdapter
import ca.wheresthebus.databinding.ActivityAddTripsBinding
import java.time.DayOfWeek
import java.time.LocalTime

class AddTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripsBinding

    private lateinit var addTripsViewModel: AddTripsViewModel

    private lateinit var addTripsView : RecyclerView

    private lateinit var tripTimeAdapter: AddTripTimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_trips)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityAddTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addTripsViewModel = ViewModelProvider(this)[AddTripsViewModel::class.java]

        setUpTripStopsAdapter()
        setUpTripTimeAdapter()

        setUpAddStop()
        setUpAddTime()
        setUpCancel()
        setUpSave()
    }

    private fun setUpTripStopsAdapter() {
        // do trip stops stuff
    }

    private fun setUpTripTimeAdapter() {
        tripTimeAdapter = AddTripTimeAdapter(mutableListOf(), supportFragmentManager)

        addTripsView = binding.recyclerViewTimes
        addTripsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tripTimeAdapter
        }
    }

    private fun setUpAddStop() {
        binding.buttonAddStop.setOnClickListener {
            Log.d("AddTripsActivity", "Add stop button clicked")
        }
    }

    private fun setUpAddTime() {
        binding.buttonAddTime.setOnClickListener {
            val newSchedule = Pair(
                mutableListOf(DayOfWeek.MONDAY),
                LocalTime.of(8, 0)
            )
            tripTimeAdapter.addTime(newSchedule)
        }
    }

    private fun setUpCancel() {
        binding.fabCancelTrip.setOnClickListener {
            finish()
        }
    }

    private fun setUpSave() {
        binding.fabNewTrip.setOnClickListener {
            finish()
            // do save stuff
        }
    }
}