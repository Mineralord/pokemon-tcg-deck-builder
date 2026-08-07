# PTCG Developer Studio Design System (PDS) — Arquitectura de Componentes (Fase 6)

**El sistema de componentes con el que se construirán TODAS las pantallas futuras.** No hay
diseño visual: sin colores, sombras, animaciones, iconografía ni tipografía. Solo **arquitectura
de componentes**: qué bloques existen, qué hacen y cómo se combinan.

**Renombrado oficial:** de aquí en adelante, "el Design System" es el **PTCG Developer Studio
Design System (PDS)** — el lenguaje de interfaz de **todo el ecosistema** Pokémon TCG Clone
(Studio, launcher del juego, visor de cartas, herramientas internas, editores futuros).

**Análisis integrado por dos lentes** (activas esta sesión): `ui-ux-pro-max` (jerarquía atómica,
accesibilidad, consistencia, escalabilidad) + `emil-design-eng` (comportamiento y feel de cada
componente, interruptibilidad, identidad). Conflicto resuelto: donde Emil pide identidad/feel y
UI/UX pide sobriedad/consistencia, **el PDS separa capas** — la *estructura* del componente
(esta fase) es sobria y única; el *feel* (Fase 7) se aplica encima como tokens, sin duplicar
componentes.

**Canon obligatorio:** 4 pilares · P1–P15 · Arq. Espacial (Z1–Z12) · Workflows · Layout
(DC-D1/DC-D2) · Modelo Mental · **Interaction Canon (IC-0…IC-13)**.

---

## Parte I — Alcance del PDS y su relación con la Especialización Absoluta

- **Reutilización INTERNA, no genérica.** El PDS se comparte entre las herramientas *del propio
  ecosistema PTCG*. Es el mismo espíritu que "plugins = modularidad interna" (pilar 2): reusar
  dentro del proyecto **acelera y unifica**; no convierte al PDS en una librería de propósito
  general publicada para terceros.
- **Frontera canónica:** el PDS **no** persigue paridad con librerías UI genéricas ni añade
  componentes "porque son estándar en la industria". Un componente entra solo si sirve a una
  pantalla real del ecosistema PTCG (prueba de pertenencia, pilar 2).
- **Neutralidad de dominio en la base, especialización en el borde:** los componentes base
  (Panel, Tree, Dialog…) son neutrales; los componentes que *encarnan* conceptos del canon
  (Resource Card, Property Grid, Timeline, Preview) conocen el Modelo Mental (Resource/Variant/
  Preset/Snapshot). Esa es la línea entre "reutilizable en el launcher" y "propio del Studio".

**Capa de tokens (reservada, NO se llena aquí):** el PDS tendrá una capa de *design tokens*
(color, espaciado, tipografía, motion, elevación) que la Fase 7 rellenará. En Fase 6 solo se
**reserva su existencia** y se prohíbe que ningún componente hornee valores visuales: todo
atributo visual futuro vendrá por token, nunca incrustado. Esto garantiza que un mismo componente
se re-tematice para Studio vs launcher sin duplicarse.

---

## Parte II — Jerarquía del Design System (Atomic Design adaptado)

Cinco niveles. Regla de dependencia: **un nivel solo compone niveles inferiores**; nunca al revés.

- **ÁTOMOS** — piezas indivisibles sin estado de dominio: Divider · Badge · Chip · Progress ·
  Focus Indicator · Selection Highlight · Shortcut Hint · Scroll Container.
- **MOLÉCULAS** — combinaciones pequeñas con un cometido: Search · Tabs · Breadcrumb ·
  Resource Card · Tree Node · Property Row · Toast · Notification · Empty State · Error State ·
  Loading State · Menu Item Group · Status Segment.
- **ORGANISMOS** — regiones funcionales completas: Panel · Dock · Toolbar · Sidebar · Inspector ·
  Property Grid · Timeline · Preview · Overlay · Dialog · Context Menu · Command Palette · Tree ·
  Status Bar.
- **PLANTILLAS (Templates)** — el **Shell** (marco + anillos del Layout Conceptual) y las
  disposiciones de zona; una plantilla coloca organismos en las zonas Z1–Z12 sin contenido de
  dominio.
- **PATRONES (Patterns)** — comportamientos transversales realizados combinando lo anterior:
  Selección/Focus · Edición-en-vivo (hot-reload) · Undo/Redo · Invocación (Palette/Overlay) ·
  Revelación progresiva (hover) · Modo Comparación · Preservación (Preset/Snapshot) · Feedback
  (progreso/error).

**Window** es la frontera de plantilla: la *superficie de nivel superior* (ventana/pestaña del
navegador y cada superficie desacoplada en multi-monitor). Hospeda exactamente un Shell.

---

## Parte III — Catálogo de componentes

Formato por componente (10 aspectos): Propósito · Responsabilidades · Comportamiento · Relación
con Interaction Canon (IC) · Variantes permitidas · Variantes prohibidas · Composición · Reuso ·
Evolución · Cuándo NO usar. Para evitar repetición, las **reglas de familia** se enuncian una vez.

**Reglas de familia (aplican a todos salvo excepción explícita):**
- **F-COMP (composición):** un componente solo compone niveles inferiores; jamás conoce al Lab.
- **F-VIS (visual):** ningún componente hornea valores visuales (todo por token, Fase 7).
- **F-A11Y:** todo componente interactivo es operable por teclado y expone su foco (IC-5).
- **F-EVO (evolución):** se evoluciona por *extensión* (props/variantes nuevas opcionales),
  nunca redefiniendo el comportamiento existente; un cambio incompatible = versión nueva del PDS.
- **F-REUSE:** si dos pantallas necesitan lo mismo, se extrae al PDS; nunca se copia.

### A · ÁTOMOS

**A1 · Divider** — Propósito: separar grupos sin jerarquía. Responsabilidades: delimitar
visualmente. Comportamiento: inerte. IC: —. Permitidas: horizontal/vertical, con/sin etiqueta.
Prohibidas: divider clicable/interactivo (usar otra cosa). Composición: dentro de Panel/Menu/
Property Grid. Reuso: universal. Evolución: F-EVO. NO usar: para crear jerarquía (eso es Panel).

