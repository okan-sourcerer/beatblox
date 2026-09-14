# BeatBlox (Strudel wrapper) — Project Plan

> **Status (2026-09-14): v1 is done.** Every phase below shipped (marked ✅), plus
> a few things the plan didn't ask for. What's left is in "Next" at the bottom.

## Context

I'm an Android developer (Kotlin/Compose, also work in Java/Spring Boot day-to-day, some Godot/C#). I want to build an Android app that wraps [Strudel](https://strudel.cc) — the browser-based live-coding music tool (a JS port of TidalCycles) — with a native Compose control layer, instead of making users type pattern code by hand.

**Core idea:** Strudel runs headless-ish inside a WebView (loaded via the `@strudel/web` embed package). A Kotlin-side data model represents the pattern as a tree of chained function calls. Compose UI edits that tree. Every edit gets serialized to a Strudel code string and pushed into the WebView via `evaluateJavascript`. Audio plays through the WebView's Web Audio context.

## Key decisions already made (don't relitigate these)

- **Strudel over alternatives (e.g. Glicol):** chose Strudel for its mature pattern vocabulary, sample ecosystem, and community depth. Licensing (Strudel is AGPL-3.0) is a known, accepted tradeoff — not a blocker, not up for debate.
- **WebView is required, not optional.** Strudel is JS + Web Audio; there's no way to run it without a browser engine. React Native was considered and rejected — it doesn't remove the WebView requirement, it just adds another JS runtime hop.
- **Background audio (app fully backgrounded / screen locked) is explicitly out of scope for now.** We're only targeting audio-while-app-is-foregrounded for v1. A foreground service + keeping the WebView attached (not detached/headless) is the known follow-up when we tackle backgrounding later — not needed yet.
- **Architecture for the WebView itself:** keep it attached in the Compose hierarchy (zero-size or hidden behind UI), not a fully detached/headless WebView instance — Android's WebView isn't reliable as a truly headless engine (no window/surface = flaky timers and rendering across OEMs).
- **Where the UI logic lives:** all Strudel-interacting logic (pattern building, sound triggering) should live as JS in the same page/context as Strudel itself — not spread across a Kotlin↔JS bridge for anything performance-sensitive. The bridge is for coarse actions (push a new pattern string, start/stop) and for receiving lightweight event callbacks (e.g. "node X just fired") — not for anything per-frame or high-frequency.
- **Pattern preview scope decision (tentative, revisit after building it):** when previewing an isolated note/node, preview it *alone* using its own ancestor chain, not including sibling layers from a surrounding `stack()`. Simpler and probably more useful; open to being wrong about this once it's actually built and used.

## What "done" looks like for v1

- Start/stop transport controls
- A block-based chaining UI (add/remove/reorder function-call blocks — `s`, `note`/`n`, `fast`, `gain`, `room`, `lpf`, `scale`, `stack`, sequences — rather than typing code)
- Easier note input via a piano-style picker instead of typing mini-notation
- Note/node preview: hear what a note sounds like in isolation (through its own chain) even when the main transport isn't playing
- Step highlighting: the currently-sounding block in the native UI lights up in sync with playback
- A curated subset of Strudel desktop's tooling ported to mobile — starting with a sample/sound browser and a params helper (sliders/dropdowns for common effect params) — not a full port of every desktop panel

## Phased build plan

### ✅ Phase 0 — Spike: prove the shell works
Single local HTML file bundling `@strudel/web` in a WebView. Confirm from Kotlin that `evaluateJavascript` can set and play a hardcoded pattern, and stop it. De-risks the architecture before investing in UI.

### ✅ Phase 1 — Transport
Wire Compose start/stop/hush buttons to the JS calls exposed by `@strudel/web`.

### ✅ Phase 2 — Pattern data model
A Kotlin tree: each node is a function call (name + params + children, to represent `stack()`/sequence groups). This tree is the single source of truth; it gets serialized to a Strudel code string for playback. Start with a small function vocabulary (`s`, `note`/`n`, `stack`, sequence, `.fast`/`.slow`, `.gain`, `.room`, `.lpf`, `.scale`) and expand later. Getting this model right is more important than any individual screen — chaining, note preview, and highlighting all depend on it.

