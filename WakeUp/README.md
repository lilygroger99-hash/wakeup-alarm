# WakeUp: Android alarm clock

A native Android alarm clock written in Kotlin with Jetpack Compose and Material 3. It works fully offline, has no
ads, no analytics and no network permission.

> **Status:** the alarm core builds on GitHub Actions and runs on a real phone. The stopwatch and the custom-audio
> feature were added afterwards: build them, then run the checklist below on a real phone before you publish.

## Features

- Alarm list with large times, AM/PM or 24-hour (follows the device, or force 12/24 in Settings), repeat days, label and an ON/OFF switch
- "Next alarm" card ("Rings in 7 hours 30 minutes") and an empty state
- Create/edit: time dial or typed time, label, repeat presets and per-day toggles, system alarm sounds, vibration, snooze length, enable/disable
- Ringing: looping alarm sound + vibration, full-screen STOP / SNOOZE screen over the lock screen, notification with the same buttons
- Snooze (1, 5, 10, 15, 20, 30 minutes) with a "snoozed until" state and a "Cancel snooze" notification button
- Reboot, time-change, time-zone and app-update recovery, with a "Missed alarm" notification for alarms that were due while the phone was off
- Stopwatch with laps (keeps running while you leave the screen)
- Your own alarm sounds: add an audio file from your phone (copied into the app, up to 20 files of 20 MB), pick it for any alarm, delete it again at any time
- Settings (theme, dynamic colors, time format, defaults for new alarms, permission status), About screen, privacy policy link
- Light, dark and system themes; TalkBack labels, 48dp touch targets, font scaling

## Open it in Android Studio

1. Install the current stable **Android Studio** (this project uses AGP 8.13, so you need Narwhal 3 / 2025.1.3 or newer).
2. **File > Open** and choose the `WakeUp` folder. Wait for "Gradle sync" to finish. Android Studio downloads the
   Android SDK 36 platform if it is missing (accept the licence prompt).
3. If Studio offers the **AGP Upgrade Assistant** (AGP 9 is available), you can accept it later. First get the project
   running as it is.
4. Run the unit tests: right-click `app/src/test` > **Run Tests** (the time calculation has 12 tests).
5. Plug in your phone (USB debugging on) and press **Run**.

## Project structure

```
app/src/main/java/com/wakeup/alarm/
  WakeUpApp.kt, MainActivity.kt
  di/AppContainer.kt          manual dependency container (no DI framework)
  domain/                     Alarm model, AlarmCalculator (pure next-time logic, unit tested)
  data/                       Room (local/), DataStore settings (settings/), AlarmRepository
  alarm/                      AlarmScheduler, AlarmReceiver, AlarmService, RingActivity, AlarmPlayer,
                              AlarmNotifications, SystemEventReceiver, AlarmActionReceiver
  ui/                         theme, common components, home, edit, ring, settings, navigation
  util/                       TimeFormatter, SoundUtils, PermissionUtils
```

Architecture: MVVM. Screens observe `StateFlow`s from ViewModels, ViewModels talk to repositories, repositories own
Room and AlarmManager. The alarm clock logic lives in `domain/` and has no Android dependencies.

## How the alarm works

| Topic | What the app does |
|---|---|
| Scheduling | `AlarmManager.setAlarmClock()`: the API meant for user-visible alarms. Exact, not delayed by Doze, shows the alarm icon and "next alarm" on the lock screen. One PendingIntent per alarm id, so scheduling again replaces instead of duplicating. |
| Exact-alarm permission | Android 13+: `USE_EXACT_ALARM` (granted at install, allowed for alarm clock apps). Android 12/12L: `SCHEDULE_EXACT_ALARM` (declared with `maxSdkVersion="32"`); the home screen shows a banner that opens the right system settings page. If the permission is missing the app falls back to an inexact alarm instead of crashing. |
| Ringing | `AlarmReceiver` starts `AlarmService` as a foreground service of type `mediaPlayback`. It plays the sound (alarm audio stream) and vibrates, holds a wake lock, and posts a high-priority notification with a full-screen intent that opens `RingActivity`. The service exists only while an alarm rings; there is no permanent background service. Unattended alarms stop after 10 minutes and leave a "Missed alarm" notification. |
| Sounds | The system alarm-sound picker. Fallback chain: chosen sound > default alarm > default notification > default ringtone > a beep generated in code. A deleted sound never means a silent alarm. |
| Repeat / one-time | Repeating alarms re-arm the next occurrence the moment they ring. One-time alarms switch themselves off after ringing. |
| Snooze | Stores a snooze time, arms it, and keeps the regular schedule intact for repeating alarms. |
| Reboot / time change | `SystemEventReceiver` handles `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` and exact-alarm permission changes and re-arms everything from the database. Alarms are wall-clock based and re-resolved in the current time zone (daylight saving is handled by `java.time`). |
| Direct Boot | The database and settings live in device-protected storage, so alarms are restored and can ring before the first unlock after a reboot. They hold only alarm times and settings. |
| Storage | Room (alarms) and Preferences DataStore (settings). `allowBackup` is off. |

