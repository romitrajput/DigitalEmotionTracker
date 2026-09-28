package com.emotiontracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emotiontracker.data.AppLimit
import com.emotiontracker.monitoring.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LimitsScreen(r:ActivityRepository,onChanged:()->Unit) {
    val apps=listOf(
        "com.instagram.android" to "Instagram",
        "com.reddit.frontpage" to "Reddit",
        "com.twitter.android" to "X",
        "com.facebook.katana" to "Facebook"
    )
    var selected by remember { mutableStateOf(apps[0].first) }
    var limit by remember { mutableIntStateOf(200) }
    var count by remember { mutableIntStateOf(0) }
    val scope=rememberCoroutineScope()

    LaunchedEffect(selected) {
        count=withContext(Dispatchers.IO){r.scrollCount(selected)}
    }

    Column(Modifier.padding(18.dp)) {
        Text("Consumption limits",style=MaterialTheme.typography.headlineSmall)
        Text("This measures scroll events as a proxy; it does not guarantee an exact Reel/post count.")
        apps.forEach { (pkg,name) ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(name)
                RadioButton(selected==pkg,{selected=pkg})
            }
        }
        Text("Daily scroll-event limit: $limit")
        Slider(value=limit.toFloat(),onValueChange={limit=it.toInt()},valueRange=20f..500f)
        Text("Recorded today: $count")
        Button(onClick={
            scope.launch {
                r.saveLimit(AppLimit(selected,apps.first{it.first==selected}.second,limit,true))
                onChanged()
            }
        }) { Text("Save limit") }
        Text(if(count>=limit)"Limit reached" else "${(limit-count).coerceAtLeast(0)} remaining")
    }
}
