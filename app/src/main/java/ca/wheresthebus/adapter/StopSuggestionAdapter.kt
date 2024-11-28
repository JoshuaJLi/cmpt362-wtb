package ca.wheresthebus.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route

class StopSuggestionAdapter(
    private val suggestedStops: ArrayList<BusStop>,
    private val mainDBViewModel: MainDBViewModel,
    private val modelFactory: ModelFactory,
    private val context: Context
) : RecyclerView.Adapter<StopSuggestionAdapter.SuggestedStopViewHolder>() {

    inner class SuggestedStopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val stopName: TextView = view.findViewById(R.id.suggested_stop_name)
        private val stopCode: TextView = view.findViewById(R.id.suggested_stop_code)
        private val routesAtStop: TextView = view.findViewById(R.id.suggested_stop_routes)
        // Key is the route short name
        private val routesMap: MutableMap<String, Route> = mutableMapOf()

        fun bind(stop: BusStop) {
            stopName.text = stop.name
            stopCode.text = stop.code.value

            // Clear old content
            routesMap.clear()
            routesAtStop.text = ""

            stop.routes.forEach { routesMap[it.shortName] = it }
            routesAtStop.text = routesMap.keys.joinToString(", ")
        }

        init {
            view.setOnClickListener {
                val selectedStop = suggestedStops[adapterPosition]
                val builder = AlertDialog.Builder(view.context)
                val customView = LayoutInflater.from(view.context).inflate(R.layout.dialog_add_fav_stop, null)
                builder.setView(customView)

                val nicknameEditText: EditText = customView.findViewById(R.id.nicknameEditText)
                val routesSpinner: Spinner = customView.findViewById(R.id.routeChoiceSpinner)

                val routesAdapter = ArrayAdapter(context, R.layout.route_spinner_item, routesMap.keys.toList())
                var selectedRoute: Route? = null

                routesSpinner.adapter = routesAdapter
                routesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val key = routesSpinner.adapter.getItem(position)
                        selectedRoute = routesMap[key]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {
                        selectedRoute = null
                    }
                }

                // Creating dialog
                builder.setTitle("Add Favourite Stop").setNegativeButton("Cancel") { _, _ ->
                    // Do nothing on cancel
                }.setPositiveButton("Ok") { _, _ ->
                    if (selectedRoute != null) {
                        val newFavouriteStop = FavouriteStop(
                            nicknameEditText.text.toString(),
                            selectedStop,
                            selectedRoute!!
                        )
                        mainDBViewModel.insertFavouriteStop(newFavouriteStop)
                    } else {
                        Toast.makeText(
                            view.context,
                            "Please select a valid bus route.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.create().show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedStopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggested_bus, parent, false)
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