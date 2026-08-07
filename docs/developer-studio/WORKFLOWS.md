# Developer Studio — Workflows Canónicos (Fase U1.5)

**Diseño de FLUJOS DE TRABAJO. No es diseño visual.** Sin colores, tamaños, componentes ni
pantallas. Aquí se define *cómo trabaja realmente* un desarrollador dentro del Studio.

> Premisa (del usuario, adoptada como ley): en Figma/Blender/VS Code/Unreal lo que se siente
> "natural" no son los colores ni los paneles: son **workflows extremadamente optimizados**.
> La UI es *consecuencia* del workflow, no al revés. Este documento es el contrato que la
> interfaz futura deberá satisfacer.

**Referencias:** principios `P1..P15` (`UX-PRINCIPLES.md`) · zonas `Z1..Z12`
(`SPATIAL-ARCHITECTURE.md`). Cada workflow cita ambos.

**Formato por workflow (10 campos obligatorios):** Objetivo · Inicio · Pasos ideales ·
Decisiones del usuario · Información visible por etapa · Errores posibles · Recuperación ·
Automatización · Reducción de clics · Aprendizaje contextual.

**Métrica rectora:** *clics/gestos hasta el primer resultado observable* y *nº de cambios de
contexto*. Ambos deben tender a cero en el bucle del 70% (P2, P3, P5, P11).

---

## Familia A — Arranque y navegación

### W1 · Abrir el Studio y continuar donde se dejó
- **Objetivo:** reanudar el trabajo sin reconstruir contexto.
- **Inicio:** lanzar el Studio (navegador a `localhost`).
- **Pasos ideales:** (1) abre → (2) aparece la sesión anterior ya restaurada (mismo Lab,
  recurso, variante, overrides no guardados, posición de timeline, layout). **Cero pasos de
  configuración.**
- **Decisiones:** ninguna obligatoria; opcionalmente "empezar limpio".
- **Información por etapa:** al abrir, Top Bar (Z1) muestra el contexto restaurado; Preview
  (Z6) reproduce el último recurso; Status Bar (Z2) confirma conexión con backend.
- **Errores:** backend caído; recurso de la última sesión borrado/renombrado; snapshot
  corrupto.
- **Recuperación:** si el recurso ya no existe, degradar a "última sesión válida" o al Lab sin
  selección, informando en Z2 sin bloquear; nunca pantalla de error dura.
- **Automatización:** persistencia continua del estado de sesión (autosave del contexto, no
  del contenido) → restaurar es automático.
- **Reducción de clics:** de "elegir Lab → elegir recurso → recolocar layout" (3+) a **0**.
- **Aprendizaje contextual:** primera vez sin sesión previa → un rastro discreto sugiere "abre
  un Lab" sin modal intrusivo.
- *Principios:* **P1** (abrir=continuar), P11.

### W2 · Buscar un recurso
- **Objetivo:** llegar a un recurso entre miles (P7).
- **Inicio:** desde cualquier estado (Biblioteca Z3 o Command Palette Z4).
- **Pasos ideales:** (1) invocar búsqueda → (2) escribir término (nombre/tag/tipo/estado/
  fecha) → (3) resultados filtrados en vivo → (4) seleccionar.
- **Decisiones:** por qué faceta filtrar; cuál de los resultados.
- **Información por etapa:** al escribir, resultados con su ficha mínima (nombre, tipo, estado,
  fecha); nunca una lista completa enumerada.
- **Errores:** término ambiguo (demasiados resultados); término sin coincidencias; el usuario
  no sabe el nombre exacto.
- **Recuperación:** búsqueda tolerante (parcial/fuzzy); si 0 resultados, sugerir facetas o
  búsquedas recientes; nunca "no encontrado" a secas.
- **Automatización:** ranking por recientes/relevancia; recordar búsquedas frecuentes.
- **Reducción de clics:** búsqueda incremental (resultados sin pulsar "buscar"); Enter abre el
  primer resultado.
- **Aprendizaje contextual:** mostrar las facetas disponibles como pistas mientras se escribe.
- *Principios:* **P7**, P5, P11.

