# Strudel Mobile

Android (Kotlin/Compose) wrapper around [Strudel](https://strudel.cc) with a
block-based pattern editor instead of typed code. See
`strudel-mobile-wrapper-plan.md` for the full plan and design decisions.

## Layout

| Path | What |
|---|---|
| `app/src/main/assets/strudel/` | The web side: `strudel-web.js` (prebuilt `@strudel/web` bundle, AGPL-3.0), `bridge.js` (our JS↔Kotlin bridge), `index.html`. |
| `app/.../engine/` | `StrudelEngine` owns the WebView and is the only thing that talks to JS. `StrudelWebViewHost` keeps it attached (1dp, invisible). |
| `app/.../model/` | The pattern tree (`Chain` / `MiniSource` / `GroupSource` / `Transform`), the vocabulary (`Vocabulary`), `Serializer` (tree → code + location table), `TreeOps` (immutable edits). |
| `app/.../ui/` | Compose: transport bar, recursive `ChainEditor`, piano/degree picker, sound browser, params helper, code readout. `EditorViewModel` glues model ↔ engine. |
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

## Build

```
./gradlew :app:installDebug
./gradlew :app:testDebugUnitTest
```

Debugging the JS side in a desktop browser: `npx http-server app/src/main/assets/strudel -p 8765`
and open it — without `window.Android` the bridge logs callbacks to the console.
