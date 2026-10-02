# aimap for Android

The Android app for [aimap](https://github.com/jaxkodex/aimap): the home screen
("the Bay") shows what the mail agent decided about each new message. It signs
in with Google through Firebase Authentication and reads `GET /home` from the
aimap API.

## Setup

Two files stay out of git. The build fails with a message until both exist.

1. **`app/google-services.json`**: the Firebase config for the Android app
   `pe.net.libre.aimap_client`. Download it from the Firebase console (Project
   settings, Your apps), or with the Firebase CLI:

   ```sh
   firebase apps:sdkconfig ANDROID <app-id> --project <project-id> > app/google-services.json
   ```

   Google sign-in only works for builds whose signing key is registered on
   that app. Add your key's SHA-1 and SHA-256 in the Firebase console. For the
   debug key, `./gradlew signingReport` prints them.

2. **`local.properties`**: Android Studio writes `sdk.dir`. Add the API's
   address:

   ```properties
   aimap.apiBaseUrl=https://your-aimap-api.example.com
   ```

   In CI, set `AIMAP_API_BASE_URL` instead.

The API only answers Firebase users whose email is in its
`AIMAP_ALLOWED_EMAILS`. Anyone else gets a 403, and the app says so.

## Build and test

```sh
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:installDebug        # build and install on a connected device
```

## Home layout

The Bay draws one of two layouts. v4 is the rack of strips and the default. v5 is the
queue layout, still a placeholder. Tap the avatar and pick one under "Home layout"; the
app writes the choice to a Jetpack DataStore preference, so it survives a restart.

## Fonts

Sofia Sans, Sofia Sans Condensed and Azeret Mono are under the SIL Open Font
License 1.1; see `licenses/fonts-OFL.txt`. The icons are from
[Lucide](https://lucide.dev) (ISC).
