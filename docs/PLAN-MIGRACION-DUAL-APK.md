# Plan canónico de migración — hacia la arquitectura dual (dos APK, un núcleo)

**Hoja de ruta oficial.** Reorganiza el repositorio desde su estado actual (una sola app de juego)
hacia dos aplicaciones Android independientes sobre un núcleo compartido, **sin romper el proyecto en
ningún momento**. Solo arquitectura y estrategia: sin código, Gradle, scripts ni Compose.

Regla de Oro que gobierna todo el plan: *el Studio nunca simula el juego; ejecuta exactamente el mismo
núcleo que el juego.* La migración existe para convertir esa regla en una **garantía estructural**.

---

## 1. Estado actual

Hoy el repositorio es **un monorepo con una única aplicación** (el juego), ya organizado en capas
descendentes limpias. Esto es una ventaja enorme: la base no está enredada.

**Módulos y responsabilidades.**
- **Núcleo puro (Kotlin/JVM, sin Android):** `engine:model/events/effects/rules` (estado, reglas,
  efectos), `core:animation` (motor de animación agnóstico), `data:cards/gacha/profile` (modelos,
  catálogos, serialización). Es la verdad del juego, testeable sin dispositivo.
- **Núcleo Android/render:** `core:animation-compose` (render del motor de animación),
  `core:designsystem` (holo/tilt, render de carta y **tokens**).
- **Datos con servicio:** `data:cloud/netplay/netfirestore` (red/Firebase).
- **Features del juego:** `feature:game` (combate del jugador), `feature:packs`, `feature:decks`.
- **Aplicación:** `app` — agrega todas las features del juego + núcleo. Es la única APK.

**Puntos de acoplamiento relevantes (medidos).**
- El grafo es **estrictamente descendente**: `app → feature:* → (designsystem, engine, data)`;
  `designsystem → engine:model`; `data:cards → engine:model`; `animation-compose → animation`. **No
  hay** dependencias feature→feature ni ascendentes. *Base sana.*
- **Acoplamiento clave a vigilar:** `core:designsystem` mezcla **dos identidades**: el visual del
  *juego* (`TcgColors`, `TypeEmblem`, `HoloCardImage`, `CardDetailDialog`, `DeckBox`) y el **PDS del
  Studio** (`StudioColorTokens` + ComponentStyle de la Fase 10). Ambos ya conviven *separados por
  nombres*, pero en un mismo módulo.
- `app` mezcla el rol de "aplicación de juego" con el de "raíz del proyecto". No existe todavía ninguna
  superficie de Studio.

**Conclusión del estado actual:** no hay deuda estructural grave; la migración es sobre todo **aditiva**
(crear el lado Studio) y de **renombrado/clarificación** (nombrar el lado juego, precisar la frontera
del designsystem), no de desenredar acoplamientos.

---

## 2. Estado objetivo

Dos aplicaciones delgadas sobre un núcleo neutral de producto. Cada responsabilidad tiene un sitio
inequívoco.

**Núcleo compartido (una sola implementación, cero duplicación).**
- *Agnóstico de plataforma:* `engine:*`, `core:animation`, `data:cards/gacha/profile` (dominio +
  serialización), y un módulo de **assets compartidos** (imágenes/datos de carta) + utilidades comunes.
- *Android/render:* `core:animation-compose`, y el **render neutral** de carta (holo/tilt) del
  designsystem.
- *Lenguaje visual:* dentro del designsystem conviven, separados, el **visual del juego** (lo consume
  solo el juego) y el **PDS del Studio** (lo consume solo el Studio).

**Datos con servicio:** `data:cloud/netplay/netfirestore` — enlazados **solo por el juego** (salvo un
sandbox de red explícito del Studio).

**Features del juego:** `feature:game`, `feature:packs`, `feature:decks`, y las futuras (colección,
tienda, ranked, museo, perfil, ajustes del jugador, menús).

**Features del Studio (Labs):** `studio:shell` (superficie única persistente) + `studio:*`
(animation-gallery, card-editor, effect-editor, skill/attack-editor, combat-lab, deck-sandbox,
inspector, debug/engine-view, build, validation).

**Aplicaciones:**
- `app-game` — el actual `app`, renombrado; agrega solo features del juego + núcleo.
- `app-studio` — nuevo; agrega solo `studio:*` + núcleo.

**Invariante de destino:** `app-game` no conoce ningún `studio:*`; `app-studio` no enlaza las features
exclusivas del juego; el núcleo no conoce a nadie por encima; ninguna app conoce a la otra.

---

## 3. Estrategia de migración (fases pequeñas, reversibles, siempre compilando)

Filosofía: **aditivo antes que destructivo.** Primero se levanta el lado Studio en paralelo (riesgo
cero para el juego); solo al final se ajustan fronteras. Cada fase deja el proyecto compilando y es
reversible revirtiendo un único paso.

