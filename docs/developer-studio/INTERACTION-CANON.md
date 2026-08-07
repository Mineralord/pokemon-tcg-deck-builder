# Developer Studio — Interaction Canon (Fase 5)

**El lenguaje de interacción universal del Studio.** No hay botones, componentes, pantallas ni
iconos: solo **comportamiento**. Define cómo *conversa* el usuario con la herramienta, igual en
**todos los Labs presentes y futuros**.

**Promesa del canon:** al diseñar cualquier Lab futuro (Animation, Rules, Shader, Benchmark…)
**no se decidirá cómo interactúa el usuario** — eso ya está aquí. Un Lab solo decide *qué
información muestra*. Todo lo demás (selección, edición, búsqueda, undo, drag, navegación) lo
dicta este documento.

**Análisis integrado por dos lentes** (ambas activas esta sesión):
- `ui-ux-pro-max` → accesibilidad, teclado, consistencia, predecibilidad, escalabilidad.
- `emil-design-eng` → percepción, feedback, feel, interruptibilidad, identidad.
- **Árbitro de conflictos:** el *framework de frecuencia* de Emil (no animar/adornar lo que se
  repite mucho; reservar el deleite a lo raro) — satisface a la vez la contención que pide
  `ui-ux-pro-max`. Donde discrepan, decide la frecuencia de uso.

**Canon previo obligatorio:** P1–P15 · Arquitectura Espacial (Z1–Z12) · Workflows (W1–W21) ·
Layout (DC-D1/DC-D2) · Modelo Mental (Resource/Variant/Preset/Snapshot/Preview/Inspector/
Timeline/Workspace/Lab) · 4 pilares (Unipersonal, Especialización, juego-producto, Studio-
herramienta).

**Formato por decisión (8 campos):** Problema · Alternativas · Ventajas · Desventajas ·
**Decisión canónica** · Impacto en todos los Labs · Impacto en el flujo · Mantenibilidad a
décadas.

---

## IC-0 · Filosofía de interacción (la ley marco)

- **Problema:** sin una gramática única, cada Lab inventaría su forma de interactuar y el Studio
  se volvería un archipiélago inaprendible a escala de cientos de Labs.
- **Alternativas:** (a) libertad por Lab; (b) gramática común estricta; (c) común con
  excepciones por Lab.
- **Ventajas de (b):** transferencia total de aprendizaje (aprende uno, sabes todos);
  predecibilidad; menos código. **Desventajas:** exige disciplina para no crear excepciones.
- **Decisión canónica:** **gramática de interacción ÚNICA e invariante entre Labs.** Cinco
  axiomas rectores: **(1) Manipulación directa** cuando el objeto es espacial; **(2) el
  resultado responde en vivo** (todo cambio se ve sin recargar/confirmar); **(3) reversibilidad
  sobre confirmación** (P8: hacer + deshacer, no "¿seguro?"); **(4) paridad de teclado** (todo
  lo alcanzable con puntero lo es con teclado y con la Command Palette); **(5) el
  comportamiento no depende del Lab**, solo el contenido.
- **Impacto en Labs:** ningún Lab define interacción; hereda esta.
- **Impacto en el flujo:** el bucle del 70% (W5) es idéntico en cada Lab.
- **Mantenibilidad:** una sola capa de interacción que mantener 20 años, no N.

---

## IC-1 · Selección, selección múltiple y focus

- **Problema:** hay que saber *sobre qué* actúan la edición, el teclado y el Inspector, de forma
  inequívoca.
- **Alternativas:** selección implícita por hover; selección explícita por clic; modo-herramienta
  (como editores de dibujo).
- **Ventajas** del clic explícito: previsible, sin cambios accidentales. **Desventajas:** un paso
  más que el hover (aceptable).
- **Decisión canónica:**
  - **Un clic selecciona** el objeto bajo el cursor y lo hace el **focus** (el "objeto activo").
  - **Existe exactamente UN focus** en todo momento; **el Inspector SIEMPRE refleja el focus**
    (regla dura: Inspector = ventana del objeto activo).
  - **Selección múltiple**: aditiva con modificador (Cmd/Ctrl+clic) y por rango (Shift+clic);
    con múltiples seleccionados, el Inspector muestra **los parámetros comunes** y editarlos
    **aplica a todos**; el focus es el último añadido.
  - **Clic en vacío limpia** la selección (y el Inspector queda sin objeto).
  - **Hover NUNCA selecciona** (IC-4).
