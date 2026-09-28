package com.emotiontracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emotiontracker.export.CsvExporter
import com.emotiontracker.monitoring.ActivityRepository
import com.emotiontracker.monitoring.ServiceHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(a: android.app.Activity, r: ActivityRepository) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var health by remember { mutableStateOf(ServiceHealth.snapshot(a)) }

    // Poll the health snapshot while this screen is visible so the diagnostic panel
    // updates live as events come in, rather than requiring a manual refresh.
    LaunchedEffect(Unit) {
        while (true) {
            health = ServiceHealth.snapshot(a)
            delay(2000)
        }
    }

    Column(Modifier.padding(20.dp).fillMaxWidth()) {
        Text("Setup & privacy", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        ServiceHealthCard(health)

        Spacer(Modifier.height(16.dp))
        Button({ a.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, Modifier.fillMaxWidth()) {
            Text("Enable Usage Access")
        }
        Spacer(Modifier.height(8.dp))
        Button({ a.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, Modifier.fillMaxWidth()) {
            Text("Enable Accessibility Tracking")
        }
        Spacer(Modifier.height(8.dp))
        Button({ requestIgnoreBatteryOptimizations(a) }, Modifier.fillMaxWidth()) {
            Text("Disable battery optimization for this app")
        }
        Text(
            "Required on most phones (OnePlus/OxygenOS, Xiaomi, Samsung, etc.) — otherwise the system kills the tracking service in the background and it silently stops recording, even though Accessibility still shows as \"on\".",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(18.dp))
        Text("Local-first privacy", style = MaterialTheme.typography.titleLarge)
        Text("• Behavioral events stay on-device in this MVP.")
        Text("• No continuous screen recording.")
        Text("• No private-message upload.")
        Text("• Mood notes remain local.")
        Text("• Scores are not medical or psychological diagnoses.")
        Spacer(Modifier.height(8.dp))
        Text(
            "Nothing here is a promise you have to take on faith — export everything below and check it yourself.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            scope.launch {
                val events = withContext(Dispatchers.IO) { r.allEvents() }
                val moods = withContext(Dispatchers.IO) { r.allMoods() }
                CsvExporter.exportAndShare(a, events, moods)
            }
        }, Modifier.fillMaxWidth()) { Text("Export all data as CSV") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { confirm = true }) { Text("Delete all local tracker data") }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete local data?") },
            text = { Text("This removes tracked events, mood check-ins and limits stored by this app.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        r.clearAllLocalData()
                        ServiceHealth.reset(a)
                        confirm = false
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ServiceHealthCard(health: ServiceHealth.Snapshot) {
    val now = System.currentTimeMillis()
    val secondsSinceLastEvent = health.lastEventTs?.let { (now - it) / 1000 }
    // "Healthy" means: the service reported itself connected AND we've seen an event
    // recently. A service can report connected while OS-level throttling means no events
    // actually arrive, so we require both signals rather than trusting connection state alone.
    val isHealthy = health.serviceReportsConnected &&
        secondsSinceLastEvent != null && secondsSinceLastEvent < 120

    val (icon, tint, headline) = when {
        !health.serviceReportsConnected -> Triple(Icons.Default.Warning, Color(0xFFB3261E), "Not connected")
        health.totalEventsCaptured == 0L -> Triple(Icons.Default.Warning, Color(0xFFB3261E), "Connected, but no events captured yet")
        !isHealthy -> Triple(Icons.Default.Warning, Color(0xFF8B6F00), "Connected, but quiet")
        else -> Triple(Icons.Default.CheckCircle, Color(0xFF2E7D32), "Tracking is active")
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint)
                Spacer(Modifier.width(8.dp))
                Text(headline, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text("Total events captured: ${health.totalEventsCaptured}")
            Text(
                if (health.lastEventTs != null)
                    "Last event: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(health.lastEventTs))} (${secondsSinceLastEvent}s ago)"
                else "Last event: none yet"
            )
            if (!health.serviceReportsConnected) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Enable Accessibility Tracking below, then use your phone for a few seconds (scroll or switch apps) and this panel will update automatically.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (health.totalEventsCaptured == 0L || !isHealthy) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "If this stays quiet after you've used your phone, the system is likely killing the service in the background. Try \"Disable battery optimization\" below.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun requestIgnoreBatteryOptimizations(activity: android.app.Activity) {
    val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
    val pkg = activity.packageName
    if (pm.isIgnoringBatteryOptimizations(pkg)) {
        // Already exempted — just show the general battery settings so the user can confirm.
        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        return
    }
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$pkg")
        }
        activity.startActivity(intent)
    } catch (t: Throwable) {
        // Some OEM builds block this direct-request intent; fall back to the general screen
        // where the user can find and allow the app manually.
        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
