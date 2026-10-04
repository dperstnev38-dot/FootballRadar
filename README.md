# Football Radar

Football Radar is an Android application built with Kotlin, Jetpack Compose, and MVVM.

## Build

On Windows, build the debug APK with the Gradle Wrapper:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Build output and APK files are intentionally excluded from Git.

## GitHub Releases

The updater reads only the latest public GitHub Release from `dperstnev38-dot/FootballRadar`, configured through the Gradle property `githubReleasesRepository`. It calls the unauthenticated GitHub Releases API and accepts APK assets only from that repository's `/releases/download/` URLs. No GitHub API token is used.

To publish an app update:

1. Increase both `appVersionCode` and `appVersionName` for the next release. These Gradle properties can be supplied on the command line; defaults are `1` and `1.0.0` for the first release.
2. Configure the release signing values in the ignored root `local.properties`: `releaseStoreFile=keys/football-radar-release.jks`, `releaseStorePassword`, `releaseKeyAlias=football-radar-release`, and `releaseKeyPassword`.
3. Build the signed release APK with `.\gradlew.bat :app:copyReleaseApk -PappVersionCode=1 -PappVersionName=1.0.0`. The copy task places `football-radar-v<versionName>-release.apk` under `app/build/outputs/apk/release/distribution/`.
4. Create a public GitHub Release with a matching semantic-version tag such as `v1.0.0` and attach the **signed** APK. The updater checks the latest release and selects its first `.apk` asset.

`versionCode` must increase for every published app update. Keep `versionName` aligned with the release tag (the optional leading `v` is ignored during comparison). Release APKs must always be signed with the same key; back up the keystore and its passwords securely. The ignored local properties file is not a substitute for an encrypted off-device backup.

The app checks automatically at most once per 24 hours and also offers a manual **Проверить обновления** action under **Settings → Updates**. A missing latest release is reported as HTTP 404 with an explanation; network, rate-limit, and other HTTP failures are reported without treating them as a successful check. Downloads stream byte progress, are written to the app cache, and are rejected if they are not a complete ZIP/APK or if their URL is not a public release asset from the configured repository. `FileProvider` grants the Android package installer temporary read access; installation always requires user confirmation.

## PitchAPI

The app uses PitchAPI for the current day's fixtures and requests match events and shot details only when a match is opened from its card. Configure the following in the ignored root `local.properties` file:

```properties
footballApiBaseUrl=https://api.pitchapi.dev/
footballApiToken=YOUR_PITCHAPI_KEY
```

Requests use the `X-API-KEY` header. The date endpoint is polled once per minute while the app is active. Match details, events, and shots are fetched only for matches the user chooses to load. Shots on target are counted from `shots[].is_on_target`; the `stats` endpoint's `shots_on_target` key is used only when shot-level counts are unavailable.

Do not commit `local.properties` or place API keys in source files, logs, or documentation. Keys embedded in an Android APK can be extracted; use a restricted key and rotate it as needed, or proxy requests through a trusted backend if the key must remain private.
