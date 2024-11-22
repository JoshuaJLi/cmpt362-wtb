package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.model.ScheduledTrip

class TripAdapter(
    private val dataSet : List<ScheduledTrip>
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nickname : TextView = view.findViewById(R.id.text_trip_nickname)
        private val active : TextView = view.findViewById(R.id.text_trip_active_time)
        private val stops : RecyclerView = view.findViewById(R.id.recycler_trips_recycler_stops)

        fun bind(trip : ScheduledTrip) {
            nickname.text = trip.nickname
        }
        init {
            view.setOnClickListener {  }
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false).let {
            return TripViewHolder(it)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        dataSet[position].let {
            holder.bind(it)
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
}