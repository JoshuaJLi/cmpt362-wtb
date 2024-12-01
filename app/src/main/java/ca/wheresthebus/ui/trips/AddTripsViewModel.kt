package ca.wheresthebus.ui.trips

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import java.time.LocalTime

class AddTripsViewModel: ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    val schedulePairs = mutableListOf<Pair<MutableList<DayOfWeek>, LocalTime>>()
}