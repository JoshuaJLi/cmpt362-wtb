package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.UpcomingTime
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.utils.TextUtils

class FavStopAdapter(
    private val dataSet: ArrayList<FavouriteStop>,
    private val type: Type = Type.HOME,
    private val busTimesMap: MutableMap<StopRequest, List<UpcomingTime>> = mutableMapOf()
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
        private val foregroundView: CardView = view.findViewById(R.id.fav_card_view)

        override fun bind(stop: FavouriteStop) {
            // Reset the swipe animation in case the view was reused
            foregroundView.translationX = 0f

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

            val busTimes = busTimesMap[Pair(stop.busStop.id, stop.route.id)]
            upcoming.text = TextUtils.upcomingBusesString(busTimes)
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
        dataSet[position].let { stop ->
            holder.bind(stop)
        }
    }

    fun updateBusTimes(busTimes: MutableMap<StopRequest, List<UpcomingTime>>) {
        busTimesMap.clear()
        busTimesMap.putAll(busTimes)
        notifyItemRangeChanged(0, itemCount)
    }
}