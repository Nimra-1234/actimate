package com.example.actimate.util

import ai.onnxruntime.OnnxJavaType
import android.content.Context
import android.util.Log
import com.example.actimate.viewmodel.LastPredictionViewModel
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.example.actimate.database.ActivityPrediction
import com.example.actimate.database.AppDatabaseProvider
import com.example.actimate.database.dao.ActivityPredictionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "SensorDataManager"
private const val SAMPLE_INTERVAL_MS = 500 // Only process data every 3 seconds

object SensorDataManager {
    // ViewModel for updating UI
    var lastPredictionViewModel: LastPredictionViewModel? = null

    // Data buffers for sensor readings
    private val accelerometerData = mutableListOf<List<Float>>()
    private val gyroscopeData = mutableListOf<List<Float>>()
    private val magnetometerData = mutableListOf<List<Float>>()

    // LSTM model expects 50 timesteps
    private const val REQUIRED_DATA_POINTS = 50

    // For activity confidence
    private val recentPredictions = mutableListOf<String>()
    private const val CONFIDENCE_THRESHOLD = 3 // Need 3 same predictions to be confident

    // Timestamp of last data processing
    private var lastProcessTimeMs = 0L

    // Flag to prevent multiple simultaneous predictions
    private var isProcessing = false

    /**
     * Updates accelerometer data buffer with new measurements
     */
    fun updateAccelerometerData(accelerometerMeasurement: List<Float>, context: Context) {
        if (accelerometerData.size >= REQUIRED_DATA_POINTS) {
            accelerometerData.removeAt(0)
        }
        accelerometerData.add(accelerometerMeasurement)

        checkAndProcessData(context)
    }

    /**
     * Updates gyroscope data buffer with new measurements
     */
    fun updateGyroscopeData(gyroscopeMeasurement: List<Float>, context: Context) {
        if (gyroscopeData.size >= REQUIRED_DATA_POINTS) {
            gyroscopeData.removeAt(0)
        }
        gyroscopeData.add(gyroscopeMeasurement)

        checkAndProcessData(context)
    }

    /**
     * Updates magnetometer data buffer with new measurements
     */
    fun updateMagnetometerData(magnetometerMeasurement: List<Float>, context: Context) {
        if (magnetometerData.size >= REQUIRED_DATA_POINTS) {
            magnetometerData.removeAt(0)
        }
        magnetometerData.add(magnetometerMeasurement)

        checkAndProcessData(context)
    }

