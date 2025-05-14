package com.example.actimate.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Helper class to manage fitness data API interactions
 */
class GoogleFitHelper(private val context: Context) {
    companion object {
        private const val TAG = "GoogleFitHelper"
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 1000

        // These are the fitness data types we'll use
        private val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .build()

        /**
         * Check if permissions have been granted
         */
        fun hasPermissions(context: Context): Boolean {
            return try {
                val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                GoogleSignIn.hasPermissions(account, fitnessOptions)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking fitness permissions", e)
                false
            }
        }
    }

    /**
     * Request fitness data permissions
     */
    fun requestPermissions(activity: Activity) {
        try {
            Log.d(TAG, "Requesting fitness permissions")

            // Check if we already have an account
            val account = GoogleSignIn.getLastSignedInAccount(context)

            // Use Google Sign In for basic authentication first
            if (account == null) {
                Log.d(TAG, "No Google account found, requesting sign in first")

                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(activity, gso)
                activity.startActivityForResult(
                    googleSignInClient.signInIntent,
                    GOOGLE_SIGN_IN_REQUEST_CODE
                )
                return
            }

            // Then request Fitness permissions
            val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

            if (!GoogleSignIn.hasPermissions(fitnessAccount, fitnessOptions)) {
                Log.d(TAG, "Requesting Fitness permissions")
                GoogleSignIn.requestPermissions(
                    activity,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    fitnessAccount,
                    fitnessOptions
                )
            } else {
                Log.d(TAG, "Fitness permissions already granted")
                // Instead of directly calling onActivityResult, we'll notify via callback
                permissionResultListener?.onPermissionResult(GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, Activity.RESULT_OK, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting fitness permissions", e)
        }
    }

    /**
     * Interface for permission result callbacks
     */
    interface PermissionResultListener {
        fun onPermissionResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    private var permissionResultListener: PermissionResultListener? = null

    /**
     * Set a listener for permission results
     */
    fun setPermissionResultListener(listener: PermissionResultListener) {
        this.permissionResultListener = listener
    }

    /**
     * Get today's step count from fitness data
     */
    suspend fun getTodayStepCount(): Int {
        if (!hasPermissions(context)) {
            Log.e(TAG, "Fitness permissions not granted")
            return 0
        }

        try {
            var total = 0

            // Method 1: Try Daily Total API first (most reliable)
            try {
                Log.d(TAG, "Trying history client for step count")
                val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                val dataSet = Fitness.getHistoryClient(context, fitnessAccount)
                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                    .await()

                if (!dataSet.isEmpty) {
                    for (dp in dataSet.dataPoints) {
                        for (field in dp.dataType.fields) {
                            total += dp.getValue(field).asInt()
                        }
                    }
                    Log.d(TAG, "Steps from daily total: $total")
                } else {
                    Log.d(TAG, "No steps for today in daily total")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading daily step total", e)
            }

            // Method 2: If daily total fails, try aggregated data
            if (total <= 0) {
                try {
                    Log.d(TAG, "Trying aggregated data for step count")
                    val calendar = Calendar.getInstance()
                    val now = calendar.timeInMillis
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis

                    val readRequest = DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .setTimeRange(startTime, now, TimeUnit.MILLISECONDS)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .build()

                    val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                    val response = Fitness.getHistoryClient(context, fitnessAccount)
                        .readData(readRequest)
                        .await()

                    for (bucket in response.buckets) {
                        for (dataSet in bucket.dataSets) {
                            for (dp in dataSet.dataPoints) {
                                for (field in dp.dataType.fields) {
                                    total += dp.getValue(field).asInt()
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Steps from aggregated data: $total")
                } catch (e: Exception) {
                    Log.e(TAG, "Error with aggregated data for steps", e)
                }
            }

            return total
        } catch (e: Exception) {
            Log.e(TAG, "Error getting step count", e)
            return 0
        }
    }

    /**
     * Get today's distance walked in meters
     */
    suspend fun getTodayDistance(): Float {
        if (!hasPermissions(context)) {
            Log.e(TAG, "Fitness permissions not granted")
            return 0f
        }

        try {
            var totalDistance = 0f

            // Method 1: Try daily total first (most accurate)
            try {
                Log.d(TAG, "Trying history client for distance")
                val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                val dataSet = Fitness.getHistoryClient(context, fitnessAccount)
                    .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
                    .await()

                if (!dataSet.isEmpty) {
                    for (dp in dataSet.dataPoints) {
                        for (field in dp.dataType.fields) {
                            totalDistance += dp.getValue(field).asFloat()
                        }
                    }
                    Log.d(TAG, "Distance from daily total: $totalDistance")
                } else {
                    Log.d(TAG, "No distance for today in daily total")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading daily distance total", e)
            }

            // Method 2: If daily total fails, try aggregate data
            if (totalDistance <= 0f) {
                try {
                    Log.d(TAG, "Trying aggregated data for distance")
                    val calendar = Calendar.getInstance()
                    val now = calendar.timeInMillis
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis

                    val readRequest = DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                        .setTimeRange(startTime, now, TimeUnit.MILLISECONDS)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .build()

                    val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                    val response = Fitness.getHistoryClient(context, fitnessAccount)
                        .readData(readRequest)
                        .await()

                    for (bucket in response.buckets) {
                        for (dataSet in bucket.dataSets) {
                            for (dp in dataSet.dataPoints) {
                                for (field in dp.dataType.fields) {
                                    totalDistance += dp.getValue(field).asFloat()
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Distance from aggregated data: $totalDistance")
                } catch (e: Exception) {
                    Log.e(TAG, "Error with aggregated data for distance", e)
                }
            }

            // Method 3: If we still have no distance, derive from steps (approximate)
            if (totalDistance <= 0f) {
                try {
                    Log.d(TAG, "Deriving distance from steps")
                    val steps = getTodayStepCount()
                    if (steps > 0) {
                        // Average stride length is approximately 0.7 meters
                        totalDistance = steps * 0.7f
                        Log.d(TAG, "Distance derived from steps: $totalDistance")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deriving distance from steps", e)
                }
            }

            return totalDistance
        } catch (e: Exception) {
            Log.e(TAG, "Error getting distance", e)
            return 0f
        }
    }

    /**
     * Get today's calories burned from fitness data
     */
    suspend fun getTodayCaloriesBurned(): Float {
        if (!hasPermissions(context)) {
            Log.e(TAG, "Fitness permissions not granted")
            return 0f
        }

        try {
            var totalCalories = 0f

            // Method 1: Try daily total first (most accurate)
            try {
                Log.d(TAG, "Trying history client for calories")
                val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                val dataSet = Fitness.getHistoryClient(context, fitnessAccount)
                    .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
                    .await()

                if (!dataSet.isEmpty) {
                    for (dp in dataSet.dataPoints) {
                        for (field in dp.dataType.fields) {
                            totalCalories += dp.getValue(field).asFloat()
                        }
                    }
                    Log.d(TAG, "Calories from daily total: $totalCalories")
                } else {
                    Log.d(TAG, "No calories for today in daily total")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading daily calories total", e)
            }

            // Method 2: If daily total fails, try aggregate data
            if (totalCalories <= 0f) {
                try {
                    Log.d(TAG, "Trying aggregated data for calories")
                    val calendar = Calendar.getInstance()
                    val now = calendar.timeInMillis
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis

                    val readRequest = DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                        .setTimeRange(startTime, now, TimeUnit.MILLISECONDS)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .build()

                    val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                    val response = Fitness.getHistoryClient(context, fitnessAccount)
                        .readData(readRequest)
                        .await()

                    for (bucket in response.buckets) {
                        for (dataSet in bucket.dataSets) {
                            for (dp in dataSet.dataPoints) {
                                for (field in dp.dataType.fields) {
                                    totalCalories += dp.getValue(field).asFloat()
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Calories from aggregated data: $totalCalories")
                } catch (e: Exception) {
                    Log.e(TAG, "Error with aggregated data for calories", e)
                }
            }

            return totalCalories
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calories", e)
            return 0f
        }
    }

    /**
     * Get hourly calories burned for today (for chart display)
     */
    suspend fun getHourlyCaloriesBurned(): Map<Int, Float> {
        if (!hasPermissions(context)) {
            Log.e(TAG, "Fitness permissions not granted")
            return emptyMap()
        }

        try {
            val hourlyCalories = mutableMapOf<Int, Float>()

            try {
                val calendar = Calendar.getInstance()
                val now = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                val readRequest = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                    .setTimeRange(startTime, now, TimeUnit.MILLISECONDS)
                    .bucketByTime(1, TimeUnit.HOURS)
                    .build()

                val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                val response = Fitness.getHistoryClient(context, fitnessAccount)
                    .readData(readRequest)
                    .await()

                for (bucket in response.buckets) {
                    for (dataSet in bucket.dataSets) {
                        for (dp in dataSet.dataPoints) {
                            val startTimeMillis = dp.getStartTime(TimeUnit.MILLISECONDS)
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = startTimeMillis
                            val hour = cal.get(Calendar.HOUR_OF_DAY)

                            var calories = 0f
                            for (field in dp.dataType.fields) {
                                calories += dp.getValue(field).asFloat()
                            }

                            hourlyCalories[hour] = (hourlyCalories[hour] ?: 0f) + calories
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting hourly calories", e)
            }

            return hourlyCalories
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hourly calories", e)
            return emptyMap()
        }
    }

    /**
     * Get calories burned for a specific day
     * @param dayStartMillis The start time of the day in milliseconds
     */
    suspend fun getDailyCaloriesBurned(dayStartMillis: Long): Float {
        if (!hasPermissions(context)) {
            Log.e(TAG, "Fitness permissions not granted")
            return 0f
        }

        try {
            var totalCalories = 0f

            // Create calendar for start and end time
            val calStart = Calendar.getInstance()
            calStart.timeInMillis = dayStartMillis
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)

            val calEnd = Calendar.getInstance()
            calEnd.timeInMillis = dayStartMillis
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)

            val startTime = calStart.timeInMillis
            val endTime = calEnd.timeInMillis

            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val fitnessAccount = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            val response = Fitness.getHistoryClient(context, fitnessAccount)
                .readData(readRequest)
                .await()

            for (bucket in response.buckets) {
                for (dataSet in bucket.dataSets) {
                    for (dp in dataSet.dataPoints) {
                        for (field in dp.dataType.fields) {
                            totalCalories += dp.getValue(field).asFloat()
                        }
                    }
                }
            }

            Log.d(TAG, "Calories for day ${calStart.time}: $totalCalories")
            return totalCalories
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily calories", e)
            return 0f
        }
    }
}