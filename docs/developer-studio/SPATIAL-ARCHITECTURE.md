# Developer Studio — Arquitectura Espacial

**Fase de diseño espacial. NO es diseño visual.** Sin colores, iconos, tipografías ni
componentes. Aquí solo se decide **cómo se reparte el espacio** para que el flujo de trabajo
(ver `UX-PRINCIPLES.md`) sea lo más eficiente posible.

> Analogía: un arquitecto distribuye las zonas de un edificio (dónde va la cocina, dónde el
> dormitorio, por qué juntos, qué se ve desde la entrada) **antes** de elegir muebles y
> pintura. Este documento es esa planta funcional. Los "muebles" (widgets) vienen después.

Toda decisión se justifica citando los principios canónicos: **P1** continuidad · **P2** el
resultado es el rey · **P3** bucle cerrado · **P4** visible lo permanente/oculto lo ocasional
· **P5** coste ∝ frecuencia · **P6** comparar de primera clase · **P7** escala por búsqueda ·
**P8** reversibilidad · **P9** consistencia entre Labs · **P10** confort · **P11** latencia ·
**P12** adaptable sobre base estable · **P13** el trabajo sobrevive · **P14** enseñar en
contexto · **P15** honestidad.

---

## Parte I — Catálogo de zonas funcionales permanentes

Antes de distribuir, hay que decidir **qué zonas existen** y su naturaleza. Una "zona
funcional" es un rol del espacio (no un panel concreto). Distingo tres clases por su
comportamiento espacial, porque eso gobierna todo lo demás:

- **Estructurales (marco fijo):** siempre presentes, ancladas, definen la gramática espacial.
  Su posición NO cambia entre Labs (P9). No se desacoplan ni se mueven; a lo sumo se colapsan.
- **De trabajo (contenido del Lab):** el corazón mutable; su *sitio* es estable pero su
  *contenido* depende del Lab activo (P9 + P12).
- **Contextuales (bajo demanda):** aparecen cuando se necesitan y se retiran (P4). Pueden
  desacoplarse y moverse con más libertad.

Para cada zona: propósito · responsabilidades · información · frecuencia · por qué ese espacio
· relaciones · ocultable · desacoplable · movible · instancias.

---

### Z1 · Barra Superior (Top Bar) — *estructural*
- **Propósito:** orientación global y estado de "dónde estoy / en qué contexto trabajo".
- **Responsabilidades:** identificar el Lab activo, el recurso/variante seleccionado, la
  sesión, y el modo de preview (headless vs mirror, ligado a P15). Punto de acceso a acciones
  globales (no de Lab).
- **Información:** ruta de contexto (Lab › recurso › variante), identidad de sesión, modo de
  preview, indicadores globales mínimos.
- **Frecuencia:** lectura *constante* (glance), interacción *baja*.
- **Por qué ese espacio:** una franja delgada arriba, siempre visible: es el "letrero de
  calle" que evita perderse (P1 continuidad de contexto; P9 mismo sitio siempre).
- **Relaciones:** refleja lo seleccionado en Biblioteca (Z3) y lo que se ve en Preview (Z6).
- **Ocultable:** no (es ancla de orientación). **Desacoplable:** no. **Movible:** no.
- **Instancias:** única.

### Z2 · Barra de Estado (Status Bar) — *estructural*
- **Propósito:** pulso de salud del sistema, siempre disponible sin robar atención.
- **Responsabilidades:** mostrar estado no-crítico continuo: conexión con backend, estado del
  preview (reproduciendo/pausado), métricas ligeras (FPS/nodos activos como *glance*),
  avisos discretos, honestidad del preview (P15: "esquemático, no pixel-perfect").
- **Información:** señales de fondo, nunca acciones primarias.
- **Frecuencia:** lectura periférica *constante*; interacción *casi nula*.
- **Por qué ese espacio:** franja fina inferior. Cumple P4 (información permanente pero
  comprimida y periférica) y P10 (reposo: los datos de fondo no interrumpen).
- **Relaciones:** resume, en una línea, lo que Consola (Z8) y Performance (Z11) detallan.
- **Ocultable:** no (o solo por preferencia; su coste espacial es mínimo). **Desacoplable:**
  no. **Movible:** no. **Instancias:** única.

