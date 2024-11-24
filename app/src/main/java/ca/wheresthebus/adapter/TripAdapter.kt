package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ca.wheresthebus.R
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.ScheduledTrip
import java.time.LocalDateTime

class TripAdapter(
    private val dataSet : List<ScheduledTrip>
) : RecyclerView.Adapter<TripAdapter.ActiveTripViewHolder>() {

    companion object {
        object ViewType {
            const val ACTIVE = 0
            const val TODAY = 1
            const val LATER = 2
        }
    }


    inner class ActiveTripViewHolder(view: View, private val viewType: Int) : ViewHolder(view) {
        private val nickname : TextView = view.findViewById(R.id.text_trip_nickname)
        private val active : TextView = view.findViewById(R.id.text_trip_active_time)
        private var stops : RecyclerView = view.findViewById(R.id.recycler_trips_recycler_stops)
        private lateinit var stopAdapter: FavStopAdapter

        fun bind(trip : ScheduledTrip) {
            val adapterType = when(viewType) {
                (ViewType.ACTIVE) -> FavStopAdapter.Type.TRIP_ACTIVE
                else -> FavStopAdapter.Type.TRIP_INACTIVE
            }

            nickname.text = trip.nickname
            stopAdapter = FavStopAdapter(trip.stops, adapterType)

            stops.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = stopAdapter
            }
        }
        init {
            view.setOnClickListener {  }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveTripViewHolder {
        LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false).let {
            return ActiveTripViewHolder(it, viewType)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: ActiveTripViewHolder, position: Int) {
        dataSet[position].let {
            holder.bind(it)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val trip = dataSet[position]
        val currentTime = LocalDateTime.now()


        if (trip.isActive(currentTime)) {
            return ViewType.ACTIVE
        } else if (trip.isToday(currentTime)) {
            return ViewType.TODAY
        }

        return ViewType.LATER
    }
}