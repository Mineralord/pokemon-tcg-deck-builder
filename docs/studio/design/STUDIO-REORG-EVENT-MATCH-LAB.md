# 🧭 Reorganización del Studio — Event Lab ⟂ Match Builder (propuesta de arquitectura + UX)

> **Objetivo:** separar por responsabilidad las herramientas del Studio para que crezca durante décadas sin
> mezclar **producción de eventos** con **preparación de partidas / depuración**. **Sin perder
> funcionalidad**; mejorando arquitectura y UX. **No es un parche.** No incluye código: es la propuesta.

---

## 🏛️ Principio Arquitectónico PERMANENTE — «Un Lab = Un Dominio»
> **Cada Lab del Studio representa un único dominio funcional y responde a una única pregunta. Si una
> herramienta responde a otra pregunta distinta, pertenece a otro Lab.**

Un Lab **no puede mezclar** producción, preparación, depuración o edición. Es la regla que resuelve hoy el
problema de "Lanzar partida sembrada" y que, durante años, impedirá que se vuelvan a mezclar herramientas de
producción, configuración y depuración en un mismo espacio. **Toda herramienta nueva se ubica respondiendo:
¿a qué pregunta responde? → ese es su Lab.**

### Mapa de dominios (la pregunta define el Lab)
| Lab | Pregunta única | Responsabilidad | Nunca |
|---|---|---|---|
| **🎬 Event Lab** | *¿Cómo se **siente** este evento?* | Producir · comparar variantes · validar dirección · aprobar eventos canónicos | Crear partidas · elegir mazos · configurar jugadores · debug de reglas |
| **🧩 Match Builder** | *¿En qué **estado del juego** quiero comenzar?* | Nueva partida · partida sembrada · checkpoints · snapshots · seeds · restauración · configuración del estado inicial | Producir eventos · comparar animaciones · validar cinematografía |
| **🎨 Asset Lab** *(futuro)* | *¿Cómo se **comporta** este recurso visual?* | Sprites · modelos · materiales · partículas · sonidos · animaciones individuales | Construir partidas · ejecutar eventos completos |
| **🧪 Rule Lab** *(futuro)* | *¿Las **reglas** funcionan correctamente?* | Motor · resolución · casos límite · tests · debug del engine | Validar experiencia visual |

*(«Match Builder», no «Match Lab»: su misión no es jugar una partida, sino **construir** un estado del
motor. El nombre describe su responsabilidad exacta.)*

---

## Diagnóstico de los 4 problemas (confirmados en el código)
1. **Mezcla evento ↔ herramienta:** hoy `BoardSimulatorLabContent` monta una única consola (`SandboxConsole`)
   con **EventPlayerPanel** (eventos canónicos) **+ RitualLauncher** ("Lanzar partida sembrada") **+
   AssetToolPanel** (herramientas). "Lanzar partida sembrada" **no es un evento** → contamina el modelo mental.
2. **UX sobrecargada:** Event Player, Sandbox, herramientas, controles y depuración compiten en la misma
   pantalla, sin jerarquía.
3. **El tablero no es el protagonista:** durante la reproducción, los controles compiten con la escena.
4. **No se percibe el Momento Firma de V0.2:** esto es un **asunto de implementación** (el render aún no
   comunica "el tablero toma bando"), **no** de arquitectura — se retoma en el refinamiento G6 tras la reorg
   (que además facilita juzgarlo, al dejar el tablero limpio).

---

## Arquitectura actual (resumen)
- `MainActivity` **hoista** `BoardSimSession` (estado compartido) y renderiza `StudioShell(labs = studioLabs())`
  o `PresentationWorkspace`.
- `StudioShell` (módulo `studio:shell`): **Rail (R2)** de Labs + **Host (R3)**; **no conoce el interior** de
  ningún Lab (solo `id`/`title`/`content`). Ya soporta **múltiples Labs** por diseño.
- `studioLabs()` registra hoy: **`board-simulator`** (mezclado) y **`animation-gallery`**.
- `BoardSimSession` (compartido, hoistado): `registry` · `sandbox` (SandboxController) · `toolController` ·
  `coordinates` · `renderState` · `actors` · `player` (EventPlayerState). Es el **motor/render único**.

**Conclusión:** el Shell ya es un contenedor de Labs multi-responsabilidad. **La reorg NO crea un Studio
nuevo ni un módulo nuevo:** parte el Lab mixto en **dos Labs** que comparten la misma `BoardSimSession`.

---

## Reparto de componentes

### → EVENT LAB (responsabilidad única: **reproducir eventos canónicos**)
- **`EventPlayerState`** (transporte: load/variant/play/pause/restart/loop/speed/canon).
- **`EventPlayerPanel`** — **recortado a SOLO el Event Catalog** (V0.1, V0.2, …). **Sin** RitualLauncher.
- **`EventPlayerClock`**, **`PlayerDrivenBoard`** (board + `cameraChannels` + `EventOverlay`).
- **`EmptyMat`** (escenario oficial vacío) para eventos **framing**; para eventos **gameplay** futuros,
  la `CombatScreen` real sobre el **checkpoint** que le provea el Match Builder.
- **Cinemáticas de framing:** `V01Cinematics`, `V02Cinematics`, y sus overlays (`V02Layer`, bloom, viñeta…).
- **Presentation Mode** (reproducción limpia, tablero protagonista) → pertenece aquí.

### → MATCH LAB (responsabilidad única: **construir estados de partida**)
- **`SandboxController.startRitual()`** y la siembra de estados → **"Nueva partida" / "Partida sembrada"**.
- **Futuro (mismo Lab):** elegir jugadores · elegir mazos · elegir formato · restaurar snapshot ·
  checkpoints · seeds · escenarios de prueba · **debug**.
