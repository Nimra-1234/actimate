package com.example.actimate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.actimate.database.ActivityPrediction
import com.example.actimate.database.AppDatabaseProvider
import com.example.actimate.processor.CaloriesDataProcessor
import com.example.actimate.ui.components.ActivityData
import com.example.actimate.util.getSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val TAG = "PredictedActivityVM"

/**
 * ViewModel for handling predicted activity data for the Today screen
 */
class PredictedActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _predictedActivityData = MutableStateFlow<List<ActivityWithDuration>>(emptyList())
    val predictedActivityData: StateFlow<List<ActivityWithDuration>> = _predictedActivityData

    private val _totalCaloriesBurned = MutableLiveData<Float>(0f)
    val totalCaloriesBurned: LiveData<Float> = _totalCaloriesBurned

    private val weightString by lazy {
        (getSharedPreferences(getApplication(), "userdata", "user_data_key")?.get("weight")).toString()
    }
    private val weight by lazy {
        val parsedWeight = weightString.toFloatOrNull()?.toInt() ?: 70
        Log.d(TAG, "Using weight: $parsedWeight kg for calorie calculations")
        parsedWeight
    }

    init {
        loadPredictedActivityData()
    }

    /**
     * Loads today's activity predictions from the database
     */
    fun loadPredictedActivityData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabaseProvider.getInstance(getApplication())
                val dao = database.activityPredictionDao()

                // Format current date as yyyy-MM-dd for query
                val today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)
                Log.d(TAG, "Loading data for today: $today")

                // Get today's predictions
                var predictions: List<ActivityPrediction> = emptyList()

                try {
                    // Try using the date-filtered method first
                    predictions = dao.getPredictionsByDate(today)
                    Log.d(TAG, "Loaded ${predictions.size} predictions for today (${today}) using getPredictionsByDate")
                } catch (e: Exception) {
                    // Fallback to getting all and filtering manually
                    Log.d(TAG, "Falling back to getAllActivityPredictions due to: ${e.message}")
                    val allPredictions = dao.getAllActivityPredictions()
                    Log.d(TAG, "Retrieved ${allPredictions.size} total predictions")

                    // Filter to today's predictions
                    predictions = allPredictions.filter { prediction ->
                        val predictionDate = try {
                            val dateTime = LocalDateTime.parse(prediction.processedAt,
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            dateTime.format(DateTimeFormatter.ISO_DATE)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing date: ${prediction.processedAt}", e)
                            ""  // Skip records with invalid dates
                        }

                        // Debug each prediction's date
                        Log.d(TAG, "Prediction date: $predictionDate vs Today: $today, Match: ${predictionDate == today}")

                        predictionDate == today
                    }
                    Log.d(TAG, "Filtered to ${predictions.size} predictions for today (${today})")
                }

                if (predictions.isNotEmpty()) {
                    // Log some sample predictions for debugging
                    predictions.take(3).forEach { prediction ->
                        Log.d(TAG, "Sample prediction: ${prediction.label} at ${prediction.processedAt}")
                    }

                    // Process predictions to get durations and ensure they're sorted by time
                    val sortedPredictions = predictions.sortedBy {
                        try {
                            LocalDateTime.parse(it.processedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        } catch (e: Exception) {
                            LocalDateTime.now() // Fallback to current time for invalid timestamps
                        }
                    }

                    // Calculate durations more accurately
                    val activitiesWithDuration = calculateActivityDurations(sortedPredictions)

                    // Update the StateFlow with the new data
                    withContext(Dispatchers.Main) {
                        _predictedActivityData.value = activitiesWithDuration
                    }

                    // Process calories data
                    calculateTotalCalories(activitiesWithDuration)
                } else {
                    Log.d(TAG, "No activity predictions for today")
                    withContext(Dispatchers.Main) {
                        _predictedActivityData.value = emptyList()
                        _totalCaloriesBurned.value = 0f
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading activity predictions", e)
                withContext(Dispatchers.Main) {
                    _predictedActivityData.value = emptyList()
                    _totalCaloriesBurned.value = 0f
                }
            }
        }
    }

    /**
     * Calculates the duration for each activity type based on prediction timestamps
     */
    private fun calculateActivityDurations(predictions: List<ActivityPrediction>): List<ActivityWithDuration> {
        if (predictions.isEmpty()) return emptyList()

        val activityDurations = mutableMapOf<String, Double>()

        // Default time interval in minutes if we can't calculate from timestamps
        val defaultTimeInterval = 0.16667 // 10 seconds in minutes

        // Process predictions to calculate durations
        for (i in predictions.indices) {
            val currentPrediction = predictions[i]
            val label = currentPrediction.label ?: "unknown"

            // Calculate duration based on time difference to next prediction
            val durationMinutes = if (i < predictions.size - 1) {
                try {
                    // Parse timestamps
                    val currentTime = LocalDateTime.parse(currentPrediction.processedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val nextTime = LocalDateTime.parse(predictions[i + 1].processedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                    // Calculate duration in minutes
                    val seconds = ChronoUnit.SECONDS.between(currentTime, nextTime)
                    seconds / 60.0 // Convert to minutes
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating duration: ${e.message}")
                    defaultTimeInterval // Default duration if calculation fails
                }
            } else {
                // Last prediction - use default interval
                defaultTimeInterval
            }

            // Update total duration for this activity
            activityDurations[label] = (activityDurations[label] ?: 0.0) + durationMinutes
        }

        // Log the calculated durations
        activityDurations.forEach { (label, duration) ->
            Log.d(TAG, "Activity: $label, Total Duration: $duration minutes")
        }

        // Convert to list of ActivityWithDuration
        return activityDurations.map { (label, duration) ->
            ActivityWithDuration(
                label = label,
                durationMinutes = duration
            )
        }
    }

    /**
     * Calculates total calories burned from activity durations
     */
    private fun calculateTotalCalories(activities: List<ActivityWithDuration>) {
        var totalCalories = 0f

        activities.forEach { activity ->
            // Calculate calories treating duration as minutes
            val calories = CaloriesDataProcessor.processMeasurementsMinutes(
                weight,
                activity.label,
                activity.durationMinutes.toFloat()
            )

            totalCalories += calories

            Log.d(TAG, "Calories for ${activity.label} (${activity.durationMinutes} min): $calories")
        }

        Log.d(TAG, "Total calories calculated: $totalCalories")

        viewModelScope.launch(Dispatchers.Main) {
            // Update the LiveData with the total calories
            _totalCaloriesBurned.value = totalCalories
            Log.d(TAG, "Updated _totalCaloriesBurned LiveData to: $totalCalories")
        }
    }

    /**
     * Force update calories calculation (for debugging)
     */
    fun forceCaloriesUpdate() {
        viewModelScope.launch {
            calculateTotalCalories(_predictedActivityData.value)
        }
    }
}

/**
 * Data class to hold activity type and its duration
 */
data class ActivityWithDuration(
    val label: String,
    val durationMinutes: Double
)