### W3 · Abrir un Lab
- **Objetivo:** entrar al entorno de un dominio (Animation, Rules…).
- **Inicio:** selector de Lab (Top Bar Z1) o Command Palette (Z4).
- **Pasos ideales:** (1) elegir Lab → (2) el marco se conserva (P9) y solo cambia el contenido
  del Anillo 1; se restaura el último estado *de ese Lab*.
- **Decisiones:** qué Lab.
- **Información por etapa:** al abrir, el Lab aparece con su último recurso/estado; zonas
  contextuales según su naturaleza (Timeline si es temporal).
- **Errores:** Lab que falla al cargar (plugin roto).
- **Recuperación:** degradación aislada — el Lab se marca no disponible en Z2, el resto del
  Studio sigue (I6).
- **Automatización:** precarga en segundo plano de los Labs usados recientemente.
- **Reducción de clics:** cambiar de Lab conserva la gramática → no hay que reorientarse (P9).
- **Aprendizaje contextual:** un Lab abierto por primera vez muestra su propósito en una línea,
  descartable.
- *Principios:* **P9**, P1, P12.

### W17 · Cambiar entre Labs
- **Objetivo:** alternar de dominio sin perder el hilo de ninguno.
- **Inicio:** con un Lab abierto, saltar a otro.
- **Pasos ideales:** (1) invocar cambio (palette o selector) → (2) el Lab destino aparece
  **en el estado exacto en que se dejó** (cada Lab mantiene su propia sesión).
- **Decisiones:** a qué Lab; volver ↔ ir.
- **Información por etapa:** transición conserva Anillo 0; solo muta Anillo 1–2.
- **Errores:** confundir en qué Lab estoy.
- **Recuperación:** Z1 siempre indica el Lab activo (orientación permanente).
- **Automatización:** "último Lab" como toggle rápido (ida/vuelta con un gesto).
- **Reducción de clics:** navegación tipo *recientes* (alternar entre los 2 últimos en 1 gesto).
- **Aprendizaje contextual:** —.
- *Principios:* **P9**, P1, P5.

### W18 · Abrir herramientas contextuales (Consola/Performance/Historial)
- **Objetivo:** invocar diagnóstico o memoria sin abandonar el trabajo.
- **Inicio:** en pleno trabajo, necesito ver logs/métricas/historial.
- **Pasos ideales:** (1) un gesto invoca el panel (drawer/lateral) → (2) consulto → (3) se
  retira y recupero el espacio del Preview.
- **Decisiones:** qué herramienta; dejarla anclada o que se cierre.
- **Información por etapa:** oculta por defecto (P4); su resumen vive siempre en Z2 (P4).
- **Errores:** dejar paneles abiertos que roban espacio al Preview.
- **Recuperación:** un gesto de "limpiar" recupera el layout de trabajo; autosave del layout no
  la fija salvo que se ancle explícitamente.
- **Automatización:** abrir Consola automáticamente **solo** ante un error (y ofrecer cerrarla).
- **Reducción de clics:** toggle único por herramienta (mismo gesto abre/cierra).
- **Aprendizaje contextual:** un error en Z2 insinúa "ver detalle" que abre la Consola.
- *Principios:* **P4**, P5, P2.

### W19 · Usar la Command Palette
- **Objetivo:** ejecutar cualquier acción o saltar a cualquier recurso por descripción.
- **Inicio:** atajo global desde cualquier estado.
- **Pasos ideales:** (1) invocar → (2) escribir intención → (3) elegir → (4) se ejecuta y el
  overlay desaparece.
- **Decisiones:** acción vs recurso; cuál.
- **Información por etapa:** lista efímera filtrada; muestra el atajo asociado a cada comando
  (aprendizaje, ver abajo).
- **Errores:** comando inexistente; acción no aplicable al contexto actual.
- **Recuperación:** acciones no aplicables aparecen atenuadas con el motivo; nunca fallo mudo.
- **Automatización:** priorizar comandos por contexto (Lab activo) y por uso reciente.
- **Reducción de clics:** es *la* herramienta anti-clics: cualquier acción en 1 atajo + texto,
  sin recorrer menús. Cero footprint permanente (P4).
