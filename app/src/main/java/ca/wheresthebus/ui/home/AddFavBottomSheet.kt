package ca.wheresthebus.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ca.wheresthebus.R
import ca.wheresthebus.databinding.BottomSheetAddFavBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddFavBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddFavBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inflater.inflate(R.layout.bottom_sheet_add_fav, container, false)

        _binding = BottomSheetAddFavBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.apply {
            searchViewBus.setupWithSearchBar(searchBarBus)

            searchViewBus.editText.setOnEditorActionListener { v, actionId, event ->
                searchViewBus.hide()
                searchBarBus.setText(searchViewBus.text)
                false
            }

        }
        return root
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}