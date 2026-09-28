package com.emotiontracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.emotiontracker.monitoring.ActivityRepository
import com.emotiontracker.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val repo = ActivityRepository(this)
        setContent { TrackerTheme { App(repo, this) } }
    }
}

@Composable
private fun App(repo: ActivityRepository, activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val tabs = listOf("Today", "Trends", "Timeline", "Mood", "Insights", "Limits", "Settings")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {},
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { p ->
        Box(Modifier.padding(p).fillMaxSize()) {
            when (tab) {
                0 -> DashboardScreen(repo, refresh)
                1 -> TrendsScreen(repo, refresh)
                2 -> TimelineScreen(repo, refresh)
                3 -> MoodScreen(repo) { refresh++ }
                4 -> InsightsScreen(repo, refresh)
                5 -> LimitsScreen(repo) { refresh++ }
                6 -> SettingsScreen(activity, repo)
            }
        }
    }
}
