package com.emotiontracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="events")
data class Event(
    @PrimaryKey(autoGenerate=true) val id:Long=0,
    val ts:Long,
    val packageName:String,
    val type:String,
    val value:Int=0
)

@Entity(tableName="mood_checkins")
data class MoodCheckIn(
    @PrimaryKey(autoGenerate=true) val id:Long=0,
    val ts:Long,
    val mood:Int,
    val stress:Int,
    val energy:Int,
    val note:String=""
)

@Entity(tableName="app_limits")
data class AppLimit(
    @PrimaryKey val packageName:String,
    val appName:String,
    val dailyScrollLimit:Int=200,
    val enabled:Boolean=true
)
