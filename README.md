# Volume Overlay

A floating +/- button that adjusts media volume. Drag it anywhere on screen. It keeps running
after you close the app (via a foreground service) and can only be hidden through the switch
inside the app.

## Getting the APK

1. Push/merge this repo to the `main` branch on GitHub.
2. Go to the **Actions** tab of the repo -> **Build APK** workflow -> wait for the green check.
3. Open the finished run, scroll to **Artifacts**, download `volume-overlay-apk.zip`.
4. Unzip it on your phone (or unzip on PC and transfer) to get `app-debug.apk`.

## Installing on your phone (Oppo A5s / ColorOS, Android 8.1)

1. Open the APK file -> Android will ask to allow install from this source (Settings > install
   unknown apps) -> allow it -> Install.
2. Open the app, tap **Grant Overlay Permission**, allow "Display over other apps".
3. Turn on the **Show Floating Volume Button** switch. The +/- bubble appears; drag it anywhere.
4. Turn the switch off to hide it again (only place it can be hidden from).

### ColorOS-specific steps (important for it to survive being killed)

ColorOS aggressively kills background apps/services to save battery, more so than stock Android.
To keep the button alive long-term:

- Settings > Battery > select this app > set to "No restrictions" / disable battery optimization.
- Settings > App management > this app > enable "Auto-start" / "Allow background running".
- Recents screen > swipe down (or long-press the app card) > tap the lock icon so it isn't
  cleared automatically when you clear all recent apps.

Without these, ColorOS may still kill the service occasionally regardless of what the app itself
does (this is a phone-manufacturer restriction, not something an app can fully override).
