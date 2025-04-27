package com.example.actimate.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.actimate.database.ActivityPrediction

/**
 * DAO for accessing ActivityPrediction data
 */
@Dao
interface ActivityPredictionDao {

    /**
     * Insert a new activity prediction
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityPrediction(activityPrediction: ActivityPrediction)

    /**
     * Get all activity predictions ordered by processedAt timestamp
     */
    @Query("SELECT * FROM activity_prediction ORDER BY processedAt DESC")
    suspend fun getAllActivityPredictions(): List<ActivityPrediction>

    /**
     * Get the most recent activity prediction
     */
    @Query("SELECT * FROM activity_prediction ORDER BY processedAt DESC LIMIT 1")
    suspend fun getMostRecentPrediction(): ActivityPrediction?

    /**
     * Get activity predictions from a specific date
     * @param date The date in ISO format (YYYY-MM-DD)
     */
    @Query("SELECT * FROM activity_prediction WHERE processedAt LIKE :date || '%' ORDER BY processedAt DESC")
    suspend fun getPredictionsByDate(date: String): List<ActivityPrediction>

    /**
     * Delete all activity predictions
     */
    @Query("DELETE FROM activity_prediction")
    suspend fun deleteAllPredictions()
}