### ✅ Phase 3 — Chaining UI
Vertical block-list over the tree: add/remove/reorder function-call blocks, edit params inline. Reordering regenerates the tree → code string → pushed to WebView.

### ✅ Phase 4 — Note input
Piano-style picker component that inserts a note token into the currently-selected chain node.

**→ Milestone: Phases 0-4 together are the first real demo (start/stop + block chaining + note picking, end to end). Build and use this before starting Phase 5/6 — the remaining phases have real design uncertainty that's easier to resolve once the basic loop is in hand.**

### ✅ Phase 5 — Isolated note/node preview
Take the selected leaf node plus its ancestor chain, serialize *that subtree alone* into a standalone one-shot expression, and trigger it via Strudel's one-shot playback (independent of whatever the main transport is doing). See the tentative sibling-exclusion decision above.

### ✅ Phase 6 — Step highlighting on native UI
Hook Strudel's event/hap scheduler for per-event callbacks. Bridge a minimal payload (which node ID is currently sounding — not audio data) back to Kotlin via a JS interface, throttled, and update Compose highlight state from that. This is the one spot where a chatty bridge could cause real jank — keep payloads tiny.

### ✅ Phase 7 — Desktop tool parity (selective)
Port the highest-value pieces of Strudel desktop's panel first: sound/sample browser, then a params helper bound to the selected node. Don't attempt full parity in v1.

### ✅ Beyond the plan (shipped alongside)

- Pattern persists across restarts; undo/redo; named pattern library (save / save as / load / rename / delete)
- Drag-to-reorder blocks and layers; bank-aware sound browser
- Vocabulary grew to ~50 blocks incl. function-valued args, chords (`chord` source + voicing/anchor/mode), variadic `layer()`
- Plain-language descriptions for sounds/banks/blocks, help sheet, pickers for every text parameter
- Samples preloaded before a pattern plays (fixes the dropped first press), loading indicator
- Share/export: code with `setcpm()` header, system share, `strudel.cc/#…` link
- About/credits (Strudel, TidalCycles, sample banks; AGPL notice), feedback form with on-device outbox → JSON POST to a configurable server
- Release build plumbing: `keystore.properties` signing, BuildConfig URLs, `LICENSE` (AGPL-3.0)

## Open questions — resolved

- **`@strudel/web` API surface:** `repl.evaluate` hot-swaps the pattern; `pattern.onTrigger(fn, false)` gives per-hap callbacks with `hap.context.locations`; `superdough(value, onset, dur, cps, begin)` does one-shot playback. All in `bridge.js`.
- **Vocabulary size:** the initial handful was enough for the demo; it grew to ~50 blocks once the model proved itself.
- **Preview concurrent with transport:** works without glitching — preview renders straight through `superdough` on the same AudioContext, independent of the scheduler.
- **Sibling exclusion in preview (the tentative one):** kept. Not revisited yet with real use — see "Next".

## Next (when there's time)

1. **Use it.** Play with it on the phone for a while and fix what annoys. Specifically decide whether sibling-excluded preview is right.
2. **Hosting:** set `strudel.downloadUrl` / `strudel.hubKey`, generate the release key, publish APK + source (AGPL requires the source to be offered).
3. ✅ **Feedback server:** integrated with the Coreworkbench hub (`POST /api/feedback`, per-app key via `strudel.hubKey`). Register the app at `/admin/apps`, add `projects/beatblox.yaml` in the hub repo, send one test report from a dev build.
4. **Tests:** only `SerializerTest` exists. `TreeOps` (edits, reorder, undo) and the bridge's location-table → chainId mapping are the two most likely silent regressions.
5. **Background audio** — the deferred v1 follow-up: foreground service + keep the WebView attached. Biggest remaining user-facing gap; OEM-fiddly (Honor especially), so do it last.
6. **Import:** the app can export code but not read it back. A mini parser for the app's own output subset would let patterns move between users/devices.
