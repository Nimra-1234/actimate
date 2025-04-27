package com.example.actimate.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Data model representing an individual magnetometer sample with x, y, z values.
 */
class MagnetometerSample() : Parcelable {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f

    fun getValues(): FloatArray {
        return floatArrayOf(x, y, z)
    }

    private constructor(parcel: Parcel) : this() {
        x = parcel.readFloat()
        y = parcel.readFloat()
        z = parcel.readFloat()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(x)
        parcel.writeFloat(y)
        parcel.writeFloat(z)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MagnetometerSample> {
        override fun createFromParcel(parcel: Parcel): MagnetometerSample {
            return MagnetometerSample(parcel)
        }

        override fun newArray(size: Int): Array<MagnetometerSample?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Data model representing a collection of magnetometer samples.
 */
class MagnetometerMeasurements private constructor(parcel: Parcel) : Parcelable {
    private val samples = mutableListOf<MagnetometerSample>()
    private var maxSize = 500

    init {
        maxSize = parcel.readInt()
        parcel.readList(samples, MagnetometerSample::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(maxSize)
        parcel.writeList(samples)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MagnetometerMeasurements> {
        override fun createFromParcel(parcel: Parcel): MagnetometerMeasurements {
            return MagnetometerMeasurements(parcel)
        }

        override fun newArray(size: Int): Array<MagnetometerMeasurements?> {
            return arrayOfNulls(size)
        }
    }
}
