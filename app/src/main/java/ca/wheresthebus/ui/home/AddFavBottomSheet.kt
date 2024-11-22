package ca.wheresthebus.ui.home

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.adapter.StopSuggestionAdapter
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

    private lateinit var mainDBViewModel: MainDBViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflater.inflate(R.layout.bottom_sheet_add_fav, container, false)
        mainDBViewModel = ViewModelProvider(requireActivity()).get(MainDBViewModel::class.java)

        _binding = BottomSheetAddFavBinding.inflate(inflater, container, false)
        val root: View = binding.root
        stopSuggestionsView = binding.recyclerViewSuggestions
        stopSuggestionsView.layoutManager = LinearLayoutManager(context)
        stopSuggestionAdapter = StopSuggestionAdapter(suggestedStops)
        stopSuggestionsView.adapter = stopSuggestionAdapter

        binding.apply {
            searchViewBus.setupWithSearchBar(searchBarBus)

            searchViewBus.editText.setOnEditorActionListener { v, actionId, event ->
                searchViewBus.hide()
                searchBarBus.setText(searchViewBus.text)
                suggestedStops.clear()
                suggestedStops.addAll(mainDBViewModel.searchByCode(searchViewBus.text.toString()))
                stopSuggestionAdapter.notifyDataSetChanged()
                false
            }

        }
        return root
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}