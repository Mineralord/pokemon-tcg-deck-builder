# Visual Tokens — Arquitectura del Sistema (Fase 9)

**Este documento define la ARQUITECTURA del sistema de design tokens del Pokémon TCG Developer
Studio: sus capas, sus familias, sus reglas de dependencia, evolución y nomenclatura.** No define
ningún valor concreto: sin HEX, sin fuentes, sin tamaños, sin duraciones, sin curvas, sin radios, sin
sombras. Esos valores pertenecen a las subfases 9.1–9.5.

**Análisis integrado por dos lentes** (activas, una sola propuesta): `ui-ux-pro-max` (jerarquía de
capas, consistencia, accesibilidad, escalabilidad, nomenclatura) + `emil-design-eng` (familias de
motion/easing/duration, buenos defaults, "los detalles invisibles se suman", cohesión). **Conflicto
resuelto:** UI/UX empuja hacia máxima estructura (muchas capas, gran flexibilidad); Emil empuja hacia
menos opciones y mejores defaults. La arquitectura resultante adopta **el mínimo número de capas que
la gramática exige** y prohíbe capas "porque la industria las tiene" (prueba de pertenencia, pilar 2).

**Principio rector (TA-0):** *un token no existe porque los Design Systems lo tengan; existe porque
implementa una ley del Visual Grammar o del Visual Language.* Todo token debe poder citar la ley que
materializa. Un token sin ley detrás es un anti-patrón (hereda VG-0: nada decora).

**Canon obligatorio heredado:** Filosofía · 4 pilares · P1–P15 · Arq. Espacial (Z1–Z12) · Workflows ·
Layout (DC-D1/DC-D2) · Modelo Mental · Interaction Canon (IC-0…IC-13) · PDS (F-VIS: nada se hornea,
todo por token) · Visual Language (VL-*) · Visual Grammar (VG-*).

---

## Parte I — ARQUITECTURA DE TOKENS (visión)

Un design token es un **nombre estable con significado, cuyo valor puede cambiar sin cambiar el
nombre.** Esa es su razón de ser: desacoplar *qué significa algo* de *qué valor tiene ahora mismo*.
La arquitectura organiza esos nombres en **capas por nivel de abstracción**, con una única dirección
de dependencia (de lo concreto a lo abstracto NO; siempre de lo abstracto a lo primitivo). Esto es lo
que permite re-tematizar (Studio ↔ launcher ↔ visor) y evolucionar durante décadas sin reescribir la
interfaz (cumple la promesa reservada en PDS Parte I y VL-EVO-3).

**Las tres preguntas que la arquitectura debe responder siempre:**
1. *¿Qué es este valor?* → capa primitiva (Foundation).
2. *¿Para qué sirve?* → capa de propósito (Semantic).
3. *¿Dónde se usa exactamente?* → capa de componente (Component), solo si aporta.

---

## Parte II — JERARQUÍA CANÓNICA DE CAPAS

Se evalúan los siete niveles propuestos. **Se adoptan tres capas nucleares + una cuarta condicionada;
se rechazan tres.** Cada decisión se justifica.

### ✅ Capa 1 · Foundation Tokens (primitivos) — ADOPTADA

- **Propósito:** el inventario crudo de valores posibles del sistema (la "paleta" en sentido amplio:
  todos los peldaños de color, escalones de espacio, escalones tipográficos, etc.). Sin semántica de
  uso: un Foundation token no sabe *para qué* sirve.
- **Responsabilidades:** ser la **única fuente de valores primitivos**; garantizar que existan como
  escalas coherentes (rampas, series), no como números sueltos.
- **Alcance:** global al ecosistema PTCG. Idéntico entre Studio, launcher y visor.
- **Dependencia:** no depende de nadie. Es la raíz del grafo.
- **Evolución:** se **añaden** peldaños por extensión; jamás se cambia el significado de un peldaño
  existente. Ampliar una rampa es seguro; redefinir un peldaño es ruptura (versión mayor).
- **Reutilización:** los primitivos **no se usan directamente en componentes** (regla dura, ver
  Reglas de Dependencia). Solo los consume la capa Semantic.
- **Nombrado:** descriptivo-neutral por escala y peldaño (p. ej. familia + escalón ordinal), **nunca**
  por uso ni por moda. El nombre describe *qué es*, no *para qué*.
- **Problemas que resuelve:** valores mágicos dispersos; inconsistencia de escalas; imposibilidad de
  re-tematizar.
- **Problemas que evita:** que un componente dependa de un HEX concreto (VG/VL prohíben hornear).
- **Ley que implementa:** VG-U3 (familia finita de distancias), VG-K (contraste como sistema), VL-EVO
  (identidad por leyes/valores separados), F-VIS (PDS).

### ✅ Capa 2 · Semantic Tokens (propósito) — ADOPTADA (capa central del sistema)

- **Propósito:** dar **significado de uso** a los primitivos. Es la capa donde vive la gramática: aquí
  un peldaño de color se convierte en "superficie de reposo", "foco", "alerta"; un escalón de espacio
  se convierte en "dentro-de-grupo" / "entre-grupos". **Es la capa que consumen los componentes.**
- **Responsabilidades:** expresar cada ley del Visual Grammar como un nombre estable; ser el **único
  punto de contacto** entre valores y componentes.
- **Alcance:** global, pero puede re-mapearse por Theme (ver capa condicionada). El significado es
  invariante; el primitivo al que apunta puede cambiar.
- **Dependencia:** depende **solo** de Foundation. Nunca de Component.
- **Evolución:** se añaden significados nuevos por extensión; un significado existente no cambia de
  intención (puede cambiar el primitivo que referencia). Añadir "superficie-elevada-persistente" es
  seguro; redefinir qué significa "foco" es ruptura.
- **Reutilización:** máxima. Un mismo semantic token sirve a decenas de componentes (VG-C1: misma
  relación ⇒ mismos recursos).
- **Nombrado:** **semántico por rol**, estructurado por *categoría → rol → estado/variante*. Nunca por
  apariencia ("azul", "grande") ni por componente ("botón"). Ejemplos de forma (sin valores):
  `superficie/reposo`, `texto/foco`, `contraste/alerta`, `espacio/entre-grupos`, `plano/interrupcion`.
- **Problemas que resuelve:** acoplar la UI a valores; imposibilidad de razonar el diseño; deriva de
  significado entre pantallas.
- **Problemas que evita:** que dos componentes expresen la misma relación con recursos distintos
  (rompería VG-C1).
- **Ley que implementa:** **toda** la Parte I del Visual Grammar (el diccionario de significados) y el
  silencio de 4 modos + protagonismo del Visual Language.

### ✅ Capa 3 · Component Tokens — ADOPTADA con restricción fuerte

- **Propósito:** exponer los puntos de estilo *propios de un componente del PDS* cuando —y solo
  cuando— ese componente necesita un ajuste que el semantic no cubre sin ambigüedad.
