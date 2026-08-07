# Hito H0 — Fundaciones · Plan Oficial de Implementación

**Fase:** Desarrollo (rige el DMI, `../DMI-DOCUMENTO-MAESTRO-IMPLEMENTACION.md`).
**Naturaleza de H0:** auditoría + consolidación, **no** greenfield. Los cuatro módulos base **ya existen y
están desarrollados**; H0 verifica que sean correctos y los deja congelados como base sólida. **No se
escribe código en este documento**; el resultado es el plan. Cada tarea (H0-T1…H0-T4) se implementará en
su propia conversación, con pruebas y criterios superados antes de pasar a la siguiente.

Alcance H0 (DMI): **`engine:model` · `engine:events` · `core:animation` · `core:designsystem`.** No
avanzar a otros módulos.

---

## Hallazgo de contexto (decisivo)
El repositorio **no es greenfield**: contiene un juego ya funcional (motor con ~44 fases de efectos del set
151, netplay, `feature:game`, `app`) y un framework de animación v1.0 estable. El DMI es el plan *idealizado
hacia adelante*; el código llegó por una trayectoria orgánica anterior (la numeración "Fase N" del napkin
es **distinta** de las Fases canónicas 0–11). Por eso H0 = **consolidar** lo existente contra el canon, no
crearlo. Esto coincide con el orden fundación-primero del DMI: estabilizar y congelar la base antes de
tocar nada aguas arriba.

## Verificaciones de canon ya ejecutadas (base del dictamen)
- **Layering de la fundación limpio:** `engine:model` **no importa** nada de `events`/`effects`/`rules`
  (verificado: 0 imports). Es una fundación pura sin dependencias circulares. ✔
- **Grafo real = grafo DMI:** `events → model`; `effects → model,events`; `rules → model,events,effects`;
  `data:cards → model`; `animation-compose → animation`; `designsystem → model` (+Compose). Coincide con
  §3.1 del DMI. ✔
- **Separación datos/intérprete:** `engine:effects` contiene **solo** `EffectInterpreter` (la lógica); las
  *data classes* `Effect`/`EffectOp` y el registro `EffectsDb` viven en `engine:model` como **datos**. Es
  consistente con el DMI (effects = intérprete que depende de model). Se anota como nuance a vigilar, no
  como contradicción. ⚠ (vigilancia, §Riesgos R-H0-1)
- **Un motor / un renderer:** no hay motores ni renderers alternativos; `engine:rules` es el único motor;
  el render de carta vive en `designsystem` + `core:animation-compose` (Regla de Oro intacta). ✔
- **core:animation NO está versionado en git** (untracked). Existe en disco y es estable, pero fuera de
  control de versiones. Es un problema de **higiene/reproducibilidad**, no de canon. ⚠ (§Riesgos R-H0-2)

---

## Estado por módulo

### 1) `engine:model` — Kotlin puro (JVM)
- **Estado actual:** existe y activo. Fuentes: `Card.kt`, `Common.kt`, `GameState.kt`, `PendingDecision.kt`,
  `Effects.kt`, `EffectsDb.kt`, `EffectKeys.kt`, `Set151Effects.kt`, `AcademiaDecksEffects.kt`. Tests:
  `ModelTest`, `EffectsDbTest`. Hay ficheros **sin commitear** (`EffectKeys.kt`, `AcademiaDecksEffects.kt`)
  y varios modificados.
- **Cumplimiento DMI:** ALTO. Fundación pura, sin deps de proyecto, sin imports de capas superiores; ID
  estable de cartas ya es el contrato (`sv3pt5-39`, etc.).
- **Dependencias:** ninguna (raíz del grafo).
- **Tareas pendientes:** confirmar que compila y tests verdes; versionar lo untracked; documentar la
  frontera "model = datos + estado; nunca reglas/interpretación"; decidir (sin ejecutar) si `EffectsDb` +
  registros de set deben permanecer en model o migrar a `effects` (decisión de layering, no de H0).
- **Riesgos:** R-H0-1 (frontera datos/lógica de efectos).
- **Prioridad:** **P0** (todo depende de model).

### 2) `engine:events` — Kotlin puro (JVM)
- **Estado actual:** existe. Fuentes: `GameEvent.kt`, `CombatLog.kt`. Test: `CombatLogTest`.
- **Cumplimiento DMI:** ALTO. Depende solo de `model` (verificado). `GameEvent` es la base de la
  cronología que consumirán Replays (11.8) y el renderer.
