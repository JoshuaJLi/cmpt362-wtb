package ca.wheresthebus.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.Route

class StopSuggestionAdapter(
    private val suggestedStops: ArrayList<BusStop>,
    private val onSearchItemClick: (Map<String, Route>, BusStop) -> Unit,
) : RecyclerView.Adapter<StopSuggestionAdapter.SuggestedStopViewHolder>() {

    inner class SuggestedStopViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        private val stopName: TextView = view.findViewById(R.id.suggested_stop_name)
        private val stopCode: TextView = view.findViewById(R.id.suggested_stop_code)
        private val routesAtStop: TextView = view.findViewById(R.id.suggested_stop_routes)

        fun bind(stop: BusStop) {
            stopName.text = stop.name
            stopCode.text = stop.code.value

            val routesMap = stop.routes.associateBy { it.shortName }
            routesAtStop.text = routesMap.keys.joinToString(", ")

            view.setOnClickListener {
                val selectedStop = suggestedStops[adapterPosition]
                onSearchItemClick(routesMap, selectedStop)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedStopViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_suggested_bus, parent, false)
        return SuggestedStopViewHolder(view)
    }

    override fun getItemCount(): Int {
        return suggestedStops.size
    }

    override fun onBindViewHolder(holder: SuggestedStopViewHolder, position: Int) {
        suggestedStops[position].let {
            holder.bind(it)
        }
    }
}