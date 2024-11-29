package ca.wheresthebus.ui.trips

import android.os.Bundle
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

        setUpSaveFab()
    }

    private fun setUpSaveFab() {
        binding.fabNewTrip.setOnClickListener {
            finish()
        }
    }
}