- **Dependencias:** `engine:model`.
- **Tareas pendientes:** compilar + tests verdes; confirmar que `GameEvent` es el **único** canal de
  eventos del motor (sin canales paralelos); verificar exhaustividad del `when` de `CombatLog`.
- **Riesgos:** bajo.
- **Prioridad:** **P0**.

### 3) `core:animation` — Kotlin puro (JVM), independiente
- **Estado actual:** existe y es **v1.0 estable** (24 fuentes; pipeline Director→Scheduler→Queue→Runner→
  Player→Steps; 5 tests verdes por napkin). **NO versionado en git** (untracked completo).
- **Cumplimiento DMI:** ALTO en diseño (independiente del engine y de la UI, solo coroutines). Marcado
  "ESTABLE, NO MODIFICAR" en el runbook.
- **Dependencias:** ninguna de proyecto (`kotlinx-coroutines`).
- **Tareas pendientes:** **versionar el módulo en git** (crítico); confirmar compilación + tests verdes en
  limpio; verificar que no depende de Android/Compose/engine.
- **Riesgos:** R-H0-2 (sin control de versiones → irreproducible / pérdida).
- **Prioridad:** **P0** para versionado; **P2** para lo demás (ya estable).

### 4) `core:designsystem` — Android/Compose
- **Estado actual:** existe, extenso (41 fuentes). Dos familias conviven: (a) **ComponentStyle/tokens**
  del Studio (`tokens/`: ComponentStyle, ContainerStyle, DataDisplayStyle, FeedbackStyle, InspectorStyle,
  NavigationStyle, OverlayStyle, PreviewStyle, TimelineStyle + Studio*Tokens + StudioTheme); (b) **render
  de carta del juego** (`HoloCardImage`, `tilt/`, `CardDetailDialog`, `DeckBox`, `TypeEmblem`, `motion/`).
  Varios untracked/modificados. Sin carpeta `src/test`.
- **Cumplimiento DMI:** ALTO y **coherente con canon**: el DMI §2 sitúa el *render de carta* en
  designsystem, y la cadena `ComponentStyle → Visual Tokens → Theme` está materializada (Fase 10). Ambas
  familias son legítimas aquí.
- **Dependencias:** `engine:model` (datos de carta) + Compose BOM. Coherente.
- **Tareas pendientes:** compilar; verificar que **ningún** componente lee Visual Tokens directamente
  (consumo por ComponentStyle); confirmar ausencia de literales de estilo hardcodeados; versionar lo
  untracked; confirmar que el render de carta es el **único** (Regla de Oro), sin duplicado.
- **Riesgos:** R-H0-3 (deriva de tokens / literal hardcodeado); R-H0-4 (mezcla de responsabilidades juego
  vs Studio en un mismo módulo — aceptable por canon, pero a vigilar).
- **Prioridad:** **P1** (renderer/UI; no bloquea el engine pero sí a features/Studio).

---

## Dictamen de contradicciones

**NO EXISTE ERROR DE IMPLEMENTACIÓN.**

Ninguno de los cuatro módulos base contradice el canon: no hay motor alternativo, no hay renderer
alternativo, no hay validación paralela, no se rompe el ID estable ni la Arquitectura Dual, y el grafo de
dependencias real coincide con el del DMI. Los dos puntos marcados (frontera datos/lógica de efectos en
model; `core:animation` sin versionar) son **cuestiones de layering e higiene**, no contradicciones de
canon; se gestionan como riesgos y tareas de consolidación dentro de H0, sin detener el hito.

---

## Plan oficial H0 (tareas pequeñas, independientes, verificables)

> **Pre-flight (gate común a todas):** partir de un árbol que compile. La **primera acción** de cada tarea
> es ejecutar el build/test del módulo en limpio y registrar el baseline. Ninguna tarea introduce
> funcionalidad nueva: solo consolida, versiona, prueba y documenta la frontera.

### H0-T1 — Consolidar `engine:model`
- **Objetivo:** dejar `engine:model` compilando, con tests verdes, todo versionado, y su frontera
  ("datos + estado; nunca reglas") documentada.
- **Módulos afectados:** `engine:model`.
- **Dependencias:** ninguna. **(Tarea inicial.)**
- **Criterios de aceptación:** `:engine:model:test` verde; 0 imports de `events/effects/rules`; ficheros
  untracked del módulo versionados; nota de frontera añadida; decisión registrada (sin ejecutar) sobre la
  ubicación de `EffectsDb`/registros de set.
