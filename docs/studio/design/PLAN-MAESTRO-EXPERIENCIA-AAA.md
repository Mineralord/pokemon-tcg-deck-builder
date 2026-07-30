# 🎬 PLAN MAESTRO · Experiencia AAA de Partida — Reemplazo Integral

> **Hoja de ruta OFICIAL y PERMANENTE** para transformar el 100 % de la experiencia visual de una partida
> de Pokémon TCG en una experiencia AAA **coherente de principio a fin**. No es una animación ni un parche:
> es la **dirección completa** de la experiencia del jugador.
>
> **Este documento no implementa nada.** Es diagnóstico + dirección + plan. Toda implementación posterior
> se rige por él. Respeta el canon existente (`EVENT-CATALOG.md`): IDs permanentes · un foco visual por
> beat · HANDOFF · escenario ⟂ partida · infraestructura bajo demanda · nacimiento de canales bajo demanda
> · dirección de dependencias Studio→Game · Studio = laboratorio de validación.

---

## 1) Diagnóstico completo de la experiencia actual

**Fortalezas (cimientos sólidos, se conservan):**
- **Motor único** (`engine:rules`) y **pantalla única** (`CombatScreen`), ya refactorizada como **compositor de capas** (`SceneLayer` · `MatchLayer` · `HudLayer` · `AnimationLayer`). Game y Studio comparten exactamente el mismo render.
- **Escenario oficial** procedural y responsive (`CombatMat`): lente central, rieles de neón reactivos al turno, texturas argyle, ambiente reactivo por tipo. Desacoplado del estado de partida.
- **Framework de Animación v1.0** (headless) en `core:animation` (Director · Scheduler · Policy · Queue · Arbiter) + `core:animation-compose` (patrón **RenderNode + Executor + NodeRenderer** para Move / DrawCard / Evolve, con `CoordinateRegistry` y `AnimationRenderState`). Es una tubería **real y extensible**.
- **Framework Motion** en `core:designsystem/motion` (`MotionContainer/Panel/Dialog/Flight`, `AnimationCurves`, `AnimationSprings`, `AnimationDurations`, `AnimationManager` con toggle de reduced-motion).
- **Carta holográfica premium** (`tilt/` giroscopio + `HoloShader` + `CardDetailDialog`): ya es calidad AAA puntual.
- **Vocabulario de eventos cerrado** (`EVENT-CATALOG.md`) y **Studio laboratorio** con Event Player + ritual sembrado.
- **V0.1** ya reemplazado a cinemática AAA única (lenguaje «Umbral de Luz»).

**Debilidad estructural central — DOS PARADIGMAS conviven:**
1. La **tubería canónica** (RenderNode/Executor/Director) — usada por Move/DrawCard/Evolve.
2. **Overlays ad-hoc dibujados inline** en `CombatScreen` y `feature:combat` — `CoinFlipOverlay`, `DealOverlay`, `LensAttackFx` (`CombatLensFx`), `BattleFx`, HP pills, números de daño, `GameOverPanel`, banners.

Esta dualidad es la raíz de la incoherencia: el juego se siente como **una colección de animaciones de distintas manos**, no como una dirección única. Es el problema #1 a resolver.

**Otras debilidades:**
- **Calidad desigual ("primer pase"):** volado, reparto, impacto de ataque (`LensFx` procedural), HP/daño son funcionales pero no AAA.
- **Sin lenguaje de cámara unificado** (transforms 2D dispersos; sin sistema de encuadre/peso compartido).
- **Sin sistema de ritmo/timing global** (duraciones sueltas: reloj 25:00, delays ad-hoc, sin niveles de "peso").
- **Mezcla de dirección artística:** réplica funcional de TCG Live vs. la nueva «Elegancia Cinematográfica».
- **Sin audio** (canon: aún solo diseño sonoro ambiental, sin implementar).
- **Eventos sin tratamiento dedicado:** estados especiales, clima, buscar/barajar, curación, herramientas, estadios.

---

## 2) Inventario de eventos visuales existentes (mapeado al Catálogo)

