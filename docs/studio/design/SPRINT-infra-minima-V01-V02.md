# 🧱 SPRINT · Infraestructura mínima para reproducir V0.1 y V0.2 (DISEÑO — sin implementar)

> **Objetivo:** que en el Board Simulator se pueda abrir el Studio, seleccionar V0.1 o V0.2, elegir entre
> P1 y P5, reproducir la secuencia completa, pausar, reiniciar, repetir, comparar variantes y **verificar
> el HANDOFF entre eventos**. Nada más.
>
> **Principio rector:** INFRAESTRUCTURA BAJO DEMANDA. Si una capacidad no la usa V0.1 o V0.2, **no
> pertenece a este sprint**. No hay framework, no hay anticipación, no hay "por si acaso".

---

## 0) Auditoría previa: qué existe YA (y por tanto NO se construye)
Reconstruir esto violaría el principio. **Se reutiliza tal cual**; el sprint solo añade lo que falte.

| Capacidad del objetivo | Ya resuelta por | Estado |
|---|---|---|
| Abrir el Studio / Board Simulator | `StudioShell`, `StudioWindow`, `BoardSimulatorLab` | ✅ existe |
| Seleccionar evento del catálogo | `EventPlayerState.load` + `EventPlayerPanel` | ✅ existe (hoy solo lista V0.1) |
| Elegir/comparar variantes (P1/P5) | `EventPlayerState.selectVariant` + chips del panel | ✅ existe |
| Reproducir · pausar · reiniciar · loop · velocidad | `EventPlayerState` (`togglePlay/restart/loop/cycleSpeed`) + `EventPlayerClock` | ✅ existe |
| Beats data-driven + interpolación | `EventVariant`/`Beat`/`SceneChannels`/`frameAt` | ✅ existe |
| Render de la cinemática sobre el tablero | `PlayerDrivenBoard`, `cameraChannels`, `EventOverlay` | ✅ existe |
| Marcar Canon (en memoria) | `EventPlayerState.markCanon` | ✅ existe (persistencia: fuera) |
| Geometría de zonas del tapete | `BoardGeometry` (`feature:game`) | ✅ existe (se reutiliza en V0.2) |
| 5 variantes de V0.1 | `V01_VARIANTS` | ✅ existe |

**Conclusión:** de las 9 capacidades pedidas, 8 ya están. El sprint se reduce a **4 piezas**.

---

## 1) Piezas del sprint (alcance exacto)

### C1 · Registrar V0.2 en el catálogo (+ sus 2 variantes P1/P5)
- **Por qué existe:** el objetivo pide "seleccionar V0.1 **o V0.2**". Hoy `PREP_EVENTS` solo contiene V0.1.
- **Responsabilidad:** declarar `CatalogEvent("V0.2", …)` con **solo dos** `EventVariant` (P1 «Ensamblaje
  por luz» y P5 «Enfoque del terreno»), como **datos de beats** (mismo modelo que V0.1). 5 beats cada una,
  cerrando en **B5 handoff**.
- **Problema concreto que resuelve:** hace a V0.2 seleccionable y reproducible con la infraestructura ya
  existente, sin tocar el Player.
- **Explícitamente fuera:** P2/P3/P4 (documentadas, no se implementan hasta que aporten algo que P1/P5 no
  cubran); cualquier otro evento del catálogo.

### C2 · Tapete VACÍO como superficie de reproducción (la pieza real del sprint)
- **Por qué existe:** V0.1 revela la arena y V0.2 **forma el tapete y sus zonas**; ambos ocurren sobre un
  **tapete vacío**. Las restricciones prohíben cartas/mano/banca/Active/HUD. Hoy el tablero monta la
  `CombatScreen` real **sembrada con cartas** (`ActorHostedBoard` + `CardRepository.load()`), lo que
  contradice el canon (las cartas no existen hasta E3).
- **Responsabilidad:** ofrecer, **para los eventos de framing**, una superficie mínima = **tapete vacío**
  (fondo del recinto + contornos de zona **vacíos**), usando `BoardGeometry` para las posiciones. Es el
  lienzo sobre el que `cameraChannels`/`EventOverlay` aplican la cinemática, y en V0.2 es **el propio
  objetivo animado** (los contornos de zona se materializan).
- **Problema concreto que resuelve:** que V0.1/V0.2 se reproduzcan sobre lo que el canon dice (tapete
  vacío) y no sobre un tablero con cartas/HUD que aún no deberían existir.
- **Explícitamente fuera:** cartas, mano, banca, ocupante del Active, HUD, energías, marcador de premios,
  y todo el andamiaje de `CombatScreen` (actores/ToolController/EvolveBridge/seed) para la **ruta de
  framing**. (Ese andamiaje **no se borra**: sigue sirviendo a otras funciones del Studio; simplemente la
  ruta de framing **no depende de él**.)
- **Decisión de diseño a validar (abajo, §3-A).**

### C3 · Canales visuales que exige V0.2 (solo los que P1/P5 usen)
- **Por qué existe:** los `SceneChannels` actuales se diseñaron para el cruce menú→arena de V0.1. Hay que
  comprobar, canal a canal, si bastan para V0.2 y añadir **únicamente** los que P1/P5 demanden.
