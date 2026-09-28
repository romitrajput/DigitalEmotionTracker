# Digital Emotion Tracker — Phase 4

## What changed in Phase 4

Phase 3's biggest problem was that the accessibility service could silently fail — Accessibility would show as "enabled" while zero events were actually captured, and the dashboard would still render a confident-looking wellbeing score computed from nothing. Phase 4 fixes that and rebuilds scoring to be honest about what it actually knows.

### Fixed: the sensor is now diagnosable
- **Settings screen shows a live health panel** — connected state, total events captured, seconds since the last event — updated every 2 seconds. A silently dead service is now visible instead of producing plausible-looking zeros.
- **"Disable battery optimization" button** — the most common real-world cause of the service going quiet on OEM Android (OnePlus/OxygenOS, Xiaomi, Samsung) is the battery manager killing the background service. This surfaces the OS's own exemption prompt.
- The service now re-asserts its `AccessibilityServiceInfo` config in code on connect (some OEM builds are unreliable trusting the XML config alone), and every DB write is wrapped so a transient failure can't silently stop future events.

### Fixed: honest scoring instead of fabricated numbers
- **Raw counts are shown first** — scrolls, app opens, taps, late-night events, screen minutes — before any derived score, so you can sanity-check the math yourself.
- **Personal baseline, not magic-number formulas.** Every signal (stimulation, compulsion, late-night, focus) is now "today vs. your own trailing 14-day average," not a fixed divisor nobody validated.
- **The wellbeing index returns "not enough data" instead of a number** when there's no event data today or fewer than 3 days of history — it no longer computes a confident-looking score off zero real signal.

### New: Trends tab
7-day / 30-day line charts for scroll activity, app switches, screen time, and mood — using the timestamped data that was already being collected but never visualized.

### New: mood-behavior correlation (the actual differentiator)
Insights now flags when a mood dip coincides with an above-baseline late-night activity pattern, explicitly labeled as correlation from your own data, not proof of cause. This is something a plain screen-time app (Digital Wellbeing, Opal, One Sec) structurally cannot offer, since none of them have a mood input.

### New: CSV export
Export all events and mood check-ins as CSV via the standard Android share sheet. Backs the "your data never leaves this phone" privacy claim with something you can actually check, rather than a paragraph you have to trust.

### New: visual identity
Replaced stock Material3 purple with a calm, low-saturation teal/slate palette (light + dark) — deliberate for a mental-health-adjacent app people may check during a stressed moment.

## What's still a known limitation

Android AccessibilityService exposes accessibility events, but there is no universal reliable API that says "this event equals one Instagram Reel" or "this equals one Reddit post." The app measures scroll activity as a proxy, not exact content counting.

The app does NOT:
- diagnose mental health or emotions
- continuously record the screen
- upload private messages
- claim that behavior proves a particular emotional state

## GitHub setup

The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) is already included — no need to regenerate it. Push the complete project to GitHub as-is; the included GitHub Action builds `./gradlew assembleDebug` and uploads the debug APK as a workflow artifact.

## Recommended testing protocol

For the first 14 days:
1. Enable Usage Access.
2. Enable Accessibility Tracking.
3. Disable battery optimization for the app (Settings tab, in-app button).
4. Confirm the Settings health panel shows "Tracking is active" with a recent event timestamp after using your phone for a minute.
5. Give 2–3 voluntary mood check-ins per day.
6. Use the phone normally — don't change behavior just to generate data.
7. Check Trends and Insights after a few days; the wellbeing index and baseline comparisons need 3+ days of history to mean anything.

## Next production hardening

- Foreground session reconstruction (currently counts discrete events, not session durations)
- NotificationListenerService as an optional additional signal
- App/category classification
- Weekly/monthly summary view ("this week vs last week")
- Lightweight in-the-moment nudge when a self-set scroll limit is reached (Limits tab is currently passive/display-only)
- On-device encrypted database
- Explicit onboarding/consent flow
- Automated tests
- Crash reporting
- AccessibilityService policy review + privacy policy + data safety form before any Play Store submission
