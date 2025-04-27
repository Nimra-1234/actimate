package com.example.actimate.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a processed accelerometer sample with statistical and spectral features.
 */
@Entity(tableName = "accelerometer_processed_sample")
data class AccelerometerProcessedSample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated ID for each sample
    val processedAt: String, // Timestamp of when the sample was processed
    val meanX: Float, // Mean value of X-axis readings
    val meanY: Float, // Mean value of Y-axis readings
    val meanZ: Float, // Mean value of Z-axis readings
    val standardDeviationX: Float, // Standard deviation of X-axis readings
    val standardDeviationY: Float, // Standard deviation of Y-axis readings
    val standardDeviationZ: Float, // Standard deviation of Z-axis readings
    val percentile25X: Float, // 25th percentile of X-axis
    val percentile50X: Float, // 50th percentile (median) of X-axis
    val percentile75X: Float, // 75th percentile of X-axis
    val percentile25Y: Float, // 25th percentile of Y-axis
    val percentile50Y: Float, // 50th percentile (median) of Y-axis
    val percentile75Y: Float, // 75th percentile of Y-axis
    val percentile25Z: Float, // 25th percentile of Z-axis
    val percentile50Z: Float, // 50th percentile (median) of Z-axis
    val percentile75Z: Float, // 75th percentile of Z-axis
    val thirdMoment: Float, // Third moment (skewness) of the data distribution
    val fourthMoment: Float, // Fourth moment (kurtosis) of the data distribution
    val entropyValue: Float, // Entropy value representing the signal randomness
    val spectralEntropy: Float, // Spectral entropy representing the frequency domain randomness
    val autocorrelation: Float, // Autocorrelation of the signal
    val correlationXY: Float? = null, // Correlation between X and Y axes, null allowed
    val correlationXZ: Float? = null, // Correlation between X and Z axes, null allowed
    val correlationYZ: Float? = null, // Correlation between Y and Z axes, null allowed
    val energyBand0_0_5Hz: Float, // Energy in 0-0.5 Hz frequency band
    val energyBand0_5_1Hz: Float, // Energy in 0.5-1 Hz frequency band
    val energyBand1_3Hz: Float, // Energy in 1-3 Hz frequency band
    val energyBand3_5Hz: Float, // Energy in 3-5 Hz frequency band
    val energyBand5HzPlus: Float // Energy in 5 Hz and above frequency band
)

/**
 * Represents a processed gyroscope sample with statistical and spectral features.
 */
@Entity(tableName = "gyroscope_processed_sample")
data class GyroscopeProcessedSample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated ID for each sample
    val processedAt: String, // Timestamp of when the sample was processed
    val meanX: Float, // Mean value of X-axis readings
    val meanY: Float, // Mean value of Y-axis readings
    val meanZ: Float, // Mean value of Z-axis readings
    val standardDeviationX: Float, // Standard deviation of X-axis readings
    val standardDeviationY: Float, // Standard deviation of Y-axis readings
    val standardDeviationZ: Float, // Standard deviation of Z-axis readings
    val percentile25X: Float, // 25th percentile of X-axis
    val percentile50X: Float, // 50th percentile (median) of X-axis
    val percentile75X: Float, // 75th percentile of X-axis
    val percentile25Y: Float, // 25th percentile of Y-axis
    val percentile50Y: Float, // 50th percentile (median) of Y-axis
    val percentile75Y: Float, // 75th percentile of Y-axis
    val percentile25Z: Float, // 25th percentile of Z-axis
    val percentile50Z: Float, // 50th percentile (median) of Z-axis
    val percentile75Z: Float, // 75th percentile of Z-axis
    val thirdMoment: Float, // Third moment (skewness) of the data distribution
    val fourthMoment: Float, // Fourth moment (kurtosis) of the data distribution
    val entropyValue: Float, // Entropy value representing the signal randomness
    val spectralEntropy: Float, // Spectral entropy representing the frequency domain randomness
    val autocorrelation: Float, // Autocorrelation of the signal
    val correlationXY: Float? = null, // Correlation between X and Y axes, null allowed
    val correlationXZ: Float? = null, // Correlation between X and Z axes, null allowed
    val correlationYZ: Float? = null, // Correlation between Y and Z axes, null allowed
    val energyBand0_0_5Hz: Float, // Energy in 0-0.5 Hz frequency band
    val energyBand0_5_1Hz: Float, // Energy in 0.5-1 Hz frequency band
    val energyBand1_3Hz: Float, // Energy in 1-3 Hz frequency band
    val energyBand3_5Hz: Float, // Energy in 3-5 Hz frequency band
    val energyBand5HzPlus: Float // Energy in 5 Hz and above frequency band
)

