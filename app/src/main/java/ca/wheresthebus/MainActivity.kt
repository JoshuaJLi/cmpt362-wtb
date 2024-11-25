package ca.wheresthebus

import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.enableSavedStateHandles
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ca.wheresthebus.databinding.ActivityMainBinding
import ca.wheresthebus.service.NfcService
import com.google.android.material.navigation.NavigationBarView
import android.Manifest
import androidx.lifecycle.lifecycleScope
import ca.wheresthebus.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainDBViewModel: MainDBViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpNavBar()

        requestNotificationPermission()

        if (isStartedByNFC(intent)) {
            NfcService.handleTap(this)
            moveTaskToBack(true)
        }

        loadStaticDataToDB()
    }

    private fun loadStaticDataToDB() {
        // Only load static data if we haven't done it yet
        if (!mainDBViewModel.isStaticDataLoaded()) {
            val context = this
            lifecycleScope.launch(Dispatchers.IO) { Utils.populateRealmDatabase(context, mainDBViewModel.getRealm()) }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun isStartedByNFC(intent: Intent?): Boolean {
        return intent != null && intent.action == NfcAdapter.ACTION_TECH_DISCOVERED
    }

    private fun setUpNavBar() {
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_trips, R.id.navigation_nearby, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        NavigationBarView.OnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    // Respond to navigation item 1 click
                    true
                }
                R.id.navigation_trips -> {
                    // Respond to navigation item 2 click
                    true
                }
                R.id.navigation_nearby -> {

                    true
                }
                R.id.navigation_settings -> {
                    true
                }
                else -> false
            }
        }
    }
}