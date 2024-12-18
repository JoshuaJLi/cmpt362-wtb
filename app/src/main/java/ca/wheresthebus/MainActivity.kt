package ca.wheresthebus

import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ca.wheresthebus.databinding.ActivityMainBinding
import ca.wheresthebus.service.BusNotifierService
import com.google.android.material.navigation.NavigationBarView
import android.Manifest
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import ca.wheresthebus.service.LiveNotificationService.Companion.ACTION_NAVIGATE_TO_TRIP
import ca.wheresthebus.utils.StaticDataLoadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController : NavController
    private val mainDBViewModel: MainDBViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpTheme()
        setUpNavBar()
        // loadStaticDataToDB() // NOTE: uncomment only if you need to generate realm from static data

        handleIncomingIntent(intent)

        requestAllPermissions()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            ACTION_NAVIGATE_TO_TRIP -> navController
                .navigate(R.id.action_trip_fragment)

            NfcAdapter.ACTION_TECH_DISCOVERED -> {
                Intent(this, BusNotifierService::class.java).also {
                    startService(it)
                }
                moveTaskToBack(true)
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun loadStaticDataToDB() {
        // Only load static data if we haven't done it yet
        if (!mainDBViewModel.isStaticDataLoaded()) {
            println("loading data from static resource files...")
            val context = this
            lifecycleScope.launch(Dispatchers.IO) {
                StaticDataLoadHelper.populateRealmDatabase(
                    context,
                    mainDBViewModel.getRealm()
                )
            }
        }
    }

    /**
     * i tried to put this within its own controller, but requesting permissions requires its own lifecycle
     * not sure of how to give a static object its own lifecycle, more on that later maybe
     * TODO: define a proper permissions workflow for if the user denies permissions more than twice
     * android 11 and up stops asking after the 2nd time and the user must go to settings to enable manually
     */
    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            val requestMultiplePermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("MainActivity", "${it.key} = ${it.value}")
                }
            }
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

    }

    private fun setUpTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeValue = sharedPreferences.getString("appearance", "system")
        val themeMode = when (themeValue) {
            getString(R.string.preference_appearance_light_value)  -> AppCompatDelegate.MODE_NIGHT_NO
            getString(R.string.preference_appearance_dark_value) -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun setUpNavBar() {
        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_trips,
                R.id.navigation_nearby,
                R.id.navigation_settings
            )
        )

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarHeight = insets.systemWindowInsetTop
            view.setPadding(
                view.paddingLeft,
                statusBarHeight,
                view.paddingRight,
                view.paddingBottom
            )
            insets.consumeSystemWindowInsets()
        }

        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
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