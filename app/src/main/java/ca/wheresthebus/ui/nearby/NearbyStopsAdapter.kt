package ca.wheresthebus.ui.nearby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.R
import ca.wheresthebus.data.model.Stop

class NearbyStopsAdapter(private val stops: List<Stop>) : RecyclerView.Adapter<NearbyStopsAdapter.ViewHolder>() {

    private var expandedPosition: Int = RecyclerView.NO_POSITION

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stopNickname: TextView = view.findViewById(R.id.NearbyBottomSheet_stopNickname)
        val stopID: TextView = view.findViewById(R.id.NearbyBottomSheet_stopID)
        val stopUpcoming: TextView = view.findViewById(R.id.NearbyBottomSheet_stopUpcoming)
        val extraInfo: LinearLayout = view.findViewById(R.id.extra_info)
        val saveButton: Button = view.findViewById(R.id.save_button)
        val discardButton: Button = view.findViewById(R.id.discard_button)
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

        val isExpanded = position == expandedPosition
        holder.extraInfo.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val previousExpandedPosition = expandedPosition
            expandedPosition = if (isExpanded) RecyclerView.NO_POSITION else position
            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(expandedPosition)
        }
    }

    override fun getItemCount() = stops.size
}