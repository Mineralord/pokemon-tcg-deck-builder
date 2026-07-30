# 🎮 Studio — Partida completa vs IA + Checkpoints manuales (propuesta de arquitectura)

> **Visión:** jugar una **partida real completa contra la IA dentro del Studio**, con el **mismo motor del
> juego**; crear **checkpoints manuales** en cualquier momento; recompilar el Studio y **recargar un
> checkpoint** para continuar exactamente desde ahí, sin perder contexto ni reconstruir la partida.
>
> **Sin código.** Análisis de alternativas + recomendación. Debe durar décadas, reutilizar el motor y **no
> duplicar lógica**. Respeta «Un Lab = Un Dominio».

## Infraestructura existente reutilizable (hallazgos del código)
- **Motor real + IA:** `engine:rules` (`GameEngine`) + `SmartAgent`/`Agent`. Ya juegan partidas reales.
- **Controlador del juego = mismo contrato de pantalla:** `GameViewModel` (PvE: motor + IA) **implementa
  `CombatSceneController`** → renderiza en **la misma `CombatScreen`** que el Studio ya usa. *(El Studio
  puede hospedar el controlador REAL del juego sin duplicar nada.)*
- **Serialización COMPLETA de estado:** `GameStateDto` (en `data:netplay`) serializa **todo el estado
  dinámico** (daño, energías, estados, premios, fase, decisiones pendientes, promociones, buffs por turno…)
  con **cartas por id** (payload pequeño; se rehidrata con `CardRepository`). Ya se mantiene al día para
  PvP. **Es exactamente lo que un checkpoint necesita.** Además existe `GameReplay`.

**Conclusión:** el 90 % ya existe. El trabajo es **componer** (hospedar el controlador real en el Studio) y
**persistir** (guardar/cargar `GameStateDto` en disco), no construir un motor ni un serializador nuevos.

---

## Parte 1 — Jugar una partida real vs IA dentro del Studio

**Enfoque recomendado:** el Studio **hospeda el controlador real del juego** (`GameViewModel`, PvE = motor
+ `SmartAgent`) dentro de `CombatScreen`, igual que el APK del juego. La IA juega; el humano juega; toda la
partida ocurre con el motor real.
- **Cero duplicación:** se reutiliza el mismo controlador y motor. El Studio solo lo **compone** y le añade
  utilidades (checkpoints) por fuera.
- **Convive con `SandboxController`:** hoy el Studio tiene dos productores de `CombatSceneController`:
  `SandboxController` (estado falso, sin reglas — para tooling/framing) y ahora `GameViewModel` (partida
  real). Ambos alimentan la MISMA pantalla; se elige uno según la tarea. **`SandboxController` no se toca.**
- **Impacto en motor/IA:** **ninguno** (se reutilizan tal cual).
- **Único añadido necesario (motor-side, mínimo):** un punto de entrada para **construir/reanudar** el
  controlador del juego **desde un `GameState` dado** (no solo desde partida nueva). Hoy `GameViewModel`
  arranca una partida fresca; para cargar un checkpoint necesita "reanudar desde este estado + de quién es
  el turno". Es una **fábrica/constructor** adicional, reutilizando su bucle; no cambia reglas.

---

## Parte 2 — Checkpoints manuales: alternativas

### A · Snapshot de estado serializado (reutiliza `GameStateDto`) — **RECOMENDADA**
Guardar el `GameState` actual como `GameStateDto` → JSON en disco, con nombre y metadatos (turno, seed).
- **Ventajas:** reutiliza el serializador **ya completo y mantenido**; payload **pequeño** (ids de carta);
  **restauración instantánea** (deserializar + reanudar); **sobrevive a recompilaciones**; determinista;
  independiente del historial.
- **Desventajas:** el DTO debe seguir cubriendo todo `GameState` (ya lo hace; se mantiene por PvP). Si
  cambian el **esquema/reglas** entre compilaciones, un checkpoint viejo puede requerir migración
  (versionado del DTO — ver Compatibilidad futura).

