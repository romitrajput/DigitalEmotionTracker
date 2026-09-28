package com.emotiontracker.monitoring

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight, synchronous health tracker for the accessibility service.
 *
 * The service's actual work goes through Room (async, on IO dispatcher), which is fine for
 * durable storage but useless for a user trying to answer "is this thing even running right
 * now?" in real time. SharedPreferences writes are fast and synchronous enough to call directly
 * from onAccessibilityEvent without adding meaningful overhead, and they survive process death,
 * which matters because OEM battery managers (OxygenOS included) can kill the service process
 * without the user doing anything, and Android does not always restart it automatically.
 *
 * The Settings screen reads this to show a live "connected / last event N seconds ago / total
 * events captured" panel, so a silently dead service is visible instead of silently producing
 * a dashboard full of zeros that look like real numbers.
 */
object ServiceHealth {
    private const val PREFS = "service_health"
    private const val KEY_CONNECTED = "connected"
    private const val KEY_LAST_EVENT_TS = "last_event_ts"
    private const val KEY_EVENT_COUNT = "event_count"
    private const val KEY_CONNECTED_SINCE = "connected_since"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun markConnected(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_CONNECTED_SINCE, System.currentTimeMillis())
            .apply()
    }

    fun markDisconnected(context: Context) {
        prefs(context).edit().putBoolean(KEY_CONNECTED, false).apply()
    }

    fun recordEvent(context: Context, ts: Long) {
        val p = prefs(context)
        val count = p.getLong(KEY_EVENT_COUNT, 0L) + 1
        p.edit()
            .putLong(KEY_LAST_EVENT_TS, ts)
            .putLong(KEY_EVENT_COUNT, count)
            .apply()
    }

    data class Snapshot(
        val serviceReportsConnected: Boolean,
        val connectedSince: Long?,
        val lastEventTs: Long?,
        val totalEventsCaptured: Long
    )

    fun snapshot(context: Context): Snapshot {
        val p = prefs(context)
        return Snapshot(
            serviceReportsConnected = p.getBoolean(KEY_CONNECTED, false),
            connectedSince = p.getLong(KEY_CONNECTED_SINCE, 0L).takeIf { it > 0 },
            lastEventTs = p.getLong(KEY_LAST_EVENT_TS, 0L).takeIf { it > 0 },
            totalEventsCaptured = p.getLong(KEY_EVENT_COUNT, 0L)
        )
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