- **Impacto en Labs:** todo Lab con objetos (Pokémon en Rules, nodos, capas…) usa el mismo
  modelo; no reinventa "qué está seleccionado".
- **Impacto en el flujo:** el par Preview↔Inspector (W5) se ancla al focus sin ambigüedad.
- **Mantenibilidad:** un solo modelo de selección/focus para toda la app.

---

## IC-2 · Acción directa vs acción por Inspector

- **Problema:** ¿cuándo se manipula el objeto directamente y cuándo se edita por parámetros?
- **Alternativas:** todo por Inspector (formulario); todo directo (manipulación); híbrido por
  naturaleza del dato.
- **Ventajas** del híbrido: cada dato se edita donde es natural. **Desventajas:** hay que definir
  la frontera con claridad para que sea predecible.
- **Decisión canónica:** **frontera por naturaleza del dato**:
  - **Directo (en el Preview/Área de Trabajo)** = lo **espacial/posicional/relacional**: mover,
    colocar, conectar, reordenar (p. ej. poner un Pokémon en la Banca en Rules Lab).
  - **Inspector** = lo **preciso/numérico/enumerado**: duration, overshoot, curve, tipo, capa…
  - **Ambos están vinculados en vivo** (editar en uno actualiza el otro; hot-reload, P11).
  - **Especialización (pilar 2):** NO hay edición de texto libre genérica; editar = manipular
    objetos o ajustar parámetros con esquema, nunca "un editor de texto".
- **Impacto en Labs:** cada Lab declara qué es directo y qué va al Inspector, pero el
  *mecanismo* es común.
- **Impacto en el flujo:** el usuario nunca duda dónde se cambia algo.
- **Mantenibilidad:** la lógica de edición vive en dos canales bien definidos, no dispersa.

---

## IC-3 · Drag & Drop

- **Problema:** mover/colocar/reordenar objetos espaciales de forma natural y reversible.
- **Alternativas:** solo por Inspector (coordenadas); drag libre; drag con *snapping*.
- **Ventajas** del drag con snapping + física: rápido e intuitivo, con precisión asistida.
  **Desventajas:** requiere reglas de destino válidas y feedback de "dónde caerá".
- **Decisión canónica:**
  - Drag para **mover/colocar/conectar/reordenar** en el área de trabajo; con **momentum/feel de
    resorte** interrumpible (Emil: los gestos mantienen velocidad) y **snapping** a destinos
    válidos.
  - **Destino válido siempre indicado** antes de soltar; soltar en inválido **no hace nada**
    (no error).
  - **Todo drag es reversible con Undo** (IC-6); ningún drag confirma nada irreversible.
  - **Nunca es la ÚNICA vía**: lo mismo se logra por Inspector/Palette (paridad, accesibilidad).
- **Impacto en Labs:** Rules Lab (colocar/retirar), Battlefield, reordenar capas… todos con el
  mismo gesto y las mismas garantías.
- **Impacto en el flujo:** manipulación directa = el corazón de Labs espaciales (W12).
- **Mantenibilidad:** una implementación de drag (con validación de destino inyectable por Lab).

---

## IC-4 · Hover, doble clic y clic derecho

- **Problema:** asignar significados **únicos e invariantes** a los tres gestos de puntero
  secundarios.
- **Alternativas:** significados por Lab (caos); significados globales fijos.
- **Ventajas** de globales fijos: predecibilidad absoluta. **Desventajas:** ninguna real si se
  eligen bien.
- **Decisión canónica (un significado, en todo el Studio):**
  - **Hover = revelar, nunca comprometer.** Muestra affordance/preview/tooltip/valor bajo
    demanda (enseñanza en contexto, P14). **Jamás** cambia estado, selecciona ni ejecuta algo
    destructivo. (Desktop-only, DC-D2 → el hover es ciudadano de primera clase.)
  - **Doble clic = "entrar / editar en el sitio".** Abre un Resource, entra a editar un valor,
    despliega en detalle. Es el gesto universal de "profundizar". Un solo significado siempre.
  - **Clic derecho = menú contextual del objeto bajo el cursor.** Acelerador de acciones
    aplicables al focus/selección; **orden de acciones consistente**; **nunca la única vía**
    (todo también en la Palette). Sin acción destructiva sin marca clara.
