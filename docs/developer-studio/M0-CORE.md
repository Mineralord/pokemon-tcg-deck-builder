# Developer Studio — Fase M0: Núcleo y Contratos

**Diseño detallado. NO es implementación.** Complementa `ARCHITECTURE.md` (v0.2).
Objetivo de M0: un **núcleo sólido** sobre el que construir durante décadas. Al terminar
M0 no hay Labs funcionales todavía; hay **contratos estables**, un **esqueleto de servidor**
que arranca en `localhost`, un **shell web** vacío con navegación por plugins, y un **canal
de comunicación** definido. Todo lo demás (Fases M1+) se enchufa a esto sin reescribirlo.

> **Regla de oro (recordatorio operativo):** en M0 no entra NINGUNA lógica de juego. Los
> contratos son deliberadamente "tontos": describen *qué* recursos/params/preview existen;
> el *cómo* siempre lo resuelven `engine:*` / `core:animation`. `studio-contracts` **no
> depende de Compose ni del engine** — así nunca puede colarse lógica exclusiva.

---

## 0. Alcance de M0 (checklist de diseño)

- [ ] Definir `studio-contracts` (tipos estables, sin deps del juego).
- [ ] Definir el esqueleto de `studio-server` (Ktor) y su ciclo de arranque.
- [ ] Definir el `StudioLab` como unidad de plugin y su descubrimiento.
- [ ] Definir el **protocolo** backend↔frontend (REST + WebSocket) a nivel de forma.
- [ ] Definir el esqueleto del `studio-web` (shell React/TS, sin Labs).
- [ ] Definir el modelo de **sesión** y el **contrato de PreviewProvider** (solo el tipo).
- [ ] Definir el layout de carpetas y la inclusión en Gradle (sin romper el build del juego).

**Fuera de M0 (no diseñar aquí):** discovery real de recursos, Animation Lab, hot-reload
efectivo, preservación, device mirror. Cada uno tiene su fase.

---

## 1. Mapa de módulos M0

```
tools/developer-studio/
├─ studio-contracts/      [M0]  Kotlin JVM puro. SIN deps del juego, SIN Compose.
├─ studio-server/         [M0]  Ktor. Arranca localhost, sirve /api + /ws + estáticos web.
├─ studio-web/            [M0]  React + TS. Shell (layout, palette, plugin host). Sin Labs.
├─ studio-preservation/   [M1+] (solo se reserva el nombre; vacío en M0)
├─ studio-runtime-android/ [M4] (no existe en M0)
└─ labs/                  [M1+] (vacío en M0; el shell tolera 0 Labs)
```

**Dependencias en M0 (grafo dirigido, sin ciclos):**
```
studio-server ──▶ studio-contracts
studio-web    ──▶ (nada de Kotlin; consume el protocolo por HTTP/WS)
```
`studio-server` **todavía no** depende de `engine:*`/`core:animation` en M0 (eso entra en
M1/M2 con el primer discovery). Esto mantiene M0 mínimo y compilable aislado.

**Gradle:** incluir `:tools:developer-studio:studio-contracts` y `:tools:developer-studio:
studio-server` en `settings.gradle.kts`. El APK del juego **no** los referencia (I3). El
frontend `studio-web` se construye con su propio toolchain (npm/vite), no con Gradle;
`studio-server` sirve el bundle estático ya compilado (o se corre por separado en dev).

---

## 2. `studio-contracts` — los tipos que vivirán décadas

Estables por diseño: se **extienden** (campos nuevos opcionales), casi nunca se rompen. Son
**datos**, no comportamiento. Formas de referencia (pseudocódigo Kotlin):

