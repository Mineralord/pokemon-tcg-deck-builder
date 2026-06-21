/* =============================================================
   efectos-pasivos.js — Capa de pasivos y stats derivadas (Fase 3)
   Recolecta los modificadores SIEMPRE ACTIVOS del tablero (habilidades pasivas,
   Pokémon Tools, Estadios y energías especiales) declarados como `pasivos` en el
   DSL (data/efectos-db.js) y los aplica al leer stats efectivas: HP, costo de
   retirada, daño entrante e inmunidad a estados.
   Si no hay EFECTOS_DB (p.ej. tests del motor base) -> todo queda neutro
   (sin regresión). Spec de mods: docs/efectos-dsl.md §7.
   ============================================================= */
(function (global) {
  'use strict';

  function rival(l) { return l === 'A' ? 'B' : 'A'; }
  function idsLado(L) { return (L.activo ? [L.activo.iid] : []).concat((L.banca || []).map(function (c) { return c.iid; })); }

  // Conjunto de iids al que apunta un pasivo `a:` relativo a su fuente.
  function setObjetivo(est, fLado, fCard, a) {
    const L = est.lados[fLado], O = est.lados[rival(fLado)];
    switch (a || 'esteP') {
      case 'esteP': return fCard ? [fCard.iid] : [];
      case 'propioActivo': return L.activo ? [L.activo.iid] : [];
      case 'propiaBanca': return (L.banca || []).map(function (c) { return c.iid; });
      case 'propioTodos': return idsLado(L);
      case 'rivalActivo': return O.activo ? [O.activo.iid] : [];
      case 'rivalBanca': return (O.banca || []).map(function (c) { return c.iid; });
      case 'rivalTodos': return idsLado(O);
      case 'todos': return idsLado(L).concat(idsLado(O));
      default: return [];
    }
  }

  function pasivosDeCarta(id) {
    const DB = global.EFECTOS_DB; if (!DB || !id) return [];
    const def = DB[id]; if (!def) return [];
    let out = [];
    if (Array.isArray(def.pasivos)) out = out.concat(def.pasivos);
    if (def.habilidades) Object.keys(def.habilidades).forEach(function (n) {
      const h = def.habilidades[n]; if (h && Array.isArray(h.pasivos)) out = out.concat(h.pasivos);
    });
    return out;
  }

  // Recorre todas las fuentes de pasivos del tablero: cb(fLado, fCard, pasivos[]).
  function forEachFuente(est, cb) {
    if (!global.EFECTOS_DB) return;
    ['A', 'B'].forEach(function (lado) {
      const L = est.lados[lado]; if (!L) return;
      const enJuego = (L.activo ? [L.activo] : []).concat(L.banca || []);
      enJuego.forEach(function (poke) {
        const ps = pasivosDeCarta(poke.id);
        if (ps.length) cb(lado, poke, ps);
        (poke.tools || []).forEach(function (t) { const tp = pasivosDeCarta(t.id); if (tp.length) cb(lado, poke, tp); });
        (poke.energias || []).forEach(function (e) { const ep = pasivosDeCarta(e.id); if (ep.length) cb(lado, poke, ep); });
      });
      if (L.estadio) { const sp = pasivosDeCarta(L.estadio.id); if (sp.length) cb(lado, null, sp); }
    });
  }

  // Modificadores agregados que afectan a la carta `iid`.
  function modsCarta(est, iid) {
    const acc = { reduceDanio: 0, aumentaDanio: 0, hpExtra: 0, retiroSet: null, inmune: false, bloqueaHab: false };
    if (iid == null) return acc;
    forEachFuente(est, function (fLado, fCard, pasivos) {
      pasivos.forEach(function (p) {
        if (setObjetivo(est, fLado, fCard, p.a).indexOf(iid) < 0) return;
        switch (p.mod) {
          case 'reduceDanio': acc.reduceDanio += (p.cantidad || 0); break;
          case 'aumentaDanio': acc.aumentaDanio += (p.cantidad || 0); break;
          case 'hpExtra': acc.hpExtra += (p.cantidad || 0); break;
          case 'costoRetiro': if (p.set != null) acc.retiroSet = p.set; break;
          case 'inmuneEstado': acc.inmune = true; break;
          case 'bloqueaHabilidad': acc.bloqueaHab = true; break;
        }
      });
    });
    return acc;
  }

  // ---------- Stats efectivas ----------
  function hpEf(est, card) { return (card ? card.hp : 0) + modsCarta(est, card && card.iid).hpExtra; }
  function retiroEf(est, card) {
    if (!card) return 0; const m = modsCarta(est, card.iid);
    return m.retiroSet != null ? m.retiroSet : (card.retirada || 0);
  }
  function statsEfectivas(est, card) {
    const m = modsCarta(est, card && card.iid);
    return { hp: (card ? card.hp : 0) + m.hpExtra, retiro: m.retiroSet != null ? m.retiroSet : (card ? card.retirada || 0 : 0) };
  }
  // ¿La carta es inmune a condiciones especiales?
  function cartaInmune(est, iid) { return modsCarta(est, iid).inmune; }
  function ladoInmune(est, lado) { const a = est.lados[lado] && est.lados[lado].activo; return !!a && cartaInmune(est, a.iid); }
  // Daño de ataque ajustado por pasivos del defensor (reduce/aumenta). dmg ya trae debilidad/resist.
  function danioAjustado(est, defCard, dmg) {
    if (!defCard || dmg <= 0) return dmg;
    const m = modsCarta(est, defCard.iid);
    return Math.max(0, dmg - m.reduceDanio + m.aumentaDanio);
  }

  const API = {
    modsCarta: modsCarta, statsEfectivas: statsEfectivas, hpEf: hpEf, retiroEf: retiroEf,
    cartaInmune: cartaInmune, ladoInmune: ladoInmune, danioAjustado: danioAjustado, _setObjetivo: setObjetivo
  };
  global.EFECTOS_PASIVOS = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