- **Impacto en Labs:** ningún Lab redefine estos gestos; los llena con su contenido.
- **Impacto en el flujo:** el usuario "sabe" qué hará cada gesto antes de hacerlo.
- **Mantenibilidad:** tres contratos globales; los Labs solo aportan la *lista* de acciones.

---

## IC-5 · Navegación por teclado, Command Palette y Shortcuts

- **Problema:** productividad experta y accesibilidad sin depender del ratón.
- **Alternativas:** ratón-primero; teclado-primero; Palette como único acelerador.
- **Ventajas** de teclado-primero + Palette: velocidad, accesibilidad, descubribilidad.
  **Desventajas:** requiere disciplina de mantener paridad.
- **Decisión canónica:**
  - **Operabilidad total por teclado**; **orden de tabulación = orden visual** (a11y).
  - **Command Palette = superficie universal de acciones**: fuzzy, consciente del contexto (Lab
    activo + focus), y **TODA acción del Studio es alcanzable aquí** (garantiza paridad y
    descubrimiento). Es el "fallback" que hace innecesario memorizar menús.
  - **Home-row de shortcuts IDÉNTICO en todos los Labs** para lo universal (undo/redo, buscar,
    Palette, play/pause, cambiar de Lab, guardar Snapshot…); cada Lab puede añadir shortcuts
    *propios* que **nunca** colisionen con los universales.
  - **Emil (frecuencia):** las acciones de teclado son de altísima frecuencia → **sin animación**
    (abrir la Palette, ejecutar un comando, cambiar de Lab NO animan). La instantaneidad ES el
    feature.
  - **La Palette enseña sus propios atajos** (muestra la tecla junto al comando → el uso migra
    al atajo directo).
- **Impacto en Labs:** heredan el home-row; declaran solo extras sin colisión.
- **Impacto en el flujo:** el experto vuela; el novato descubre por la Palette (W19).
- **Mantenibilidad:** un registro central de comandos/atajos con espacio de nombres por Lab.

---

## IC-6 · Undo / Redo, Historial y Estados temporales

- **Problema:** permitir experimentar sin miedo (P8) y sin perder trabajo (P13).
- **Alternativas:** confirmaciones; undo lineal; undo ramificado (tree).
- **Ventajas** del undo **lineal**: modelo mental trivial, predecible. **Desventajas:** no
  conserva ramas abandonadas (mitigado por Snapshots para lo que valga preservar).
- **Decisión canónica:**
  - **Undo/Redo universal y LINEAL**, por sesión, que cubre **toda** edición reversible
    (incluidos los ajustes de parámetros en caliente y los drags). Es el **mecanismo primario de
    seguridad** → **sustituye a las confirmaciones** (IC-7).
  - **Unipersonal (pilar 1):** al haber un solo actor, el undo es una **pila lineal simple**, sin
    resolución de conflictos, sin merges, sin locks. El principio *simplifica* el modelo.
  - **Estados temporales** (arrastrando, editando un valor, overrides no guardados) son
    **siempre claramente reversibles** y **nunca se persisten en silencio**; se distinguen del
    estado comprometido.
  - **Historial** = manifestación navegable del undo + la serie de **Snapshots** (canon) del
    Resource; volver a un punto es coherente con la continuidad (W1/W15).
  - **Emil:** las transiciones de estado usan *transitions* interrumpibles (no keyframes) para
    que undo/redo rápidos encadenen sin saltos.
- **Impacto en Labs:** cada edición de Lab debe ser expresable como paso reversible (contrato).
- **Impacto en el flujo:** experimentación libre; el "me gustaba más antes" se resuelve con undo.
- **Mantenibilidad:** un solo motor de undo lineal; los Labs registran comandos reversibles.

---

## IC-7 · Confirmaciones y acciones destructivas

- **Problema:** proteger sin fatigar; los "¿estás seguro?" constantes matan la velocidad.
- **Alternativas:** confirmar todo; no confirmar nada; confirmar solo lo irreversible.
- **Ventajas** de "solo lo irreversible": velocidad + seguridad real. **Desventajas:** exige
  clasificar bien qué es irreversible.
- **Decisión canónica:**
  - **Por defecto NO hay confirmaciones**: se actúa y se **deshace** si hace falta (IC-6).
  - **Confirmación SOLO para lo genuinamente irreversible o fuera del Studio** (p. ej. borrar un
    Snapshot de forma permanente, o algo que no cabe en el undo). Aun así, se prefiere
    **reversibilidad** (borrado suave con ventana de undo) antes que un diálogo.
  - **Acciones destructivas minimizadas**; cuando existan, marcadas y separadas de las comunes,
    nunca en un gesto casual (no en hover, no como acción por defecto).
