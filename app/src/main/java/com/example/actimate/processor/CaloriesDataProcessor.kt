package com.example.actimate.processor

import android.util.Log

private const val TAG = "CaloriesProcessor"

/**
 * Object that calculates the number of calories burned based on the user's weight, activity type, and time spent.
 */
object CaloriesDataProcessor {

    /**
     * Processes the given measurements and calculates the number of calories burned.
     *
     * @param weight The user's weight in kilograms.
     * @param activityType The activity type detected by the model (e.g., "walking", "running", "standing").
     * @param timeInHours The duration of the activity in HOURS.
     * @return The estimated number of calories burned.
     */
    fun processMeasurements(weight: Int, activityType: String, timeInHours: Float): Float {
        // Check if time input is invalid
        if (timeInHours <= 0) {
            Log.w(TAG, "Invalid time input: $timeInHours hours for activity: $activityType")
            return 0f
        }

        // Log input values for debugging
        Log.d(TAG, "Calculating calories for: Activity=$activityType, Weight=$weight kg, Time=$timeInHours hours")

        // Map activity type detected by our model to appropriate MET values
        // MET (Metabolic Equivalent of Task) values based on the Compendium of Physical Activities
        val met = when (activityType.lowercase()) {
            "walking" -> 3.5      // Walking at moderate pace (~3mph)
            "running" -> 8.0      // Running/jogging (~5mph)
            "standing" -> 1.5     // Standing quietly
            "downstairs" -> 3.0   // Walking downstairs
            "upstairs" -> 4.0     // Walking upstairs
            "unknown" -> 1.5      // Default to light activity if unknown
            else -> 1.5           // Default to light activity if unrecognized
        }

        // Calculate calories using the formula:
        // Calories burned = weight (kg) * MET * time (hours)
        // This formula gives kilocalories (kcal)
        val caloriesBurned = weight * met * timeInHours

        Log.d(TAG, "Calculated $caloriesBurned calories (Weight=$weight kg × MET=$met × Time=$timeInHours hrs)")

        // Ensure we don't return negative or unreasonably large values
        return when {
            caloriesBurned < 0 -> 0f
            caloriesBurned > 1000 -> 1000f // Cap at a reasonable maximum per calculation
            else -> caloriesBurned.toFloat()
        }
    }

    /**
     * Alternative method that accepts time in minutes for easier use
     *
     * @param weight The user's weight in kilograms.
     * @param activityType The activity type detected by the model.
     * @param timeInMinutes The duration of the activity in MINUTES.
     * @return The estimated number of calories burned.
     */
    fun processMeasurementsMinutes(weight: Int, activityType: String, timeInMinutes: Float): Float {
        // Convert minutes to hours and use the main method
        val timeInHours = timeInMinutes / 60f
        return processMeasurements(weight, activityType, timeInHours)
    }
}