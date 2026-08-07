# Developer Studio — Modelo Mental y Canon Terminológico (Fase 4.5)

**Cómo debe PENSAR el usuario sobre el Studio.** No hay botones, clics, paneles ni
componentes. Aquí se fija el **modelo mental único** y el **vocabulario canónico** que
significará exactamente lo mismo en todo el Studio, en toda su vida.

Análisis integrado por dos lentes (ambas activas esta sesión):
- **`ui-ux-pro-max`** → arquitectura de información, consistencia, aprendizaje, escalabilidad.
- **`emil-design-eng`** → identidad, cohesión, percepción, "el nombre crea identidad".

**Conflicto resuelto (naming):** Emil empuja nombres evocadores (memorabilidad); UI/UX empuja
nombres convencionales (predecibilidad). **Decisión técnica:** el **léxico de dominio es
deliberadamente plano y preciso** (como `commit`/`branch` en Git, `Frame` en Figma, `Actor` en
Unreal) — se optimiza para *no cambiar de significado en 30 años*, no para sonar bonito. La
identidad evocadora se reserva para el producto y sus interacciones-firma, nunca para estos
conceptos.

> Referencias obligatorias: `UX-PRINCIPLES` (P1–P15) · `SPATIAL-ARCHITECTURE` · `WORKFLOWS` ·
> `LAYOUT-CONCEPTUAL` · `D1-D2-RESOLUCION` · Regla de Oro (`ARCHITECTURE`, I1).

---

## Parte I — El modelo mental único

**Dos ejes, uno de contenido y uno de entorno. Todo concepto pertenece a exactamente uno.**

```
                      EJE DE CONTENIDO (lo que creas y preservas)
                      ─────────────────────────────────────────
                                    RESOURCE ★
                                (la unidad fundamental)
                    ┌───────────┬───────────┬───────────┬───────────┐
                 Variant      Preset      Snapshot    Benchmark    Preview
              (implementa-  (configura-  (captura    (referencia  (manifesta-
               ción alt.)    ción)        inmutable)  externa)     ción viva)

                      EJE DE ENTORNO (dónde y cómo trabajas)
                      ──────────────────────────────────────
        PROJECT ────contiene──── LAB(s) ────hospedan──── Resources
        (el mundo)              (talleres por tipo)
        SESSION  = tu contexto de trabajo en el tiempo (qué tienes abierto + overrides)
        WORKSPACE = tu disposición del espacio (qué zonas ves y cómo)
```

**La frase que el usuario internaliza (el modelo en una línea):**
> *El Developer Studio es un **taller** (**Project**) con **bancos de trabajo** especializados
> (**Labs**); en cada banco creas y preservas **Resources**; cada Resource puede tener
> **Variants** (implementaciones), **Presets** (configuraciones), **Snapshots** (capturas
> inmutables) y un **Benchmark** (referencia); lo que ves ejecutándose es su **Preview**; tu
> contexto de trabajo es la **Session** y la disposición del espacio es el **Workspace**.*

**Regla raíz del modelo:** *todo lo que se crea o preserva ES un Resource o una faceta de un
Resource; todo lo demás es el entorno donde trabajas.* Si algún día un concepto no encaja en
"faceta de Resource" ni en "entorno", es señal de que el modelo necesita revisión (no de que se
añada una excepción).

---

## Parte II — Conceptos (definición canónica: *es / no es*)

Cada concepto: una frase canónica, qué **es**, qué **no es**, y su tipo (fundamental/derivado).

### Fundamentales

**PROJECT** — *el universo completo sobre el que opera el Studio.*
- **Es:** el cuerpo entero de Resources, su historia y el conocimiento del Studio para **un**
  código de juego (el Pokémon TCG Clone). El Studio sirve a **un** Project.
- **No es:** un "proyecto de animación" suelto, ni una baraja, ni un archivo. Singular por
  diseño. *(Regla de Oro: el Project del Studio refleja el juego real; no inventa contenido.)*

**LAB** — *el taller donde se crean y editan los Resources de UN tipo.*
- **Es:** un entorno de trabajo ligado a exactamente un **ResourceKind** (Animation Lab ↔
  animaciones, Rules Lab ↔ escenarios de estado, Shader Lab ↔ shaders…). Es un **lugar**.
- **No es:** un Resource, ni un panel, ni una herramienta suelta. Un Lab **hospeda** Resources;
  no los *es*.

**RESOURCE** ★ — *la unidad fundamental de creación y preservación del Studio.*
- **Es:** una cosa autorada y gestionada por el Studio (una animación, un shader, un escenario
  de Rules, un componente de UI…). Todo el resto del vocabulario orbita el Resource.
