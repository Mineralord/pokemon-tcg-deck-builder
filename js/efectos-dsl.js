/* =============================================================
   efectos-dsl.js — Esquema y validador del DSL de efectos (Fase 1b)
   Define el catálogo CERRADO de operaciones, objetivos, filtros, condiciones
   y mods pasivos, y valida que una definición de carta esté bien formada.
   No ejecuta nada: la ejecución vive en js/efectos-motor.js (Fase 1c).
   Spec: docs/efectos-dsl.md
   ============================================================= */
(function (global) {
  'use strict';

  // ---------- Catálogos cerrados ----------
  // Ops que ya ejecuta el motor en Fase 1 (deterministas, sin elección).
  const OPS_F1 = ['danio', 'danioExtra', 'recoil', 'estado', 'quitarEstado',
    'curar', 'descartarEnergia', 'robar', 'descartarMano',
    'aLostZone', 'descartarMazo', 'lostMazo', 'barajarManoEnMazo'];
  // Ops reservadas para Fase 2 (requieren elección). Se validan pero el motor de
  // Fase 1 las deja al respaldo manual.
  const OPS_F2 = ['buscarMazo', 'buscarDescarte', 'elegirObjetivo', 'moverEnergia', 'ponerEnBanca',
    'cambiarActivo', 'mirarTopN'];
  const OPS = OPS_F1.concat(OPS_F2);

  const OBJETIVOS = ['esteP', 'propioActivo', 'rivalActivo', 'propiaBanca', 'rivalBanca',
    'propioTodos', 'rivalTodos', 'todos', 'propioMazo', 'propioDescarte', 'propiaMano', 'elegido'];

  const FILTRO_KEYS = ['tipo', 'etapa', 'nombre', 'esBasico', 'supertipo', 'tieneDanio', 'conEnergia'];

  const COND_TIPOS = ['coin', 'tieneDanio', 'tieneCondicion', 'cuenta', 'primerTurno'];

  const PORCADA_OPS = ['cuenta', 'energias', 'contadores', 'caras', 'carasDe', 'carasHastaCruz'];

  const MODS = ['reduceDanio', 'hpExtra', 'costoRetiro', 'costoAtaque', 'inmuneEstado',
    'proveeEnergia', 'aumentaDanio', 'bloqueaHabilidad', 'noRetira', 'noWeakness'];

  const ESTADOS = ['asleep', 'confused', 'paralyzed', 'poisoned', 'burned'];

  // Ops que mueven daño/curación y por tanto requieren cantidad XOR porCada.
  const OPS_NUMERICAS = ['danio', 'danioExtra', 'recoil', 'curar'];

  function esEntero(n) { return typeof n === 'number' && isFinite(n) && Math.floor(n) === n; }

  // ---------- Validación ----------
  function validarPorCada(pc, ruta, errores) {
    if (typeof pc !== 'object' || pc === null) { errores.push(ruta + ': porCada debe ser objeto'); return; }
    if (PORCADA_OPS.indexOf(pc.op) < 0) errores.push(ruta + ': porCada.op desconocido "' + pc.op + '"');
    if (pc.objetivo && OBJETIVOS.indexOf(pc.objetivo) < 0) errores.push(ruta + ': porCada.objetivo desconocido "' + pc.objetivo + '"');
    if (!esEntero(pc.multiplica)) errores.push(ruta + ': porCada.multiplica debe ser entero');
  }

  function validarCondicion(cond, ruta, errores) {
    if (typeof cond !== 'object' || cond === null) { errores.push(ruta + ': condicion debe ser objeto'); return; }
    if (COND_TIPOS.indexOf(cond.tipo) < 0) errores.push(ruta + ': condicion.tipo desconocido "' + cond.tipo + '"');
    if (cond.objetivo && OBJETIVOS.indexOf(cond.objetivo) < 0) errores.push(ruta + ': condicion.objetivo desconocido "' + cond.objetivo + '"');
    if (cond.estado && ESTADOS.indexOf(cond.estado) < 0) errores.push(ruta + ': condicion.estado desconocido "' + cond.estado + '"');
  }

  function validarFiltro(filtro, ruta, errores) {
    Object.keys(filtro).forEach(function (k) {
      if (FILTRO_KEYS.indexOf(k) < 0) errores.push(ruta + ': filtro.' + k + ' desconocido');
    });
  }

  function validarEstadoVal(v, ruta, errores) {
    const arr = Array.isArray(v) ? v : [v];
    arr.forEach(function (e) { if (ESTADOS.indexOf(e) < 0) errores.push(ruta + ': estado desconocido "' + e + '"'); });
  }

  function validarOp(o, ruta, errores) {
    if (typeof o !== 'object' || o === null) { errores.push(ruta + ': op debe ser objeto'); return; }
    if (OPS.indexOf(o.op) < 0) { errores.push(ruta + ': op desconocida "' + o.op + '"'); return; }
    if (o.objetivo && OBJETIVOS.indexOf(o.objetivo) < 0) errores.push(ruta + ': objetivo desconocido "' + o.objetivo + '"');
    if (o.filtro) validarFiltro(o.filtro, ruta, errores);
    if (o.condicion) validarCondicion(o.condicion, ruta, errores);
    if (o.estado) validarEstadoVal(o.estado, ruta, errores);
    // cantidad XOR porCada para ops numéricas (salvo formas "todo"/"todas").
    if (OPS_NUMERICAS.indexOf(o.op) >= 0) {
      const tieneCant = (o.cantidad != null), tienePC = (o.porCada != null);
      if (tieneCant && tienePC) errores.push(ruta + ': usa cantidad O porCada, no ambos');
      if (!tieneCant && !tienePC && !o.todo) errores.push(ruta + ': falta cantidad o porCada');
      if (tieneCant && !esEntero(o.cantidad)) errores.push(ruta + ': cantidad debe ser entero');
      if (tienePC) validarPorCada(o.porCada, ruta, errores);
    }
  }

  function validarPasivo(p, ruta, errores) {
    if (typeof p !== 'object' || p === null) { errores.push(ruta + ': pasivo debe ser objeto'); return; }
    if (MODS.indexOf(p.mod) < 0) errores.push(ruta + ': mod desconocido "' + p.mod + '"');
    if (p.a && OBJETIVOS.indexOf(p.a) < 0) errores.push(ruta + ': pasivo.a desconocido "' + p.a + '"');
  }

  // Valida una "entrada de efecto" { ops?, pasivos?, efectoJS? }.
  function validarEntrada(ent, ruta, errores) {
    if (typeof ent !== 'object' || ent === null) { errores.push(ruta + ': entrada debe ser objeto'); return; }
    if (ent.efectoJS != null && typeof ent.efectoJS !== 'function') errores.push(ruta + ': efectoJS debe ser función');
    if (ent.ops != null) {
      if (!Array.isArray(ent.ops)) errores.push(ruta + ': ops debe ser array');
      else ent.ops.forEach(function (o, i) { validarOp(o, ruta + '.ops[' + i + ']', errores); });
    }
    if (ent.pasivos != null) {
      if (!Array.isArray(ent.pasivos)) errores.push(ruta + ': pasivos debe ser array');
      else ent.pasivos.forEach(function (p, i) { validarPasivo(p, ruta + '.pasivos[' + i + ']', errores); });
    }
  }

  // Valida la definición completa de una carta:
  // { ataques:{name:entrada}, habilidades:{name:entrada}, jugar:entrada, pasivos:[...] }
  function validar(def) {
    const errores = [];
    if (typeof def !== 'object' || def === null) return { ok: false, errores: ['def debe ser objeto'] };
    ['ataques', 'habilidades'].forEach(function (grupo) {
      if (def[grupo] == null) return;
      if (typeof def[grupo] !== 'object') { errores.push(grupo + ' debe ser objeto'); return; }
      Object.keys(def[grupo]).forEach(function (nombre) {
        validarEntrada(def[grupo][nombre], grupo + '[' + nombre + ']', errores);
      });
    });
    if (def.jugar != null) validarEntrada(def.jugar, 'jugar', errores);
    if (def.pasivos != null) {
      if (!Array.isArray(def.pasivos)) errores.push('pasivos debe ser array');
      else def.pasivos.forEach(function (p, i) { validarPasivo(p, 'pasivos[' + i + ']', errores); });
    }
    return { ok: errores.length === 0, errores: errores };
  }

  // ¿Una entrada es ejecutable por el motor de Fase 1? (solo ops F1, o efectoJS)
  function esEjecutableF1(ent) {
    if (!ent) return false;
    if (typeof ent.efectoJS === 'function') return true;
    if (!Array.isArray(ent.ops) || !ent.ops.length) return false;
    return ent.ops.every(function (o) { return OPS_F1.indexOf(o.op) >= 0; });
  }

  const API = {
    OPS: OPS, OPS_F1: OPS_F1, OPS_F2: OPS_F2,
    OBJETIVOS: OBJETIVOS, FILTRO_KEYS: FILTRO_KEYS, COND_TIPOS: COND_TIPOS,
    PORCADA_OPS: PORCADA_OPS, MODS: MODS, ESTADOS: ESTADOS,
    validar: validar, validarEntrada: validarEntrada, esEjecutableF1: esEjecutableF1
  };

  global.EFECTOS_DSL = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;

})(typeof globalThis !== 'undefined' ? globalThis : (typeof self !== 'undefined' ? self : this));
