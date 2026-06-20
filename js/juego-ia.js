/* =============================================================
   juego-ia.js — Rival automático para "Práctica vs IA"
   Dificultad seleccionable + modo "randomiza" vs "sigue reglas".
   Opera sobre el lado 'B' usando la API del motor (global.JUEGO).
   ============================================================= */
(function (global) {
  'use strict';

  const DIFICULTAD = Object.freeze({ FACIL: 'facil', MEDIO: 'medio', DIFICIL: 'dificil' });
  const config = { dificultad: DIFICULTAD.MEDIO, modo: 'reglas' /* 'reglas' | 'random' */ };
  function configurar(opts) { Object.assign(config, opts || {}); }

  function enJuego(L) { return (L.activo ? [L.activo] : []).concat(L.banca); }

  // Juega el turno completo del lado 'B' y lo termina.
  function jugarTurno(est) {
    const J = global.JUEGO; if (!J) return est;
    const lado = 'B';
    if (est.turnoDe !== lado || est.ganador || est.fase !== J.FASE.MAIN) return est;
    const L = est.lados[lado];
    const aleatorio = config.modo === 'random' || config.dificultad === DIFICULTAD.FACIL;

    // 1) Poner Básicos en la banca.
    L.mano.filter(function (c) { return c.supertipo === 'Pokemon' && c.esBasico; }).slice()
      .forEach(function (c) { if (L.banca.length < 5) J.ponerEnBanca(est, lado, c.iid); });

    // 2) Evolucionar lo que se pueda.
    L.mano.filter(function (c) { return c.supertipo === 'Pokemon' && c.evolucionaDe; }).slice()
      .forEach(function (evo) {
        const t = enJuego(L).filter(function (x) { return x.nombre === evo.evolucionaDe && est.turno > (x.enJuegoDesde || 0); })[0];
        if (t) J.evolucionar(est, lado, evo.iid, t.iid);
      });

    // 3) Adjuntar 1 energía (al Activo).
    if (!L.energiaUsada) {
      const e = L.mano.find(function (c) { return c.supertipo === 'Energy'; });
      const tgt = L.activo || L.banca[0];
      if (e && tgt) J.adjuntarEnergia(est, lado, e.iid, tgt.iid);
    }

    // 4) Jugar Entrenadores de robar (seguros).
    if (!aleatorio) {
      L.mano.filter(function (c) { return c.supertipo === 'Trainer' && /draw/i.test(c.texto || ''); }).slice()
        .forEach(function (c) { J.jugarEntrenador(est, lado, c.iid); });
    }

    // 5) Atacar (si puede). Atacar termina el turno automáticamente.
    if (J.puedeAtacar(est)) {
      const at = L.activo, def = est.lados.A.activo;
      const payable = (at.ataques || []).map(function (a, i) { return { a: a, i: i }; })
        .filter(function (x) { return J.puedePagar(at, x.a.coste); });
      if (payable.length) {
        let pick;
        if (aleatorio) {
          pick = payable[Math.floor(Math.random() * payable.length)];
        } else {
          payable.sort(function (x, y) { return J.danioEfectivo(at, def, y.a) - J.danioEfectivo(at, def, x.a); });
          const rem = def ? (def.hp - (def.danio || 0)) : 0;
          const ko = payable.find(function (x) { return def && J.danioEfectivo(at, def, x.a) >= rem; });
          pick = (config.dificultad === DIFICULTAD.DIFICIL && ko) ? ko : payable[0];
        }
        J.atacar(est, lado, pick.i);
        return est;
      }
    }
    J.terminarTurno(est);
    return est;
  }

  const API = { DIFICULTAD, config, configurar, jugarTurno, siguienteAccion: function () { return null; } };
  global.JUEGO_IA = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