/**
 * Represents a processed magnetometer sample with statistical and spectral features.
 */
@Entity(tableName = "magnetometer_processed_sample")
data class MagnetometerProcessedSample(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated ID for each sample
    val processedAt: String, // Timestamp of when the sample was processed
    val meanX: Float, // Mean value of X-axis readings
    val meanY: Float, // Mean value of Y-axis readings
    val meanZ: Float, // Mean value of Z-axis readings
    val standardDeviationX: Float, // Standard deviation of X-axis readings
    val standardDeviationY: Float, // Standard deviation of Y-axis readings
    val standardDeviationZ: Float, // Standard deviation of Z-axis readings
    val percentile25X: Float, // 25th percentile of X-axis
    val percentile50X: Float, // 50th percentile (median) of X-axis
    val percentile75X: Float, // 75th percentile of X-axis
    val percentile25Y: Float, // 25th percentile of Y-axis
    val percentile50Y: Float, // 50th percentile (median) of Y-axis
    val percentile75Y: Float, // 75th percentile of Y-axis
    val percentile25Z: Float, // 25th percentile of Z-axis
    val percentile50Z: Float, // 50th percentile (median) of Z-axis
    val percentile75Z: Float, // 75th percentile of Z-axis
    val thirdMoment: Float, // Third moment (skewness) of the data distribution
    val fourthMoment: Float, // Fourth moment (kurtosis) of the data distribution
    val entropyValue: Float, // Entropy value representing the signal randomness
    val spectralEntropy: Float, // Spectral entropy representing the frequency domain randomness
    val autocorrelation: Float, // Autocorrelation of the signal
    val correlationXY: Float? = null, // Correlation between X and Y axes, null allowed
    val correlationXZ: Float? = null, // Correlation between X and Z axes, null allowed
    val correlationYZ: Float? = null, // Correlation between Y and Z axes, null allowed
    val energyBand0_0_5Hz: Float, // Energy in 0-0.5 Hz frequency band
    val energyBand0_5_1Hz: Float, // Energy in 0.5-1 Hz frequency band
    val energyBand1_3Hz: Float, // Energy in 1-3 Hz frequency band
    val energyBand3_5Hz: Float, // Energy in 3-5 Hz frequency band
    val energyBand5HzPlus: Float // Energy in 5 Hz and above frequency band
)

/**
 * Represents a model containing accelerometer, gyroscope, and magnetometer data, along with a label.
 */
@Entity(tableName = "input_model")
data class InputModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated ID for each sample
    val processedAt: String, // Timestamp of when the model was processed
    var accX: Float? = Float.NaN, // Accelerometer X-axis value (NaN as default)
    var accY: Float? = Float.NaN, // Accelerometer Y-axis value (NaN as default)
    var accZ: Float? = Float.NaN, // Accelerometer Z-axis value (NaN as default)
    var gyroX: Float? = Float.NaN, // Gyroscope X-axis value (NaN as default)
    var gyroY: Float? = Float.NaN, // Gyroscope Y-axis value (NaN as default)
    var gyroZ: Float? = Float.NaN, // Gyroscope Z-axis value (NaN as default)
    var magnX: Float? = Float.NaN, // Magnetometer X-axis value (NaN as default)
    var magnY: Float? = Float.NaN, // Magnetometer Y-axis value (NaN as default)
    var magnZ: Float? = Float.NaN, // Magnetometer Z-axis value (NaN as default)
    var label: String? = null // Label for activity classification
)

/**
 * Represents an activity prediction model with a predicted label.
 */
@Entity(tableName = "activity_prediction")
data class ActivityPrediction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generated ID for each prediction
    val processedAt: String, // Timestamp of when the prediction was made
    var label: String? = "standing" // Predicted activity label (default to "standing")
)
