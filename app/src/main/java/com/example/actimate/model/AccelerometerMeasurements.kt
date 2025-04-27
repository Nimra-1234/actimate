package com.example.actimate.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Data model representing an individual accelerometer sample with x, y, z values and a timestamp.
 */
class AccelerometerSample() : Parcelable {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var timestamp: Long = 0L // Timestamp in milliseconds since epoch

    private constructor(parcel: Parcel) : this() {
        x = parcel.readFloat()
        y = parcel.readFloat()
        z = parcel.readFloat()
        timestamp = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(x)
        parcel.writeFloat(y)
        parcel.writeFloat(z)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AccelerometerSample> {
        override fun createFromParcel(parcel: Parcel): AccelerometerSample {
            return AccelerometerSample(parcel)
        }

        override fun newArray(size: Int): Array<AccelerometerSample?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Data model representing a collection of accelerometer measurements.
 */
class AccelerometerMeasurements private constructor(parcel: Parcel) : Parcelable {
    private val samples = mutableListOf<AccelerometerSample>()
    private var maxSize = 500

    init {
        maxSize = parcel.readInt()
        parcel.readList(samples, AccelerometerSample::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(maxSize)
        parcel.writeList(samples)
    }

    override fun describeContents(): Int = 0

      // parcelable allow it to be passed between Android components
    companion object CREATOR : Parcelable.Creator<AccelerometerMeasurements> {
        override fun createFromParcel(parcel: Parcel): AccelerometerMeasurements {
            return AccelerometerMeasurements(parcel)
        }

        override fun newArray(size: Int): Array<AccelerometerMeasurements?> {
            return arrayOfNulls(size)
        }
    }
}