**A2 · Badge** — Propósito: contador o marcador de estado sobre un objeto. Responsabilidades:
comunicar cantidad/estado (p. ej. status de un Resource). Comportamiento: reactivo, no
interactivo. IC: refleja estado (IC-8/IC-11). Permitidas: numérico, de estado, de punto.
Prohibidas: badge como botón. Composición: sobre Resource Card, Tab, ítem de Tree. Reuso:
universal. NO usar: para acciones (usar Chip/menú).

**A3 · Chip** — Propósito: token compacto seleccionable/filtrable (una faceta, un tag).
Responsabilidades: representar un valor discreto; opcionalmente togglear. Comportamiento:
seleccionable (IC-1), a veces removible. IC: IC-10 (filtros), IC-1 (selección). Permitidas:
filtro (toggle), input (removible), lectura. Prohibidas: chip que ejecuta acción destructiva.
Composición: en Search, Inspector (enums multi), Toolbar. NO usar: como botón primario.

**A4 · Progress** — Propósito: comunicar avance de una operación. Responsabilidades: mostrar
progreso determinado o indeterminado. Comportamiento: no bloqueante (IC-8). IC: IC-8. Permitidas:
lineal, circular, indeterminado. Prohibidas: progreso que **bloquea** el bucle. Composición: en
Toast, Status Bar, Loading State. NO usar: para operaciones <100 ms (no mostrar nada, IC-8).

**A5 · Focus Indicator** — Propósito: señalar el elemento con foco de teclado. Responsabilidades:
visibilidad del foco (a11y). Comportamiento: sigue al foco (IC-1/IC-5). IC: IC-1, IC-5.
Permitidas: una sola forma canónica de indicar foco en todo el PDS. Prohibidas: focos distintos
por componente (rompería consistencia). Composición: adjunto a cualquier interactivo. NO usar:
nunca omitir en interactivos (regla dura de a11y).

**A6 · Selection Highlight** — Propósito: marcar objeto(s) seleccionado(s). Responsabilidades:
distinguir selección de foco. Comportamiento: refleja el modelo de selección (IC-1). IC: IC-1.
Permitidas: single, múltiple, rango. Prohibidas: confundir selección con hover (IC-4) o con foco
(A5) usando el mismo lenguaje. Composición: en Tree, Preview, listas. NO usar: para hover.

**A7 · Shortcut Hint** — Propósito: mostrar el atajo de una acción (enseñanza en contexto).
Responsabilidades: revelar la tecla. Comportamiento: informativo; aparece junto a comandos.
IC: IC-5 (la Palette y menús enseñan atajos), P14. Permitidas: inline junto a acción, en tooltip.
Prohibidas: hint sin acción real detrás. Composición: en Command Palette, Context Menu, Tooltip.
NO usar: para acciones sin atajo.

**A8 · Scroll Container** — Propósito: contener contenido desbordante. Responsabilidades: scroll
predecible, preservar posición (P1). Comportamiento: scroll estándar; recuerda posición por
sesión. IC: —. Permitidas: vertical, horizontal, ambos, virtualizado (para miles de ítems, P7).
Prohibidas: scroll anidado ambiguo (dos barras compitiendo). Composición: envuelve Tree, listas,
Inspector. NO usar: cuando el contenido cabe (evita scroll innecesario).

### B · MOLÉCULAS

**B1 · Search** — Propósito: encontrar por descripción (P7). Responsabilidades: input incremental
+ facetas (Chips) por conceptos del canon. Comportamiento: resultados en vivo; Enter abre el
primero; **mismo motor que la Command Palette** (IC-10/O4). IC: IC-10. Permitidas: global (en
Biblioteca), local (dentro de un selector). Prohibidas: búsqueda por ruta de archivos (pilar 2).
Composición: input + Chips + lista de resultados (Resource Cards / Tree). Reuso: en cualquier
selector del ecosistema. NO usar: para <10 ítems triviales (una lista basta).

**B2 · Tabs** — Propósito: alternar vistas hermanas dentro de una misma región. Responsabilidades:
conmutar contenido sin cambiar de zona. Comportamiento: cambio instantáneo, sin animación si es
frecuente (Emil). IC: IC-12 (si conmuta Labs, usar el conmutador de Lab, no Tabs). Permitidas:
tabs de contenido dentro de un Panel. Prohibidas: **Tabs para navegar entre Labs** (eso es
navegación de primer nivel, IC-12), ni tabs anidadas profundas. Composición: dentro de Panel.
NO usar: cuando las vistas no son hermanas (usar navegación).

**B3 · Breadcrumb** — Propósito: mostrar y navegar la ruta de contexto (Lab › Resource ›
Variant). Responsabilidades: orientación + salto atrás. Comportamiento: cada segmento navega
(IC deep-link). IC: refleja el focus (IC-1) y el contexto (Top Bar Z1). Permitidas: ruta
conceptual (no de archivos). Prohibidas: breadcrumb de sistema de ficheros (pilar 2).
Composición: en Top Bar. NO usar: para jerarquías profundas cambiantes (usar Tree).

**B4 · Resource Card** — Propósito: representar un **Resource** (canon) como unidad navegable.
Responsabilidades: mostrar su ficha mínima (nombre, kind, status, favorito) y ser
seleccionable/abrible. Comportamiento: clic selecciona (IC-1), doble clic abre (IC-4), clic
derecho = acciones (IC-4), arrastrable (IC-3), favoritable (IC-9). IC: IC-1/3/4/9/10. Permitidas:
compacta (lista), extendida (rejilla). Prohibidas: Resource Card que edita en línea (eso es el
Inspector). Composición: en resultados de Search, Biblioteca. Reuso: visor de cartas y launcher
pueden reusar la *estructura* Card. Evolución: nuevos ResourceKind reutilizan la misma Card.
NO usar: para no-Resources.

**B5 · Tree Node** — Propósito: nodo de una jerarquía navegable (zonas, capas, escenario).
Responsabilidades: expandir/colapsar, seleccionar, revelar acciones. Comportamiento: IC-1
(selección), IC-4 (doble clic entra, derecho acciones), teclado (flechas). IC: IC-1/4/5.
Permitidas: con/sin badges, con/sin arrastre (IC-3). Prohibidas: nodo con lógica de dominio
propia. Composición: dentro de Tree (organismo). NO usar: para listas planas (usar lista/Cards).

