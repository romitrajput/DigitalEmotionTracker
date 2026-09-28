package com.emotiontracker.monitoring

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.emotiontracker.data.*
import java.util.Calendar

class ActivityRepository(private val context: Context) {
    private val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val dao = TrackerDatabase.get(context).dao()

    private fun startOfDay(dayOffset: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun dayStart() = startOfDay(0)
    private fun dayEnd() = dayStart() + 86_400_000L

    fun usageForRange(start: Long, end: Long): List<UsageStats> =
        usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

    fun todayUsage(): List<UsageStats> = usageForRange(dayStart(), System.currentTimeMillis())

    suspend fun eventsToday() = dao.events(dayStart(), dayEnd())
    suspend fun moodsToday() = dao.moods(dayStart(), dayEnd())
    suspend fun allEvents() = dao.allEvents()
    suspend fun allMoods() = dao.allMoods()
    suspend fun limits() = dao.limits()
    suspend fun saveMood(m: MoodCheckIn) = dao.insertMood(m)
    suspend fun saveLimit(l: AppLimit) = dao.upsertLimit(l)

    suspend fun scrollCount(pkg: String) =
        dao.events(dayStart(), dayEnd()).count { it.packageName == pkg && it.type == "SCROLL" }

    suspend fun clearAllLocalData() {
        dao.deleteAllEvents()
        dao.deleteAllMoods()
        dao.deleteAllLimits()
    }

    /** One day's worth of events, for [daysAgo] days before today (0 = today). */
    suspend fun eventsForDay(daysAgo: Int): List<Event> =
        dao.eventsAsc(startOfDay(-daysAgo), startOfDay(-daysAgo + 1))

    /** One day's tracked screen-time minutes, for [daysAgo] days before today (0 = today). */
    fun screenMinutesForDay(daysAgo: Int): Int {
        val start = startOfDay(-daysAgo)
        val end = if (daysAgo == 0) System.currentTimeMillis() else startOfDay(-daysAgo + 1)
        return (usageForRange(start, end).sumOf { it.totalTimeInForeground } / 60000L).toInt()
    }

    /** Events across the last [days] days (today inclusive), oldest first. */
    suspend fun eventsForLastDays(days: Int): List<Event> =
        dao.eventsAsc(startOfDay(-(days - 1)), dayEnd())

    /** Mood check-ins across the last [days] days (today inclusive), oldest first. */
    suspend fun moodsForLastDays(days: Int): List<MoodCheckIn> =
        dao.moodsAsc(startOfDay(-(days - 1)), dayEnd())

    /** Midnight timestamp for [daysAgo] days before today — used to bucket history by day. */
    fun dayBucketStart(daysAgo: Int): Long = startOfDay(-daysAgo)
}
