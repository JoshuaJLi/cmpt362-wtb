package ca.wheresthebus.ui.nearby

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class NearbyMarkerManager (private val googleMap: GoogleMap) {

    private val markers = mutableMapOf<String, Marker>();

    fun addOrUpdateMarker(id: String, position: LatLng, title: String) {
        val marker = markers[id];
        if (marker == null){
            // add a new marker
            val newMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(title)
            );
            markers[id] = newMarker!!; // cannot be null in this instance
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
        for (marker in markers.values) {
            marker.remove();
        }
        markers.clear();
    }
}