**B6 · Property Row** — Propósito: una fila editable de parámetro (clave→editor por tipo).
Responsabilidades: mostrar y editar UN parámetro según su `ParamField` (Float/Int/Bool/Enum/
Curve/Duration). Comportamiento: edición en vivo (IC-2, hot-reload P11), reversible (IC-6), doble
clic = editar (IC-4). IC: IC-1/2/6. Permitidas: un editor por tipo de parámetro. Prohibidas:
editor de texto libre genérico (pilar 2); fila que dispara acción destructiva. Composición: dentro
de Property Grid. Reuso: cualquier herramienta con parámetros. NO usar: para acciones (usar
Toolbar/menú).

**B7 · Toast** — Propósito: aviso efímero no bloqueante de un evento. Responsabilidades: informar
y desaparecer; ofrecer acción de recuperación/undo. Comportamiento: no bloqueante (IC-8),
apilable, interrumpible (Emil: transitions, pausa al perder foco). IC: IC-8, IC-6 (undo).
Permitidas: info, éxito, advertencia, error-leve. Prohibidas: toast para errores que **impiden**
seguir (usar Error State/Dialog); toast con formulario. Composición: capa Overlay. NO usar: para
información persistente (usar Status Bar/Notification).

**B8 · Notification** — Propósito: aviso persistente consultable (a diferencia del Toast).
Responsabilidades: registrar eventos relevantes recuperables. Comportamiento: se acumula, se
revisa bajo demanda (P4). IC: IC-8. Permitidas: lista de notificaciones. Prohibidas: notificación
modal. Composición: invocable desde Status Bar. NO usar: para lo efímero (usar Toast).

**B9 · Empty State** — Propósito: comunicar "aquí aún no hay nada" y orientar. Responsabilidades:
explicar y sugerir la primera acción (P14). Comportamiento: informativo; puede ofrecer "crear"
(W4). IC: —. Permitidas: neutro, con acción sugerida. Prohibidas: empty state que culpa/alarma.
Composición: dentro de Panel/Preview/Tree vacíos. NO usar: cuando hay contenido.

**B10 · Error State** — Propósito: comunicar que algo falló sin expulsar al usuario.
Responsabilidades: explicar cerca de la causa, ofrecer recuperación (IC-8). Comportamiento: no
bloqueante salvo que impida seguir; enlaza a Consola. IC: IC-8. Permitidas: inline (en la zona
afectada), a nivel de operación. Prohibidas: error modal para fallos recuperables; jerga cruda.
Composición: dentro de la zona afectada. NO usar: para advertencias leves (usar Toast).

**B11 · Loading State** — Propósito: indicar que el contenido llega. Responsabilidades: reservar
el espacio (evitar saltos, P11) y no bloquear. Comportamiento: no bloqueante; muestra Progress.
IC: IC-8. Permitidas: placeholder de estructura, indicador. Prohibidas: loading que congela el
bucle. Composición: dentro de la zona que carga. NO usar: para <100 ms (no mostrar nada).

**B12 · Menu Item Group** — Propósito: agrupar acciones en un Context Menu/Command Palette.
Responsabilidades: listar acciones aplicables con su Shortcut Hint. Comportamiento: navegable por
teclado (IC-5), orden consistente (IC-4). IC: IC-4/5. Permitidas: con separadores, submenús
someros. Prohibidas: submenús profundos; acciones no aplicables activas (atenuar con motivo).
Composición: en Context Menu / Command Palette. NO usar: como navegación primaria.

**B13 · Status Segment** — Propósito: un dato de glance en la Status Bar. Responsabilidades:
mostrar señal de fondo (conexión, estado de Preview, honestidad P15, métrica ligera).
Comportamiento: informativo; algunos invocan su detalle (Consola/Performance). IC: IC-8.
Permitidas: texto/indicador compacto. Prohibidas: segmento con acción destructiva o formulario.
Composición: en Status Bar (Z2). NO usar: para acciones primarias.

### C · ORGANISMOS

**C1 · Panel** — Propósito: contenedor rectangular con un cometido, unidad de composición del
Shell. Responsabilidades: encabezar, contener, colapsar/redimensionar. Comportamiento: colapsable/
redimensionable según Layout; recuerda estado (P1). IC: —. Permitidas: fijo, colapsable,
desacoplable. Prohibidas: Panel que se auto-reordena por tamaño (rompería P9/DC-D1). Composición:
hospeda moléculas/organismos; se coloca en zonas. Reuso: base de casi todo. NO usar: para overlays
efímeros (usar Overlay).

**C2 · Dock** — Propósito: alojar y organizar Panels acoplables en los bordes del Shell.
Responsabilidades: acoplar/desacoplar/reordenar Panels; memoria de layout (P1, W20). Comportamiento:
arrastrar para acoplar (IC-3); desacople multi-monitor (IC-13). IC: IC-3/13. Permitidas: docking a
bordes definidos por el Layout. Prohibidas: docking libre que rompa la gramática espacial (las
posiciones relativas son fijas, P9). Composición: contiene Panels. NO usar: dentro de un Panel.

**C3 · Toolbar** — Propósito: agrupar acciones/controles de una región. Responsabilidades: exponer
acciones frecuentes con paridad de teclado (IC-5). Comportamiento: acciones inmediatas (IC-8).
IC: IC-5/8. Permitidas: de Panel, de Preview (controles de reproducción), contextual. Prohibidas:
toolbar como navegación de Labs (IC-12); sobrecarga de acciones raras (esas van a la Palette, P5).
Composición: dentro de Panel/Preview. NO usar: para lo poco frecuente (Palette).

**C4 · Sidebar** — Propósito: Panel lateral estructural (Biblioteca / Inspector). Responsabilidades:
navegación (izq) o edición (der) según Layout. Comportamiento: colapsable a overlay en Compacto
(DC-D2). IC: IC-1/10. Permitidas: navegación, inspección. Prohibidas: sidebar que cambia de lado
por Lab (P9). Composición: Panel + Search/Tree o Property Grid. NO usar: para contenido central.

