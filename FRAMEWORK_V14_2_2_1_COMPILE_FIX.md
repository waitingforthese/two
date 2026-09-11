# LifeAlarm V14.2.2.1 – Framework Compile Fix

Parent candidate: LifeAlarm V14.2.2 – Framework Transit Overlay
Locked baseline: LifeAlarm V14.1 – Notification Compile Fix

Fix:
- Moved `degreeText()` to file-level scope in `FrameworkScreen.kt`.
- This fixes GitHub Actions error: `FrameworkScreen.kt:406:65 Unresolved reference: degreeText`.

No alarm, notification, Aaradhana, or V14.1 baseline functionality was intentionally changed.

Note: `libandroidx.graphics.path.so` strip message is a packaging warning and is not the Kotlin compilation failure.
