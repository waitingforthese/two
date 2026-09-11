# LifeAlarm V14.2.2.2 — Live Transit Fix

Built from `LifeAlarm_V14_2_2_1_ALL_SCREENS_KUNDLI_FREEZEPANE.zip`.

### Locked behavior preserved
- V14.1 notification baseline remains untouched.
- Kundli popup, D-1 + Chandra reference, pastel transit overlay and freeze-pane UI are preserved.
- BirthChartCalculator / natal Lagna correction is preserved.
- Aaradhana, alarms, Master Alarm and other existing functions are unchanged.

### Transit correction
- One shared `LiveTransitCalculator` now supplies sidereal planetary longitudes.
- Exact current instant is used; no fixed 12:00/noon calculation.
- Lahiri/Chitrapaksha sidereal mode.
- Rahu = Swiss Ephemeris TRUE_NODE; Ketu = Rahu + 180°.
- Today Prediction and Framework/Kundli Reference use the same transit source.
- Framework's -2/+2 comparison uses the same local clock time on each date.
