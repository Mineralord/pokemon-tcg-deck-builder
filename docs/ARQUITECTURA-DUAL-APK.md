# Arquitectura del proyecto — Dos APK, un único núcleo

**Decisión de alto impacto (2026-07-22).** El proyecto produce **dos aplicaciones Android
independientes** que comparten el máximo de código posible mediante un núcleo común. No se modifica el
canon del juego ni el del Studio; se define la arquitectura de módulos que ambos habitan.

- **APK 1 — Pokémon TCG Clone** (el juego, para jugadores). Solo funcionalidades de juego.
- **APK 2 — Pokémon TCG Studio** (herramienta de desarrollo, solo para el desarrollador). Solo
  desarrollo/validación, con **exactamente el mismo motor** que el juego (Regla de Oro I1).

> Arquitectura únicamente. Sin código, sin Gradle, sin Compose, sin implementación.

---

## 1. Principio rector

**Un solo núcleo, dos cáscaras.** Todo lo que *define cómo se comporta y se ve el juego* (motor,
reglas, render, animación, modelos, serialización, assets, lenguaje visual) vive en un **núcleo
compartido, neutral de producto**. Cada APK es una **cáscara delgada** que ensambla features propias
sobre ese núcleo. El Studio **observa y conduce** el mismo núcleo que el juego **ejecuta**: por eso lo
que el Studio valida es, literalmente, lo que el juego enviará (pilares 3 y 4).

Regla de dependencia global (una sola dirección):
```
app-game / app-studio  →  features (propias)  →  núcleo compartido  →  (nada por encima)
```
El núcleo **nunca** conoce a las features ni a las apps. Las features de un producto **nunca** conocen
a las del otro. Ninguna app depende de la otra.

---

## 2. Taxonomía de módulos (grafo por capas)

### Capa 0 — Núcleo agnóstico de plataforma (Kotlin/JVM puro, sin Android) · COMPARTIDO
El corazón. Testeable sin dispositivo; base de la paridad juego↔Studio.
- `engine:model` · `engine:events` · `engine:effects` · `engine:rules` — estado, simulación, reglas,
  DSL de efectos. **La verdad de la partida.**
- `core:animation` — motor de animación agnóstico (líneas de tiempo, curvas, estados). Sin UI.
- `data:cards` · `data:profile` · `data:gacha` (parte pura) — modelos de dominio, catálogos,
  **serialización** y validación de datos.
- *(Conceptual, si se extrae)* `core:common` / `core:serialization` — utilidades y formato de guardado
  compartido. Hoy repartido; candidato a consolidar.

### Capa 1 — Núcleo Android / render compartido (librería Android) · COMPARTIDO
Lo visual reutilizable por ambos productos.
- `core:animation-compose` — render del motor de animación en Compose (mismo resultado en juego y
  Studio: el Studio ve *exactamente* la animación del juego).
- `core:designsystem` — render de cartas (holo/tilt), formas, y **tokens**: `StudioColorTokens` (PDS,
  del Studio) y `TcgColors`/`TypeEmblem` (identidad del juego) **ya conviven separados** aquí.
- *(Conceptual)* `core:render` — si el renderer de tablero/carta crece, se aísla aquí; ambos lo usan.

### Capa 2 — Datos con dependencia de servicio/plataforma
- `data:cloud` · `data:netplay` · `data:netfirestore` — red/persistencia remota (Firebase).
  **Predominantemente del JUEGO** (PvP/Ranked/Tienda/nube). El Studio no los enlaza salvo un sandbox de
  red explícito.

### Capa 3 — Features (específicas de producto)
- **Del juego:** `feature:game` (combate del jugador), `feature:packs`, `feature:decks` (UI de
  construcción), y las venideras: colección, tienda, museo, ranked, perfil, ajustes del jugador, menús.