- **`AssetToolPanel` + `ToolController` + Active Tool workflow** (probar assets/animaciones sobre el
  tablero) → herramienta de construcción/depuración → aquí (o, más adelante, su propio "Asset Lab").
- **`reset()`** de la partida sembrada.

### DEPENDENCIAS COMPARTIDAS (siguen en `BoardSimSession`, hoistada sobre el Shell)
El **motor y el render únicos**, que **ambos** Labs componen (sin duplicar): `CombatScreen` (compositor de
capas) · `CombatMat` (escenario oficial) · `SandboxController` · `CoordinateRegistry` · `AnimationRenderState`
· `ActorVisualController` · `AssetRegistry` · `ToolController` · director de animación canónico.

> **El puente Event ⟂ Match (clave y ya canónico):** **Match Builder construye la partida/checkpoint**;
> **Event Lab reproduce el evento** — framing sobre escenario vacío, o gameplay **sobre el checkpoint que
> Match Builder dejó en la sesión compartida**. Respeta *escenario ⟂ partida* y el modelo de checkpoints
> deterministas. Para V0.1/V0.2 (framing) el Event Lab es autosuficiente; los eventos con cartas
> (volado, ataque, KO…) tomarán su estado del Match Builder.

---

## Qué se reutiliza / mueve / desaparece
- **Se reutiliza (prácticamente todo; sin pérdida de funcionalidad):** `EventPlayerState`,
  `SandboxController`, overlays, `CombatScreen`, `EmptyMat`, cinemáticas. Es sobre todo **RE-HOSTING**
  (qué composable vive en qué Lab), no reescritura.
- **Se mueve:** `RitualLauncher`/`startRitual` y `AssetToolPanel` salen de la consola de eventos → Match Builder.
  El `EventPlayerPanel` se **recorta** (quita el lanzador de partida; queda solo catálogo + transporte).
- **Desaparece:** el Lab mixto **`board-simulator`** como pantalla única; la `SandboxConsole` que agrupaba
  las tres cosas; y **"Lanzar partida sembrada" del listado de eventos** (se va a Match Builder).
- **Módulos:** ninguno nuevo. Cambios contenidos en `app-studio` (labs) + reutilizar `studio:shell`.

---

## Navegación resultante
```
StudioShell (Rail R2)
├── 🎬 Event Lab      → reproducir eventos (Event Catalog) · Presentation Mode
├── 🧩 Match Builder      → construir partidas: nueva/sembrada, jugadores, mazos, formato,
│                        snapshots, checkpoints, seeds, escenarios, debug, herramientas de asset
└── 🎞️ Galería de Animaciones  (existente)
   (… futuros Labs …)
```
El Rail del Shell ya conmuta entre Labs sin tocar el Shell. `MainActivity` seguirá hoistando la sesión
compartida; **Presentation Mode** pasa a ser un modo del **Event Lab**.

---

## Propuesta de UX (jerarquía; el tablero protagonista)
**Event Lab — "board-first":**
- El **tablero ocupa la pantalla**; los controles son **mínimos y retráctiles**.
- **Selector de evento** = un catálogo compacto que se **invoca** (no permanente); al elegir, se retrae.
- **Transporte** (play/pausa/reinicio/velocidad/variantes A-B) en una **barra fina** que **se auto-oculta
  durante la reproducción** y reaparece al tocar (como el grab handle actual, pero solo con lo esencial).
- **Presentation Mode** = tablero 100 % limpio (ya existe); Event Lab es su versión con transporte mínimo.
- Resultado: durante V0.2 el jugador ve **el tablero tomar bando**, no una consola.

**Match Builder — "tool-first":**
- Aquí **sí** hay paneles, listas y controles (construir estados es una tarea de herramienta).
- Organizado por secciones: *Partida* (nueva/sembrada/seed/formato) · *Snapshots/Checkpoints* ·
  *Assets/Debug*. La densidad de herramientas está **aislada** aquí, no contamina el Event Lab.

Esto ataca directamente los problemas **2** (jerarquía) y **3** (protagonista), y el **1** (separación) por
diseño.

---

## Impacto sobre la producción de eventos
- **Positivo y alineado con el canon** ("Studio = laboratorio de validación"): el Event Lab se vuelve una
  superficie **limpia y board-first**, ideal para **juzgar el Momento Firma** (problema 4).
- **Production Pipeline intacto** (G1–G8): no cambia el proceso; solo mejora la herramienta donde ocurre G6.
- **La comparación Nivel A (chips A/B)** vive en el Event Lab, con el tablero protagonista → comparación más
  justa.
- **V0.2 se reanuda** tras la reorg exactamente donde quedó (elegir A/B → refinar → CANON), ahora en un
  entorno que permite **sentir** el concepto.

---

## Plan de ejecución sugerido (incremental, cuando lo apruebes; una cosa a la vez, build verde)
1. **Extraer `EventLabContent`** desde `BoardSimulatorLabContent` (solo eventos + transporte board-first;
   sin ritual ni asset tools). Registrar `event-lab` en `studioLabs`.
2. **Crear `MatchBuilderContent`** con "Nueva/Partida sembrada" (mover `RitualLauncher`) + `AssetToolPanel`.
   Registrar `match-builder`.
3. **Retirar** el Lab mixto `board-simulator` y la `SandboxConsole` combinada; mover Presentation Mode al
   Event Lab.
4. **Pulir UX** (retráctiles, jerarquía) y validar; build verde en los tres targets; APK.

Cada paso preserva funcionalidad (re-hosting), no reescribe lógica.

---
*Propuesta de reorganización. Solo tras tu aprobación se implementa (sin código aún). Después se reanuda la
producción de V0.2 en el nuevo Event Lab.*
