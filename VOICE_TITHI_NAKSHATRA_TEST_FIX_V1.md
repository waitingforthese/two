Life Alarm 3.1 changes

Baseline: LifeAlarm Voice Announcement V1 – FIXED V2

1. Standardized Nakshatra name to शतभिषा across live Moon, live Sun, MoonState and Nakshatra Guidance.
2. Fixed Panchang “पुढील बदल” naming: next Tithi/Yoga/Karana/Paksha name is sampled just after the boundary so the current value is not repeated at an exact transition.
3. Added full end-to-end Voice/Notification test sequence for 13 events: Moon Rashi, Moon Nakshatra, Moon Charan, Sun Rashi, Sun Nakshatra, Sun Charan, Tithi, Yoga, Karana, Paksha, Prahar, Lagna, Nakshatra Guidance.
4. Test events are 15 seconds apart to reduce TTS overlap.
5. Added “सर्व Test Alarm बंद करा” control.
6. Nakshatra Guidance test uses the same guidance notification path.
7. Female Marathi TTS voice is preferred when the installed TTS engine exposes a female voice by name; Hindi fallback also attempts female selection.
8. Only voice_background.mp3 remains in res/raw; old ringtone assets are not required.
9. Version 3.1 / versionCode 11.

Validation performed in this environment: static source checks, reference checks, delimiter balance, ZIP integrity. Android SDK/Gradle runtime build was not available in this environment, so GitHub Actions build remains the final compile/runtime verification.
