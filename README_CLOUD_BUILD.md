# Building without Android Studio (GitHub Actions)

For a slow PC. GitHub builds the app on its own servers (free GitHub accounts include free build minutes; check
GitHub's current limits for private repositories). You only need a browser, a phone and Git.

## One-time setup

1. Create a GitHub account and a **new private repository** (for example `wakeup-alarm`). Don't add a README there.
2. In the project folder (the unzipped `WakeUp` folder) open a terminal, for example VS Code > Terminal > New Terminal:
   ```
   git init
   git add .
   git commit -m "WakeUp first version"
   git branch -M main
   git remote add origin https://github.com/YOUR_NAME/wakeup-alarm.git
   git push -u origin main
   ```
   GitHub asks you to sign in (a browser window opens).
3. Open the repository on github.com > **Actions** tab. The "Build WakeUp" run starts by itself. The first run takes
   about 5 to 10 minutes.

## Getting the APK onto your phone

1. When the run shows a green tick, open it and download the artifact **WakeUp-debug-apk** (a zip) at the bottom.
2. Unzip it, send `app-debug.apk` to your phone (USB cable, WhatsApp to yourself, Drive...).
3. On the phone open the file and allow "Install unknown apps" when asked. Test with the checklist in `README.md`.

## When the build fails (red cross)

Open the failed run > the failed step > copy the red error lines and send them to Claude. Fix, `git add .`,
`git commit -m "fix"`, `git push`, and the build runs again.

## Signed release bundle for Google Play (later)

1. Make the upload key once on any PC with Java (keep the file and passwords safe):
   `keytool -genkeypair -v -keystore upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. Convert it to text. Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-key.jks")) | Set-Clipboard`
3. In the GitHub repository: **Settings > Secrets and variables > Actions > New repository secret**. Create four:
   `KEYSTORE_BASE64` (paste the text), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (for example `upload`), `KEY_PASSWORD`.
4. Run the workflow again (Actions > Build WakeUp > Run workflow). The artifact **WakeUp-release** then contains the
   signed `.aab` for Play Console, plus `mapping.txt`.

Never put the keystore file or the passwords into the repository itself.
