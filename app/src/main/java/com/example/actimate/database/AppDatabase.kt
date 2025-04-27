package com.example.actimate.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.actimate.database.dao.AccelerometerDao
import com.example.actimate.database.dao.ActivityPredictionDao
import com.example.actimate.database.dao.GyroscopeDao
import com.example.actimate.database.dao.MagnetometerDao
import com.example.actimate.database.dao.InputModelDao

/**
 * Defines the Room database for the application.
 * This class provides access to the app's DAOs (Data Access Objects), which handle the interaction
 * with the database for different entities.
 */
@Database(
    entities = [AccelerometerProcessedSample::class, GyroscopeProcessedSample::class, MagnetometerProcessedSample::class, InputModel::class, ActivityPrediction::class],
    version = 11 // Increment the version when schema changes occur
)
abstract class AppDatabase : RoomDatabase() {

    // Accessors for DAOs for each entity in the database
    abstract fun accelerometerDao(): AccelerometerDao
    abstract fun gyroscopeDao(): GyroscopeDao
    abstract fun magnetometerDao(): MagnetometerDao
    abstract fun inputModelDao(): InputModelDao
    abstract fun activityPredictionDao(): ActivityPredictionDao
}

/**
 * Singleton provider for the AppDatabase instance.
 * Ensures that only one instance of the database is created and reused across the app.
 */
object AppDatabaseProvider {

    // Volatile annotation ensures visibility of changes to INSTANCE across threads.
    @Volatile
    private var INSTANCE: AppDatabase? = null

    /**
     * Returns a singleton instance of the AppDatabase.
     * If the instance doesn't exist, it will be created.
     *
     * @param context The application context to initialize the database.
     * @return The singleton instance of the AppDatabase.
     */
    fun getInstance(context: Context): AppDatabase {
        // If the database instance is null, synchronize the code block to create a new instance.
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext, // Use the application context to avoid memory leaks.
                AppDatabase::class.java,    // Define the database class.
                "app_database"
            )
                // If schema changes occur, the fallback option will recreate the database.
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}
