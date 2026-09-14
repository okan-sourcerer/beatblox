# BeatBlox

Android (Kotlin/Compose) wrapper around [Strudel](https://strudel.cc) with a
block-based pattern editor instead of typed code. See
`plan.md` for the full plan and design decisions.

## Layout

| Path | What |
|---|---|
| `app/src/main/assets/strudel/` | The web side: `strudel-web.js` (prebuilt `@strudel/web` bundle, AGPL-3.0), `bridge.js` (our JS↔Kotlin bridge), `index.html`. |
| `app/.../engine/` | `StrudelEngine` owns the WebView and is the only thing that talks to JS. `StrudelWebViewHost` keeps it attached (1dp, invisible). |
| `app/.../model/` | The pattern tree (`Chain` / `MiniSource` / `GroupSource` / `Transform`), the vocabulary (`Vocabulary`), `Serializer` (tree → code + location table), `TreeOps` (immutable edits). |
| `app/.../ui/` | Compose: transport bar, recursive `ChainEditor`, piano/degree picker, sound browser, params helper, code readout. `EditorViewModel` glues model ↔ engine. |
| `app/.../feedback/` | `FeedbackClient`: on-device outbox + `POST /api/feedback` to the Coreworkbench hub. |
| `web/` | npm project used only to fetch/update the Strudel bundle: `npm install && npm run sync`. |

## How it works

- Every edit produces a new immutable tree → `Serializer` emits Strudel code
  plus a table of `{chainId, start, end}` char offsets for each mini-notation
  string → `StrudelBridge.setPattern(code, table, autostart)` hot-swaps the
  pattern (`repl.evaluate`).
- **Step highlighting**: the bridge taps every hap via `pattern.onTrigger(fn, false)`,
  maps `hap.context.locations` (absolute offsets, same as the desktop REPL's
  highlighter) back to `chainId` + token range using the table, and posts only
  *diffs* of the active set to Kotlin at ≤30 Hz as a tiny string
  (`"id:from:to,..."`).
- **Preview** (piano key / ▶ on a block): `Serializer.serializePreview` emits the
  target chain plus its ancestors' transforms (siblings excluded), and the bridge
  renders one cycle directly through `superdough(...)` — independent of the
  transport.
- The page is served via `WebViewAssetLoader` at `https://appassets.androidplatform.net/`
  so it is a secure context (AudioWorklets) and can fetch sample banks from
  GitHub (`dough-samples`). `mediaPlaybackRequiresUserGesture = false` lets the
  native Play button start audio.

- **Share / export** (⋮ → Share pattern, or the buttons on the Code tab):
  `Share.exportCode` prepends `setcpm(...)` so the code plays at the same tempo
  on strudel.cc; `Share.strudelUrl` builds a `https://strudel.cc/#<base64>` link
  the REPL opens directly.
- **Feedback** (⋮ → Send feedback): reports go to the Coreworkbench hub
  (`POST {HUB_URL}/api/feedback`, `Authorization: Bearer <per-app key>`; field
  contract in `docs/integration.md`, not tracked). Each report is queued in
  SharedPreferences first, then `FeedbackClient.flush()` delivers the queue
  (on submit and at app start); every report carries its own
  `idempotency_key`, so retries can't duplicate. 2xx = delivered, 401/422 =
  dropped (would never succeed), anything else = kept for retry. With
  `beatblox.hubKey` empty nothing leaves the device and the UI says so.
  Sent: type, message, title, optional email, per-install `user_id`,
  app/OS/device/locale/timezone/screen, the engine log (≤200 lines) and
  `metadata{pattern, pattern_name, cpm, strudel_version, app_version_code}`.
  `environment` is `dev` for debug builds and `prod` for release.

## Build

```
./gradlew :app:installDebug
./gradlew :app:testDebugUnitTest
```

### Release build

1. Create a signing key once and keep it safe — updates must be signed with the same key:
   `keytool -genkeypair -v -keystore strudel-release.jks -alias strudel -keyalg RSA -keysize 4096 -validity 10000`
2. Put `keystore.properties` in the repo root (git-ignored):
   ```
   storeFile=strudel-release.jks
   storePassword=...
   keyAlias=strudel
   keyPassword=...
   ```
3. Set the deployment URLs (in `gradle.properties`, `~/.gradle/gradle.properties`, or `-P`):
   `beatblox.hubKey` (the hub's per-app API key — keep it in
   `~/.gradle/gradle.properties`, not in the repo), `beatblox.sourceUrl`,
   `beatblox.downloadUrl`. Empty URLs hide the corresponding link; an empty key
   keeps feedback queued on the device.
4. `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
   Bump `versionCode`/`versionName` in `app/build.gradle.kts` for every upload.

## License

Copyright (C) 2026 Okan Tanrıverdi (okan-sourcerer). AGPL-3.0-or-later — see `LICENSE`
(full text) and `NOTICE` (copyright + third-party credits). The app bundles
Strudel, which is AGPL, so the app is too. Hosting the APK for download means
also offering the source; point `beatblox.sourceUrl` at this repository.

Debugging the JS side in a desktop browser: `npx http-server app/src/main/assets/strudel -p 8765`
and open it — without `window.Android` the bridge logs callbacks to the console.
