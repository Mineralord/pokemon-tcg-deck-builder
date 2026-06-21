/* =============================================================
   efectos-motor.js — Intérprete del DSL de efectos (Fases 1c + 2)
   Ejecuta las `ops` de una entrada del DSL contra el estado de la partida.
   - Fase 1: ops deterministas sin elección.
   - Fase 2: ops con elección (buscarMazo, elegirObjetivo, cambiarActivo,
     ponerEnBanca, mirarTopN). Cuando una op necesita decidir algo del jugador,
     el motor PAUSA: escribe `est.pendiente` (serializable, por iids) y devuelve la
     marca 'pendiente'. El juego reanuda con resumir(est, seleccion).
   Determinismo: moneda/baraje con el MISMO algoritmo que js/juego.js, para que
   online / IA / tests coincidan jugada a jugada.
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
  // Baraja determinista (avanza el RNG del estado).
  function rndDe(est) {
    est.rngN = (est.rngN || 0) + 1;
    return rng32(((est.seed || 1) ^ Math.imul(est.rngN, 40503)) >>> 0);
  }
  function barajar(arr, rnd) {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(rnd() * (i + 1));
      const t = arr[i]; arr[i] = arr[j]; arr[j] = t;
    }
    return arr;
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

  const OPS_ELECCION = ['buscarMazo', 'elegirObjetivo', 'cambiarActivo', 'ponerEnBanca', 'mirarTopN'];
  function esEleccion(op) { return OPS_ELECCION.indexOf(op) >= 0; }

  // ---------- Resolución de objetivos ----------
  function ladoRival(lado) { return lado === 'A' ? 'B' : 'A'; }

  function cartaUnica(ctx, objetivo) {
    const est = ctx.est, L = est.lados[ctx.lado], O = est.lados[ctx.op];
    switch (objetivo) {
      case 'esteP': return ctx.fuente || ctx.at || L.activo;
      case 'propioActivo': return L.activo;
      case 'rivalActivo': return O.activo;
      case 'elegido': return (ctx.elegido && ctx.elegido[0]) || null;
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
      case 'todos': arr = (L.activo ? [L.activo] : []).concat(L.banca, O.activo ? [O.activo] : [], O.banca); break;
      case 'elegido': arr = ctx.elegido ? ctx.elegido.slice() : []; break;
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

  // Busca una carta por iid en cualquier zona de cualquier lado.
  function buscarPorIid(est, iid) {
    if (iid == null) return null;
    const lados = est.lados;
    for (const k in lados) {
      const L = lados[k];
      if (L.activo && L.activo.iid === iid) return L.activo;
      const zonas = [L.banca, L.mano, L.mazo, L.descarte, L.premios, L.lost];
      for (let z = 0; z < zonas.length; z++) {
        const arr = zonas[z]; if (!arr) continue;
        for (let i = 0; i < arr.length; i++) if (arr[i] && arr[i].iid === iid) return arr[i];
      }
    }
    return null;
  }
  function iidsDe(cards) { return (cards || []).map(function (c) { return c && c.iid; }).filter(function (x) { return x != null; }); }

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

  // ---------- Ejecución de ops deterministas (Fase 1) ----------
  function dueñoDe(ctx, card) {
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
        const PAS = global.EFECTOS_PASIVOS;
        cartasObjetivo(ctx, o.objetivo || 'rivalActivo', o.filtro).forEach(function (c) {
          if (PAS && PAS.cartaInmune && PAS.cartaInmune(ctx.est, c.iid)) return; // inmune (pasivo)
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
        out.push('manual'); // op desconocida -> respaldo manual asistido
    }
  }

  // ---------- Ops con elección (Fase 2) ----------
  // Construye la solicitud serializable de elección de una op.
  function opcionDe(c, zona) { return { iid: c.iid, nombre: c.nombre, imagen: c.imagen || null, zona: zona }; }
  function solicitudDe(ctx, o) {
    const est = ctx.est, L = est.lados[ctx.lado], O = est.lados[ctx.op];
    switch (o.op) {
      case 'buscarMazo': {
        const op = L.mazo.filter(function (c) { return cumpleFiltro(c, o.filtro); });
        return { tipo: 'buscarMazo', prompt: o.prompt || 'Busca en tu mazo', destino: o.destino || 'mano',
          opciones: op.map(function (c) { return opcionDe(c, 'mazo'); }), min: 0, max: o.cantidad || 1, cancelable: true };
      }
      case 'mirarTopN': {
        const top = L.mazo.slice(0, o.n || 1).filter(function (c) { return cumpleFiltro(c, o.filtro); });
        return { tipo: 'mirarTopN', prompt: o.prompt || 'Mira las cartas de arriba', destino: o.destino || 'mano',
          opciones: top.map(function (c) { return opcionDe(c, 'mazo'); }), min: 0, max: o.cantidad || 1, cancelable: true };
      }
      case 'elegirObjetivo': {
        const cs = cartasObjetivo(ctx, o.objetivo || 'propioTodos', o.filtro);
        const n = o.cuantos || 1;
        return { tipo: 'elegirObjetivo', prompt: o.prompt || 'Elige un objetivo',
          opciones: cs.map(function (c) { return opcionDe(c, 'juego'); }), min: Math.min(n, cs.length), max: n, cancelable: false };
      }
      case 'cambiarActivo': {
        const lado = o.objetivo === 'rival' ? ctx.op : ctx.lado;
        const banca = est.lados[lado].banca;
        return { tipo: 'cambiarActivo', prompt: o.prompt || 'Elige el nuevo Activo', ladoCambio: lado,
          opciones: banca.map(function (c) { return opcionDe(c, 'banca'); }), min: banca.length ? 1 : 0, max: 1, cancelable: false };
      }
      case 'ponerEnBanca': {
        const basicos = L.mano.filter(function (c) { return c.supertipo === 'Pokemon' && c.esBasico && cumpleFiltro(c, o.filtro); });
        return { tipo: 'ponerEnBanca', prompt: o.prompt || 'Pon Básicos en tu Banca',
          opciones: basicos.map(function (c) { return opcionDe(c, 'mano'); }), min: 0, max: Math.min(o.cantidad || 5, 5 - L.banca.length), cancelable: true };
      }
      default: return { tipo: o.op, opciones: [], min: 0, max: 0, cancelable: true };
    }
  }

  // Aplica una op de elección con la selección (array de iids) ya provista.
  function aplicarEleccion(ctx, o, sel, out) {
    const est = ctx.est, L = est.lados[ctx.lado];
    sel = sel || [];
    switch (o.op) {
      case 'buscarMazo':
      case 'mirarTopN': {
        const destino = o.destino || 'mano';
        sel.forEach(function (iid) {
          const i = L.mazo.findIndex(function (c) { return c.iid === iid; });
          if (i < 0) return;
          const c = L.mazo.splice(i, 1)[0];
          if (destino === 'banca' && L.banca.length < 5) c.enJuegoDesde = est.turno, L.banca.push(c);
          else if (destino === 'activo' && !L.activo) c.enJuegoDesde = est.turno, L.activo = c;
          else L.mano.push(c);
        });
        if (o.op === 'buscarMazo') barajar(L.mazo, rndDe(est)); // barajar tras buscar
        out.push('buscar');
        break;
      }
      case 'elegirObjetivo': {
        ctx.elegido = sel.map(function (iid) { return buscarPorIid(est, iid); }).filter(Boolean);
        out.push('elegido');
        break;
      }
      case 'cambiarActivo': {
        const lado = o.objetivo === 'rival' ? ctx.op : ctx.lado;
        const Lc = est.lados[lado];
        const iid = sel[0]; const i = Lc.banca.findIndex(function (c) { return c.iid === iid; });
        if (i >= 0) { const nuevo = Lc.banca.splice(i, 1)[0]; if (Lc.activo) { Lc.activo.condiciones = []; Lc.banca.push(Lc.activo); } Lc.activo = nuevo; }
        out.push('cambiar');
        break;
      }
      case 'ponerEnBanca': {
        sel.forEach(function (iid) {
          if (L.banca.length >= 5) return;
          const i = L.mano.findIndex(function (c) { return c.iid === iid; });
          if (i < 0) return; const c = L.mano.splice(i, 1)[0]; c.enJuegoDesde = est.turno; L.banca.push(c);
        });
        out.push('banca');
        break;
      }
    }
  }

  // ---------- Bucle de ejecución con pausa/reanudación ----------
  function construirCtx(est, lado, ctxExtra, cont) {
    const ctx = {
      est: est, lado: lado, op: ladoRival(lado),
      at: cont ? buscarPorIid(est, cont.atIid) : ((ctxExtra && ctxExtra.at) || est.lados[lado].activo),
      def: cont ? buscarPorIid(est, cont.defIid) : ((ctxExtra && ctxExtra.def) || est.lados[ladoRival(lado)].activo),
      fuente: cont ? buscarPorIid(est, cont.fuenteIid) : ((ctxExtra && ctxExtra.fuente) || null),
      elegido: cont ? (cont.elegidoIids || []).map(function (i) { return buscarPorIid(est, i); }).filter(Boolean) : [],
      flip: function () { return flipDe(est); }
    };
    ctx._faseFinal = cont ? cont.faseFinal : (ctxExtra && ctxExtra.faseFinal) || null;
    return ctx;
  }

  function pausar(ctx, opActual, opsRestantes, req, out) {
    const cont = {
      lado: ctx.lado,
      atIid: ctx.at && ctx.at.iid, defIid: ctx.def && ctx.def.iid, fuenteIid: ctx.fuente && ctx.fuente.iid,
      elegidoIids: iidsDe(ctx.elegido),
      ops: opsRestantes.slice(),         // ops[0] es la op pausada (se re-ejecuta con la selección)
      marcas: out.slice(),
      faseFinal: ctx._faseFinal
    };
    ctx.est.pendiente = {
      tipo: req.tipo, lado: ctx.lado, prompt: req.prompt || '', opciones: req.opciones || [],
      min: req.min || 0, max: req.max || 1, cancelable: !!req.cancelable, cont: cont
    };
  }

  // Corre una lista de ops. `selInicial` (iids) resuelve la primera op si es de elección.
  function correrOps(ctx, ops, selInicial) {
    const out = (ctx._marcas || []).slice(); ctx._marcas = null;
    for (let i = 0; i < ops.length; i++) {
      const o = ops[i];
      if (esEleccion(o.op)) {
        if (o.condicion && !evalCondicion(ctx, o.condicion)) continue;
        if (i === 0 && selInicial != null) { aplicarEleccion(ctx, o, selInicial, out); selInicial = null; continue; }
        const req = solicitudDe(ctx, o);
        if ((req.opciones || []).length === 0) { continue; } // nada que elegir -> se omite
        pausar(ctx, o, ops.slice(i), req, out);
        return out.concat(['pendiente']);
      }
      ejecutarOp(ctx, o, out);
    }
    return out.length ? out : ['manual'];
  }

  // ---------- API pública ----------
  function ejecutar(est, lado, entrada, ctxExtra) {
    if (!entrada) return ['manual'];
    if (typeof entrada.efectoJS === 'function') {
      const ctx = construirCtx(est, lado, ctxExtra, null);
      try { entrada.efectoJS(ctx); return ['efectoJS']; } catch (e) { return ['manual']; }
    }
    if (!Array.isArray(entrada.ops) || !entrada.ops.length) return ['manual'];
    const ctx = construirCtx(est, lado, ctxExtra, null);
    return correrOps(ctx, entrada.ops, null);
  }

  // Reanuda un efecto pausado con la selección del jugador (array de iids o null=cancelar).
  // Devuelve las marcas acumuladas; deja est.pendiente si requiere otra elección.
  function resumir(est, seleccion) {
    const p = est.pendiente; if (!p) return ['manual'];
    const cont = p.cont; const ctx = construirCtx(est, cont.lado, null, cont);
    ctx._marcas = cont.marcas || [];
    // Validar la selección contra las opciones ofrecidas.
    const validos = {}; (p.opciones || []).forEach(function (op) { validos[op.iid] = true; });
    let sel = Array.isArray(seleccion) ? seleccion.filter(function (i) { return validos[i]; }) : [];
    if (sel.length > p.max) sel = sel.slice(0, p.max);
    if (sel.length < p.min && !p.cancelable) { /* selección insuficiente: ignora, sigue pendiente */ if (sel.length === 0) return ['pendiente']; }
    est.pendiente = null;
    return correrOps(ctx, cont.ops, sel);
  }

  const API = {
    ejecutar: ejecutar, resumir: resumir, flipDe: flipDe, esEleccion: esEleccion,
    _evalCondicion: evalCondicion, _cartasObjetivo: cartasObjetivo, _buscarPorIid: buscarPorIid
  };
  global.EFECTOS_MOTOR = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
