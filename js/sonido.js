/* =============================================================
   sonido.js — Efectos de sonido + háptica del juego virtual
   Síntesis con WebAudio (sin archivos). API: SONIDO.play('attack'),
   SONIDO.vibrate(ms), SONIDO.setMute(b), SONIDO.isMuted().
   ============================================================= */
(function (global) {
  'use strict';

  const MUTE_KEY = 'jv_mute';
  let muted = false;
  try { muted = localStorage.getItem(MUTE_KEY) === '1'; } catch (e) {}
  let ctx = null;

  function ac() {
    if (muted) return null;
    try {
      if (!ctx) ctx = new (global.AudioContext || global.webkitAudioContext)();
      if (ctx.state === 'suspended') ctx.resume();
      return ctx;
    } catch (e) { return null; }
  }

  // Un tono simple con envolvente.
  function tono(freq, dur, tipo, vol, t0) {
    const c = ac(); if (!c) return;
    const t = t0 || c.currentTime;
    const o = c.createOscillator(), g = c.createGain();
    o.type = tipo || 'sine'; o.frequency.setValueAtTime(freq, t);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol || 0.18, t + 0.012);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(c.destination);
    o.start(t); o.stop(t + dur + 0.02);
  }
  // Barrido de frecuencia (zaps, KO).
  function barrido(f1, f2, dur, tipo, vol) {
    const c = ac(); if (!c) return;
    const t = c.currentTime;
    const o = c.createOscillator(), g = c.createGain();
    o.type = tipo || 'sawtooth';
    o.frequency.setValueAtTime(f1, t);
    o.frequency.exponentialRampToValueAtTime(Math.max(40, f2), t + dur);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol || 0.2, t + 0.015);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(c.destination);
    o.start(t); o.stop(t + dur + 0.02);
  }
  // Ráfaga de ruido (impactos).
  function ruido(dur, vol) {
    const c = ac(); if (!c) return;
    const n = Math.floor(c.sampleRate * dur);
    const buf = c.createBuffer(1, n, c.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / n);
    const src = c.createBufferSource(); src.buffer = buf;
    const g = c.createGain(); g.gain.value = vol || 0.25;
    const f = c.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 1800;
    src.connect(f); f.connect(g); g.connect(c.destination); src.start();
  }
  function arpa(freqs, paso, dur, tipo, vol) {
    const c = ac(); if (!c) return;
    freqs.forEach(function (f, i) { tono(f, dur, tipo || 'triangle', vol || 0.16, c.currentTime + i * paso); });
  }
  // Whoosh: ráfaga de ruido con filtro paso-banda que barre (embestida).
  function whoosh(f1, f2, dur, vol) {
    const c = ac(); if (!c) return; const t = c.currentTime;
    const n = Math.floor(c.sampleRate * dur);
    const buf = c.createBuffer(1, n, c.sampleRate); const d = buf.getChannelData(0);
    for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1);
    const src = c.createBufferSource(); src.buffer = buf;
    const f = c.createBiquadFilter(); f.type = 'bandpass'; f.Q.value = 0.8;
    f.frequency.setValueAtTime(f1, t); f.frequency.exponentialRampToValueAtTime(Math.max(60, f2), t + dur);
    const g = c.createGain(); g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol || 0.18, t + dur * 0.3);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    src.connect(f); f.connect(g); g.connect(c.destination); src.start();
  }

  const EFECTOS = {
    button: function () { tono(420, 0.06, 'square', 0.08); },
    select: function () { tono(560, 0.045, 'sine', 0.07); },
    draw: function () { whoosh(900, 2200, 0.12, 0.1); tono(660, 0.06, 'triangle', 0.08); },
    energy: function () { barrido(300, 720, 0.18, 'sine', 0.16); },
    evolve: function () { arpa([440, 660, 880, 1100], 0.06, 0.2, 'triangle', 0.15); },
    attack: function () { whoosh(1400, 300, 0.2, 0.18); barrido(720, 160, 0.18, 'sawtooth', 0.16); },
    hit: function () { ruido(0.16, 0.3); tono(110, 0.12, 'square', 0.13); },
    ko: function () { barrido(300, 50, 0.5, 'sawtooth', 0.25); ruido(0.32, 0.26); tono(80, 0.4, 'sine', 0.12); },
    coin: function () { tono(900, 0.05, 'square', 0.1); tono(1200, 0.05, 'square', 0.09, (ctx && ctx.currentTime || 0) + 0.07); },
    prize: function () { arpa([700, 950, 1180], 0.06, 0.14, 'triangle', 0.16); },
    turn: function () { arpa([523, 784, 1047], 0.085, 0.24, 'triangle', 0.17); },
    win: function () { arpa([523, 659, 784, 1047], 0.1, 0.3, 'triangle', 0.2); },
    lose: function () { arpa([440, 349, 262], 0.12, 0.35, 'sawtooth', 0.18); }
  };

  function play(name) { if (muted) return; const f = EFECTOS[name]; if (f) try { f(); } catch (e) {} }
  function vibrate(ms) { if (muted) return; try { if (navigator.vibrate) navigator.vibrate(ms || 10); } catch (e) {} }
  function isMuted() { return muted; }
  function setMute(b) { muted = !!b; try { localStorage.setItem(MUTE_KEY, muted ? '1' : '0'); } catch (e) {} }

  const API = { play, vibrate, isMuted, setMute };
  global.SONIDO = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof self !== 'undefined' ? self : this);
