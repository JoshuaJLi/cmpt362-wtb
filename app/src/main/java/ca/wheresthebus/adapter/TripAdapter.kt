package ca.wheresthebus.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.model.ScheduledTrip

class TripAdapter(
    private val dataSet : Array<ScheduledTrip>
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        init {
            view.setOnClickListener {  }
        }
    }

    inner class StopsAdapter(view:View) : RecyclerView.Adapter<StopsAdapter.StopViewHolder>() {

        inner class StopViewHolder(view : View) : RecyclerView.ViewHolder(view) {

        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
            TODO("Not yet implemented")
        }

        override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
            TODO("Not yet implemented")
        }

        override fun getItemCount(): Int {
            TODO("Not yet implemented")
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        TODO("Not yet implemented")
    }
}