**C5 · Inspector** — Propósito: ver/editar los parámetros del **focus** (IC-1). Responsabilidades:
reflejar SIEMPRE el objeto activo; alojar el Property Grid; mostrar variantes/historial/docs bajo
demanda (P14). Comportamiento: edición en vivo (IC-2, P11); multi-select → params comunes (IC-1).
IC: IC-1/2/6/11. Permitidas: una instancia (única, canon). Prohibidas: múltiples Inspectores;
Inspector que no siga al focus. Composición: Sidebar der + Property Grid. Reuso: cualquier
herramienta con objetos parametrizables. NO usar: para navegación.

**C6 · Property Grid** — Propósito: rejilla de Property Rows derivada de un `ParamSchema`.
Responsabilidades: generar editores por tipo automáticamente; agrupar parámetros. Comportamiento:
edición en vivo/reversible (IC-2/6). IC: IC-1/2/6. Permitidas: agrupada, plana, con búsqueda de
parámetros. Prohibidas: campos hardcodeados por Lab (deben venir del esquema → escala). Composición:
Property Rows dentro de Inspector. Reuso: núcleo reutilizable del ecosistema. NO usar: para datos
no parametrizables.

**C7 · Timeline** — Propósito: control temporal del Preview (Labs temporales). Responsabilidades:
play/pause/step/loop/scrub/velocidad; conducir el tiempo del Preview (IC-13). Comportamiento: play
= modo; scrub sin animación (Emil, frecuente). IC: IC-13. Permitidas: presente solo en Labs
temporales. Prohibidas: Timeline en Labs sin tiempo (desaparece, P4/DC). Composición: bajo el
Preview (Z9). Reuso: cualquier artefacto temporal del ecosistema. NO usar: para lo no temporal.

**C8 · Preview** — Propósito: manifestación viva y honesta de un Resource (canon Preview).
Responsabilidades: renderizar el resultado; reproducir; entrar en **modo comparación** (DC-D1).
Comportamiento: selección → focus (IC-13); honestidad headless (P15). IC: IC-13. Permitidas: single,
comparación dual (manual, DC-D1). Prohibidas: ocultarse; auto-split por tamaño (DC-D1). Composición:
centro del Shell (Z6). Reuso: el visor de cartas es un Preview especializado. NO usar: como
contenedor genérico.

**C9 · Overlay** — Propósito: capa efímera por encima del Shell (sin robar espacio permanente).
Responsabilidades: alojar Command Palette, Dialogs, Toasts, menús. Comportamiento: aparece/retira;
descartable (Esc). IC: IC-4/5/7/8. Permitidas: modal (Dialog) y no-modal (Palette/Toast/Menu).
Prohibidas: overlay que persiste como panel (usar Panel). Composición: capa superior del Window.
NO usar: para contenido permanente.

**C10 · Dialog** — Propósito: interacción puntual que requiere atención acotada. Responsabilidades:
confirmar lo irreversible (IC-7) o pedir un dato mínimo. Comportamiento: modal centrado (Emil:
origin center para modales); descartable; **raro por diseño** (IC-7). IC: IC-7. Permitidas:
confirmación de lo irreversible, entrada mínima. Prohibidas: Dialog para lo reversible (usar
undo); Dialogs encadenados; formularios largos. Composición: en Overlay. NO usar: como patrón por
defecto (IC-7: preferir hacer+undo).

**C11 · Context Menu** — Propósito: acciones del objeto bajo el cursor (IC-4 clic derecho).
Responsabilidades: listar acciones aplicables (Menu Item Groups) con Shortcut Hints; orden
consistente. Comportamiento: origen en el cursor (Emil: origin-aware); teclado (IC-5); nunca la
única vía (IC-4). IC: IC-4/5. Permitidas: contextual por tipo de objeto. Prohibidas: acciones no
aplicables activas; submenús profundos. Composición: en Overlay. NO usar: para acciones globales
(Palette).

**C12 · Command Palette** — Propósito: superficie universal de acciones y salto (IC-5).
Responsabilidades: fuzzy, consciente del contexto, **toda acción alcanzable aquí**; enseña atajos.
Comportamiento: **sin animación** (altísima frecuencia, Emil); Esc cierra; mismo motor que Search
(O4). IC: IC-5/10. Permitidas: una, global. Prohibidas: Palette que omite acciones (rompería la
garantía de paridad). Composición: Overlay + Search + Menu Item Groups. Reuso: todo el ecosistema.
NO usar: para navegación espacial (usar Dock/Sidebar).

**C13 · Tree** — Propósito: jerarquía navegable (Tree Nodes). Responsabilidades: expandir,
seleccionar, buscar, arrastrar; virtualizar a escala (P7). Comportamiento: IC-1/3/4/5. IC:
IC-1/3/4/5. Permitidas: con Search, con arrastre. Prohibidas: Tree como sistema de ficheros
(pilar 2). Composición: Scroll Container + Tree Nodes. NO usar: para listas planas.

**C14 · Status Bar** — Propósito: pulso permanente de glance (Z2). Responsabilidades: alojar
Status Segments; resumir Consola/Performance/honestidad (P4/P15). Comportamiento: informativo;
algunos segmentos invocan detalle. IC: IC-8. Permitidas: una, fija, mínima. Prohibidas: acciones
primarias, formularios. Composición: Status Segments. NO usar: para lo interactivo primario.

### D · PLANTILLAS (Templates)

**D1 · Shell** — Propósito: la plantilla raíz que realiza el Layout Conceptual (marco Z1/Z2 +
anillos + centro sagrado). Responsabilidades: colocar organismos en zonas; aplicar la política
responsiva (DC-D2) y de comparación (DC-D1); memoria de layout (P1). Comportamiento: invariante
entre Labs (P9); un Lab solo llena las zonas de contenido. IC: todo IC (es el marco donde vive).
Permitidas: una por Window. Prohibidas: Shells alternativos por Lab (rompería P9). Composición:
Dock + Panels + Sidebars + Preview + Timeline + Status Bar + Top Bar. Reuso: el launcher y otras
herramientas del ecosistema usan un Shell (con o sin todas las zonas). NO usar: fuera de una
Window.

**D2 · Zone Template** — Propósito: contrato de "qué organismo va en cada zona" (Z1–Z12).
Responsabilidades: fijar posiciones relativas invariables (P9). Comportamiento: un Lab declara qué
pone en cada zona, no dónde. IC: —. Permitidas: la del Studio; variantes reducidas para
herramientas más simples (p. ej. visor de cartas = Preview + Inspector). Prohibidas: reubicar
zonas. Composición: mapea zonas→organismos. NO usar: para inventar layouts nuevos.

