package com.emotiontracker.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.emotiontracker.data.Event
import com.emotiontracker.data.MoodCheckIn
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Local-only CSV export, shared via Android's standard share sheet (email, Drive, save to
 * Files, etc.) — the user chooses where it goes, nothing is uploaded automatically. This is
 * what backs the "your data never leaves this phone" claim in Settings with something the
 * user can actually verify, rather than trusting a paragraph of privacy-policy prose.
 */
object CsvExporter {
    private val tsFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportAndShare(context: Context, events: List<Event>, moods: List<MoodCheckIn>) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val eventsFile = File(dir, "events_$stamp.csv").apply {
            bufferedWriter().use { w ->
                w.write("timestamp,package_name,event_type,value\n")
                events.forEach { e ->
                    w.write("${tsFormat.format(Date(e.ts))},${csvSafe(e.packageName)},${e.type},${e.value}\n")
                }
            }
        }

        val moodsFile = File(dir, "moods_$stamp.csv").apply {
            bufferedWriter().use { w ->
                w.write("timestamp,mood,stress,energy,note\n")
                moods.forEach { m ->
                    w.write("${tsFormat.format(Date(m.ts))},${m.mood},${m.stress},${m.energy},${csvSafe(m.note)}\n")
                }
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val uris = arrayListOf(
            FileProvider.getUriForFile(context, authority, eventsFile),
            FileProvider.getUriForFile(context, authority, moodsFile)
        )

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export tracker data").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun csvSafe(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n")) "\"$escaped\"" else escaped
    }
}