- **Aprendizaje contextual:** **enseña sus propios atajos** — al mostrar un comando revela su
  tecla, de modo que el uso repetido migra al atajo directo (curva experto natural).
- *Principios:* **P5**, P4, P7, P14.

---

## Familia B — Creación y edición de animaciones (el bucle del 70%)

### W4 · Crear una nueva animación
- **Objetivo:** dar de alta un recurso de animación nuevo y empezar a iterarlo.
- **Inicio:** Animation Lab activo.
- **Pasos ideales:** (1) "nueva animación" (palette/acción) → (2) elegir base (plantilla/
  triad canónico o duplicar de una existente) → (3) aparece ya reproduciéndose en el Preview
  con parámetros por defecto → (4) empiezo a iterar (→ W5).
- **Decisiones:** partir de plantilla vs duplicar existente; nombre (aplazable).
- **Información por etapa:** al crear, Inspector (Z7) con el esquema de params por defecto;
  Preview (Z6) mostrando algo desde el primer segundo (nunca lienzo vacío).
- **Errores:** crear sin base coherente; nombre colisiona.
- **Recuperación:** todo es borrador reversible (P8); el nombre se resuelve al guardar, no al
  crear (no bloquea).
- **Automatización:** derivar el esquema de parámetros del propio tipo de animación (discovery);
  nombre provisional automático.
- **Reducción de clics:** "crear" produce **algo ejecutable de inmediato** (no un formulario);
  se nombra después.
- **Aprendizaje contextual:** ofrecer el *triad canónico* como plantilla explicada (qué es
  cada pieza) → enseña el patrón del proyecto (P14).
- *Principios:* **P2**, P5, P8, P14, **I1** (usa las recetas reales del juego).

### W5 · Editar una animación existente (BUCLE CENTRAL)
- **Objetivo:** afinar parámetros hasta que "se sienta bien". Es el 70% del trabajo.
- **Inicio:** una animación seleccionada, reproduciéndose.
- **Pasos ideales (bucle):** (1) observo el Preview → (2) identifico el parámetro → (3) lo
  ajusto en el Inspector → (4) el Preview cambia **en caliente, sin recargar** → (5) repito.
  Reproducir de nuevo = un gesto (o loop automático).
- **Decisiones:** qué parámetro tocar; cuándo "ya está"; guardar como variante o no.
- **Información por etapa:** Preview + Inspector **simultáneos** siempre (P3); Timeline (Z9)
  para inspeccionar frames concretos; valor actual vs por-defecto visible.
- **Errores:** valor fuera de rango; "me gustaba más hace 3 ajustes"; ajuste que rompe el
  render.
- **Recuperación:** rangos acotados en el esquema; **undo/redo** de parámetros (P8) para volver
  N pasos; loop permite comparar antes/después de memoria — mejor aún, W6.
- **Automatización:** loop de reproducción automático; recálculo en vivo (hot-reload, P11).
- **Reducción de clics:** editar y ver, **mismo lugar, mismo instante** (P3); sin "aplicar", sin
  "recargar", sin confirmar.
- **Aprendizaje contextual:** cada parámetro puede revelar su significado/rango (P14) al
  enfocarlo, sin abandonar el bucle.
- *Principios:* **P3, P2, P5, P11, P8** — este workflow ES la razón de ser del Studio.

### W6 · Comparar variantes
- **Objetivo:** decidir entre A y B mirándolas, no recordándolas (P6).
- **Inicio:** un recurso con ≥2 variantes (o actual vs. un ajuste).
- **Pasos ideales:** (1) activar "comparar" → (2) el Preview entra en modo dual (Z10 = modo del
  rey) → (3) A y B se reproducen sincronizadas → (4) elijo ganadora.
- **Decisiones:** qué dos comparar; sincronizar reproducción o no; cuál gana.
- **Información por etapa:** dos previews lado a lado con sus identidades y params clave; resto
  del chrome se atenúa (foco en la comparación).
