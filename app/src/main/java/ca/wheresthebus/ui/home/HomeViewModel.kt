package ca.wheresthebus.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.UpcomingTime

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    val busTimes =  MutableLiveData<MutableMap<StopRequest, List<UpcomingTime>>>()
}