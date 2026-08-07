# Developer Studio — Layout Conceptual (Fase U2)

**Traducción de UX + Arquitectura Espacial + Workflows en una distribución espacial concreta.**
NO es diseño visual: sin colores, iconografía, tipografía ni componentes gráficos finales.
Aquí se deciden **posiciones, proporciones y comportamientos responsivos**, no estética.

**Referencias obligatorias:** `UX-PRINCIPLES.md` (P1–P15) · `SPATIAL-ARCHITECTURE.md` (Z1–Z12,
anillos) · `WORKFLOWS.md` (W1–W21, O1–O8). Toda proporción se justifica contra ellas.

> Recordatorio de método (del usuario): la UI es *consecuencia* de los workflows. Este layout
> existe para que W5 (bucle del 70%) y W1 (continuidad) sean casi perfectos; el resto encaja
> alrededor sin estorbarlos.

**Unidad de medida:** proporciones *relativas y adaptativas* (fracciones del área útil, no
píxeles). Los números son **recomendaciones de partida**, no dogmas de tamaño fijo.

---

## Parte I — Definición espacial por zona

Cada zona: posición · tamaño relativo · proporciones · prioridad espacial · redimensionar ·
pantalla pequeña · pantalla ultraancha · multi-monitor · al ocultarse · interacción.

**Escala de prioridad espacial** (quién cede ante quién cuando falta sitio):
`Preview(100) > Inspector(80) > Área de Trabajo(80, si el Lab la usa) > Timeline(70) >
Biblioteca(50) > Barras(fijas, mínimas) > contextuales(0, viven ocultas)`.

### Z1 · Barra Superior
- **Posición:** franja horizontal, borde superior, ancho total.
- **Tamaño relativo:** ~4–5% de la altura; **fijo** (no escala con contenido).
- **Prioridad:** estructural máxima (nunca cede su franja, pero es delgada).
- **Redimensionar:** altura constante; el contenido interno se comprime/prioriza, nunca crece.
- **Pantalla pequeña:** conserva contexto esencial (Lab › recurso); acciones globales colapsan
  a invocables (Palette).
- **Ultraancha:** no se estira su altura; el exceso de ancho lo absorben el centro y los flancos.
- **Multi-monitor:** única, vive en el monitor del marco principal.
- **Al ocultarse:** no se oculta (P9, orientación permanente).
- **Interacción:** refleja selección de Biblioteca (Z3) y contenido de Preview (Z6).

### Z2 · Barra de Estado
- **Posición:** franja horizontal, borde inferior, ancho total.
- **Tamaño relativo:** ~3% de la altura; **fijo y mínimo**.
- **Prioridad:** estructural, coste casi nulo.
- **Redimensionar:** altura constante; datos periféricos se priorizan al comprimir.
- **Pantalla pequeña:** mantiene solo los indicadores críticos (conexión, estado preview,
  honestidad P15); el resto pasa a invocable.
- **Ultraancha:** el ancho extra permite más *glance* sin expandir altura.
- **Multi-monitor:** única, marco principal.
- **Al ocultarse:** no se oculta (P4: presencia mínima permanente).
- **Interacción:** resume en una línea lo que Consola (Z8) y Performance (Z11) detallan.

### Z6 · Preview *(el rey — se define antes que sus vecinos porque los gobierna)*
- **Posición:** **centro geométrico** del área entre barras.
- **Tamaño relativo:** el **mayor bloque contiguo**: recomendado **≥55–65% del ancho** interior
  y la mayor parte de la altura útil (menos Timeline si aplica).
- **Proporciones:** se adapta al *aspect ratio* del artefacto (una animación de tablero
  vertical vs un shader cuadrado); mantiene el artefacto sin deformar (P15).
- **Prioridad:** **100 — máxima**. Todo lo demás cede ante él.
- **Redimensionar:** **crece primero, encoge último**. Al ganar espacio, se lo queda el Preview.
- **Pantalla pequeña:** conserva su centralidad; los flancos colapsan a invocables para que el
  Preview no baje de un mínimo utilizable.
