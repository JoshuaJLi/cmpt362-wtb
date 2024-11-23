package ca.wheresthebus.ui.home

import android.graphics.ColorSpace.Model
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.adapter.StopSuggestionAdapter
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.databinding.BottomSheetAddFavBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddFavBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddFavBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stopSuggestionsView: RecyclerView
    private lateinit var stopSuggestionAdapter: StopSuggestionAdapter
    private var suggestedStops: ArrayList<BusStop> = arrayListOf()
    private var nearbyStops: ArrayList<BusStop> = arrayListOf()

    private lateinit var mainDBViewModel: MainDBViewModel
    private lateinit var modelFactory: ModelFactory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflater.inflate(R.layout.bottom_sheet_add_fav, container, false)
        mainDBViewModel = ViewModelProvider(requireActivity()).get(MainDBViewModel::class.java)
        modelFactory = ModelFactory()
        _binding = BottomSheetAddFavBinding.inflate(inflater, container, false)
        val root: View = binding.root
        stopSuggestionsView = binding.recyclerViewSuggestions
        stopSuggestionsView.layoutManager = LinearLayoutManager(context)
        stopSuggestionAdapter = StopSuggestionAdapter(suggestedStops, mainDBViewModel, modelFactory, requireContext())
        stopSuggestionsView.adapter = stopSuggestionAdapter
        //used for testing
        nearbyStops.add(mainDBViewModel.getBusStopByCode("55234")!!)
        binding.apply {
            searchViewBus.setupWithSearchBar(searchBarBus)
            val nearbySuggestionsRecyclerView = binding.recyclerViewNearbySuggestions
            val nearbyAdapter = StopSuggestionAdapter(nearbyStops, mainDBViewModel, modelFactory, requireContext())
            nearbySuggestionsRecyclerView.layoutManager = LinearLayoutManager(context)
            nearbySuggestionsRecyclerView.adapter = nearbyAdapter
            //Todo: populate suggestedStops array with the nearby stops and notify.
            searchViewBus.addTransitionListener { searchViewBus, previousState, newState ->
                if (newState != com.google.android.material.search.SearchView.TransitionState.SHOWING) {
                    nearbySuggestionsRecyclerView.visibility = View.VISIBLE
                    stopSuggestionsView.visibility = View.INVISIBLE
                }
            }

            searchViewBus.editText.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                    if (charSequence?.toString().isNullOrBlank()) {
                        nearbySuggestionsRecyclerView.visibility = View.VISIBLE
                        stopSuggestionsView.visibility = View.INVISIBLE
                    }
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, after: Int) {
                    // Update the search bar text and perform search every time the text changes
                    val searchText = charSequence?.toString() ?: ""
                    if (searchText.isBlank()) {
                        nearbySuggestionsRecyclerView.visibility = View.VISIBLE
                        stopSuggestionsView.visibility = View.INVISIBLE
                    } else {
                        nearbySuggestionsRecyclerView.visibility = View.INVISIBLE
                        stopSuggestionsView.visibility = View.VISIBLE
                        // Perform the search and update suggestions
                        suggestedStops.clear()
                        suggestedStops.addAll(mainDBViewModel.searchByCode(searchText))

                        // Notify the adapter that the data has changed
                        stopSuggestionAdapter.notifyDataSetChanged()
                    }
                    //searchBarBus.setText(charSequence.toString()) // Sync searchBar with SearchView input
                }

                override fun afterTextChanged(editable: Editable?) {
                    if (editable?.isEmpty() == true) {
                        nearbySuggestionsRecyclerView.visibility = View.VISIBLE
                        stopSuggestionsView.visibility = View.INVISIBLE
                    }
                }
            })

        }
        return root
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}