# Football Radar

Football Radar is an Android application built with Kotlin, Jetpack Compose, and MVVM.

## Build

On Windows, build the debug APK with the Gradle Wrapper:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Build output and APK files are intentionally excluded from Git.

## GitHub Releases

The updater reads public GitHub Releases from `dperstnev38-dot/FootballRadar`. The repository can be changed using the Gradle property `githubReleasesRepository`.

To publish an app update:

1. Increase `versionCode` and update `versionName` in `app/build.gradle.kts`.
2. Build a release APK signed with the same signing key as the installed app.
3. Create a GitHub Release with a semantic version tag such as `v1.1.0`.
4. Attach the signed APK to the release. The updater selects the first `.apk` release asset.

Do not commit signing keys, passwords, generated APKs, or other credentials. Keep the release signing key secure; APKs signed with a different key cannot update an existing installation.