- **Errores:** comparar más de dos (satura el juicio); confundir cuál es cuál.
- **Recuperación:** comparación **pareada** por diseño (máx. 2); etiquetas de identidad claras.
- **Automatización:** reproducción sincronizada; "swap A/B" para contraste directo.
- **Reducción de clics:** comparar es un **modo**, no montar dos ventanas: 1 gesto entra/sale.
- **Aprendizaje contextual:** si hay benchmark asociado, ofrecer "comparar contra referencia"
  desde aquí (puente a W9).
- *Principios:* **P6**, P2, P11.

### W7 · Crear una variante nueva
- **Objetivo:** materializar un ajuste como variante nombrada sin perder la original.
- **Inicio:** editando (W5) con overrides que valen la pena.
- **Pasos ideales:** (1) "guardar como variante" → (2) nombrar + estado (Experimental por
  defecto) → (3) la variante nace y queda seleccionable junto a las demás.
- **Decisiones:** nombre; estado (Experimental/…); ¿sustituye o convive?
- **Información por etapa:** al guardar, se muestran los params que difieren de la base (qué
  hace única a la variante).
- **Errores:** sobrescribir la original por error; nombre duplicado.
- **Recuperación:** "guardar como variante" **nunca** toca la original (crea, no sustituye);
  colisión de nombre se avisa antes de confirmar; y aun así, reversible (P8).
- **Automatización:** capturar automáticamente el *diff* de params como descripción de la
  variante.
- **Reducción de clics:** desde el bucle de edición a variante guardada en 1–2 pasos.
- **Aprendizaje contextual:** explicar el ciclo de estados (Experimental→Canonical) al asignarlo.
- *Principios:* **P8, P13, P6, P5**.

---

## Familia C — Benchmark (calidad relativa)

### W8 · Abrir un benchmark
- **Objetivo:** cargar la referencia de calidad de un recurso (estándar visual + docs de skills).
- **Inicio:** desde el recurso (su ficha enlaza el benchmark) o Benchmark Lab.
- **Pasos ideales:** (1) abrir benchmark del recurso → (2) se muestra la referencia y sus docs
  (animation-benchmark / implementation-research / revision-critica) en contexto.
- **Decisiones:** qué referencia (si hay varias).
- **Información por etapa:** referencia + fichas de las skills junto a ella (P14).
- **Errores:** recurso sin benchmark definido.
- **Recuperación:** si falta, indicar que aún no tiene estándar y ofrecer crearlo (enlaza el
  flujo de las skills).
- **Automatización:** vincular automáticamente benchmark↔recurso por metadatos (`DocRefs`).
- **Reducción de clics:** desde el recurso a su benchmark en 1 gesto (relación pre-cableada).
- **Aprendizaje contextual:** el benchmark **es** aprendizaje: muestra el porqué del estándar.
- *Principios:* **P14**, P6.

### W9 · Comparar benchmark vs implementación
- **Objetivo:** juzgar cuánto se acerca nuestra implementación a la referencia (P6, P15).
- **Inicio:** benchmark abierto (W8) con el recurso implementado.
- **Pasos ideales:** (1) "comparar contra referencia" → (2) Preview en modo dual: referencia |
  nuestra → (3) reproducción sincronizada → (4) valorar / iterar (vuelve a W5) / promover.
- **Decisiones:** ¿suficientemente cerca? ¿iterar o promover a Canonical?
- **Información por etapa:** ambos lado a lado + honestidad del preview (P15: si es headless,
  se advierte que no es pixel-perfect y por tanto el juicio de "juice" es provisional).
- **Errores:** juzgar juice sobre un preview esquemático y darlo por bueno.
- **Recuperación:** advertencia P15 persistente en modo headless; el veredicto de calidad
  visual final exige preview de dispositivo (A).
- **Automatización:** alinear tiempos de ambas reproducciones; marcar diferencias de timing.
- **Reducción de clics:** reutiliza el modo comparación (W6); no es un flujo nuevo, es W6 con la
  referencia como B.
- **Aprendizaje contextual:** la revisión crítica (skill) se muestra junto a la comparación
  como checklist.