| Bloque | Eventos | Implementación actual | Estado |
|---|---|---|---|
| **Preparación** | V0.1 | Cinemática AAA única (`V01Cinematics`/`EventPlayer`) | ✅ AAA |
| | V0.2–V0.5, E1 | — (framing pendiente) | ⚪ |
| | E2 Volado | `CoinFlipOverlay` (Canvas simple) | ♻️ primer pase |
| | E3 Reparto | `DealOverlay` | ♻️ |
| | E4/E5 Básicos/Mulligan | flujo setup (parcial) | ♻️ |
| | E6/E7 Activo/Banca | modo setup en `MatchLayer` | ♻️ |
| | E8 Premios · E9 Revelado | flag `revealing` + `PrizeFan` | ♻️ |
| | V10 Entrega del control | HUD normal | ♻️ |
| **Turno** | T1–T8 | encendido de rieles + reloj; sin ceremonia | ♻️/⚪ |
| **Acciones** | A1 Banca, A3 Energía, A4 Objeto, A5 Partidario, A7 Herramienta, A9 Retirada | intents + `MotionFlight` (vuelo) | ♻️ |
| | A2 Evolución | **tubería canónica** (`EvolveExecutor`/`EvolveRenderNode`) | ✅ parcial |
| | A6 Estadio · A8 Habilidad | mínimos | ⚪ |
| **Combate** | C1–C3 decl/objetivo/anticipación | UI mínima | ⚪ |
| | C4 Impacto | `LensAttackFx` (procedural) | ♻️ |
| | C5 Debilidad/Resistencia · C6 Daño/contadores | HP pills + números | ♻️ |
| | C7 Efectos · C8 Anulado | mínimos | ⚪ |
| **KO** | K1–K5 | mínimos/none | ⚪ |
| **Premios** | P1–P3 | `PrizeFan` estático | ♻️ |
| **Estados** | S1–S7 | iconos de estado | ♻️/⚪ |
| **Utilidades** | U1 Robo, U9 Vuelo | `DrawCard*`, `MotionFlight` | ♻️ |
| | U2 Barajar, U3 Buscar, U4 Moneda, U8 Descarte | mínimos/none | ⚪ |
| **Final** | G1 Victoria/G2 Derrota | `GameOverPanel` (fundido) | ♻️ |
| | G3 Empate · G4 Desempate · Concesión | none | ⚪ |
| **Post-partida** | R1–R3 | none | ⚪ |
| **Transversal** | Carta a detalle (holo/gyro) | `CardDetailDialog` + tilt | ✅ AAA |
| | Clima visual | none | ⚪ |

Leyenda: ✅ AAA · ♻️ existe, primer pase (reemplazar) · ⚪ sin tratamiento dedicado.

---

## 3) Problemas detectados (priorizados)
1. **Dualidad de paradigmas** (tubería canónica vs overlays inline) → incoherencia estructural. *(Crítico)*
2. **Sin lenguaje transversal** de cámara, ritmo, peso y feedback → cada evento "va por su cuenta". *(Crítico)*
3. **Calidad desigual** entre eventos AAA y primer pase → rompe la ilusión de "versión definitiva".
4. **Riesgo de legibilidad**: en un TCG, el estado SIEMPRE debe leerse; el espectáculo no puede ocultarlo.
5. **Sin audio** ni ganchos de audio-ambiental integrados en la tubería.
6. **Cobertura incompleta**: estados, clima, buscar, barajar, curación, estadios, post-partida.
7. **Regresión potencial** al tocar `CombatScreen`/`feature:combat` (código compartido con el Game).

---

## 4) Referentes estudiados (síntesis; detalle en `research/V0.1-investigacion-aaa.md`)
- **Juegos de cartas:** Legends of Runeterra (escenario vivo con profundidad), Hearthstone (ritual, tactilidad, feedback sonoro), Marvel Snap (ritmo, cero aire muerto, respeto al tiempo), Yu-Gi-Oh Master Duel (drama del acento), Magic Arena (legibilidad), Gwent/Balatro/Inscryption/Slay the Spire (materialidad y game-feel de carta).
- **Fuera del género:** pantallas VS de lucha (SF6/Tekken 8/MK1) para presentación de contendientes; **broadcast deportivo** (lower-thirds, repetición, handoff a la acción) para marcadores y entrega de control; MOBA/hero-shooter (establishing del recinto); cine (plano de establecimiento, match-cut, "settle" antes de la acción).
- **Motion design / UX moderno:** un foco por momento, curvas de easing con intención, motion con propósito, jerarquía visual, microinteracciones con feedback inmediato.
- **Game-feel competitivo:** feedback de impacto por capas (flash + shake + freeze-frame + partícula), telegrafía de acciones, hit-stop selectivo.

**Extracto (mejores ideas a adaptar, nunca copiar):** escenario como lugar vivo · ritmo que nunca se arrastra · ritual legible · impacto por capas con hit-stop en clímax · handoff de control de calidad broadcast · materialidad y sonido · legibilidad por encima del espectáculo.

---

## 5) Dirección artística (única para toda la partida)
**«Elegancia Cinematográfica»** (base canónica) llevada a AAA:
- **El escenario oficial es un LUGAR vivo** (no un fondo): profundidad, iluminación reactiva, ambiente sutil que respira; la contienda ocurre en un recinto real.
- **Lenguaje de luz «Umbral de Luz»** como firma transversal (cálida, con propósito; revela, acompaña, acentúa).
- **Materialidad**: cartas y objetos con peso, sombra propia y respuesta física; nada "flota" sin razón.
- **Paleta y jerarquía**: color con significado (tipo, bando, estado); el foco siempre guía la mirada.
- **Contención que acelera en los clímax**: sobriedad en lo rutinario, espectáculo reservado a KO/Evolución/Impacto/Victoria.