- **Ultraancha:** puede **absorber ancho** o entrar en **modo comparación dual** (W6/W9): dos
  previews lado a lado aprovechando el ancho extra (O2).
- **Multi-monitor:** **desacoplable a pantalla completa** en un monitor (P2 llevado al extremo).
- **Al ocultarse:** **nunca** (ocultarlo niega el Studio).
- **Interacción:** gobernado por Inspector (Z7) y Timeline (Z9); comparado en su propio modo
  (Z10); medido por Performance (Z11).

### Z7 · Inspector
- **Posición:** **flanco derecho** (lado de acción), adyacente y simultáneo al Preview.
- **Tamaño relativo:** **~20–25% del ancho** interior; redimensionable dentro de un rango.
- **Proporciones:** columna alta; scroll vertical para esquemas de muchos parámetros.
- **Prioridad:** 80 (segundo tras el Preview): es la otra mitad del bucle (W5).
- **Redimensionar:** rango acotado (ni desaparece ni domina); al faltar sitio, encoge antes que
  el Preview pero después de la Biblioteca.
- **Pantalla pequeña:** colapsa a invocable (deslizable sobre el borde), pero su reapertura es 1
  gesto — porque W5 lo necesita constantemente.
- **Ultraancha:** ancho estable; el excedente va al Preview, no al Inspector (params no ganan por
  más ancho).
- **Multi-monitor:** desacoplable (útil junto a Preview en el otro monitor).
- **Al ocultarse:** rara vez conviene (rompe W5); su resumen no vive en barras (es acción, no
  estado).
- **Interacción:** el par **Preview↔Inspector es el bucle del 70%** (P3). Hospeda variantes
  (W6), historial (Z12) y docs en contexto (P14).

### Z3 · Biblioteca
- **Posición:** **flanco izquierdo** (lado de navegación).
- **Tamaño relativo:** **~15–20% del ancho** interior; **colapsable a 0**.
- **Proporciones:** columna alta con búsqueda arriba (W2) y resultados debajo.
- **Prioridad:** 50 (cede antes que Inspector y Preview).
- **Redimensionar:** primera en encoger cuando falta sitio; puede colapsar del todo durante la
  iteración pura.
- **Pantalla pequeña:** **oculta por defecto**, invocable como overlay lateral (P4).
- **Ultraancha:** ancho estable; no crece (una lista más ancha no ayuda; la escala se resuelve
  por búsqueda, P7).
- **Multi-monitor:** desacoplable (útil para curaduría/exploración en monitor aparte).
- **Al ocultarse:** el trabajo del 70% no la necesita; se recupera con búsqueda/Palette.
- **Interacción:** su selección alimenta Preview (Z6), Inspector (Z7) y Área de Trabajo (Z5).

### Z5 · Área Principal de Trabajo *(condicional al Lab)*
- **Posición:** **coincide con el centro** (Z6): en Labs de manipulación (Rules/Battlefield) el
  centro **es** editable y el resultado se observa ahí mismo (W12).
- **Tamaño relativo:** el del centro (hereda de Z6).
- **Prioridad:** 80 cuando existe; en Labs de solo-parametrización no existe y el Preview ocupa
  todo el centro.
- **Redimensionar / responsivo:** idénticos a Z6 (son el mismo bloque físico con distinto rol).
- **Multi-monitor:** viaja con el centro.
- **Al ocultarse:** no aplica (es el corazón cuando existe).
- **Interacción:** manipulo aquí → veo efecto aquí mismo → ajusto en Inspector (Z7).

