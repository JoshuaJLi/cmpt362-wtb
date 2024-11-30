package ca.wheresthebus.ui.nearby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.model.BusStop
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NearbyBottomSheet(
    private val stops: List<BusStop>,
    private val selectedStopId: String? = null // because the user may have selected a stop
) : BottomSheetDialogFragment(), NearbyStopSavedListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_nearby, container, false);
    }

    override fun onStart() {
        super.onStart();
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet);
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it);
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED;

            // adjust the bottom sheet height to be above the navigation bar
            val layoutParams = it.layoutParams;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            it.layoutParams = layoutParams;
        }

        val recyclerView: RecyclerView = dialog!!.findViewById(R.id.nearby_stops_recycler_view);
        recyclerView.layoutManager = LinearLayoutManager(context);
        val recyclerViewAdapter: NearbyStopsAdapter = NearbyStopsAdapter(stops);
        recyclerView.adapter = NearbyStopsAdapter(stops);

        // if there is a selected stop, scroll to it and expand it
        if (selectedStopId != null) {
            val position = stops.indexOfFirst { stop -> stop.id.value == selectedStopId };
            if (position != -1) { // if the stop is found
                recyclerView.scrollToPosition(position);
                recyclerViewAdapter.setExpandedPosition(position);
            }
        }
        val recyclerView: RecyclerView = dialog!!.findViewById(R.id.nearby_stops_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = NearbyStopsAdapter(requireActivity(), stops, this)
    }

    override fun onStopSaved() {
        this.dismiss()
    }
}