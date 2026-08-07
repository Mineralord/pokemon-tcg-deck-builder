# DMI — Documento Maestro de Implementación (versión canónica)

**Fase Puente (Diseño → Desarrollo).** No es la "Fase 12": no introduce mecánicas de juego ni decisiones
de diseño. Es el **contrato oficial entre el diseño congelado (Fases 0–11) y el desarrollo**. A partir de
su aprobación, toda implementación sigue este documento.

> **Qué NO hace este documento.** No diseña funcionalidades, no modifica decisiones, no escribe código, no
> crea arquitectura nueva. **Transforma el canon existente en un plan de construcción.**

Anclaje en el repositorio real (módulos Gradle actuales, `settings.gradle.kts`):
`engine:{model,events,effects,rules}` · `data:{gacha,cards,profile,cloud,netplay,netfirestore}` ·
`core:{animation,animation-compose,designsystem}` · `feature:{packs,decks,game}` · `app`.

---

## 1. Objetivos generales
1. Implementar el **núcleo compartido** (engine + renderer + core + assets) una sola vez, y construir
   **sobre él** tanto el **Juego del jugador (APK 1)** como el **Pokémon TCG Studio (APK 2)**.
2. Preservar durante todo el desarrollo la **Arquitectura Dual**, la **Regla de Oro** (un motor, un
   renderer, una validación) y el **modelo de recursos por ID estable**.
3. Materializar cada pantalla **por composición** de ComponentStyle existentes, sin ampliar el Design
   System.
4. Entregar valor de forma **incremental y verificable** (hitos con criterios de aceptación), minimizando
   retrabajo.

## 2. Principios de implementación (reglas oficiales)
- **Nunca romper el canon.** Las Fases 0–11 son inmutables; discrepancia código↔canon = el código está
  mal.
- **Nunca duplicar lógica.** Una regla existe en un único lugar (engine); nadie la reimplementa.
- **Nunca crear motores alternativos.** Studio y Juego consumen el **mismo** `engine:rules`.
- **Nunca crear renderers alternativos.** Todo preview/tablero monta el **renderer oficial**
  (`core:animation-compose` + render de carta de `core:designsystem`).
- **Toda funcionalidad nueva pasa por auditoría** de suficiencia del Design System y de Regla de Oro
  (proceso canónico, `00-PROCESO.md`).
- **Toda integración preserva los ID estables.** Los recursos se referencian por ID; jamás se copian.
- **Composición sobre creación:** ningún componente/token/principio nuevo sin fase canónica que lo
  justifique.
- **El Build & Validation Lab prepara y valida; no compila** (DC-2). La compilación física del APK es un
  paso externo.

## 3. Mapa de dependencias (grafo real + capas de diseño)

### 3.1 Dependencias verificadas hoy (código)
```
engine:model        ── (fundación, Kotlin puro, sin deps de proyecto)
engine:events       ── engine:model
engine:effects      ── engine:model, engine:events
engine:rules        ── engine:model, engine:events, engine:effects        ← MOTOR ÚNICO
data:cards          ── engine:model
data:profile/gacha/cloud/netplay/netfirestore ── (capa de datos)
core:animation      ── (fundación de animación, Kotlin puro JVM, independiente)
core:animation-compose ── core:animation                                   ← RENDERER (Compose)
core:designsystem   ── (Compose; ComponentStyle → Visual Tokens → Theme)
feature:game        ── core:designsystem, engine:rules, data:cards, data:profile, data:netplay
feature:packs, feature:decks ── (features del juego)
app                 ── designsystem, feature:{packs,decks,game}, data:*, engine:{model,rules}  ← APK 1
```

### 3.2 Módulos de diseño aún no materializados (Studio, APK 2)
El Studio (Shell + 9 Labs, Fase 11) está **diseñado pero no implementado**. Se construirá como capa
`feature:studio*` + `app-studio`, **reutilizando** `engine:rules` (motor), `core:animation-compose` +
`core:designsystem` (renderer/UI) y el registro de recursos por ID — **sin** crear engine/renderer
propios. Esto es lo que garantiza la Regla de Oro en la práctica.

