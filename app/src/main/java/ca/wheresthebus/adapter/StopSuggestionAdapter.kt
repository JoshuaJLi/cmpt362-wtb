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

    inner class SuggestedStopViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        private val stopName: TextView = view.findViewById(R.id.suggested_stop_name)
        private val stopCode: TextView = view.findViewById(R.id.suggested_stop_code)
        private val routesAtStop: TextView = view.findViewById(R.id.suggested_stop_routes)

        // Key is the route short name
        private var customView: View =
            LayoutInflater.from(view.context).inflate(R.layout.dialog_add_fav_stop, null)
        private val nicknameEditText: EditText = customView.findViewById(R.id.nicknameEditText)
        private val routesSpinner: Spinner = customView.findViewById(R.id.routeChoiceSpinner)

        fun bind(stop: BusStop) {
            stopName.text = stop.name
            stopCode.text = stop.code.value

            val routesMap = stop.routes.associateBy { it.shortName }
            routesAtStop.text = routesMap.keys.joinToString(", ")

            var selectedRoute: Route? = null

            routesSpinner.adapter = ArrayAdapter(context, R.layout.route_spinner_item, routesMap.keys.toList())
            routesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    val key = routesSpinner.adapter.getItem(position)
                    selectedRoute = routesMap[key]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedRoute = null
                }
            }

            view.setOnClickListener {
                val selectedStop = suggestedStops[adapterPosition]
                val builder = AlertDialog.Builder(customView.context)

                // Creating dialog
                builder
                    .setView(customView)
                    .setTitle("Add Favourite Stop")
                    .setNegativeButton("Cancel") { _, _ -> }
                    .setPositiveButton("Ok") { _, _ ->
                        if (selectedRoute != null) {
                            mainDBViewModel.insertFavouriteStop(
                                FavouriteStop(
                                    nicknameEditText.text.toString(),
                                    selectedStop,
                                    selectedRoute!!
                                )
                            )
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