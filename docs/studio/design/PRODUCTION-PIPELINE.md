# 🏭 PRODUCTION PIPELINE — Metodología Oficial de Producción

> **Documento permanente.** Responde una sola pregunta: **¿Cómo nace un evento AAA?** — desde que se elige
> hasta que llega al juego definitivo. A partir de su aprobación, **todo evento** se produce con **este
> mismo proceso**; ningún sprint vuelve a preguntar "¿qué hacemos ahora?".
>
> **Marca el fin de la etapa de diseño global y el inicio de la PRODUCCIÓN.** Los siguientes sprints ya no
> diseñan metodologías: diseñan **eventos concretos** usando este pipeline.
>
> **Cadena documental canónica** (autoridad, de mayor a menor sobre lo visual):
> **Visual Language Bible** (cómo se siente — vocabulario §1–§13 + gramática §16) →
> **Event Catalog** (qué sucede y en qué orden) → **Plan Maestro** (estrategia de reemplazo) →
> **este Production Pipeline** (cómo se ejecuta cada evento).

---

## Pipeline de un vistazo (8 puertas, un solo camino)
```
[G1] Selección → [G2] Investigación AAA → [G3] Objetivo emocional → [G4] Diseño conceptual
      → [G5] Validación (gate) → [G6] Implementación + Iteración en Studio → [G7] Aprobación (CANON)
      → [G8] Migración al Game
```
Cada puerta tiene **criterio de entrada** y **criterio de salida**; no se avanza sin cumplir el de salida.
Mapea 1:1 con los estados del Catálogo: `PENDIENTE → INVESTIGACIÓN → DISEÑO → IMPLEMENTACIÓN → ITERACIÓN →
CANON`. **Regla absoluta (canon):** nunca dos eventos a la vez; no se avanza al siguiente hasta que el
actual sea `CANON`.

---

## Principios de proceso destilados (investigación de metodologías; principios, no procesos copiados)
- **Estudios AAA:** *art bible / style guide* como autoridad (→ nuestra Bible), *vertical slice* para validar
  el estándar antes de escalar, **milestone gates** con criterios de salida, *review boards* (animación/arte).
- **Game feel:** "juice con contención", **playtesting** temprano y frecuente, iteración sobre *feel* medible.
- **UX / Motion design:** definir la intención antes del artefacto, jerarquía y foco, tokens compartidos.
- **Producción cinematográfica:** **previz → animatic → final**, *dailies* (revisión diaria), un **director**
  que garantiza coherencia, el guion antes del rodaje.
- **Control de calidad:** **Definition of Done** explícita, **quality gates**, pruebas de regresión, "no se
  aprueba lo mediocre" como política, no como opinión.
> Adaptación al proyecto: **un solo autor/director** (desarrollo unipersonal) hace de art director, animator,
> QA y aprobador; las "puertas" sustituyen a los comités; el **Studio** es el laboratorio de *dailies* y
> *playtesting*; la **Bible** es el art bible; los **checkpoints deterministas** son el vertical slice.

---

## CAPÍTULO 1 · Selección del evento  *(Puerta G1)*
**Cómo se elige y prioriza.** El orden lo marca el **Plan Maestro** (fases) y el **Event Catalog**. Dentro
de una fase, se prioriza con **cuatro factores**:
1. **Impacto** (tier S/A/B/C/D del Catálogo + emoción): mayor impacto, antes.
2. **Frecuencia** (cuántas veces lo ve el jugador): lo omnipresente pesa más.
3. **Dependencias** (habilitación): un evento que desbloquea a otros va antes.
4. **Complejidad / riesgo**: a igualdad, primero lo que reduce incertidumbre (pilotos).

**Identificación de dependencias:** se leen del Catálogo — `Anterior → Siguiente` (orden narrativo) y
**Precondición** (estado/eventos que deben existir para reproducirlo legalmente; guía el checkpoint). Se
listan explícitamente antes de empezar.