- **No es:** un archivo crudo (eso es un **Asset**, ver Parte V), ni su representación en
  pantalla (eso es el **Preview**). El Resource es la *cosa*, no su *imagen*.

### Derivados de contenido (definidos SIEMPRE respecto a un Resource)

**VARIANT** — *una implementación alternativa del MISMO Resource.*
- **Es:** una realización distinta con la misma identidad de Resource (Canonical, Experimental,
  MarvelSnapStyle, HearthstoneStyle, Legacy…). Cambia el *cómo se hace* (receta/comportamiento).
- **No es:** un Preset (eso cambia valores, no la implementación) ni un Resource nuevo. Un
  Resource tiene ≥1 Variant (al menos la Canonical).

**PRESET** — *un conjunto de VALORES de parámetros con nombre, aplicable a un Resource/Variant.*
- **Es:** una configuración reutilizable de los "mandos" (duration, overshoot…). Cambia el
  *ajuste*, no la implementación.
- **No es:** una Variant (misma receta, distintos valores) ni un Snapshot (no es inmutable ni
  histórico). *La distinción Variant↔Preset es la más importante del canon.*

**SNAPSHOT** — *una captura INMUTABLE y reproducible de un Resource en un momento.*
- **Es:** el estado congelado `(Resource, Variant, valores, entorno)` que se puede reabrir años
  después bit a bit (P13). Es historia; no se edita.
- **No es:** una Variant (viva y editable) ni un "guardado" cualquiera. La **historia** de un
  Resource ES su serie ordenada de Snapshots. "v1..vN" son Snapshots, no un concepto aparte.

**BENCHMARK** — *la referencia externa contra la que se juzga un Resource.*
- **Es:** el estándar de calidad ("cómo debería verse/sentirse") + la documentación de las
  skills (`animation-benchmark`/`implementation-research`/`revision-critica`). Es verdad
  externa.
- **No es:** una implementación nuestra. Una Variant puede *imitar* un Benchmark
  (MarvelSnapStyle), pero el Benchmark **no** es una Variant: es la vara de medir, no lo medido.

**PREVIEW** — *la manifestación viva y honesta de un Resource en ejecución.*
- **Es:** la vista que renderiza `(Resource, Variant, valores)` reproduciéndose. Es *el mapa*,
  no *el territorio*.
- **No es:** el Resource (la cosa) ni una zona espacial genérica. El Preview **muestra** un
  Resource; puede mostrar **dos** solo en modo comparación (DC-D1). *(P15: si es headless,
  declara que no es pixel-perfect.)*

### Derivados de entorno (tu manera de trabajar, no contenido)

**SESSION** — *tu contexto de trabajo a lo largo del tiempo.*
- **Es:** qué tienes abierto (Lab, Resource, Variant), tus overrides no guardados, la posición
  de timeline y el Workspace en uso. Es restaurable (P1: abrir = continuar).
- **No es:** un artefacto preservado de un Resource (eso es un Snapshot). La Session es sobre
  **ti trabajando**, no sobre una cosa terminada.

**WORKSPACE** — *tu disposición del espacio.*
- **Es:** la configuración espacial (qué zonas ves, tamaños, layout; "foco-preview" vs
  "curaduría"). Trata del **espacio** (P12 sobre P9).
- **No es:** una Session (contenido-en-progreso). Una Session *usa* un Workspace; no son lo
  mismo.

---

## Parte III — Relaciones entre conceptos (con cardinalidad)

```
PROJECT 1 ──contiene──► N LAB
LAB     1 ──hospeda───► N RESOURCE            (un Lab = un ResourceKind)
RESOURCE N ──vive en──► 1 LAB
RESOURCE 1 ──tiene────► N VARIANT   (≥1: siempre existe la Canonical)
RESOURCE 1 ──tiene────► N PRESET
RESOURCE 1 ──tiene────► N SNAPSHOT  (su historia = serie ordenada de Snapshots)
RESOURCE 1 ──se juzga► 1..N BENCHMARK (referencia externa; puede haber varias, p.ej. Marvel Snap + TCG Live)
RESOURCE 1 ──se ve en► 1 PREVIEW    (2 solo en modo comparación, DC-D1)
VARIANT  N ──aplica──► valores base; un PRESET puede aplicarse sobre una Variant
SESSION  1 ──apunta a► (1 LAB activo, 1 RESOURCE, 1 VARIANT, overrides, 1 WORKSPACE)
SNAPSHOT captura ────► (RESOURCE + VARIANT + valores + entorno)   [inmutable]
```

