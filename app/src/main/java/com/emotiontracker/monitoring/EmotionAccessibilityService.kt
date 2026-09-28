package com.emotiontracker.monitoring

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.emotiontracker.data.Event
import com.emotiontracker.data.TrackerDatabase
import kotlinx.coroutines.*

class EmotionAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao by lazy { TrackerDatabase.get(this).dao() }
    private var lastPackage: String? = null
    private var lastScroll = 0L

    // Some OEM builds (OxygenOS included) are unreliable about honoring the XML-declared
    // AccessibilityServiceInfo alone. Re-asserting it in code on connect is a cheap safeguard,
    // and onServiceConnected is also our reliable signal that Android actually bound the
    // service, which the XML config alone cannot tell us.
    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                notificationTimeout = 50
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            serviceInfo = info
        } catch (t: Throwable) {
            Log.e("EmotionTracker", "Failed to set serviceInfo", t)
        }
        ServiceHealth.markConnected(this)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // Cheap, synchronous — always record that the service is alive and doing work,
        // independent of whether the DB write below succeeds.
        ServiceHealth.recordEvent(this, now)

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                if (pkg != lastPackage) {
                    lastPackage = pkg
                    save(Event(ts = now, packageName = pkg, type = "APP_OPEN"))
                }

            AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                if (now - lastScroll > 250) {
                    lastScroll = now
                    save(Event(ts = now, packageName = pkg, type = "SCROLL", value = 1))
                }

            AccessibilityEvent.TYPE_VIEW_CLICKED ->
                save(Event(ts = now, packageName = pkg, type = "CLICK"))

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ->
                save(Event(ts = now, packageName = pkg, type = "TEXT_CHANGED"))
        }
    }

    private fun save(e: Event) = scope.launch {
        try {
            dao.insertEvent(e)
        } catch (t: Throwable) {
            // A single failed insert (e.g. transient DB lock) should never crash the service
            // or silently stop future events from being recorded.
            Log.e("EmotionTracker", "Failed to save event", t)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        ServiceHealth.markDisconnected(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ServiceHealth.markDisconnected(this)
        scope.cancel()
        super.onDestroy()
    }
}
