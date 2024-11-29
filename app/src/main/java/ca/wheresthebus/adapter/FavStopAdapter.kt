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
    private val dataSet: ArrayList<FavouriteStop>
) : RecyclerView.Adapter<FavStopAdapter.FavStopViewHolder>() {

    private val busTimesMap: MutableMap<StopCode, List<Duration>> = mutableMapOf()

    inner class FavStopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nickname: TextView = view.findViewById(R.id.text_stop_nickname)
        private val id: TextView = view.findViewById(R.id.text_stop_id)
        private val upcoming: TextView = view.findViewById(R.id.text_stop_upcoming)

        fun bind(stop: FavouriteStop) {
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

        init {
            view.setOnClickListener {
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavStopViewHolder {
        LayoutInflater.from(parent.context).inflate(R.layout.item_fav_bus, parent, false).let {
            return FavStopViewHolder(it)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: FavStopViewHolder, position: Int) {
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