- **Responsabilidades:** dar un punto de anclaje estable por componente (p. ej. "padding interno de la
  Resource Card") que **siempre referencia un semantic token**, nunca un primitivo ni un literal.
- **Alcance:** local al componente.
- **Dependencia:** depende **solo** de Semantic. Es una hoja del grafo.
- **Evolución / restricción dura (TA-COMP):** un Component token **solo se crea si su ausencia obliga
  a repetir la misma decisión en varios sitios o a romper el semantic**. Por defecto **no se crea**:
  la mayoría de componentes deben vestirse solo con semantics. Esto evita la explosión de tokens que
  degrada los sistemas grandes (miles de `button-primary-hover-bg-…`).
- **Reutilización:** baja por diseño (son locales). No se reutilizan entre componentes; si dos los
  necesitan iguales, el valor vive en Semantic, no aquí (hereda F-REUSE).
- **Nombrado:** `componente/propiedad/estado`, siempre apuntando a un semantic.
- **Problemas que resuelve:** ajustes finos legítimos sin ensuciar la capa semantic.
- **Problemas que evita:** explosión combinatoria de tokens; acoplar componentes entre sí.
- **Ley que implementa:** F-COMP/F-REUSE (PDS), VG-X (excepción con propósito: un component token *es*
  una excepción reglada al uso directo del semantic).

### ⚠️ Capa 4 · Theme Tokens — ADOPTADA como MECANISMO, no como capa nueva

- **Decisión:** *no es una cuarta capa de tokens*, sino una **dimensión de la capa Semantic**: un
  Theme es un **conjunto alternativo de mapeos Semantic→Foundation**. "Studio", "launcher" y "visor"
  son themes; también lo son claro/oscuro si el proyecto los adopta.
- **Propósito:** permitir que el *mismo* semantic (`superficie/reposo`) apunte a distintos primitivos
  según contexto de producto, **sin duplicar componentes** (cumple la promesa de PDS Parte I).
- **Alcance:** todo el ecosistema; se selecciona uno activo por Workspace/superficie.
- **Dependencia:** el theme reasigna el borde Semantic→Foundation; no introduce dependencias nuevas.
- **Evolución:** se añaden themes por extensión; los nombres semánticos son idénticos entre themes
  (invariante). Un theme nuevo NO puede inventar semantics propios (rompería continuidad).
- **Nombrado:** `theme/<nombre>` como espacio de resolución, no como prefijo de cada token.
- **Ley que implementa:** VL-EVO-3 (re-tema libre, gramática fija), Especialización Absoluta
  (reutilización interna del ecosistema).

### ❌ Context Tokens — RECHAZADA (absorbida por Semantic + Density)

- **Motivo:** el "contexto" (p. ej. densidad de un Lab, zona compacta) **no necesita una capa
  propia**: se modela como (a) *variantes de estado* dentro de Semantic y (b) la familia **Density**
  (abajo). Una capa Context separada duplicaría significado y multiplicaría rutas de resolución,
  violando la sobriedad (Emil: menos opciones, mejores defaults) y VG-L2 (un signo, un significado).
- **Riesgo evitado:** grafos de resolución de tokens impredecibles y difíciles de razonar a 20 años.

### ❌ Alias Tokens — RECHAZADA como capa (permitida como utilidad puntual)

- **Motivo:** un alias genérico ("este token = aquel token") es exactamente lo que ya hace el borde
  Semantic→Foundation. Una **capa** de alias añade indirección sin significado nuevo y facilita
  cadenas de referencia opacas (`a→b→c→d`) que nadie entiende en el futuro. Se **prohíbe como capa**.
  Se permite, excepcionalmente y documentado, un alias de *migración temporal* (renombrar un semantic
  manteniendo el viejo apuntando al nuevo durante una transición), que debe eliminarse al cerrar la
  migración (hereda VG-X2: la excepción crónica se canoniza o se retira).
- **Ley que implementa la prohibición:** VG-L2, TA-0 (sin significado nuevo, no hay token).

### ❌ Runtime Tokens — RECHAZADA como capa de diseño

- **Motivo:** los valores que cambian en tiempo de ejecución (posición de arrastre, progreso, tamaño
  medido) **no son tokens de diseño**: son *estado de la aplicación*. Modelarlos como tokens
  contaminaría el sistema y chocaría con el rendimiento (Emil: no mutar variables CSS heredables en
  caliente; escribir el transform directo). El sistema de tokens describe **decisiones de diseño
  estables**, no estado efímero.
- **Matiz:** la única "runtime-ness" legítima es *seleccionar el theme activo* y *responder a
  `prefers-reduced-motion` / densidad elegida*, que se resuelven cambiando el conjunto Semantic
  activo, no creando tokens de runtime.
- **Ley que implementa la prohibición:** Regla de Oro (sin lógica de juego/estado en la capa
  equivocada), separación de responsabilidades.

### Resumen de la jerarquía adoptada

```
Foundation (primitivos, sin uso)
    ↓  (solo Semantic puede leer Foundation)
Semantic (propósito = gramática; consumido por componentes)
    ├─ dimensión Theme  (Semantic→Foundation alternativo; mismos nombres)
    ├─ variantes de Estado (Interaction States)
    └─ variantes de Density
    ↓  (solo Component puede leer Semantic)
Component (local, excepcional, siempre apunta a Semantic)
```

Rechazadas: Context (→ Semantic+Density), Alias (→ es el borde Semantic→Foundation), Runtime
(→ estado de app, no diseño).

---

## Parte III — FAMILIAS DE TOKENS

Cada familia se justifica por la ley que implementa. Solo arquitectura: qué es la familia, qué
estructura tiene, en qué capa vive; **sin valores**.

| Familia | Qué modela | Capa raíz | Ley VG/VL que implementa | Decisión |
|---|---|---|---|---|
| **Color** | Rampas de valor lumínico/cromático | Foundation→Semantic | VG-K (contraste=rol), VL-Luz, silencio 4 modos | ✅ |
| **Typography** | Escala tipográfica (roles de texto) | Foundation→Semantic | VG-J (jerarquía), VL-fatiga (lectura prolongada) | ✅ |
| **Spacing** | Familia finita de distancias con significado | Foundation→Semantic | VG-U3, VG-E (proximidad/separación), ritmo | ✅ |
| **Radius** | Grado de suavizado de bordes | Foundation→Semantic | VG-CONTENEDOR (borde=límite de dominio) | ✅ |
| **Border** | Presencia/peso del límite | Foundation→Semantic | VG "borde=frontera", agrupación mínima (VG-E) | ✅ |
| **Opacity** | Grado de presencia/atenuación | Foundation→Semantic | VL-silencio (Desaparecer/Neutro), VG-K | ✅ |
| **Elevation** | Señal visual de estar "encima" | Semantic | VG-PROF (profundidad=interrupción) | ✅ (deriva de Depth) |
| **Depth** | Orden semántico de planos | Semantic | VG-D (escala de planos corta y fija) | ✅ (rige a Elevation y Z-Index) |
| **Motion** | Repertorio de transiciones con frase | Semantic | VG-MOV (5 frases), VL-movimiento | ✅ |
| **Duration** | Escala de tiempos | Foundation→Semantic | VG-MOV, framework de decisión (frecuencia) | ✅ |
| **Easing** | Repertorio de curvas | Foundation→Semantic | VG-MOV (continuidad/cambio/…), Emil easing | ✅ |
| **Z-Index** | Orden de apilamiento técnico | Semantic (derivado de Depth) | VG-D (misma escala de planos) | ✅ (subordinado a Depth) |
| **Blur** | Difuminado (máscara de transición / fondo) | Foundation→Semantic | VG-MOV (masking), VL-Luz difusa | ✅ (uso restringido) |
| **Icon Size** | Escala de tamaño de icono | Foundation→Semantic | VG-J (jerarquía), consistencia | ✅ |
| **Stroke** | Grosor de trazo (iconos/líneas) | Foundation→Semantic | VG-K (peso=rol), consistencia icónica | ✅ |
| **Grid** | Retícula subyacente | Foundation→Semantic | VG-U4 (alineación/retícula), estructura | ✅ |
| **Density** | Compacidad de una zona/Lab | Semantic (modificador) | VL-densidad organizada, VG-E (ritmo) | ✅ (modificador, no capa) |
| **Interaction States** | Reposo/hover/foco/activo/selección/deshab. | Semantic (variantes) | IC-1/IC-4/IC-5, VG-K, VL-silencio | ✅ |
| **Breakpoints** | Umbrales de ancho de layout | Foundation | DC-D2 (tamaño mínimo soportado) | ⚠️ mínimo |

**Notas de arquitectura por familia (las relevantes):**

- **Depth es la familia rectora de la tercera dimensión.** Elevation (señal visual) y Z-Index (orden
  técnico de apilamiento) **no son familias independientes**: ambas *derivan* de la escala de planos
  de Depth (VG-D2: base → elevado-persistente → interrupción → alerta). Esto impide el clásico caos de
  `z-index: 9999`: no hay z-index libres, solo planos semánticos.
- **Motion, Duration y Easing son tres familias separadas pero acopladas** por la capa Semantic: un
  *motion semántico* (p. ej. "entrada de overlay") referencia un duration y un easing de las escalas
  Foundation. Nunca se escribe una duración/curva suelta en un componente (hereda F-VIS). Las cinco
  frases del movimiento (VG-MOV) son los *roles* semánticos de la familia Motion.
- **Interaction States** no es una familia de valores nuevos: es un **eje de variación** que se aplica
  sobre Color/Opacity/Elevation/Motion semánticos. Un estado (hover, foco, activo, selección,
  deshabilitado) mapea a IC-1/IC-4/IC-5 y a VG-K (el estado cambia el rol/importancia).
- **Density** es un **modificador** que reescala Spacing (y quizá Typography/Icon Size) para zonas
  compactas vs. cómodas, sin duplicar la escala base. Implementa "densidad alta pero organizada" (VL).
- **Breakpoints — adopción mínima:** el Studio es una herramienta de escritorio (Desarrollo
  Unipersonal, no app móvil). No se adopta un sistema responsive de producto de consumo. Solo se
  declaran los **umbrales necesarios para respetar el tamaño mínimo soportado (DC-D2)** y la
  degradación honesta (P15). Nada de rampas de breakpoints "por si acaso".
- **Blur — uso restringido:** solo dos usos legítimos: (a) máscara de transición imperfecta (técnica
  de Emil) y (b) coherencia con la luz difusa (VL-Luz). Prohibido como decoración de fondo ("glass"
  gratuito) porque violaría VG-0 y la sobriedad del chasis.
- **Color y Typography** solo fijan aquí su *estructura* (qué roles semánticos existen: superficie,
  texto, foco, alerta / display, cuerpo, dato, código…). Sus valores llegan en 9.1 y 9.2.

---

## Parte IV — REGLAS DE DEPENDENCIA

- **TD-1 · Dirección única.** El grafo de dependencias fluye en un solo sentido:
  `Component → Semantic → Foundation`. Nunca al revés, nunca en salto.
- **TD-2 · Prohibido saltar capas.** Un componente **no** puede leer un Foundation token directamente;
  debe pasar por un Semantic. Un Semantic no puede depender de un Component.
- **TD-3 · Foundation es acíclico y hoja-raíz.** No referencia a nadie; no contiene semántica de uso.
- **TD-4 · Sin cadenas de alias.** Máxima profundidad de referencia: Component→Semantic→Foundation
  (dos saltos). Prohibidas las cadenas `a→b→c→d` (no hay capa Alias).
- **TD-5 · Theme reasigna solo el borde Semantic→Foundation.** No añade profundidad ni nombres nuevos.
- **TD-6 · Depth gobierna Elevation y Z-Index.** Ningún z-index o elevación se define fuera de la
  escala de planos de Depth.
- **TD-7 · Estado y Density son ejes, no capas.** Se aplican como variantes sobre Semantic; no crean
  una jerarquía paralela.

---

## Parte V — REGLAS DE EVOLUCIÓN

- **TE-1 · Extensión, no mutación.** Se añaden peldaños/roles/themes; no se redefine el significado de
  uno existente (hereda F-EVO, VL-EVO-2, VG-EVO-3).
- **TE-2 · El nombre es un contrato.** Una vez publicado un nombre de token, su *significado* es
  inmutable; solo su *valor* puede cambiar. Cambiar el significado = versión mayor del sistema.
- **TE-3 · Deprecación reglada.** Un token que sobra se marca deprecado, se mantiene apuntando a su
  sucesor durante una migración acotada, y se elimina al cerrarla (VG-X2).
- **TE-4 · Un valor cambia en un solo lugar.** Gracias a la dirección única, cambiar un primitivo
  propaga a todo su uso; nunca se parchea en el componente.
- **TE-5 · Los themes no divergen en nombres.** Añadir un theme jamás introduce semantics propios;
  todos los themes comparten el mismo vocabulario semántico (continuidad, VG-C1).
- **TE-6 · Prueba de Gramática obligatoria.** Ningún token nuevo se canoniza sin pasar la Prueba de
  Gramática Visual (Fase 8) y la Prueba de Identidad Visual (Fase 7).

---

## Parte VI — REGLAS DE NOMENCLATURA

Objetivo: que un nombre siga siendo comprensible dentro de treinta años. **Nombres semánticos, nunca
ligados a apariencia ni a modas.**

- **TN-1 · Estructura canónica:** `capa · categoría · rol · [variante] · [estado]`, en minúsculas,
  separadas por un delimitador único y estable (p. ej. `/` o `.`, se fija en 9.1). Ejemplos de *forma*
  (sin valores):
  - Foundation: `color/neutral/10`, `space/6`, `duration/3`, `easing/standard` — describen *qué es*.
  - Semantic: `color.superficie.reposo`, `color.texto.foco`, `color.contraste.alerta`,
    `space.entre-grupos`, `depth.interrupcion`, `motion.overlay.entrada`, `state.hover`.
  - Component: `resource-card.padding`, `inspector.row.gap` (siempre apuntando a un semantic).
- **TN-2 · Prohibido nombrar por apariencia.** Nunca `azul`, `rojo`, `grande`, `sombra-fuerte`. El
  color/tamaño puede cambiar; el rol no. (Un `color.texto.alerta` puede dejar de ser rojo sin renombrar.)
- **TN-3 · Prohibido nombrar por moda o marca temporal.** Nada de `neo-`, `glass-`, `2025-`, nombres de
  tendencias. Violarían VG-EVO-1 (leyes, no modas).
- **TN-4 · Prohibido nombrar Semantic por componente.** Un semantic es transversal; `button-bg` como
  semantic está prohibido (eso, si acaso, es Component token).
- **TN-5 · Vocabulario alineado al canon.** Los roles usan el idioma del Visual Grammar/Language
  (superficie, foco, reposo, alerta, interrupción, entre-grupos…). Un concepto = una palabra
  (Auditoría Semántica, Modelo Mental). Se elige un idioma único y se mantiene (coherencia con el
  resto de la doc en español; el término técnico puede quedar en inglés si ya es canon —"foco" vs
  "focus"— se resuelve en 9.1, aquí se fija la *regla*, no el idioma).
- **TN-6 · Escalas ordinales, no adjetivos.** Los peldaños Foundation se numeran por escala ordinal
  (…/10/20/…) en lugar de `sm/md/lg/xl`, que se agotan y se vuelven ambiguos al crecer. Los adjetivos
  de talla, si se usan, viven solo en Semantic con significado de rol, no de tamaño.
- **TN-7 · Estados con nombres canónicos fijos:** `reposo`, `hover`, `foco`, `activo`, `seleccion`,
  `deshabilitado` (alineados a IC). No se inventan sinónimos.

---

## Parte VII — TOKENS PROHIBIDOS

- **Token sin ley detrás** — cualquier token que no cite una ley del Visual Grammar/Language (TA-0).
- **Token con valor horneado** — un componente con un literal visual en vez de un token (viola F-VIS).
- **Token nombrado por apariencia** (`azul-500` como semantic) o **por moda** (`glass-*`, `neo-*`).
- **Token de salto de capa** — un componente que lee Foundation directamente (viola TD-2).
- **Cadena de alias** — indirección `a→b→c→d` sin significado nuevo (no hay capa Alias).
- **Z-index libre** — cualquier valor de apilamiento fuera de la escala de Depth (viola TD-6).
- **Runtime token de diseño** — modelar estado efímero de la app como token de diseño.
- **Context token** — capa de contexto separada (se modela con Semantic+Density).
- **Breakpoint especulativo** — umbrales responsive de producto de consumo no justificados por DC-D2.
- **Blur decorativo** — difuminado de fondo sin función (viola VG-0 y sobriedad del chasis).
- **Duración/curva suelta** — motion escrito en un componente sin pasar por las familias
  Motion/Duration/Easing.

---

## Parte VIII — DECISIONES CANÓNICAS

- **DC-TA1 —** Arquitectura de **tres capas nucleares** (Foundation → Semantic → Component) con
  dirección de dependencia única e irrompible.
- **DC-TA2 —** **Semantic es la capa central**: encarna el diccionario del Visual Grammar y es el único
  punto de contacto de los componentes.
- **DC-TA3 —** **Theme = dimensión de Semantic**, no una capa nueva; mismos nombres semánticos entre
  Studio/launcher/visor (y claro/oscuro si se adoptan).
- **DC-TA4 —** **Se rechazan** las capas Context, Alias y Runtime, con justificación (absorción,
  redundancia, y "es estado de app", respectivamente).
- **DC-TA5 —** **Depth rige Elevation y Z-Index**; no existen z-index libres.
- **DC-TA6 —** **Motion/Duration/Easing** son familias separadas acopladas por Semantic; las cinco
  frases (VG-MOV) son sus roles.
- **DC-TA7 —** **Estado y Density son ejes de variación** sobre Semantic, no jerarquías paralelas.
- **DC-TA8 —** **Nomenclatura semántica y ordinal**, prohibidos los nombres por apariencia y por moda;
  el nombre es un contrato de significado inmutable.
- **DC-TA9 —** **Component tokens por excepción**: por defecto no se crean; solo cuando su ausencia
  obliga a repetir una decisión.
- **DC-TA10 —** Todo token nuevo pasa la **Prueba de Gramática Visual** y la **Prueba de Identidad
  Visual** antes de canonizarse.

---

## Parte IX — Plan de subfases (materialización de valores)

Esta fase cierra la arquitectura. Los **valores concretos** se asignan después, una familia por
subfase, cada una validada contra ambas Pruebas y sin contradecir esta arquitectura:

- **9.1 — Color** (rampas Foundation + roles Semantic + themes).
- **9.2 — Typography** (escala + roles de texto).
- **9.3 — Spacing** (familia de distancias + Grid + Density).
- **9.4 — Elevation y Depth** (escala de planos, Elevation, Z-Index, Radius/Border/Blur asociados).
- **9.5 — Motion** (Duration, Easing, roles de Motion = 5 frases).

---

# Auditoría Semántica

Contra `MENTAL-MODEL.md`, Visual Language y Visual Grammar:

- **Capas** (Foundation/Semantic/Component/Theme) usan nombres estándar de la disciplina con
  significado único y explícito; no colisionan con el Modelo Mental de dominio (Project/Lab/Resource…).
- **Roles semánticos** reutilizan el idioma del Visual Grammar (superficie, foco, reposo, alerta,
  interrupción, entre-grupos, plano): **un concepto = una palabra**, sin sinónimos. La familia Depth
  absorbe "elevation" y "z-index" como derivados, evitando tres palabras para una idea.
- **"Tarjeta/Card" vs "carta Pokémon":** la disciplina ortográfica de las fases 7–8 se mantiene; los
  component tokens de `resource-card` se refieren al componente, no al contenido de dominio.
- **Riesgo de idioma** (español del canon vs. términos técnicos en inglés) señalado y **diferido a
  9.1** como decisión de nomenclatura concreta; aquí se fija la regla (TN-5), no el idioma.

**Veredicto: sin ambigüedad ni duplicación. Aprobada.**

---

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No. Refuerza consistencia, accesibilidad (estados y contraste como
   semantics), productividad (Density) y P15 (Breakpoints honestos, sin responsive especulativo).
2. **¿Contradice la Arquitectura Espacial / Layout?** No. Grid y Density se apoyan en Z1–Z12 y en
   DC-D2 (tamaño mínimo); no reubican zonas.
3. **¿Contradice el Interaction Canon?** No. Interaction States mapea 1:1 a IC-1/IC-4/IC-5.
4. **¿Contradice el PDS (Fase 6)?** No: **cumple** su reserva de la capa de tokens (F-VIS) y su
   promesa de re-tematización sin duplicar componentes (Parte I del PDS).
5. **¿Contradice Visual Language / Visual Grammar?** No: cada familia cita la ley que implementa
   (Parte III); TA-0 es la aplicación directa de VG-0.
6. **¿Introduce excepciones/duplicación innecesarias?** No: rechaza tres capas precisamente para
   evitar redundancia; los Component tokens son excepción reglada (VG-X).
7. **¿Respeta la Regla de Oro?** Sí: los Runtime tokens se rechazan por ser estado de app, no diseño.
8. **¿Escala a cientos de Labs y décadas?** Sí: dirección única, extensión-no-mutación, nombre como
   contrato.
9. **¿Genera deuda?** No; la previene (evita explosión de tokens, z-index libres, cadenas de alias).
10. **¿Canónico?** Sí.

**Veredicto: Canónico.**

---

# Auditoría de Filosofía

Contra `PRINCIPIO-DESARROLLO-UNIPERSONAL.md`:

1. **¿Respeta el Desarrollo Unipersonal?** Sí. La arquitectura minimiza capas y opciones para que un
   único desarrollador la sostenga mentalmente; rechaza complejidad especulativa (Context/Alias/
   Runtime/Breakpoints).
2. **¿Introduce complejidad multi-desarrollador?** No. No hay gobernanza de equipos, ni namespaces por
   autor, ni themes por usuario.
3. **¿Puede simplificarse por ser un solo desarrollador?** Ya se hizo: tres capas, un theme activo por
   superficie, defaults fuertes (Emil) sobre exceso de configurabilidad.
4. **¿Las IA siguen siendo herramientas, no actores?** Sí. Los tokens describen decisiones de diseño;
   ninguna IA es un "actor" con theme o namespace propio.
5. **¿Simplicidad, preservación, evolución a largo plazo?** Es la tesis del documento (Partes V y VI).

**Veredicto: alineado con los cuatro pilares.**

---

# Impacto en el Futuro

- **Condiciona 9.1–9.5:** cada subfase rellena valores dentro de esta estructura sin poder añadir
  capas ni familias nuevas sin revisión mayor. La escala de Depth (9.4) predetermina Elevation y
  Z-Index; las cinco frases (9.5) predeterminan los roles de Motion.
- **Condiciona Fase 10 (Component Styling):** los componentes se vestirán *solo* con semantics
  (component tokens por excepción); esto disciplina el estilado y evita la deriva.
- **Condiciona Fase 11 (Screen Design):** las pantallas heredan themes y density sin tocar valores.
- **Oportunidades:** re-tematización Studio↔launcher↔visor sin reescritura; theming claro/oscuro si se
  decide; verificabilidad (cada token cita su ley); onboarding rápido por nomenclatura semántica.
- **Limitaciones deliberadas:** menos flexibilidad "libre" (no hay z-index sueltos, no hay tokens de
  runtime); coste asumido a cambio de coherencia de décadas.
- **Riesgos a vigilar:** (a) presión por crear Component tokens de más → aplicar TA-COMP con dureza;
  (b) valores en 9.x que traicionen una ley (contraste alto por defecto, motion decorativo); (c)
  tentación de reintroducir Alias/Context al crecer → recordar la justificación de rechazo; (d) deriva
  de idioma en la nomenclatura → cerrar TN-5 en 9.1.

---

# ¿A la altura de un producto AAA?

*¿Estaría esta arquitectura a la altura de Figma / Unreal / JetBrains / VS Code / Unity?*

Sí. Los sistemas de tokens de referencia (Material 3, Primer, Polaris, Fluent) convergen exactamente
en el modelo de tres capas Foundation→Semantic→Component con theming sobre la capa semántica; esta
arquitectura lo adopta y además **endurece** dos puntos donde esos sistemas suelen degradarse con el
tiempo: la **explosión de Component tokens** (aquí, excepción reglada por TA-COMP) y el **caos de
z-index** (aquí, subordinado a una escala de Depth semántica). El rasgo diferencial y defendible es
**TA-0**: ningún token existe sin citar la ley que implementa, lo que convierte el sistema en
verificable en lugar de acumulativo. Lo que aún **falta** para alcanzar el estándar es, legítimamente,
la materialización de valores (9.1–9.5) y su validación empírica de contraste/accesibilidad y feel.
Como *arquitectura*, está a la altura AAA; su *realización* llega en las subfases. **Aprobada como
canon.**

---

# Anexo 9.2 — Typography (materialización de valores)

*No modifica ninguna decisión de arquitectura de este documento; materializa la familia Typography
del ecosistema Pokémon TCG (Studio, Launcher, Card Viewer, herramientas) dentro de la capa
`Foundation → Semantic(role)` ya definida. Artefactos: `tokens/tokens.typography.json` (fuente de
verdad) y `core/designsystem/.../tokens/StudioTypographyTokens.kt` (Compose, compila).*

## Familias tipográficas (2)
- **Sans** (intención: **Inter**; fallback `sans-serif`): todo el texto de UI y lectura. Grotesca
  neutra, altamente legible en tamaños pequeños de escritorio; sin personalidad de moda (VG-EVO-1).
- **Mono** (intención: **JetBrains Mono**; fallback `monospace`): código/DSL/JSON y **datos numéricos**
  (garantiza alineación de columnas — VG-U4). Dos familias y no más: menos es más silencio (VL).

> Las familias se declaran por intención; hasta añadir los `.ttf` se resuelven con fallbacks del
> sistema. Cambiar a las fuentes definitivas es una sola edición en `Foundation.Family` (honestidad P15).

## Escala (Foundation.size, sp)
Modular anclada en **base = 13sp**, ratio ≈1.2, **redondeada a sp enteros**: una escala modular
estricta produce medios píxeles borrosos en UI densa de escritorio; se redondea para nitidez (Emil).
`11 · 12 · 13(base) · 15 · 18 · 22 · 28 · 36(reservado)`. Base 13 (no 16) por ser herramienta densa de
escritorio multi-monitor, no web móvil; el reposo lo da el interlineado y el contraste reservado, no
un cuerpo grande.

## Pesos (3): Regular 400 · Medium 500 · SemiBold 600
Sin Light (fatiga/legibilidad) ni Bold 700 por defecto (rompería el reposo del chasis; reservado).

## Roles semánticos (12) — cada uno justifica su existencia
| Rol | Familia · Peso · Tamaño/lh · Tracking | Existe porque… (ley) |
|---|---|---|---|
| **Display** | Sans SemiBold 28/34 −0.4 | encabezados grandes de estados vacíos/bienvenida; raro (VG-K5). |
| **Heading** | Sans SemiBold 22/28 −0.2 | primer nivel de lectura de una pantalla (VG-J). |
| **Title** | Sans SemiBold 18/24 −0.2 | títulos de Panel/Dialog (VG-J · materiales VL). |
| **Section** | Sans Medium 15/20 0 | subsecciones dentro de un panel (VG-J). |
| **Body** | Sans Regular 13/20 0 | lectura primaria; lh 1.5 para jornadas largas (VL-fatiga). |
| **Caption** | Sans Regular 12/16 +0.1 | ayuda/apoyo terciario (VG-J). |
| **Label** | Sans Medium 12/16 +0.4 | nombres de propiedad del Inspector, etiquetas de formulario. |
| **Mono** | Mono Regular 13/20 0 | código, DSL de efectos, JSON (legibilidad monoespaciada). |
| **Numeric** | Mono Regular 13/20 0 | datos numéricos alineados en tablas/Inspector (VG-U4). |
| **Shortcut** | Mono Medium 12/16 0 | pistas de atajo de teclado (A7 · IC-5). |
| **Badge** | Sans SemiBold 11/14 +0.2 | contador/estado sobre un objeto (A2). |
| **Status** | Sans Medium 12/16 +0.2 | segmentos de Status Bar / notificaciones (IC-8). |

## Reglas de lectura, datos, código, tablas y por contexto
- **Lectura:** cuerpo = Body; interlineado 1.5; longitud de línea objetivo ≈ 65–75 car. (se controla en
  Layout/Screen, no aquí). Tracking 0 en cuerpo; negativo solo en tamaños grandes (Display/Heading).
- **Datos:** Numeric (Mono) para toda cifra tabulada → columnas alineadas (VG-U4). Texto de un campo de
  datos no numérico = Body/Label según sea valor o nombre.
- **Código / DSL / JSON:** Mono. Nunca Sans para código (rompería alineación y expectativa de dev).
- **Tablas:** cabecera = Label; celdas de texto = Body; celdas numéricas = Numeric; densidad por
  Density (9.3), no cambiando de rol.
- **Títulos:** Heading (pantalla) → Title (panel/dialog) → Section (subsección). Una sola jerarquía.
- **Inspector:** nombre de propiedad = Label; valor editable = Body; valor numérico = Numeric.
- **Preview:** el chasis usa Caption/Status como mucho; el protagonismo es del contenido (VL-CONT); sin
  tipografía decorativa que compita con la carta.
- **Diálogos:** título = Title; cuerpo = Body; acciones = Label/Body (según el estilado de botón, 9.10).
- **Overlays / Command Palette:** ítems = Body; atajos = Shortcut; agrupadores = Label.
- **Status:** Status Bar = Status; Badge para contadores.
- **Notificaciones / Toast:** título = Label/Section; cuerpo = Caption/Body; sin superar a Title.

## Decisiones canónicas (Typography)
- **DC-T1 —** Dos familias (Sans/Mono) por intención Inter/JetBrains Mono; cambio de fuente = 1 edición.
- **DC-T2 —** Escala modular base 13sp redondeada a sp enteros (nitidez de escritorio).
- **DC-T3 —** Tres pesos (400/500/600); sin Light; Bold reservado.
- **DC-T4 —** 12 roles semánticos; los componentes eligen rol por significado, nunca tamaño suelto (TA-0).
- **DC-T5 —** Numeric y datos tabulados en Mono para alineación (VG-U4).
- **DC-T6 —** Tracking negativo solo en tamaños grandes; positivo leve en tamaños ≤12 para legibilidad.
- **DC-T7 —** Artefactos: `tokens.typography.json` (fuente de verdad) + `StudioTypographyTokens.kt`
  (Compose). El JSON manda; el Kotlin se regenera.

## Auditoría de compatibilidad con el canon (Typography)
- **VL / VG:** jerarquía por rol (VG-J), reposo y anti-fatiga (VL, lh 1.5, sin Light/Bold), alineación
  de datos (VG-U4), contraste tipográfico reservado (Display raro, VG-K5). Cada rol cita su ley (TA-0). ✔
- **Arquitectura de Tokens:** respeta `Foundation → Semantic(role)`; sin salto de capa; sin nuevas
  capas/familias; nomenclatura semántica y ordinal (size 100…800), no por apariencia. ✔
- **Color (9.1):** Typography no define color; el color del texto lo aporta `content.*` (9.1). Sin
  duplicación. ✔
- **Interaction Canon / PDS:** roles Shortcut (A7/IC-5), Badge (A2), Status (IC-8), Label (Inspector)
  mapean a componentes existentes; no se crean componentes. ✔
- **Filosofía / 4 pilares:** ecosistema-wide, un único sistema para todas las herramientas
  (Especialización Absoluta, reutilización interna); sostenible por un desarrollador (Unipersonal);
  IA como herramienta. ✔
- **Regla de Oro:** no toca la tipografía del *juego*; artefacto aislado en subpaquete `tokens`. ✔

**Veredicto: compatible con el canon. Materialización aprobada (no altera la arquitectura).**

---

# Anexo 9.6 — Motion System (materialización de valores) · CIERRE de Visual Tokens

*No modifica ninguna decisión de arquitectura; materializa las **cinco frases del movimiento**
(VG-MOV) del ecosistema Pokémon TCG. El sistema **limita** el movimiento, no lo multiplica. Con este
anexo queda **cerrada la infraestructura de Visual Tokens** (Color · Typography · Spacing · Depth ·
Surface · Motion). Artefactos: `tokens/tokens.motion.json` + `core/designsystem/.../tokens/StudioMotionTokens.kt`
(Compose, compila).*

## Escala mínima
- **Duraciones (4):** `Instant 0` (alta frecuencia/teclado) · `Fast 120` (confirmación/salida) ·
  `Base 180` (entrada/continuidad) · `Slow 240` (transformación). Todas <300ms (Emil).
- **Easings (3):** `Standard` ease-out fuerte `cubic(0.23,1,0.32,1)` (entra/sale) · `Movement`
  ease-in-out `cubic(0.77,0,0.175,1)` (morph en pantalla) · `Linear` (progreso constante). **Nunca
  ease-in** en UI.
- **Amplitud:** `Distance sm 4dp / md 8dp` · `Scale enterFrom 0.97 / press 0.97` (nunca desde 0) ·
  `Opacity fadeFrom 0` · `Stagger 40ms (max 50)`.

## Las cinco frases como roles oficiales (VG-MOV)
| Rol | Propósito / significado | Cuándo usar | Cuándo NO | Prioridad | Duración | Intensidad | IC / VG |
|---|---|---|---|---|---|---|---|
| **Continuity** | el mismo objeto se mueve manteniendo identidad | reordenar/reposicionar | para entradas/salidas | Normal | Base 180 | Subtle | IC-1/8 · VG-MOV |
| **Change** | algo entra o sale de escena | aparición/desaparición | para mover el mismo objeto | Normal | 180 entra / **120 sale** | Subtle | IC-8 · VG-MOV |
| **Transformation** | un objeto se convierte en otro estado suyo | expandir a detalle, morph | para cambios triviales | Normal | Slow 240 | Moderate | IC · VG-MOV |
| **Confirmation** | respuesta breve a una acción | feedback de pulsación | acciones de teclado/alta frecuencia | **Immediate** | Fast 120 | Subtle | IC-4/5 · VG-MOV |
| **Error** | algo no puede completarse / requiere atención | validación/bloqueo | de forma ambiental o frecuente | **Immediate** | Base 180 | Moderate | IC-8 · VG-MOV · VL Alerta |

*Regla de existencia:* si una transición no dice una de estas cinco frases, **no ocurre** (VG-0/VG-MOV).

## Políticas del sistema (lo pedido)
- **Transform:** solo `translate/scale/opacity`; jamás layout (padding/size) — rendimiento (Emil).
- **Priority / Interruption:** `Immediate` (Confirmation/Error) tiene precedencia; transiciones
  **interrumpibles** (retargetables), no keyframes.
- **Cancellation:** cancelar = **revertir desde el estado actual** (no saltar).
- **Chaining:** desaconsejado; solo stagger corto (≤50ms), nunca secuencias largas.
- **Accessibility:** con `prefers-reduced-motion` se retira el transform y se conserva un fade breve
  (`reducedMotionSpec`).
- **Quietud por defecto (VG-U4/VL):** lo de alta frecuencia o iniciado por teclado no se anima; el
  movimiento se gana. El movimiento nunca compite con el contenido (VL-CONT).

## Decisiones canónicas (Motion)
- **DC-MO1 —** Las 5 frases (VG-MOV) son los únicos roles de movimiento.
- **DC-MO2 —** 4 duraciones + 3 easings; sin ease-in en UI; todo <300ms.
- **DC-MO3 —** Salida asimétrica (más rápida que la entrada).
- **DC-MO4 —** Solo transform+opacity; transiciones interrumpibles; cancelación por reversión.
- **DC-MO5 —** Nunca `scale(0)`; entrada desde 0.97 + opacity.
- **DC-MO6 —** Quietud por defecto: alta frecuencia/teclado sin animación (VG-U4).
- **DC-MO7 —** `reducedMotionSpec` para accesibilidad (fade sin transform).
- **DC-MO8 —** Artefactos: `tokens.motion.json` (fuente de verdad) + `StudioMotionTokens.kt` (Compose).

## Auditoría de compatibilidad con el canon (Motion)
- **VG/VL:** VG-MOV (5 frases = roles), VG-0 (sin movimiento sin significado), VG-U4 (quietud por
  defecto), VL-CONT (no compite con el contenido), VL-fatiga (jornada de 8 h). Cada token cita su ley
  (TA-0). ✔
- **Interaction Canon:** Confirmation→IC-4/5, Change/Error→IC-8; feedback de pulsación y no bloqueo. ✔
- **Depth/Surface (9.4/9.5):** Change viste la aparición de planos Floating/Modal/Notification;
  coherente con sus elevaciones; no redefine planos ni materiales. ✔
- **Arquitectura de Tokens:** `Foundation → Semantic(role)`; escala mínima; nomenclatura por rol. ✔
- **Filosofía / 4 pilares / Regla de Oro:** ecosistema-wide; sostenible por un desarrollador; no toca
  el framework de animación del juego; artefacto aislado en `tokens`. ✔

**Veredicto: compatible con el canon. Materialización aprobada. — Con este anexo, la capa de Visual
Tokens (9.1–9.6) queda COMPLETA.**

---

# Anexo 9.5 — Surface System (materialización de valores)

*No modifica ninguna decisión de arquitectura ni la jerarquía de Depth; materializa la **apariencia
física** de los planos de 9.4 (corner/radius/border/shadow/blur/opacity/materiales). Escala mínima
("menos opciones, mejores decisiones"). Artefactos: `tokens/tokens.surface.json` +
`core/designsystem/.../tokens/StudioSurfaceTokens.kt` (Compose, compila).*

## Forma y escalas mínimas
- **Corner shape:** redondeada **uniforme** (4 esquinas iguales; sin asimetrías). Un solo lenguaje de
  esquina en todo el ecosistema (VG-C1, consistencia).
- **Radius (3 + pill):** `None 0` (datos/tablas/hairlines) · `Control 6` (botones/inputs/chips/card) ·
  `Container 10` (paneles/popovers/diálogos/toasts) · `Full` (pills/badges). Pocos y por rol.
- **Border (2 anchos):** `Thin 1dp` (hairlines y bordes de contenedor; la frontera se distingue por
  **color**, no por grosor — VG borde=frontera) · `Focus 2dp` (anillo de foco, IC-5/A5).
- **Elevation/Shadow (3 niveles + none):** materializa los planos de Depth → `Content 0 · Raised 2 ·
  Floating 8 · Modal 16 · Notification 8 · Alert 16`. En dark UI la separación primaria la da el
  **escalón de color de superficie** (9.1) + hairline; el shadow es refuerzo sutil (VL luz difusa).
- **Blur:** `None 0` por defecto (chasis opaco). `Backdrop 12` **reservado/restringido**: solo overlay
  a pantalla completa que deba ocultar contenido ocupado; nunca por estética (VG-0).
- **Opacity:** `Full 1.0` (chasis opaco = reposo) · `Scrim 0.55` (= `color.overlay.scrim`, 9.1).

## Glass — evaluado y RECHAZADO
Sometido a la Prueba de Gramática Visual: (1) ¿comunica significado o decora? → decora; (2) ¿respeta el
protagonismo del contenido? → no, un chasis translúcido compite con la carta; (3) ¿mantiene el
contraste reservado y la fatiga baja? → no, el desenfoque/translucidez añade ruido en jornadas largas.
**Falla VG-0 y VL-CONT/VL-fatiga → rechazado** como material del chasis. El chasis es opaco.

## Materiales (composición de tokens; el plano coincide con Depth 9.4)
| Material | Superficie (9.1) | Radius | Border | Elevación | Plano |
|---|---|---|---|---|---|
| **Panel** | surface.panel | Container | subtle | Content | Content |
| **Toolbar** | surface.panel | None | subtle (hairline) | Content | Content |
| **Sidebar** | surface.panel | None | subtle | Content | Content |
| **Inspector** | surface.panel | None | subtle | Content | Content |
| **Timeline** | surface.panel | None | subtle | Content | Content |
| **Card** | surface.panel | Control | subtle | Content | Content |
| **CardRaised** | surface.raised | Control | — | Raised | Raised |
| **Preview** | preview.backdrop | None | — | Content | Content |
| **Floating** | surface.overlay | Container | default | Floating | Floating |
| **Dialog** | surface.overlay | Container | default | Modal | Modal |
| **Toast** | surface.raised | Container | subtle | Notification | Notification |

## Justificación transversal (lo pedido)
- **Significado/propósito:** cada material viste un rol de dominio (materiales VL) sin inventar
  apariencia; la superficie comunica comportamiento, no estética.
- **Relación VG/VL/Depth:** radius/border = VG-CONTENEDOR; shadow = materialización de VG-D (Depth);
  opacidad/blur = VL luz difusa + VL-CONT; Preview acromático y sin chrome = VL-CONT.
- **Fatiga/jornadas:** chasis opaco, shadows sutiles, esquina única y hairlines de bajo contraste
  reducen el ruido perceptivo sostenido.
- **Protagonismo:** Preview sin borde ni redondeo (full-bleed) y sin glass → la carta posee la escena.
- **Cuándo NO:** no usar Glass; no subir elevación por estética (solo por plano, VG-D3); no variar la
  esquina por componente.

## Decisiones canónicas (Surface)
- **DC-SF1 —** Esquina redondeada uniforme; 3 radios + pill.
- **DC-SF2 —** 2 anchos de borde; la frontera se distingue por color (Thin) salvo foco (Focus 2dp).
- **DC-SF3 —** 3 niveles de shadow que materializan los planos de Depth (`Elevation.forPlane`).
- **DC-SF4 —** Chasis **opaco**; Glass **rechazado** por la Prueba de Gramática.
- **DC-SF5 —** Blur `None` por defecto; `Backdrop` reservado/restringido (nunca decorativo).
- **DC-SF6 —** Materiales = composición de color(9.1)+radius+border+elevación; el color se resuelve por
  tema vía `Scheme` (mismo material en dark/light/launcher).
- **DC-SF7 —** Artefactos: `tokens.surface.json` (fuente de verdad) + `StudioSurfaceTokens.kt` (Compose).

## Auditoría de compatibilidad con el canon (Surface)
- **Depth (9.4):** consume los planos; `Elevation.forPlane` cubre los 6; **no redefine la jerarquía**. ✔
- **Color (9.1):** materiales referencian roles `surface.*`/`border.*`/`preview.*`; sin HEX nuevos; se
  re-tematiza por `Scheme`. ✔
- **VG/VL:** VG-CONTENEDOR (radius/border), VG-D (shadow), VG-0 (Glass rechazado), VL luz difusa/
  protagonismo/fatiga. Cada token cita su ley (TA-0). ✔
- **Arquitectura de Tokens:** `Foundation → Semantic(material)`; escala mínima; nomenclatura por rol. ✔
- **Filosofía / 4 pilares / Regla de Oro:** ecosistema-wide; sostenible por un desarrollador; no toca el
  juego; artefacto aislado en `tokens`. ✔

**Veredicto: compatible con el canon. Materialización aprobada (no altera la arquitectura).**

---

# Anexo 9.4 — Depth System (materialización de valores)

*No modifica ninguna decisión de arquitectura; materializa la percepción espacial (Depth → Elevation +
Z-Index, DC-TA5) del ecosistema Pokémon TCG. Define **solo** planos y orden; NO define radius/border/
blur/corner/surface styling (Fase 9.5 Surface). El `level` de elevación es **ordinal**; su shadow
concreto lo materializa Surface. Artefactos: `tokens/tokens.depth.json` +
`core/designsystem/.../tokens/StudioDepthTokens.kt` (Compose, compila).*

## Escala de planos (6, fija y estable) — VG-D2
| # | Plano | z | Significado | Cuándo usar | Cuándo NO | Atención / carga / jornada |
|---|---|---|---|---|---|---|
| 0 | **Content** | 0 | base donde vive el contenido | Preview, paneles, dock, sidebar, inspector, timeline | para nada que interrumpa | atención plena al contenido; carga mínima; es el reposo de la jornada. |
| 1 | **Raised** | 100 | elevado-persistente, adherido | header pegado, **carta seleccionada**, base de arrastre, controles de preview | para algo transitorio o anclado a disparador | reclamo suave (VG-D3); no roba foco; sostenible. |
| 2 | **Floating** | 200 | anclado a un disparador, transitorio | tooltip, popover, menú contextual, Command Palette, ghost de arrastre | como estado permanente | atención puntual; desaparece pronto → carga baja. |
| 3 | **Modal** | 300 | interrupción con scrim | diálogos que exigen decisión | para info no bloqueante | máxima captura de atención; usar con cuentagotas (fatiga si abunda). |
| 4 | **Notification** | 400 | feedback del sistema no bloqueante | toasts | para contenido o acción primaria | voz alta breve; sobre modal para no quedar oculto (IC-8). |
| 5 | **Alert** | 500 | crítico topmost reservado | pérdida de trabajo / bloqueo | para cualquier cosa rutinaria | rarísimo por diseño; por raro, funciona (VL-silencio Alerta). |

**Paso entre planos = 100** (`ZStep`), con holgura para apilamiento intra-plano en runtime (p. ej.
toasts apilados) sin inventar planos nuevos.

## Jerarquía de flotantes y modales
`Content < Raised < Floating(tooltip/popover/menu) < Modal(dialog+scrim) < Notification(toast) < Alert`.
El **scrim** de los modales usa `color.overlay.scrim` (9.1); su geometría/estilo llega en Surface (9.5).

## Layering por contexto (aliases; sin valores nuevos)
- **Dock/Sidebar/Inspector/Timeline:** viven en **Content** (chrome estructural). El *flyout* del Dock
  sube a **Floating** por estar anclado y ser transitorio.
- **Preview:** telón en **Content** (protagonismo, VL-CONT); sus controles apenas suben a **Raised** para
  no competir con la carta.
- **Selected card / drag:** la carta seleccionada sube a **Raised** (reclamo, VG-D3); el *ghost* de
  arrastre a **Floating** (flota sobre el contenido).

## Justificación transversal (lo pedido, por nivel)
- **Significado / cuándo / cuándo no:** tabla anterior.
- **Relación con VG:** VG-D2 (escala corta de planos), VG-D3 (subir = interrumpir/reclamar), VG-D4 (no
  hay profundidad decorativa), VG-PROF (regla dura).
- **Atención:** cada plano superior captura más atención; el diseño mantiene casi todo en Content.
- **Carga cognitiva:** 6 niveles memorizables; sin z-index libres que obliguen a razonar caso a caso.
- **Sesiones largas:** la mayor parte del tiempo se vive en Content (reposo); los planos altos son
  raros y efímeros → no fatigan.

## Decisiones canónicas (Depth)
- **DC-DP1 —** Escala fija de **6 planos** (Content…Alert); no hay z-index fuera de ella (DC-TA5).
- **DC-DP2 —** El `level` de elevación es **ordinal**; el shadow concreto lo define Surface (9.5).
- **DC-DP3 —** Chrome estructural en Content; solo interrumpir/reclamar sube de plano (VG-D3).
- **DC-DP4 —** Orden de flotantes fijo: tooltip/popover/menú < modal < toast < alerta.
- **DC-DP5 —** Preview y su telón en Content; controles en Raised (protagonismo, VL-CONT).
- **DC-DP6 —** `ZStep = 100` para apilamiento intra-plano sin crear planos nuevos.
- **DC-DP7 —** Artefactos: `tokens.depth.json` (fuente de verdad) + `StudioDepthTokens.kt` (Compose).

## Auditoría de compatibilidad con el canon (Depth)
- **VG / VL:** VG-D2/D3/D4/PROF (planos semánticos, subir = interrumpir), VL-CONT (Content protagonista),
  VL-silencio (Notification/Alert raros). Cada plano cita su ley (TA-0). ✔
- **Arquitectura de Tokens:** cumple DC-TA5 (Depth rige Elevation y Z-Index; sin z-index libres);
  `Foundation → Semantic → Context`. ✔
- **Interaction Canon / PDS:** tooltip/popover/menu/dialog/toast/palette mapean a organismos existentes;
  no se crean componentes. Modal usa `overlay.scrim` (9.1). ✔
- **Color/Typography/Spacing (9.1–9.3):** ortogonal; no define color, texto ni distancia. ✔
- **Filosofía / 4 pilares / Regla de Oro:** ecosistema-wide; sostenible por un desarrollador; no toca el
  juego; artefacto aislado en `tokens`. Sin surface styling (respeta la separación Depth/Surface). ✔

**Veredicto: compatible con el canon. Materialización aprobada (no altera la arquitectura).**

---

# Anexo 9.3 — Spatial System (materialización de valores)

*No modifica ninguna decisión de arquitectura; materializa las familias Grid + Spacing + Density del
ecosistema Pokémon TCG dentro de la capa `Foundation → Semantic → Context`. Artefactos:
`tokens/tokens.spacing.json` (fuente de verdad) y `core/designsystem/.../tokens/StudioSpacingTokens.kt`
(Compose, compila).*

## Rejilla base y ramp
- **Baseline grid 4dp** (VG-U4): toda distancia es múltiplo de 4dp. **Único sub-múltiplo permitido:
  2dp**, exclusivamente como *hairline* interno. Se elige 4dp (no 8dp) porque el Studio es una
  herramienta densa de escritorio con inspectores y árboles: 8dp sería demasiado grueso para el control
  fino; 4dp da densidad sin caos.
- **Ramp Foundation (dp):** `0 · 2 · 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64`.

## Familia finita de distancias con significado (VG-U3) — capa Semantic
| Relación | dp | Significado (VG) | Problema que resuelve |
|---|---|---|---|
| **Hairline** | 2 | separación mínima interna (VG-E) | separar sin introducir un salto perceptible. |
| **Within** | 4 | dentro de grupo / items muy próximos = pertenencia (VG-E1) | agrupar por proximidad sin bordes. |
| **Related** | 8 | items relacionados de un grupo (VG-E1) | ritmo interno legible. |
| **Group** | 16 | entre grupos distintos (VG-E2) | salto inequívoco dentro-vs-entre grupos. |
| **Section** | 24 | entre secciones (VG-E2) | jerarquía espacial de segundo nivel. |
| **Zone** | 32 | entre zonas/regiones (Arq. Espacial) | respiración entre anillos/zonas. |

**Insets** (respiración interna, VG-E3): `xs 4 · sm 8 · md 12 · lg 16`.
**Layout:** `gutter 16 · margin 16 · readingMaxWidth 640dp` (≈65–75 car. a 13sp, VL-fatiga).

## Densidad (modificador, DC-TA7) — altera SOLO la altura de fila
`Comfortable 32 · Default 28 · Compact 24`. Los **insets horizontales permanecen constantes** entre
densidades: así la alineación de columnas (VG-U4) y la memoria muscular (VL) no se rompen al cambiar de
densidad; solo respira más o menos el eje vertical.

## Espaciados por contexto (aliases sobre Relation/Inset; sin valores nuevos)
`Dock`(pad 8, gap 4) · `Panel`(pad 12, header 12, contentGap 16) · `Toolbar`(padH 8, padV 4, gap 8) ·
`Sidebar`(pad 8, itemGap 2, itemH = densidad) · `Inspector`(pad 12, rowGap 4, labelValueGap 8, groupGap
16) · `Dialog`(pad 16, gap 16, actionGap 8) · `Overlay`(pad 8, itemGap 2) · `Timeline`(pad 8, trackGap
4) · `Preview`(pad 24 — aire para el protagonismo de la carta, VL-CONT) · `Tree`(indent 16, rowGap 2,
iconGap 8) · `Property`(rowH = densidad, rowGap 4, labelValueGap 8) · `Table`(cellPadX 8, cellPadY 4,
headerPadY 8, rowGap 0).

## Justificación transversal (cada decisión responde a lo pedido)
- **Qué problema resuelve:** elimina distancias arbitrarias (anti-patrón VG); toda separación pertenece
  a la familia finita y significa un grado de relación.
- **Cómo implementa el Visual Grammar:** proximidad=pertenencia (VG-E1), salto entre grupos (VG-E2),
  retícula/alineación (VG-U4), respiración (VG-E3), ritmo constante (VG-C2).
- **Fatiga:** ritmo espacial predecible y `readingMaxWidth` reducen el coste perceptivo en jornadas
  largas (VL-fatiga).
- **Legibilidad:** insets y line-length controlados; datos alineados por rejilla de 4dp.
- **Consistencia:** una relación = una distancia en todo el ecosistema (VG-C1); contextos como aliases,
  nunca valores sueltos.
- **Sesiones largas:** densidad ajustable sin mover insets horizontales → la mano no se re-aprende.

## Decisiones canónicas (Spatial)
- **DC-S1 —** Baseline 4dp; 2dp solo como hairline; ramp fijo 0–64.
- **DC-S2 —** Familia finita de 6 relaciones (Hairline…Zone) como única fuente legítima de separación.
- **DC-S3 —** Insets xs/sm/md/lg y Layout (gutter/margin/readingMaxWidth).
- **DC-S4 —** Densidad = modificador de altura de fila; insets horizontales constantes.
- **DC-S5 —** Contextos (Dock…Table) son aliases semánticos; no introducen valores nuevos (TA-0).
- **DC-S6 —** `Preview` recibe el mayor aire (Section) por protagonismo del contenido (VL-CONT).
- **DC-S7 —** Artefactos: `tokens.spacing.json` (fuente de verdad) + `StudioSpacingTokens.kt` (Compose).

## Auditoría de compatibilidad con el canon (Spatial)
- **VL / VG:** familia finita con significado (VG-U3/E), retícula (VG-U4), respiración (VG-E3), ritmo
  (VG-C2), protagonismo del Preview (VL-CONT), anti-fatiga (VL). Cada token cita su ley (TA-0). ✔
- **Arquitectura de Tokens:** `Foundation → Semantic → Context`; sin salto de capa; densidad como
  modificador (DC-TA7); nomenclatura ordinal/semántica. ✔
- **Arquitectura Espacial (Z1–Z12) / Layout:** las relaciones Zone/contextos se apoyan en zonas y
  anillos existentes; no los redefinen. ✔
- **Color / Typography (9.1/9.2):** ortogonal; no define color ni tipografía. Sin duplicación. ✔
- **Filosofía / 4 pilares:** ecosistema-wide (Especialización Absoluta), sostenible por un desarrollador
  (Unipersonal); Regla de Oro intacta (no toca el juego; artefacto aislado en `tokens`). ✔

**Veredicto: compatible con el canon. Materialización aprobada (no altera la arquitectura).**
