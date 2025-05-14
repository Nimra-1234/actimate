package com.example.actimate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.actimate.ui.components.ActivityData
import com.example.actimate.util.GoogleFitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "GoogleFitViewModel"

/**
 * ViewModel for handling fitness data
 */
class GoogleFitViewModel(application: Application) : AndroidViewModel(application) {

    private val fitnessHelper = GoogleFitHelper(application.applicationContext)

    // Step count data
    private val _stepCount = MutableLiveData<Int>(0)
    val stepCount: LiveData<Int> = _stepCount

    // Daily step goal (default 10,000)
    private val _stepGoal = MutableLiveData<Int>(10000)
    val stepGoal: LiveData<Int> = _stepGoal

    // Distance walked data (in meters)
    private val _distanceWalked = MutableLiveData<Float>(0f)
    val distanceWalked: LiveData<Float> = _distanceWalked

    // Calories burned data from fitness API
    private val _fitCaloriesBurned = MutableLiveData<Float>(0f)
    val fitCaloriesBurned: LiveData<Float> = _fitCaloriesBurned

    // Flag to track if goal was achieved
    private val _goalAchieved = MutableLiveData<Boolean>(false)
    val goalAchieved: LiveData<Boolean> = _goalAchieved

    // Flow for hourly calories data (for chart)
    private val _hourlyFitCaloriesData = MutableStateFlow<List<ActivityData>>(emptyList())
    val hourlyFitCaloriesData: StateFlow<List<ActivityData>> = _hourlyFitCaloriesData

    // Flow for weekly calories data (for chart)
    private val _weeklyCaloriesData = MutableStateFlow<List<ActivityData>>(emptyList())
    val weeklyCaloriesData: StateFlow<List<ActivityData>> = _weeklyCaloriesData

    // Flag to track permission status
    private val _hasPermissions = MutableLiveData<Boolean>(false)
    val hasPermissions: LiveData<Boolean> = _hasPermissions

    init {
        // Initialize with current permission status
        _hasPermissions.value = GoogleFitHelper.hasPermissions(application.applicationContext)

        // Load data if we have permissions
        if (_hasPermissions.value == true) {
            refreshFitData()
            getWeeklyCaloriesData()
        }
    }

    /**
     * Check if we have fitness permissions
     */
    fun checkPermissions() {
        // Check actual permissions
        val hasPermissionsValue = GoogleFitHelper.hasPermissions(getApplication())
        _hasPermissions.value = hasPermissionsValue

        Log.d(TAG, "Fitness permissions status: $hasPermissionsValue")

        if (hasPermissionsValue) {
            refreshFitData()
            getWeeklyCaloriesData()
        }
    }

    /**
     * Refresh all fitness data
     */
    fun refreshFitData() {
        // Only refresh if we have permissions
        if (_hasPermissions.value != true) {
            Log.d(TAG, "Cannot refresh Fit data - permissions not granted")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get today's step count
                val steps = fitnessHelper.getTodayStepCount()

                // Get today's distance walked
                val distance = fitnessHelper.getTodayDistance()

                // Get today's calories burned
                val calories = fitnessHelper.getTodayCaloriesBurned()

                // Get hourly calories data
                val hourlyCalories = fitnessHelper.getHourlyCaloriesBurned()

                Log.d(TAG, "Fetched Google Fit data - Steps: $steps, Distance: $distance m, Calories: $calories")

                withContext(Dispatchers.Main) {
                    _stepCount.value = steps
                    _distanceWalked.value = distance
                    _fitCaloriesBurned.value = calories

                    // Check if step goal was achieved
                    val previouslyAchieved = _goalAchieved.value ?: false
                    val currentlyAchieved = steps >= (_stepGoal.value ?: 10000)

                    // Only update if there's a change in achievement status
                    if (previouslyAchieved != currentlyAchieved) {
                        _goalAchieved.value = currentlyAchieved
                    }

                    // Update hourly calories data for chart
                    updateHourlyCaloriesData(hourlyCalories)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing Google Fit data", e)
            }
        }
    }