### 3.3 Qué puede ir en paralelo y qué es obligatoriamente secuencial
- **Ruta crítica (secuencial):** `engine:model → events → effects → rules`. Nada de reglas/combate/Studio
  avanza sin ella.
- **Paralelizable desde el inicio** (no dependen del engine): `core:animation` → `core:animation-compose`;
  `core:designsystem` (ComponentStyle). Renderer y Design System pueden madurar en paralelo al engine.
- **Paralelizable tras `engine:model`:** `data:cards` (solo necesita model) y demás `data:*`.
- **Punto de convergencia:** `feature:game` requiere **engine:rules + designsystem + data** ya estables.
- **Studio:** requiere engine + renderer + designsystem + modelo de recursos por ID; sus Labs son
  **paralelizables entre sí** una vez existe el Shell y los contratos (host + ID).

## 4. Orden oficial de construcción (justificado)
El orden de **diseño** (Shell→Labs) **no** es el orden de **implementación**. Se implementa de la
**fundación compartida hacia arriba**, porque el mayor riesgo y el mayor coste de retrabajo están en el
núcleo: si el engine o el renderer cambian tarde, invalidan todo lo construido encima.

**O1 · Fundaciones puras (paralelas).**
`engine:model` + `engine:events` | `core:animation` | `core:designsystem` (tokens/ComponentStyle).
*Por qué primero:* sin dependencias de proyecto; son el sustrato de todo; estabilizarlos elimina el
retrabajo aguas arriba.

**O2 · Motor y renderer.**
`engine:effects` → `engine:rules` (motor único) | `core:animation-compose` (renderer).
*Por qué:* es la Regla de Oro hecha código; todo lo demás los **consume**, no los duplica.

**O3 · Datos y recursos por ID.**
`data:cards` (+ registro de recursos con ID estable) y `data:profile/gacha/cloud`.
*Por qué:* el modelo de ID estable debe existir antes de que cualquier Lab o feature referencie recursos.

**O4 · Juego del jugador (APK 1) — vertical mínima jugable.**
`feature:game` (+ `feature:packs`, `feature:decks`) → `app`.
*Por qué antes que el Studio:* valida el núcleo con el consumidor **más exigente** (partida real,
render real, datos reales); si el núcleo sirve al juego, sirve al Studio.

**O5 · Shell del Studio (APK 2) + contratos.**
`app-studio` + Shell (11.1): host del Workspace, estado global, navegación, contrato de montaje + ID.
*Por qué:* es el andamiaje que todos los Labs necesitan; se construye una vez.

**O6 · Labs sobre el núcleo ya probado (paralelizables).**
Orden recomendado por dependencia de datos: **bibliotecas → editores → prueba → QA → build**:
Animation Gallery (11.2) + Animation Editor (11.3) → Effect Editor (11.4) → Card Gallery (11.5) + Card
Editor (11.6) → Combat Lab (11.7) → Deck Sandbox (11.9) → QA & Regression Lab (11.8) → Build & Validation
Lab (11.10).
*Por qué este orden:* cada Lab consume artefactos del anterior por ID; el Combat Lab necesita cartas/
efectos/animaciones reales; QA necesita ejecuciones del Combat Lab; Build cierra verificando todo el
grafo.

**O7 · Networking y online.** `data:netplay` → `data:netfirestore` integrados en `feature:game`.
*Por qué al final del bloque de juego:* PvP se apoya sobre un combate local ya correcto y determinista.

## 5. Hitos verificables

### H0 — Fundaciones estables
- **Objetivo:** model/events, animación base y Design System (ComponentStyle) compilando y probados.
- **Entregables:** `engine:model`, `engine:events`, `core:animation`, `core:designsystem` con tests.
- **Aceptación:** compila todo; ComponentStyle consume la cadena `→ Visual Tokens → Theme`; sin literales
  hardcodeados; tests de model/eventos verdes.
