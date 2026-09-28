package com.emotiontracker.scoring

import com.emotiontracker.data.MoodCheckIn
import com.emotiontracker.monitoring.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val BASELINE_WINDOW_DAYS = 14

data class DailyPoint(val daysAgo: Int, val raw: RawCounts)

/**
 * Single place that assembles today's raw counts + trailing baseline history, used by
 * Dashboard, Insights and the trend charts so they can never silently disagree about what
 * "today" or "baseline" means.
 */
object ScoreLoader {
    suspend fun loadToday(r: ActivityRepository): Pair<RawCounts, List<DailyPoint>> = withContext(Dispatchers.IO) {
        val todayEvents = r.eventsToday()
        val todayMinutes = (r.todayUsage().sumOf { it.totalTimeInForeground } / 60000L).toInt()
        val todayRaw = WellbeingEngine.rawCounts(todayEvents, todayMinutes)

        val history = (1..BASELINE_WINDOW_DAYS).map { daysAgo ->
            val events = r.eventsForDay(daysAgo)
            val minutes = r.screenMinutesForDay(daysAgo)
            DailyPoint(daysAgo, WellbeingEngine.rawCounts(events, minutes))
        }.filter { it.raw.totalEvents > 0 || it.raw.screenMinutes > 0 }

        todayRaw to history
    }

    suspend fun loadScore(r: ActivityRepository): Score = withContext(Dispatchers.IO) {
        val (todayRaw, history) = loadToday(r)
        val moods = r.moodsToday()
        val allMoods = r.allMoods()
        WellbeingEngine.score(todayRaw, history.map { it.raw }, moods, allMoods.takeLast(30))
    }

    suspend fun loadScoreAndMoods(r: ActivityRepository): Triple<Score, List<MoodCheckIn>, Double?> = withContext(Dispatchers.IO) {
        val (todayRaw, history) = loadToday(r)
        val moods = r.moodsToday()
        val allMoods = r.allMoods()
        val score = WellbeingEngine.score(todayRaw, history.map { it.raw }, moods, allMoods.takeLast(30))
        Triple(score, moods, WellbeingEngine.weeklyAverage(allMoods.takeLast(7)))
    }
}