**Lecturas clave del grafo (invariantes semánticas):**
1. **Resource es el centro:** Variant/Preset/Snapshot/Benchmark/Preview **no existen solos**;
   siempre son "de un Resource". Nunca se nombran sin su Resource.
2. **Contenido vs entorno no se cruzan:** un Lab (entorno) nunca "es" un Resource; una Session
   (entorno) nunca "es" un Snapshot (contenido).
3. **Vivo vs congelado:** Variant/Preset son editables (vivos); Snapshot es inmutable
   (congelado). Es la frontera que evita confundir "estoy iterando" con "estoy preservando".
4. **Nuestro vs externo:** Variant es nuestra; Benchmark es ajeno. La comparación (W9) es
   *nuestro vs externo*, y esa asimetría es semánticamente significativa.

---

## Parte IV — Conceptos fundamentales vs derivados

| Nivel | Conceptos | Por qué |
|-------|-----------|---------|
| **Fundamentales** | **Project · Lab · Resource** | No se definen en términos de otros; son la base. |
| **Derivados de contenido** | Variant · Preset · Snapshot · Benchmark · Preview | Se definen SIEMPRE como "de un Resource". |
| **Derivados de entorno** | Session · Workspace | Describen *cómo trabajas*, no *qué creas*. |

Regla: un derivado **nunca** se documenta ni se nombra sin anclarlo a su fundamental
("la Variant *del Resource* X", "la Session actual").

---

## Parte V — Confusiones, sinónimos prohibidos, términos ambiguos

### Las 6 confusiones a prevenir activamente
1. **Variant ↔ Preset** (la más peligrosa): implementación vs configuración. Regla mnemónica:
   *Variant cambia el CÓMO; Preset cambia el CUÁNTO.*
2. **Snapshot ↔ Variant:** congelado/histórico vs vivo/editable.
3. **Snapshot ↔ Session:** artefacto preservado de un Resource vs tu contexto de trabajo.
4. **Session ↔ Workspace:** contenido-en-progreso vs disposición del espacio.
5. **Preview ↔ Resource:** la vista vs la cosa (el mapa no es el territorio).
6. **Benchmark ↔ Variant:** referencia externa vs implementación nuestra.

### Sinónimos PROHIBIDOS (un concepto = una palabra, en docs, UI, código y API)
| Concepto canónico | Prohibido decir | Por qué |
|-------------------|-----------------|---------|
| Resource | Asset*, Item, Entity, Object | "Asset" se reserva para archivo crudo (ver abajo); el resto diluye. |
| Variant | Experiment, Version, Style, Flavor | "Version" se confunde con Snapshot; "Style" es un nombre de Variant, no el concepto. |
| Preset | Config, Configuration, Profile, Settings | Ambiguos; "Preset" es preciso y estándar. |
| Snapshot | Save, Backup, Version, Checkpoint, Revision | El artefacto histórico se llama SIEMPRE Snapshot. |
| Benchmark | Reference, Target, Baseline, Gold | "Reference" es descripción, no el artefacto. |
| Preview | Scene, Render, Viewport, Output, Canvas | "Viewport/Canvas" son términos espaciales; "Preview" es la vista de contenido. |
| Lab | Tool, Module, Section, Panel, Tab | "Panel/Tab" son espaciales; "Module" es de arquitectura de build. |
| Session | State, Context, Snapshot(!) | Nunca llamar "snapshot" a una Session. |
| Workspace | Layout, Perspective, View, Mode | "Layout" es su contenido, no su nombre; "View/Mode" son ambiguos. |
| Project | Workspace(!), Repo, Game | No confundir Project (mundo del Studio) con Workspace (espacio). |

**\*Asset** es un término **permitido y distinto**, no un sinónimo: un **Asset** es un *archivo
crudo externo* (imagen, audio) del que un Resource puede **depender**. Asset ≠ Resource: el
Resource es autorado y gestionado; el Asset es materia prima referenciada.

### Términos ambiguos a evitar por completo
- **"Version"** como sustantivo-artefacto → usar **Snapshot**. "version" solo como *atributo/
  etiqueta* de un Snapshot (`v3`), nunca como concepto independiente.
- **"View"** → demasiado usado; preferir Preview (contenido) o Workspace (espacio) según toque.
- **"Mode"** → permitido solo para modos acotados y nombrados (p. ej. "modo comparación" del
  Preview, DC-D1); nunca como concepto de primer nivel.

