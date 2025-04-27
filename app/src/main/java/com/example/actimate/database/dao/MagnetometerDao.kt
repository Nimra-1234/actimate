package com.example.actimate.database.dao

import com.example.actimate.database.MagnetometerProcessedSample
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object (DAO) for performing operations on the magnetometer_processed_sample table.
 */
@Dao
interface MagnetometerDao {

    /**
     * Inserts a new MagnetometerProcessedSample into the database.
     * @param sample The MagnetometerProcessedSample object to be inserted.
     */
    @Insert
    fun insertProcessedSample(sample: MagnetometerProcessedSample)

    /**
     * Retrieves all processed magnetometer samples from the database.
     * @return A list of all MagnetometerProcessedSample objects.
     */
    @Query("SELECT * FROM magnetometer_processed_sample")
    fun getAllProcessedSamples(): List<MagnetometerProcessedSample>

    /**
     * Deletes samples from the magnetometer_processed_sample table that were processed more than one day ago.
     * This query removes entries where the 'processedAt' timestamp is older than one day.
     */
    @Query("""
        DELETE FROM magnetometer_processed_sample
        WHERE processedAt <= datetime('now', '-1 day')
    """)
    fun deleteOldSamples()

    /**
     * Deletes all magnetometer samples from the database.
     * This function is meant to be used from an IDE for testing purposes.
     */
    @Query("DELETE FROM magnetometer_processed_sample")
    suspend fun deleteAllMagnetometerSamples()
}