### B · Log de intents + replay (reutiliza `GameReplay`)
Guardar `seed + lista de GameIntent` desde el inicio; para restaurar, **re-ejecutar** los intents.
- **Ventajas:** almacenamiento mínimo; aprovecha el determinismo; `GameReplay` existe.
- **Desventajas (DESCALIFICANTES para este caso):** para llegar al turno 12 hay que **re-simular todos los
  turnos** (lento). Y sobre todo: **el flujo del usuario es cambiar el proyecto entre compilaciones**; si la
  lógica cambia, el **replay diverge** y el checkpoint deja de reproducir el mismo estado. Rompe el objetivo.

### C · Solo en memoria (sin persistencia)
- **Desventaja (descalificante):** no sobrevive a la recompilación — que es justo el requisito central.

### D · Híbrido (snapshot + log opcional para micro-replay)
- **Veredicto:** sobre-ingeniería hoy. El snapshot (A) cubre el caso; el log puede añadirse el día que se
  necesite (p. ej. auditar una secuencia). Infraestructura bajo demanda.

**Recomendación: A (snapshot vía `GameStateDto` a disco).** Es la única que **sobrevive a recompilaciones**
de forma robusta **y** restaura al instante, reutilizando infraestructura existente.

---

## Parte 3 — Persistencia entre compilaciones
- **`CheckpointStore`** (nueva infra mínima, compartida): guarda/lista/carga/borra checkpoints como archivos
  JSON en el almacenamiento de la app (`filesDir`, subcarpeta `checkpoints/`). Cada checkpoint =
  `{ nombre, timestamp, turno, seed, gameStateDto }`.
- **Sobrevive a recompilaciones:** `adb install -r` (update) **conserva** los datos de la app → los
  checkpoints permanecen. *(Nota: una desinstalación completa los borra; si se quiere blindaje extra, usar
  el directorio externo específico de la app. Recomendación: `filesDir` por defecto; externo como opción.)*
- **Coste de almacenamiento:** unos **pocos KB por checkpoint** (ids + estado dinámico). Cientos de
  checkpoints ≈ insignificante.
- **Coste de memoria:** solo el checkpoint cargado vive en RAM; la biblioteca está en disco.

---

## Parte 4 — Ubicación en Labs (principio «Un Lab = Un Dominio»)
**Configurar una partida** y **jugarla** NO son dominios distintos: son **dos MODOS del mismo
Match Builder** (su dominio único es *la partida*). **No se crea un Match Lab.** El mapa del Studio queda:
**Event Lab · Match Builder · Asset Lab (futuro) · Rule Lab (futuro).**
*(Se usa "Modo", no "estado", para no confundir con el `GameState` del motor, el estado de Compose ni un
estado de UI: "Modo" es simplemente una forma distinta de usar la misma herramienta.)*

### 🧩 Match Builder — dominio único: *la partida* — con DOS MODOS
- **Modo Configuración** *(Configuration Mode)*: formato · jugadores · IA · mazos · seed · reglas ·
  **Biblioteca de Checkpoints** · cualquier parámetro inicial. Compone (o carga) un `GameState` de partida.
- **Modo Partida** *(Play Mode)*: al pulsar **"Jugar"**, el Match Builder **cambia de modo** (no de Lab):
  deja la pantalla de configuración y hospeda el controlador real (`GameViewModel`: `GameEngine` +
  `SmartAgent`) en `CombatScreen`. Juegas contra la IA; botón **"Crear Checkpoint"** en cualquier momento
  (escribe en el `CheckpointStore`). Al **salir**, se vuelve al **Modo Configuración**, donde está la
  biblioteca para elegir otro checkpoint y volver a **"Jugar"**.

Es un **cambio de modo dentro del mismo dominio**, no un cambio de Lab — coherente con
«Un Lab = Un Dominio». **No hay "modo Debug":** "Crear Checkpoint" es un botón discreto del Modo Partida.

- **🎬 Event Lab** — sin cambios (reproduce eventos). *Futuro puente:* "Abrir evento actual" durante una
  partida viva = snapshot al vuelo + reproducir en Event Lab; "Regresar a la partida" = restaurar. Los
  checkpoints son exactamente el mecanismo que habilita ese puente.

*(Alternativa considerada y **descartada**: un "Match Lab" separado para la partida viva. Rompería el
principio de dominio al separar artificialmente dos modos de un mismo dominio; además escala
peor. Configurar y jugar viven juntos en Match Builder.)*

---