```kotlin
// --- Identidad y clasificación ---
@JvmInline value class ResourceId(val value: String)      // "draw-card"
@JvmInline value class LabId(val value: String)           // "animation"
@JvmInline value class VariantId(val value: String)       // "marvel-snap"
@JvmInline value class SnapshotId(val value: String)      // hash/uuid inmutable

enum class ResourceKind { ANIMATION, SHADER, PARTICLE, CARD_SKIN, UI_COMPONENT,
                          GAME_STATE, CAMERA, AUDIO /* extensible */ }

enum class ResourceStatus { DRAFT, EXPERIMENTAL, CANONICAL, DEPRECATED }

// --- Esquema de parámetros (alimenta el Inspector; NO ejecuta nada) ---
sealed interface ParamField { val key: String; val label: String }
data class FloatField(  ... , val min: Float, val max: Float, val step: Float) : ParamField
data class IntField(    ... , val min: Int,   val max: Int) : ParamField
data class BoolField(   ... ) : ParamField
data class EnumField(   ... , val options: List<String>) : ParamField
data class CurveField(  ... ) : ParamField                 // p.ej. CubicBezier editable
data class DurationField(...) : ParamField
data class ParamSchema(val fields: List<ParamField>)

// Valores actuales = mapa key→valor serializable (JSON). El Studio NO interpreta la
// semántica; solo transporta. Quien la aplica es el pipeline REAL del juego.
data class ParamValues(val values: Map<String, JsonElement>)

// --- Ficha de catálogo (derivada del código + overlay editable de preservación) ---
data class ResourceDescriptor(
    val id: ResourceId,
    val kind: ResourceKind,
    val labId: LabId,
    val name: String,
    val version: String,
    val author: String,
    val dateCreatedIso: String,
    val status: ResourceStatus,
    val tags: List<String>,
    val dependencies: List<ResourceId>,
    val variants: List<VariantRef>,
    val paramSchema: ParamSchema,
    val history: List<SnapshotId>,
    val notes: String,
    val docs: DocRefs,                // enlaces a las 3 skills (§ ARCHITECTURE §9)
)
data class VariantRef(val id: VariantId, val label: String, val status: ResourceStatus)
data class DocRefs(val benchmark: String?, val research: String?, val critique: String?)

// --- Preview (solo el CONTRATO en M0; sin backends todavía) ---
enum class PreviewMode { HEADLESS, DEVICE_MIRROR }   // C primero, A después
data class PreviewRequest(
    val resource: ResourceId,
    val variant: VariantId?,
    val params: ParamValues,
    val mode: PreviewMode,
)

// --- Unidad de plugin ---
interface StudioLab {
    val id: LabId
    val title: String
    // En M0 puede devolver listas VACÍAS: el shell debe tolerar Labs sin recursos.
    fun resources(ctx: DiscoveryContext): List<ResourceDescriptor>
    fun actions(): List<StudioAction>
    fun previewFor(resource: ResourceDescriptor, variant: VariantId?, params: ParamValues): PreviewRequest
}
data class StudioAction(val id: String, val label: String)   // comando invocable (palette/API)
interface DiscoveryContext { /* servicios de escaneo; vacío/mínimo en M0 */ }
```

**Por qué así:**
- `ParamField`/`ParamValues` separan **esquema** (qué se puede editar) de **valores** (qué
  está puesto). El Inspector se genera desde el esquema; el hot-reload transporta valores.
  El Studio nunca "sabe" qué significa `overshoot`: lo aplica el renderer real.
- `PreviewRequest` es **datos**: describe qué previsualizar; el `PreviewProvider` (M1+)
  decide cómo. En M0 solo existe el tipo.
- `StudioLab` puede estar **vacío** en M0 → el shell arranca con 0 Labs sin romperse.

---

## 3. `studio-server` — esqueleto Ktor

**Responsabilidad en M0:** arrancar en `localhost:8080`, exponer el contrato de API/WS,
descubrir `StudioLab`s del classpath (aunque sean 0), y servir el bundle de `studio-web`.
**Sin** discovery de recursos del juego todavía.

