package com.emotiontracker.data

import android.content.Context
import androidx.room.*

@Database(
    entities=[Event::class,MoodCheckIn::class,AppLimit::class],
    version=1,
    exportSchema=false
)
abstract class TrackerDatabase:RoomDatabase() {
    abstract fun dao():TrackerDao

    companion object {
        @Volatile private var instance:TrackerDatabase?=null
        fun get(c:Context):TrackerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    c.applicationContext,
                    TrackerDatabase::class.java,
                    "emotion_tracker.db"
                ).build().also { instance=it }
            }
    }
}