- **M0 · Línea base y salvaguardas.** No se mueve nada. Se fija el punto de partida: build y tests en
  verde, y se documenta el grafo de dependencias actual como *baseline*. Se define la **regla de
  frontera** que se hará cumplir al final (juego ↛ studio; núcleo ↛ arriba). Reversible por definición.
- **M1 · Nombrar el lado juego.** Renombrar conceptualmente `app` → `app-game`. Solo cambia identidad;
  el juego sigue idéntico. Reversible (revertir el rename).
- **M2 · Levantar `app-studio` vacío.** Nueva aplicación que depende **solo** del núcleo compartido y
  arranca a un Shell placeholder. Puramente aditivo: el juego no se toca. Prueba de extremo a extremo
  de que el build dual funciona **desde el principio**.
- **M3 · Crear `studio:shell`.** Contenedor vacío de la superficie única; `app-studio → studio:shell →
  núcleo`. Aún sin Labs. Aditivo.
- **M4 · Consolidar assets y utilidades compartidas.** Si hay duplicación (o riesgo de ella), extraer
  un módulo de **assets compartidos** y uno de **utilidades comunes** para que ambas apps referencien
  una sola fuente. Reversible módulo a módulo.
- **M5 · Precisar la frontera del `designsystem` (opcional, diferible).** Mantener el módulo
  **compartido**. Si se desea, separar internamente *render neutral* / *visual del juego* / *PDS del
  Studio* mediante reorganización con fachada, sin romper consumidores. **Recomendación: diferir** hasta
  que moleste; hoy no aporta y añade riesgo.
- **M6 · Primer Lab: Galería de Animaciones.** Levantar `studio:animation-gallery` sobre
  `core:animation(+compose)` — el Lab protagonista y la **prueba más fuerte de la Regla de Oro**. A
  partir de aquí, los demás Labs entran **uno por iteración** (ya como trabajo de Fase 11), sin tocar
  el juego.
- **M7 · Sellar fronteras.** Activar la salvaguarda del grafo (definida en M0) que falla si `app-game`
  llega a depender de cualquier `studio:*` o si el núcleo depende hacia arriba. Cierre.

No existe ninguna fase "big bang": el lado juego permanece intacto y compilando durante M1–M7.

---

## 4. Orden recomendado de movimientos

El orden minimiza el riesgo yendo **de lo estable a lo nuevo**, y dejando lo delicado (fronteras
visuales) para el final:

1. **Núcleo primero — confirmar, no mover.** Verificar que `engine:*`, `core:animation`,
   `data:* puros` ya son neutrales de producto. No requieren traslado; son la base compartida.
2. **Renderer y animation engine — verificar que ya son compartibles.** `core:animation` y
   `core:animation-compose` se quedan donde están; solo se confirma que no tienen nada específico de
   juego. Son la columna vertebral de la paridad.
3. **Renombrar `app` → `app-game`.**
4. **Crear `app-studio` (vacío) + `studio:shell`.**
5. **Assets** compartidos → módulo único (antes de que ningún Lab los consuma).
6. **Design System — al final y con cuidado.** Se queda compartido; la separación interna (si se hace)
   es el último paso, con fachada.
7. **Labs — incrementales.** Galería de Animaciones primero; luego card-editor, effect-editor,
   combat-lab, deck-sandbox, build, validación, uno por iteración.
8. **Features del juego — no se mueven** salvo renombrar su agrupación; ya están en su sitio.

Regla mnemotécnica del orden: *primero lo que ya es compartido (confirmar), luego lo nuevo del Studio
(añadir), y lo visual compartido al final (precisar).*

---

## 5. Hitos de validación (qué comprobar tras cada fase)

- **M0:** build completo en verde; suite de tests del núcleo en verde; grafo de dependencias capturado
  como referencia.
- **M1:** el juego compila y arranca exactamente igual tras el rename; ninguna referencia rota.
- **M2:** *dos* APK se generan; `app-studio` arranca a su placeholder; `app-game` sin cambios de
  comportamiento; `app-studio` **no** enlaza ninguna feature del juego.
- **M3:** `app-studio → studio:shell → núcleo` verificado; sin dependencias ascendentes.
- **M4:** un único origen de assets/utilidades; **sin duplicación**; ambas apps compilan contra él.
- **M5 (si se ejecuta):** consumidores del designsystem intactos; sin regresiones visuales; frontera
  interna documentada.
- **M6:** la Galería reproduce animaciones con el **mismo** motor/render que el juego (paridad
  verificable comparando una animación en ambos); `studio:*` no toca `feature:*` del juego.
- **M7:** el chequeo de grafo pasa: **cero** referencias `app-game → studio:*`, **cero** ascendentes,
  **cero** cruces feature-juego ↔ feature-studio.

Verificaciones transversales a repetir en cada fase: *compila · tests verdes · grafo descendente · sin
duplicación · sin referencias cruzadas.*

---

## 6. Riesgos por fase (problema · detección · prevención)

- **M1 (rename):** referencias colgantes al identificador antiguo. *Detección:* build roto / búsquedas
  del nombre viejo. *Prevención:* rename atómico en un solo paso reversible.
