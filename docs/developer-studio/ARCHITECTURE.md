# Pokémon TCG Clone — Developer Studio

**Documento de arquitectura · Fase 0 (diseño). NO es implementación.**
Herramienta oficial de desarrollo del proyecto. Vida útil = la del juego (décadas).
Estado: `DRAFT v0.2` — decisiones §2 y §12 CERRADAS. Fase M0 detallada en `M0-CORE.md`.

**Decisiones cerradas:**
- **Preview:** estrategia **C → A**. Primero `HeadlessTimelinePreview` (sin dispositivo,
  valida arquitectura y flujo); después `DeviceMirrorPreview` (pixel-perfect con los
  renderers reales). No se retrasa el Studio esperando el mirror.
- **Frontend:** **React + TypeScript**. El Studio es herramienta de desarrollo, no el juego;
  ecosistema IDE superior; **cero lógica de negocio en el frontend** (solo consume contratos
  y renderiza). Toda la lógica vive en el backend Kotlin y los módulos compartidos.

---

## 1. Visión y no-negociables

El Developer Studio **no depura**: **acelera el desarrollo**. Todo sistema visual/técnico
nuevo nace, se prueba, se compara, se documenta y se preserva **aquí**, y solo cuando está
validado se **consume** desde el juego. El juego deja de ser el laboratorio.

**Invariantes (no se negocian nunca):**

| # | Invariante | Consecuencia de diseño |
|---|-----------|------------------------|
| I1 | **Regla de oro:** el Studio NUNCA contiene lógica exclusiva. | Todo lo que corre en el Studio debe poder correr en el juego con los MISMOS componentes. El Studio solo cambia el *origen de los datos* y añade *chrome de edición*. |
| I2 | **Misma arquitectura.** Nunca motores paralelos. | El Studio depende de `engine:*`, `core:animation`, renderers, `core:designsystem`, card renderer. No los reimplementa. |
| I3 | **Módulo desacoplado.** Vive en `tools/developer-studio`. | No pertenece a `feature:game` ni al cliente Android. La dependencia fluye Studio → módulos del juego, jamás al revés. |
| I4 | **Hot reload.** Editar parámetro → ver resultado sin reinstalar. | Todo parámetro editable viaja como *datos* (JSON) por un canal en vivo; nada de recompilar/reflashear para cambiar `duration`/`overshoot`. |
| I5 | **Preservación.** Reproducir una animación de hace 10 años, bit a bit. | Versionado de definiciones + snapshots inmutables + entorno de reproducción determinista. |
| I6 | **Modularidad tipo plugin.** Añadir un Lab no toca el núcleo ni otros Labs. | Contrato `StudioLab` + descubrimiento; el shell no conoce Labs concretos. |

---

## 2. La tensión central: "app web" ⨯ "mismos renderers Compose"

Tus renderers reales son **Compose/Android** (no portables a un navegador). Tus capas de
lógica son **Kotlin JVM puro** (portables a cualquier backend). Por tanto la reutilización
"mismo código" se estratifica:

```
                 ┌──────────────────────────────────────────────┐
 REUTILIZABLE    │  TIER LÓGICO  (Kotlin JVM puro)               │  100% igual que el juego
 TAL CUAL        │  engine:model/events/effects/rules           │  corre en backend directo
 EN BACKEND      │  core:animation (Director/Scheduler/Queue/    │
                 │      Player/Definitions/Recipes de datos)     │
                 │  data:cards (datos de carta)                  │
                 └──────────────────────────────────────────────┘
                 ┌──────────────────────────────────────────────┐
 NO CORRE EN     │  TIER RENDER  (Compose / Android)            │  hay que PROYECTARLO
 NAVEGADOR       │  NodeRenderers (DrawCardNodeRenderer…)        │  al navegador, no clonarlo
 (Compose/AGSL)  │  Card Renderer, HoloCard/AGSL shaders        │
                 │  Partículas Compose, core:designsystem UI     │
                 └──────────────────────────────────────────────┘
```

**Resolución adoptada — `Preview Provider` (abstracción de superficie de render).**
El Studio separa **CONTROL** (web: timeline, inspector, catálogo, plugins, exportación,
docs) de **PREVIEW** (la superficie donde se dibuja de verdad). El preview se sirve a través
de una interfaz con backends intercambiables, de modo que la regla de oro (I1) se cumple
**literalmente**: los píxeles que ves en el Studio los pintan los renderers **del juego**.

