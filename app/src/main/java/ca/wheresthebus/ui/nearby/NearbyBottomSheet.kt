package ca.wheresthebus.ui.nearby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ca.wheresthebus.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NearbyBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_nearby, container, false)
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = 200 // Set the height of the visible portion
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Adjust the bottom sheet height to be above the navigation bar
            val layoutParams = it.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.layoutParams = layoutParams

            // Set the height of the bottom sheet to the height of the visible fragment
            view?.viewTreeObserver?.addOnGlobalLayoutListener {
                val visibleHeight = view?.height ?: 0
                layoutParams.height = visibleHeight
                it.layoutParams = layoutParams
            }
        }
    }

}