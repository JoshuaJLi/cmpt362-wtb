package ca.wheresthebus.ui.nearby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.model.Stop

class NearbyStopsAdapter(private val stops: List<Stop>) : RecyclerView.Adapter<NearbyStopsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stopNickname: TextView = view.findViewById(R.id.NearbyBottomSheet_stopNickname)
        val stopID: TextView = view.findViewById(R.id.NearbyBottomSheet_stopID)
        val stopUpcoming: TextView = view.findViewById(R.id.NearbyBottomSheet_stopUpcoming)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_bus, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stop = stops[position]
        holder.stopNickname.text = stop.stopName
        holder.stopID.text = stop.stopNumber
        holder.stopUpcoming.text = "Upcoming Buses: NOT YET IMPLEMENTED"
    }

    override fun getItemCount() = stops.size
}