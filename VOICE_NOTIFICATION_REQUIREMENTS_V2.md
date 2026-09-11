# Life Alarm — Voice/Notification Requirements V2

Implemented on baseline `LifeAlarm Voice Announcement V1 – FIXED V2`.

## Notification timing wording
- Event ending/changing today: say `आज ...` and include time.
- Event tomorrow: say `उद्या ...` and include time.
- Event after tomorrow: say `<date> <Marathi month> <year> रोजी ...` and include time.
- The same timing is shown in standard change notifications.

## Voice
- Marathi TTS is preferred.
- A female Marathi voice is selected when the installed TTS engine exposes one.
- The selected/auto-selected TTS voice name is persisted and reused on future announcements.
- Soft background music remains optional and separately switchable.

## Saved user settings
- All alarm switches are persisted in SharedPreferences.
- Voice and background-music switches are persisted.
- Preferred TTS voice name is persisted.
- Temporary 3-hour notification/voice mute end time is persisted, so it survives app restart.

## Temporary mute
- Settings contains `3 तासांसाठी Notification + Voice बंद`.
- It suppresses future Life Alarm notifications and voice announcements without deleting the scheduled alarms.
- Existing displayed notifications are cleared when the user activates the mute.
- `पुन्हा सुरू करा` cancels the mute immediately.

## Test coverage
The existing full voice test now includes 16 tests:
1–13: all supported panchang/astronomy voice announcements.
14: today timing wording.
15: tomorrow timing wording.
16: future-date timing wording.

All test alarms can be cancelled with the existing `सर्व Test Alarm बंद करा` action.
