# aimap for Android

The Android app for [aimap](https://github.com/jaxkodex/aimap): the home screen
("the Bay") shows what the mail agent decided about each new message. It signs
in with Google through Firebase Authentication and reads `GET /home` from the
aimap API.

Tapping a strip opens the message: its strip pinned over the letter, read from
`GET /messages/{id}` and `GET /messages/{id}/body`. System Back returns to the
Bay where you left it. The two keys on the foot, Handled and Later, write
aimap's own state; the mailbox is never touched.

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

## Handled, Later and Undo

The keys on a strip and the two on the read screen's foot post to
`POST /messages/{id}/actions`. Handled takes the message off the Bay and counts
it down on its plate; Later keeps it in its section but sends it to the end,
marked LATER; Undo puts it back. Nothing waits for the API: the Bay moves
first, and a call that fails puts it back and says why on the error bar. A
Handled leaves a "Handled · Undo" bar at the top for about five seconds.

## Draft a reply

A letter that wants an answer carries a third key above Handled and Later. The
service decides which ones do and sends a `reply` object with the message; an
older service without it leaves the app guessing from the labels (the reply
bucket, or `act_now` on a letter no list sent).

The key asks for `POST /messages/{id}/draft` and `GET /messages/{id}/thread`
together. The sheet shows the draft, how many letters went into it and one line
per letter behind it, so you can see what the model read. The instructions
field rewrites it, Copy takes the body, and "Open in mail app" hands subject
and body to an `ACTION_SENDTO` intent. A thread call that fails costs the list
of earlier letters and nothing else. aimap sends nothing itself: the mailbox is
never touched.

`Can go` and the per-pile Archive keys have no API yet. They read as keys but
do nothing, and TalkBack says "Coming soon".

Debug builds carry two paths that need no account: "Open the sample bay" under
the sign-in plate, and "Open the sample message" in the avatar sheet. Acting on
the sample bay's second card always fails, which is how the rollback gets
tried.

## Home layout

The Bay draws one of two layouts. v4 is the rack of strips and the default. v5 is the
queue: one line of work, the message in hand opened out with its keys, everything else
waiting in order, and the Can go piles as the last step. Tap the avatar and pick one under
"Home layout"; the app writes the choice to a Jetpack DataStore preference, so it survives
a restart.

## Fonts

Sofia Sans, Sofia Sans Condensed and Azeret Mono are under the SIL Open Font
License 1.1; see `licenses/fonts-OFL.txt`. The icons are from
[Lucide](https://lucide.dev) (ISC).
