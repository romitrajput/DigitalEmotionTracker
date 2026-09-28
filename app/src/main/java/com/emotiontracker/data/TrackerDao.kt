package com.emotiontracker.data

import androidx.room.*

@Dao
interface TrackerDao {
    @Insert suspend fun insertEvent(e:Event)
    @Insert suspend fun insertMood(m:MoodCheckIn)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsertLimit(l:AppLimit)

    @Query("SELECT * FROM events WHERE ts>=:start AND ts<:end ORDER BY ts DESC")
    suspend fun events(start:Long,end:Long):List<Event>

    @Query("SELECT * FROM events WHERE ts>=:start AND ts<:end ORDER BY ts ASC")
    suspend fun eventsAsc(start:Long,end:Long):List<Event>

    @Query("SELECT * FROM mood_checkins WHERE ts>=:start AND ts<:end ORDER BY ts ASC")
    suspend fun moodsAsc(start:Long,end:Long):List<MoodCheckIn>

    @Query("SELECT * FROM mood_checkins WHERE ts>=:start AND ts<:end ORDER BY ts DESC")
    suspend fun moods(start:Long,end:Long):List<MoodCheckIn>

    @Query("SELECT * FROM mood_checkins ORDER BY ts ASC")
    suspend fun allMoods():List<MoodCheckIn>

    @Query("SELECT * FROM events ORDER BY ts ASC")
    suspend fun allEvents():List<Event>

    @Query("SELECT * FROM app_limits")
    suspend fun limits():List<AppLimit>

    @Query("DELETE FROM events WHERE ts<:before")
    suspend fun purgeEvents(before:Long)

    @Query("DELETE FROM mood_checkins WHERE ts<:before")
    suspend fun purgeMoods(before:Long)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM mood_checkins")
    suspend fun deleteAllMoods()

    @Query("DELETE FROM app_limits")
    suspend fun deleteAllLimits()
}