- *Principios:* **P6, P15, P14**.

---

## Familia D — Presets, snapshots y exportación (preservación, los 30 años)

### W10 · Guardar un preset
- **Objetivo:** conservar una combinación de parámetros reutilizable.
- **Inicio:** configuración actual que quiero reutilizar.
- **Pasos ideales:** (1) "guardar preset" → (2) nombrar → (3) queda disponible para aplicar.
- **Decisiones:** nombre; ámbito (del recurso / compartible).
- **Información por etapa:** qué params captura el preset.
- **Errores:** confundir preset con variante; nombre duplicado.
- **Recuperación:** distinción clara preset (config aplicable) vs variante (implementación);
  reversible.
- **Automatización:** capturar el set de params activo automáticamente.
- **Reducción de clics:** 1–2 pasos; nombrar aplazable con nombre provisional.
- **Aprendizaje contextual:** explicar preset vs variante la primera vez.
- *Principios:* **P13, P5, P8**.

### W11 · Restaurar un preset
- **Objetivo:** aplicar una configuración guardada al recurso actual.
- **Inicio:** recurso abierto + presets disponibles.
- **Pasos ideales:** (1) elegir preset → (2) el Preview refleja el cambio en caliente → (3)
  seguir o revertir.
- **Decisiones:** qué preset; conservar overrides previos o reemplazar.
- **Información por etapa:** vista previa del efecto antes de fijar (idealmente al enfocar el
  preset, ya se ve).
- **Errores:** aplicar y perder ajustes no guardados.
- **Recuperación:** undo devuelve el estado anterior (P8); avisar si hay overrides sin guardar.
- **Automatización:** previsualización al enfocar (sin confirmar).
- **Reducción de clics:** aplicar = 1 gesto; efecto inmediato (P11).
- **Aprendizaje contextual:** —.
- *Principios:* **P8, P11, P13**.

### W15 · Restaurar snapshots
- **Objetivo:** volver a un estado histórico completo y reproducirlo bit a bit (P13).
- **Inicio:** Historial (Z12) del recurso.
- **Pasos ideales:** (1) abrir historial → (2) elegir snapshot (con su entorno registrado) →
  (3) cargar → (4) reproducir idéntico a como fue capturado.
- **Decisiones:** qué snapshot; cargar como sesión temporal vs restaurar definitivamente.
- **Información por etapa:** por snapshot: fecha, versión, entorno (BOM, minSdk, modo preview),
  params; advertencia si el entorno actual difiere del capturado.
- **Errores:** el entorno de hoy no reproduce fielmente el de hace años; assets referenciados
  perdidos.
- **Recuperación:** honestidad (P15): avisar de divergencias de entorno; snapshots referencian
  assets por hash → detectar faltantes en vez de fallar silenciosamente.
- **Automatización:** registrar entorno completo en cada captura para reproducibilidad.
- **Reducción de clics:** "cargar snapshot" restaura contexto como W1 (mismo mecanismo de
  continuidad).
- **Aprendizaje contextual:** mostrar qué cambió entre el snapshot y el presente.
- *Principios:* **P13, P1, P15, P8**.

### W16 · Exportar configuraciones
- **Objetivo:** sacar datos (JSON/preset/comparativa) para uso externo o versionado.
- **Inicio:** recurso/variante/snapshot seleccionado.
- **Pasos ideales:** (1) "exportar" → (2) elegir qué y formato → (3) obtener el artefacto.
- **Decisiones:** qué exportar; formato; destino.
- **Información por etapa:** previsualización de lo que se exportará.
- **Errores:** exportar algo incompleto o dependiente de recursos no incluidos.
- **Recuperación:** validar dependencias antes de exportar; advertir de referencias externas.
- **Automatización:** incluir metadatos y dependencias automáticamente.
- **Reducción de clics:** exportación con defaults sensatos (1 gesto para el caso común).
- **Aprendizaje contextual:** explicar qué contiene el JSON (formato durable, legible).
- *Principios:* **P13, P5**.

---

## Familia E — Rules Lab (escenarios)

