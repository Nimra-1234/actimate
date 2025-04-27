package com.example.actimate.ui.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.example.actimate.databinding.TodayScreenBinding
import com.example.actimate.processor.CaloriesDataProcessor
import com.example.actimate.ui.components.ActivityData
import com.example.actimate.ui.components.HistogramActivityChart
import com.example.actimate.ui.components.CaloriesBurnedChart
import com.example.actimate.util.GoogleFitHelper
import com.example.actimate.util.getSharedPreferences
import com.example.actimate.viewmodel.ActivityWithDuration
import com.example.actimate.viewmodel.GoogleFitViewModel
import com.example.actimate.viewmodel.PredictedActivityViewModel
import com.example.actimate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

private const val TAG = "TodayScreenActivity"

class TodayScreenActivity : Fragment(), GoogleFitHelper.PermissionResultListener {
    private var binding: TodayScreenBinding? = null

    private lateinit var viewModel: PredictedActivityViewModel
    private lateinit var googleFitViewModel: GoogleFitViewModel
    private lateinit var googleFitHelper: GoogleFitHelper

    private val caloriesData = mutableStateOf<List<ActivityData>>(emptyList())
    private val classes = mutableStateOf<List<String>>(emptyList())
    private val durations = mutableStateOf<Map<String, Double>>(emptyMap())  // Store duration for each class
    private var totalCaloriesBurned = mutableStateOf(0f)
    private val weightString by lazy {
        (getSharedPreferences(requireContext(), "userdata", "user_data_key")?.get("weight")).toString()
    }
    private val weight by lazy {
        val parsedWeight = weightString.toFloatOrNull()?.toInt() ?: 70
        Log.d(TAG, "Using weight: $parsedWeight kg for calorie calculations")
        parsedWeight
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 10000L // 10 seconds

    // Track if we're currently in the process of requesting permissions
    private var isRequestingPermissions = false

    // Flag to indicate whether to use Google Fit calories or app calories
    private val useGoogleFitCalories = true

    private val runnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Refreshing data at ${System.currentTimeMillis()}")
            viewModel.loadPredictedActivityData()  // Refresh the activity data from app database
            googleFitViewModel.refreshFitData()    // Refresh fitness data
            handler.postDelayed(this, updateInterval)  // Schedule next refresh
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TodayScreenBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels
        viewModel = ViewModelProvider(requireActivity())[PredictedActivityViewModel::class.java]
        googleFitViewModel = ViewModelProvider(requireActivity())[GoogleFitViewModel::class.java]

        // Initialize GoogleFitHelper and set the listener
        googleFitHelper = GoogleFitHelper(requireContext())
        googleFitHelper.setPermissionResultListener(this)

        // Setup Compose views
        setupComposeViews()

        // Initial data load
        viewModel.loadPredictedActivityData()

        // Check Google Fit permissions and request if needed
        checkAndRequestGoogleFitPermissions()

        // Observe data changes
        observeData()

        // Start periodic data refreshing
        handler.post(runnable)

        // Force initial UI update
        updateTotalCalories(0f)
    }

    private fun setupComposeViews() {
        // HistogramActivityChart shows app activity durations
        binding?.histogramChartCompose?.setContent {
            val currentDurations by durations
            HistogramActivityChart(durations = currentDurations)
        }

        // CaloriesBurnedChart shows Google Fit weekly calories data
        binding?.caloriesChartCompose?.setContent {
            val currentCaloriesData by caloriesData
            CaloriesBurnedChart(activityData = currentCaloriesData)
        }
    }

    private fun observeData() {
        // Based on the flag, we'll either use Google Fit calories or app calories
        if (useGoogleFitCalories) {
            // If using Google Fit calories, observe only the fitness data
            googleFitViewModel.fitCaloriesBurned.observe(viewLifecycleOwner) { fitCalories ->
                Log.d(TAG, "Using Google Fit calories: $fitCalories")
                totalCaloriesBurned.value = fitCalories
                updateTotalCalories(fitCalories)
            }
        } else {
            // If using app calories, observe both and combine them
            viewModel.totalCaloriesBurned.observe(viewLifecycleOwner) { appCalories ->
                Log.d(TAG, "Observed app calories: $appCalories")
                googleFitViewModel.fitCaloriesBurned.observe(viewLifecycleOwner) { fitCalories ->
                    Log.d(TAG, "Combined with Google Fit calories: $fitCalories")
                    val totalCalories = appCalories + fitCalories
                    totalCaloriesBurned.value = totalCalories
                    updateTotalCalories(totalCalories)
                }
            }
        }

        // Use lifecycleScope to collect flow from ViewModel
        lifecycleScope.launch {
            viewModel.predictedActivityData.collectLatest { activities ->
                Log.d(TAG, "Collected activity data: ${activities.size} activities")

                if (activities.isEmpty()) {
                    Log.d(TAG, "No activity data received")
                    durations.value = emptyMap()
                    return@collectLatest
                }

                // Create map of activity durations for the histogram - THIS MUST ALWAYS COME FROM APP DATABASE
                val durationsMap = activities.associate { it.label to it.durationMinutes }
                durations.value = durationsMap

                // Log each activity duration for debugging
                durationsMap.forEach { (activity, duration) ->
                    Log.d(TAG, "Activity duration: $activity = $duration minutes")
                }

                // Map the activity labels
                classes.value = activities.map { it.label }
            }
        }

        // Observe Google Fit data
        observeGoogleFitData()

        // Load weekly calories data from Google Fit
        loadWeeklyCaloriesData()
    }