### E · PATRONES (Patterns)

Comportamientos transversales; no son componentes, son *cómo se combinan* para cumplir el
Interaction Canon. Cada Lab los hereda "gratis":

- **E1 Selección/Focus** (IC-1): Selection Highlight + Focus Indicator + Inspector reflejando el
  focus.
- **E2 Edición en vivo** (IC-2/P11): Property Grid ↔ Preview con hot-reload.
- **E3 Undo/Redo** (IC-6): toda edición como paso reversible; Historial navegable.
- **E4 Invocación** (IC-5/7/8): Overlay para Palette/Dialog/Menu/Toast.
- **E5 Revelación progresiva** (IC-4/P4/P14): hover revela; detalle bajo demanda.
- **E6 Modo Comparación** (DC-D1): Preview dual manual, disposición adaptativa.
- **E7 Preservación** (IC-11): Preset (aplicar/previsualizar) + Snapshot (capturar/restaurar).
- **E8 Feedback** (IC-8): Progress + Toast + Error/Loading State + Status Bar, no bloqueantes.

---

## Catálogo Canónico de Componentes

| Nivel | Componentes |
|-------|-------------|
| **Átomos** | Divider · Badge · Chip · Progress · Focus Indicator · Selection Highlight · Shortcut Hint · Scroll Container |
| **Moléculas** | Search · Tabs · Breadcrumb · Resource Card · Tree Node · Property Row · Toast · Notification · Empty State · Error State · Loading State · Menu Item Group · Status Segment |
| **Organismos** | Panel · Dock · Toolbar · Sidebar · Inspector · Property Grid · Timeline · Preview · Overlay · Dialog · Context Menu · Command Palette · Tree · Status Bar |
| **Plantillas** | Shell · Zone Template |
| **Patrones** | Selección/Focus · Edición-en-vivo · Undo/Redo · Invocación · Revelación progresiva · Comparación · Preservación · Feedback |
| **Frontera** | Window (superficie de nivel superior; 1 Shell por Window) |

---

## Reglas de Evolución del Design System

1. **Extensión, no ruptura (F-EVO):** añadir variantes/props opcionales; nunca cambiar el
   comportamiento canónico de un componente existente. Cambio incompatible → nueva versión PDS.
2. **Un problema, un componente:** antes de crear uno nuevo, probar si un existente + variante lo
   resuelve. Duplicar capability está prohibido (F-REUSE).
3. **Prueba de pertenencia (pilar 2):** un componente entra solo si una pantalla real del
   ecosistema PTCG lo necesita; no "porque es estándar".
4. **Neutralidad en la base, dominio en el borde:** los componentes base no conocen el Modelo
   Mental; solo Resource Card / Property Grid / Preview / Timeline lo encarnan.
5. **Todo atributo visual por token (F-VIS):** ningún valor visual horneado; re-tematizable
   Studio↔launcher sin duplicar.
6. **Paridad de teclado obligatoria (F-A11Y):** un componente interactivo sin operación por
   teclado no es admisible.
7. **Obediencia al Interaction Canon:** un componente no puede introducir un gesto/comportamiento
   que contradiga IC-0…IC-13.

---

## Componentes Prohibidos (canon)

- ❌ **Editor de texto genérico / área de código** (pilar 2; editar = objetos + parámetros).
- ❌ **Terminal embebida**, **explorador de archivos**, **panel de Git** (pilar 2).
- ❌ **Tabs para navegar entre Labs** (es navegación de primer nivel, IC-12).
- ❌ **Panel que se auto-reordena por tamaño** o **Preview con auto-split** (DC-D1/P9).
- ❌ **Dialog como patrón por defecto** / confirmaciones para lo reversible (IC-7).
- ❌ **Modal para errores recuperables** (usar Error State/Toast, IC-8).
- ❌ **Tree/Breadcrumb de sistema de ficheros** (pilar 2).
- ❌ **Submenús profundos** y **menús con acciones no aplicables activas** (IC-4).
- ❌ **Componentes con valores visuales horneados** (F-VIS).
- ❌ **Marketplace / instalador de componentes de terceros** (pilar 2).

## Componentes Experimentales (estatus, no promesa)

Zona de cuarentena: pueden existir marcados como *Experimental* (status del canon) hasta pasar
`revision-critica`. No se usan en pantallas canónicas hasta promoverse. Candidatos plausibles a
futuro (NO aprobados aún, requieren su propia prueba de pertenencia):
- **Graph/Node Canvas** (si un Lab futuro necesitara edición nodal) — riesgo: presiona la
  gramática de interacción; auditar contra IC-0.
- **Curve Editor avanzado** (más allá del `CurveField`) — probable, para easing fino.
- **Diff Viewer** (comparación estructurada más allá del Preview dual) — evaluar vs E6.
Regla: un experimental NO es canónico; promoverlo exige Auditorías completas.

---

# Decisiones Canónicas

- **DC-DS1 · PDS = lenguaje de interfaz del ecosistema PTCG** (Studio + launcher + visor +
  herramientas), reutilización **interna**, nunca librería genérica de terceros.
- **DC-DS2 · Jerarquía Atomic de 5 niveles** (Átomos→Moléculas→Organismos→Plantillas→Patrones) +
  Window como frontera; dependencia solo hacia abajo.
- **DC-DS3 · Catálogo cerrado por criterio:** los componentes del Catálogo Canónico, evolucionables
  solo por extensión (F-EVO) y prueba de pertenencia.
- **DC-DS4 · Capa de tokens reservada:** ningún valor visual horneado; el *feel* (Fase 7) se
  aplica por token sobre componentes que ya existen.
- **DC-DS5 · Neutralidad/base vs dominio/borde:** solo Resource Card, Property Grid, Preview,
  Timeline conocen el Modelo Mental; el resto es reutilizable sin dominio.
- **DC-DS6 · Todo componente obedece el Interaction Canon** y expone paridad de teclado.

---

# Auditoría Semántica

