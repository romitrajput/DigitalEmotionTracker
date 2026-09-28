package com.emotiontracker.scoring

import com.emotiontracker.data.Event
import com.emotiontracker.data.MoodCheckIn
import java.util.Calendar

/** Raw, unweighted counts for one day — shown to the user before any derived score,
 *  so they can sanity-check the math themselves rather than trusting an opaque number. */
data class RawCounts(
    val scrollEvents: Int,
    val appOpens: Int,
    val clicks: Int,
    val textChanges: Int,
    val lateNightEvents: Int,
    val screenMinutes: Int,
    val totalEvents: Int
)

/** A single day's behavioral signal, expressed as this-day-vs-your-own-recent-history
 *  rather than an arbitrary fixed threshold. `null` when there isn't enough history yet
 *  to compare against — shown as "not enough data" rather than a fabricated number. */
data class BaselineComparison(
    val todayValue: Double,
    val baselineAverage: Double?,
    /** Percent difference from baseline; positive = higher than usual. Null if no baseline. */
    val percentVsBaseline: Int?
)

data class Score(
    val index: Int?,
    val stimulation: BaselineComparison,
    val compulsion: BaselineComparison,
    val lateNight: BaselineComparison,
    val focus: BaselineComparison,
    val confidence: Int,
    val raw: RawCounts,
    val hasEnoughDataForIndex: Boolean
)

data class Insight(val title: String, val body: String)

object WellbeingEngine {

    private fun isLateNight(ts: Long): Boolean {
        val h = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
        return h >= 23 || h < 5
    }

    fun rawCounts(events: List<Event>, screenMinutes: Int): RawCounts = RawCounts(
        scrollEvents = events.count { it.type == "SCROLL" },
        appOpens = events.count { it.type == "APP_OPEN" },
        clicks = events.count { it.type == "CLICK" },
        textChanges = events.count { it.type == "TEXT_CHANGED" },
        lateNightEvents = events.count { isLateNight(it.ts) },
        screenMinutes = screenMinutes,
        totalEvents = events.size
    )

    /**
     * Compares today's raw counts against the user's own trailing average from
     * [historicalDays] (each day's events + screen minutes, oldest first, NOT including today).
     * This replaces fixed divisors (e.g. "scrolls / 8") with a real personal baseline: the
     * question each metric answers is "is today unusual for *this person*", not "does today
     * cross some population-wide number nobody validated."
     */
    fun score(
        todayRaw: RawCounts,
        historicalDays: List<RawCounts>,
        moods: List<MoodCheckIn>,
        baselineMoods: List<MoodCheckIn>
    ): Score {
        fun baseline(selector: (RawCounts) -> Int): BaselineComparison {
            val today = selector(todayRaw).toDouble()
            if (historicalDays.isEmpty()) return BaselineComparison(today, null, null)
            val avg = historicalDays.map { selector(it).toDouble() }.average()
            val pct = if (avg > 0.01) (((today - avg) / avg) * 100).toInt() else null
            return BaselineComparison(today, avg, pct)
        }

        val stimulation = baseline { it.scrollEvents }
        val compulsion = baseline { it.appOpens }
        val lateNight = baseline { it.lateNightEvents }
        // Focus is framed as "less fragmented than usual": fewer app switches per hour of
        // screen time than your own baseline, not an arbitrary constant subtraction.
        val focus = baseline { raw -> if (raw.screenMinutes > 0) (raw.appOpens * 60 / raw.screenMinutes) else raw.appOpens }

        val hasHistory = historicalDays.size >= 3
        val hasEvents = todayRaw.totalEvents > 0

        val confidence = when {
            !hasEvents -> 0
            historicalDays.size >= 14 && moods.isNotEmpty() -> 90
            historicalDays.size >= 7 -> 70
            historicalDays.size >= 3 -> 50
            else -> 25
        }

        // The overall index is only computed once there's both real event data today AND
        // enough history to compare against — otherwise it returns null and the UI must say
        // "not enough data" rather than rendering a number that looks meaningful but isn't.
        val index: Int? = if (hasEvents && hasHistory) {
            var points = 100.0
            stimulation.percentVsBaseline?.let { if (it > 20) points -= (it.coerceAtMost(200) / 4.0) }
            compulsion.percentVsBaseline?.let { if (it > 20) points -= (it.coerceAtMost(200) / 5.0) }
            lateNight.percentVsBaseline?.let { if (it > 20) points -= (it.coerceAtMost(200) / 3.0) }
            if (moods.isNotEmpty() && baselineMoods.size >= 5) {
                val moodDelta = moods.map { it.mood }.average() - baselineMoods.map { it.mood }.average()
                points += moodDelta * 6
            }
            points.toInt().coerceIn(0, 100)
        } else null

        return Score(index, stimulation, compulsion, lateNight, focus, confidence, todayRaw, hasHistory && hasEvents)
    }

    fun insights(
        score: Score,
        moods: List<MoodCheckIn>,
        baselineMoods: List<MoodCheckIn>
    ): List<Insight> {
        val out = mutableListOf<Insight>()

        if (score.raw.totalEvents == 0) {
            out += Insight(
                "No activity captured today",
                "The tracking service hasn't recorded any events yet today. Check Settings to confirm it's connected and active."
            )
            return out
        }

        if (!score.hasEnoughDataForIndex) {
            out += Insight(
                "Still building your baseline",
                "A few more days of tracked activity are needed before comparisons against your personal baseline are meaningful. Keep using the app normally."
            )
        }

        score.lateNight.percentVsBaseline?.let {
            if (it >= 40) out += Insight(
                "More late-night activity than usual",
                "Activity between 11 PM and 5 AM today was about ${it}% above your recent average."
            )
        }

        score.stimulation.percentVsBaseline?.let {
            if (it >= 40) out += Insight(
                "Higher scrolling than your usual",
                "Scroll activity today was about ${it}% above your recent average."
            )
        }

        score.compulsion.percentVsBaseline?.let {
            if (it >= 40) out += Insight(
                "More app-switching than usual",
                "You opened/switched apps noticeably more than your recent average today."
            )
        }

        if (moods.isNotEmpty() && baselineMoods.size >= 5) {
            val current = moods.map { it.mood }.average()
            val baseline = baselineMoods.map { it.mood }.average()
            if (current <= baseline - 0.7)
                out += Insight("Mood below your baseline", "Today's self-reported mood is below your recent personal baseline.")
            if (current >= baseline + 0.7)
                out += Insight("Mood above your baseline", "Today's self-reported mood is above your recent personal baseline.")

            // The differentiator: correlate mood against actual tracked behavior, not just
            // mood-vs-mood. This is the one thing a plain screen-time app cannot offer.
            if (current <= baseline - 0.5) {
                val lateNightPct = score.lateNight.percentVsBaseline
                if (lateNightPct != null && lateNightPct >= 30) {
                    out += Insight(
                        "Possible link: late-night activity and mood",
                        "Lower mood today coincided with more late-night activity than your usual pattern. This is a correlation from your own data, not a proven cause."
                    )
                }
            }
        }

        if (out.isEmpty())
            out += Insight("No strong signal", "Nothing crossed a noteworthy threshold compared to your recent baseline today.")

        return out.take(5)
    }

    fun weeklyAverage(moods: List<MoodCheckIn>): Double? =
        moods.takeIf { it.isNotEmpty() }?.map { it.mood }?.average()
}
