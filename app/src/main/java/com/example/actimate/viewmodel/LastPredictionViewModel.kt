package com.example.actimate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.actimate.util.addPredictionToSharedPreferences
import com.example.actimate.util.getTodayPredictionsFromSharedPreferences
import com.example.actimate.util.clearSharedPreferencesForNewDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "LastPredictionViewModel"
private const val MIN_TIME_BETWEEN_RECORDS_MS = 30000 // 30 seconds minimum between recorded activities

// Data class for prediction with timestamp and label
data class Prediction(
    val timestamp: String,
    val label: String
)

class LastPredictionViewModel(application: Application) : AndroidViewModel(application) {

    private val _lastPredictionData = MutableLiveData<String?>()
    val lastPredictionData: LiveData<String?> = _lastPredictionData

    private val _recentPredictions = MutableLiveData<List<Prediction>>(emptyList())
    val recentPredictions: LiveData<List<Prediction>> = _recentPredictions

    // Track the last recorded activity label and timestamp
    private var lastRecordedActivity: String? = null
    private var lastRecordedTimeMs: Long = 0

    init {
        // Initialize with null
        _lastPredictionData.value = null
        // Load initial recent predictions
        loadRecentPredictions()
    }

    private fun loadRecentPredictions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext

                // Step 1: Get all the predictions stored in SharedPreferences for today
                val rawPredictions = getTodayPredictionsFromSharedPreferences(context, "predictions")
                Log.d(TAG, "Loaded ${rawPredictions.size} predictions from SharedPreferences")

                // Step 2: Check if we have any predictions and if the last prediction's date is different from today
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                var shouldClearOldData = false

                if (rawPredictions.isNotEmpty()) {
                    try {
                        // If last prediction is not null, check its date
                        val lastPrediction = rawPredictions.last()
                        val lastPredictionTs = lastPrediction.timestamp.toLongOrNull() ?: 0L
                        if (lastPredictionTs > 0) {
                            lastRecordedTimeMs = lastPredictionTs
                            lastRecordedActivity = lastPrediction.label

                            val lastPredictionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(Date(lastPredictionTs))

                            if (lastPredictionDate != currentDate) {
                                // If the last prediction's date is not today, clear SharedPreferences
                                shouldClearOldData = true
                                Log.d(TAG, "Last prediction date ($lastPredictionDate) is different from today ($currentDate)")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking last prediction date: ${e.message}")
                        // Don't clear data on error, just continue
                    }
                }

                if (shouldClearOldData) {
                    // Clear SharedPreferences and save the current prediction
                    clearSharedPreferencesForNewDay(context, "predictions")
                    Log.d(TAG, "Cleared old prediction data")

                    // Update recent predictions with an empty list
                    _recentPredictions.postValue(emptyList())
                } else {
                    // Update predictions with the list from SharedPreferences but limit to max 20 entries
                    val limitedPredictions = if (rawPredictions.size > 20) {
                        rawPredictions.takeLast(100)
                    } else {
                        rawPredictions
                    }
                    _recentPredictions.postValue(limitedPredictions)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading recent predictions: ${e.message}")
                // On error, just provide an empty list
                _recentPredictions.postValue(emptyList())
            }
        }
    }

    fun updateLastPredictionData(lastPrediction: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Always update the current activity display
                _lastPredictionData.postValue(lastPrediction)

                val currentTimeMs = System.currentTimeMillis()

                // Determine if we should record this prediction based on:
                // 1. Is it different from the last recorded activity?
                // 2. Has enough time passed since the last recording?
                val activityChanged = lastPrediction != lastRecordedActivity
                val timeSinceLastRecordMs = currentTimeMs - lastRecordedTimeMs
                val enoughTimeElapsed = timeSinceLastRecordMs >= MIN_TIME_BETWEEN_RECORDS_MS

                // Only record the prediction if it's a new activity or enough time has passed
                if (activityChanged || enoughTimeElapsed) {
                    Log.d(TAG, "Recording prediction: $lastPrediction (changed: $activityChanged, time elapsed: ${timeSinceLastRecordMs}ms)")

                    val context = getApplication<Application>().applicationContext
                    val timestamp = currentTimeMs.toString()
                    val prediction = Prediction(timestamp = timestamp, label = lastPrediction)

                    val saveSuccess = addPredictionToSharedPreferences(context, prediction, "predictions")
                    if (saveSuccess) {
                        // Update our tracking variables
                        lastRecordedActivity = lastPrediction
                        lastRecordedTimeMs = currentTimeMs

                        // Reload the predictions to update the UI
                        loadRecentPredictions()
                    } else {
                        Log.e(TAG, "Failed to save prediction")
                    }
                } else {
                    Log.d(TAG, "Skipping recording for $lastPrediction - not enough time elapsed (${timeSinceLastRecordMs}ms)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating prediction: ${e.message}")
            }
        }
    }

    // Helper method to get a safe prediction at a specific index
    fun getPredictionAt(index: Int): Prediction? {
        val predictions = _recentPredictions.value
        return if (predictions != null && index >= 0 && index < predictions.size) {
            predictions[index]
        } else {
            null
        }
    }
}