- **Pruebas necesarias:** `ModelTest`, `EffectsDbTest` (existentes) verdes; verificación estática de que
  model no depende de capas superiores.

### H0-T2 — Consolidar `engine:events`
- **Objetivo:** `engine:events` compilando, tests verdes, confirmado como único canal de eventos del motor.
- **Módulos afectados:** `engine:events`.
- **Dependencias:** H0-T1 (usa `model`).
- **Criterios de aceptación:** `:engine:events:test` verde; `GameEvent` es el único tipo de evento del
  motor; `when` de `CombatLog` exhaustivo; solo depende de `model`.
- **Pruebas necesarias:** `CombatLogTest` (existente) verde; revisión de exhaustividad del sellado.

### H0-T3 — Consolidar `core:animation`
- **Objetivo:** **versionar** el módulo (hoy untracked) y confirmar su estabilidad e independencia.
- **Módulos afectados:** `core:animation`.
- **Dependencias:** ninguna técnica (paralelizable con T1/T2); se ordena tras T2 por foco secuencial.
- **Criterios de aceptación:** módulo bajo control de versiones (fuentes + `build.gradle.kts` + tests);
  `:core:animation:test` verde en limpio; 0 dependencias de Android/Compose/engine; se preserva "ESTABLE,
  NO MODIFICAR" (sin cambios funcionales).
- **Pruebas necesarias:** los 5 tests existentes (Director/Player/Queue/Scheduler/PolicyArbiter) verdes.

### H0-T4 — Consolidar `core:designsystem`
- **Objetivo:** confirmar la cadena de consumo `ComponentStyle → Visual Tokens → Theme`, sin literales
  hardcodeados, con render de carta único; versionar lo pendiente.
- **Módulos afectados:** `core:designsystem`.
- **Dependencias:** H0-T1 (usa `model`).
- **Criterios de aceptación:** `:core:designsystem` compila; ningún componente lee Visual Tokens
  directamente (consumo vía ComponentStyle); 0 literales de estilo hardcodeados en componentes; render de
  carta único (sin duplicado); untracked versionado.
- **Pruebas necesarias:** compilación Android del módulo; auditoría estática de literales y de consumo de
  tokens (checklist); (si aplica) verificación de que `StudioTheme` provee la cadena.

---

## Riesgos del Hito H0

| # | Riesgo | Impacto | Prob. | Mitigación |
|---|--------|---------|-------|------------|
| R-H0-1 | Frontera difusa **datos vs lógica** de efectos en `engine:model` (EffectsDb/registros de set) | Medio | Media | Documentar la frontera en T1; decisión explícita (sin ejecutar) sobre migrar EffectsDb a `effects`; el DMI ya admite datos en model. No bloquea. |
| R-H0-2 | `core:animation` **sin versionar** → irreproducible / riesgo de pérdida | Alto | Alta | T3 lo versiona como primera acción; verificar build limpio desde git. |
| R-H0-3 | Deriva del Design System: **literal de estilo hardcodeado** o lectura directa de Visual Tokens | Medio | Media | T4 audita consumo por ComponentStyle y literales; regla canónica ya congelada. |
| R-H0-4 | Un mismo módulo (`designsystem`) alberga juego + Studio | Bajo | Media | Aceptable por DMI §2; solo vigilar cohesión; no fusionar responsabilidades nuevas. |
| R-H0-5 | El código mid-flight (efectos 151, animación) **cambia** durante la consolidación | Medio | Media | H0 es solo-consolidación: **no** añadir funcionalidad; congelar cada módulo al cerrar su tarea. |

---

## Auditoría final del plan
- **Coherencia con fases cerradas (0–11):** ✔ el plan no altera diseño; solo consolida módulos existentes.
- **Coherencia con el Studio:** ✔ `designsystem` (ComponentStyle) es la base que el Studio consumirá; H0
  la deja verificada.
- **Coherencia con Arquitectura Dual:** ✔ base compartida; ningún módulo H0 pertenece a un solo APK.
- **Coherencia con Regla de Oro:** ✔ un motor (`rules`, fuera de H0 pero base verificada), un renderer
  (designsystem+animation-compose), sin alternativos.
- **Coherencia con ID estable:** ✔ el ID de carta de `model` es el contrato; no se toca.
- **Ausencia de contradicciones:** ✔ ninguna; grafo real = grafo DMI.

**Plan H0 aprobado.** La primera tarea a implementar en la siguiente conversación es **H0-T1 — Consolidar
`engine:model`**. Al superar sus criterios y pruebas, se marca completada y se pasa a H0-T2.
