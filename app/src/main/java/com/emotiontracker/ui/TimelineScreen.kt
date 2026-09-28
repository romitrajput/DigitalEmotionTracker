package com.emotiontracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emotiontracker.data.Event
import com.emotiontracker.monitoring.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimelineScreen(r:ActivityRepository,refresh:Int) {
    var events by remember { mutableStateOf(emptyList<Event>()) }

    LaunchedEffect(refresh) {
        events=withContext(Dispatchers.IO) { r.eventsToday() }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Behavior timeline",style=MaterialTheme.typography.headlineSmall)
        Text("${events.size} tracked events today")
        LazyColumn {
            items(events.take(500)) { e ->
                ListItem(
                    headlineContent={Text(e.type)},
                    supportingContent={
                        Text("${e.packageName} • ${
                            SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date(e.ts))
                        }")
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
