package ca.wheresthebus.ui.nearby

/**
 * Listener for the NearbyBottomSheet to know if an item was saved
 * so that it can dismiss itself
 */
interface NearbyStopSavedListener {
    fun onStopSaved()
}