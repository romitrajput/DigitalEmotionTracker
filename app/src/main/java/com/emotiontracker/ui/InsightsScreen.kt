package com.emotiontracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emotiontracker.monitoring.ActivityRepository
import com.emotiontracker.scoring.Insight
import com.emotiontracker.scoring.ScoreLoader
import com.emotiontracker.scoring.WellbeingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InsightsScreen(r: ActivityRepository, refresh: Int) {
    var insights by remember { mutableStateOf(emptyList<Insight>()) }
    var moodAvg by remember { mutableStateOf<Double?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refresh) {
        loading = true
        val (score, moods, avg) = ScoreLoader.loadScoreAndMoods(r)
        val allMoods = withContext(Dispatchers.IO) { r.allMoods() }
        insights = WellbeingEngine.insights(score, moods, allMoods.takeLast(30))
        moodAvg = avg
        loading = false
    }

    Column(Modifier.padding(18.dp)) {
        Text("Personal insights", style = MaterialTheme.typography.headlineSmall)
        Text("Built from your own behavior and voluntary mood check-ins.")
        Spacer(Modifier.height(12.dp))
        moodAvg?.let { Text("Recent check-in average: ${"%.1f".format(it)}/5") }
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Text("Loading…")
        } else {
            insights.forEach {
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(it.title, style = MaterialTheme.typography.titleMedium)
                        Text(it.body)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("These are correlations and behavioral signals, not proof of cause or diagnosis.")
    }
}