**Determinación de complejidad (rúbrica):** Baja (una capa de feedback, sin infra nueva) · Media (varios
beats, reutiliza infra) · Alta (nuevo tipo de efecto/canal, varias capas) · AAA-clímax (hit-stop, cámara,
multicapa, gramática de clímax mayor). La complejidad fija el presupuesto de tiempo y de capas.

**Criterio de salida G1:** evento seleccionado (por ID), prioridad justificada, dependencias y complejidad
declaradas. → Catálogo pasa a `INVESTIGACIÓN`.

---

## CAPÍTULO 2 · Investigación AAA  *(Puerta G2 — obligatoria)*
Metodología (no una lista de enlaces). Sigue la skill `animation-benchmark`.

- **Cuándo:** **siempre** antes de diseñar un evento no trivial. Sin research aprobado, no se diseña.
- **Qué investigar:** la **mejor experiencia existente** para la emoción objetivo del evento — no solo TCG
  Live: videojuegos AAA, juegos de cartas, cine, broadcast, motion design, UX, game-feel competitivo.
- **Qué recursos:** gameplay/trailers oficiales, **GDC/SIGGRAPH**, breakdowns técnicos, entrevistas, y los
  **vídeos de referencia locales** (`replicar-tcglive` / `vidref.py`) para TCG Live.
- **Cómo analizar:** **ficha por referencia** (identificación · descomposición visual · calidad ·
  complejidad · reutilización) + **clasificación en tiers S/A/B/C/D**. Nunca analizar una sola: **comparar**
  y justificar por qué una supera a otra.
- **Inspiración vs copia:** se extraen **principios reutilizables**, **nunca** implementaciones. La salida
  describe *qué feel conseguir*, no *qué copiar*.
- **Justificar decisiones:** **decision log** con referencias elegidas, referencias **descartadas** y el
  porqué. Toda decisión posterior debe poder trazarse a una referencia o a la Bible.

**Entregable:** **Research Package** en `docs/studio/research/<ID>-...md` (estados: `Pendiente → Investigando
→ Referencias aprobadas → Cerrado`).
**Criterio de salida G2:** Research Package con estándar visual objetivo (tier S a igualar) y decision log.

---

## CAPÍTULO 3 · Objetivo emocional  *(Puerta G3)*
Antes de diseñar nada: **¿qué debe sentir exactamente el jugador?** (regla emocional permanente del canon).

- **Declaración:** una frase de emoción-objetivo tomada de la **paleta oficial** (Bible §13: Calma ·
  Preparación · Expectativa · Acción · Impacto · Peligro · Victoria · Derrota), con su expresión visual
  esperada.
- **Cómo se MIDE (test de aceptación emocional):** el evento **pasa** si, al reproducirlo en el Studio, un
  observador que **no** conoce la intención **reporta la emoción objetivo** (o una compatible) sin que se le
  guíe. Complementos medibles: la lectura del estado nunca se pierde; el pico emocional cae donde la
  gramática lo exige; ninguna emoción contradictoria contamina la frase (Bible §16.C.3).
- **Antivalor:** "que se vea bonito" no es un objetivo emocional. Si no hay emoción declarada, no se avanza.

**Criterio de salida G3:** emoción-objetivo declarada + su test de aceptación definido.

---

## CAPÍTULO 4 · Diseño conceptual  *(Puerta G4)*
Se produce el **beat sheet** del evento usando **exclusivamente** el vocabulario y la gramática de la Bible.
Es el "guion" (previz) antes de implementar.

Para el evento y **por cada beat**, se declara:
- **Foco visual principal** (un solo foco por beat — canon).
- **Ritmo** (nivel R0–R3, Bible §2) y **estructura gramatical** (Anunciar→Actuar→Resolver→Asentar→Entregar,
  §16.A) proporcional a su intensidad.
- **Recursos:** cámara (§3, movimientos permitidos) · iluminación (§4) · profundidad (§5) · materiales/peso
  (§6) · partículas (§7, ≤2 capas) · shake (§8, nivel S0–S3) · transiciones/handoff (§9).
- **UI y feedback:** familia de UI (§10) + capa mínima de feedback suficiente (§11).
- **Jerarquía y handoff:** cómo se dirige la mirada (§12) y cómo entrega al siguiente beat (§16.E).

