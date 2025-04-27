package com.example.actimate.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Data model representing an individual gyroscope sample with x, y, z values.
 */
class GyroscopeSample() : Parcelable {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f

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

    companion object CREATOR : Parcelable.Creator<GyroscopeSample> {
        override fun createFromParcel(parcel: Parcel): GyroscopeSample {
            return GyroscopeSample(parcel)
        }

        override fun newArray(size: Int): Array<GyroscopeSample?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Data model representing a collection of gyroscope samples.
 */
class GyroscopeMeasurements private constructor(parcel: Parcel) : Parcelable {
    private val samples = mutableListOf<GyroscopeSample>()
    private var maxSize = 500

    init {
        maxSize = parcel.readInt()
        parcel.readList(samples, GyroscopeSample::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(maxSize)
        parcel.writeList(samples)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GyroscopeMeasurements> {
        override fun createFromParcel(parcel: Parcel): GyroscopeMeasurements {
            return GyroscopeMeasurements(parcel)
        }

        override fun newArray(size: Int): Array<GyroscopeMeasurements?> {
            return arrayOfNulls(size)
        }
    }
}
