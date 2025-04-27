package com.example.actimate.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.actimate.database.InputModel

/**
 * Data Access Object (DAO) for performing operations on the input_model table.
 */
@Dao
interface InputModelDao {

    /**
     * Inserts a new InputModel into the database.
     * @param input The InputModel object to be inserted.
     */
    @Insert
    suspend fun insertInputModel(input: InputModel)

    /**
     * Deletes the oldest InputModel record from the database.
     * This query deletes the record with the smallest 'id' value, effectively removing the oldest entry.
     */
    @Query("DELETE FROM input_model WHERE id = (SELECT id FROM input_model ORDER BY id ASC LIMIT 1)")
    suspend fun deleteOldestInputModel()

    /**
     * Retrieves the most recent InputModel from the database.
     * This query fetches the latest record based on the 'id' field in descending order.
     * @return The last InputModel, or null if no records exist.
     */
    @Query("SELECT * FROM input_model ORDER BY id DESC LIMIT 1")
    suspend fun getLastInputModel(): InputModel?

    /**
     * Returns the total number of rows in the input_model table.
     * @return The count of records in the table.
     */
    @Query("SELECT COUNT(*) FROM input_model")
    suspend fun getRowCount(): Int

    /**
     * Updates an existing InputModel in the database.
     * @param input The InputModel object with updated values.
     */
    @Update
    suspend fun updateInputModel(input: InputModel)
}
