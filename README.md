# Auto Lens

Launch the app, check accessibility access, close the app screen, **wait one second**, capture one screenshot, and **share it with Google Lens**.

A native Java app for Android 11 (API 30) and later. Auto Lens opens a compatible Google Lens image-sharing activity directly and requires no backend server.

Application ID and Java package: `works.unspec.autolens`.

## Getting started

1. Install the APK and open Auto Lens.
2. Read the accessibility disclosure and tap **Agree and open Settings**.
3. Enable the **Auto Lens** accessibility service in system settings, then return to the app. The first capture starts when you return.
4. Subsequent launches close the app screen and capture whatever is visible one second later, provided accessibility access is still enabled.

To capture another app, add the **Auto Lens** tile using the **Quick Settings editor**. Tap the tile while viewing the screen you want to capture. The Quick Settings panel closes before capture. Launching from the home screen may capture the home screen; Auto Lens does not switch to your previously used app.

On Android 13 and later, a sideloaded APK may require **Settings → Apps → Auto Lens → top-right menu → Allow restricted settings** before accessibility access can be enabled. Menu locations vary by device manufacturer.

## Permissions and behavior

- Uses the accessibility service `takeScreenshot()` API. Does not read the view hierarchy, perform touch gestures, or monitor accessibility events.
- Does not request storage, photo library, camera, internet, notification, or overlay permissions.
- Captures once per app or tile launch. Does not capture while the device is locked.
- Saves PNG screenshots in the private app cache. Before each save, removes images older than 24 hours and keeps at most five recent images, including the new screenshot. Cleanup does not run automatically at the 24-hour mark. Android may also clear the cache.
- Shares a `FileProvider` content URI with temporary read access. The screenshot is not deleted immediately after sharing because Lens may read it asynchronously.
- Images processed by Google Lens are subject to the Google privacy policy. Auto Lens itself has no network permission.
- Closes the app screen and removes its recent-apps entry. **The accessibility service remains connected to Android and idle**, so the next launch can reuse the permission. The app does not force-kill its process. Disable the service in accessibility settings to turn the feature off completely.

## Google Lens compatibility

1. Prefers an enabled, exported image-sharing activity in `com.google.ar.lens`.
2. Otherwise, looks for an exported Lens image-sharing activity in the Google app, `com.google.android.googlequicksearchbox`.
3. Shows an install/update prompt if no compatible receiver is available. Does not automatically share with unrelated apps.

Lens sharing activities can vary between installed Google app and Lens versions. The integration does not rely on a fixed public Lens SDK contract, so verify sharing on your device. Protected screens (`FLAG_SECURE`, DRM content, and some banking apps) may reject capture or appear black.

## Build and install

- JDK 17
- Android SDK Platform 35 and Build Tools 35.0.0
- Gradle Wrapper 8.11.1 and Android Gradle Plugin 8.9.2

Open this folder in Android Studio, or set `sdk.dir` in `local.properties` to your Android SDK location and run:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is intended for installation and development testing. Store distribution requires a separate release signing configuration. Google Play distribution of apps using accessibility APIs requires the relevant declaration and review. Auto Lens does not identify itself as a disability support tool.

A build with a different application ID is installed as a separate app. Accessibility access and Quick Settings tiles do not transfer automatically; enable the service and add the tile for the new installation.

## Verification

Automated tests cover the initial disclosure and settings navigation, the one-second delay, duplicate request prevention, cancellation, locked-device handling, timeout recovery, prevention of event-triggered capture, Lens receiver selection, URI permissions, cache cleanup, and provider path restrictions. Capture and activity tests run against simulated Android 11 and Android 15 environments.

Device verification checklist:

- First launch → enable accessibility access → return → capture and open Lens.
- Denied or revoked access → show setup without capturing.
- Quick Settings tile from another app → collapse the panel and share that screen.
- Repeated launches → no duplicate capture requests while a capture is in progress.
- Lens unavailable → show the install/update prompt.
- Rotation, lock screen, and protected content → handle errors or platform restrictions.
- Successful sharing → close the app screen and allow another launch.

## API references

- [AccessibilityService.takeScreenshot](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#takeScreenshot(int,java.util.concurrent.Executor,android.accessibilityservice.AccessibilityService.TakeScreenshotCallback))
- [MediaProjection and consent for each capture session](https://developer.android.com/media/grow/media-projection)
- [Secure file sharing](https://developer.android.com/training/secure-file-sharing/share-file)
- [Google Play accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491)
