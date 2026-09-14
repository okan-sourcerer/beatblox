/*
 * bridge.js — the JS half of the Kotlin <-> Strudel bridge.
 *
 * Everything that touches Strudel lives here, in the same page as Strudel
 * itself. Kotlin only ever calls the coarse entry points on `StrudelBridge`
 * (set a pattern, start/stop, preview) and receives small event callbacks on
 * `window.Android` (a JavascriptInterface). Nothing high-frequency crosses
 * the bridge: highlight updates are diffed and throttled here.
 */
(function () {
  'use strict';

  // --- outbound: JS -> Kotlin --------------------------------------------
  // When running in a desktop browser (for debugging) there is no Android
  // object, so fall back to console logging.
  const native = (name, ...args) => {
    const host = window.Android;
    if (host && typeof host[name] === 'function') {
      try { host[name](...args); } catch (e) { console.error('bridge->native', name, e); }
    } else {
      console.log('[native]', name, ...args);
    }
  };

  const logEl = document.getElementById('log');
  const log = (msg, type = 'info') => {
    if (logEl) {
      logEl.textContent = (msg + '\n' + logEl.textContent).slice(0, 4000);
    }
    native('onLog', String(msg), type);
  };

  // Strudel's own logger dispatches DOM events; forward them.
  document.addEventListener('strudel.log', (e) => {
    const { message, type } = e.detail || {};
    if (type === 'error') native('onError', String(message));
    else native('onLog', String(message), String(type || 'info'));
    if (logEl) logEl.textContent = (message + '\n' + logEl.textContent).slice(0, 4000);
  });

  window.addEventListener('error', (e) => native('onError', String(e.message)));
  window.addEventListener('unhandledrejection', (e) =>
    native('onError', String(e.reason && e.reason.message ? e.reason.message : e.reason)));

  // --- state ---------------------------------------------------------------
  let repl = null;
  let ready = false;
  const ds = 'https://raw.githubusercontent.com/felixroos/dough-samples/main/';

  /**
   * Location table for the current pattern: [{id, start, end}] where start is
   * the char offset of the opening quote of a source node's mini-notation
   * string in the code we evaluated, and end the offset of the closing quote.
   * Hap locations (hap.context.locations) are absolute offsets into the same
   * code, so mapping a hap to a node is a range lookup.
   */
  let locTable = [];

  /** Haps that are (or will shortly be) sounding: {id, from, to, begin, end} in audio-clock seconds. */
  let liveHaps = [];
  let lastActiveKey = '';
  let highlightTimer = null;

  const nodeForLocation = (loc) => {
    for (const entry of locTable) {
      if (loc.start >= entry.start && loc.end <= entry.end + 1) {
        return { id: entry.id, from: loc.start - entry.start - 1, to: loc.end - entry.start - 1 };
      }
    }
    return null;
  };

  // Called for every hap that the scheduler triggers on the main transport.
  // `t` is the absolute audio-context onset time; duration is in seconds.
  const onHapTriggered = (hap, currentTime, cps, t) => {
    const locs = hap.context && hap.context.locations;
    if (!locs || !locs.length || !hap.whole) return;
    // Not hap.duration: the default output runs before this tap and writes
    // value.duration in *seconds*, which hap.duration then returns as if it
    // were cycles. The whole timespan is untouched.
    const duration = hap.whole.end.sub(hap.whole.begin).valueOf() / cps;
    for (const loc of locs) {
      const mapped = nodeForLocation(loc);
      if (mapped) liveHaps.push({ ...mapped, begin: t, end: t + Math.max(duration, 0.05) });
    }
  };

  // ~30Hz: compute the set of currently sounding tokens and push to Kotlin only
  // when it changes. Payload stays tiny: node ids + token ranges.
  const tickHighlight = () => {
    if (!repl) return;
    const now = strudel.getAudioContext().currentTime;
    liveHaps = liveHaps.filter((h) => h.end > now);
    const active = liveHaps.filter((h) => h.begin <= now);
    const items = active.map((h) => h.id + ':' + h.from + ':' + h.to);
    const key = Array.from(new Set(items)).sort().join(',');
    if (key !== lastActiveKey) {
      lastActiveKey = key;
      native('onActive', key);
    }
  };

  const startHighlightLoop = () => {
    if (highlightTimer) return;
    highlightTimer = setInterval(tickHighlight, 33);
  };
  const stopHighlightLoop = () => {
    if (highlightTimer) clearInterval(highlightTimer);
    highlightTimer = null;
    liveHaps = [];
    if (lastActiveKey !== '') {
      lastActiveKey = '';
      native('onActive', '');
    }
  };

  // --- sounds ------------------------------------------------------------
  const collectSounds = () => {
    const map = strudel.soundMap.get();
    const out = [];
    for (const name of Object.keys(map)) {
      if (name.startsWith('_')) continue;
      const data = map[name].data || {};
      let count = 1;
      if (Array.isArray(data.samples)) count = data.samples.length;
      else if (data.samples && typeof data.samples === 'object') count = Object.keys(data.samples).length;
      out.push({ name, type: data.type || 'sample', count, tag: data.tag || '' });
    }
    out.sort((a, b) => a.name.localeCompare(b.name));
    return out;
  };

  // --- init ----------------------------------------------------------------
  async function init() {
    log('[bridge] initStrudel');
    try {
      repl = await strudel.initStrudel({
        prebake: async () => {
          // The same default banks the strudel.cc REPL prebakes.
          const banks = ['tidal-drum-machines', 'piano', 'Dirt-Samples', 'EmuSP12', 'vcsl', 'mridangam'];
          await Promise.allSettled(banks.map((b) => strudel.samples(ds + b + '.json')));
        },
        onToggle: (started) => {
          if (started) startHighlightLoop(); else stopHighlightLoop();
          native('onToggle', !!started);
        },
        onEvalError: (err) => native('onError', String(err && err.message ? err.message : err)),
        // Attach the highlight tap to every pattern that goes through the
        // main transport. dominant=false keeps the webaudio output running.
        editPattern: (pat) => pat.onTrigger(onHapTriggered, false),
      });
      // Worklets + resume the context. The WebView is configured to not need a
      // user gesture for this.
      await strudel.initAudio();
      ready = true;
      log('[bridge] ready');
      native('onReady');
      native('onSounds', JSON.stringify(collectSounds()));
    } catch (err) {
      console.error(err);
      native('onError', 'init failed: ' + (err && err.message ? err.message : err));
    }
  }

  // --- sample preloading -----------------------------------------------------
  // superdough fetches a sample the first time it is triggered and drops that
  // first event ("took too long"). Warm the cache for everything a pattern
  // will use before it starts, so the first press plays.
  let loadingCount = 0;
  const setLoading = (delta) => {
    const was = loadingCount > 0;
    loadingCount = Math.max(0, loadingCount + delta);
    const now = loadingCount > 0;
    if (was !== now) native('onLoading', now);
  };

  async function preloadPattern(pat, cycles) {
    let haps;
    try {
      haps = pat.queryArc(0, cycles).filter((h) => h.hasOnset());
    } catch (e) {
      return 0;
    }
    const seen = new Set();
    const jobs = [];
    for (const hap of haps) {
      hap.ensureObjectValue();
      const v = hap.value;
      if (!v || typeof v.s !== 'string') continue;
      const key = (v.bank ? v.bank + '_' : '') + v.s;
      const sound = strudel.getSound(key);
      if (!sound || !sound.data || sound.data.type !== 'sample') continue;
      const id = key.toLowerCase() + ':' + (v.n || 0) + ':' + (v.note !== undefined ? v.note : '');
      if (seen.has(id)) continue;
      seen.add(id);
      if (jobs.length >= 64) break;
      jobs.push(strudel.getSampleBuffer(v, sound.data.samples).catch(() => {}));
    }
    if (!jobs.length) return 0;
    setLoading(+1);
    try {
      await Promise.all(jobs);
    } finally {
      setLoading(-1);
    }
    return jobs.length;
  }

  // --- inbound: Kotlin -> JS ----------------------------------------------
  const requireReady = () => {
    if (!ready) throw new Error('strudel not ready');
  };

  // Build a pattern from generated code without the transpiler. All strudel
  // functions are globals after evalScope and miniAllStrings() makes plain
  // strings parse as mini-notation, so this is enough for preview.
  const buildPattern = (code) => {
    // eslint-disable-next-line no-new-func
    const pat = new Function('return (' + code + ');')();
    if (!strudel.isPattern(pat)) throw new Error('code did not produce a pattern');
    return pat;
  };

  window.StrudelBridge = {
    /** Replace the transport pattern. locTableJson maps string offsets to node ids. */
    setPattern(code, locTableJson, autostart) {
      requireReady();
      locTable = locTableJson ? JSON.parse(locTableJson) : [];
      liveHaps = [];
      // Warm the sample cache first (built without the transpiler; cheap),
      // then evaluate for real. Eight cycles covers <a b c d> alternations.
      let warm = Promise.resolve();
      try { warm = preloadPattern(buildPattern(code), 8); } catch (e) { /* evaluate() will report it */ }
      return warm
        .then(() => repl.evaluate(code, !!autostart))
        .then(
          () => native('onPatternSet', code),
          (err) => native('onError', String(err && err.message ? err.message : err)),
        );
    },

    start() {
      requireReady();
      strudel.getAudioContext().resume();
      repl.start();
    },

    stop() {
      requireReady();
      repl.stop();
    },

    /** Stop and clear the pattern. */
    hush() {
      requireReady();
      repl.stop();
      repl.setPattern(strudel.silence, false);
      locTable = [];
      stopHighlightLoop();
    },

    setCpm(cpm) {
      requireReady();
      repl.scheduler.setCps(Number(cpm) / 60);
    },

    /**
     * One-shot preview of `code` for `cycles` cycles, independent of the main
     * transport. Haps are rendered directly through superdough at their
     * onset offsets from "now".
     */
    preview(code, cycles) {
      requireReady();
      const ctx = strudel.getAudioContext();
      ctx.resume();
      const cps = repl.scheduler.cps || 0.5;
      const n = Math.max(1, Number(cycles) || 1);
      let pat;
      try {
        pat = buildPattern(code);
      } catch (err) {
        native('onError', 'preview: ' + (err && err.message ? err.message : err));
        return 0;
      }
      const haps = pat
        .queryArc(0, n, { _cps: cps })
        .filter((h) => h.hasOnset())
        .sort((a, b) => a.whole.begin.valueOf() - b.whole.begin.valueOf());
      // Schedule only once the samples are in, so the first audition is heard.
      preloadPattern(pat, n).then(() => {
        const t0 = ctx.currentTime + 0.05;
        for (const hap of haps) {
          hap.ensureObjectValue();
          const onset = t0 + hap.whole.begin.valueOf() / cps;
          strudel.superdough(hap.value, onset, hap.duration / cps, cps, hap.whole.begin.valueOf());
        }
      });
      return haps.length;
    },

    isReady() { return ready; },

    sounds() { return JSON.stringify(collectSounds()); },
  };

  init();
})();