### Reglas de nomenclatura (canon)
1. **Un concepto, una palabra.** Sin sinónimos en docs, UI, código ni API.
2. **Significado invariante entre Labs.** Ninguna palabra cambia de sentido según el Lab (regla
   central de esta fase). Un Snapshot en Rules Lab significa lo mismo que en Animation Lab.
3. **Singular, sustantivo, PascalCase** para el tipo de concepto (Lab, Resource, Variant…).
4. **Derivado siempre anclado** a su fundamental al nombrarlo.
5. **Léxico plano y preciso** (no evocador): se optimiza para durar, no para sonar bien.
6. **Extensión, no redefinición:** un concepto nuevo se añade sin cambiar el sentido de los
   existentes; si obliga a redefinir uno, se audita el modelo (no se parchea el término).

---

## Parte VI — Canon Terminológico (glosario oficial de referencia)

Referencia obligatoria para TODA documentación, UI, código y API futuros del Studio.

| Término | Tipo | Definición canónica (una línea) |
|---------|------|--------------------------------|
| **Project** | Fundamental | El universo completo de Resources e historia para un juego; el Studio sirve a uno. |
| **Lab** | Fundamental | Taller que crea/edita los Resources de un único ResourceKind. |
| **Resource** | Fundamental | Unidad fundamental de creación y preservación del Studio. |
| **Variant** | Derivado (contenido) | Implementación alternativa del mismo Resource (cambia el cómo). |
| **Preset** | Derivado (contenido) | Conjunto de valores de parámetros con nombre (cambia el cuánto). |
| **Snapshot** | Derivado (contenido) | Captura inmutable y reproducible de un Resource en un momento. |
| **Benchmark** | Derivado (contenido) | Referencia externa contra la que se juzga un Resource. |
| **Preview** | Derivado (contenido) | Manifestación viva y honesta de un Resource en ejecución. |
| **Session** | Derivado (entorno) | Tu contexto de trabajo en el tiempo, restaurable. |
| **Workspace** | Derivado (entorno) | Tu disposición del espacio (qué zonas y cómo). |
| **Asset** | Auxiliar | Archivo crudo externo del que un Resource puede depender (≠ Resource). |
| **ResourceKind** | Auxiliar | Categoría de Resource que define a qué Lab pertenece. |

---

# Auditoría Semántica

Auditoría específica del lenguaje (además de la de coherencia).

1. **¿Concepto ambiguo?** Se eliminan los dos riesgos históricos: "version" (→ Snapshot como
   artefacto; "version" solo etiqueta) y "view" (→ Preview vs Workspace). No queda ningún
   concepto de primer nivel con sentido difuso.
2. **¿Dos palabras para un concepto?** No: la tabla de sinónimos prohibidos garantiza 1
   palabra por concepto. Se corrige una deriva previa: docs anteriores hablaban de
   "definiciones versionadas / v1..vN" — **se canoniza que esos son Snapshots**, no un concepto
   "version" aparte.
3. **¿Un concepto con dos significados?** No, tras separar Session (contenido-en-progreso) de
   Workspace (espacio), y Preview (vista) de Resource (cosa). El caso más frágil, "Preset vs
   Variant", queda resuelto con la regla CÓMO/CUÁNTO.
4. **¿Puede un usuario nuevo construir el modelo mental?** Sí: dos ejes (contenido/entorno) +
   un centro (Resource) + la frase-resumen. La carga es baja porque los derivados se *deducen*
   del Resource, no se memorizan sueltos.
5. **¿Palabra a prohibir?** Sí, las de la tabla; en especial **"version"** como artefacto y el
   uso cruzado de **"snapshot"** para Sessions. También **"asset" como sinónimo de Resource**
   (se admite solo con su sentido estricto de archivo crudo).
6. **¿Escala décadas?** Sí: léxico plano y convencional (estilo Git/Figma), significado
   invariante entre Labs, y crecimiento por *extensión*. Añadir el Lab nº 200 no introduce
   vocabulario nuevo: reusa Resource/Variant/Preset/Snapshot/Benchmark/Preview.

---

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No; los ancla en lenguaje. Session↔P1 (continuidad), Snapshot↔P13
   (preservación), Preview↔P15 (honestidad), Workspace↔P12/P9.
2. **¿Contradice la Arquitectura Espacial?** No, la refuerza: distingue **contenido** (Resource
   y facetas) de **espacio** (zonas). Evita el error de llamar "Preview" a la *zona* Z6 y a la
   *cosa* mostrada: la zona es espacial; el Preview es el contenido que vive en ella.
3. **¿Contradice los Workflows?** No; los precisa. W6/W7 (Variant), W10/W11 (Preset), W15
   (Snapshot), W8/W9 (Benchmark), W1/W14 (Session), W20 (Workspace) quedan nombrados sin
   ambigüedad.
