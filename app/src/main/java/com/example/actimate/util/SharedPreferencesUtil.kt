package com.example.actimate.util

import android.content.Context
import android.util.Log
import com.example.actimate.viewmodel.Prediction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SharedPreferencesUtils"

/**
 * Gets shared preferences with the given name and key
 */
fun getSharedPreferences(context: Context, name: String, key: String): Map<String, String>? {
    val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    val jsonString = sharedPreferences.getString(key, null) ?: return null

    val gson = Gson()
    val type: Type = object : TypeToken<Map<String, String>>() {}.type
    return gson.fromJson(jsonString, type)
}

/**
 * Sets shared preferences with the given name and key
 */
fun setSharedPreferences(context: Context, data: Map<String, String>, name: String, key: String): Boolean {
    try {
        val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = gson.toJson(data)

        sharedPreferences.edit().putString(key, jsonString).apply()
        return true
    } catch (e: Exception) {
        Log.e(TAG, "Error saving data to SharedPreferences: ${e.message}")
        return false
    }
}

/**
 * Adds a prediction to shared preferences
 */
fun addPredictionToSharedPreferences(context: Context, prediction: Prediction, prefsName: String): Boolean {
    try {
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val gson = Gson()

        // Get the existing predictions list or create a new one
        val existingPredictionsJson = sharedPreferences.getString("predictions_list", null)
        val type: Type = object : TypeToken<List<Prediction>>() {}.type

        val predictions = if (existingPredictionsJson != null) {
            try {
                gson.fromJson<List<Prediction>>(existingPredictionsJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing existing predictions: ${e.message}")
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        // Safety check for null predictions list
        val mutablePredictions = predictions?.toMutableList() ?: mutableListOf()

        // Add the new prediction
        mutablePredictions.add(prediction)

        // Save the updated list
        val updatedJson = gson.toJson(mutablePredictions)
        sharedPreferences.edit().putString("predictions_list", updatedJson).apply()

        return true
    } catch (e: Exception) {
        Log.e(TAG, "Error saving prediction to SharedPreferences: ${e.message}")
        return false
    }
}

/**
 * Gets today's predictions from shared preferences
 */
fun getTodayPredictionsFromSharedPreferences(context: Context, prefsName: String): List<Prediction> {
    try {
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val gson = Gson()

        // Get the predictions list
        val predictionsJson = sharedPreferences.getString("predictions_list", null) ?: return emptyList()

        // Parse the list
        val type: Type = object : TypeToken<List<Prediction>>() {}.type
        val allPredictions = try {
            gson.fromJson<List<Prediction>>(predictionsJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing predictions: ${e.message}")
            emptyList<Prediction>()
        }

        // Get today's date for filtering
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Filter to today's predictions
        return allPredictions.filter { prediction ->
            try {
                // Convert timestamp to long and then to date
                val timestamp = prediction.timestamp.toLongOrNull() ?: 0L
                val predictionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                predictionDate == today
            } catch (e: Exception) {
                Log.e(TAG, "Error comparing dates: ${e.message}")
                false
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting predictions from SharedPreferences: ${e.message}")
        return emptyList()
    }
}

/**
 * Clears old predictions from shared preferences
 */
fun clearSharedPreferencesForNewDay(context: Context, prefsName: String) {
    try {
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        // Clear the predictions list
        sharedPreferences.edit().remove("predictions_list").apply()
        Log.d(TAG, "Cleared predictions for new day")
    } catch (e: Exception) {
        Log.e(TAG, "Error clearing SharedPreferences: ${e.message}")
    }
}