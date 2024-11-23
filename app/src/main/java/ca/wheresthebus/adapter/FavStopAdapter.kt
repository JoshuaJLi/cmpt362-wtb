package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.mongo_model.MongoBusStop

class FavStopAdapter(
    private val dataSet: ArrayList<FavouriteStop>,
    private val type : FavStopAdapter.Type = Type.HOME
) : RecyclerView.Adapter<FavStopAdapter.BindingFavStopHolder>() {

     enum class Type {
         HOME,
         TRIP
     }

     abstract inner class BindingFavStopHolder(view : View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(stop : FavouriteStop)
     }

    inner class HomeScreenViewHolder(view: View) : BindingFavStopHolder(view) {
        private val nickname: TextView = view.findViewById(R.id.text_stop_nickname)
        private val id: TextView = view.findViewById(R.id.text_stop_id)
        private val upcoming: TextView = view.findViewById(R.id.text_stop_upcoming)


        override fun bind(stop: FavouriteStop) {
            nickname.text = buildString {
                append(stop.route.shortName)
                append(" - ")
                append(stop.nickname)
            }
            id.text = buildString {
                append("Stop Code: ")
                append(stop.busStop.code.value)
            }
            //todo: fixate with the live views.
            upcoming.text = stop.busStop.location.toString()
        }

    inner class TripListViewHolder(view : View) : BindingFavStopHolder(view) {
        private val nickname : TextView = view.findViewById(R.id.text_trip_stop_nickname)
        private val upcoming : TextView = view.findViewById(R.id.text_trip_stop_next_time)

        override fun bind(stop: FavouriteStop) {
            nickname.text = stop.nickname
            upcoming.text = stop.busStop.location.toString()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingFavStopHolder {
        if (type == Type.HOME) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_fav_bus, parent, false).let {
                return HomeScreenViewHolder(it)
            }
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_trip_stop, parent, false).let {
                return TripListViewHolder(it)
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

    // TODO: make an actual class to hold this information
    fun updateNextBus(listOfBusCodeAndNextTime: List<String>) {

    }
}