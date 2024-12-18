package ca.wheresthebus.ui.nearby

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import ca.wheresthebus.Globals.NEARBY_DISTANCE_THRESHOLD
import ca.wheresthebus.Globals.NEARBY_ZOOM_LEVEL
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
    OnMapReadyCallback
{

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _binding: FragmentNearbyBinding? = null

    private val nearbyViewModel: NearbyViewModel by viewModels<NearbyViewModel>()

    private lateinit var googleMap: GoogleMap
    private var currentLocationMarker: Marker? = null
    private var currentLocationRadius: Circle? = null
    private lateinit var nearbyMarkerManager: NearbyMarkerManager
    private var isInitialCameraMove = true;

    private lateinit var expandListButton: ExtendedFloatingActionButton
    private lateinit var recenterButton: FloatingActionButton
    private lateinit var nearbyBottomSheet: BottomSheetDialogFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMapFragment()
        initializeViewModel()
        initializeInterface()
        observeLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Toast.makeText(context, "Finding bus stops near you...", Toast.LENGTH_SHORT).show()

        nearbyMarkerManager = NearbyMarkerManager(googleMap)

        // apply dark mode if the phone is in dark mode
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_dark_mode))
        }

        // set the initial camera position to vancouver bc
        val vancouver = LatLng(49.19664043305816, -122.84714899957181)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vancouver, 10f))
        googleMap.clear()

        // disable location updates when a marker is clicked
        googleMap.setOnMarkerClickListener { marker ->
            Log.d("NearbyFragment", "Marker clicked")
            nearbyViewModel.stopLocationUpdates()
            expandBottomSheetToBusStop(marker)
            marker.showInfoWindow()
            true
        }

        // set the camera move started listener: allows the user to move around the map freely
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                Log.d("NearbyFragment", "Camera move started by user gesture")
                nearbyViewModel.stopLocationUpdates()
            }
        }

        // when the map is moved by the user, once the user stops moving the map, show stops near the new location
        googleMap.setOnCameraIdleListener {
            // https://developers.google.com/maps/documentation/android-sdk/reference/com/google/android/libraries/maps/model/CameraPosition
            val cameraPosition = googleMap.cameraPosition.target
            Log.d("NearbyFragment", "Camera idle at: $cameraPosition")
            updateNearbyStopMarkers(cameraPosition)
            updateCurrentLocationRadius(cameraPosition)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        nearbyViewModel.stopLocationUpdates()

    }

    private fun initializeMapFragment() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.NearbyFragment_nearbyMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initializeViewModel() {
        val mainDBViewModel: MainDBViewModel = ViewModelProvider(requireActivity())[MainDBViewModel::class.java]
        nearbyViewModel.setMainDBViewModel(mainDBViewModel)
        nearbyViewModel.loadStopsFromDatabase()
        nearbyViewModel.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private fun initializeInterface() {

        expandListButton = requireView().findViewById(R.id.NearbyFragment_expandListButton)
        recenterButton = requireView().findViewById(R.id.NearbyFragment_recenterButton)

        expandListButton.setOnClickListener {
            expandListButtonOnClickListener()
        }

        recenterButton.setOnClickListener {
            recenterButtonOnClickListener()
        }
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng) {
        val fromPosition = marker.position

        // represents the fraction of the animation that has been completed (like percentage of animation shown)
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)

        valueAnimator.duration = 1000 // duration of the animation in milliseconds

        // call a listener on each frame of the animation
        valueAnimator.addUpdateListener { animation ->
            // on every frame of the animation
            val fraction = animation.animatedFraction // get the fraction of the animation that has been completed

            // interpolate the position of the marker from the previous position to the new position
            /**
             * calculates the intermediate latitude/longitude and finds the difference between it and the target latitude/longitude
             * multiplies the difference by the fraction and adds the starting latitude to calculate where the marker should be in between
             */
            val lat = (toPosition.latitude - fromPosition.latitude) * fraction + fromPosition.latitude
            val lng = (toPosition.longitude - fromPosition.longitude) * fraction + fromPosition.longitude

            // set the position of the marker to the new interpolated position
            marker.position = LatLng(lat, lng)
        }

        valueAnimator.start()
    }

    private fun observeLocationUpdates() {
        nearbyViewModel.locationUpdates.observe(viewLifecycleOwner) { location ->
            val currentLocation = LatLng(location.latitude, location.longitude)
            Log.d("NearbyFragment", "Location updated: $currentLocation")

            try {
                // if the location updates before the map is initialized, it can cause a crash
                updateCurrentLocationMarker(currentLocation)
                updateCurrentLocationRadius(currentLocation)

                updateNearbyStopMarkers(currentLocation)

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, NEARBY_ZOOM_LEVEL))
                isInitialCameraMove = false
            } catch (e: Exception) {
                Log.e("NearbyFragment", "${e.message}")
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
        if (isInitialCameraMove == true) {
            return // don't show markers before the camera initially centers to user
        }

        val location: Location = Location("")
        location.latitude = currentLocation.latitude
        location.longitude = currentLocation.longitude
        nearbyViewModel.loadNearbyStopsFromDatabase(location)

        // calculate the distance between the user and the stops
        // if within distance, add the stop to the nearby stops list
        val nearbyStops: ArrayList<BusStop> = filterStopsInRange(
            currentLocation,
            nearbyViewModel.busStopList
        )

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

        // remove markers for stops that are no longer nearby
        val currentMarkerIds: List<String> = nearbyMarkerManager.getMarkerIds().toList() // Create a copy of the marker IDs
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
        } else {
            animateMarker(currentLocationMarker!!, currentLocation)
        }
    }

    private fun updateCurrentLocationRadius(currentLocation: LatLng) {
        if (currentLocationRadius == null) {
            // add a circle radius around the user's location
            currentLocationRadius = googleMap.addCircle(
                CircleOptions()
                    .center(currentLocation)
                    .radius(300.0)
                    .strokeWidth(3f)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(25, 0, 0, 255))
            )
            currentLocationRadius!!.isVisible = true // won't be null idt
        } else {
            currentLocationRadius!!.center = currentLocation
        }
    }

    private fun expandBottomSheetToBusStop(marker: Marker) {
        val markerId = marker.tag as? String
        val stop = nearbyViewModel.busStopList.find { it.id.value == markerId } // find the stop with the matching
        Log.d("NearbyFragment", "Marker clicked: $markerId")

        if (stop != null) { // if a stop is found
            Log.d("NearbyFragment", "Expanding bottom sheet to stop: ${stop.name}")

            // create a nearbyStops list with ONLY the stop that was clicked
            val nearbyStops = ArrayList<BusStop>()
            nearbyStops.add(stop)

            // sort the nearby stops by distance, then by name
            nearbyStops.sortedWith(compareBy { it.name })
            Log.d("NearbyFragment", "Nearby stops: $nearbyStops")

            nearbyBottomSheet = NearbyBottomSheet(nearbyStops, markerId) // create a new bottom sheet with the stop
            nearbyBottomSheet.show(parentFragmentManager, "NearbyBottomSheet")
        }
    }

    private fun recenterButtonOnClickListener() {
        nearbyViewModel.startLocationUpdates(requireContext())
        Toast.makeText(context, "Recentering...", Toast.LENGTH_SHORT).show()
    }

    private fun expandListButtonOnClickListener() {
        try {
            // with the implementation of free movement, get the location of the camera's position instead
            val cameraPosition: LatLng = googleMap.cameraPosition.target

            val currentLocation: Location = Location("")
            currentLocation.latitude = cameraPosition.latitude
            currentLocation.longitude = cameraPosition.longitude
            nearbyViewModel.loadNearbyStopsFromDatabase(currentLocation)

            val nearbyStops: ArrayList<BusStop> = filterStopsInRange(
                cameraPosition,
                nearbyViewModel.busStopList
            )

            // sort the nearby stops by distance, then by name
            val sortedList = sortNearbyStopsByDistance(nearbyStops, cameraPosition)

            nearbyBottomSheet = NearbyBottomSheet(sortedList)
            nearbyBottomSheet.show(parentFragmentManager, "NearbyBottomSheet")
        } catch (e: Exception) {
            Log.e("NearbyFragment", "${e.message}")
            Toast.makeText(
                context,
                "Failed to load nearby stops, please wait a moment.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }

    private fun filterStopsInRange(userLocation: LatLng, stops: List<BusStop>): ArrayList<BusStop> {
        val nearbyStops: ArrayList<BusStop> = ArrayList()
        for (stop in stops) {
            val stopLocation = LatLng(stop.location.latitude, stop.location.longitude)
            if (nearbyViewModel.isInRange(
                userLocation,
                stopLocation,
                NEARBY_DISTANCE_THRESHOLD
            )) {
                nearbyStops.add(stop)
            }
        }

        return nearbyStops
    }

    private fun sortNearbyStopsByDistance(nearbyStops: ArrayList<BusStop>, userLocation: LatLng): List<BusStop> {
        return nearbyStops.sortedWith(compareBy { stop ->
            val stopLocation = LatLng(stop.location.latitude, stop.location.longitude)
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                stopLocation.latitude, stopLocation.longitude,
                results
            )
            results[0] // distance in meters
        })
    }

}