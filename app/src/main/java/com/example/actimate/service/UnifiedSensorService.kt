package com.example.actimate.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.actimate.util.SensorDataManager
import com.example.actimate.util.NotificationHelper

class UnifiedSensorService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationHelper: NotificationHelper

    // Sensor references
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    // BroadcastReceiver to handle mode changes
    private val samplingRateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.actimate.UPDATE_SAMPLING_RATE") {
                val workingMode = intent.getStringExtra("workingMode") ?: "maxaccuracy"
                Log.d("SensorService", "Received mode change: $workingMode")
                updateSamplingRate(workingMode)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        notificationHelper = NotificationHelper(this) // Initialize NotificationHelper

        // Register the BroadcastReceiver with the required flag
        val filter = IntentFilter("com.example.actimate.UPDATE_SAMPLING_RATE")
        ContextCompat.registerReceiver(
            this,
            samplingRateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        initializeSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Use NotificationHelper to create and start the foreground notification
        val notification = notificationHelper.createServiceNotification("Unified Sensor Service")
        startForeground(1, notification)

        // Return START_STICKY to ensure the service is restarted if it's killed by the system
        return START_STICKY
    }

    /**
     * Initializes the sensors and registers the listener with the default working mode.
     */
    private fun initializeSensors() {
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val workingMode = sharedPreferences.getString("workingmode", "maxaccuracy") ?: "maxaccuracy"
        updateSamplingRate(workingMode)
    }

    /**
     * Updates the sensor sampling rate based on the working mode.
     */
    private fun updateSamplingRate(workingMode: String) {
        stopSensors() // Unregister the previous listeners

        val samplingDelay = if (workingMode == "maxbatterysaving") {
            SensorManager.SENSOR_DELAY_NORMAL // Normal delay for battery saving mode
        } else {
            SensorManager.SENSOR_DELAY_UI // Default UI delay
        }

        Log.d("SensorService", "Updating sampling delay to: $samplingDelay")

        registerSensors(samplingDelay)
    }

    /**
     * Registers sensors with the specified sampling delay.
     */
    private fun registerSensors(samplingDelay: Int) {
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, samplingDelay)
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, samplingDelay)
        }
        magnetometerSensor?.let {
            sensorManager.registerListener(this, it, samplingDelay)
        }
    }

    /**
     * Unregisters the sensor listener and stops all sensor activity.
     */
    private fun stopSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            Log.d("SensorData", "Sensor type: ${it.sensor.type}, Values: ${it.values.joinToString()}")
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometerData(it)
                Sensor.TYPE_GYROSCOPE -> handleGyroscopeData(it)
                Sensor.TYPE_MAGNETIC_FIELD -> handleMagnetometerData(it)
            }
        }
    }

    private fun handleAccelerometerData(event: SensorEvent) {
        val sample = listOf(event.values[0], event.values[1], event.values[2])
        SensorDataManager.updateAccelerometerData(sample, applicationContext)
    }

    private fun handleGyroscopeData(event: SensorEvent) {
        val sample = listOf(event.values[0], event.values[1], event.values[2])
        SensorDataManager.updateGyroscopeData(sample, applicationContext)
    }

    private fun handleMagnetometerData(event: SensorEvent) {
        val sample = listOf(event.values[0], event.values[1], event.values[2])
        SensorDataManager.updateMagnetometerData(sample, applicationContext)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if needed
    }

    override fun onDestroy() {
        // Clean up resources when the service is stopped
        unregisterReceiver(samplingRateReceiver)
        stopSensors()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null since this is not a bound service
        return null
    }
}