```
PreviewProvider (contrato)
├─ (A) DeviceMirrorPreview   ← RECOMENDADO para arrancar
│     Un "Studio Runtime" (harness Android mínimo) embebe los renderers REALES del juego.
│     El backend le empuja parámetros por WebSocket (hot reload I4); el harness recompone
│     y transmite frames (WebRTC/scrcpy) al panel de preview del navegador.
│     ✔ Pixel-true, cero clonado de lógica de render.  ✘ Requiere emulador/dispositivo.
│
├─ (B) ComposeMultiplatformPreview   ← evolución futura, NO ahora
│     Si/ cuando el tier render migre a Compose Multiplatform (Desktop/Wasm), el preview
│     se dibuja nativo en el propio Studio. Máxima comodidad; coste alto (shaders AGSL y
│     partículas necesitan backend por-target). La abstracción deja la puerta abierta.
│
└─ (C) HeadlessTimelinePreview   ← ELEGIDO PRIMERO (Fase 1), sin dispositivo
      El tier lógico corre en backend y emite el AnimationRenderState frame a frame
      (nodos, rects, progress) como DATOS. El navegador dibuja una representación
      esquemática (cajas/trayectorias) para timeline/inspección. NO son los píxeles
      finales, pero valida el pipeline puro (Director→Scheduler→…→RenderState) sin Android.
```

> **DECIDIDO (v0.2):** Fase 1 = **(C) headless** (sin dispositivo, valida arquitectura y
> flujo, respeta I1 porque usa el `core:animation` real); **(A) device mirror justo
> después** para fidelidad pixel-perfect. **B** queda como horizonte. El Studio NO se
> retrasa esperando el mirror.

Esta abstracción es la pieza que impide que el Studio degenere en "un segundo motor".

---

## 3. Arquitectura en capas

```
┌───────────────────────────── FRONTEND (navegador, localhost) ─────────────────────────────┐
│  Studio Shell (IA, layout dockable, command palette, theming pro)                          │
│  ├─ Lab Host  → carga Labs por plugin (Animation, Card, Particle, Shader, UI, Battlefield, │
│  │              Camera, Audio, Benchmark, Performance)                                       │
│  ├─ Inspector (edición de parámetros por reflexión de esquema)                              │
│  ├─ Catálogo / Biblioteca (fichas, tags, dependencias, historial, variantes)               │
│  ├─ Timeline (play/pause/step/loop/scrub/velocidad)                                         │
│  └─ Preview Pane (consume un PreviewProvider)                                               │
└───────────────────────────────────────────────────────────────────────────────────────────┘
                    ▲  WebSocket (estado, hot-reload de params, control de timeline)
                    │  REST (catálogo, snapshots, exportación, historial)
┌───────────────────────────── BACKEND (JVM local, Ktor) ───────────────────────────────────┐
│  Studio Server                                                                             │
│  ├─ Resource Discovery  (escanea classpath → AnimationDefinitions, Recipes, Card skins,    │
│  │                        Shaders, Particle configs, UI components) — I2, auto (§6)         │
│  ├─ Session Orchestrator (corre el pipeline REAL: DefaultAnimationDirector.create + registry)│
│  ├─ Preview Provider host (A/C) + bridge al Studio Runtime (device) si aplica              │
│  ├─ Preservation Store   (definiciones versionadas, snapshots, presets, comparativas)      │
│  └─ Docs Bridge          (skills: animation-benchmark / implementation-research /          │
│                            revision-critica → fichas junto a la implementación)             │
└───────────────────────────────────────────────────────────────────────────────────────────┘
                    │ depende de (solo lectura, sin fork)
┌───────────────────────────── MÓDULOS DEL JUEGO (sin cambios) ─────────────────────────────┐
│  engine:model/events/effects/rules · core:animation · data:cards                          │
│  (+ tier render vía Studio Runtime Android para PreviewProvider A)                          │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

**Principio de acoplamiento:** el backend depende de los módulos del juego **como
consumidor**. Si el Studio necesita "ver" algo que hoy es privado del juego, la corrección
correcta es **exponerlo en el juego** (una API pública reutilizable), NO duplicarlo en el
Studio (I1).

---

## 4. Desglose en módulos (`tools/developer-studio`)

Gradle multi-módulo, para que "añadir un Lab" sea añadir un módulo, no tocar el núcleo (I6).

```
tools/developer-studio/
├─ studio-contracts/         # Kotlin JVM puro. Contratos: StudioLab, ResourceDescriptor,
│                            #   ParamSchema, PreviewProvider, SnapshotId, VariantRef.
│                            #   SIN dependencias del juego → estable durante décadas.
├─ studio-server/            # Ktor. Discovery + Orchestrator + Preservation + REST/WS.
│                            #   Depende de engine:*, core:animation, data:cards y de los
│                            #   plugins de Lab. NO de Compose.
├─ studio-runtime-android/   # (PreviewProvider A) harness Android mínimo que embebe los
│                            #   renderers REALES + emite frames. Reutiliza core:animation-
│                            #   compose y core:designsystem TAL CUAL.
├─ studio-web/               # Frontend. Shell + Labs (ver §5). Build propio (Vite/TS o
│                            #   Compose-Wasm; decisión de stack en §10-M1).
├─ labs/                     # Un módulo por Lab (plugin). Cada uno declara su StudioLab.
│   ├─ lab-animation/        #   backend-side de cada Lab: expone recursos + esquema de
│   ├─ lab-card-renderer/    #   parámetros + acciones. El frontend del Lab vive en studio-
│   ├─ lab-particle/         #   web pero se registra por el mismo contrato.
│   ├─ lab-shader/
│   ├─ lab-ui/
│   ├─ lab-rules/          #   construye GameState arbitrario vía engine:* real (§ Rules Lab)
│   ├─ lab-battlefield/
│   ├─ lab-camera/
│   ├─ lab-audio/
│   ├─ lab-benchmark/
│   └─ lab-performance/
└─ studio-preservation/      # Store de versiones/snapshots/presets (formato en disco, §8).
```

> **Nota de build:** `tools/developer-studio` se incluye en `settings.gradle.kts` pero se
> puede excluir del ensamblado del APK del juego (los módulos del juego **no** dependen del
> Studio → I3). El juego compila y publica sin el Studio.

---

## 5. Navegación e IA (Information Architecture)

Modelo mental **IDE profesional** (Unreal/Unity/JetBrains), no "página con pestañas".

```
┌ Top bar ─ command palette (Ctrl-K) · sesión · Preview Provider · perfil de rendimiento ─┐
├───────────┬────────────────────────────────────────────────┬──────────────────────────┤
│ IZQUIERDA │                 CENTRO                           │        DERECHA           │
│  Biblioteca│              Preview Pane                        │       Inspector          │
│  (árbol de │  (PreviewProvider: mirror | headless)           │  (ParamSchema del objeto │
│   recursos │                                                 │   seleccionado, editable │
│   por Lab, │─────────────────────────────────────────────────│   → hot reload I4)       │
│   con      │              Timeline                            │  Ficha de catálogo (§7)  │
│   filtros  │  play·pause·⏮frame·frame⏭·loop·scrub·velocidad   │  Variantes (§ variantes) │
│   y tags)  │                                                 │  Historial (§8)          │
├───────────┴────────────────────────────────────────────────┴──────────────────────────┤
│ ABAJO ─ consola/logs · Benchmark docs (skills) · export · diagnóstico Preview            │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

