package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.model.FavouriteStop
import java.time.Duration

class FavStopAdapter(
    private val dataSet: ArrayList<FavouriteStop>,
    private val type: Type = Type.HOME,
    private val busTimesMap: MutableMap<StopCode, List<Duration>> = mutableMapOf()
) : RecyclerView.Adapter<FavStopAdapter.BindingFavStopHolder>() {

    enum class Type {
        HOME,
        TRIP_ACTIVE,
        TRIP_INACTIVE
    }


    abstract inner class BindingFavStopHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(stop: FavouriteStop)
    }

    inner class HomeScreenViewHolder(view: View) : BindingFavStopHolder(view) {
        private val nickname: TextView = view.findViewById(R.id.text_stop_nickname)
        private val id: TextView = view.findViewById(R.id.text_stop_id)
        private val upcoming: TextView = view.findViewById(R.id.text_stop_upcoming)

        override fun bind(stop: FavouriteStop) {
            nickname.text = buildString {
                append(stop.route.shortName)
                if (stop.nickname.isNotEmpty()) {
                    append(" - ")
                    append(stop.nickname)
                }
            }

            id.text = buildString {
                append("Stop Code: ")
                append(stop.busStop.code.value)
            }

            val busTimes = busTimesMap[stop.busStop.code]
            if (!busTimes.isNullOrEmpty()) {
                val formattedTimes = busTimes.map { busArrivalTime ->
                    val minutes = busArrivalTime.toMinutes()
                    if (minutes >= 1) "$minutes min" else "Now"
                }

                upcoming.text = buildString {
                    append(formattedTimes.joinToString(", "))
                }
            } else {
                upcoming.text = buildString {
                    append("No upcoming buses")
                }
            }
        }
    }

    inner class TripListViewHolder(view: View) : BindingFavStopHolder(view) {
        private val nickname: TextView = view.findViewById(R.id.text_trip_stop_nickname)
        private val upcoming: TextView = view.findViewById(R.id.text_trip_stop_next_time)

        override fun bind(stop: FavouriteStop) {
            nickname.text = stop.nickname
            upcoming.text = stop.busStop.location.toString()
        }

    }

    inner class InactiveTripListViewHolder(view: View) : BindingFavStopHolder(view) {
        private val nickname: TextView = view.findViewById(R.id.text_trip_inactive_stop_nickname)

        override fun bind(stop: FavouriteStop) {
            nickname.text = stop.nickname
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingFavStopHolder {
        when (type) {
            Type.HOME -> {
                LayoutInflater.from(parent.context).inflate(R.layout.item_fav_bus, parent, false)
                    .let {
                        return HomeScreenViewHolder(it)
                    }
            }
            Type.TRIP_INACTIVE -> {
                LayoutInflater.from(parent.context).inflate(R.layout.item_trip_stop_inactive, parent, false)
                    .let {
                        return InactiveTripListViewHolder(it)
                    }
            }
            else -> {
                LayoutInflater.from(parent.context).inflate(R.layout.item_trip_stop_active, parent, false)
                    .let {
                        return TripListViewHolder(it)
                    }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: BindingFavStopHolder, position: Int) {
        dataSet[position].let {
            holder.bind(it)
        }
    }

    fun updateBusTimes(busTimes: Map<StopCode, List<Duration>>) {
        busTimesMap.clear()
        busTimesMap.putAll(busTimes)
        // TODO: figure out a more efficient way to do this in the future
        notifyDataSetChanged()
    }
}