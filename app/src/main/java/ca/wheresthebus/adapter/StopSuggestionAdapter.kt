package ca.wheresthebus.adapter

import android.app.AlertDialog
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop

class StopSuggestionAdapter(
    private val suggestedStops: ArrayList<BusStop>,
    private val mainDBViewModel: MainDBViewModel,
    private val modelFactory: ModelFactory
) : RecyclerView.Adapter<StopSuggestionAdapter.SuggestedStopViewHolder>() {

    @RequiresApi(Build.VERSION_CODES.O)
    inner class SuggestedStopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val stopName: TextView = view.findViewById(R.id.suggested_stop_name)
        private val stopCode: TextView = view.findViewById(R.id.suggested_stop_code)
        private val routesAtStop: TextView = view.findViewById(R.id.suggested_stop_routes)

        fun bind(stop: BusStop) {
            stopName.text = stop.name
            stopCode.text = stop.code.value
            val routeShortNames = ArrayList<String>()
            for (route in stop.routes) {
                routeShortNames.add(route.shortName)
            }
            routesAtStop.text = routeShortNames.joinToString(", ")
        }

        init {
            view.setOnClickListener {
                val selectedStop = suggestedStops[adapterPosition]
                val builder = AlertDialog.Builder(view.context)
                val customView = LayoutInflater.from(view.context).inflate(R.layout.dialog_add_fav_stop, null)
                builder.setView(customView)
                val nicknameEditText: EditText = customView.findViewById(R.id.nicknameEditText)
                val routeEditText: EditText = customView.findViewById(R.id.routeEditTextField)
                builder.setTitle("Add Favourite Stop!").setNegativeButton("Cancel") { _, _ ->
                    // Handle the negative button click
                }.setPositiveButton("Ok") { _, _ ->
                    val selectedRoute = mainDBViewModel.searchForRouteByShortName(routeEditText.text.toString())
                    if (selectedRoute != null) {
                        val newFavouriteStop = FavouriteStop(nicknameEditText.text.toString(), selectedStop, selectedRoute)
                        mainDBViewModel.insertFavouriteStop(newFavouriteStop)
                    } else {
                        Toast.makeText(view.context, "This route does not stop here, try again.", Toast.LENGTH_LONG).show()
                    }
                }.create().show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedStopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggested_bus, parent, false)
        return SuggestedStopViewHolder(view)
    }

    override fun getItemCount(): Int {
        return suggestedStops.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: SuggestedStopViewHolder, position: Int) {
        suggestedStops[position].let {
            holder.bind(it)
        }
    }
}