- **Layout dockable** (paneles reordenables/desacoplables, presets de workspace por Lab).
- **Selector de Lab** = cambia el contenido de los 4 paneles según el `StudioLab` activo,
  pero el *shell* (biblioteca/preview/timeline/inspector) es común → coherencia entre Labs.
- **Command palette** para todo (reproducir, comparar contra benchmark, exportar, crear
  variante…). Todo comando es también invocable por API (scriptable/CI a futuro).
- **Deep-linking:** cada recurso/variante/snapshot tiene URL estable
  (`/lab/animation/draw-card/variant/marvel-snap@v3`) → reproducibilidad e I5.

---

## 6. Descubrimiento automático y catálogo

Nada se registra a mano (requisito explícito). El backend **escanea** y construye la
Biblioteca.

**`ResourceDescriptor` (contrato, `studio-contracts`):**
```
ResourceDescriptor {
  id: String                 // "draw-card"
  kind: ResourceKind         // ANIMATION | SHADER | PARTICLE | CARD_SKIN | UI_COMPONENT ...
  labId: String              // "animation"
  name, version, author, dateCreated
  status: Draft|Experimental|Canonical|Deprecated
  tags: List<String>
  dependencies: List<ResourceRef>
  variants: List<VariantRef>
  paramSchema: ParamSchema   // parámetros editables + tipos + rangos (para el Inspector)
  history: List<SnapshotId>
  notes: String
  docs: DocRefs              // enlaces a las 3 skills (§9)
}
```

**Estrategia de discovery (por kind):**
- **Animations:** escanear los `AnimationDefinitionContributor` del classpath (ya son un
  patrón distribuido en el proyecto — `DrawCardAnimations`, etc.). Cada contributor →
  descriptor. El `ParamSchema` se deriva de los campos del `AnimationStep`/receta.