### Z9 · Timeline *(condicional a Labs temporales)*
- **Posición:** **franja horizontal bajo el centro** (adyacente al Preview).
- **Tamaño relativo:** **~8–12% de la altura** interior cuando está presente.
- **Prioridad:** 70; cede altura al Preview antes que al revés.
- **Redimensionar:** altura acotada; el scrubbing necesita ancho → toma el ancho del centro.
- **Pantalla pequeña:** se comprime a controles esenciales (play/scrub); detalles a invocable.
- **Ultraancha:** aprovecha el ancho (más resolución de scrubbing).
- **Multi-monitor:** normalmente viaja con el Preview.
- **Al ocultarse:** **desaparece del todo** en Labs sin dimensión temporal (P4) — libera esa
  altura para el Preview.
- **Interacción:** conduce el tiempo del Preview mirándolo (P3).

### Z8 · Consola / Z11 · Performance / Z12 · Historial *(contextuales, drawer/panel invocable)*
- **Posición:** **drawer inferior** (Consola, Performance) sobre la Status Bar; **panel junto al
  Inspector** (Historial, por ser "del objeto activo").
- **Tamaño relativo:** al invocarse, **~25–35% de la altura** (drawer) o el ancho del Inspector
  (Historial); **0% cuando ocultas** (estado por defecto).
- **Prioridad:** 0 permanente; al invocarse, roban temporalmente al centro y se retiran (W18).
- **Redimensionar:** el drawer es redimensionable mientras está abierto; nunca fija.
- **Pantalla pequeña:** ocupan overlay casi completo al invocarse (no hay sitio para convivir);
  se cierran al terminar.
- **Ultraancha:** el drawer no necesita todo el ancho; puede quedar acotado y centrado.
- **Multi-monitor:** **desacoplables** (Performance en continuo en monitor aparte es un caso de
  uso real de optimización).
- **Al ocultarse:** su resumen de una línea permanece en Status Bar (Z2) (P4).
- **Interacción:** detallan lo que Z2 insinúa; Historial alimenta Comparación (Z10) y W15.

### Z10 · Benchmark / Comparación *(modo del Preview, no zona propia)*
- **Posición:** **dentro del centro**: el Preview se **divide en dos** (W6/W9).
- **Tamaño relativo:** dos mitades del centro; sincronizadas.
- **Prioridad:** hereda 100 (es el rey en modo dual).
- **Responsivo:** en pantalla pequeña, la comparación puede alternar A/B en el mismo espacio en
  vez de dividir (swap) si no cabe lado a lado. En ultraancha, la división es natural (O2).
- **Multi-monitor:** una mitad por monitor es una disposición válida.
- **Al ocultarse:** es un modo; se sale y el centro se reúne.
- **Interacción:** consume docs (P14, W8/W9) y decide curaduría (W7).

### Z4 · Command Palette *(overlay sin espacio permanente)*
- **Posición:** overlay **centrado**, transitorio, sobre todo lo demás.
- **Tamaño relativo:** **0% permanente**; al invocarse, un bloque central acotado.
- **Responsivo/multi-monitor:** aparece en el monitor con foco; tamaño estable relativo.
- **Al ocultarse:** su estado natural (solo existe invocada).
- **Interacción:** puede actuar sobre cualquier zona (W19); comparte motor con la búsqueda (O4).

### Síntesis de proporciones de partida (pantalla estándar, Lab temporal, sin contextuales)
```
Altura: [Z1 Top ~5%] [ núcleo ~89% ] [Z2 Status ~3%]
Núcleo (ancho): [Z3 Biblioteca ~18%] [Z6 Preview ~57%] [Z7 Inspector ~25%]
Núcleo (alto):  [ Preview/Área ~85% ] [Z9 Timeline ~15%]
```
*En Lab no temporal: Timeline desaparece → Preview toma su altura. En iteración pura:
Biblioteca colapsa → Preview toma su ancho. Ultraancha: el excedente va a Preview o a modo dual.*

---

## Parte II — Alternativas de layout (ventajas / desventajas)

Se evalúan contra el bucle del 70% (W5), la continuidad (W1), la comparación (W6/W9) y la
escala a 30 años.

### A · IDE clásico (laterales + editor central + panel inferior)
- **Ventajas:** familiar; jerarquía por permanencia clara; contextuales bien resueltos con
  drawer inferior. Encaja con P4/P9.