- **Del Studio (Labs):** `studio:shell` (superficie única persistente), `studio:animation-gallery`
  (Lab protagonista), `studio:card-editor`, `studio:effect-editor`, `studio:skill-attack-editor`,
  `studio:combat-lab`, `studio:deck-sandbox`, `studio:inspector`, `studio:debug`/`studio:engine-view`,
  `studio:build`, `studio:validation`.

### Capa 4 — Aplicaciones (dos APK)
- `app-game` (Application) → solo features del juego + núcleo compartido.
- `app-studio` (Application) → solo features del Studio + núcleo compartido.
  *(El actual `:app` pasa a ser `app-game`; `app-studio` es nuevo. Renombrado conceptual.)*

---

## 3. Qué se comparte / qué es exclusivo

| Ámbito | Compartido (núcleo) | Exclusivo JUEGO | Exclusivo STUDIO |
|---|---|---|---|
| Motor y reglas | ✅ engine:* | | |
| Animación (motor + render) | ✅ core:animation(+compose) | | |
| Render de carta / holo / tilt | ✅ core:designsystem (render) | | |
| Modelos / catálogos / serialización | ✅ data:cards/profile/gacha (puro) | | |
| Assets del juego (imágenes, datos de carta) | ✅ módulo de assets compartido | | |
| Lenguaje visual del juego (TcgColors) | vive en designsystem | ✅ lo consume | (no) |
| Lenguaje visual del Studio (PDS/Tokens) | vive en designsystem | (no) | ✅ lo consume |
| Red / nube / Firebase | (capa 2) | ✅ | ⚠️ solo sandbox |
| Menús, colección, tienda, ranked, museo, perfil | | ✅ | |
| Shell, Labs, inspector, depuración, sandbox, build, validación | | | ✅ |

**Nunca se duplica lógica de dominio:** reglas, efectos, animación, serialización y render tienen **una
sola implementación** (la del núcleo). El Studio jamás reimplementa nada del juego; lo instrumenta.

---

## 4. Reglas de frontera (invariantes de arquitectura)

1. `app-game` **no** depende de ningún módulo `studio:*`. (El juego jamás contiene herramientas —
   Especialización §2.) Verificable por el grafo de dependencias.
2. `app-studio` **no** depende de módulos exclusivos del juego (tienda, ranked, nube) salvo los de
   dominio compartido.
3. Ninguna app depende de la otra. Ambas dependen del **mismo commit** del núcleo (monorepo ⇒ misma
   versión garantizada, sin *drift*).
4. El núcleo (capas 0–1) **no** importa nada de `feature:*`, `studio:*`, `app-*`.
5. La instrumentación de depuración del Studio se apoya en **puntos de observación neutrales** del
   motor (o en un módulo `studio:engine-view` que el juego no enlaza), nunca en ramas “si es Studio”
   dentro del motor. El motor no sabe que el Studio existe.
6. Secretos/credenciales (Firebase) solo en `app-game`.

---

## 5. Riesgos y mitigaciones

- **Deriva del núcleo por conveniencia de un lado.** → El núcleo es *neutral de producto*; cualquier
  cambio en el motor debe servir al canon del juego. El Studio solo observa/conduce (invariante 5).
- **Fuga de herramientas al juego** (que `app-game` acabe enlazando algo del Studio). → Prohibido por
  invariante 1; se blinda con una comprobación del grafo de módulos en CI.
- **Duplicación de assets.** → Un único módulo de assets compartido; ambas apps lo referencian.
- **Acoplamiento del render a Compose** dificultando pruebas. → El motor de animación (capa 0) es
  agnóstico; el render (capa 1) es una capa fina y sustituible.
- **Crecimiento de la superficie de red en el juego.** → Aislada en capa 2; el Studio no la arrastra.
- **Divergencia de versiones.** → Monorepo: imposible por construcción; ambas apps compilan del mismo
  árbol.

## 6. Ventajas