---

## 6) Dirección cinematográfica (ritmo de la partida)
**Mapa de ritmo por naturaleza del evento:**

| Categoría | Feel | Ejemplos | Duración objetivo |
|---|---|---|---|
| **Clímax** (espectacular, hit-stop) | Peso máximo, cámara se acerca, tiempo se dilata | KO decisivo, Evolución, Impacto de ataque, Victoria | 0.8–1.6 s |
| **Núcleo** (con acento, legible) | Claro y satisfactorio, sin arrastrar | Volado, Reparto, Robo destacado, Premio, Cambio de turno | 0.4–0.9 s |
| **Rutina** (rápido, casi invisible) | Fluido, mínimo, no interrumpe | Robar del turno, Adjuntar energía, Fin de turno | 0.15–0.4 s |
| **Utilidad** (claridad > espectáculo) | Sobrio, informativo | Buscar, Barajar, Descartar, Estados | 0.2–0.6 s |

**Reglas cinematográficas transversales:**
- **Anunciar → Actuar → Resolver** en toda acción importante (telegrafía + acción + acento de cierre).
- **Un solo foco visual por beat** (canon).
- **La cámara nunca duda**; el movimiento tiene intención; **hit-stop** solo en clímax.
- **Toda transición prepara el siguiente evento** (HANDOFF; secuencia continua).
- **Acelerar** en lo repetitivo (turnos de rival, robos); **desacelerar** en lo decisivo (KO, revelación, victoria).

---

## 7) Principios permanentes de UX
1. **Legibilidad primero**: el estado de juego se entiende SIEMPRE; el espectáculo nunca lo tapa.
2. **Un foco por momento**: la mirada se dirige sin ambigüedad.
3. **Respeto al tiempo del jugador**: rutinas rápidas; cinemáticas **saltables** tras la primera vez.
4. **Vocabulario de feedback consistente**: la misma acción produce siempre la misma respuesta visual/sonora.
5. **Telegrafía**: toda acción importante se anuncia antes de resolverse.
6. **Accesibilidad**: respetar reduced-motion (`AnimationManager`); nunca depender solo del color o del movimiento.
7. **Sin ambigüedad**: cada estado (turno, objetivo, daño, premios) es inequívoco.
8. **Intención**: nada existe solo porque "se ve bonito"; todo comunica, aclara o refuerza emoción.

---

## 8) Principios permanentes de Motion Design
1. **Niveles de duración** (tokens): micro (≤150 ms) · estándar (150–350 ms) · enfatizado (350–700 ms) · cinemático (700 ms–1.6 s). Unificar sobre `AnimationDurations`.
2. **Vocabulario de easing** (unificar sobre `AnimationCurves`): entrada acelera, salida desacelera; enfatizado con overshoot medido; lineal solo para viajes continuos.
3. **Spring vs tween**: springs para lo físico/táctil (aterrizajes, rebotes); tween para lo dirigido (cámara, luz).
4. **Anticipación → acción → follow-through** en toda animación con peso.
5. **Motion con propósito**: nada se mueve sin comunicar; sin adornos gratuitos.
6. **Peso por capas**: el impacto AAA se compone (flash + desplazamiento + partícula + hit-stop + sonido), no un solo efecto.
7. **Overshoot disciplinado**: sí en lo satisfactorio, jamás comprometiendo la legibilidad.
8. **Coherencia de escala**: tamaños y distancias relativos al lienzo (fracciones, `BoardGeometry`), nunca literales.

---

## 9) Clasificación de eventos por prioridad e impacto

| Tier | Prioridad | Eventos (IDs) | Intención |
|---|---|---|---|
| **S · Clímax** | Máxima | K2/K5 KO, A2 Evolución, C4 Impacto, P3 último premio, G1 Victoria, E9 Revelado | Espectáculo pleno + hit-stop; el corazón emocional |
| **A · Núcleo con acento** | Alta | E2 Volado, E3 Reparto, C6 Daño, P1 Premio, T6 Cambio de turno, V10 Entrega, G2 Derrota | Claro, satisfactorio, legible |
| **B · Rutina fluida** | Media | T2 Robo, A3 Energía, A1 Banca, A9 Retirada, T4 Fin de turno, U1 Robo | Rápido, casi invisible, sin interrumpir |
| **C · Utilidad sobria** | Media-baja | U2 Barajar, U3 Buscar, U8 Descarte, S1–S7 Estados, A4/A5 Entrenadores, A6 Estadio, A7 Herramienta, clima | Claridad informativa > espectáculo |
| **D · Post-partida** | Baja | R1–R3, G3/G4, Concesión | Cierre limpio y coherente |

