package ca.wheresthebus.ui.nearby

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ca.wheresthebus.R
import ca.wheresthebus.data.model.Stop
import ca.wheresthebus.databinding.FragmentNearbyBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.BufferedReader
import java.io.InputStreamReader

class NearbyFragment : Fragment(), OnMapReadyCallback {

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _binding: FragmentNearbyBinding? = null

    private lateinit var nearbyViewModel: NearbyViewModel;

    private lateinit var mMap: GoogleMap

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        nearbyViewModel = ViewModelProvider(this)[NearbyViewModel::class.java]
        nearbyViewModel.intializeContext(requireContext());

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        nearbyViewModel.loadStopsFromCSV();
        nearbyViewModel.initializeFusedLocationProviderClient(LocationServices.getFusedLocationProviderClient(requireContext()));

        setupMapFragment();
        getLocationPermissions();
    }

    private fun setupMapFragment() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.poopy) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // give the map some time to load, then grab the user's current location
        Handler(Looper.getMainLooper()).postDelayed({
            // Get the last known location
            nearbyViewModel.fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Get the location's latitude and longitude
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    Toast.makeText(context, "Current location: ${currentLatLng.latitude}, ${currentLatLng.longitude}", Toast.LENGTH_SHORT).show()

                    // Add a marker at the current location
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Current Location")
                            .snippet("Your current location")
                            .draggable(true)
                    )
                    // add a circle radius around the user's location
                    var userRadius = mMap.addCircle(
                        com.google.android.gms.maps.model.CircleOptions()
                            .center(currentLatLng)
                            .radius(1000.0)
                            .strokeWidth(3f)
                            .strokeColor(Color.BLUE)
                            .fillColor(Color.argb(25, 0, 0, 255))
                    )
                    userRadius.isVisible = true;

                    // Move the camera to the current location
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    val sfu = LatLng(49.2791, -122.9202)
                    mMap.addMarker(MarkerOptions().position(sfu).title("SFU"))
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sfu, 15f))

                    // Move the camera to the current location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                } else {
                    // Handle case where location is null
                    // You might want to notify the user or provide a default location
                }
            }
        }, 2000);


        for (stop in nearbyViewModel.stopList) {
            val newStopLatLng = LatLng(stop.latitude.toDouble(), stop.longitude.toDouble())
            mMap.addMarker(MarkerOptions().position(newStopLatLng).title(stop.stopNumber + " - " + stop.stopName))
        }
    }

    private fun getLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)

            return
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}