### W12 · Crear un escenario en Rules Lab
- **Objetivo:** construir un `GameState` arbitrario sin jugar una partida (I1).
- **Inicio:** Rules Lab activo.
- **Pasos ideales:** (1) partir de vacío o de una plantilla → (2) manipular el estado en el
  Área de Trabajo (Z5): Activos, Bancas, Energías, contadores, premios, mano, mazo → (3) el
  estado resultante es un `GameState` válido del engine real → (4) con un gesto, lanzar una
  animación o probar una regla sobre él.
- **Decisiones:** qué situación construir; partir de plantilla vs cero.
- **Información por etapa:** el tablero editable + propiedades del elemento seleccionado en el
  Inspector; validez del estado según el motor (qué es legal).
- **Errores:** construir un estado ilegal/imposible para el motor.
- **Recuperación:** el propio engine valida; señalar por qué es ilegal y ofrecer corrección; el
  Studio no inventa reglas propias (I1).
- **Automatización:** plantillas de escenarios comunes; auto-relleno coherente (mazo legal).
- **Reducción de clics:** manipulación directa (arrastrar/colocar) en vez de formularios.
- **Aprendizaje contextual:** al construir, explicar restricciones del motor en contexto.
- *Principios:* **I1, P2, P8, P13**.

### W13 · Portar / reabrir un escenario (portabilidad, NO colaboración)
> **Proyecto unipersonal:** no existe "otro desarrollador". Portar = mover un escenario entre
> *tus propios entornos* (otra máquina, una reinstalación, tu yo futuro) o entregárselo a una
> **IA de asistencia** como contexto de un problema. Nunca es un flujo colaborativo.
- **Objetivo:** reproducir bit a bit una situación de juego en otro entorno tuyo (o dársela a
  una IA para depurar), sin volver a construirla a mano.
- **Inicio:** escenario construido (W12).
- **Pasos ideales:** (1) "exportar escenario" → (2) obtengo un artefacto portable (el
  `GameState` serializado + entorno) → (3) al reimportarlo (aquí o en otra máquina) veo lo mismo.
- **Decisiones:** exportar estado solo, o con la animación/prueba asociada.
- **Información por etapa:** qué incluye el paquete portado.
- **Errores:** el entorno destino tiene datos de carta distintos; versión de motor distinta.
- **Recuperación:** incluir versión de datos/motor; avisar de incompatibilidades al importar
  (P15).
- **Automatización:** empaquetar dependencias (cartas referenciadas) por identidad estable.
- **Reducción de clics:** reutiliza W16 (exportar) especializado a escenario.
- **Aprendizaje contextual:** —.
- *Principios:* **P13, P15, I1**.

---

## Familia F — Sesiones y personalización

### W14 · Abrir una sesión anterior
- **Objetivo:** recuperar un contexto de trabajo pasado concreto (no solo el último).
- **Inicio:** listado de sesiones/estados guardados.
- **Pasos ideales:** (1) abrir sesiones → (2) elegir una → (3) el Studio restaura su contexto
  completo (Lab, recurso, layout, overrides).
- **Decisiones:** qué sesión; abrir en paralelo (otra ventana) o reemplazar la actual.
- **Información por etapa:** por sesión: cuándo, qué Lab/recurso, resumen del estado.
- **Errores:** sesión que referencia recursos ya inexistentes.
- **Recuperación:** degradación como W1; informar de lo que falta sin bloquear.
- **Automatización:** autosave periódico de sesiones nombradas por contexto.
- **Reducción de clics:** mismas mecánicas de continuidad que W1 (coherencia).
- **Aprendizaje contextual:** —.
- *Principios:* **P1, P13**.

### W20 · Personalizar el workspace
- **Objetivo:** acomodar el espacio a mi forma de trabajar sin romper la gramática (P12/P9).
- **Inicio:** en cualquier Lab.
- **Pasos ideales:** (1) colapsar/expandir/redimensionar flancos, anclar o cerrar contextuales
  → (2) el layout se recuerda (por Lab y global).