- **Dependencias:** ninguna. **Riesgos:** deriva de tokens; sobre-ingeniería del engine.

### H1 — Motor único + Renderer único
- **Objetivo:** `engine:rules` (con `effects`) ejecuta reglas de forma determinista; renderer oficial
  dibuja carta/tablero.
- **Entregables:** `engine:effects`, `engine:rules`, `core:animation-compose`; RNG determinista con
  semilla (seam neutral).
- **Aceptación:** una partida scriptada produce el mismo resultado con la misma semilla; el renderer monta
  una carta real; **no existe** segundo motor/renderer.
- **Dependencias:** H0. **Riesgos:** acoplar reglas al render; falta de determinismo.

### H2 — Recursos por ID estable
- **Objetivo:** registro de recursos (cartas/efectos/animaciones/sonidos) con ID estable e inmutable.
- **Entregables:** `data:cards` + modelo de referencias por ID; validaciones oficiales expuestas por el
  motor.
- **Aceptación:** ningún recurso se referencia por copia; resolver por ID funciona; IDs inmutables.
- **Dependencias:** H0–H1. **Riesgos:** IDs inestables; duplicación temprana.

### H3 — Juego del jugador jugable (APK 1)
- **Objetivo:** vertical mínima: mazo → combate local → resultado, con render y datos reales.
- **Entregables:** `feature:game` (+ packs/decks) montado en `app`.
- **Aceptación:** una partida completa PvE local con el motor y renderer reales; sin lógica duplicada en
  la UI.
- **Dependencias:** H1–H2. **Riesgos:** lógica de reglas filtrándose a la capa de UI.

### H4 — Shell del Studio (APK 2)
- **Objetivo:** contenedor permanente (11.1) con contrato de host + navegación + estado global.
- **Entregables:** `app-studio` + Shell; Command Palette, Rail, host R3, Status Bar (composiciones).
- **Aceptación:** monta un Lab vacío por contrato; restaura sesión; **cero** componentes nuevos.
- **Dependencias:** H0 (+ H1/H2 para previews reales). **Riesgos:** que el Shell conozca el interior de un
  Lab (acoplamiento).

### H5 — Labs del Studio (incremental)
- **Objetivo:** implementar los 9 Labs por composición, en el orden O6.
- **Entregables:** 11.2–11.10 montados en el host, referenciando recursos por ID.
- **Aceptación por Lab:** cumple su doc de arquitectura; usa motor/renderer compartidos; sin componentes/
  tokens/principios nuevos; navegación por ID a otros Labs funcionando.
- **Dependencias:** H4 (+ artefactos del Lab previo). **Riesgos:** un Lab invadiendo responsabilidades de
  otro; validación paralela.

### H6 — Online / PvP
- **Objetivo:** partida en red sobre el combate determinista.
- **Entregables:** `data:netplay` + `data:netfirestore` integrados en `feature:game`.
- **Aceptación:** dos clientes convergen al mismo estado con la misma semilla/eventos; sin motor especial
  de red.
- **Dependencias:** H3. **Riesgos:** divergencia de estado; lógica de reglas en el transporte.

### H7 — Verificación global del proyecto
- **Objetivo:** el Build & Validation Lab (11.10) verde sobre todo el grafo de recursos.
- **Entregables:** reporte técnico sin errores (referencias, IDs, huérfanos, faltantes, metadatos,
  versiones).
- **Aceptación:** integridad global validada; **preparar build** OK; compilación física = paso externo.
- **Dependencias:** H2–H5. **Riesgos:** hallazgos acumulados si H7 se deja para el final (mitigado: correr
  Build continuamente desde H2).

## 6. Estrategia de integración continua
- **Grafo Gradle como CI:** cada módulo compila y testea de forma aislada; la ruta crítica
  (`model→events→effects→rules`) se valida en cada cambio.
