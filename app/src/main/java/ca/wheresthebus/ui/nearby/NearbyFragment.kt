package ca.wheresthebus.ui.nearby

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.databinding.FragmentNearbyBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NearbyFragment :
    Fragment(),
    OnMapReadyCallback {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _binding: FragmentNearbyBinding? = null

    private lateinit var nearbyViewModel: NearbyViewModel;

    private lateinit var googleMap: GoogleMap
    private var currentLocationMarker: Marker? = null;
    private var currentLocationRadius: Circle? = null;
    private lateinit var nearbyMarkerManager: NearbyMarkerManager;

    private lateinit var expandListButton: ExtendedFloatingActionButton;
    private lateinit var recenterButton: FloatingActionButton;
    private lateinit var nearbyBottomSheet: BottomSheetDialogFragment;

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
        initializeInterface();
        observeLocationUpdates();
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Toast.makeText(context, "Finding bus stops near you...", Toast.LENGTH_SHORT).show();

        nearbyMarkerManager = NearbyMarkerManager(googleMap);

        // apply dark mode if the phone is in dark mode
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_dark_mode));
        }

        // disable location updates when a marker is clicked
        googleMap.setOnMarkerClickListener {
            Log.d("NearbyFragment", "Marker clicked");
            nearbyViewModel.stopLocationUpdates();
            false;
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        nearbyViewModel.stopLocationUpdates();

    }

    private fun initializeMapFragment() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.NearbyFragment_nearbyMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initializeViewModel() {
//        nearbyViewModel.loadStopsFromCSV(requireContext());
        val mainDBViewModel: MainDBViewModel = ViewModelProvider(requireActivity())[MainDBViewModel::class.java]
        nearbyViewModel.setMainDBViewModel(mainDBViewModel);
        nearbyViewModel.loadStopsFromDatabase();

        nearbyViewModel.getLocationPermissions(requireContext());
        nearbyViewModel.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    private fun initializeInterface() {

        expandListButton = requireView().findViewById(R.id.NearbyFragment_expandListButton);
        recenterButton = requireView().findViewById(R.id.NearbyFragment_recenterButton);

        expandListButton.setOnClickListener {
            try {
                val nearbyStops: ArrayList<BusStop> = ArrayList<BusStop>();
                for (stop in nearbyViewModel.busStopList) {
                    val stopLocation: LatLng = LatLng(stop.location.latitude, stop.location.longitude);
                    val currentLocation: LatLng = LatLng(nearbyViewModel.locationUpdates.value!!.latitude, nearbyViewModel.locationUpdates.value!!.longitude);

                    if (nearbyViewModel.isInRange(currentLocation, stopLocation, 300.0)) {
                        nearbyStops.add(stop);
                    }
                }

                nearbyBottomSheet = NearbyBottomSheet(nearbyStops);
                nearbyBottomSheet.show(parentFragmentManager, "NearbyBottomSheet");
            } catch (e: Exception) {
                Log.e("NearbyFragment", "${e.message}");
                Toast.makeText(context, "Failed to load nearby stops, please wait a moment.", Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }
        }

        recenterButton.setOnClickListener {
            nearbyViewModel.startLocationUpdates(requireContext());
            Toast.makeText(context, "Recentering...", Toast.LENGTH_SHORT).show();
        }
    }

    private fun observeLocationUpdates() {
        nearbyViewModel.locationUpdates.observe(viewLifecycleOwner) { location ->
            val currentLocation = LatLng(location.latitude, location.longitude)
            Log.d("NearbyFragment", "Location updated: $currentLocation")

            try {
                // if the location updates before the map is initialized, it can cause a crash
                updateCurrentLocationMarker(currentLocation)

                updateNearbyStopMarkers(currentLocation)

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
            } catch (e: Exception) {
                Log.e("NearbyFragment", "${e.message}");
            }
        }
        nearbyViewModel.startLocationUpdates(requireContext())

        // Observe the isTracking to change the icon respectively for the recenter button
        nearbyViewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
            updateRecenterIcon(isTracking)
        }
    }

    private fun updateRecenterIcon(isTracking: Boolean) {
        if (isTracking) {
            recenterButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.baseline_gps_fixed_24
                )
            )
        } else {
            recenterButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.baseline_gps_not_fixed_24
                )
            )
        }
    }

    private fun updateNearbyStopMarkers(currentLocation: LatLng) {
        // calculate the distance between the user and the stops
        // if within distance, add the stop to the nearby stops list
        val nearbyStops = ArrayList<BusStop>()
        for (stop in nearbyViewModel.busStopList) {
            val stopLocation = LatLng(stop.location.latitude, stop.location.longitude)
            if (nearbyViewModel.isInRange(currentLocation, stopLocation, 300.0)) {
                nearbyStops.add(stop)
            }
        }

        // on each interval of a location update, update the markers for the nearby stops
        val stopIds = nearbyStops.map { it.id.value } // get the IDs of the nearby stops
        for (stop in nearbyStops) {
            val stopLocation = LatLng(stop.location.latitude, stop.location.longitude)
            nearbyMarkerManager.addOrUpdateMarker(
                stop.id.value,
                stopLocation,
                "${stop.code.value} - ${stop.name}"
            )
        }

        // Remove markers for stops that are no longer nearby
        val currentMarkerIds =
            nearbyMarkerManager.getMarkerIds().toList() // Create a copy of the marker IDs
        for (id in currentMarkerIds) {
            if (id != "currentLocation" && !stopIds.contains(id)) {
                nearbyMarkerManager.removeMarker(id)
            }
        }
    }

    private fun updateCurrentLocationMarker(currentLocation: LatLng) {
        // update the current location marker
        if (currentLocationMarker == null) {
            currentLocationMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )

            // add a circle radius around the user's location
            currentLocationRadius = googleMap.addCircle(
                CircleOptions()
                    .center(currentLocation)
                    .radius(300.0)
                    .strokeWidth(3f)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(25, 0, 0, 255))
            );
            currentLocationRadius!!.isVisible = true; // won't be null idt
        } else {
            currentLocationMarker!!.position = currentLocation
            currentLocationRadius!!.center = currentLocation
        }
    }

}