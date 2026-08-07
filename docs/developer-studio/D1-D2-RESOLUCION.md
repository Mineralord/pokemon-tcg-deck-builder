# Developer Studio — Cierre de Decisiones D1 y D2

**Resolución definitiva de las dos decisiones abiertas del Layout Conceptual (U2).**
Análisis integrado por dos lentes profesionales, en una **única propuesta**:

- **`ui-ux-pro-max`** → ergonomía, arquitectura de información, consistencia, productividad,
  accesibilidad, escalabilidad.
- **`emil-design-eng`** → composición, lenguaje visual, herramientas AAA, percepción del
  usuario, jerarquía espacial.

Referencias obligatorias: `UX-PRINCIPLES` (P1–P15) · `SPATIAL-ARCHITECTURE` · `WORKFLOWS`
(W1–W21, O1–O8) · `LAYOUT-CONCEPTUAL` (U2). No es preferencia: es análisis contra el canon.

> **Nota de método:** donde las dos lentes coincidieron, lo señalo como *convergencia*
> (señal fuerte). Donde discreparon, expongo el conflicto y decido con justificación técnica.

---

## D1 — Comparación dual

### Recordatorio del problema
`LAYOUT-CONCEPTUAL` (O2 y decisión D1) dejó abierto si el **modo comparación** (Z10, dividir
el Preview en dos, W6/W9) debe activarse **automáticamente** al haber ancho suficiente, o
**manualmente**. La auditoría de U2 marcó el "auto por defecto en ultraancha" como posible
excepción que roza la previsibilidad (P9).

### Alternativas
- **A) Automática:** al superar cierto ancho, el Preview se divide solo.
- **B) Manual:** el usuario entra/sale del modo con un comando/gesto explícito.
- **C) Híbrida:** trigger manual, pero la *presentación dentro del modo* y la *facilidad de
  invocación* se adaptan al espacio.

### Análisis por ejes

| Eje | A (Auto) | B (Manual) | C (Híbrida) |
|-----|----------|------------|-------------|
| **Ergonomía** | El canvas cambia sin pedirlo → sobresalto | Control total, sin sorpresas | Control + comodidad |
| **Previsibilidad (P9)** | ❌ el layout muta con el resize | ✅ máxima | ✅ el *entrar* es siempre explícito |
| **Productividad (W6/O2)** | Rápido pero intrusivo | 1 gesto | 1 gesto + presentación óptima según espacio |
| **Carga cognitiva** | Alta: "¿por qué se dividió?" | Baja | Baja |
| **Aprendizaje** | Confuso (comportamiento mágico) | Claro y enseñable | Claro; la sugerencia enseña la función |
| **Consistencia** | ❌ mismo recurso se ve distinto según ventana | ✅ | ✅ el modo es idéntico; solo su disposición interna varía |
| **Escalabilidad** | Reglas de breakpoint frágiles a 30 años | Estable | Estable |
| **Multi-monitor (W21)** | Auto-split pelea con desacople manual | Limpio | Limpio; A/B puede repartirse por monitor |
| **Impacto Workflows** | Rompe la intención de W6 (comparar *cuando yo decido*) | Sirve W6/W9 tal cual | Sirve W6/W9 + O2 sin violarlos |
| **Impacto Arq. Espacial** | Z10 dejaría de ser "modo invocado" | Coherente (Z10 = modo) | Coherente |
| **Impacto Principios** | Choca P9; roza P2 (el sistema decide por el rey) | Cumple P4/P5/P6/P9 | Cumple P4/P5/P6/P9 |

### Lo que aportó cada lente
- **`ui-ux-pro-max`:** la previsibilidad y la consistencia mandan; un layout que se
  reorganiza según el ancho de ventana es una forma de *content-jumping* estructural —
  desorienta y viola el modelo mental. Recomienda invocación explícita con una *affordance*
  consistente, y advierte contra "comportamiento mágico" que no se puede aprender ni predecir.
- **`emil-design-eng`:** ninguna herramienta AAA (Figma, Unreal, Blender, Unity, JetBrains)
  **reestructura el canvas sola** según el tamaño de ventana; el *split view* es SIEMPRE un
  modo que el usuario activa. Un auto-split se percibe como que la herramienta "toma el
  control" del lienzo — lo contrario de la sensación de dominio que da un tool profesional. La
  percepción de control sobre el resultado (el rey) es sagrada.