- **Responsabilidad / análisis:**
  - **P5 «Enfoque del terreno»** reutiliza canales existentes: `arenaAlpha`, `arenaBlur` (rack-focus) y
    `bloom`. **No añade canales.** (Y **no** usa `lightBand`, que es un **barrido** horizontal: P5 es
    no-barrido por diseño.)
  - **P1 «Ensamblaje por luz»** necesita expresar: (a) **acento del eje central** primero, (b) **dibujado
    progresivo de los contornos de zona**, (c) **materialización del tapete**. Esto **no** se puede
    expresar con los canales actuales sin abusar de `lightBand`. Se propone añadir **los mínimos**:
    `centerAccent` (0..1), `zoneReveal` (0..1) y `matPresence` (0..1). Tres floats, nada más.
- **Problema concreto que resuelve:** dar a P1 un vocabulario visual honesto sin inventar canales para
  eventos futuros.
- **Explícitamente fuera:** cualquier canal que P1/P5 no consuman (p. ej. efectos de energía, de carta, de
  cámara libre, de partículas complejas más allá del `particles` ya existente).

### C4 · Verificación del HANDOFF V0.1 → V0.2 (encadenado mínimo)
- **Por qué existe:** el objetivo pide **verificar el handoff entre eventos**. Hoy el Player carga **un**
  evento aislado; el estado inicial de cada variante se reinicia a `SceneStart`, así que la costura
  V0.1→V0.2 no se puede observar.
- **Responsabilidad:** una capacidad mínima de **reproducción encadenada de dos eventos** — reproducir la
  variante elegida de V0.1 y, al terminar, **continuar** en la variante elegida de V0.2 **sin reiniciar el
  estado visual** (la B5 de V0.1 debe empalmar con la B1 de V0.2). Sirve para **juzgar la costura**.
- **Problema concreto que resuelve:** poder responder "¿el handoff es limpio?" mirándolo, no deduciéndolo.
- **Explícitamente fuera:** un secuenciador/timeline general de todos los eventos; guardar secuencias;
  encadenar más de estos dos; transición automática dirigida por el motor (eso es el Director, futuro).
- **Contrato que habilita (mínimo):** que el **estado visual final** de una variante sea legible como
  **estado inicial** de la siguiente (hoy `frameAt` asume `from = SceneStart`). Es el único punto donde el
  Player se toca.

---

## 2) Simplificaciones y exclusiones que propongo (para reducir alcance)
- **No** tocar el transporte: ya cubre play/pause/restart/loop/speed/compare/canon. Cero trabajo ahí.
- **No** persistir Canon ni parámetros: comparar es en-sesión. Se mantiene la nota "Guardar: futura".
- **No** construir un secuenciador: C4 es **solo** el par V0.1→V0.2, no un timeline.
- **No** reutilizar `CombatScreen` con cartas para framing: forzar a "vacío" una pantalla que arrastra
  HUD/mano/banca/actores es **más** trabajo (y más acoplamiento) que un tapete vacío mínimo. Recomiendo el
  tapete vacío dedicado (§3-A).
- **Reutilizar** `BoardGeometry` para las zonas de C2: no se define geometría nueva.

## 3) Decisiones (RESUELTAS)
**A) Tapete vacío de C2 → A1 APROBADA: superficie mínima dedicada.** Independiente de `CombatScreen`.
  Su **única responsabilidad**: representar el campo vacío. Se limita **estrictamente** a: fondo · geometría
  del tapete (vía `BoardGeometry`) · contornos de zonas vacías · las capas necesarias para reproducir
  V0.1/V0.2. **No** puede convertirse en un "segundo tablero": sin lógica de juego, sin HUD, sin componentes
  "por si acaso". Fundamento: separa **escenario** (espacio físico del duelo) de **partida** (estado
  jugable) — principio permanente elevado a canon (EVENT-CATALOG · Canon transversal).

**B) Handoff de C4 → APROBADO: encadenado manual mínimo.** Solo: un botón "Encadenar V0.1→V0.2" ·
  reproducción automática de ambos · preservación del estado visual en la costura · poder observar/validar
  la costura. **Nada más.** Un secuenciador general nacerá cuando existan varios eventos consecutivos con
  necesidad real (no antes).

**C) Canales de C3 → APROBADO: +3 para P1, 0 para P5.** P1 añade exactamente `centerAccent`, `zoneReveal`
  y `matPresence` (cada uno usado directamente por P1). P5 se construye **solo** con canales existentes
  (`arenaBlur`, `bloom`, …) → prueba de que el sistema ya tiene expresividad para una dirección distinta
  sin ampliarse. Regla permanente asociada (un canal solo nace cuando una propuesta no puede expresarse con
  los existentes) elevada a canon.

---

## 4) Lo que NO entra en el sprint (recordatorio del canon)
cartas · mano · banca · Active · HUD completo · energías · Coin Flip · Mulligan · Setup · turnos · IA ·
red · animaciones de eventos futuros · herramientas genéricas "por si acaso". Todo eso nace cuando un
evento concreto (E3, E6, E2…) lo exija.

---

## 5) Resumen del alcance
**4 piezas:** C1 registrar V0.2 (datos) · C2 tapete vacío (único render nuevo) · C3 +3 canales para P1 ·
C4 encadenado mínimo V0.1→V0.2. **8 de 9 capacidades ya existen.** El sprint es pequeño **a propósito**:
esa es la prueba de que el principio de Infraestructura Bajo Demanda se está respetando.