### Known limitations (be aware before publishing)

- If the exact-alarm permission is missing (Android 12 only) the fallback inexact alarm may be delayed, and Android may refuse to start the ringing service from the background. The banner on the home screen tells the user how to fix it.
- Some manufacturers (Xiaomi, Huawei, OnePlus, Samsung "sleeping apps") add their own battery killers. If alarms ever fail on one of those phones, the user must exclude the app from battery optimisation in the phone's own settings. Test on a few brands.
- The generated beep is a last-resort fallback; the default sound normally plays.

## Build commands

Run these in the project folder (use `gradlew.bat` instead of `./gradlew` on Windows). The first run downloads Gradle and
the dependencies, so it needs internet.

**Debug APK** (for testing on your phone; installs next to the release app as `com.wakeup.alarm.debug`):
```
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`. In Android Studio: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

**Release APK / signed AAB.** First create an **upload key** (once):
```
keytool -genkeypair -v -keystore wakeup-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
Keep `wakeup-upload.jks` and its passwords somewhere safe and backed up, **outside** the project folder or git. Then copy
`keystore.properties.example` to `keystore.properties` and fill in the path and passwords. (`*.jks` and
`keystore.properties` are already in `.gitignore`.)

```
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk  (for direct installs)
./gradlew bundleRelease       # app/build/outputs/bundle/release/app-release.aab  (this is what you upload to Google Play)
```
Without `keystore.properties` the release build is produced unsigned (it still builds, but it cannot be installed or
uploaded). In Android Studio you can instead use **Build > Generate Signed App Bundle / APK**.

R8 shrinking is on for release builds. After building, upload `app/build/outputs/mapping/release/mapping.txt`
along with the bundle in the Play Console (App bundle explorer / deobfuscation file) to get readable crash reports.

Before every update: raise `versionCode` in `app/build.gradle.kts` (it must increase with each upload) and update
`versionName`.

## Before you publish

1. **Change the `applicationId`** in `app/build.gradle.kts` (`com.wakeup.alarm` is a placeholder). It can never be changed after the first upload. Also check that no one else on Google Play already uses it.
2. **Set a real privacy-policy URL** in `app/src/main/res/values/strings.xml` (`privacy_policy_url`; currently `https://example.com/wakeup/privacy`) and fill in the developer name in `about_developer_value`.
3. Read `PLAY_STORE_CHECKLIST.md` (listing, Data safety, exact-alarm declaration, testing tracks).

## Renaming the app

- Display name: change `app_name` in `app/src/main/res/values/strings.xml` (everything in the UI reads from it).
- Store/Gradle name: `rootProject.name` in `settings.gradle.kts`.
- Package id: `applicationId` in `app/build.gradle.kts`. The Kotlin `namespace` / package folder (`com.wakeup.alarm`) does not have to change; if you want it renamed too, use Android Studio's **Refactor > Rename** on the package so imports and the manifest update together.
- The launcher icon is an adaptive icon made of three PNG layers in `res/drawable-nodpi/` (`ic_launcher_bg.png`, `ic_launcher_fg.png`, `ic_launcher_mono.png` for Android 13+ themed icons), wired up in `res/mipmap-anydpi-v26/ic_launcher*.xml`. The About screen shows `about_logo.png`. The 512 × 512 Play Store icon (`wakeup-play-icon-512.png`) is the same artwork as one full square.

## Test checklist (do this on a real phone)

1. Create an alarm for 2 minutes from now, lock the phone, wait: it should ring with the full-screen screen. Press **Stop**.
2. Same, press **Snooze**: it should ring again after the chosen time; the home screen shows "Snoozed until".
3. Create a repeating alarm (weekdays): after it rings, the home screen still shows it ON and "Next alarm" points to the next matching day.
4. A one-time alarm turns itself OFF after ringing.
5. Reboot the phone with an alarm set for later: it still rings. (Also try restarting just before the alarm time passes.)
6. Change the time zone / the clock in system settings: "Next alarm" updates and the alarm still rings at its local time.
7. Deny notification permission: the sound and vibration still work and the banner explains what is limited.
8. Pick a custom sound, then delete or disable it (or use an SD-card sound and remove the card): the default sound plays instead.
9. Switch Settings > Theme between System, Light and Dark; turn on TalkBack and swipe through an alarm card and the edit screen; set system font size to the largest.
10. Custom audio: in an alarm tap Sound > Add your own audio, pick an mp3/m4a, save, and let it ring. Then delete it in the same chooser: the alarm must fall back to the default sound.
11. Stopwatch (clock icon on the home screen): start, lap, pause, leave the screen and come back, rotate the phone, reset.
12. Turn on Do Not Disturb (alarms allowed): the alarm still rings. Put the phone in battery saver and leave it idle for an hour before an alarm.
