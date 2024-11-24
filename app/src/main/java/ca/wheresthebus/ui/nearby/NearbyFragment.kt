package ca.wheresthebus.ui.nearby

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ca.wheresthebus.R
import ca.wheresthebus.data.model.Stop
import ca.wheresthebus.databinding.FragmentNearbyBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class NearbyFragment : Fragment(), OnMapReadyCallback, OnCameraMoveListener {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _binding: FragmentNearbyBinding? = null

    private lateinit var nearbyViewModel: NearbyViewModel;

    private lateinit var mMap: GoogleMap
    private var currentLocationMarker: Marker? = null;
    private var currentLocationRadius: Circle? = null;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        nearbyViewModel = ViewModelProvider(this)[NearbyViewModel::class.java]

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        initializeMapFragment();
        initializeViewModel();
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

//        for (stop in nearbyViewModel.stopList) {
//            val newStopLatLng = LatLng(stop.latitude.toDouble(), stop.longitude.toDouble())
//            mMap.addMarker(MarkerOptions().position(newStopLatLng).title(stop.stopNumber + " - " + stop.stopName))
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        nearbyViewModel.stopLocationUpdates();
    }

    private fun initializeMapFragment() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.poopy) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onCameraMove() {
        TODO("Not yet implemented")
    }

    private fun initializeViewModel() {
        nearbyViewModel.loadStopsFromCSV(requireContext());
        nearbyViewModel.getLocationPermissions(requireContext());
        nearbyViewModel.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // whenever a location is posted into the live data, this will be called
        nearbyViewModel.locationUpdates.observe(viewLifecycleOwner) { location ->
            val currentLocation: LatLng = LatLng(location.latitude, location.longitude);

            // remove the previous marker once the location updates
            if (currentLocationMarker != null) { // if the marker isn't null, the radius won't be null either
                currentLocationMarker?.remove();
                currentLocationRadius?.remove();
            }

            // remove all the markers from the map
            mMap.clear();

            // add a marker at the current location
            currentLocationMarker = mMap.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("Current Location")
                    .snippet("Your current location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .draggable(true)
            );

            // add a circle radius around the user's location
            currentLocationRadius = mMap.addCircle(
                CircleOptions()
                    .center(currentLocation)
                    .radius(300.0)
                    .strokeWidth(3f)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(25, 0, 0, 255))
            );
            currentLocationRadius!!.isVisible = true; // won't be null idt

            // calculate the distances between the user's location and the stops and make sure it's within the radius
            val nearbyStops: ArrayList<Stop> = ArrayList<Stop>();
            for (stop in nearbyViewModel.stopList) {
                val stopLocation: LatLng = LatLng(stop.latitude.toDouble(), stop.longitude.toDouble());
                if (nearbyViewModel.isInRange(currentLocation, stopLocation, 300.0)) {
                    nearbyStops.add(stop);
                }
            }

            // add the nearby stops to the map
            for (stop in nearbyStops) {
                val newStopLatLng = LatLng(stop.latitude.toDouble(), stop.longitude.toDouble());
                mMap.addMarker(MarkerOptions().position(newStopLatLng).title(stop.stopNumber + " - " + stop.stopName));
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f));
        }
        nearbyViewModel.startLocationUpdates();
    }

}