### Z3 · Biblioteca / Navegador de Recursos — *estructural*
- **Propósito:** el punto de entrada a *qué* voy a trabajar; escala a miles de recursos.
- **Responsabilidades:** buscar, filtrar y seleccionar recursos (animaciones, shaders,
  reglas, escenarios…). Es la puerta de la **exploración** (20% del tiempo).
- **Información:** resultados de búsqueda/filtro por nombre, tag, tipo, estado, fecha; NO una
  lista enumerada completa (P7).
- **Frecuencia:** *media* — se usa al cambiar de tarea (frecuente pero no en el bucle del 70%).
- **Por qué ese espacio:** lateral, colapsable. Presente cuando exploro, retirable cuando
  itero (P4). Su prioridad de espacio es menor que el Preview (P2).
- **Relaciones:** su selección alimenta Área de Trabajo (Z5), Preview (Z6) e Inspector (Z7).
- **Ocultable:** **sí** — durante la iteración pura estorba (P2, P3). **Desacoplable:** sí
  (útil en multi-monitor para curaduría). **Movible:** sí (lateral izq/der). **Instancias:**
  única (varias vistas confunden el "dónde busco").

### Z4 · Command Palette — *contextual (invocable)*
- **Propósito:** ejecutar cualquier acción por descripción, sin buscarla en menús.
- **Responsabilidades:** acceso universal por teclado a comandos de Lab y globales; también
  salto rápido a recursos (P7 aplicado a acciones).
- **Información:** lista efímera filtrada por lo que escribes; desaparece al actuar.
- **Frecuencia:** *media-alta* para usuarios expertos; *cero footprint* cuando no se usa.
- **Por qué ese espacio:** **cero espacio permanente**. Overlay temporal centrado que aparece
  y se va. Es la máxima expresión de P4 (existe siempre, presente nunca) y P5 (una acción
  rara alcanzable en un gesto sin ocupar metros fijos).
- **Relaciones:** puede disparar acciones sobre cualquier zona.
- **Ocultable:** por naturaleza (solo existe al invocarse). **Desacoplable:** no aplica.
  **Movible:** no. **Instancias:** única, transitoria.

### Z5 · Área Principal de Trabajo — *de trabajo*
- **Propósito:** el "escenario" del Lab activo; donde ocurre la manipulación directa que un
  Lab necesita (p. ej. el tablero editable del Rules Lab, el lienzo del Battlefield Lab).
- **Responsabilidades:** albergar la interacción específica del dominio; es la zona con más
  libertad de contenido (P12) sobre sitio fijo (P9).
- **Información:** depende del Lab; en muchos Labs el "área de trabajo" y el "preview"
  coinciden o están fusionados.
- **Frecuencia:** *alta* en Labs de manipulación (Rules/Battlefield/UI); *baja o nula* en Labs
  donde el trabajo es solo parametrizar (Animation/Shader), donde el Preview absorbe su rol.
- **Por qué ese espacio:** centro, el mayor. Es el foco de atención junto al Preview.
- **Relaciones:** íntimamente ligada al Preview (Z6) e Inspector (Z7): manipulo aquí, veo el
  efecto en el Preview, ajusto en el Inspector.
- **Ocultable:** parcial (según Lab). **Desacoplable:** no (es el corazón). **Movible:** no
  (su centralidad es la gramática). **Instancias:** una por Lab activo.

### Z6 · Preview — *de trabajo (protagonista)*
- **Propósito:** mostrar **el resultado real** de lo que se está creando. Es *el rey* (P2).
- **Responsabilidades:** reproducir el artefacto (animación/shader/render) con máxima
  fidelidad honesta (P15), permitir re-reproducir sin fricción (P5), y ser el ancla visual
  del bucle de iteración (P3).
- **Información:** el artefacto en ejecución + su estado mínimo de reproducción.
- **Frecuencia:** *máxima* — se mira constantemente (70% del tiempo).
- **Por qué ese espacio:** el mayor bloque contiguo, central, siempre visible. Todo lo demás
  cede espacio ante él (P2). Nunca se tapa para editar (P3).
- **Relaciones:** gobernado por Inspector (Z7) y Timeline (Z9); comparado con Benchmark (Z10);
  medido por Performance (Z11). Es el punto de convergencia del Studio.