**Restricciones de diseño (canon):** escenario ⟂ partida · un canal/curva/nivel nuevo **solo** nace si el
idioma actual no puede expresar la intención (nacimiento bajo demanda) · nada de infraestructura "por si
acaso". El diseño **cita** las reglas de la Bible que aplica (trazabilidad).

**Criterio de salida G4:** beat sheet completo, con foco por beat y recursos citados a la Bible.

---

## CAPÍTULO 5 · Validación (gate)  *(Puerta G5 — checklist oficial)*
El diseño **no** pasa a implementación hasta cumplir **toda** la checklist:

- [ ] **Test de pertenencia** (Bible §0): alineado con las 5 anclas y la regla maestra de conflicto.
- [ ] **Test gramatical** (Bible §16.F): estructura, preparación→resolución, presupuesto de intensidad,
      concordancia, handoff, una idea por frase.
- [ ] **Compatibilidad con el Event Catalog:** respeta ID, orden, `Anterior→Siguiente`, Precondición, tipo/ancla.
- [ ] **Compatibilidad con el Plan Maestro:** encaja en su fase; usa la tubería/lenguaje unificados.
- [ ] **Compatibilidad con la filosofía/canon:** escenario ⟂ partida · un foco por beat · HANDOFF ·
      infra/canal bajo demanda · dirección de dependencias Studio→Game · sin conceptos del Studio en el Game.
- [ ] **Legibilidad:** el estado de juego se entiende en todo instante (regla maestra).
- [ ] **Accesibilidad:** define su degradación con reduced-motion (`AnimationManager`).
- [ ] **Determinismo:** reproducible por checkpoint (no depende de estado no reproducible).
- [ ] **Presupuesto:** capas/duración dentro de lo que su complejidad permite (rendimiento móvil).

**Criterio de salida G5:** checklist 100 % en verde. → Catálogo pasa a `IMPLEMENTACIÓN`.

---

## CAPÍTULO 6 · Implementación e Iteración en Studio  *(Puerta G6)*

### 6.0 · Política de Exploración de Implementaciones (canónica)
**Antes de iniciar G6, responder UNA pregunta:** *¿Este evento será recordado espontáneamente por un jugador
meses después de jugar?* — **Sí → Evento Firma (Nivel A)** · **No → Evento Funcional (Nivel B)**. *(Guía: si
el evento tiene un **Momento Firma** registrado, es Nivel A.)*

- **Nivel A — Eventos Firma** (construyen la identidad, permanecen en la memoria: entrada, "el tablero toma
  bando", presentación de jugadores, volado, inicio del 1.er turno, evoluciones importantes, ataques
  icónicos, KO, último Premio, victoria, derrota…):
  - **Obligatorio: ≥ 2 implementaciones** (recomendado 3 cuando el impacto artístico lo justifique).
  - **Comparación objetiva** de las variantes en el Studio → **selección de UNA sola dirección** →
    **refinamiento hasta CANON**. Las no elegidas se archivan como Legacy (no se borran).
- **Nivel B — Eventos Funcionales** (frecuentes; prioridad claridad/rapidez/consistencia: robar, adjuntar
  energía, mover contador, girar carta, confirmaciones, apertura de paneles, micro-estados de UI, feedback
  secundario):
  - **Una implementación inicial → iteraciones de mejora sobre ESA misma → validación → CANON.** No se
    exploran variantes; sería derrochar tiempo en lo rutinario.

**Razón:** la creatividad se concentra donde aporta valor; la producción mantiene ritmo constante; el
proyecto es sostenible con cientos de eventos. La clasificación **no** modifica las fases del pipeline: solo
define **cuántas direcciones** se exploran dentro de G6.

**Cómo se implementa:** sobre el **motor y la pantalla actuales** (`CombatScreen` compositor de capas),
mediante la **tubería canónica** (RenderNode/Executor/Director) o, en framing, el Event Player — **nunca**
duplicando lógica ni creando otra pantalla/tapete. Reutilizar toda la infraestructura posible; infra nueva
solo si el diseño validado la exige (justificada).

