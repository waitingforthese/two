# User Switching + Female Voice V1

- Saved birth profiles are now persistent as a JSON list in SharedPreferences.
- Logout deactivates the current profile instead of deleting saved profiles.
- The Login screen lists saved users and provides a `वापरा` action to reactivate an old user.
- New users are appended to the saved list; an existing matching user is updated instead of duplicated.
- The active profile is restored after app restart.
- AlarmScheduler continues to read the active profile, so personalized guidance follows the selected user.
- TTS prefers an explicitly female Marathi/Indian voice by voice-name keywords and refuses to reuse a saved voice unless it also matches those female keywords.
- If the installed TTS engine exposes no identifiable female Marathi voice, Android cannot guarantee gender through the standard TTS API; the app uses the best Marathi/Indian fallback and logs this condition.