### Convergencia y conflicto
**Convergencia fuerte:** ambas rechazan **A (automática)** por el mismo motivo desde ángulos
distintos (previsibilidad/consistencia ↔ percepción de control AAA). Es la señal más clara del
análisis.

**Conflicto menor:** `ui-ux-pro-max` empujaba hacia **B pura** (máxima previsibilidad);
`emil-design-eng` valora que, dentro del modo, la *composición* se adapte al espacio (lado a
lado en ancho amplio; apilado/swap en estrecho) por calidad visual. **Resolución técnica:** no
es contradictorio — se separan dos cosas distintas: **(1) entrar al modo** (siempre manual,
predecible → satisface a `ui-ux-pro-max`) y **(2) cómo se compone el modo una vez dentro**
(adaptativo al espacio → satisface a `emil-design-eng`). El conflicto era aparente porque
mezclaba "trigger" con "presentación".

### ✅ Decisión canónica D1 — **Híbrida (C), definida con precisión**
1. **Entrar y salir del modo comparación es SIEMPRE manual** (comando/gesto explícito, W6/W19).
   El resize de la ventana **nunca** activa ni desactiva la comparación. → P9, consistencia.
2. **La composición *dentro* del modo se adapta al espacio, sin cambiar el estado del modo:**
   - ancho amplio → **lado a lado** (O2);
   - ancho insuficiente → **apilado o swap A/B** en el mismo espacio.
   Adaptar la *disposición interna* no viola P9 porque el usuario ya eligió estar en el modo.
3. **Sugerencia, no acción:** con espacio de sobra, el Studio puede *insinuar* de forma
   discreta y descartable que la comparación está disponible (aprendizaje contextual, P14),
   pero **jamás la activa por su cuenta**.
4. **Comparación pareada (máx. 2)** se mantiene (WORKFLOWS W6): más de dos satura el juicio.
5. **Multi-monitor:** en modo comparación, A y B pueden repartirse entre monitores; sigue
   siendo el mismo modo, activado manualmente.

**Esto deroga la parte de O2 que decía "dual por defecto/automática en ultraancha".** O2 se
reinterpreta: en ultraancha la comparación es *más cómoda y se sugiere*, no *automática*.

---

## D2 — Tamaño mínimo soportado

### Principio rector (del usuario, adoptado)
El Studio **no** intentará funcionar en cualquier resolución. Prefiere ser **honesto sobre sus
límites** (P15) antes que degradarse hasta lo inservible. **Sin degradación infinita.**

### Lo que aportó cada lente
- **`emil-design-eng`:** las herramientas AAA son *desktop-first* y **declaran mínimos**; no
  fingen ser responsive móvil. Un tool profesional que se comprime hasta caber en un teléfono
  pierde su identidad y su composición. Mejor un mínimo digno y una declaración honesta que un
  layout roto.
- **`ui-ux-pro-max`:** la política debe ser **clara, coherente y accesible**: el aviso debe ser
  **no bloqueante** (el usuario puede tener motivos para seguir), legible, y con opción de
  continuar. Nada de lockouts duros. Coherencia de breakpoints para que escale a 30 años.

**Convergencia:** ambas quieren **mínimos declarados + aviso honesto no bloqueante**, no
responsive infinito. Sin conflicto real.

### Política canónica de tamaño (por niveles, ancho como eje primario, alto secundario)

| Nivel | Rango (ancho × alto) | Comportamiento |
|-------|----------------------|----------------|
| **Óptimo** | ≥ 1440 × 900 | Experiencia completa. Todos los flancos y contextuales caben sin colapsar. Objetivo de diseño. |
| **Completo** | 1280–1439 × ≥ 800 | Todo funcional. Flancos algo más estrechos; sin pérdida de función. |
| **Compacto** | 1024–1279 × ≥ 720 | Flancos (Biblioteca Z3, Inspector Z7) → **overlays invocables**; marco + centro + Timeline permanecen. El bucle W5 (Preview + Inspector deslizable) sigue intacto. |
| **Mínimo soportado** | 1024–1279 muy bajo, o **piso 1024 × 640** | Solo **marco (Z1/Z2) + centro (Preview)**; todo lo demás es overlay invocable. **Aviso discreto una vez** ("espacio reducido: algunas zonas se invocan bajo demanda"), descartable. |
| **No soportado** | < 1024 de ancho **o** < 640 de alto | **Aviso honesto persistente** (P15): "El Developer Studio está diseñado para pantallas de escritorio; este tamaño no permite una experiencia de trabajo correcta." **No bloquea** (deja ver el Preview), pero **NO se sigue rediseñando** por debajo: se conserva el layout mínimo. Fin de la degradación. |

