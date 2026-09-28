package com.emotiontracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emotiontracker.monitoring.ActivityRepository
import com.emotiontracker.scoring.BaselineComparison
import com.emotiontracker.scoring.RawCounts
import com.emotiontracker.scoring.Score
import com.emotiontracker.scoring.ScoreLoader

@Composable
fun DashboardScreen(r: ActivityRepository, refresh: Int) {
    var score by remember { mutableStateOf<Score?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refresh) {
        loading = true
        score = ScoreLoader.loadScore(r)
        loading = false
    }

    LazyColumn(Modifier.padding(18.dp)) {
        item {
            Text("Today", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            val s = score
            if (loading || s == null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) { Text("Loading…") }
                }
                return@item
            }

            IndexCard(s)
            Spacer(Modifier.height(16.dp))

            Text("Raw counts today", style = MaterialTheme.typography.titleMedium)
            Text(
                "These are the actual numbers the signals below are built from.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            RawCountsGrid(s.raw)

            Spacer(Modifier.height(16.dp))
            Text("Signals vs. your recent baseline", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BaselineMetric("Scroll activity", s.stimulation)
            BaselineMetric("App switching", s.compulsion)
            BaselineMetric("Late-night activity", s.lateNight)
            BaselineMetric("Switches per hour (lower = steadier focus)", s.focus, lowerIsBetter = true)

            Spacer(Modifier.height(12.dp))
            Text(
                "The index is not a medical or psychological diagnosis. It compares today against your own recent history, not a population average.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IndexCard(s: Score) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            if (s.index != null) {
                Text("${s.index}", style = MaterialTheme.typography.displayMedium)
                Text("Digital wellbeing index")
                Text("Experimental behavioral signal • confidence ${s.confidence}%")
            } else {
                Text("—", style = MaterialTheme.typography.displayMedium)
                Text("Not enough data yet", fontWeight = FontWeight.Medium)
                Text(
                    if (s.raw.totalEvents == 0)
                        "No events captured today. Check Settings to confirm tracking is active."
                    else
                        "Keep using the app for a few more days to build your personal baseline.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RawCountsGrid(raw: RawCounts) {
    Column {
        RawRow("Screen time", "${raw.screenMinutes} min")
        RawRow("Scrolls", "${raw.scrollEvents}")
        RawRow("App opens/switches", "${raw.appOpens}")
        RawRow("Taps", "${raw.clicks}")
        RawRow("Late-night events (11pm–5am)", "${raw.lateNightEvents}")
        RawRow("Total tracked events", "${raw.totalEvents}")
    }
}

@Composable
private fun RawRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BaselineMetric(name: String, b: BaselineComparison, lowerIsBetter: Boolean = false) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        if (b.baselineAverage == null) {
            Text("Today: ${b.todayValue.toInt()} • no baseline yet", style = MaterialTheme.typography.bodySmall)
        } else {
            val pct = b.percentVsBaseline ?: 0
            val worse = if (lowerIsBetter) pct > 0 else pct > 0
            val arrow = if (pct > 0) "▲" else if (pct < 0) "▼" else "–"
            val color = if (worse && kotlin.math.abs(pct) >= 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            Text(
                "Today: ${b.todayValue.toInt()} • your average: ${"%.1f".format(b.baselineAverage)} • $arrow ${kotlin.math.abs(pct)}%",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}