- **M2 (app-studio):** que el nuevo módulo arrastre por comodidad una feature del juego. *Detección:*
  inspección del grafo del nuevo módulo. *Prevención:* declarar desde el inicio que `app-studio` solo
  depende del núcleo; nada más.
- **M4 (assets):** romper rutas de recursos del juego al centralizar. *Detección:* recursos que no
  resuelven / regresiones visuales en el juego. *Prevención:* mover en lotes pequeños, validando el
  juego tras cada lote.
- **M5 (designsystem):** romper consumidores al reorganizar tokens/visual. *Detección:* fallos de
  compilación en features del juego y en Labs. *Prevención:* fachada estable; diferir si no aporta.
- **M6 (primer Lab):** tentación de "adaptar" el motor para el Studio. *Detección:* aparición de ramas
  condicionales "si es Studio" en el núcleo. *Prevención:* el Studio solo **observa/conduce**; toda
  instrumentación vive en `studio:*` o en puntos de observación neutrales del motor.
- **Transversal:** deriva de versiones entre apps. *Detección:* imposible en monorepo (mismo commit).
  *Prevención:* mantener ambas apps en el mismo árbol; nunca versionar el núcleo por separado sin
  necesidad.

---

## 7. Criterios de finalización (objetivos y verificables)

La migración termina cuando **todos** se cumplen:
1. Se generan **dos APK** independientes (`app-game`, `app-studio`) desde el mismo árbol.
2. `app-game` arranca y se comporta **igual** que antes de migrar (sin regresiones de juego).
3. `app-studio` arranca a su Shell y ejecuta al menos la **Galería de Animaciones** con el **mismo
   motor/render** que el juego (paridad demostrada).
4. El grafo de dependencias es **estrictamente descendente**: núcleo ← features ← apps; **cero**
   `app-game → studio:*`; **cero** cruces entre features de ambos productos; ninguna app depende de la
   otra.
5. **Sin duplicación** de lógica de dominio, motor, render, animación, serialización ni assets: una
   sola implementación de cada uno en el núcleo.
6. La salvaguarda de frontera está activa y **pasa**.
7. Suite de tests del núcleo en verde, cubriendo lo compartido.

Mientras 1–7 no se cumplan por completo, la migración se considera **en curso**, pero el proyecto
**siempre compila** (por diseño aditivo).

---

## 8. Auditoría de compatibilidad con el canon

- **Filosofía / 4 Pilares.** El plan es la forma literal de los pilares 3 y 4: el juego *consume*
  componentes validados; el Studio es la *herramienta especializada* donde se desarrollan antes. ✔
- **Especialización Absoluta (§2/§3).** "El juego nunca contiene herramientas" pasa de disciplina a
  invariante verificable (M7). El núcleo compartido es la Regla de Oro §3.2. ✔
- **Regla de Oro.** El plan la coloca en el centro: la Galería de Animaciones (M6) reproduce con el
  mismo motor; cualquier rama "si es Studio" en el núcleo se trata como defecto (riesgo M6). ✔
- **Arquitectura Dual.** El plan implementa exactamente el grafo aprobado en `ARQUITECTURA-DUAL-APK`
  (capas 0–4, dirección única, dos cáscaras). ✔
- **Design System / Visual Tokens / ComponentStyle (F6/F9/F10).** No se modifican; permanecen en el
  designsystem compartido. La separación PDS (Studio) ↔ visual del juego ya existe por nombres y solo
  se *precisa* (M5, opcional). Ningún token ni ComponentStyle cambia. ✔
- **Interaction Canon / Arquitectura Espacial / Workflows.** Intactos; se materializan en `studio:*`.
  ✔
- **Fase 11.** El Shell único + Labs (auditoría 11.0) mapea directamente a `studio:shell` + `studio:*`;
  el plan crea justo ese andamiaje. ✔

**Incompatibilidades:** ninguna. El plan no toca ninguna fase cerrada; solo añade el lado Studio y
nombra el lado juego, reforzando el canon en lugar de tensarlo.

---

## ¿Listo para comenzar el desarrollo de la interfaz?

**Sí, con una condición mínima y clara.** Como la migración es **aditiva**, no hace falta terminarla
entera para empezar la UI del Studio. Basta completar el **andamiaje**: M1 (nombrar `app-game`), M2
(`app-studio` vacío) y M3 (`studio:shell`). En ese punto existe una superficie real donde construir la
Fase 11 sobre los ComponentStyle ya congelados, **sin riesgo para el juego** y **sin esperar** a M4–M7,
que pueden avanzar en paralelo.

Recomendación de secuencia: ejecutar M0–M3 (andamiaje) → comenzar Fase 11 por *Boot + Shell* y
*Galería de Animaciones* (que coincide con M6) → continuar M4–M7 como trabajo de fondo. Hasta que no se
levante el andamiaje M1–M3, la UI del Studio no tiene dónde vivir; una vez levantado, el proyecto queda
**oficialmente listo para empezar la interfaz**.
