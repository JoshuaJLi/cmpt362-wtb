package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.model.BusStop

class FavStopAdapter(
    private val dataSet: Array<BusStop>
) : RecyclerView.Adapter<FavStopAdapter.FavStopViewHolder>() {

    inner class FavStopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nickname: TextView = view.findViewById(R.id.text_stop_nickname)
        private val id: TextView = view.findViewById(R.id.text_stop_id)
        private val upcoming: TextView = view.findViewById(R.id.text_stop_upcoming)

        fun bind(stop: BusStop) {
            id.text = stop.code.id
            upcoming.text = stop.location.toString()
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

    // TODO: make an actual class to hold this information
    fun updateNextBus(listOfBusCodeAndNextTime: List<String>) {

    }
}