### Reglas de comportamiento al reducir la ventana
1. **Orden de colapso** (coherente con la política responsiva de U2): contextuales (ya ocultas)
   → **Biblioteca** → **Timeline** → **Inspector** (a overlay) → **nunca el Preview** por
   debajo de su mínimo utilizable.
2. **Qué desaparece** (deja de ocupar espacio, se invoca): Timeline en Labs no temporales;
   drawer de diagnóstico; Historial.
3. **Qué colapsa** (se convierte en overlay deslizable, recuperable a 1 gesto): Biblioteca e
   Inspector.
4. **Qué nunca cede:** el marco (Z1/Z2, coste mínimo) y el Preview (Z6, el rey).
5. **Umbral del aviso:** el aviso "no soportado" aparece **solo** al cruzar el piso (< 1024 de
   ancho o < 640 de alto), no antes. En "mínimo soportado" el aviso es discreto y único.
6. **Reversibilidad:** al recuperar tamaño, el layout vuelve a su estado (memoria de layout,
   P1/W20); el aviso desaparece. Nada se pierde por haber encogido.
7. **Honestidad activa (P15):** el aviso explica *por qué* (no "error"), y ofrece **continuar**;
   no impone. Coherente con la Status Bar, que ya declara la honestidad del preview.

### Por qué estos números
Alineados con el estándar de herramientas de escritorio profesionales (Figma/Unreal/JetBrains
operan cómodas ~1280+ y declaran mínimos ~1024): un tool web profesional puede fijar **1024×640
como piso soportado** y **1440×900 como objetivo óptimo**, sin pretender ser responsive móvil.
Son *recomendaciones de partida* parametrizables como tokens (evita constantes mágicas, deuda
señalada en U2).

---

# Decisiones Canónicas

Solo las decisiones **definitivas aprobadas**. Pasan a formar parte del canon del Developer
Studio y son de obligada referencia para todas las fases siguientes.

- **DC-D1 · Comparación dual = Híbrida controlada.** Entrar/salir del modo comparación es
  **siempre manual**; el resize nunca lo activa/desactiva. La *disposición interna* del modo se
  adapta al espacio (lado a lado / apilado-swap). Con espacio amplio se puede *sugerir* (no
  activar). Máx. 2 elementos. Deroga el "auto por defecto en ultraancha" de O2.
- **DC-D2 · Tamaño soportado = política de niveles honesta y finita.** Óptimo ≥1440×900;
  Completo 1280–1439; Compacto 1024–1279 (flancos → overlays); Mínimo soportado piso
  **1024×640** (solo marco + Preview); por debajo, **aviso honesto persistente, no bloqueante y
  sin más degradación**. Orden de colapso fijo; el Preview nunca cede; todo reversible. Números
  como tokens parametrizables.

---

# Auditoría de Coherencia

Análisis crítico contra Principios UX, Arquitectura Espacial, Workflows y Layout Conceptual.

1. **¿Contradice P1–P15?** No; los refuerza. DC-D1 protege P9 (previsibilidad) y respeta P6/P2;
   DC-D2 materializa P15 (honestidad) y P1 (memoria de layout). Tensión resuelta: P5
   (comodidad) quería auto-split; se satisface con "sugerencia", sin sacrificar P9.
2. **¿Contradice la Arquitectura Espacial?** No; la consolida. Z10 sigue siendo *modo del
   Preview* (no zona), y el orden de colapso de DC-D2 respeta la escala de prioridad espacial
   (Preview 100 nunca cede).
