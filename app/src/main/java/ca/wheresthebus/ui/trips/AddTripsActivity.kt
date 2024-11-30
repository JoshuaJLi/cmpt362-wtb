package ca.wheresthebus.ui.trips

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ca.wheresthebus.R
import ca.wheresthebus.databinding.ActivityAddTripsBinding

class AddTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripsBinding

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

        setUpAddStop()
        setUpAddTime()
        setUpSaveFab()
    }

    private fun setUpAddStop() {
        binding.buttonAddStop.setOnClickListener {
            Log.d("AddTripsActivity", "Add stop button clicked")
        }
    }

    private fun setUpAddTime() {
        binding.buttonAddTime.setOnClickListener {
            Log.d("AddTripsActivity", "Add time button clicked")
        }
    }

    private fun setUpSaveFab() {
        binding.fabNewTrip.setOnClickListener {
            finish()
        }
    }
}