4. **¿Excepciones innecesarias?** No; **elimina** ambigüedades previas (unifica "version"→
   Snapshot). Ninguna excepción nueva.
5. **¿Duplicación de responsabilidades?** No; al contrario, separa responsabilidades que el
   lenguaje mezclaba (Session/Workspace, Preview/Resource).
6. **¿Regla de Oro?** Sí: el Project refleja el juego real; Resource/Preview no modelan lógica
   propia; el Rules Lab produce Resources que *son* `GameState` del engine real.
7. **¿Escala a cientos de Labs / miles de recursos / décadas?** Sí: vocabulario constante;
   crece el nº de Resources, no el de conceptos.
8. **¿Deuda futura?** Menor: hay documentos previos con "version/definiciones versionadas" que
   deberán releerse a la luz de "Snapshot" (deuda de *migración terminológica*, no de modelo).
   Anotada.
9. **¿Decisiones a revisar?** Una recomendación: aplicar el canon retroactivamente en `M0-CORE`
   y `ARCHITECTURE` (ya usan Resource/Lab/Snapshot correctamente; revisar solo "version").
10. **¿Canónico?** **Sí.** Con la única tarea de higiene de alinear el término "version" en
    documentos previos (no bloquea).

---

# Impacto en el Futuro

- **Decisiones condicionadas:** el Sistema de Interacción (Fase 5) y el Diseño Visual heredan
  este vocabulario como *contrato de etiquetas*: ninguna pantalla podrá inventar un término
  fuera del canon ni reusar uno con otro sentido. Las etiquetas de UI = términos del canon.
- **Módulos afectados:** `studio-contracts` (los tipos ya se llaman ResourceDescriptor/
  ResourceKind/VariantRef/SnapshotId → alinear cualquier "version"); toda la API/WS
  (nombres de mensajes y campos); la documentación completa del Studio.
- **Oportunidades:** onboarding drástico más barato (un modelo, no uno por Lab); búsqueda y
  catálogo coherentes (facetas = conceptos del canon); i18n más simple (un término → una
  traducción estable); reducción de malentendidos entre *tu yo presente y tu yo futuro* —y con
  las IA de asistencia— a lo largo de los años (proyecto unipersonal).
- **Limitaciones:** el canon **congela el significado**; introducir un concepto genuinamente
  nuevo exigirá pasar por esta fase (Auditoría Semántica), no colar una palabra. Es intencional.
- **Riesgos a vigilar:** (R1) *deriva terminológica* al crecer los Labs (el propio desarrollador
  —o una IA de asistencia— introduciendo jerga nueva con los años) → el canon es de cumplimiento
  obligatorio al autorar. (R2) *"version"
  residual* en docs antiguos → tarea de higiene. (R3) presión por sinónimos "más bonitos" (Emil)
  → se resiste: el dominio es plano a propósito.
- **Reevaluación recomendada:** revisar el canon al aparecer el primer Lab cuyo dominio no
  encaje limpiamente en "Resource + facetas" (p. ej. Audio/Camera si introdujeran nociones
  temporales propias), para confirmar que no fuerza el modelo.

---

# ¿A la altura de un producto AAA?

> *¿Estaría este modelo mental a la altura de Figma, Unreal, JetBrains, VS Code o Unity?*

**Sí.** Justificación:
- Esas herramientas escalan **precisamente** porque tienen un vocabulario de dominio pequeño,
  plano e invariante: Git (`commit`/`branch`/`merge`), Figma (`Frame`/`Component`/`Instance`),
  Blender (`Object`/`Mesh`/`Modifier`), Unreal (`Actor`/`Component`/`Blueprint`). Nuestro canon
  sigue el **mismo patrón**: pocos fundamentales (Project/Lab/Resource) + derivados deducibles.
- Aporta algo que muchas ni documentan explícitamente: la **separación formal
  contenido/entorno** y la distinción **Variant(cómo)/Preset(cuánto)/Snapshot(congelado)**, que
  en herramientas reales suele aprenderse por dolor. Documentarlo de entrada nos pone *por
  encima* de la media en coherencia semántica.
- **Qué faltaría para el máximo (fases siguientes, no aquí):** que la UI *refuerce* el modelo
  (que cada término aparezca donde el modelo predice, con la misma etiqueta) y que las
  interacciones-firma le den identidad — eso pertenece al Sistema de Interacción y al Diseño
  Visual. A nivel de **modelo**, ya está al nivel de esas herramientas.
