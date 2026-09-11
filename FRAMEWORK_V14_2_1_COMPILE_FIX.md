LifeAlarm V14.2.1 – Framework Compile Fix

Locked baseline: LifeAlarm V14.1 – Notification Compile Fix

Change:
- Fixed FrameworkScreen.kt unresolved reference `nativeCanvas` by adding the required Compose graphics extension import.
- No alarm, notification, Aaradhana, birth-chart, or other baseline functionality intentionally changed.

Build note:
- ZIP integrity and source-level structural checks performed locally.
- Full Gradle/GitHub Actions build not available in this environment; run GitHub Actions to verify compilation.
