package ca.wheresthebus.ui.nearby

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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

    private var _binding: FragmentNearbyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // for testing purposes
    private var stopList: ArrayList<Stop> = ArrayList<Stop>()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NearbyViewModel::class.java)

        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textNotifications
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val minput = InputStreamReader(context?.assets?.open("stops.csv"))
        val reader = BufferedReader(minput)

        var line : String?
        var displayData : String = ""
        while (reader.readLine().also { line = it } != null){
            val row : List<String> = line!!.split(",")
//            var newStop = Stop(row[1], row[2], row[4], row[5])
//            stopList.add(newStop)
//            displayData = displayData + row[4] + "\t" + row[5] + "\n"
            try {
                val stopId = row[1] // Change index based on your CSV structure
                val stopName = row[2]
                val stopLat = row[4].toDouble() // Make sure this column has valid numeric values
                val stopLon = row[5].toDouble() // Make sure this column has valid numeric values

                // Create a new Stop object
                val newStop = Stop(stopId, stopName, stopLat, stopLon)
                stopList.add(newStop)
            } catch (e: NumberFormatException) {
                println("Failed to parse row: ${line}. Error: ${e.message}")
                // Handle the error or skip the row
            } catch (e: IndexOutOfBoundsException) {
                println("Row has insufficient columns: $line")
            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.poopy) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        val goButton: Button = view.findViewById(R.id.getNextBusButton)
//        val stopNumberInput: EditText = view.findViewById(R.id.stopNumberInputField)
//        goButton.setOnClickListener() {
//            Toast.makeText(requireContext(), stopNumberInput.text, Toast.LENGTH_SHORT).show()
//        }
        getLocationPermissions()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Get the last known location
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Get the location's latitude and longitude
                val currentLatLng = LatLng(location.latitude, location.longitude)

                // Add a marker at the current location
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                // Move the camera to the current location
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                val sfu = LatLng(49.2791, -122.9202)
                mMap.addMarker(MarkerOptions().position(sfu).title("SFU"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sfu, 15f))
            } else {
                // Handle case where location is null
                // You might want to notify the user or provide a default location
            }
        }
        // Add a marker at a specific location and move the camera to it.
//        val sydney = LatLng(stopList[12].latitude.toDouble(), stopList[12].longitude.toDouble())
//        mMap.addMarker(MarkerOptions().position(sydney).title("Lol"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        for (stop in stopList) {
            val newStopLatLng = LatLng(stop.latitude.toDouble(), stop.longitude.toDouble())
            mMap.addMarker(MarkerOptions().position(newStopLatLng).title(stop.stopNumber + " - " + stop.stopName))
        }
    }

    private fun getLocationPermissions() {
        val currentLocation = fusedLocationProviderClient.lastLocation
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