- **Ocultable:** **nunca** (ocultarlo niega el propósito del Studio). **Desacoplable:** sí
  (multi-monitor: preview a pantalla completa en un monitor). **Movible:** no dentro del marco
  (su centralidad es invariante). **Instancias:** normalmente **una**; **dos o más de forma
  temporal** para comparación lado a lado (P6) — ver decisión en Parte III.

### Z7 · Inspector — *de trabajo (compañero del Preview)*
- **Propósito:** ver y **editar** todos los parámetros del objeto seleccionado (P.I: el
  control del bucle de iteración).
- **Responsabilidades:** exponer el esquema de parámetros editable; los cambios se reflejan en
  el Preview en caliente (P3, P11). Es el "mando" del resultado.
- **Información:** parámetros del recurso activo (duration, curve, overshoot, layer…), su ficha
  y metadatos bajo demanda.
- **Frecuencia:** *muy alta* — es la otra mitad del bucle de iteración junto al Preview.
- **Por qué ese espacio:** lateral, **adyacente y simultáneo al Preview** — jamás en otra
  ventana ni pestaña que tape el resultado (P3: editar y observar, mismo lugar y momento).
- **Relaciones:** el par Preview↔Inspector *es* el bucle del 70%. También muestra variantes
  (P6) e invoca docs (P14) en contexto.
