package com.example.actimate.activity
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.actimate.databinding.ActivityMainBinding
import com.example.actimate.service.UnifiedSensorService
import com.example.actimate.ui.screens.OnboardingSettings
import com.example.actimate.util.GoogleFitHelper
import com.example.actimate.util.SensorDataManager
import com.example.actimate.viewmodel.GoogleFitViewModel
import com.example.actimate.viewmodel.LastPredictionViewModel
import com.example.actimate.R
import android.graphics.Color


/**
 * MainActivity is the entry point of the app where sensor services are initialized and the UI is set.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    private val lastPredictionViewModel: LastPredictionViewModel by viewModels()
    private val googleFitViewModel: GoogleFitViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var permissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setup the toolbar
        setSupportActionBar(binding.toolbar)

        SensorDataManager.lastPredictionViewModel = lastPredictionViewModel

        // Check permissions first
        checkAndRequestPermissions()
    }

    /**
     * Check and request permissions if not granted
     */
    private fun checkAndRequestPermissions() {
        if (!allPermissionsGranted()) {
            Log.d("MainActivity", "Requesting permissions")
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        } else {
            permissionsGranted = true
            proceedWithAppInitialization()
        }
    }

    /**
     * Check if all required permissions are granted
     */
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted
                permissionsGranted = true
                proceedWithAppInitialization()
            } else {
                // Check if we should show rationale
                val shouldShowRationale = permissions.any {
                    shouldShowRequestPermissionRationale(it)
                }

                if (shouldShowRationale) {
                    // Show rationale dialog
                    showPermissionRationaleDialog()
                } else {
                    // User permanently denied permission, direct to settings
                    showSettingsDialog()
                }
            }
        }
    }

    /**
     * Handle activity results, including Google Fit permissions
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle Google Fit permissions result
        if (requestCode == GoogleFitHelper.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("MainActivity", "Google Fit permissions granted")
                googleFitViewModel.checkPermissions()
            } else {
                Log.e("MainActivity", "Google Fit permissions denied")
                Toast.makeText(
                    this,
                    "Google Fit permissions are required for step counting and distance tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Show rationale dialog when permission was denied
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs activity recognition and location permissions to track your activities accurately. Without these permissions, the app cannot function properly.")
            .setPositiveButton("Request Again") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "App requires permissions to function properly", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * Show dialog when permission was permanently denied
     */
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Some required permissions have been permanently denied. Please enable them in the app settings to use this app.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "App requires permissions to function properly", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /**
     * Proceed with app initialization after permissions are handled
     */
    private fun proceedWithAppInitialization() {
        // Check user data before initializing other components
        if (checkUserDataAndNavigate()) {
            // Battery optimization dialog removed here
            initializeAppComponents()
        }
        setupToolbarTitleBasedOnFragment()
    }

    private fun checkUserDataAndNavigate(): Boolean {
        val sharedPrefs = getSharedPreferences("userdata", MODE_PRIVATE)

        val hasAllData = sharedPrefs.contains("weight") &&
                sharedPrefs.contains("height") &&
                sharedPrefs.contains("age") &&
                sharedPrefs.contains("gender")

        return if (!hasAllData) {
            Log.d("MainActivity", "User data missing, showing OnboardingSettings fragment")

            // Hide navigation and main content
            binding.bottomNavigation.visibility = View.GONE
            binding.fragmentContainerView6.visibility = View.GONE
            binding.onboardingSettings.visibility = View.VISIBLE

            // Load the OnboardingSettings fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.onboardingSettings, OnboardingSettings())
                .commit()

            false
        } else {
            // Show main content
            binding.bottomNavigation.visibility = View.VISIBLE
            binding.fragmentContainerView6.visibility = View.VISIBLE
            binding.onboardingSettings.visibility = View.GONE
            true
        }
    }

    private fun initializeAppComponents() {
        // Only initialize if permissions are granted
        if (permissionsGranted) {
            startUnifiedSensorService()
            nav()
        }
    }
    /**
     *switch from onboardingsettings screen to main content
     **/
    fun switchToMainContent() {
        // Show main content
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fragmentContainerView6.visibility = View.VISIBLE
        binding.onboardingSettings.visibility = View.GONE

        // Initialize app components
        initializeAppComponents()
    }
    /**
     *function to set the navigation component using botomNavigation Nav_graph
     */
    private fun nav() {
        try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView6) as NavHostFragment
            val navController = navHostFragment.navController
            binding.bottomNavigation.setupWithNavController(navController)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up navigation", e)
        }
    }

    /**
     * Starts the unified sensor service in the foreground without checking the working mode.
     */
    private fun startUnifiedSensorService() {
        Log.d("MainActivity", "Starting UnifiedSensorService")

        val unifiedServiceIntent = Intent(this, UnifiedSensorService::class.java)
        try {
            startForegroundService(unifiedServiceIntent)
            Log.d("MainActivity", "UnifiedSensorService started successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start UnifiedSensorService", e)
        }
    }

    /**
     * Stops the UnifiedSensorService.
     */
    fun stopUnifiedSensorService() {
        // Make sure to stop the service when exiting
        val unifiedServiceIntent = Intent(this, UnifiedSensorService::class.java)
        stopService(unifiedServiceIntent)
        Log.d("MainActivity", "UnifiedSensorService stopped successfully.")
    }

    private fun setupToolbarTitleBasedOnFragment() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView6) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null) {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                val fragmentLabel = destination.label.toString()

                when (fragmentLabel) {
                    "today_screen" -> {
                        setToolbarTitle("Today's Insights")
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        invalidateOptionsMenu()  // This will trigger the menu to be recreated (including the Exit button)
                    }
                    "settings_screen" -> {
                        setToolbarTitle("Settings")
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        invalidateOptionsMenu()  // Ensure the Exit button is hidden when on other screens
                    }
                    "data_screen" -> {
                        setToolbarTitle("Data")
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        invalidateOptionsMenu()  // Ensure the Exit button is hidden when on other screens
                    }
                    else -> {
                        setToolbarTitle("ActiMate")
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                        invalidateOptionsMenu()  // Ensure the Exit button is hidden when on other screens
                    }
                }
            }
        } else {
            Log.e("MainActivity", "NavController is null, unable to set toolbar title")
        }
    }

    private fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_exit -> {
                item.icon?.setTintList(ColorStateList.valueOf(Color.RED))
                // Stop the UnifiedSensorService before exiting the app
                stopUnifiedSensorService()
                finish()  // This will close the app or activity when "Exit" is clicked
                true
            }
            android.R.id.home -> {
                // Use OnBackPressedDispatcher instead of the deprecated onBackPressed
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Get the current fragment displayed in the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView6) as? NavHostFragment
        val navController = navHostFragment?.navController
        val currentDestination = navController?.currentDestination

        // Check if the current fragment is the "Today Screen"
        if (currentDestination != null && currentDestination.label == "settings_screen") {
            // Inflate the menu only if we are on the "Today" screen
            menuInflater.inflate(R.menu.toolbar_menu, menu)
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        // Check permissions again when the app resumes, in case the user changed them in settings
        if (!permissionsGranted && allPermissionsGranted()) {
            permissionsGranted = true
            proceedWithAppInitialization()
        }

        // Check Google Fit permissions status
        if (permissionsGranted) {
            googleFitViewModel.checkPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
    }
}