- **Paridad garantizada (Regla de Oro I1):** el Studio valida el *mismo* motor/render que se envía; lo
  que se ve en la Galería de Animaciones es lo que verá el jugador. Cero “funciona distinto en el juego”.
- **Juego magro y seguro:** `app-game` no arrastra editores ni depuración → APK menor, menor superficie
  de ataque, arranque más rápido.
- **Especialización sin fricción:** cada producto evoluciona en su eje sin estorbar al otro.
- **Una sola fuente de verdad** para dominio/motor/render → menos bugs, tests centralizados.

## 7. Impacto en mantenimiento y escalabilidad

- **Mantenimiento:** un arreglo en el motor beneficia a ambos a la vez; los tests del núcleo protegen a
  los dos productos. El Studio es el *laboratorio* donde algo se prueba **antes** de que el juego lo
  consuma (pilares 3–4).
- **Escalabilidad:** añadir un Lab = un módulo `studio:*` nuevo, sin tocar el juego; añadir una feature
  de juego = un `feature:*` nuevo, sin tocar el Studio. El núcleo escala *en profundidad*, no en
  amplitud (Especialización §6).
- **Compatibilidad futura:** el núcleo puro (capa 0) es candidato natural a **Kotlin Multiplatform** si
  algún día se quisiera un visor de escritorio; podrían añadirse más cáscaras (p. ej. `app-cardviewer`)
  sobre el mismo núcleo sin rediseñar nada. La decisión no cierra ninguna puerta.

---

## 8. Revisión de compatibilidad con las fases cerradas (canon)

- **4 Pilares.** Pilar 3 (“el juego como producto que solo consume componentes validados”) y pilar 4
  (“el Studio como herramienta especializada donde todo se desarrolla antes de integrarse”): **dos APK
  sobre un núcleo común son su encarnación literal.** ✔ *Refuerza el canon.*
- **Especialización Absoluta.** §2 prohíbe que el juego contenga herramientas de desarrollo; **dos APK
  lo garantizan físicamente**, no solo por disciplina. §3.2 (Regla de Oro: reutilizar los componentes
  reales del juego) = el núcleo compartido. ✔ *Refuerza.*
- **Desarrollo Unipersonal / Proyecto único.** Sigue siendo un repo, un proyecto, un desarrollador; dos
  artefactos de salida no crean multi-proyecto. ✔
- **Design System (F6) · Visual Tokens (F9) · ComponentStyle (F10).** El PDS ya se declaró “lenguaje del
  **ecosistema**” y el módulo `core:designsystem` **ya separa** `StudioColorTokens` (PDS) de `TcgColors`
  (juego). Compartir ese módulo entre ambas apps es coherente: cada producto consume su propia paleta
  semántica. ✔
- **Interaction Canon · Arquitectura Espacial · Workflows.** Describen solo el Studio; residen en
  `studio:*`/`app-studio`. La partición no los toca. ✔
- **Fase 11 (auditoría de navegación 11.0).** Shell único + Labs → mapea exactamente a `studio:shell` +
  `studio:*`. ✔

### Incompatibilidades detectadas
**Ninguna que bloquee.** Una sola **precisión** (no contradicción): la doc del Design System mencionaba
el PDS sirviendo también al “launcher del juego”. Con la separación limpia, la **identidad visual del
juego es propia** (`TcgColors`/`TypeEmblem`) y el **PDS es el lenguaje de las herramientas** (Studio).
Ambos ya coexisten separados en `core:designsystem`; a futuro podría dividirse en
`designsystem-studio` (PDS) y el visual del juego, pero **no es necesario** y no afecta a esta decisión.

**Conclusión:** la decisión de dos APK sobre un núcleo común es **totalmente compatible** con todo el
canon existente y, de hecho, lo **materializa** mejor que una sola APK: convierte una regla de
disciplina (“el juego no lleva herramientas”, “el Studio reutiliza el motor real”) en una **garantía
estructural**.