**Ciclo de arranque (diseño):**
```
main()
 └─ StudioServer.start(port = 8080)
     ├─ LabRegistry.discover()          // ServiceLoader<StudioLab> → lista (0..n)
     ├─ SessionStore.init()             // 1 sesión viva + sesiones guardadas restaurables (§5)
     ├─ install(ContentNegotiation/JSON)
     ├─ install(WebSockets)
     ├─ routing {
     │     get  /api/health             // {status:"ok", version}
     │     get  /api/labs               // List<LabSummary> (id, title)  ← 0 en M0
     │     get  /api/labs/{id}/resources// List<ResourceDescriptor>       ← [] en M0
     │     ws   /ws/studio             // canal en vivo ÚNICO (§4) — 1 desarrollador, 1 sesión viva
     │     static("/") { web bundle }
     │  }
     └─ log("Studio en http://localhost:8080")
```

**`LabRegistry`:** descubrimiento por `ServiceLoader` (explícito y auditable, sin "magia
mágica"). Añadir un Lab = añadir un módulo con su `META-INF/services`. El registry **no
conoce** ningún Lab concreto (I6). Un Lab que lanza excepción al cargar se **omite con log**;
el resto del Studio sigue vivo.

---

## 4. Protocolo backend ↔ frontend (forma, no payloads finales)

Dos canales, responsabilidades separadas:

- **REST (`/api`)** — lo **estable y cacheable**: catálogo, descriptores, listar/guardar/
  restaurar snapshots y sesiones guardadas (M3), exportar (M3). Idempotente y sin estado de
  sesión viva.
- **WebSocket (`/ws/studio`)** — canal **único** en vivo y bidireccional: hot-reload de params,
  control de timeline, stream de estado de preview. Es el canal que hace realidad I4.
  **Desarrollo unipersonal:** UN solo canal, UNA sesión viva → sin `sessionId`, sin
  multiplexado, sin bloqueos ni resolución de concurrencia. Si el desarrollador abre el Studio
  en dos ventanas, la política es **last-write-wins simple** (no hay dos personas que puedan
  chocar); no se diseña coordinación de concurrencia.

**Mensajes WS (sobre para futuro; en M0 solo se define el envelope + handshake):**
```
// Cliente → Servidor
{ type: "hello" }                                                          // sin sessionId
{ type: "param.patch",   resource, variant?, patch: {key: value, ...} }   // M2
{ type: "timeline.cmd",  cmd: "play|pause|stepFwd|stepBack|loop|scrub|speed", arg? } // M1
{ type: "preview.select", resource, variant?, mode }                      // M1

// Servidor → Cliente
{ type: "welcome", capabilities: [...] }                    // M0: capacidades del server
{ type: "preview.frame", frameIndex, nodes: [...] }         // M1: headless RenderState
{ type: "state", playing, frameIndex, totalFrames, speed }  // M1
{ type: "error", code, message }
```
**Envelope común:** `{ type, seq, ts }` + payload. Versionado por `capabilities` en el
`welcome` (el frontend se adapta a lo que el server soporta → compatibilidad a lo largo de
los años). En M0 solo se implementa `hello`/`welcome`/`error`.

**Contrato de tipos compartido:** el frontend NO comparte código Kotlin, pero SÍ debe
compartir la **forma** de estos tipos. Diseño: el backend expone un **esquema** (OpenAPI
para REST + JSON-Schema/AsyncAPI para WS) generado desde los contratos; el frontend genera
sus tipos TS desde ese esquema. Así "React solo consume contratos" (Decisión 2) sin
duplicar definiciones a mano.

---

## 5. Modelo de sesión (unipersonal)

**Canon (`MENTAL-MODEL`):** una **Session** es *tu contexto de trabajo en el tiempo*. En un
proyecto de **un único desarrollador** existe **exactamente UNA sesión viva** (la actual) más un
conjunto de **sesiones guardadas** restaurables (W1/W14). No hay `SessionId` para multiplexar
usuarios simultáneos; el id solo sirve para *nombrar una sesión guardada* que se quiera reabrir.
Sin owners, sin permisos, sin locks.

```
// La sesión VIVA (única, en memoria). Se materializa a disco solo al guardar (M3).
LiveSession {
  activeLab: LabId?
  selected: ResourceId?
  variant: VariantId?
  overrides: ParamValues        // ediciones no persistidas (hot-reload)
  previewMode: PreviewMode      // HEADLESS en M0/M1
  timeline: TimelineState       // playing, frameIndex, speed, loop (M1)
  workspace: WorkspaceState     // disposición del espacio (canon: Workspace ≠ Session)
}
// SessionStore: la viva + N sesiones guardadas (nombre + snapshot del contexto) para W14.
```
En M0 la sesión viva es casi vacía; se define su forma para que M1/M2 la rellenen sin rediseñar.
**Simplificación por unipersonalidad:** `SessionStore` es un contenedor trivial (una viva +
lista de guardadas), no un gestor de concurrencia/sesiones-de-usuario.

---

## 6. `studio-web` — esqueleto del shell (React + TS)

**Solo el shell.** Sin lógica de negocio (Decisión 2). Estructura de referencia:

```
studio-web/
├─ src/
│  ├─ app/          Shell: layout dockable, top bar, command palette (Ctrl-K), theming.
│  ├─ panels/       Los 4 slots del shell (Library, Preview, Timeline, Inspector) VACÍOS,
│  │                con contrato de "un Lab llena este slot".
│  ├─ plugin/       LabHost: pide /api/labs, monta la navegación, lazy-load del panel de
│  │                cada Lab por id. Tolera 0 Labs (estado "sin laboratorios").
│  ├─ api/          Cliente REST + WS. Tipos generados desde el esquema del backend (§4).
│  └─ theme/        Estética pro (oscuro tipo IDE); tokens de diseño.
└─ (vite + tsconfig)
```

**Contrato de panel de Lab (frontend):** cada Lab registra por `id` un componente que recibe
`{ descriptor, paramSchema, session, ws }` y pinta en los slots. El shell no sabe nada del
contenido → añadir un Lab no toca el shell (I6, lado frontend).

**Estado de M0 visible:** el Studio abre en `localhost:8080`, muestra el shell con layout
dockable, command palette operativa (comandos base: about, reload labs), y un mensaje
"0 laboratorios cargados" — porque los Labs llegan en M1+. Eso es **éxito** de M0: la
carcasa y el canal existen y son extensibles.

---

## 7. Criterios de aceptación de M0 (diseño listo cuando…)

1. Los contratos de `studio-contracts` están definidos y **no dependen** de `engine:*` ni
   Compose (garantía anti-fuga de la regla de oro).
2. El grafo de dependencias M0 es acíclico y el APK del juego compila **sin** el Studio.
3. `StudioLab` + `LabRegistry` permiten añadir un Lab como módulo nuevo **sin tocar** server
   ni shell, y el sistema tolera 0 Labs.
4. El protocolo REST/WS está especificado a nivel de envelope + handshake, con estrategia de
   versionado (`capabilities`) para durar años.
5. La sesión y el `PreviewRequest`/`PreviewMode` existen como tipos, listos para que M1
   (headless) los use sin rediseño.

---

## 8. Puente a M1

M1 añade, sin tocar el núcleo M0:
- `studio-server` empieza a depender de `core:animation` (real) y expone el primer discovery
  (los `AnimationDefinitionContributor`).
- `HeadlessTimelinePreview`: corre el `DefaultAnimationDirector` real, dirige el reloj y
  emite `preview.frame` (nodos del `AnimationRenderState`) por WS.
- El frontend estrena Timeline (play/pause/step/loop/scrub/velocidad) y un Preview
  esquemático que dibuja los nodos/trayectorias recibidos.

Todo ello se **enchufa** a los contratos de M0. Si algo de M1 obligara a romper un contrato
de M0, es señal de que el contrato estaba mal → se corrige en M0, no se parchea en M1.