- **Barreras de arquitectura automatizadas:** verificar que ningún módulo de UI/feature/Studio dependa de
  algo que reimplemente reglas; que Studio dependa de `engine:rules`/renderer y **no** de duplicados.
- **Auditoría de literales:** ComponentStyle no contiene literales de estilo (consumo por la cadena).
- **Determinismo en CI:** una suite de partidas scriptadas con semilla fija que debe reproducir resultados
  byte-a-byte.

## 7. Estrategia de validación
- **Única fuente de validación: el motor.** Toda legalidad (mazo, jugada, evolución) se consulta a
  `engine:rules`. Ni Studio ni UI validan por su cuenta (Deck Sandbox 11.9, Build 11.10 solo consultan).
- **Build & Validation Lab** como control de integridad del grafo de recursos (solo informa; corrige el
  Lab de origen).
- **QA & Regression Lab** como oráculo: Snapshots (estado) y Replays (eventos) fijan comportamiento
  histórico correcto.

## 8. Estrategia de pruebas
- **Unitarias** en engine (model/events/effects/rules): reglas, efectos, RNG determinista.
- **De datos** (`data:cards`): mapeo/mapper, integridad de IDs (ya existen `CardRepositoryTest`,
  `StarterDecksTest`).
- **De contrato** (netplay): round-trip de mensajes (ya existe `NetplayRoundTripTest`).
- **De regresión visual/funcional:** Replays reproducidos deben coincidir con su oráculo; Snapshots
  restaurados deben validar igual.
- **De composición (Studio):** cada Lab se prueba montándose en el host y resolviendo referencias por ID.

## 9. Estrategia de migraciones
- **Datos por ID estable:** las migraciones **nunca cambian IDs**; migran metadatos/estructura alrededor
  del ID.
- **Versionado explícito:** cada recurso guarda versión de motor/renderer (ya en Snapshots/Replays);
  migraciones convierten versiones antiguas a nuevas de forma verificable.
- **Migración hacia arriba, best-effort:** lo irrecuperable se descarta con aviso (patrón de restauración
  del Shell), nunca corrompe el estado.

## 10. Estrategia de preservación
- **QA & Regression Lab** es la memoria histórica: cualquier combate se preserva como Snapshot/Replay por
  ID, reproducible exactamente con su semilla/versión.
- **Assets y recursos** se preservan por ID inmutable; evolucionar un recurso no rompe referencias
  históricas.
- **Documentación canónica** (`docs/developer-studio/*`) es la fuente de verdad de diseño; el DMI la
  traduce a construcción.

## 11. Estrategia de compatibilidad retroactiva
- **IDs estables = contrato retroactivo:** un ID nunca se reutiliza ni se reasigna; los recursos antiguos
  siempre resuelven.
- **Versión de motor/renderer registrada:** un Replay antiguo indica con qué versión debe interpretarse;
  incompatibilidades se marcan (no se ejecutan a ciegas).
- **Cambios de reglas → auditados y versionados:** un cambio en `engine:rules` que altere comportamiento
  histórico se detecta por regresión (Replays) antes de romper compatibilidad.
- **Design System congelado:** la UI evoluciona por composición; los ComponentStyle no rompen pantallas
  existentes.

## 12. Riesgos técnicos

