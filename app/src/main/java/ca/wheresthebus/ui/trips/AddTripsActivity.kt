package ca.wheresthebus.ui.trips

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.adapter.AddTripTimeAdapter
import ca.wheresthebus.adapter.FavStopAdapter
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Schedule
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.databinding.ActivityAddTripsBinding
import ca.wheresthebus.ui.home.AddFavBottomSheet
import com.google.android.material.slider.Slider
import org.mongodb.kbson.ObjectId
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

class AddTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripsBinding

    private lateinit var addTripsViewModel: AddTripsViewModel

    private lateinit var addTripsView : RecyclerView

    private lateinit var addBusView : RecyclerView

    private lateinit var tripTimeAdapter: AddTripTimeAdapter

    private lateinit var stopAdapter: FavStopAdapter

    private lateinit var slider : Slider

    private lateinit var sliderValue : TextView

    private lateinit var mainDBViewModel: MainDBViewModel

    private lateinit var nickname : EditText


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
        mainDBViewModel = ViewModelProvider(this).get(MainDBViewModel::class.java)


        addTripsViewModel = ViewModelProvider(this)[AddTripsViewModel::class.java]

        setUpTripStopsAdapter()
        setUpTripTimeAdapter()

        setUpNickname()
        setUpAddStop()
        setUpAddTime()
        setUpCancel()
        setUpSave()
        setUpSlider()
    }

    private fun setUpNickname() {
        nickname = findViewById(R.id.text_add_trip_name)
    }

    private fun setUpSlider() {
        slider = findViewById(R.id.slider_duration)
        sliderValue = findViewById(R.id.text_duration_label)


        slider.addOnChangeListener { _, value, _ ->
            val durationText  = "${value.toInt()} minutes"
            sliderValue.text = durationText

            addTripsViewModel.duration = value.toInt()
        }
    }

    private fun setUpTripStopsAdapter() {

        stopAdapter = FavStopAdapter(addTripsViewModel.selectedTrips, FavStopAdapter.Type.CREATE_TRIP)
        addBusView = binding.recyclerViewBusses
        addBusView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = stopAdapter
        }
    }

    private fun setUpTripTimeAdapter() {
        binding.recyclerViewTimes.itemAnimator = null

        tripTimeAdapter = AddTripTimeAdapter(addTripsViewModel.schedulePairs, supportFragmentManager, binding.root)

        addTripsView = binding.recyclerViewTimes
        addTripsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tripTimeAdapter
        }
    }

    private fun addStop(favouriteStop : FavouriteStop) {
        addTripsViewModel.selectedTrips.add(favouriteStop)

        stopAdapter.notifyItemInserted(addTripsViewModel.selectedTrips.size - 1)
    }

    private fun setUpAddStop() {
        val bottom = AddFavBottomSheet()
            .assignAddFavourite(::addStop)

        binding.buttonAddStop.setOnClickListener {
                bottom.show(supportFragmentManager, AddFavBottomSheet.TAG)
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
            val schedules = addTripsViewModel.schedulePairs
                .flatMap { (days, time) -> days.map { day -> day to time }  }
                .map { (day, time) -> Schedule(day, time) }
                .toList()


            val trip = ScheduledTrip(
                id = ScheduledTripId(ObjectId().toHexString()),
                nickname = nickname.text.toString(),
                stops = addTripsViewModel.selectedTrips,
                activeTimes = ArrayList(schedules),
                duration = Duration.ofMinutes(slider.value.toLong())
            )
            mainDBViewModel.insertScheduledTrip(trip)
            finish()
            // do save stuff
        }
    }
}