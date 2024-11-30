package ca.wheresthebus.ui.nearby

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class NearbyStopsAdapter(
    private val activity: FragmentActivity,
    private val stops: List<BusStop>,
    private val onItemSavedListener: NearbyStopSavedListener
) : RecyclerView.Adapter<NearbyStopsAdapter.ViewHolder>() {

    private var expandedPosition: Int = RecyclerView.NO_POSITION
    private val mainDBViewModel: MainDBViewModel by lazy {
        ViewModelProvider(activity).get(MainDBViewModel::class.java)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stopNickname: TextView = view.findViewById(R.id.NearbyBottomSheet_stopNickname)
        val buses: TextView = view.findViewById(R.id.NearbyBottomSheet_buses)
        val extraInfo: LinearLayout = view.findViewById(R.id.extra_info)
        val saveButton: Button = view.findViewById(R.id.save_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_bus, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stop = stops[position]
        holder.stopNickname.text = buildString {
            append(stop.name)
        }
        holder.buses.text = buildString {
            append("Buses: ")
            append(stop.routes.joinToString(", ") { it.shortName })
        }

        val isExpanded = position == expandedPosition
        holder.extraInfo.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val previousExpandedPosition = expandedPosition
            expandedPosition = if (isExpanded) RecyclerView.NO_POSITION else position
            notifyItemChanged(previousExpandedPosition)
            notifyItemChanged(expandedPosition)
        }

        holder.saveButton.setOnClickListener {
            onSaveButtonClick(position)
        }
    }

    private fun onSaveButtonClick(position: Int) {
        val busStop: BusStop = stops[position]
        val routesMap = busStop.routes.associateBy { it.shortName }

        val customView =
            LayoutInflater.from(activity).inflate(R.layout.dialog_add_fav_stop, null)
        val nicknameEditText: EditText = customView.findViewById(R.id.nicknameEditText)
        val routesDropdown: AutoCompleteTextView = customView.findViewById(R.id.routes_dropdown)
        val routesDropdownLayout: TextInputLayout = customView.findViewById(R.id.routes_dropdown_layout)

        // set dropdown content
        val routesAdapter =
            ArrayAdapter(activity, R.layout.route_spinner_item, routesMap.keys.toList())
        routesDropdown.setAdapter(routesAdapter)

        // Grab selected dropdown option
        var selectedRoute: Route? = null
        routesDropdown.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedRoute = routesMap[s.toString()]
            }
        })

        // Build the dialog
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(customView)
            .setTitle("Adding New Favourite Stop")
            .setMessage(busStop.name)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Confirm", null)
            .create()

        // override OK to check valid dropdown selections
        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            confirmButton.setOnClickListener {
                if (selectedRoute != null) {
                    routesDropdownLayout.error = null
                    mainDBViewModel.insertFavouriteStop(
                        FavouriteStop(
                            nickname = nicknameEditText.text.toString(),
                            busStop = busStop,
                            route = selectedRoute!!
                        )
                    )
                    dialog.dismiss() // dismiss this dialog
                    onItemSavedListener.onStopSaved() // dismiss the bottom sheet fragment
                    Toast.makeText(activity, "Added to favourites", Toast.LENGTH_SHORT).show()
                } else {
                    routesDropdownLayout.error = "Please select a valid bus route."
                }
            }
        }

        dialog.show()
    }

    override fun getItemCount() = stops.size

    fun setExpandedPosition(position: Int) {
        val previousExpandedPosition = expandedPosition
        expandedPosition = position
        notifyItemChanged(previousExpandedPosition)
        notifyItemChanged(expandedPosition)
    }
}