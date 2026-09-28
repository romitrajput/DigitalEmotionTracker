package com.emotiontracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emotiontracker.data.MoodCheckIn
import com.emotiontracker.monitoring.ActivityRepository
import kotlinx.coroutines.launch

@Composable
fun MoodScreen(r:ActivityRepository,onSaved:()->Unit) {
    var mood by remember { mutableFloatStateOf(3f) }
    var stress by remember { mutableFloatStateOf(3f) }
    var energy by remember { mutableFloatStateOf(3f) }
    var note by remember { mutableStateOf("") }
    val scope=rememberCoroutineScope()

    Column(Modifier.padding(20.dp)) {
        Text("Mood check-in",style=MaterialTheme.typography.headlineSmall)
        Text("Your self-report is used to personalize the behavioral baseline.")
        Spacer(Modifier.height(10.dp))
        SliderField("Mood",mood){mood=it}
        SliderField("Stress",stress){stress=it}
        SliderField("Energy",energy){energy=it}
        OutlinedTextField(
            value=note,onValueChange={note=it},
            label={Text("Optional note")},
            modifier=Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick={
            scope.launch {
                r.saveMood(
                    MoodCheckIn(
                        ts=System.currentTimeMillis(),
                        mood=mood.toInt(),stress=stress.toInt(),
                        energy=energy.toInt(),note=note
                    )
                )
                note=""
                onSaved()
            }
        }) { Text("Save check-in") }
    }
}

@Composable
private fun SliderField(name:String,value:Float,onChange:(Float)->Unit) {
    Text("$name: ${value.toInt()}/5")
    Slider(value=value,onValueChange=onChange,valueRange=1f..5f,steps=3)
}