- **Desventajas:** trata el centro como "editor de texto", no como *resultado visual temporal*;
  no da rango de primera clase a la comparación (P6) ni a la Timeline. El centro no es "el rey"
  por diseño, solo "lo grande".
- **Veredicto:** buena base estructural, insuficiente para P2/P6.

### B · Centrado en Preview (todo orbita el resultado, chrome minimalista tipo lienzo)
- **Ventajas:** máxima expresión de P2/P3/P10; el resultado domina y el chrome se desvanece;
  ideal para W5. Comparación natural (dividir el lienzo).
- **Desventajas:** por sí solo descuida la navegación a escala (P7) y las contextuales; si no se
  estructura, degenera en "un lienzo con paneles flotantes" difícil de aprender (contra P9).
- **Veredicto:** el alma correcta, le falta esqueleto.

### C · Adaptable según el Lab (cada Lab define su propio layout)
- **Ventajas:** cada dominio obtiene lo que necesita (P12); Rules vs Shader pueden diferir.
- **Desventajas:** **rompe P9** si el marco cambia entre Labs → el usuario se reorienta cada
  vez; con cientos de Labs es inmanejable (anti-escala). Alto riesgo de fragmentación.
- **Veredicto:** peligroso como principio rector; válido solo para el *contenido*, no el marco.

### D · Por Workspaces (disposiciones preconfiguradas por tarea, conmutables)
- **Ventajas:** presets "foco preview" / "curaduría" / "diagnóstico" aceleran W20; memoria de
  layout (P1). Buen complemento.
- **Desventajas:** como base única, añade una capa de gestión (elegir workspace) que puede
  friccionar el arranque; no define por sí mismo dónde va cada zona.
- **Veredicto:** excelente **encima** de una base fija, no como base.

### E · Híbrido (marco IDE estable + centro-rey + workspaces sobre esa base)
- **Ventajas:** combina el esqueleto de A (estructura, contextuales), el alma de B (Preview
  rey), la disciplina de P9 (marco invariante), y los workspaces de D como capa opcional (P12).
- **Desventajas:** exige disciplina para no derivar en C (que el marco NO cambie entre Labs).
- **Veredicto:** **la síntesis correcta.** Es el Layout Canónico.

---

## Parte III — Layout Canónico del Developer Studio

**Elección: Híbrido (E)** — *marco invariante + centro sagrado + flancos + drawer contextual +
workspaces como capa opcional.* Formalizado por anillos (coherente con la Arquitectura Espacial):

1. **Marco fijo (Anillo 0):** Top Bar (Z1, ~5% alto) y Status Bar (Z2, ~3% alto), ancho total,
   **nunca cambian entre Labs** → *P9, P1*. Enmarcan todo con coste mínimo → *P4, P10*.

2. **Centro sagrado (Anillo 1, corazón):** Preview/Área de Trabajo (Z6/Z5), ≥55–65% del ancho,
   la mayor altura útil. **Crece primero, encoge último, nunca se oculta** → *P2, P3*. En Labs de
   manipulación es editable (W12); en Labs de parámetros es puro Preview (W5). Admite **modo dual**
   para comparación (Z10, W6/W9) → *P6, O2*.

3. **Flancos (Anillo 1):**
   - Derecha = **Inspector** (Z7, ~20–25%), pegado y simultáneo al centro → el par que sostiene
     W5 → *P3, P11*. Rara vez se oculta.
   - Izquierda = **Biblioteca** (Z3, ~15–20%, **colapsable a 0**) → navegación a escala por
     búsqueda → *P7*; cede primero para dar aire al centro → *P2, P4*.

4. **Banda temporal (Anillo 1, condicional):** Timeline (Z9, ~8–15% alto) bajo el centro, **solo
   si el Lab tiene tiempo**; si no, desaparece y el centro gana su altura → *P4, P3*.