**Cómo se prueba, compara e itera (el Studio es el laboratorio de *dailies*):**
- **Probar:** reproducir el evento sobre el escenario oficial (Event Player para framing; ritual sembrado
  para la ceremonia). Transporte: play/pausa/reinicio/velocidad; frame a frame; repetir.
- **Comparar:** contra el **estándar S** del Research Package (¿iguala el feel?) y, si aplica, entre iteraciones.
- **Iterar:** ciclos pequeños **build verde (3 targets) → validación visual → ajuste**; nunca acumular
  cambios enormes; mantener el proyecto compilando.

**Herramientas del Studio:** Board Simulator / Event Player, lanzador de ritual, transporte, comparación,
Presentation Mode, checkpoints deterministas.

**Qué debe quedar registrado:** decision log (qué se ajustó y por qué), nº de iteraciones, y el estado en el
Catálogo (`ITERACIÓN`). El Studio **valida**, no crea el lenguaje: no introduce conceptos propios en el Game.

**Criterio de salida G6:** el evento se reproduce a calidad candidata, verde en los tres targets, validado
visualmente contra el estándar.

---

## CAPÍTULO 7 · Aprobación  *(Puerta G7 — CANON)*
**Cuándo un evento está terminado (Definition of Done):**
- [ ] Cumple **toda** la checklist de G5 **verificada sobre la implementación** (no solo el diseño).
- [ ] **Iguala o supera el estándar S** del Research Package (pasa la comparación de feel).
- [ ] **Pasa el test de aceptación emocional** (G3): la emoción objetivo se percibe.
- [ ] **Legibilidad y accesibilidad** verificadas en el dispositivo.
- [ ] **Sin regresiones** en Game ni Studio; build verde en los tres targets.
- [ ] **Validado visualmente** por el aprobador en el Studio (y en APK si es Demo).

**Cómo evitar aprobar experiencias mediocres:** la aprobación es **por criterios, no por cansancio**. Si
falla **un** punto de la DoD, **no** es CANON: se itera. "Casi" no aprueba. El aprobador es el **director del
proyecto** (unipersonal). Regla anti-mediocridad: *si no iguala la referencia S, no es AAA.*

**Criterio de salida G7:** evento marcado **CANON** en el Catálogo (Madurez → `AAA/CANON`, Workflow → `CANON`,
🟢). Las variantes exploratorias se archivan como Legacy (no se borran).

---

## CAPÍTULO 8 · Migración al Game  *(Puerta G8)*
**Cuándo deja de ser experimental:** al alcanzar **CANON** (G7). Como Game y Studio **comparten el mismo
código** (`feature:combat` / motor), un evento CANON **ya está disponible en el Game**; la "migración" es el
**cableado del disparo real**: mapear el `GameEvent`/transición del motor → evento del Catálogo → variante
CANON, mediante el **Director** (capa de dirección), sin lógica exclusiva del Studio ni cambios de reglas.

**Documentación que debe acompañarlo (paquete de cierre del evento):**
- Research Package (`Cerrado`) + beat sheet (diseño conceptual) + registro de validación (checklist G5/G7) +
  decision log + nota CANON en el `EVENT-CATALOG.md` (fila del evento actualizada).

**Criterio de salida G8:** evento disparándose automáticamente en el Game desde su `GameEvent` real,
documentado, con el Catálogo actualizado. **Solo entonces** se selecciona el siguiente evento (vuelta a G1).

---

## Resumen operativo (tarjeta de sprint)
Todo sprint de producción de un evento sigue: **G1 selección → G2 research → G3 emoción → G4 beat sheet →
G5 checklist → G6 build+iterar en Studio → G7 aprobar (CANON) → G8 cablear al Game**. Una puerta a la vez, un
evento a la vez, build siempre verde, la Bible como autoridad y el Studio como laboratorio de validación.

---
*Production Pipeline — metodología oficial y permanente. Su aprobación cierra la etapa de diseño global y
abre la producción real del Pokémon TCG Clone. A partir de aquí, cada sprint produce un evento con este
proceso, sin improvisación.*
