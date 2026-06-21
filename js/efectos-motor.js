/* =============================================================
   efectos-motor.js — Intérprete del DSL de efectos (Fase 1c)
   Ejecuta las `ops` deterministas (sin elección) de una entrada del DSL contra
   el estado de la partida. Las ops que requieren elección (Fase 2) se ignoran
   aquí y devuelven la marca 'manual' para caer al respaldo asistido.
   Determinismo: usa una moneda con el MISMO algoritmo que js/juego.js (_flip),
   para que online / IA / tests coincidan jugada a jugada.
   ============================================================= */
(function (global) {
  'use strict';

  function rng32(seed) {
    let a = (seed >>> 0) || 1;
    return function () {
      a |= 0; a = (a + 0x6D2B79F5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }
  // Moneda serializable idéntica a juego.js: true = cara.
  function flipDe(est) {
    est.rngN = (est.rngN || 0) + 1;
    const f = rng32(((est.seed || 1) ^ Math.imul(est.rngN, 2654435761)) >>> 0);
    return f() < 0.5;
  }

  const ROTATIVAS = ['asleep', 'confused', 'paralyzed'];
  function aplicarEstadoCarta(card, cond) {
    if (!card) return;
    card.condiciones = card.condiciones || [];
    if (ROTATIVAS.indexOf(cond) >= 0) {
      card.condiciones = card.condiciones.filter(function (x) { return ROTATIVAS.indexOf(x) < 0; });
      card.condiciones.push(cond);
    } else if (card.condiciones.indexOf(cond) < 0) card.condiciones.push(cond);
  }

  // ---------- Resolución de objetivos ----------
  function ladoRival(lado) { return lado === 'A' ? 'B' : 'A'; }

  function cartaUnica(ctx, objetivo) {
    const est = ctx.est, L = est.lados[ctx.lado], O = est.lados[ctx.op];
    switch (objetivo) {
      case 'esteP': return ctx.fuente || ctx.at || L.activo;
      case 'propioActivo': return L.activo;
      case 'rivalActivo': return O.activo;
      default: return null;
    }
  }

  function cumpleFiltro(card, f) {
    if (!f) return true;
    if (f.tipo != null && (card.tipos || []).indexOf(f.tipo) < 0) return false;
    if (f.etapa != null && card.stage !== f.etapa) return false;
    if (f.nombre != null && card.nombre !== f.nombre) return false;
    if (f.esBasico != null && !!card.esBasico !== !!f.esBasico) return false;
    if (f.supertipo != null && card.supertipo !== f.supertipo) return false;
    if (f.tieneDanio != null && ((card.danio || 0) > 0) !== !!f.tieneDanio) return false;
    if (f.conEnergia != null && ((card.energias || []).length > 0) !== !!f.conEnergia) return false;
    return true;
  }

  function cartasObjetivo(ctx, objetivo, filtro) {
    const est = ctx.est, L = est.lados[ctx.lado], O = est.lados[ctx.op];
    let arr;
    switch (objetivo) {
      case 'propiaBanca': arr = L.banca.slice(); break;
      case 'rivalBanca': arr = O.banca.slice(); break;
      case 'propioTodos': arr = (L.activo ? [L.activo] : []).concat(L.banca); break;
      case 'rivalTodos': arr = (O.activo ? [O.activo] : []).concat(O.banca); break;
      default: { const c = cartaUnica(ctx, objetivo); arr = c ? [c] : []; }
    }
    return filtro ? arr.filter(function (c) { return cumpleFiltro(c, filtro); }) : arr;
  }

  function zonaObjetivo(ctx, objetivo) {
    const est = ctx.est, L = est.lados[ctx.lado];
    switch (objetivo) {
      case 'propioMazo': return L.mazo;
      case 'propioDescarte': return L.descarte;
      case 'propiaMano': return L.mano;
      default: return null;
    }
  }

  function tamObjetivo(ctx, objetivo) {
    const zona = zonaObjetivo(ctx, objetivo);
    if (zona) return zona.length;
    return cartasObjetivo(ctx, objetivo).length;
  }

  // ---------- Evaluadores ----------
  function evalPorCada(ctx, pc) {
    const mult = pc.multiplica || 0;
    let n = 0;
    switch (pc.op) {
      case 'cuenta': n = tamObjetivo(ctx, pc.objetivo); break;
      case 'energias': { const c = cartaUnica(ctx, pc.objetivo || 'rivalActivo'); n = c ? (c.energias || []).length : 0; break; }
      case 'contadores': { const c = cartaUnica(ctx, pc.objetivo || 'esteP'); n = c ? Math.floor((c.danio || 0) / 10) : 0; break; }
      case 'carasHastaCruz': { while (ctx.flip()) n++; break; }
      case 'carasDe': { for (let k = 0; k < (pc.monedas || 0); k++) if (ctx.flip()) n++; break; }
      case 'caras': { if (ctx.flip()) n = 1; break; }
    }
    return n * mult;
  }

  function valorCantidad(ctx, o) {
    if (o.porCada) return evalPorCada(ctx, o.porCada);
    return o.cantidad || 0;
  }

  function cmp(a, op, b) {
    switch (op) {
      case '>=': return a >= b; case '>': return a > b;
      case '<=': return a <= b; case '<': return a < b;
      default: return a === b;
    }
  }

  function evalCondicion(ctx, cond) {
    if (!cond) return true;
    switch (cond.tipo) {
      case 'coin': return ctx.flip();
      case 'tieneDanio': { const c = cartaUnica(ctx, cond.objetivo || 'esteP'); return !!c && (c.danio || 0) > 0; }
      case 'tieneCondicion': { const c = cartaUnica(ctx, cond.objetivo || 'rivalActivo'); return !!c && (c.condiciones || []).indexOf(cond.estado) >= 0; }
      case 'cuenta': return cmp(tamObjetivo(ctx, cond.objetivo), cond.op || '>=', cond.n || 0);
      case 'primerTurno': return ctx.est.lados[ctx.lado].turnosJugados === 0;
      default: return false;
    }
  }

  // ---------- Ejecución de ops ----------
  function dueñoDe(ctx, card) {
    // ¿de qué lado es esta carta? (para descartes correctos)
    const L = ctx.est.lados[ctx.lado], O = ctx.est.lados[ctx.op];
    if (L.activo === card || L.banca.indexOf(card) >= 0) return L;
    if (O.activo === card || O.banca.indexOf(card) >= 0) return O;
    return L;
  }

  function ejecutarOp(ctx, o, out) {
    if (o.condicion && !evalCondicion(ctx, o.condicion)) return;
    switch (o.op) {
      case 'danioExtra': {
        if (ctx.def) { const n = valorCantidad(ctx, o); if (n > 0) { ctx.def.danio = (ctx.def.danio || 0) + n; out.push('danioExtra'); } }
        break;
      }
      case 'danio': {
        const n = valorCantidad(ctx, o);
        cartasObjetivo(ctx, o.objetivo, o.filtro).forEach(function (c) { if (n > 0) c.danio = (c.danio || 0) + n; });
        if (n > 0) out.push('danio');
        break;
      }
      case 'recoil': {
        const c = ctx.fuente || ctx.at; const n = valorCantidad(ctx, o);
        if (c && n > 0) { c.danio = (c.danio || 0) + n; out.push('recoil'); }
        break;
      }
      case 'estado': {
        const estados = Array.isArray(o.estado) ? o.estado : [o.estado];
        cartasObjetivo(ctx, o.objetivo || 'rivalActivo', o.filtro).forEach(function (c) {
          estados.forEach(function (e) { aplicarEstadoCarta(c, e); });
        });
        out.push('estado');
        break;
      }
      case 'quitarEstado': {
        cartasObjetivo(ctx, o.objetivo || 'esteP', o.filtro).forEach(function (c) {
          if (!c.condiciones) return;
          if (o.todas) c.condiciones = [];
          else if (o.estado) c.condiciones = c.condiciones.filter(function (x) { return x !== o.estado; });
        });
        out.push('quitarEstado');
        break;
      }
      case 'curar': {
        cartasObjetivo(ctx, o.objetivo || 'esteP', o.filtro).forEach(function (c) {
          if (o.todo) c.danio = 0;
          else c.danio = Math.max(0, (c.danio || 0) - valorCantidad(ctx, o));
        });
        out.push('curar');
        break;
      }
      case 'descartarEnergia': {
        cartasObjetivo(ctx, o.objetivo || 'esteP', o.filtro).forEach(function (c) {
          const L = dueñoDe(ctx, c);
          const n = o.todo ? (c.energias || []).length : valorCantidad(ctx, o);
          for (let k = 0; k < n && (c.energias || []).length; k++) L.descarte.push(c.energias.pop());
        });
        out.push('descartarEnergia');
        break;
      }
      case 'robar': {
        const L = ctx.est.lados[ctx.lado]; const n = o.cantidad || 0;
        for (let k = 0; k < n && L.mazo.length; k++) L.mano.push(L.mazo.shift());
        out.push('robar');
        break;
      }
      case 'descartarMano': {
        const L = ctx.est.lados[ctx.lado];
        const n = o.todo ? L.mano.length : (o.cantidad || 0);
        for (let k = 0; k < n && L.mano.length; k++) L.descarte.push(L.mano.pop());
        out.push('descartarMano');
        break;
      }
      default:
        out.push('manual'); // op de Fase 2: respaldo manual asistido
    }
  }

  // ---------- API pública ----------
  // Ejecuta una entrada del DSL. ctxExtra: { at, def, fuente, flip }.
  // Devuelve lista de marcas de efectos aplicados (o ['manual'] si no ejecutable).
  function ejecutar(est, lado, entrada, ctxExtra) {
    if (!entrada) return ['manual'];
    const ctx = {
      est: est, lado: lado, op: ladoRival(lado),
      at: (ctxExtra && ctxExtra.at) || est.lados[lado].activo,
      def: (ctxExtra && ctxExtra.def) || est.lados[ladoRival(lado)].activo,
      fuente: (ctxExtra && ctxExtra.fuente) || null,
      flip: (ctxExtra && ctxExtra.flip) || function () { return flipDe(est); }
    };
    if (typeof entrada.efectoJS === 'function') {
      try { entrada.efectoJS(ctx); return ['efectoJS']; } catch (e) { return ['manual']; }
    }
    if (!Array.isArray(entrada.ops) || !entrada.ops.length) return ['manual'];
    const out = [];
    entrada.ops.forEach(function (o) { ejecutarOp(ctx, o, out); });
    return out.length ? out : ['manual'];
  }

  const API = { ejecutar: ejecutar, flipDe: flipDe, _evalCondicion: evalCondicion, _cartasObjetivo: cartasObjetivo };
  global.EFECTOS_MOTOR = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
