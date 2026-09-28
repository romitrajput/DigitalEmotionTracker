package com.emotiontracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emotiontracker.monitoring.ActivityRepository
import com.emotiontracker.scoring.RawCounts
import com.emotiontracker.scoring.WellbeingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private data class DayPoint(val label: String, val raw: RawCounts, val moodAvg: Double?)

@Composable
fun TrendsScreen(r: ActivityRepository, refresh: Int) {
    var windowDays by remember { mutableIntStateOf(7) }
    var points by remember { mutableStateOf<List<DayPoint>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refresh, windowDays) {
        loading = true
        points = withContext(Dispatchers.IO) {
            val allMoods = r.allMoods()
            (windowDays - 1 downTo 0).map { daysAgo ->
                val events = r.eventsForDay(daysAgo)
                val minutes = r.screenMinutesForDay(daysAgo)
                val raw = WellbeingEngine.rawCounts(events, minutes)
                val dayStart = r.dayBucketStart(daysAgo)
                val dayEnd = r.dayBucketStart(daysAgo - 1)
                val dayMoods = allMoods.filter { it.ts >= dayStart && it.ts < dayEnd }
                val moodAvg = if (dayMoods.isNotEmpty()) dayMoods.map { it.mood }.average() else null
                val label = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dayStart))
                DayPoint(label, raw, moodAvg)
            }
        }
        loading = false
    }

    Column(Modifier.padding(18.dp).fillMaxSize()) {
        Text("Trends", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row {
            WindowToggle("7 days", windowDays == 7) { windowDays = 7 }
            Spacer(Modifier.width(8.dp))
            WindowToggle("30 days", windowDays == 30) { windowDays = 30 }
        }
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Text("Loading…")
        } else if (points.all { it.raw.totalEvents == 0 && it.raw.screenMinutes == 0 }) {
            Text("No tracked activity in this window yet. Trends will appear once the service has been running for a few days.")
        } else {
            TrendChart("Scroll activity", points) { it.raw.scrollEvents.toFloat() }
            Spacer(Modifier.height(20.dp))
            TrendChart("App switches", points) { it.raw.appOpens.toFloat() }
            Spacer(Modifier.height(20.dp))
            TrendChart("Screen time (min)", points) { it.raw.screenMinutes.toFloat() }
            Spacer(Modifier.height(20.dp))
            if (points.any { it.moodAvg != null }) {
                TrendChart("Mood check-ins (1-5)", points, maxOverride = 5f) { it.moodAvg?.toFloat() ?: -1f }
            }
        }
    }
}

@Composable
private fun WindowToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun TrendChart(
    title: String,
    points: List<DayPoint>,
    maxOverride: Float? = null,
    valueOf: (DayPoint) -> Float
) {
    val values = points.map(valueOf)
    val maxValue = maxOverride ?: (values.filter { it >= 0 }.maxOrNull()?.coerceAtLeast(1f) ?: 1f)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
            val validPoints = values.mapIndexedNotNull { i, v ->
                if (v < 0) null else Offset(i * stepX, size.height - (v / maxValue) * size.height)
            }
            // baseline
            drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            for (i in 0 until validPoints.size - 1) {
                drawLine(
                    primary,
                    validPoints[i],
                    validPoints[i + 1],
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
            validPoints.forEach { p -> drawCircle(primary, radius = 6f, center = p) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEachIndexed { i, p ->
                if (points.size <= 8 || i % (points.size / 7).coerceAtLeast(1) == 0) {
                    Text(p.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
