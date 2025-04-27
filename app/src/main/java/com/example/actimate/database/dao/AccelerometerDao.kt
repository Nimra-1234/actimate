package com.example.actimate.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.actimate.database.AccelerometerProcessedSample

/**
 * Data Access Object (DAO) for performing operations on the accelerometer_processed_sample table.
 */
@Dao
interface AccelerometerDao {

    /**
     * Inserts a processed accelerometer sample into the database.
     * @param sample The accelerometer processed sample to be inserted.
     */
    @Insert
    fun insertProcessedSample(sample: AccelerometerProcessedSample)

    /**
     * Retrieves all processed accelerometer samples from the database.
     * @return A list of all accelerometer processed samples.
     */
    @Query("SELECT * FROM accelerometer_processed_sample")
    fun getAllProcessedSamples(): List<AccelerometerProcessedSample>

    /**
     * Deletes all accelerometer processed samples that are older than 1 day.
     * The function uses the current time and deletes any entries processed more than a day ago.
     */
    @Query("""
        DELETE FROM accelerometer_processed_sample
        WHERE processedAt <= datetime('now', '-1 day')
    """)
    fun deleteOldSamples()

    /**
     * Deletes all accelerometer processed samples from the database.
     * This function should be used with caution, as it removes all data.
     */
    @Query("DELETE FROM accelerometer_processed_sample")
    fun deleteAllAccelerometerSamples()
}
