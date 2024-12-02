package ca.wheresthebus.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

object Utils {
    fun checkLocationPermission(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}