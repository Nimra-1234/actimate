package com.example.actimate.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.actimate.database.GyroscopeProcessedSample

/**
 * Data Access Object (DAO) for performing operations on the gyroscope_processed_sample table.
 */
@Dao
interface GyroscopeDao {

    /**
     * Inserts a processed gyroscope sample into the database.
     * @param sample The processed gyroscope sample to be inserted.
     */
    @Insert
    fun insertProcessedSample(sample: GyroscopeProcessedSample)

    /**
     * Retrieves all processed gyroscope samples from the database.
     * @return A list of all processed gyroscope samples.
     */
    @Query("SELECT * FROM gyroscope_processed_sample")
    fun getAllProcessedSamples(): List<GyroscopeProcessedSample>

    /**
     * Deletes gyroscope samples older than one day from the database.
     * This function removes records where the 'processedAt' timestamp is more than 1 day ago.
     */
    @Query("""
        DELETE FROM gyroscope_processed_sample
        WHERE processedAt <= datetime('now', '-1 day')
    """)
    fun deleteOldSamples()

    /**
     * Deletes all gyroscope samples from the database.
     * Use this function to clear all records in the gyroscope_processed_sample table.
     */
    @Query("DELETE FROM gyroscope_processed_sample")
    suspend fun deleteAllGyroscopeSamples()
}