3. **¿Contradice los Workflows?** No. DC-D1 sirve W6/W9 sin violar su intención ("comparar
   cuando yo decido"); DC-D2 preserva W5 en Compacto y W20/W21 (memoria/desacople).
   **Corrige** O2: se reinterpreta de "automática" a "sugerida", eliminando la excepción que la
   auditoría de U2 ya había señalado como sospechosa. Es una mejora de coherencia, no una nueva
   contradicción.
4. **¿Excepciones innecesarias?** Se **elimina** una (auto-split de O2). DC-D2 introduce niveles,
   pero son una política *uniforme y declarada*, no excepciones ad-hoc.
5. **¿Duplicación de responsabilidades?** No. La adaptación de composición dentro del modo
   comparación reutiliza la política responsiva general; no crea un segundo sistema de layout.
6. **¿Respeta la Regla de Oro?** Sí. Ninguna decisión toca lógica del juego; son políticas de
   presentación/espacio del Studio.
7. **¿Escala a cientos de Labs / miles de recursos / décadas?** Sí. DC-D1 es una regla única
   para todos los Labs; DC-D2 son breakpoints estables como tokens. Ninguna crece con el nº de
   recursos.
8. **¿Deuda técnica/conceptual?** Baja. Vigilar: (a) que los breakpoints vivan como tokens, no
   constantes mágicas; (b) que "sugerir comparación" no derive en un patrón intrusivo con el
   tiempo (debe seguir siendo discreto y descartable).
9. **¿Decisiones a revisar antes de continuar?** Ninguna pendiente: D1 y D2 quedan **cerradas**.
   Revisar empíricamente los números de DC-D2 cuando exista el primer Lab en uso real intensivo.
10. **¿Canónico?** **Sí, plenamente canónico.** Cierra las dos notas abiertas de U2 sin dejar
    nuevas.

---

# Impacto en el Futuro

- **Decisiones futuras condicionadas:** el Sistema de Interacción (U-Fase 5) heredará DC-D1
  como comando explícito (con su atajo) y DC-D2 como contrato de estados de ventana; el Diseño
  Visual (U-Fase 6) deberá diseñar el *aviso honesto* y la *sugerencia discreta* como piezas de
  primera clase, no como afterthoughts.
- **Módulos afectados:** `studio-web` (implementa breakpoints, modo comparación y aviso);
  `StudioLab` (declara si un Lab admite comparación y si es temporal → afecta colapso de
  Timeline); `studio-preservation` (memoria de layout y de estado de modo entre sesiones).
- **Oportunidades habilitadas:** comparación como rasgo reutilizable y predecible en todos los
  Labs; una política de tamaño que permite optimizar el diseño para un rango acotado (mejor
  composición al no perseguir el móvil); base sólida para presets de workspace (W20).
- **Limitaciones introducidas:** el Studio **renuncia explícitamente** a pantallas pequeñas/
  móviles (decisión deliberada, no accidental); la comparación queda **acotada a 2** (si algún
  día se necesitara comparar 3+, sería una decisión nueva que revisar, no un ajuste menor).
- **Riesgos a vigilar:** (R1) *scope creep de la sugerencia de comparación* hacia algo
  intrusivo — mantener discreta. (R2) *breakpoints endurecidos como constantes* — tokens.
  (R3) *presión para soportar tablets/móvil* en el futuro — cualquier cambio de DC-D2 debe
  pasar por Auditoría de Coherencia, no colarse como "arreglo responsive".

---

# ¿A la altura de un producto AAA?

> *¿Estas decisiones estarían a la altura de Figma, Unreal Engine, JetBrains IDE, VS Code o
> Unity Editor?* (Norma de estándar de calidad, a partir de ahora obligatoria.)

**Sí.** Justificación, no autocomplacencia:
- **DC-D1** replica el estándar de facto de esas herramientas: el *split/compare view* es
  siempre un **modo explícito** que el usuario activa (Figma comparar, DevTools split, editores
  con vista dividida), **nunca** un auto-split por tamaño de ventana. Además añade valor propio:
  composición interna adaptativa + sugerencia contextual descartable.
- **DC-D2** replica su enfoque **desktop-first con mínimos declarados**: ninguna de esas
  herramientas finge ser responsive móvil; todas asumen escritorio y degradan con dignidad
  hasta un piso. Nuestra política es incluso más explícita (niveles nombrados + aviso honesto),
  lo que la hace *más* auditable que la media.

**Qué faltaría para el nivel máximo (a resolver en fases visuales, no aquí):** el *cómo se ve y
se siente* el aviso honesto y la sugerencia (microcopy, timing, que no molesten) — eso pertenece
al Sistema de Interacción y al Diseño Visual. A nivel de **política**, estas decisiones ya están
al nivel de esas herramientas.
