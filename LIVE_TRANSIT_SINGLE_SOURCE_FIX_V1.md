# LifeAlarm V14.2.2.2 — Live Transit Single-Source Fix

## Problem fixed
The Kundli Reading Reference transit overlay could disagree with the Home/Today live planetary positions because FrameworkCalculator evaluated transits at a fixed 12:00 time instead of the actual current instant.

## Fix
- Added `LiveTransitCalculator` as the single sidereal transit source.
- Uses Swiss Ephemeris with Lahiri/Chitrapaksha sidereal mode.
- Uses the actual current instant (`System.currentTimeMillis()`) converted to UTC Julian Day.
- Rahu uses `SE_TRUE_NODE`; Ketu is exactly 180° from Rahu, matching the existing project convention.
- Framework 5-day comparison uses the same local clock time for each day.
- Today Prediction now reads planetary longitudes from the same shared calculator.
- Kundli Reading Reference therefore receives the same current transit rashi and degree as the live prediction engine.
- Birth-chart calculations and UI/freeze-pane behavior are unchanged.

## Validation
At the current test instant, the shared Swiss Ephemeris calculation produced approximately:
- Sun 22.289° Leo
- Moon 26.795° Cancer
- Mars 24.300° Gemini
- Mercury 3.205° Virgo
- Jupiter 21.161° Cancer
- Venus 4.995° Libra
- Saturn 18.949° Pisces
- Rahu 5.590° Aquarius
- Ketu 5.590° Leo

These positions agree with the observed Home-screen Moon/Sun rashis and remove the previous fixed-noon discrepancy.