## Parte 5 — Flujo objetivo (ejemplo del usuario, resuelto)
1. Match Builder (**Modo Configuración**): nueva partida (o cargar checkpoint) → **"Jugar"** → el mismo
   Match Builder pasa al **Modo Partida** (motor + IA).
2. Turno 8, antes de evolucionar → **"Crear Checkpoint"** → "Antes de evolucionar Venusaur" (guardado en disco).
3. Continúo; la animación no convence → **modifico el proyecto → recompilo el Studio**.
4. Abro Studio → Match Builder (**Modo Configuración**) → Biblioteca de Checkpoints → cargar "Antes de
   evolucionar Venusaur" → **"Jugar"** (**Modo Partida**): la partida vuelve **exactamente** a ese estado.
5. Evoluciono → valido la **nueva** animación → **continúo jugando** con normalidad.
6. Al **salir** → vuelvo al **Modo Configuración**; todo el ciclo (configurar ↔ jugar ↔ checkpoint) ocurre
   **dentro del mismo Match Builder**.

Sin reconstruir la partida, sin re-jugar turnos, sin cambiar a un modo especial.

---

## Parte 6 — Impactos, costes y compatibilidad
- **Motor (`engine:rules`):** sin cambios de reglas. Único añadido: **reanudar desde `GameState`** (fábrica).
- **`GameViewModel` / controlador del juego:** añadir constructor/entrada "desde estado dado + turno". Vive
  en `feature:game` (compartido); el Studio lo **compone**, sin duplicar. Respeta dirección de dependencias
  (Studio → Game).
- **`SandboxController`:** **sin impacto** (sigue para framing/tooling; la partida real usa `GameViewModel`).
- **IA (`SmartAgent`):** **sin impacto** (se reutiliza; tras cargar un checkpoint, si es su turno, decide
  normal).
- **`GameStateDto`:** ya completo; **se reutiliza**. Debe seguir cubriendo todo `GameState` (ya es política
  por PvP).
- **Memoria/almacenamiento:** insignificantes (KB por checkpoint; solo el activo en RAM).
- **Compatibilidad futura:** **checkpoints robustos ante recompilaciones VISUALES** (animaciones, VFX, UI,
  cámara — el caso de uso principal): el estado no cambia, la restauración funciona. Ante cambios de
  **reglas/esquema** de `GameState`, versionar el DTO (`schemaVersion` en el archivo) y migrar o marcar
  incompatibles los checkpoints viejos. Recomendado incluir `schemaVersion` desde el día 1.

---

## Recomendación final
1. **Jugar vs IA:** hospedar el **controlador real del juego** (`GameViewModel`, motor + `SmartAgent`) en
   `CombatScreen` como el **Modo Partida del Match Builder** (no un Lab nuevo). Cero duplicación.
2. **Checkpoints:** **snapshot de estado** (Alternativa A) vía el **`GameStateDto` existente**, persistido
   por un **`CheckpointStore`** en disco (`filesDir/checkpoints/*.json`, con `schemaVersion`).
3. **Labs (sin cambios en el mapa):** **Event Lab · Match Builder · Asset Lab (futuro) · Rule Lab
   (futuro)**. El Match Builder tiene **dos modos** (Modo Configuración ↔ Modo Partida) dentro de su
   **dominio único**; la biblioteca de checkpoints vive en el Modo Configuración. **Event Lab** intacto, con el puente
   "abrir evento actual" como evolución futura habilitada por checkpoints.
4. **Añadido mínimo al motor:** un punto de **reanudar desde `GameState`** (fábrica), sin tocar reglas.

Es la arquitectura que cumple el objetivo (jugar completo vs IA + checkpoints persistentes que sobreviven a
recompilaciones y restauran al instante), **reutiliza al máximo** (motor, IA, serialización, pantalla),
**respeta «Un Lab = Un Dominio»** (configurar y jugar son estados del mismo dominio) y **escala décadas**.

---
*Propuesta. Tras tu aprobación se planifica la implementación incremental (p. ej.: `CheckpointStore` →
reanudar-desde-estado → **Modo Partida del Match Builder** con `GameViewModel` → botón
Crear/Cargar Checkpoint → biblioteca en el Modo Configuración del Match Builder), una pieza por vez con build verde.
No antes de retomar/cerrar V0.2 según tu prioridad.*