- **Ocultable:** sí, pero rara vez conviene (rompe el bucle). **Desacoplable:** sí (multi-
  monitor). **Movible:** sí (lateral opuesto a Biblioteca). **Instancias:** única (refleja "el
  objeto activo"; múltiples inspectores fragmentarían el foco).

### Z8 · Consola / Logs — *contextual*
- **Propósito:** detalle diagnóstico cuando algo va mal o se quiere trazar.
- **Responsabilidades:** registro cronológico de eventos, errores del pipeline, mensajes del
  backend; salida de acciones.
- **Información:** flujo textual, revisable, no crítico salvo error.
- **Frecuencia:** *baja* en operación normal; *puntual/alta* al depurar.
- **Por qué ese espacio:** panel inferior colapsable. Oculto por defecto (P4); su resumen de
  una línea vive en la Status Bar (Z2).
- **Relaciones:** detalla lo que Z2 insinúa; complementa Performance (Z11).
- **Ocultable:** **sí**, por defecto oculta. **Desacoplable:** sí. **Movible:** sí (zona
  inferior). **Instancias:** única.

### Z9 · Timeline — *de trabajo (condicional al Lab)*
- **Propósito:** control temporal de artefactos que transcurren en el tiempo (animaciones,
  cámaras, audio).
- **Responsabilidades:** play/pause/step/loop/scrub/velocidad; navegar el tiempo del preview.
- **Información:** posición temporal, duración, marcadores.
- **Frecuencia:** *muy alta* en Labs temporales (Animation/Camera/Audio); *inexistente* en Labs
  no temporales (Shader estático, UI).
- **Por qué ese espacio:** franja horizontal **bajo el Preview**, adyacente a él (controlo el
  tiempo mirando el resultado, P3). Aparece solo cuando el Lab tiene dimensión temporal.
- **Relaciones:** conduce el Preview (Z6); acotada por Inspector (Z7) que define los params de
  la animación.
- **Ocultable:** **sí** — desaparece en Labs sin tiempo (P4: presente solo cuando aplica).
  **Desacoplable:** raramente. **Movible:** limitado (su valor está pegado al Preview).
  **Instancias:** única.

### Z10 · Benchmark / Comparación — *contextual (de primera clase)*
- **Propósito:** contrastar el resultado contra una referencia o contra otra variante (P6).
- **Responsabilidades:** poner dos fuentes frente a frente (referencia externa vs nuestra
  implementación; v3 vs v7) y mostrar sus docs (P14).
- **Información:** dos artefactos comparables + su documentación de las skills.
- **Frecuencia:** *media* — clave en modo exploración y en autoría (promover a canónica).
- **Por qué ese espacio:** **reutiliza el espacio del Preview** entrando en "modo comparación"
  (el Preview se divide), en lugar de reclamar metros propios permanentes. Comparar es tan
  central (P6) que merece ser un *modo del rey*, no un panel secundario.
- **Relaciones:** es una faceta del Preview (Z6); consume docs (P14); alimenta la decisión de
  curaduría (Z3/Inspector).
- **Ocultable:** sí (es un modo, no permanente). **Desacoplable:** sí. **Movible:** hereda del
  Preview. **Instancias:** una a la vez (comparación pareada; más de dos satura el juicio).

### Z11 · Performance / Métricas — *contextual*
- **Propósito:** medir el coste del artefacto (FPS, frame time, draw calls, memoria, nodos).
- **Responsabilidades:** diagnóstico cuantitativo bajo demanda; su versión *glance* vive en Z2.
- **Información:** métricas en detalle, series temporales.
- **Frecuencia:** *baja-media* — se consulta al optimizar, no en cada iteración.
- **Por qué ese espacio:** panel invocable (comparte zona con Consola/Historial en la banda
  inferior o lateral). Oculto por defecto (P4).
- **Relaciones:** mide el Preview (Z6); su resumen está en Status Bar (Z2).
- **Ocultable:** sí. **Desacoplable:** sí (útil en monitor aparte para vigilar en continuo).
  **Movible:** sí. **Instancias:** única.

### Z12 · Historial / Preservación — *contextual*
- **Propósito:** navegar versiones y snapshots; volver a un estado antiguo (P13).
- **Responsabilidades:** listar versiones/variantes/snapshots del recurso activo, restaurar,
  comparar contra el presente (enlaza con Z10).
- **Información:** línea de vida del recurso (v1..vN), snapshots con su entorno.
- **Frecuencia:** *baja* (autoría/curaduría, 10%) pero *crítica* para los 30 años.
- **Por qué ese espacio:** panel invocable junto al Inspector (el historial *es* del objeto
  activo). Oculto por defecto (P4), un gesto para abrirlo (P5).
- **Relaciones:** del Inspector (Z7, mismo objeto); alimenta Comparación (Z10); materializa P13.
- **Ocultable:** sí. **Desacoplable:** sí. **Movible:** sí. **Instancias:** única.

---

### Síntesis de la naturaleza espacial

| Zona | Clase | Frecuencia | Ocultable | Desacoplable | Movible | Instancias |
|------|-------|-----------|-----------|--------------|---------|-----------|
| Z1 Top Bar | Estructural | Glance constante | No | No | No | 1 |
| Z2 Status Bar | Estructural | Glance constante | No | No | No | 1 |
| Z3 Biblioteca | Estructural | Media | Sí | Sí | Sí | 1 |
| Z4 Command Palette | Contextual | Media (0 footprint) | Innata | — | No | 1 transitoria |
| Z5 Área de Trabajo | De trabajo | Alta (según Lab) | Parcial | No | No | 1/Lab |
| Z6 Preview | De trabajo ★ | Máxima | **Nunca** | Sí | No | 1 (2 temporal) |
| Z7 Inspector | De trabajo | Muy alta | Sí (raro) | Sí | Sí | 1 |
| Z8 Consola | Contextual | Baja/puntual | Sí (def. oculta) | Sí | Sí | 1 |
| Z9 Timeline | De trabajo | Alta si temporal | Sí | Raro | Limitado | 1 |
| Z10 Benchmark | Contextual | Media | Sí (modo) | Sí | Hereda | 1 |
| Z11 Performance | Contextual | Baja-media | Sí | Sí | Sí | 1 |
| Z12 Historial | Contextual | Baja/crítica | Sí | Sí | Sí | 1 |

**Lectura clave:** solo **4 zonas** compiten por espacio permanente en el bucle del 70%
(Preview, Inspector, y —según Lab— Área de Trabajo y Timeline). Todo lo demás es periférico
(barras) o invocable (contextual). Eso es P2+P4 hechos planta.

---

## Parte II — Modelos espaciales de referencia (qué aprender, no qué copiar)

No copiamos ninguna; extraemos su **principio espacial** y lo cruzamos con nuestro caso.

- **VS Code — "Actividad lateral + editor central + panel inferior colapsable".**
  Principio útil: **jerarquía por permanencia**: el centro (editor) es sagrado; lo lateral e
  inferior se colapsa. Encaja con P2/P4. Límite: VS Code es *mono-artefacto textual*; nosotros
  tenemos preview visual temporal, que pide una banda temporal (Timeline) que VS Code no tiene.

- **Unreal Engine — "Viewport central dominante + Details a la derecha + Content Browser
  abajo + Outliner".** Principio útil: **el viewport (resultado) manda y los Details (inspector)
  viven pegados a él**; es casi exactamente nuestro par Preview↔Inspector (P2+P3). Riesgo:
  Unreal satura con decenas de paneles simultáneos → fatiga (choca con P10); tomamos su eje
  viewport-details, no su densidad.

- **Unity — similar a Unreal (Scene/Game + Inspector + Hierarchy + Project).** Aporta la
  distinción **Scene (editable) vs Game (resultado)** = nuestra distinción **Área de Trabajo
  (Z5) vs Preview (Z6)**, que en algunos Labs se fusionan y en otros no. Confirma que ambas
  pueden coexistir.

- **Blender — "áreas divisibles y reconfigurables; workspaces por tarea".** Principio útil:
  **workspaces preconfigurados por tipo de trabajo** = para nosotros, *presets espaciales por
  Lab* sobre un marco común (P9+P12). Riesgo: la libertad total de Blender tiene curva de
  aprendizaje alta; nosotros fijamos la gramática y solo dejamos ajustar el detalle.

- **Figma — "lienzo infinito central + capas a la izquierda + propiedades a la derecha; UI
  que se desvanece".** Principio útil: **el lienzo domina y los paneles son delgados y
  discretos; el chrome cede ante el contenido** (P2+P10). Muy alineado con "el resultado es el
  rey". Aporta también la idea de *paneles que casi desaparecen* para sesiones largas.

- **JetBrains IDEs — "tool windows acoplables a los cuatro bordes, colapsables, con memoria de
  layout".** Principio útil: **tool windows como zonas contextuales que se anclan a bordes y se
  recuerdan**; y **memoria de estado por proyecto** (P1 continuidad). Es el mejor modelo para
  nuestras zonas contextuales (Z8/Z10/Z11/Z12).

- **Chrome DevTools — "panel principal + drawer inferior invocable (Esc); todo en un contenedor
  acoplable/desacoplable".** Principio útil: **el drawer inferior invocable** para lo
  diagnóstico (Consola/Performance) que aparece con un gesto y se va (P4+P5). Nuestro Z8/Z11
  siguen justo este patrón.

### Destilado transversal
1. **Centro sagrado para el resultado** (Unreal/Unity/Figma) → nuestro Preview (P2).
2. **Inspector pegado al resultado**, nunca en otra ventana (Unreal/Unity/Figma) → par Z6↔Z7
   (P3).
3. **Laterales colapsables para navegación** (VS Code/JetBrains) → Biblioteca (P4/P7).
4. **Drawer inferior invocable para diagnóstico** (DevTools/VS Code) → Consola/Performance
   (P4/P5).
5. **Banda temporal solo si hay tiempo** → Timeline condicional (P4).
6. **Marco fijo + presets por tarea** (Blender/JetBrains) → gramática invariable, layout por
   Lab (P9/P12).
7. **Memoria de layout y contexto** (JetBrains) → continuidad al abrir (P1).

---

## Parte III — Arquitectura Espacial Canónica del Developer Studio

Una única gramática espacial, válida para todos los Labs durante décadas. Se describe por
**anillos de permanencia**, de fuera (siempre) hacia dentro (foco), no como pantalla dibujada.

### Anillo 0 — Marco de orientación (siempre presente, coste mínimo)
- **Top Bar (Z1)** arriba y **Status Bar (Z2)** abajo: dos franjas finas que enmarcan todo.
  Nunca se ocultan, nunca se mueven. Son las "paredes maestras": dan orientación (P1) y pulso
  (P4/P15) sin robar foco (P10). **Justificación:** anclas invariables → P9; presencia
  comprimida y periférica → P4.

### Anillo 1 — Núcleo de trabajo (el 70%, siempre visible)
El interior se organiza en torno a un **centro sagrado** y **dos flancos**:
- **Centro: Preview (Z6)** — el mayor bloque, siempre visible, nunca tapado. En Labs de
  manipulación, el **Área de Trabajo (Z5)** *es* este centro (editable) y el resultado se
  observa en el mismo lugar; en Labs de parametrización, el centro es puramente Preview.
  **Justificación:** P2 (el resultado es el rey), P3 (no taparlo para editar).
- **Flanco de control: Inspector (Z7)** — pegado al centro, simultáneo, del lado "de acción".
  **Justificación:** el par Preview↔Inspector materializa el bucle de iteración → P3, P11.
- **Flanco de navegación: Biblioteca (Z3)** — lado opuesto, **colapsable**. Presente al
  explorar, retirable al iterar. **Justificación:** P7 (buscar) y P4 (retirable cuando el 70%
  no la necesita); su prioridad de espacio cede al Preview → P2.
- **Banda temporal: Timeline (Z9)** — bajo el centro, **solo si el Lab tiene tiempo**.
  **Justificación:** P3 (controlar el tiempo mirando el resultado), P4 (ausente si no aplica).

### Anillo 2 — Diagnóstico y memoria (invocable, oculto por defecto)
Una **banda inferior colapsable** y **paneles de borde** hospedan las zonas contextuales:
- **Drawer inferior invocable:** Consola (Z8), Performance (Z11) — aparecen con un gesto, se
  van (patrón DevTools/JetBrains). **Justificación:** P4 (presente solo cuando se necesita),
  P5 (un gesto para lo poco frecuente).
- **Junto al Inspector:** Historial/Preservación (Z12) — del objeto activo; se abre bajo
  demanda. **Justificación:** P13 (el trabajo sobrevive) sin ocupar el bucle diario → P4.

### Anillo 3 — Sin espacio permanente (overlays y modos)
- **Command Palette (Z4):** overlay transitorio, cero footprint, acceso universal.
  **Justificación:** P5 + P4 en su forma pura (existe siempre, presente nunca).
- **Comparación/Benchmark (Z10):** **no es un panel nuevo, es un MODO del Preview**: el centro
  sagrado se divide en dos para comparar (variante A/B, referencia vs nuestra, v-antigua vs
  actual) y se recompone al salir. **Justificación:** comparar es de primera clase (P6), así
  que se implementa dando al rey un modo dual, no degradándolo a un panel lateral. Aquí es
  donde el Preview admite **dos instancias temporales**; fuera de comparación, siempre una.

### Reglas de la gramática (invariantes durante 30 años)
1. **La posición relativa de las zonas estructurales y de trabajo NO cambia entre Labs.** Solo
   cambia su *contenido* y qué zonas contextuales están abiertas (P9 + P12). Aprender un Lab =
   reconocer dónde está todo.
2. **El Preview nunca se oculta ni se tapa para editar.** Todas las demás zonas ceden ante él
   (P2, P3).
3. **Lo contextual (Anillo 2 y 3) está oculto por defecto** y se invoca; su resumen mínimo
   vive en la Status Bar (P4).
4. **Todo el layout tiene memoria:** al abrir, se restaura el estado espacial exacto de la
   última sesión, incluido qué había abierto/colapsado (P1).
5. **La adaptabilidad se limita al Anillo 1–2** (colapsar, redimensionar flancos, desacoplar a
   otro monitor). El Anillo 0 y las posiciones relativas son fijas: se ajusta el *detalle*,
   nunca la *gramática* (P12 sobre P9).
6. **Desacoplamiento pensado para multi-monitor:** Preview, Inspector, Biblioteca y los
   diagnósticos pueden salir a monitor aparte; el marco central sigue coherente sin ellos.

### Cómo cada Lab habita la misma gramática (ejemplos, no pantallas)
- **Animation Lab:** centro = Preview; flanco = Inspector (params de la animación); Timeline
  activa; Comparación para variantes/benchmark. El bucle del 70% en estado puro.
- **Rules Lab:** centro = Área de Trabajo (tablero editable) *fusionado* con Preview; Inspector
  = propiedades del elemento seleccionado del estado; Timeline ausente (salvo que se dispare
  una animación sobre el estado); Historial guarda "escenarios".
- **Shader Lab:** centro = Preview; Inspector = uniforms; Timeline normalmente ausente;
  Performance más presente. Misma planta, distinto inquilino.

**Conclusión:** una sola planta funcional — *marco fijo + centro sagrado + dos flancos +
diagnóstico invocable + overlays sin espacio* — sirve a todos los Labs, escala a miles de
recursos (la escala se absorbe por búsqueda en la Biblioteca, no por más paneles) y respeta el
bucle del 70% como criterio rector.

---

**Límite de esta fase:** aquí termina la distribución funcional del espacio. NO se ha decidido
ningún aspecto visual (tamaños exactos, colores, tipografía, iconos, componentes). El siguiente
nivel —cuando se autorice— traducirá esta planta en diseño visual, validando cada elección
contra esta arquitectura espacial y contra `UX-PRINCIPLES.md`.