5. **Diagnóstico y memoria (Anillo 2, invocable):** drawer inferior (Consola Z8, Performance
   Z11) y panel junto al Inspector (Historial Z12), **ocultos por defecto**, un gesto para
   abrir/cerrar, resumen mínimo en Status Bar → *P4, P5, W18*.

6. **Sin espacio permanente (Anillo 3):** Command Palette (Z4, overlay) y el **modo comparación**
   (Z10, división del centro). Cero footprint hasta invocarse → *P4, P5, P6*.

7. **Capa opcional de Workspaces (sobre la base fija):** presets de disposición conmutables
   (foco-preview / curaduría / diagnóstico) que **solo alteran qué contextuales están abiertas y
   los tamaños de los flancos**, jamás las posiciones relativas → *P12 sobre P9, W20*.

**Reglas de responsividad canónicas (una sola política, todos los tamaños):**
- **Falta espacio →** encogen en este orden: contextuales (ya ocultas) → Biblioteca → Timeline →
  Inspector → *nunca* el Preview por debajo de su mínimo utilizable.
- **Pantalla pequeña →** flancos y drawer pasan a **overlays invocables**; queda marco + centro.
  El bucle W5 sigue siendo posible (Preview + Inspector deslizable) → *P2, P3*.
- **Ultraancha →** el excedente de ancho va al **Preview** o habilita **comparación dual** por
  defecto → *P2, P6, O2*. Los flancos NO se ensanchan (no aportan por más ancho).
- **Multi-monitor →** cualquier zona salvo el marco (Z1/Z2) puede **desacoplarse**; el centro
  sigue coherente sin ella; el arreglo se recuerda → *P1, P12, W21*.

**Por qué escala 30 años:** la escala (miles de recursos, cientos de Labs) **no** se absorbe
añadiendo paneles, sino por **búsqueda en la Biblioteca** (P7) y por **contenido variable sobre
marco fijo** (P9/P12). El número de zonas es **constante**; crece el *contenido*, no la
*estructura*. Por eso no se degrada con el tiempo.

---

# Auditoría de Coherencia

Análisis crítico del presente documento contra todo el canon (`UX-PRINCIPLES`, `SPATIAL-
ARCHITECTURE`, `WORKFLOWS`, y la Regla de Oro de `ARCHITECTURE`). No es checklist: busca fallos.

1. **¿Contradice P1–P15?** No detecto contradicción; las refuerza. Riesgo de *tensión* real:
   fijar Inspector en ~20–25% (P3, siempre visible) compite con "Preview crece primero" (P2). Se
   resuelve con la política de encogido (Inspector cede *después* de Biblioteca/Timeline, *antes*
   que el Preview) y con su colapso a overlay en pantalla pequeña. Es una tensión gestionada, no
   una contradicción.
2. **¿Contradice la Arquitectura Espacial?** No: es su traducción directa (anillos, zonas, roles
   idénticos). Añado *números de partida* que la Espacial dejó abiertos a propósito; no reasigno
   ningún rol.
3. **¿Contradice los Workflows Canónicos?** No. W5 (par Preview↔Inspector simultáneo), W1
   (memoria de layout), W6/W9 (modo dual), W2/W19 (Biblioteca/Palette), W18 (drawer), W20/W21
   (workspaces/multi-monitor) tienen todos su correlato espacial explícito.
4. **¿Excepciones innecesarias?** Una vigilar: el **modo comparación dual por defecto en
   ultraancha**. Es cómodo (O2) pero es una excepción de comportamiento según ancho de pantalla.
   Mitigación: debe ser *sugerencia reversible*, no forzado — si no, viola la previsibilidad (P9).
   Lo marco como decisión a validar (punto 9).
5. **¿Duplicación de responsabilidades?** No. Área de Trabajo (Z5) y Preview (Z6) comparten el
   *mismo bloque físico* con roles distintos por Lab: es *fusión* deliberada, no duplicación.
   Benchmark (Z10) es un *modo* del Preview, no un panel paralelo → evita duplicar superficie.