1. **¿Concepto ambiguo?** No; los nombres de componente son convencionales y no chocan con los
   conceptos del Modelo Mental (p. ej. "Preview" el componente = superficie que muestra el
   Preview-concepto; misma palabra, un solo sentido, coherente). "Inspector"/"Timeline"/"Preview"
   son a la vez zona (espacial), concepto (mental) y componente (PDS) — **se alinean a propósito**
   (la zona hospeda el componente que muestra el concepto), no divergen.
2. **¿Dos palabras para un concepto?** No; se prohíbe "widget/control" como sinónimos sueltos:
   el término es *componente* y cada uno tiene nombre propio.
3. **¿Un concepto con dos significados?** No; "Panel" (contenedor) ≠ "Dialog"/"Overlay"
   (efímeros) quedan separados; "Tabs" acotado (no navegación de Labs).
4. **¿Modelo mental construible?** Sí: 5 niveles + reglas de familia → se deduce dónde encaja
   cualquier pieza nueva.
5. **¿Palabra a prohibir?** Sí: "widget", "add-on", "plugin de componente", "editor de código"
   como componentes.
6. **¿Escala décadas?** Sí; catálogo cerrado + extensión por variantes + tokens = crecimiento sin
   proliferación.

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No; los materializa en piezas (P2 Preview central, P4 overlays, P7
   Search/Tree virtual, P11 edición en vivo).
2. **¿Contradice Arq. Espacial / Layout / Workflows / Modelo Mental / Interaction Canon?** No; el
   Shell realiza el Layout, cada componente cita su IC, y Resource/Preset/Snapshot se encarnan en
   componentes concretos.
3. **¿Excepciones innecesarias?** No; consolida y prohíbe explícitamente lo que rompería el canon.
4. **¿Duplicación de responsabilidades?** Evitada por F-REUSE y "un problema, un componente".
5. **¿Regla de Oro (I1)?** Sí; los componentes de dominio muestran/editan datos que resuelven
   engine/framework reales; no incrustan lógica de juego.
6. **¿Escala a cientos de Labs / décadas?** Sí; un Lab nuevo compone el catálogo existente, no
   crea componentes.
7. **¿Deuda futura?** Baja; riesgo = presión por componentes genéricos → filtra la prueba de
   pertenencia; los Experimentales están en cuarentena.
8. **¿Decisión a revisar?** Ninguna abierta.
9. **¿Canónico?** Sí — Design System arquitectónico completo.

# Auditoría de Filosofía

1. **¿Desarrollo Unipersonal?** Sí; sin componentes de colaboración/permresos/roles; favoritos y
   sesión personales.
2. **¿Complejidad para múltiples desarrolladores?** Ninguna.
3. **¿Simplificable por unipersonal?** Ya: sin componentes de gestión de usuarios/concurrencia.
4. **¿IA como herramienta?** Sí; ningún componente modela a la IA como actor.
5. **¿Simplicidad/preservación/largo plazo?** Sí; catálogo cerrado, tokens, extensión — envejece
   bien.
6. **¿Especialización Absoluta?** Reforzada: prohíbe editor de texto/terminal/git/archivos/
   marketplace; el PDS es del ecosistema PTCG, no genérico.

# Impacto en el Futuro

- **Decisiones condicionadas:** el Diseño Visual (Fase 7) rellena los **tokens** y el *feel* sobre
  este catálogo; no crea componentes nuevos salvo prueba de pertenencia. Cada Lab futuro = componer
  el catálogo + declarar contenido.
- **Módulos afectados:** el PDS será un módulo/paquete reutilizable por `studio-web` y por las
  futuras interfaces del ecosistema (launcher, visor). El contrato `StudioLab` consume componentes,
  no los define.
- **Oportunidades:** identidad de interfaz única en todo el ecosistema durante décadas; construir
  una pantalla = ensamblar piezas conocidas; testing centralizado; re-tematización por tokens.
- **Limitaciones (deliberadas):** no habrá componentes genéricos "de IDE"; ampliar el catálogo es
  un acto auditado, no libre.
- **Riesgos a vigilar:** (R1) proliferación de componentes casi-iguales → F-REUSE. (R2) valores
  visuales horneados colándose → F-VIS. (R3) un Lab exigiendo un gesto propio en un componente →
  IC-0. (R4) Experimentales usados en producción sin promover → cuarentena estricta.

# Evaluación frente al estándar AAA

> *¿Estaría este sistema de componentes a la altura de Figma, Unreal, JetBrains, VS Code o Unity?*

**Sí.** Justificación:
- Esas herramientas escalan porque tienen **un catálogo de componentes cerrado y consistente** con
  jerarquía clara (paneles acoplables, inspector/property grid, command palette, tree, timeline).
  El PDS adopta ese repertorio probado y lo **cierra por criterio** en vez de dejarlo crecer.
- Añade disciplina de élite explícita: **tokens desde el día 1** (re-tematización sin duplicar,
  como los design systems de Figma/JetBrains), **prueba de pertenencia** (contra el bloat típico
  de IDEs), y **obediencia a un Interaction Canon** (consistencia declarada, no emergente).
- **Ventaja propia:** al ser el lenguaje de **todo el ecosistema PTCG**, logra una coherencia de
  identidad que las herramientas generalistas no persiguen para un dominio concreto.
- **Qué faltaría para el máximo (Fase 7, no aquí):** el *aspecto y el feel* — tokens de color,
  tipografía, elevación, motion (curvas/duraciones de Emil), estados de foco visibles. A nivel de
  **arquitectura de componentes**, el PDS ya está al nivel de esas herramientas.