- **Decisiones:** qué mostrar/ocultar; tamaños; presets de workspace.
- **Información por etapa:** el propio layout responde en vivo.
- **Errores:** dejar un layout inutilizable (todo cerrado, Preview minúsculo).
- **Recuperación:** "restablecer layout" a los defaults sensatos (siempre a un gesto); nunca se
  puede llegar a un estado irrecuperable.
- **Automatización:** memoria de layout por Lab y por monitor (P1).
- **Reducción de clics:** presets de workspace para saltar entre disposiciones (p. ej. "foco
  preview" vs "curaduría").
- **Aprendizaje contextual:** —. La gramática invariante (Anillo 0 + posiciones relativas) no
  es personalizable → nadie se pierde (P9).
- *Principios:* **P12, P9, P1, P10**.

### W21 · Trabajar con múltiples monitores
- **Objetivo:** aprovechar más pantalla desacoplando zonas.
- **Inicio:** entorno multi-monitor.
- **Pasos ideales:** (1) desacoplar una zona (Preview / Inspector / Biblioteca / diagnóstico)
  → (2) llevarla a otro monitor → (3) el marco central sigue coherente sin ella → (4) el
  arreglo se recuerda.
- **Decisiones:** qué desacoplar; a qué monitor.
- **Información por etapa:** cada superficie mantiene su identidad; el Studio sabe qué está
  fuera.
- **Errores:** perder una zona desacoplada; monitor desconectado.
- **Recuperación:** reacoplar todo a un gesto; si un monitor desaparece, sus zonas vuelven al
  marco principal (no se pierden).
- **Automatización:** recordar la disposición multi-monitor por entorno (P1).
- **Reducción de clics:** desacoplar/reacoplar como gesto directo, no configuración.
- **Aprendizaje contextual:** —.
- *Principios:* **P12, P2 (Preview a pantalla completa), P1**.

---

## Parte II — Workflows más frecuentes y optimizaciones específicas

Ranking por frecuencia real (del análisis de modos: Iteración 70 / Exploración 20 / Autoría 10):

| Rango | Workflow | Modo | Objetivo de fricción |
|-------|----------|------|----------------------|
| 1 | **W5 Editar animación (bucle)** | Iteración | 0 cambios de contexto, latencia ≈0 |
| 2 | **W1 Abrir=continuar** | Arranque | 0 pasos |
| 3 | **W6/W9 Comparar (variantes/benchmark)** | Iteración/Explor. | 1 gesto entra/sale |
| 4 | **W2 Buscar recurso** | Exploración | resultado en vivo, Enter abre |
| 5 | **W17 Cambiar entre Labs** | Navegación | estado preservado, 1 gesto toggle |
| 6 | **W19 Command Palette** | Transversal | cualquier acción en 1 atajo+texto |
| 7 | **W7 Crear variante** | Autoría | 1–2 pasos desde el bucle |

**Optimizaciones específicas (por qué y contra qué principio):**

- **O1 · Loop + hot-reload como estado por defecto del bucle (W5).** La animación se
  reproduce en bucle y todo cambio de parámetro se aplica en caliente. Elimina el gesto
  "reproducir" repetido miles de veces. *→ P3, P11, P5.*
- **O2 · Comparación como modo del Preview, no como montaje manual (W6/W9).** Un solo gesto
  divide/reúne el rey; nunca abrir dos ventanas a mano. *→ P6, P2.*
- **O3 · Continuidad total de contexto (W1/W14/W17).** El mismo mecanismo de restauración
  sirve para abrir el Studio, abrir una sesión y cambiar de Lab: **una sola idea**, aplicada
  en tres sitios → coherencia y cero reaprendizaje. *→ P1, P9.*
- **O4 · Búsqueda incremental universal (W2) y Palette (W19) comparten motor.** Buscar recursos
  y buscar acciones son el mismo gesto mental (describir lo que quiero). Unificarlos reduce la
  carga cognitiva. *→ P7, P5.*
- **O5 · "Crear = algo ejecutable ya" (W4).** Nunca un formulario vacío: crear produce un
  artefacto reproduciéndose, y el nombrado se aplaza al guardar. Convierte "dar de alta" en
  "empezar a iterar". *→ P2, P5, P8.*
- **O6 · Reversibilidad sobre confirmación en todo el bucle (W5/W7/W11).** undo/redo de
  parámetros y "guardar como variante nunca toca la original" → se actúa rápido sin miedo, sin
  diálogos de "¿seguro?". *→ P8, P5.*
- **O7 · Diagnóstico invocable con auto-apertura solo ante error (W18).** La Consola no ocupa
  espacio salvo que haga falta; un error la ofrece desde la Status Bar. *→ P4, P2.*
- **O8 · Honestidad del preview siempre presente en comparación/snapshot (W9/W15).** En modo
  headless, la advertencia P15 evita falsos veredictos de "juice". *→ P15.*

---

## Parte III — Catálogo de Workflows Canónicos (referencia obligatoria)

Toda pantalla futura deberá poder trazarse a uno o varios de estos flujos y respetar sus
objetivos de fricción. Si una pantalla no sirve a ningún workflow canónico, sobra.

| ID | Workflow | Familia | Frecuencia | Fricción objetivo |
|----|----------|---------|-----------|-------------------|
| W1 | Abrir = continuar | Arranque | Diaria | 0 pasos (auto) |
| W2 | Buscar recurso | Navegación | Alta | En vivo; Enter abre |
| W3 | Abrir un Lab | Navegación | Media | 1 gesto, estado restaurado |
| W4 | Crear animación | Creación | Media | Ejecutable de inmediato |
| W5 | Editar animación (bucle) | Iteración | **Máxima** | 0 context-switch, latencia ≈0 |
| W6 | Comparar variantes | Iteración | Alta | 1 gesto (modo) |
| W7 | Crear variante | Autoría | Media | 1–2 pasos, no destructivo |
| W8 | Abrir benchmark | Benchmark | Media | 1 gesto desde recurso |
| W9 | Benchmark vs implementación | Benchmark | Media | Reusa W6 + honestidad P15 |
| W10 | Guardar preset | Preservación | Media | 1–2 pasos |
| W11 | Restaurar preset | Preservación | Media | 1 gesto, en caliente |
| W12 | Crear escenario (Rules) | Rules | Alta | Manipulación directa |
| W13 | Portar/reabrir escenario | Rules | Baja | Reusa W16 (portabilidad, no colaboración) |
| W14 | Abrir sesión anterior | Sesiones | Baja | Reusa continuidad W1 |
| W15 | Restaurar snapshots | Preservación | Baja/crítica | Reproducción fiel + entorno |
| W16 | Exportar configuraciones | Preservación | Baja | Defaults, 1 gesto común |
| W17 | Cambiar entre Labs | Navegación | Alta | Toggle 1 gesto, estado vivo |
| W18 | Herramientas contextuales | Diagnóstico | Media | Toggle único, oculto por def. |
| W19 | Command Palette | Transversal | Alta | 1 atajo + texto |
| W20 | Personalizar workspace | Personalización | Baja | Reversible, presets |
| W21 | Multi-monitor | Personalización | Baja | Desacople directo, recordado |

**Invariantes de todos los workflows:**
1. **Ninguno saca al usuario del contexto de trabajo** salvo que él lo pida (P3).
2. **Ninguno pierde trabajo**; todo es reversible o preservable (P8, P13).
3. **Ninguno depende de que el usuario recuerde estado**; el Studio lo restaura (P1).
4. **Ninguno introduce lógica del juego en el Studio**; siempre usa engine/framework reales
   (I1 — regla de oro).
5. **La frecuencia manda:** el coste de cada workflow es inverso a cuántas veces se hace (P5).

---

**Límite de esta fase:** aquí termina el diseño de *cómo se trabaja*. NO se ha decidido nada
visual (layout dibujado, tamaños, colores, componentes). El siguiente nivel (Layout
conceptual) traducirá estos workflows en disposición concreta, y cada pantalla se validará
contra este catálogo, contra `SPATIAL-ARCHITECTURE.md` y contra `UX-PRINCIPLES.md`.