6. **¿Respeta la Regla de Oro?** Sí. El layout no contiene lógica de juego; solo distribuye
   espacio. La única zona que "toca" el dominio (Rules Lab en Z5) edita un `GameState` del engine
   real; el layout no modela nada propio.
7. **¿Escala a cientos de Labs / miles de recursos / décadas?** Sí, por construcción: nº de zonas
   **constante**, escala absorbida por búsqueda (P7) y contenido-sobre-marco-fijo (P9). Riesgo
   latente: si un futuro Lab *exige* una zona estructural nueva, habría que ampliar la gramática
   (ver Impacto §limitaciones).
8. **¿Deuda técnica/conceptual futura?** Menor: (a) los porcentajes de partida podrían
   endurecerse informalmente en implementación y volverse "mágicos" — deben quedar como *tokens de
   layout* parametrizables. (b) La política responsiva única deberá probarse en pantallas
   realmente pequeñas (¿es W5 usable ahí, o es honesto declarar un tamaño mínimo soportado?).
9. **¿Decisiones a revisar antes de continuar?** Dos: **(D1)** ¿comparación dual *automática* en
   ultraancha o siempre *manual*? Recomiendo manual con sugerencia (preserva P9). **(D2)** ¿existe
   un *tamaño mínimo soportado* por debajo del cual el Studio declara honestamente "no óptimo"
   (P15) en vez de comprimir hasta lo inservible? Recomiendo declararlo.
10. **¿Canónico o necesita ajustes?** **Canónico con dos notas abiertas (D1, D2)** que no
    bloquean el avance pero deben cerrarse antes del diseño visual (U3+). El marco y las
    proporciones de partida se consideran estables.

---

# Impacto en el Futuro

- **¿Qué decisiones futuras condiciona este documento?** Fija el *contrato espacial* que el
  Layout Visual (U3), el Sistema de Interacción y los Componentes deberán respetar: dónde vive
  cada cosa, quién cede ante quién, cómo responde a cada tamaño. El diseño visual heredará estas
  proporciones como restricción, no como sugerencia.
- **¿Qué módulos se verán afectados?** El shell de `studio-web` (implementa este marco y la
  política responsiva), el contrato `StudioLab` (cada Lab declara si usa Área de Trabajo y/o
  Timeline → el marco decide mostrarlas), y `studio-preservation` (debe guardar *estado de
  layout* como parte de la sesión para P1/W1/W20/W21).
- **¿Qué oportunidades habilita?** (a) Workspaces conmutables baratos (capa sobre base fija).
  (b) Modo comparación como rasgo de primera clase reutilizable por todos los Labs. (c)
  Multi-monitor sin rediseño (desacople sobre marco coherente). (d) Onboarding barato: aprender
  un Lab = reconocer el marco.
- **¿Qué limitaciones introduce?** (a) El **número de zonas estructurales queda congelado**:
  añadir una zona permanente nueva en el futuro es un cambio de *gramática* (caro y global), no
  un plugin. Esto es intencional (protege P9), pero hay que asumirlo. (b) Labs con necesidades
  espaciales radicalmente distintas (p. ej. un editor de grafos a pantalla completa) deberán
  encajar en "Área de Trabajo = centro" o forzar la gramática.
- **¿Qué riesgos vigilar en fases siguientes?** (R1) *Deriva hacia el layout adaptable por Lab
  (alternativa C)*: cada Lab pidiendo "su" disposición hasta romper P9 — vigilar en cada Lab
  nuevo. (R2) *Porcentajes convertidos en constantes mágicas* — mantenerlos como tokens.
  (R3) *Preview comprimido por acumulación de contextuales ancladas* — la política de encogido y
  el autosave de layout deben protegerlo. (R4) *Cierre de D1/D2* antes del diseño visual.
- **Reevaluación recomendada:** revisar este layout cuando exista el **quinto Lab real** (masa
  crítica para saber si la gramática única aguanta dominios diversos) y al integrar el **primer
  Lab no temporal + no manipulativo** que estrese la condicionalidad de Timeline/Área de Trabajo.
