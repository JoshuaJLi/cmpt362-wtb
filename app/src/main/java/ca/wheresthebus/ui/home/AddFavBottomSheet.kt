package ca.wheresthebus.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.adapter.StopSuggestionAdapter
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.Route
import ca.wheresthebus.databinding.BottomSheetAddFavBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class AddFavBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddFavBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopSuggestionsView: RecyclerView
    private lateinit var stopSuggestionAdapter: StopSuggestionAdapter
    private var suggestedStops: ArrayList<BusStop> = arrayListOf()

    private lateinit var nearbySuggestionsView: RecyclerView
    private lateinit var nearbyAdapter: StopSuggestionAdapter
    private var nearbyStops: ArrayList<BusStop> = arrayListOf()

    private lateinit var mainDBViewModel: MainDBViewModel
    private lateinit var addStop: (FavouriteStop) -> Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflater.inflate(R.layout.bottom_sheet_add_fav, container, false)
        mainDBViewModel = ViewModelProvider(requireActivity()).get(MainDBViewModel::class.java)
        _binding = BottomSheetAddFavBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // The recycler view of results DURING a user search
        stopSuggestionsView = binding.recyclerViewSuggestions
        stopSuggestionsView.layoutManager = LinearLayoutManager(context)
        stopSuggestionAdapter = StopSuggestionAdapter(suggestedStops, ::onSearchItemClick)
        stopSuggestionsView.adapter = stopSuggestionAdapter

        // The recycler view of results BEFORE a user starts searching
        // Todo: populate suggestedStops array with the nearby stops and notify.
        nearbySuggestionsView = binding.recyclerViewNearbySuggestions
        nearbyAdapter = StopSuggestionAdapter(nearbyStops, ::onSearchItemClick)
        nearbySuggestionsView.layoutManager = LinearLayoutManager(context)
        nearbySuggestionsView.adapter = nearbyAdapter

        binding.apply {
            searchViewBus.setupWithSearchBar(searchBarBus)

            searchViewBus.addTransitionListener { _, _, newState ->
                if (newState != com.google.android.material.search.SearchView.TransitionState.SHOWING) {
                    nearbySuggestionsView.visibility = View.VISIBLE
                    stopSuggestionsView.visibility = View.INVISIBLE
                }
            }

            searchViewBus.editText.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                    if (charSequence?.toString().isNullOrBlank()) {
                        nearbySuggestionsView.visibility = View.VISIBLE
                        stopSuggestionsView.visibility = View.INVISIBLE
                    }
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, after: Int) {
                    // Update the search bar text and perform search every time the text changes
                    val searchText = charSequence?.toString() ?: ""
                    if (searchText.isBlank()) {
                        nearbySuggestionsView.visibility = View.VISIBLE
                        stopSuggestionsView.visibility = View.INVISIBLE
                    } else {
                        nearbySuggestionsView.visibility = View.INVISIBLE
                        stopSuggestionsView.visibility = View.VISIBLE
                        // Perform the search and update suggestions
                        suggestedStops.clear()
                        suggestedStops.addAll(mainDBViewModel.searchForStop(searchText))

                        // Notify the adapter that the data has changed
                        stopSuggestionAdapter.notifyDataSetChanged()
                    }
                }

                override fun afterTextChanged(editable: Editable?) {
                    if (editable?.isEmpty() == true) {
                        nearbySuggestionsView.visibility = View.VISIBLE
                        stopSuggestionsView.visibility = View.INVISIBLE
                    }
                }
            })
        }
        return root
    }

    fun assignAddFavourite(func : (FavouriteStop) -> Unit): AddFavBottomSheet {
        this.addStop = func
        return this
    }

    // Create and show a dialogue to add a favorite stop
    private fun onSearchItemClick(
        routesMap: Map<String, Route>,
        selectedStop: BusStop,
    ) {
        val customView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_fav_stop, null)
        val nicknameEditText: EditText = customView.findViewById(R.id.nicknameEditText)
        val routesDropdown: AutoCompleteTextView = customView.findViewById(R.id.routes_dropdown)
        val routesDropdownLayout: TextInputLayout = customView.findViewById(R.id.routes_dropdown_layout)

        // set dropdown content
        val routesAdapter =
            ArrayAdapter(requireContext(), R.layout.route_spinner_item, routesMap.values.map {
                "${it.shortName} - ${it.longName}"
            })
        routesDropdown.setAdapter(routesAdapter)

        // Grab selected dropdown option
        var selectedRoute: Route? = null
        routesDropdown.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // index routesMap using the shortname part of the dropdown content
                selectedRoute = routesMap[s?.split(" - ")?.get(0)?.trim()]
            }
        })

        // Build the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setTitle("Adding New Favourite Stop")
            .setMessage(selectedStop.name)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Confirm", null)
            .create()

        // override OK to check valid dropdown selections
        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            confirmButton.setOnClickListener {
                if (selectedRoute != null) {
                    routesDropdownLayout.error = null
                    addStop( FavouriteStop(
                        nickname = nicknameEditText.text.toString(),
                        busStop = selectedStop,
                        route =  selectedRoute!!
                    ))
                    dialog.dismiss() // dismiss this dialog
                    this.dismiss() // close the bottom sheet fragment
                } else {
                    routesDropdownLayout.error = "Please select a valid bus route."
                }
            }
        }

        dialog.show()
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}