    private fun observeGoogleFitData() {
        // Observe Google Fit permissions status
        googleFitViewModel.hasPermissions.observe(viewLifecycleOwner) { hasPermissions ->
            if (hasPermissions) {
                googleFitViewModel.refreshFitData()
                loadWeeklyCaloriesData()
            }
        }

        // Observe step count
        googleFitViewModel.stepCount.observe(viewLifecycleOwner) { steps ->
            binding?.stepsCount?.text = steps.toString()
            binding?.stepsProgressBar?.progress = steps

            // Update progress text
            val stepGoal = googleFitViewModel.stepGoal.value ?: 10000
            binding?.stepsProgressText?.text = "$steps / $stepGoal steps"

            // Check for goal achievement and trigger animation if needed
            if (googleFitViewModel.goalAchieved.value == true) {
                playGoalAchievedAnimation()
            }
        }

        // Observe distance walked
        googleFitViewModel.distanceWalked.observe(viewLifecycleOwner) { distance ->
            val distanceKm = distance / 1000f
            val formattedDistance = String.format("%.1f", distanceKm)
            binding?.distanceValue?.text = formattedDistance
        }

        // Observe Google Fit calories for display in the chip
        googleFitViewModel.fitCaloriesBurned.observe(viewLifecycleOwner) { fitCalories ->
            binding?.caloriesCount?.text = fitCalories.toInt().toString()
        }
    }

    /**
     * Load weekly calories data from Google Fit for the chart
     */
    private fun loadWeeklyCaloriesData() {
        // We'll update this method to fetch and display the last week's calories data
        googleFitViewModel.getWeeklyCaloriesData()

        // Observe the weekly calories data
        lifecycleScope.launch {
            googleFitViewModel.weeklyCaloriesData.collectLatest { weeklyData ->
                caloriesData.value = weeklyData
                Log.d(TAG, "Updated calories chart with weekly data: ${weeklyData.size} data points")
            }
        }
    }

    private fun checkAndRequestGoogleFitPermissions() {
        // First check if we have permissions
        googleFitViewModel.checkPermissions()

        // If we don't, request them after a short delay (to avoid immediate pop-up)
        handler.postDelayed({
            if (googleFitViewModel.hasPermissions.value != true && !isRequestingPermissions) {
                requestGoogleFitPermissions()
            }
        }, 1000) // 1 second delay
    }

    private fun requestGoogleFitPermissions() {
        if (isRequestingPermissions) return

        isRequestingPermissions = true
        googleFitHelper.requestPermissions(requireActivity())
    }

    /**
     * Implementation of GoogleFitHelper.PermissionResultListener
     */
    override fun onPermissionResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            GoogleFitHelper.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> {
                isRequestingPermissions = false

                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Google Fit permissions granted")
                    googleFitViewModel.checkPermissions()
                    googleFitViewModel.refreshFitData()
                    loadWeeklyCaloriesData()
                } else {
                    Log.e(TAG, "Google Fit permissions denied")
                    // Simply log the error, user experience continues without Google Fit data
                }
            }
        }
    }

    private fun playGoalAchievedAnimation() {
        // Skip animation if view is not available
        val celebrationIcon = binding?.goalAchievedIcon ?: return

        // Make celebration icon visible
        celebrationIcon.visibility = View.VISIBLE
        celebrationIcon.alpha = 0f

        // Create animation set
        val animatorSet = AnimatorSet()

        // Fade in
        val fadeIn = ObjectAnimator.ofFloat(celebrationIcon, "alpha", 0f, 1f)
        fadeIn.duration = 500

        // Scale up with overshoot
        val scaleX = ObjectAnimator.ofFloat(celebrationIcon, "scaleX", 0.5f, 1.2f, 1f)
        scaleX.duration = 700
        scaleX.interpolator = OvershootInterpolator()

        val scaleY = ObjectAnimator.ofFloat(celebrationIcon, "scaleY", 0.5f, 1.2f, 1f)
        scaleY.duration = 700
        scaleY.interpolator = OvershootInterpolator()

        // Rotation
        val rotate = ObjectAnimator.ofFloat(celebrationIcon, "rotation", 0f, 20f, -20f, 10f, -10f, 0f)
        rotate.duration = 800

        // Play animations together
        animatorSet.playTogether(fadeIn, scaleX, scaleY, rotate)

        // Start the animation
        animatorSet.start()

        // Hide the icon after a delay
        handler.postDelayed({
            celebrationIcon.animate()
                .alpha(0f)
                .setDuration(500)
                .start()
        }, 3000) // Hide after 3 seconds
    }

    private fun updateTotalCalories(calories: Float) {
        // Log the total calories
        Log.d(TAG, "Updating UI with total calories: $calories")

        // Update the TextView on the main thread
        activity?.runOnUiThread {
            // Update the large number display
            binding?.totalCaloriesNumber?.text = calories.toInt().toString()

            // Update the calories chip - only if we're using Google Fit calories
            if (useGoogleFitCalories) {
                binding?.caloriesCount?.text = calories.toInt().toString()
            }
        }
    }

    /**
     * Handle Google Sign In result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            GoogleFitHelper.GOOGLE_SIGN_IN_REQUEST_CODE -> {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "Google Sign In success, now requesting fitness permissions")

                    // Now that sign in is complete, request fitness permissions
                    googleFitHelper.requestPermissions(requireActivity())
                } catch (e: ApiException) {
                    Log.e(TAG, "Google Sign In failed: ${e.statusCode}", e)
                    isRequestingPermissions = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop the handler when the view is destroyed
        handler.removeCallbacks(runnable)
        binding = null
    }
}