- **Impacto en Labs:** ningún Lab añade diálogos de confirmación salvo para lo irreversible.
- **Impacto en el flujo:** cero fricción en el 99% de acciones.
- **Mantenibilidad:** una política única; menos diálogos que mantener y traducir.

---

## IC-8 · Feedback inmediato, operaciones largas, errores y recuperación

- **Problema:** el usuario debe sentir siempre que la herramienta *le oyó* y saber cuándo algo
  tarda o falla, sin bloquearse.
- **Alternativas:** feedback bloqueante (modal/spinner que congela); feedback no bloqueante y
  honesto.
- **Ventajas** del no bloqueante: nunca se pierde el bucle. **Desventajas:** requiere diseñar el
  progreso y el error como estados, no como interrupciones.
- **Decisión canónica:**
  - **Acuse < ~100 ms** para toda acción (P11); aplicación **optimista** para hot-reload (se
    muestra el efecto ya; si el backend corrige, se ajusta).
  - **Operaciones largas: NO bloquean** el bucle; muestran **progreso honesto** (P15) y son
    **cancelables**. Nunca un "freeze".
  - **Errores: cerca de la causa, en lenguaje claro, no modales** salvo que impidan de verdad
    seguir; **siempre ofrecen recuperación** (reintentar / deshacer / ver detalle en Consola).
    Su resumen vive en la Status Bar (Z2).
  - **Emil:** un *spinner* percibe más rápido si gira rápido; la percepción de velocidad importa
    tanto como la real.
- **Impacto en Labs:** todo Lab reporta progreso/errores por el mismo canal (Status Bar +
  Consola), no inventa modales.
- **Impacto en el flujo:** el bucle nunca se congela; los fallos no expulsan al usuario.
- **Mantenibilidad:** un sistema de feedback/errores central; los Labs solo emiten eventos.

---

## IC-9 · Copiar, Pegar, Duplicar, Renombrar, Favoritos

- **Problema:** operaciones de objeto universales que hoy cada herramienta implementa distinto.
- **Alternativas:** portapapeles de texto; portapapeles **semántico** (objetos del canon).
- **Ventajas** del semántico: pegar "entiende" qué es (un Preset, una Variant); coherente con el
  modelo mental. **Desventajas:** debe definirse qué es pegable dónde.
