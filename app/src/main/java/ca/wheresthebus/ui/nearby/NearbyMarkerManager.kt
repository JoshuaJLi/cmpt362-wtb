package ca.wheresthebus.ui.nearby

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class NearbyMarkerManager (private val googleMap: GoogleMap) {

    private val markers = mutableMapOf<String, Marker>();

    fun getMarkerIds(): Set<String> {
        return markers.keys
    }

    fun addOrUpdateMarker(id: String, position: LatLng, title: String) {
        val marker = markers[id]; // get the marker with the given id if it exists in the map
        if (marker == null){ // if the marker does not exist in the map
            // add a new marker
            val newMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(title)
            );
            newMarker?.tag = id; // set the tag of the marker to the bus stop code (for expanding the bottom sheet)

            markers[id] = newMarker!!; // cannot be null in this instance because we just added it
        } else {
            // update an existing marker
            marker.position = position;
            marker.title = title;
        }
    }

    fun removeMarker(id: String) {
        markers[id]?.remove(); // remove the marker from the google map fragment
        markers.remove(id); // remove the marker from the markers map
    }

    fun clearMarkers() {
        // remove all markers from the google map fragment
        for (marker in markers.values) {
            marker.remove();
        }
        markers.clear();
    }
}