- **Shaders / Particles / Card skins / UI:** convención de carpeta + anotación ligera
  (`@StudioResource`) leída por reflexión/KSP, para no obligar a registro manual pero
  mantener el grafo **auditable** (mismo espíritu que el registry distribuido: explícito,
  sin "magia" no rastreable).
- El catálogo es **derivado** (se reconstruye del código); la ficha editable (notas, tags,
  estado) se **superpone** desde `studio-preservation` sin mutar el código.

---

## 7. Variantes, Inspector y parámetros en vivo

- **Variantes:** un recurso (p. ej. `Draw Card`) tiene N implementaciones
  (`Canonical`, `Experimental`, `MarvelSnapStyle`, `HearthstoneStyle`, `Legacy`).
  Cambiar de variante es cambiar qué `AnimationDefinition`/receta alimenta el pipeline
  real → intercambio instantáneo, sin reconstruir el Studio. Regla I1: cada variante es
  **código/datos del juego**, no un mock del Studio.
- **Inspector:** lee el `ParamSchema` y pinta editores por tipo (slider para `duration`,
  curva editable para `easing`, número para `overshoot`, enum para `layer`/`priority`…).
  Editar emite un *param patch* (JSON) por WebSocket → el `PreviewProvider` lo aplica en
  caliente (I4). Los params viven como **overrides de sesión**; "Guardar como variante"
  los materializa en el Preservation Store.

---

## 8. Preservación, versionado e historial (I5)

- **Definiciones versionadas:** `DrawCard v1..vN`. Cada versión es un artefacto **inmutable**
  (receta + params + metadatos). Restaurable.
- **Snapshots:** captura reproducible de una prueba: `{definiciónRef@version, params,
  seed/determinismo, entorno (BOM, minSdk, PreviewProvider), assets referenciados}`.
  Volver en 10 años = cargar el snapshot en el entorno registrado.
- **Formato en disco (durabilidad):** JSON legible + assets referenciados por hash, bajo
  `tools/developer-studio/preservation/` (versionable en git; el historial "de verdad" es
  el propio repo + el índice del store). Nada binario propietario que caduque.
- **Comparativas:** un snapshot puede fijarse como *benchmark* y guardarse junto a la
  captura de referencia (Marvel Snap, TCG Live) para el Benchmark Lab.

---

## 9. Integración con las skills (docs junto a la implementación)

El proyecto ya tiene el flujo canónico: `animation-benchmark → implementation-research →
integración sobre Framework v1.0 → revision-critica → Canónica`. El Studio lo hace de
**primera clase**:
- Cada `ResourceDescriptor.docs` enlaza su **benchmark** (estándar visual), su **ficha de
  research** (A/B/C/D/E de implementaciones) y su **revisión crítica**.
- El Benchmark Lab muestra referencia ⨯ implementación **lado a lado** + los docs de las
  skills en el mismo panel. Promover a `Canonical` exige que las tres existan (gate).

---

## 9-bis. Rules Lab (constructor de estado de partida)

Uno de los Labs de mayor valor a largo plazo: **construir un `GameState` arbitrario sin
jugar una partida** y, con un clic, lanzar una animación o ejercitar una regla sobre él.

- **Fuente de verdad = `engine:*` real (I1).** El Rules Lab NO modela estado propio: edita
  el `GameState` del juego usando el mismo modelo (`PokemonInPlay`, zonas, premios, mano,
  mazo, contadores, estados). El "estado construido" es un `GameState` legítimo que el
  motor acepta.
- **Capacidades de edición:** elegir Activos, poblar Bancas, adjuntar Energías, asignar
  contadores de daño/estados, configurar premios, manipular mazo y mano. Todo vía
  *builders* que producen un `GameState` válido (con validación del propio motor cuando
  aplique).
- **Puente con otros Labs:** un estado construido es un **input** para Animation Lab
  (disparar `CardDrawn`/`PokemonPlayed`/… sobre ese estado), Battlefield Lab (colocación
  manual) y Benchmark/Performance. Se **preserva** como snapshot (§8): "escenario X v3".
- **Regla:** si construir un estado requiere algo hoy privado del motor, se **expone en el
  engine** como API reutilizable, no se duplica en el Studio.

## 10. Sistema de plugins (I6)