- **Decisión canónica:**
  - **Portapapeles SEMÁNTICO**: se copian **objetos del canon** (Resource/Variant/Preset/
    selección del área de trabajo), no texto. **Pegar es consciente del contexto** (un Preset
    solo se pega donde aplica).
  - **Duplicar = copia instantánea con nombre provisional** (coherente con W4/W7 "crear =
    ejecutable ya"; nunca abre un formulario).
  - **Renombrar = en el sitio, diferido y no bloqueante**; **jamás requerido al crear** (canon).
  - **Favoritos = marca personal ligera** para acelerar el reencuentro; es **una faceta de
    búsqueda/filtro** (IC-10). Unipersonal (pilar 1): son *tus* favoritos, sin compartir.
- **Impacto en Labs:** todos exponen copy/paste/duplicate/rename/favorite con idéntica
  semántica; solo declaran qué tipos participan.
- **Impacto en el flujo:** operaciones de objeto reflejas, sin reaprender por Lab.
- **Mantenibilidad:** un portapapeles semántico central; los Labs registran tipos pegables.

---

## IC-10 · Búsqueda y Filtros

- **Problema:** llegar a lo que quieres entre miles de Resources (P7), igual en todos lados.
- **Alternativas:** enumeración/árbol; búsqueda incremental facetada.
- **Ventajas** de búsqueda facetada: escala a décadas; O(1) percibido. **Desventajas:** depende
  de buenos metadatos (ya garantizados por el catálogo).
- **Decisión canónica:**
  - **Búsqueda incremental** (resultados en vivo, Enter abre el primero) y **filtros facetados
    por conceptos del canon**: kind, tag, status (Draft/Experimental/Canonical/Deprecated),
    fecha, favorito.
  - **Mismo comportamiento de búsqueda en toda la app** y **mismo motor que la Command Palette**
    (buscar recursos y buscar acciones = el mismo gesto mental, O4).
  - **Nunca** navegación por sistema de archivos (pilar 2): se busca por Resources, no por
    carpetas.
- **Impacto en Labs:** la Biblioteca y cualquier selector usan la misma búsqueda/filtros.
- **Impacto en el flujo:** encontrar es un reflejo, no una tarea (W2).
- **Mantenibilidad:** un motor de búsqueda/facetas; añadir un Lab no añade forma de buscar.

---

## IC-11 · Presets y Snapshots (interacción)

- **Problema:** aplicar configuraciones y viajar en el tiempo de forma segura y coherente.
- **Alternativas:** aplicar con confirmación; aplicar en vivo y reversible.
- **Ventajas** de en-vivo-reversible: se juzga viendo, sin miedo. **Desventajas:** hay que
  distinguir "aplicar" (barato/reversible) de "capturar" (deliberado).
- **Decisión canónica:**
  - **Aplicar un Preset**: **previsualización al enfocar** (se ve antes de fijar), **commit al
    seleccionar**, **reversible con Undo** (W11). Cambia el *cuánto*, no la implementación (canon
    Preset≠Variant).
  - **Snapshot**: **capturar es un acto deliberado** (autoría, W10/W15 — no ocurre solo);
    **restaurar** reabre el contexto como la continuidad de sesión (W1). Inmutable por canon.
  - **Cambiar de Variant** es instantáneo y reversible (intercambio de implementación, W6).
- **Impacto en Labs:** Preset/Snapshot/Variant se comportan igual en cada Lab (Animation, Rules,
  Shader…): mismo "aplicar/capturar/restaurar".
- **Impacto en el flujo:** iterar (Preset) y preservar (Snapshot) son gestos distintos y claros.
- **Mantenibilidad:** una semántica de preservación; los Labs solo dicen qué parámetros/estado
  entran.

---

## IC-12 · Navegación entre Labs

- **Problema:** alternar de dominio sin perder el hilo de ninguno.
- **Alternativas:** cada cambio reinicia el Lab; cada Lab conserva su estado.
- **Ventajas** de conservar estado: continuidad total. **Desventajas:** más estado que recordar
  (aceptable, unipersonal → simple).
- **Decisión canónica:**
  - Cambiar de Lab es **instantáneo, preserva el estado** de cada Lab (su focus, overrides,
    timeline, Workspace) y ofrece **toggle al último Lab** en un gesto (W17).
  - **Sin animación** (frecuente, Emil) — el marco (Z1/Z2) no cambia, solo el contenido (P9).
  - Alcanzable por selector y por **Command Palette**.
- **Impacto en Labs:** todos participan del mismo mecanismo de conmutación con memoria.
- **Impacto en el flujo:** saltar de Rules a Animation y volver no cuesta contexto.
- **Mantenibilidad:** un conmutador central + estado por Lab; añadir un Lab no lo toca.

---

## IC-13 · Coordinación de zonas (Preview↔Inspector, Preview↔Timeline, Workspaces, Multi-monitor)

- **Problema:** que las zonas del Layout se comporten como un todo coherente, no como paneles
  sueltos.
- **Alternativas:** zonas independientes; zonas acopladas por reglas fijas.
- **Ventajas** del acoplamiento: el Studio se siente "de una pieza". **Desventajas:** hay que
  fijar las reglas de acoplamiento (aquí).
- **Decisión canónica:**
  - **Preview ↔ Inspector (bidireccional, en vivo):** seleccionar en el Preview fija el focus →
    el Inspector lo muestra; editar en el Inspector actualiza el Preview en caliente. **Es el
    bucle del 70% (W5)**; su latencia ≈0 es sagrada (P11).
  - **Preview ↔ Timeline:** la Timeline **conduce el tiempo del Preview** (scrub/step/play);
    editar parámetros con la Timeline detenida en un frame actualiza **ese** frame; *play* es un
    **modo** (Emil: play/scrub de altísima frecuencia → sin adornos, instantáneos).
  - **Workspaces:** conmutar Workspace **reordena SOLO el espacio**, **nunca** el contenido, el
    focus ni la selección (canon: Workspace≠Session). Instantáneo y recordado.
  - **Multi-monitor:** superficies desacopladas **comparten la ÚNICA sesión viva** (pilar 1 → sin
    concurrencia entre ventanas: *last-write-wins* simple); la interacción es idéntica en
    cualquier monitor. Reacoplar es reversible; si un monitor desaparece, sus zonas vuelven.
  - **Comparación (DC-D1):** entrar/salir del modo comparación es **siempre manual**; su
    disposición interna se adapta al espacio.
- **Impacto en Labs:** las reglas de acoplamiento son del marco, no del Lab; todo Lab las hereda.
- **Impacto en el flujo:** el Studio responde como un organismo, no como paneles aislados.
- **Mantenibilidad:** el acoplamiento vive en el shell; los Labs no lo reimplementan.

---

# Decisiones Canónicas (resumen del Interaction Canon)

| ID | Regla canónica (una línea) |
|----|----------------------------|
| **IC-0** | Gramática de interacción única e invariante entre Labs; 5 axiomas (directo · vivo · reversible · paridad-teclado · independiente-del-Lab). |
| **IC-1** | Un clic selecciona; UN focus siempre; el Inspector refleja el focus; multi-select edita parámetros comunes; clic en vacío limpia. |
| **IC-2** | Directo = espacial (Preview); Inspector = numérico/enumerado; vinculados en vivo; sin edición de texto genérica. |
| **IC-3** | Drag para mover/colocar/reordenar, con snapping y feel de resorte, siempre reversible y nunca la única vía. |
| **IC-4** | Hover revela (nunca compromete); doble clic = entrar/editar; clic derecho = menú contextual; significados globales fijos. |
| **IC-5** | Teclado total (tab=orden visual); Command Palette universal (toda acción alcanzable); home-row idéntico entre Labs; sin animación en acciones de teclado. |
| **IC-6** | Undo/Redo lineal universal (incluye params y drags) = seguridad primaria; estados temporales reversibles y nunca persistidos en silencio. |
| **IC-7** | Sin confirmaciones salvo lo irreversible/fuera-del-Studio; preferir borrado reversible; destructivo minimizado y marcado. |
| **IC-8** | Acuse <100 ms; largas no bloquean, progreso honesto y cancelable; errores junto a la causa, no modales, con recuperación. |
| **IC-9** | Portapapeles semántico (objetos, no texto); duplicar instantáneo; renombrar diferido; favoritos = faceta personal. |
| **IC-10** | Búsqueda incremental facetada por conceptos del canon; mismo motor que la Palette; nunca por sistema de archivos. |
| **IC-11** | Preset: previsualiza al enfocar, commit al seleccionar, reversible; Snapshot: capturar deliberado, restaurar = continuidad. |
| **IC-12** | Cambio de Lab instantáneo, con memoria de estado, toggle al último, sin animación. |
| **IC-13** | Preview↔Inspector en vivo (bucle 70%); Timeline conduce el Preview; Workspace mueve solo el espacio; multi-monitor comparte 1 sesión. |

---

# Auditoría Semántica

1. **¿Concepto ambiguo?** No; se reutilizan términos del canon (focus, selección, Preset,
   Snapshot, Variant, Inspector, Timeline, Workspace) sin nuevos sentidos. Se introduce **focus**
   = "el objeto activo único"; queda definido sin chocar con "selección" (focus ⊆ selección).
2. **¿Dos palabras para un concepto?** No; "seleccionar" y "enfocar" se distinguen a propósito
   (selección puede ser múltiple; focus es único).
3. **¿Un concepto con dos significados?** No; "modo" se usa solo acotado (modo comparación, modo
   play), coherente con el canon.
4. **¿Modelo mental construible por un novato?** Sí: 5 axiomas + gestos globales fijos → se
   deduce el resto. Aprender un Lab = aprender su contenido, no su interacción.
5. **¿Palabra a prohibir?** Sí: en interacción, prohibido "editor de texto/terminal" como
   comportamientos (pilar 2); "confirmar" como patrón por defecto (IC-7).
6. **¿Escala décadas?** Sí; gramática constante: añadir Labs no añade gestos ni vocabulario.

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No; los operacionaliza: P3 (bucle IC-13), P5 (coste∝frecuencia en
   IC-5), P8 (IC-6/IC-7), P11 (IC-8), P7 (IC-10), P4/P14 (hover IC-4).
2. **¿Contradice Arquitectura Espacial / Layout / Workflows / Modelo Mental?** No; los ejecuta.
   Ancla el bucle al par Preview↔Inspector (Z6/Z7) y respeta DC-D1/DC-D2.
3. **¿Excepciones innecesarias?** No; su tesis es *cero excepciones por Lab*.
4. **¿Duplicación de responsabilidades?** No; centraliza selección, undo, búsqueda, portapapeles,
   feedback → elimina duplicación futura por Lab.
5. **¿Regla de Oro (I1)?** Respetada: la interacción no introduce lógica de juego; edita
   parámetros/objetos que resuelven engine/framework reales.
6. **¿Escala a cientos de Labs / miles de recursos / décadas?** Sí, por construcción (gramática
   invariante + búsqueda facetada).
7. **¿Deuda futura?** Baja; el riesgo es *presión por gestos especiales por Lab* → se rechaza por
   IC-0.
8. **¿Decisión a revisar?** Ninguna abierta; se apoya en D1/D2 ya cerradas.
9. **¿Canónico?** Sí — Interaction Canon completo y cerrado.

# Auditoría de Filosofía

1. **¿Respeta el Desarrollo Unipersonal?** Sí, y **lo aprovecha**: undo lineal simple (sin
   merges/locks), favoritos personales, multi-monitor sin concurrencia (una sesión viva).
2. **¿Complejidad para múltiples desarrolladores?** Ninguna; se descartó explícitamente la
   coordinación de concurrencia entre ventanas.
3. **¿Puede simplificarse por ser unipersonal?** Ya se hizo (undo lineal, sesión única).
4. **¿IA como herramienta, no actor?** Sí; ninguna interacción modela a la IA como usuario.
5. **¿Simplicidad, preservación, evolución a largo plazo?** Sí: una gramática mínima, undo +
   Snapshots como preservación, y crecimiento por contenido, no por gestos.
6. **¿Especialización Absoluta?** Reforzada: sin edición de texto genérica, sin terminal, sin
   navegación de archivos; se interactúa con Resources, no con un IDE.

# Impacto en el Futuro

- **Decisiones condicionadas:** el Diseño Visual (Fase 6) y los Componentes (Fase 7) deben
  *vestir* estos comportamientos, no inventar interacción; cada Lab futuro solo define contenido.
- **Módulos afectados:** el shell de `studio-web` implementa selección/focus, undo, búsqueda,
  portapapeles, Palette, feedback y acoplamiento de zonas de forma central; `StudioLab` declara
  solo *qué objetos/params/acciones* aporta, no *cómo* se interactúa con ellos.
- **Oportunidades:** diseñar un Lab nuevo se reduce a "declarar contenido"; consistencia de clase
  mundial; onboarding trivial; testing centralizado de la interacción.
- **Limitaciones (deliberadas):** ningún Lab podrá introducir un gesto propio que rompa la
  gramática; si un dominio *exigiera* interacción radicalmente distinta, sería una enmienda a
  este canon (cara y global), no un parche local.
- **Riesgos a vigilar:** (R1) *gestos especiales por Lab* → rechazar vía IC-0. (R2) *confirmaciones
  reintroducidas por costumbre* → IC-7. (R3) *latencia del bucle Preview↔Inspector* degradándose
  al crecer los Labs → vigilar P11 en cada Lab.

# Evaluación frente al estándar AAA

> *¿Estaría este lenguaje de interacción a la altura de Figma, Unreal, JetBrains, VS Code o
> Unity?*

**Sí.** Justificación técnica, no autocomplacencia:
- La marca de esas herramientas es exactamente esto: **una gramática de interacción única** que,
  aprendida una vez, sirve en todo el producto (selección/inspector en Figma y Unity;
  Command Palette y undo universal en VS Code/JetBrains; manipulación directa + inspector en
  Blender/Unreal). Nuestro canon adopta ese patrón como **ley explícita**, no como accidente.
- Incorpora prácticas de élite que muchas solo tienen implícitas: **undo sobre confirmación**,
  **paridad total teclado/Palette**, **feedback no bloqueante y honesto**, **portapapeles
  semántico**, y el **framework de frecuencia** para decidir cuándo algo se anima.
- **Qué faltaría para el nivel máximo (fases siguientes, no aquí):** el *cómo se ve y se siente*
  cada comportamiento (curvas de easing exactas, microcopy, estados de foco visibles, timings
  concretos) — eso es Diseño Visual/Componentes (Fases 6–7). A nivel de **comportamiento**, este
  canon ya está al nivel de esas herramientas, y su *consistencia declarada* lo pone por encima
  de la media.
```