| # | Riesgo | Impacto | Prob. | Mitigación |
|---|--------|---------|-------|------------|
| R1 | **Lógica de reglas filtrada a UI/Studio** (motor duplicado de facto) | Alto | Media | Barreras de dependencia en CI; toda validación vía `engine:rules`; auditoría por Lab. |
| R2 | **Acoplar reglas al renderer** (romper Regla de Oro) | Alto | Media | Separación `engine` (JVM puro) ↔ `core:animation-compose`; el engine no conoce Compose. |
| R3 | **No-determinismo del RNG** | Alto | Media | Seam determinista con semilla; suite de partidas scriptadas en CI (H1). |
| R4 | **IDs inestables o reasignados** | Alto | Baja | ID inmutable por contrato; tests de integridad; Build Lab detecta duplicados/huérfanos. |
| R5 | **Cambio tardío en engine/renderer** invalida trabajo encima | Alto | Media | Orden fundación-primero (O1–O2); congelar H1 antes de escalar features/Labs. |
| R6 | **Un Lab invade responsabilidades de otro** | Medio | Media | Docs 11.x como contrato; Build solo-informa; revisión de cohesión por Lab. |
| R7 | **Divergencia de estado en PvP** | Alto | Media | Combate determinista local primero (H3); red transporta eventos/semilla, no reglas (H6). |
| R8 | **Deriva del Design System** (componente nuevo "de urgencia") | Medio | Media | Regla: sin componentes nuevos sin fase canónica; gráficos = composición (Table/Progress/Badge). |
| R9 | **Migraciones que tocan IDs** | Alto | Baja | Prohibido cambiar IDs; migrar alrededor del ID; versionado explícito. |
| R10 | **Assets faltantes/incompatibles** en build | Medio | Media | Build & Validation Lab corriendo continuamente desde H2, no solo al final. |

---

## Auditoría final

Verificación explícita de coherencia:
- **Con todas las fases cerradas (0–11):** ✔ el DMI **no** altera ninguna decisión; solo ordena su
  construcción. El orden de implementación (fundación→arriba) es una decisión de *proceso*, no de diseño.
- **Con el Studio:** ✔ Shell + 9 Labs se implementan por composición sobre el núcleo compartido; el orden
  O5–O6 respeta los contratos (host + ID) de la Fase 11.
- **Con la Arquitectura Dual:** ✔ un único núcleo compartido; APK 1 (juego) y APK 2 (Studio) construidos
  encima; ningún Lab pertenece al juego.
- **Con la Regla de Oro:** ✔ un motor (`engine:rules`), un renderer (`core:animation-compose` + render de
  carta), una validación (consultada al motor); prohibidos alternativos y simulaciones.
- **Con el modelo de recursos por ID estable:** ✔ H2 lo establece antes de cualquier consumidor;
  migraciones y compatibilidad se anclan en IDs inmutables.
- **Ausencia de contradicciones:** ✔ el grafo de dependencias del DMI coincide con el grafo Gradle real;
  no reordena responsabilidades ni introduce módulos que dupliquen motor/renderer.

**NO EXISTE NINGUNA CONTRADICCIÓN.**

## Dictamen — DMI aprobado

Se declara **oficialmente aprobado el Documento Maestro de Implementación** e incorporado al canon como
**Fase Puente (Diseño → Desarrollo)** — no como Fase 12, por no introducir mecánicas de juego.

### Por qué el proyecto está preparado para iniciar el desarrollo
1. **Diseño completo y congelado** (Fases 0–11, auditadas globalmente sin ERROR DE ARQUITECTURA): no hay
   decisiones abiertas que bloqueen la construcción.
2. **Grafo de dependencias real y sano:** ruta crítica clara (`model→events→effects→rules`), fundaciones
   paralelizables, punto de convergencia identificado (`feature:game`), Studio sobre el mismo núcleo.
3. **Orden fundación-primero** que minimiza retrabajo: el mayor riesgo (engine/renderer) se estabiliza y
   congela (H1) antes de construir features y Labs encima.
4. **Hitos verificables con criterios de aceptación** y control de integridad continuo (Build Lab desde
   H2), de modo que el desarrollo nunca pregunta "¿qué hacemos ahora?": el orden, las dependencias y los
   criterios ya están fijados.
5. **Invariantes de seguridad a décadas:** un motor, un renderer, una validación e ID estable — los cuatro
   pilares que impiden la fragmentación más cara de mantener.

**El Documento Maestro de Implementación queda listo para regir el desarrollo del Pokémon TCG Clone.**