    /**
     * Get weekly calories data for the chart
     */
    fun getWeeklyCaloriesData() {
        // Only refresh if we have permissions
        if (_hasPermissions.value != true) {
            Log.d(TAG, "Cannot get weekly data - permissions not granted")
            generateSimulatedWeeklyData() // Use simulated data if no permissions
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get data for the last 7 days
                val weeklyData = mutableListOf<ActivityData>()

                // Get the calendar instance for today
                val calendar = Calendar.getInstance()

                // Format for logging day of week
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

                // Go back 6 days to get a full week (today + 6 previous days)
                calendar.add(Calendar.DAY_OF_YEAR, -6)

                // For each day of the week
                for (i in 0 until 7) {
                    val startTimeMillis = calendar.timeInMillis
                    val dayOfWeek = dayFormat.format(calendar.time)

                    try {
                        // Get calories for this day
                        val dailyCalories = fitnessHelper.getDailyCaloriesBurned(startTimeMillis)

                        // Add to our list, using day index as the "hour" field to repurpose the ActivityData class
                        weeklyData.add(
                            ActivityData(
                                hour = i, // Using hour field to store day index
                                activityType = dayOfWeek, // Store day of week as activity type
                                caloriesBurned = dailyCalories
                            )
                        )

                        Log.d(TAG, "Weekly data: Day=$dayOfWeek, Calories=$dailyCalories")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting data for $dayOfWeek", e)
                        // Add zero calories for days with errors
                        weeklyData.add(
                            ActivityData(
                                hour = i,
                                activityType = dayOfWeek,
                                caloriesBurned = 0f
                            )
                        )
                    }

                    // Move to next day
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                withContext(Dispatchers.Main) {
                    _weeklyCaloriesData.value = weeklyData
                    Log.d(TAG, "Updated weekly calories data with ${weeklyData.size} days")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting weekly data", e)
                generateSimulatedWeeklyData() // Use simulated data if error
            }
        }
    }

    /**
     * Generate simulated weekly data for testing/fallback
     */
    private fun generateSimulatedWeeklyData() {
        viewModelScope.launch {
            val weeklyData = mutableListOf<ActivityData>()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val calendar = Calendar.getInstance()

            // Go back 6 days
            calendar.add(Calendar.DAY_OF_YEAR, -6)

            // Generate simulated data for each day
            for (i in 0 until 7) {
                val dayOfWeek = dayFormat.format(calendar.time)

                // Generate random calories between 800-2000
                val calories = (800 + Math.random() * 1200).toFloat()

                weeklyData.add(
                    ActivityData(
                        hour = i,
                        activityType = dayOfWeek,
                        caloriesBurned = calories
                    )
                )

                // Move to next day
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            _weeklyCaloriesData.value = weeklyData
            Log.d(TAG, "Updated with simulated weekly data")
        }
    }

    /**
     * Convert hourly calories data to ActivityData format for chart display
     */
    private fun updateHourlyCaloriesData(hourlyCalories: Map<Int, Float>) {
        val activityDataList = mutableListOf<ActivityData>()

        hourlyCalories.forEach { (hour, calories) ->
            activityDataList.add(
                ActivityData(
                    hour = hour,
                    activityType = "google_fit", // Special activity type for Google Fit data
                    caloriesBurned = calories
                )
            )
        }

        _hourlyFitCaloriesData.value = activityDataList
        Log.d(TAG, "Updated hourly calories data with ${activityDataList.size} entries")
    }

    /**
     * Update step goal value
     */
    fun updateStepGoal(goal: Int) {
        _stepGoal.value = goal

        // Update goal achievement status with new goal
        val steps = _stepCount.value ?: 0
        _goalAchieved.value = steps >= goal
    }
}