**Contrato `StudioLab` (en `studio-contracts`):**
```
interface StudioLab {
  val id: String
  val title: String
  fun resources(ctx: DiscoveryContext): List<ResourceDescriptor>   // qué expone
  fun actions(): List<StudioAction>                                // comandos del Lab
  fun previewFor(resource: ResourceDescriptor): PreviewRequest     // cómo se previsualiza
}
```
- **Descubrimiento:** `ServiceLoader`/registro explícito de `StudioLab` en el classpath del
  backend; el shell del frontend recibe la lista por API y monta la navegación. Añadir un
  Lab = nuevo módulo `labs/lab-xxx` que implementa `StudioLab`; **cero cambios** en shell,
  server-core u otros Labs.
- **Aislamiento:** un Lab no puede referenciar a otro Lab (solo a `studio-contracts` y a
  módulos del juego). El shell media todo. Un Lab que falla al cargar se degrada solo
  (el resto del Studio sigue).
- **Frontend del Lab:** panel React/Web (o Compose-Wasm) registrado por `id`; el shell hace
  lazy-load. Contrato de UI mínimo: recibe `resource + paramSchema + previewHandle`.

---

## 11. Hoja de ruta por etapas

Cada etapa entrega algo **usable** y respeta I1–I6. No se implementa nada en esta fase 0.

| Etapa | Nombre | Entregable | Valida |
|-------|--------|-----------|--------|
| **M0** | Esqueleto + contratos | `studio-contracts` (StudioLab, ResourceDescriptor, ParamSchema, PreviewProvider) + `studio-server` Ktor vacío que arranca en `localhost` + shell web mínimo (layout dockable, command palette). | Que la carcasa de plugin y el canal WS existen. |
| **M1** | Decisión de stack web + Preview headless | Elegir stack frontend. `HeadlessTimelinePreview` (C): backend corre el `core:animation` REAL y emite `AnimationRenderState` frame a frame; el navegador dibuja esquemático + timeline (play/pause/step/loop/scrub/velocidad). | I1 + I4 sin dispositivo. Pipeline puro visible. |
| **M2** | **Animation Lab (primer Lab real)** | Discovery de `AnimationDefinitionContributor` → catálogo. Inspector con `ParamSchema` (duration/easing/overshoot/…). Hot-reload de params. **Draw Card #001** como primer recurso, con sus variantes. | El flujo "editar param → ver cambio" de punta a punta. |
| **M3** | Preservación + variantes + historial | Preservation Store (versiones, snapshots, restore). Variantes intercambiables (Canonical/Experimental/…). Exportación JSON. | I5. |
| **M4** | **Preview de dispositivo (A)** | `studio-runtime-android` embebe los renderers REALES; stream de frames al Preview Pane; hot-reload sobre el harness. | Pixel-true; regla de oro literal. |
| **M5** | Benchmark Lab + skills | Referencia ⨯ implementación lado a lado; docs de las 3 skills embebidos; gate de promoción a Canonical. | §9. |
| **M6** | Performance Lab | FPS/CPU/GPU/draw calls/memoria/tiempo por frame/animaciones activas/RenderNodes, leídos del runtime real. | Diagnóstico continuo. |
| **M4.5** | **Rules Lab** | Constructor de `GameState` arbitrario sobre `engine:*` real; alimenta Animation/Battlefield/Benchmark; escenarios preservables. Alta prioridad: elimina "jugar partidas completas" para reproducir escenarios. | I1 + productividad. |
| **M7+** | Resto de Labs por plugin | Card Renderer, Particle, Shader, UI, Battlefield, Camera, Audio — cada uno como módulo `labs/lab-xxx`, sin tocar el núcleo. | I6 a escala. |

**Orden deliberado:** primero los Labs que el proyecto necesita YA (Animation, Benchmark,
Performance) porque el backlog de animaciones está activo; el resto llega como plugins sin
fricción cuando toque.

---

## 12. Riesgos y decisiones abiertas

1. ~~[DECISIÓN] Preview de Fase 1.~~ **CERRADA:** C (headless) → A (device mirror). (§2)
2. ~~[DECISIÓN] Stack del frontend.~~ **CERRADA:** React + TypeScript. Cero lógica de
   negocio en el frontend; solo consume contratos y renderiza.
3. **Riesgo de fuga de la regla de oro:** vigilar que ningún Lab introduzca cálculo de
   animación/daño/render propio. Mitigación: `studio-contracts` no depende de Compose ni
   permite lógica; los cálculos vienen SIEMPRE de `core:animation`/`engine:*`.
4. **Fidelidad del preview headless (C):** no es pixel-true por diseño; sirve para pipeline
   y timing, no para juzgar "juice". Por eso A es necesario para validación visual final.
5. **Determinismo para preservación (I5):** requiere fijar seeds y registrar el entorno
   (BOM Compose, minSdk, versión de renderers) en cada snapshot.
```
