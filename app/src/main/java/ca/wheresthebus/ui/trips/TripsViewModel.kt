package ca.wheresthebus.ui.trips

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.UpcomingTime

class TripsViewModel : ViewModel() {
    val busTimes =  MutableLiveData<MutableMap<StopRequest, List<UpcomingTime>>>()
}