    /**
     * Checks if we have enough data for prediction and processes it
     */
    private fun checkAndProcessData(context: Context) {
        // Prevent multiple simultaneous predictions
        if (isProcessing) {
            return
        }

        // Only process data at specified intervals
        val currentTimeMs = System.currentTimeMillis()
        if (currentTimeMs - lastProcessTimeMs < SAMPLE_INTERVAL_MS) {
            return
        }

        // Check if we have enough data points from all sensors
        val hasEnoughData = accelerometerData.size >= REQUIRED_DATA_POINTS &&
                gyroscopeData.size >= REQUIRED_DATA_POINTS &&
                magnetometerData.size >= REQUIRED_DATA_POINTS

        if (hasEnoughData) {
            isProcessing = true
            lastProcessTimeMs = currentTimeMs

            // Run model inference in background
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    // Create the input sequence for the model
                    val sequenceData = createSequenceData()

                    // Run the model to get activity prediction
                    val prediction = runModelOnDevice(sequenceData, context)

                    // Add to recent predictions for confidence check
                    recentPredictions.add(prediction)
                    if (recentPredictions.size > CONFIDENCE_THRESHOLD) {
                        recentPredictions.removeAt(0)
                    }

                    // Only update UI if we're confident in the activity
                    val confidentActivity = getConfidentActivity()
                    if (confidentActivity != null) {
                        // Update UI with the prediction
                        withContext(Dispatchers.Main) {
                            lastPredictionViewModel?.updateLastPredictionData(confidentActivity)
                        }

                        // Insert prediction into database
                        val db = AppDatabaseProvider.getInstance(context)
                        val dao = db.activityPredictionDao()
                        insertActivityPredictionToDB(dao, confidentActivity)

                        Log.d(TAG, "Processed data and confidently predicted: $confidentActivity")
                    } else {
                        Log.d(TAG, "Processed data but not confident enough yet. Current: $prediction")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing sensor data", e)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    /**
     * Creates the 3D tensor input required by the LSTM model
     * Format: [1, sequence_length, features] where features = 9 (3 values from each sensor)
     */
    private fun createSequenceData(): Array<Array<FloatArray>> {
        // Ensure we have the exact number of data points needed
        val accData = accelerometerData.takeLast(REQUIRED_DATA_POINTS)
        val magData = magnetometerData.takeLast(REQUIRED_DATA_POINTS)
        val gyroData = gyroscopeData.takeLast(REQUIRED_DATA_POINTS)

        // Create sequence tensor with shape [1, 50, 9]
        val sequence = Array(REQUIRED_DATA_POINTS) { i ->
            FloatArray(9).apply {
                // Copy accelerometer data (x, y, z)
                this[0] = accData[i][0]
                this[1] = accData[i][1]
                this[2] = accData[i][2]

                // Copy magnetometer data (x, y, z)
                this[3] = magData[i][0]
                this[4] = magData[i][1]
                this[5] = magData[i][2]

                // Copy gyroscope data (x, y, z)
                this[6] = gyroData[i][0]
                this[7] = gyroData[i][1]
                this[8] = gyroData[i][2]
            }
        }

        // Add batch dimension [1, 50, 9] for ONNX Runtime
        return arrayOf(sequence)
    }

    /**
     * Runs the ONNX model to predict activity from sensor data
     */
    private fun runModelOnDevice(sequenceData: Array<Array<FloatArray>>, context: Context): String {
        var predictedValue: Long? = null
        var env: OrtEnvironment? = null
        var session: OrtSession? = null
        var tensor: OnnxTensor? = null

        try {
            // Create a new ONNX session for each prediction
            env = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("model.onnx").readBytes()
            session = env.createSession(modelBytes)

            // Get the actual input name from the model
            val inputName = session.inputNames.iterator().next()

            // Create input tensor from sequence data
            tensor = OnnxTensor.createTensor(env, sequenceData)

            // Create input map with the correct input name from ONNX model
            val inputs = mapOf(inputName to tensor)

            // Run model inference
            val result = session.run(inputs)

            // Get the first output
            val outputName = session.outputNames.iterator().next()
            val outputOptional = result[outputName]

            if (outputOptional.isPresent) {
                val outputValue = outputOptional.get()

                if (outputValue is OnnxTensor) {
                    if (outputValue.info.type == OnnxJavaType.INT64) {
                        // For classification output (assuming INT64 class index)
                        val outputIntValue = outputValue.value as LongArray
                        predictedValue = outputIntValue[0]
                    } else if (outputValue.info.type == OnnxJavaType.FLOAT) {
                        // For models that output class probabilities
                        val outputFloatValue = outputValue.value as Array<FloatArray>
                        val maxIndex = outputFloatValue[0].withIndex().maxByOrNull { it.value }?.index
                        predictedValue = maxIndex?.toLong()
                    }

                    // Always close the output tensor
                    outputValue.close()
                }

                // Convert prediction to activity label
                return convertLabelToActivity(predictedValue)
            } else {
                Log.e(TAG, "Model output is not present")
                return "unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during model inference", e)
            return "Error: ${e.message}"
        } finally {
            // Close resources
            tensor?.close()
            session?.close()
            env?.close()
        }
    }

    /**
     * Get the confident activity by requiring multiple consecutive same predictions
     */
    private fun getConfidentActivity(): String? {
        if (recentPredictions.size < CONFIDENCE_THRESHOLD) {
            return null
        }

        // Check if we have CONFIDENCE_THRESHOLD consecutive same predictions
        val lastPrediction = recentPredictions.last()
        val confidentCount = recentPredictions.count { it == lastPrediction }

        return if (confidentCount >= CONFIDENCE_THRESHOLD) {
            lastPrediction
        } else {
            null
        }
    }

    /**
     * Converts numeric prediction to activity label
     */
    private fun convertLabelToActivity(prediction: Long?): String {
        return when (prediction) {
            0L -> "downstairs"
            1L -> "running"
            2L -> "standing"
            3L -> "upstairs"
            4L -> "walking"
            else -> "unknown"
        }
    }

    /**
     * Inserts activity prediction into database
     */
    suspend fun insertActivityPredictionToDB(dao: ActivityPredictionDao, label: String) {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val activityPrediction = ActivityPrediction(
            processedAt = currentDateTime,
            label = label
        )

        dao.insertActivityPrediction(activityPrediction)
    }
}