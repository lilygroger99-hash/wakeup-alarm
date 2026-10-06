# Google Play publishing checklist for WakeUp

Verified against Google's official pages on 2026-10-04. Play rules change, so re-read the linked pages before you submit.

## 1. Before you build the release

- [ ] **Application id**: replace `com.wakeup.alarm` in `app/build.gradle.kts` with an id you own. It is permanent after the first upload.
- [ ] **Target API**: the project targets **API 36 (Android 16)**. Google Play requires new apps and updates to target API 36 or higher
      ([Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878)). Next year's deadline will move this up; check each August.
- [ ] **Privacy policy URL** set in `strings.xml` (`privacy_policy_url`). Use `PRIVACY_POLICY_TEMPLATE.md` as a starting point and host it on a public page (GitHub Pages, your website, Google Sites).
- [ ] Developer name filled in on the About screen (`about_developer_value`).
- [ ] `versionCode` / `versionName` set. `versionCode` must go up with every upload.
- [ ] Test on at least one Samsung and one Xiaomi/OnePlus/Oppo phone (aggressive battery managers) and on Android 12, 13/14 and 15/16 if you can.
- [ ] Build the signed bundle: `./gradlew bundleRelease` (see README).

## 2. Developer account

- [ ] Create a Google Play Console account (one-time registration fee, identity verification; organizations also need a D-U-N-S number).
- [ ] **Closed-testing rule for personal accounts**: if your *personal* account was created after 13 Nov 2023, you must run a **closed test with at least 12 testers opted in continuously for 14 days** before you can apply for production access
      ([Google's testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)). Organization accounts are not subject to this. Recruit testers early.

## 3. App signing

- [ ] Use **Play App Signing** (default for new apps). You upload with your *upload key* (`wakeup-upload.jks`); Google holds the real app-signing key.
- [ ] Back up the upload keystore and passwords in two places. If you lose it you can request an upload-key reset, but it takes time.

## 4. Create the app in Play Console

- [ ] **Create app**: name (max 30 chars), default language, App (not game), Free.
- [ ] Accept the Developer Program Policies and US export laws declarations.

## 5. Store listing

| Asset | Requirement (check Console for exact limits) |
|---|---|
| App name | up to 30 characters, e.g. "WakeUp: Simple Alarm Clock" |
| Short description | up to 80 characters, e.g. "A fast, private alarm clock with snooze and repeat days." |
| Full description | up to 4000 characters; list the features from README; mention it works offline and collects no data |
| App icon | 512 × 512 px PNG (32-bit, no rounded corners; Play rounds them). Use `wakeup-play-icon-512.png` (full square, no baked-in rounded corners). |
| Feature graphic | 1024 × 500 px PNG or JPG |
| Phone screenshots | at least 2 (up to 8). Suggested set: home with alarms, empty state, edit screen, full-screen ringing screen, dark theme, settings |
| Category | Tools (or Productivity) |
| Contact | email address (public), e.g. pax.official.com@gmail.com, optional website/phone |

Take screenshots on a real device or emulator (Pixel, 1080 × 2400). Don't add device frames that imitate other brands' products, and don't use other apps' UI or any copyrighted material.

## 6. App content declarations (Policy > App content)

- [ ] **Privacy policy**: paste the URL.
- [ ] **Ads**: "No, my app does not contain ads".
- [ ] **App access**: all functionality available without login.
- [ ] **Content rating**: fill the IARC questionnaire (no violence, no user-generated content, no purchases). An alarm clock normally lands at "Everyone".
- [ ] **Target audience**: 13+ / general audience. Do not target children under 13 unless you want the Families policy obligations.
- [ ] **News app / COVID / government / financial / health apps**: answer No.
- [ ] **Data safety form**: the app does not collect or share user data and has no network permission. Declare *"No data collected"* and *"No data shared"*, and confirm that matches your privacy policy. Everything (alarms, settings) stays on the device. If you later add analytics, crash reporting, ads or cloud backup, update this form first.
- [ ] **Exact alarms / permissions**: the app declares `USE_EXACT_ALARM`, which is only permitted for alarm-clock and calendar apps. If Play Console asks for a permission declaration, choose "alarm clock" as the core functionality. Your listing and screenshots should clearly show an alarm clock.
- [ ] **Full-screen intent declaration**: the app declares `USE_FULL_SCREEN_INTENT` to show the ringing screen over the lock screen. Since 22 Jan 2025 only apps with calling or **alarm** functionality get it enabled by default on Android 14+, and Play requires a Console declaration that your core functionality is permitted
      ([Foreground service and full-screen intent requirements](https://support.google.com/googleplay/android-developer/answer/13392821)).
      Select that the app's core functionality is an alarm clock.
- [ ] **Foreground service declaration**: the app uses a `mediaPlayback` foreground service while an alarm rings. In **Policy > App content > Foreground service permissions** you must describe what the service does, what happens if it is interrupted, pick the matching use case, and **link a short video** showing how a user triggers it
      ([same page](https://support.google.com/googleplay/android-developer/answer/13392821)).
      Suggested video (30-60 s, unlisted on YouTube): create an alarm for one minute ahead, lock the phone, show it ringing with the full-screen screen, press Stop. Description text: "WakeUp plays the alarm sound the user scheduled. The service runs only while an alarm is ringing, stops when the user presses Stop/Snooze or after 10 minutes. If interrupted, the user would not be woken."
- [ ] **Government apps / Financial features / Health**: not applicable.

## 7. Testing tracks (recommended order)

1. **Internal testing**: up to 100 testers by email, available within minutes, no review wait. Upload your first `.aab` here to check that the declarations and signing work, then install from the Play link on real phones.
2. **Closed testing**: required for new personal accounts (12 testers, 14 days, see section 2). Use the Play Console's tester list or a Google Group. Ask testers to leave the app installed and to try real wake-up alarms.
3. **Open testing** (optional): public beta from the store listing.
4. **Production**: once production access is granted, create a release, upload the `.aab`, write release notes, choose countries, and set the rollout (a staged rollout of 20% first is a good idea for an alarm app).

Also upload `app/build/outputs/mapping/release/mapping.txt` with each release for readable crash stacks.

## 8. Pre-launch report and review

- [ ] Open **Pre-launch report** after your first closed/internal upload. It runs the app on real devices and flags crashes and accessibility issues.
- [ ] Fix anything in **Release > Policy status**. The first review of a new app can take several days.
- [ ] Re-check **Android vitals** after launch (ANR/crash rate, wake-lock usage).

## 9. After launch

- [ ] Reply to reviews; read crash reports in Android vitals.
- [ ] Every August check the new target-API deadline, bump `compileSdk`/`targetSdk`, and re-test alarms (alarm and foreground-service behaviour changes almost every Android release).
- [ ] Never ship an update without raising `versionCode` and re-running the test checklist from the README.