```

---

# Anexo Fase 10 — Component Styling (materialización)

*El PDS (arquitectura de componentes) se **viste** con los Visual Tokens de la Fase 9. No se crean
componentes, tokens ni principios; se aplica el lenguaje visual existente. Ejecutado en iteraciones
internas (una sola Fase 10), cada una compilable y auditable. Infraestructura de tema:
`core/designsystem/.../tokens/StudioTheme.kt` (CompositionLocals del `Scheme`/densidad activos).*

## Cadena oficial de dependencia (arquitectura del Design System)
**Regla dura (desde 10.2):** ningún componente/composable puede consumir Visual Tokens directamente.
Todo componente consume EXCLUSIVAMENTE su ComponentStyle correspondiente. La cadena canónica es:

`Component → ComponentStyle → Visual Tokens → Theme`

- **Component** (composable PDS): comportamiento; lee solo su descriptor de ComponentStyle.
- **ComponentStyle** (`ComponentStyle.kt` / `NavigationStyle.kt` / `ContainerStyle.kt`): único punto
  autorizado a leer Visual Tokens (9.1–9.6). Expone apariencia por estado.
- **Visual Tokens** (Fase 9): Color/Typography/Spacing/Depth/Surface/Motion, congelados.
- **Theme** (`StudioTheme.kt`): resuelve el `Scheme`/densidad activos vía CompositionLocals.

Cambiar de tema = cambiar el `Scheme` provisto; ni componentes ni descriptores se tocan (VL-EVO-3).

## Modelo de estilado
- **Descriptores de estilo, no reimplementación.** `ComponentStyle.kt` define la *apariencia por
  estado* de cada componente (colores/métricas/tipografía/motion/foco); el comportamiento sigue en el
  composable PDS. Separa *cómo se ve* (Fase 10) de *cómo se comporta* (PDS/Interaction Canon).
- **Solo tokens, cero literales.** Todo color sale de `StudioColorTokens.Scheme` (resuelto por tema),
  toda distancia de `StudioSpacingTokens`, forma/borde/elevación de `StudioSurfaceTokens`, texto de
  `StudioTypographyTokens`, transición de `StudioMotionTokens`, plano de `StudioDepthTokens`.
- **Estados canónicos** (`VisualState`): Rest·Hover·Pressed·Focused·Disabled·Selected·Loading·Error,
  mapeados a IC-1/IC-4/IC-5/IC-8. Foco = anillo único (`StudioFocusRing`, A5). Contraste reservado
  (VG-K): el chasis reposa; solo foco/selección/alerta suben.

## Iteración 10.1 — Controles básicos ✅ (artefacto: `ComponentStyle.kt`)
Button (Primary/Neutral/Ghost/Danger) · IconButton · ToggleButton · SegmentedButton · TextField ·
SearchField · Checkbox · RadioButton · Switch · Slider · Progress · Spinner · Chip · Badge. Cada uno
define colores por estado + métricas (altura = densidad, radius Control, gaps de la familia de
distancias) + tipografía (Label/Body/Badge) + motion (Confirmation para pulsación, Change para
entrada/salida) + foco. Botón primario = acento; Danger = feedback; Ghost = transparente + acento;
selección = acento tenue + `selectionEdge`; error/foco en campos suben el borde a `focusRing`/
`feedbackDanger`.

## Iteración 10.2 — Navegación y contenedores ✅ (artefactos: `NavigationStyle.kt`, `ContainerStyle.kt`)
- **Navegación** (`NavigationStyle.kt`): Tabs · Toolbar · Sidebar · Dock · Menu · ContextMenu ·
  StatusBar. Contrato común `NavItemStyle` (colores por estado + tipografía + motion + foco). Tabs =
  indicador `selectionEdge` que se desliza (Continuity); Sidebar = fila activa `selectionBackground` +
  barra guía `selectionEdge`; Menu/ContextMenu = superficie Floating + resaltado `accentMuted` + pista
  de atajo (Shortcut/`contentMuted`), item destructivo `feedbackDanger`; Toolbar/StatusBar = chrome de
  contenido (material Toolbar) + separadores hairline; StatusBar en densidad Compact.
- **Contenedores** (`ContainerStyle.kt`): Divider · ScrollArea · Scrollbar · Panel · Window. Divider =
  frontera por color (subtle/strong), sin estados; Scrollbar = pulgar que sube contraste en
  hover/arrastre y se oculta disabled; ScrollArea = chasis transparente (hereda superficie del host);
  Panel = material Panel, panel activo eleva la frontera a `focusRing`; Window = material Dialog +
  scrim, distingue activa (`contentEmphasis`) de inactiva (`contentSecondary`), entrada centrada.
- **Comportamiento intacto:** solo apariencia. Sin componentes/tokens/principios nuevos.

## Iteración 10.3 — Visualización de datos ✅ (artefactos: `DataDisplayStyle.kt`, `InspectorStyle.kt`)
- **Datos** (`DataDisplayStyle.kt`): Table · Tree · TreeRow · ListView · ListRow · EmptyState ·
  Placeholder · LoadingPlaceholder · Skeleton. Contrato común `DataRowStyle` (colores por estado +
  motion + foco + marcador de selección); filas activas = `selectionBackground` + `selectionEdge`.
  Table = encabezado Label, celdas Body/Numeric (mono, VG-U4), rejilla hairline, radio None, orden con
  `accentRest`; Tree/List = chevron/marcador + sangría por token; EmptyState = Display + Body atenuado;
  Skeleton/Loading = brillo Linear / Spinner de 10.1.
- **Inspector** (`InspectorStyle.kt`): PropertyRow · PropertyGroup · InspectorSection. PropertyRow =
  etiqueta Label secundaria + valor Body/Numeric, edición con borde `focusRing`, error `feedbackDanger`;
  PropertyGroup = pertenencia por proximidad (VG-E1/E2), sin recuadro; InspectorSection = material
  Inspector, encabezado Section colapsable con chevron.
- **Estado `editing` sin token nuevo:** se materializa reutilizando `TextFieldStyle` de 10.1 (expuesto
  como `InlineEditFieldStyle`); no se añade `VisualState` (ComponentStyle congelado). Comportamiento
  intacto: solo apariencia.

## Iteración 10.4 — Feedback y overlays ✅ (artefactos: `FeedbackStyle.kt`, `OverlayStyle.kt`)
- **Feedback no bloqueante** (`FeedbackStyle.kt`): Tooltip · Popover · Snackbar · Toast. Tooltip =
  Floating + Caption, no interactivo; Popover = Floating + foco atrapado (`focusRing`); Snackbar/Toast =
  Notification (material Toast), tono por `BadgeStyle.Tone`, salida más rápida que entrada, pausa al hover.
- **Interrupción** (`OverlayStyle.kt`): Dialog · ModalDialog · ConfirmationDialog · ProgressOverlay ·
  BusyOverlay · BlockingOverlay. Dialog = Modal + scrim, entrada centrada; ConfirmationDialog reutiliza
  `ButtonStyle` (Neutral/Danger, la destructiva nunca es foco por defecto); Progress/Busy = Modal +
  ProgressStyle/SpinnerStyle de 10.1; BlockingOverlay = **Plane.Alert** + motion Error.
- **Jerarquía al pie de la letra:** cada overlay declara su `Plane` existente y resuelve la elevación
  con `Elevation.forPlane()`; scrim de `overlayScrim`; sin nuevos planos ni materiales; motion solo con
  roles de 9.6; foco = anillo único.

## Iteración 10.5 — Especializados del Studio ✅ (artefactos: `TimelineStyle.kt`, `PreviewStyle.kt`)
- **Timeline** (`TimelineStyle.kt`): Timeline · TimelineTrack · TimelineMarker · TimelineSelection.
  Chrome de contenido (material Timeline), reglilla Numeric legible sobre decoración, marcador con
  acento único que se reposiciona (Continuity), rango con `selectionEdge`/`selectionBackground`.
- **Preview y manipulación directa** (`PreviewStyle.kt`): Preview · PreviewOverlay · SelectionFrame ·
  TransformHandle · ResizeHandle · GuideLine · DropTarget. Preview permanece en **Plane.Content**
  (telón acromático, protagonismo del contenido); PreviewOverlay solo sube a Raised; SelectionFrame usa
  **exclusivamente** `selectionEdge`; handles neutros que no compiten (contraste solo bajo puntero);
  GuideLine neutra (`borderDefault`, nunca acento); DropTarget acento tenue / rechazo feedback Danger.
- **Reutilización sin duplicar:** ResizeHandle delega en TransformHandle; StatusBar ya vivía en 10.2.

# Auditoría final de la Fase 10 (cierre) ✅
- □→✔ **Todos los componentes del PDS poseen ComponentStyle.** 10.1 controles · 10.2 navegación/
  contenedores · 10.3 datos/inspector · 10.4 feedback/overlays · 10.5 especializados. Cobertura de
  átomos/moléculas/organismos del inventario de la Parte II.
- □→✔ **Ningún descriptor accede a Visual Tokens fuera de la capa semántica** (`Scheme`/roles); no se
  referencian primitivos `Foundation.*` salvo las escalas ya autorizadas en 10.1 (Space para tamaños).
- □→✔ **Todos consumen exclusivamente ComponentStyle** (los composables leerán el descriptor, no el token).
- □→✔ **Sin valores hardcodeados:** `grep` de literales `Color(0x…)`, `.dp`, `.sp` en `*Style.kt` = 0
  coincidencias; único `Color.Transparent` = ausencia de fondo (no color de tema).
- □→✔ **Sin estilos duplicados entre familias:** delegación/alias (`ModalDialogStyle`→`DialogStyle`,
  `ContextMenuStyle` by `MenuStyle`, `ResizeHandleStyle`→`TransformHandleStyle`), helpers compartidos
  (`dataRowColors`, `NavItemStyle`, `DataRowStyle`, `InlineEditFieldStyle`).
- □→✔ **Jerarquía `Component → ComponentStyle → Visual Tokens → Theme` sin excepciones**, incluidos los
  especializados (Timeline/Preview/handles no abren excepciones "por especiales").
- □→✔ **Compila:** `:core:designsystem:compileReleaseKotlin` BUILD SUCCESSFUL.

**La Fase 10 queda oficialmente CERRADA.** Lista para la Fase 11 — Screen Design (ensamblaje de
componentes ya estilizados; no se reabren decisiones de diseño ni se crean reglas visuales nuevas).

## Auditoría de compatibilidad (10.4)
- **Congelación total:** Filosofía, Interaction Canon, Design System, Visual Tokens, ComponentStyle
  10.1–10.3 y cadena de dependencias intactos; sin principios, tokens ni estados nuevos. ✔
- **Jerarquía Depth+Surface+Motion:** Dialog=Modal, Tooltip/Popover=Floating, Snackbar/Toast=Notification,
  BlockingOverlay=Alert; elevación siempre por `Elevation.forPlane()`; sin nuevos planos/materiales. ✔
- **Sin excepciones ni literales:** descriptores = único lector de tokens (`Color.Transparent` no aplica
  aquí); motion solo roles de 9.6; foco anillo único. ✔
- **Solo apariencia:** comportamiento intacto. Compila `:core:designsystem:compileReleaseKotlin`. ✔

## Auditoría de compatibilidad (10.3)
- **Congelación total:** no se tocan Filosofía, Design System, Interaction Canon, Visual Tokens,
  ComponentStyle de 10.1/10.2 ni la arquitectura de dependencias; sin tokens ni principios nuevos. ✔
- **Cadena de dependencia:** descriptores = único lector de Visual Tokens; sin literales
  (`Color.Transparent` = ausencia de fondo, no color). ✔
- **`editing` sin alterar ComponentStyle:** reutiliza `TextFieldStyle` (10.1) en lugar de añadir estado. ✔
- **Solo apariencia:** comportamiento de los componentes intacto. ✔
- **Compila:** `:core:designsystem:compileReleaseKotlin` BUILD SUCCESSFUL. ✔

## Auditoría de compatibilidad (10.2)
- **Cadena de dependencia:** los descriptores son el único punto que lee Visual Tokens; los
  componentes consumirán solo ComponentStyle (`Component → ComponentStyle → Visual Tokens → Theme`). ✔
- **Congelación:** no se tocan Visual Tokens, ComponentStyle de 10.1, Design System ni Interaction
  Canon; no se crean tokens ni principios. ✔
- **Solo apariencia:** ningún cambio de comportamiento; los estados son descriptivos. ✔
- **Compila:** `:core:designsystem:compileReleaseKotlin` BUILD SUCCESSFUL. ✔

## Auditoría de compatibilidad (10.1)
- **Visual Tokens (9.1–9.6):** consumo exclusivo; sin literales; sin tokens nuevos. ✔
- **Interaction Canon:** estados = IC-1/4/5/8; foco único (A5/IC-5); motion de pulsación = Confirmation
  (IC-4). ✔
- **VG/VL:** contraste reservado (VG-K), silencio por defecto, protagonismo (los controles no compiten
  con el contenido). ✔
- **Arquitectura / Regla de Oro:** descriptores en el subpaquete `tokens`; no tocan el juego; no crean
  componentes experimentales ni del juego. Compila (`:core:designsystem:compileReleaseKotlin`). ✔