---

## 10) Plan Maestro de reemplazo integral (fases validables)

> Regla de oro del plan: **Fase 0 primero**. Sin un lenguaje y una tubería únicos, cada evento seguiría siendo un parche. La coherencia se construye **antes** que los eventos.

- **FASE 0 · Fundaciones (el backbone de coherencia).** Unificar sobre la **tubería canónica** (RenderNode/Executor/Director) como el ÚNICO pipeline de eventos visuales; migrar progresivamente los overlays inline a ella. Definir el **lenguaje transversal**: tokens de cámara/encuadre, niveles de ritmo/duración, vocabulario de feedback (impacto por capas), ganchos de audio-ambiental, y el "look" del escenario vivo. *Validación:* un evento piloto (p. ej. C4 Impacto) reproducido por la tubería con el nuevo lenguaje, comparado en Studio.
- **FASE 1 · Preparación (arco de inicio).** V0.1 (hecho) → V0.2 ritual completo (volado→reparto→setup→revelado→Turno 1), transición imperceptible. *Validación:* ritual end-to-end en el Studio.
- **FASE 2 · Bucle de turno.** T1–T8: inicio, robo, fase de acciones, fin, checkup, cambio de turno, turno de rival telegrafiado.
- **FASE 3 · Cadena de combate.** C1–C8: declaración → objetivo → anticipación → **impacto (clímax)** → debilidad/resistencia → daño/contadores → efectos.
- **FASE 4 · KO y Premios (clímax).** K1–K5 + P1–P3: telegrafía, desvanecimiento con peso, descarte, toma de premio, marcador, último premio.
- **FASE 5 · Acciones.** A1–A9: Banca, Evolución, Energía, Objeto, Partidario, Estadio, Herramienta, Habilidad, Retirada.
- **FASE 6 · Estados, utilidades y clima.** S1–S7, U1–U9, clima visual: legibilidad sobria y consistente.
- **FASE 7 · Final y post-partida.** G1–G4, Concesión, R1–R3: cierre coherente y de calidad broadcast.
- **FASE 8 · Pulido global + audio + pase de coherencia.** Revisión integral del arco completo; audio ambiental; consistencia de ritmo y feedback; accesibilidad.

Cada fase: **objetivos claros → implementación incremental → validación en Studio → criterio de salida** antes de avanzar. El objetivo siempre es **el 100 %**, no mejoras aisladas.

---

## 11) Riesgos técnicos
1. **Migración de dos paradigmas a uno**: riesgo de regresión en `CombatScreen`/`feature:combat` (compartido con el Game). *Mitigación:* migrar evento a evento tras el compositor de capas, con validación en Studio y build verde en los tres targets.
2. **Rendimiento móvil**: overdraw de Compose, shaders (holo), partículas, hit-stop. *Mitigación:* presupuesto de capas por evento, `AnimationManager` (reduced-motion), medición.
3. **Legibilidad bajo espectáculo**: el clímax no puede ocultar el estado. *Mitigación:* principio UX #1 como criterio de validación obligatorio.
4. **Determinismo para replay**: los eventos visuales no deben depender de estado no reproducible. *Mitigación:* la tubería consume `GameEvent`/estado del motor; checkpoints deterministas.
5. **Dirección de dependencias**: no filtrar conceptos del Studio al Game. *Mitigación:* orquestación en el Studio; `feature:combat` solo bloques neutrales.
6. **Alcance/tiempo**: es el mayor sprint del proyecto. *Mitigación:* fases validables; Fase 0 habilita el resto.
7. **Audio inexistente**: identidad sonora pendiente. *Mitigación:* diseñar ganchos de audio-ambiental en Fase 0, sonido pleno cuando exista identidad musical.

---

## 12) Recomendación final
Adoptar este documento como **hoja de ruta oficial** y ejecutar **Fase 0 antes que cualquier evento**: es lo que convierte "una colección de animaciones" en "una sola dirección". La palanca decisiva es **unificar sobre la tubería canónica + un lenguaje transversal** (cámara, ritmo, feedback, escenario vivo, audio-ambiental). Con esa base, cada fase reemplaza su bloque de eventos **sobre el motor y el Studio actuales**, sin nueva APK, sin duplicar lógica, sin tocar reglas, respetando todo el canon.

Resultado al terminar: **todos** los eventos de una partida —de la entrada al resultado— reemplazados por versiones AAA que se sienten como **una única experiencia dirigida por una sola mano**: la versión definitiva de Pokémon TCG.

---
*Plan Maestro — hoja de ruta permanente. Las fases se ejecutan una a una con validación en Studio. El objetivo es el 100 % de la experiencia, coherente y pulida de principio a fin.*
