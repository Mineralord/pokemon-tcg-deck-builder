# Color Tokens — Pokémon TCG Developer Studio (Fase 9.1)

**Primer artefacto técnico del sistema visual.** Esta fase materializa la familia **Color** de la
arquitectura de tokens (Fase 9): define la semántica del color, su árbol de tokens y sus **valores
concretos** para el tema por defecto del Studio (`studio-dark`), y produce dos ficheros listos para
código:

- **Fuente de verdad portable:** `docs/developer-studio/tokens/tokens.color.json`
- **Artefacto Kotlin/Compose:** `core/designsystem/src/main/kotlin/com/mineralord/tcg/core/designsystem/tokens/StudioColorTokens.kt`

**Análisis integrado por dos lentes** (activas, una sola propuesta): `ui-ux-pro-max` (contraste,
accesibilidad AA/AAA, estados, consistencia) + `emil-design-eng` (fatiga en jornadas largas, contraste
reservado, "los detalles invisibles se suman", chasis que cede al contenido). **Conflicto resuelto:**
donde UI/UX empujaría a subir contraste "para verse pro", Emil y el canon exigen reposo; se resuelve
con la regla del **contraste reservado** (VG-K5): el chasis vive en contraste medio-bajo y solo el
foco/dato/alerta suben.

**Ley rectora heredada (TA-0):** ningún color existe sin implementar una ley del Visual Grammar o del
Visual Language. La tabla de tokens cita, por cada familia, la ley que materializa.

**Canon obligatorio:** Filosofía · 4 pilares · P1–P15 · VL-* (silencio de 4 modos, protagonismo,
luz difusa, fatiga) · VG-* (VG-0, VG-K contraste, VG-D planos, VG-CONTENEDOR) · Arquitectura de Tokens
(Foundation → Semantic → Component; Theme = dimensión de Semantic; nomenclatura semántica).

---

## Parte I — Color Canon (las leyes del color del Studio)

- **CC-1 · Tema por defecto oscuro.** El Studio es un banco de trabajo de jornadas largas: el tema
  canónico es `studio-dark`. Un tema claro (`studio-light`) y temas de ecosistema (launcher, visor)
  son *mapeos futuros* con **los mismos nombres semánticos** (VL-EVO-3). Esta fase entrega `studio-dark`
  completo.
- **CC-2 · Ni negro puro ni blanco puro.** Prohibidos `#000000` como fondo y `#FFFFFF` como texto de
  cuerpo: fatigan y elevan el contraste al máximo permanente (VG-K5, VL-fatiga). El blanco máximo se
  reserva como peldaño raro.
- **CC-3 · Neutro con temperatura única y sutil.** Toda la rampa neutra comparte un subtono frío-azulado
  muy leve (sensación tecnológica de confianza mecánica, VL). La temperatura es constante para no
  introducir ruido cromático en el chasis.
- **CC-4 · Contraste reservado (VG-K).** El chasis (superficies, bordes, texto secundario) vive en
  contraste medio-bajo estable. El contraste alto se reserva para: texto primario de trabajo, **foco**,
  **dato clave** y **alerta**. El contraste base no fluctúa durante la jornada.
- **CC-5 · El color pertenece al contenido, no al marco (VL-CONT).** El chasis es casi acromático. El
  único croma del marco es un **acento contenido** (azul-cian mesurado) y los **feedback** (éxito/aviso/
  peligro) *desaturados*. Los colores saturados y vivos son territorio de la carta Pokémon, nunca del
  chasis. `Preview.Backdrop` es deliberadamente acromático para que la carta posea todo el color.
- **CC-6 · Un solo acento.** No hay paleta de acentos decorativos. Un único acento sirve a acción
  primaria, selección y foco (con variaciones de estado). Menos color = más silencio (VL, Emil).
- **CC-7 · Feedback = modo Alerta del silencio.** Los colores de feedback existen para el modo "voz
  alta" (VL-silencio); por eso son raros y contenidos, no ambientales.
- **CC-8 · El color nunca es el único indicador (a11y).** Todo estado comunicado por color va también
  por forma/texto/icono (P1, WCAG 1.4.1). El daltonismo no puede dejar a nadie sin información.
- **CC-9 · Accesibilidad como piso, no como techo.** Texto de cuerpo ≥ 4.5:1 (AA); texto primario
  apunta a ≥ 7:1 (AAA) cuando no compromete el reposo. Texto muted (decorativo/no esencial) ≥ 3:1 y
  nunca porta información crítica.

---

## Parte II — Familias Semánticas de Color

Se evalúa la lista de referencia. **Se adoptan 9 familias; se elimina "Interaction".**

### ✅ 1 · Surface — superficies / planos
- **Propósito / significado:** el plano que hospeda contenido; encarna los materiales del VL
  (Superficie/Panel/Overlay) y la escala de planos de Depth (VG-D).
- **Relación VG:** VG-CONTENEDOR (ámbito), VG-D (profundidad = plano de atención/interrupción).
- **Relación PDS:** viste Panel, Dock, Sidebar, Dialog, Overlay, Card (fondo), Status Bar.
- **Cuándo usarla:** todo fondo de región/contenedor.
- **Cuándo NO:** para texto, iconos o bordes; para transmitir estado (eso es Feedback/State).
- **Evolución:** se añaden peldaños de plano por extensión; no se redefine un plano existente.

### ✅ 2 · Content — texto, iconos y datos (foreground)
- **Propósito / significado:** el contenido legible del chasis; jerarquía por énfasis (VG-J).
- **Relación VG:** VG-J (jerarquía), VG-K (contraste = importancia), VL-fatiga (lectura prolongada).
- **Relación PDS:** todo texto/icono; Property Row, Tree Node, Toolbar labels.
- **Cuándo usarla:** cualquier foreground informativo.
- **Cuándo NO:** como fondo; `OnAccent` solo sobre superficies de acento.
- **Evolución:** los niveles (Primary/Secondary/Muted/Disabled/Emphasis) son estables; se re-mapea el
  primitivo, no el rol.

### ✅ 3 · Border — límites y separadores
- **Propósito / significado:** frontera de un dominio (VG "borde = límite"); la separación más discreta
  cuando el espacio no basta (VG-E).
- **Relación VG:** VG-CONTENEDOR, VG-E (separación mínima), VG-K (peso = énfasis del límite).
- **Relación PDS:** Divider, borde de Panel/Card/Inspector, separadores de Property Grid.
- **Cuándo NO:** para agrupar cuando basta la proximidad (VG-U2: primero espacio, luego borde).

### ✅ 4 · Accent — acento único (acción primaria e interacción)
- **Propósito / significado:** el único croma del marco; marca la acción primaria y lo interactivo
  relevante (CC-6).
- **Relación VG:** VG-K (destacar = rol distinto), VL (destacar sin gritar).
- **Relación PDS:** botón primario, control activo, enlace, base cromática de Selection/Focus.
- **Cuándo NO:** para decorar; para múltiples acentos; para grandes áreas de fondo (rompería reposo).

### ✅ 5 · Feedback — estado del sistema (éxito / aviso / peligro)
- **Propósito / significado:** el modo Alerta del silencio (CC-7); comunica resultado o riesgo.
- **Relación VG:** VG-MOV/VL-silencio (voz alta, rara), VG-K (contraste solo cuando importa).
- **Relación PDS:** Toast, Notification, Error State, botón destructivo, Status Segment.
- **Cuándo NO:** como color de marca o decoración; de forma ambiental permanente.
- **Nota:** **no hay "info"** — la información neutra usa Content/Accent. Añadir "info" duplicaría el
  azul del acento (se elimina por prueba de pertenencia).

### ✅ 6 · Selection — objeto(s) seleccionado(s) (IC-1)
- **Propósito / significado:** marca la selección persistente del modelo, distinta de hover y de foco.
- **Relación VG:** IC-1 (selección), VG-K; A6 del PDS (Selection Highlight).
- **Cuándo NO:** para hover (IC-4) ni para foco de teclado (A5) — deben ser lenguajes distintos.

### ✅ 7 · Focus — indicador de foco de teclado (A5 / IC-5)
- **Propósito / significado:** la **única** señal canónica de foco de teclado en todo el PDS (a11y).
- **Relación VG:** IC-5, P1 (accesibilidad); A5 Focus Indicator.
- **Cuándo NO:** para selección o hover; nunca omitir en interactivos (regla dura a11y).

### ✅ 8 · Overlay — atenuación tras una interrupción (scrim)
- **Propósito / significado:** oscurece el plano base cuando algo lo interrumpe (diálogo/overlay),
  bajando el contexto a "pausado" (VG-D "detrás = contexto pausado").
- **Relación VG:** VG-D (planos), VL-silencio (Desaparecer del fondo).
- **Cuándo NO:** como fondo permanente; para efectos "glass" decorativos (prohibido, VG-0).

### ✅ 9 · Preview — telón del contenido (protagonismo, VL-CONT)
- **Propósito / significado:** el fondo del escenario donde la carta Pokémon (protagonista) se observa;
  **acromático a propósito** para cederle todo el color (CC-5).
- **Relación VG:** VL-CONT (protagonismo), VG-K (el chasis no compite en croma).
- **Relación PDS:** Preview (organismo), visor de cartas, escenario de animación.
- **Cuándo NO:** para UI de trabajo (usa Surface); nunca teñir el telón con color de marca.

### ❌ Interaction — ELIMINADA como familia de color
- **Motivo:** los estados (hover/active/focus/selected/disabled) son un **eje de variación** sobre las
  familias existentes (Arquitectura DC-TA7), no una familia de color propia. Se materializan como
  variantes `state` dentro de Accent/Content/Surface/Feedback. Crear "Interaction" duplicaría
  significado (VG-L2: un signo, un significado).

---

## Parte III — Árbol Semántico de Color

```
color
├─ surface
│  ├─ canvas          (plano base donde vive el contenido)
│  ├─ panel           (territorio permanente en reposo)
│  ├─ raised          (plano elevado-persistente)
│  ├─ overlay         (plano de interrupción: diálogo/menú/popover)
│  └─ sunken          (pozo/recesión: campos, canales)
├─ content
│  ├─ primary         (texto/dato de trabajo)
│  ├─ secondary       (apoyo)
│  ├─ muted           (terciario / no esencial)
│  ├─ disabled        (inactivo)
│  ├─ emphasis        (alta jerarquía: títulos, foco textual)
│  └─ on-accent       (texto/icono sobre superficie de acento)
├─ border
│  ├─ subtle          (separación mínima)
│  ├─ default         (límite estándar)
│  └─ strong          (contenedor enfatizado/activo)
├─ accent
│  ├─ rest
│  ├─ hover
│  ├─ active
│  └─ muted           (superficie de acento tenue: fondo de activo/seleccionado)
├─ feedback
│  ├─ success / success.surface
│  ├─ warning / warning.surface
│  └─ danger / danger.hover / danger.active / danger.surface
├─ selection
│  ├─ background      (relleno de selección)
│  └─ edge            (borde de celda/rango activo)
├─ focus
│  └─ ring            (único indicador de foco)
├─ overlay
│  └─ scrim           (atenuación tras interrupción)
└─ preview
   ├─ backdrop        (telón acromático primario)
   └─ backdrop-alt    (telón alterno para juzgar transparencias)
```

---

## Parte IV — Tokens Definitivos (tema `studio-dark`)

Rampa **Foundation** (primitivos, subtono frío ~hue 220, saturación baja). No se usan directamente;
solo los referencia la capa Semantic.

| Primitivo | HEX | Uso raíz |
|---|---|---|
| `neutral/0`  | `#0E1116` | plano más profundo / pozo |
| `neutral/5`  | `#14181F` | canvas base |
| `neutral/10` | `#1A1F27` | panel reposo |
| `neutral/15` | `#212732` | raised |
| `neutral/20` | `#2A313D` | overlay / border subtle |
| `neutral/30` | `#3D4653` | border default |
| `neutral/40` | `#4C5563` | border strong |
| `neutral/50` | `#5E6773` | disabled |
| `neutral/60` | `#78828F` | muted |
| `neutral/70` | `#9AA4B0` | secondary |
| `neutral/80` | `#C2CAD3` | primary (cuerpo) |
| `neutral/90` | `#E2E7EC` | emphasis |
| `neutral/100`| `#F5F7FA` | máximo (reservado, raro) |
| `accent/50`  | `#3E88AD` | accent active |
| `accent/60`  | `#4FA3CC` | accent rest / selection edge |
| `accent/70`  | `#6FBBDE` | accent hover / focus ring |
| `accent/muted`| `#21404E`| superficie de acento tenue |
| `success/60` | `#4F9D6E` | éxito |
| `warning/60` | `#C79445` | aviso |
| `danger/50`  | `#A94B4B` | peligro active |
| `danger/60`  | `#CF5D5D` | peligro |
| `danger/70`  | `#E07B7B` | peligro hover |

**Semantic (`studio-dark`) con justificación y contraste** (contrastes aproximados, verificados sobre
`surface.panel #1A1F27` salvo indicación):

| Token | → Primitivo | HEX | Justificación (a11y · fatiga · contraste · protagonismo · identidad) |
|---|---|---|---|
| `surface.canvas` | neutral/5 | `#14181F` | plano base sereno; no-negro (CC-2). |
| `surface.panel` | neutral/10 | `#1A1F27` | territorio de reposo; contraste base estable (CC-4). |
| `surface.raised` | neutral/15 | `#212732` | plano elevado por función, no por adorno (VG-D). |
| `surface.overlay` | neutral/20 | `#2A313D` | plano de interrupción, distinguible del panel. |
| `surface.sunken` | neutral/0 | `#0E1116` | pozo/canal; recesión = "detrás". |
| `content.primary` | neutral/80 | `#C2CAD3` | cuerpo ≈ **9.9:1** (AAA); legible en jornadas largas. |
| `content.secondary` | neutral/70 | `#9AA4B0` | apoyo ≈ **6.5:1** (AA); jerarquía sin gritar. |
| `content.muted` | neutral/60 | `#78828F` | terciario ≈ **4.2:1**; solo no-esencial (CC-9). |
| `content.disabled` | neutral/50 | `#5E6773` | inactivo; comunica "apagado" (no porta info). |
| `content.emphasis` | neutral/90 | `#E2E7EC` | títulos/foco textual ≈ **13:1**; alto por rol (CC-4). |
| `content.on-accent` | neutral/0 | `#0E1116` | sobre `accent.rest` ≈ **6.7:1**; texto oscuro sobre cian. |
| `border.subtle` | neutral/20 | `#2A313D` | separación mínima cuando el espacio no basta (VG-U2). |
| `border.default` | neutral/30 | `#3D4653` | límite estándar de contenedor. |
| `border.strong` | neutral/40 | `#4C5563` | contenedor activo/enfatizado. |
| `accent.rest` | accent/60 | `#4FA3CC` | único acento; foco/acción ≈ **5.8:1** vs panel. |
| `accent.hover` | accent/70 | `#6FBBDE` | realce de hover (IC-4). |
| `accent.active` | accent/50 | `#3E88AD` | pulsado (IC), más profundo. |
| `accent.muted` | accent/muted | `#21404E` | fondo de activo/seleccionado sin gritar. |
| `feedback.success` | success/60 | `#4F9D6E` | éxito desaturado (CC-7); + icono/texto (CC-8). |
| `feedback.success.surface` | — | `#1B2E24` | banner de éxito, tenue. |
| `feedback.warning` | warning/60 | `#C79445` | aviso ámbar mesurado. |
| `feedback.warning.surface` | — | `#2E2717` | banner de aviso, tenue. |
| `feedback.danger` | danger/60 | `#CF5D5D` | peligro contenido; voz alta rara. |
| `feedback.danger.hover` | danger/70 | `#E07B7B` | hover destructivo. |
| `feedback.danger.active` | danger/50 | `#A94B4B` | pulsado destructivo. |
| `feedback.danger.surface` | — | `#2E1B1B` | banner de error, tenue. |
| `selection.background` | — | `#22404E` | selección persistente (IC-1), distinta de hover. |
| `selection.edge` | accent/60 | `#4FA3CC` | borde de celda/rango activo. |
| `focus.ring` | accent/70 | `#6FBBDE` | foco único ≈ **7.2:1**; visible incluso sobre acento. |
| `overlay.scrim` | — | `#080B10` @ 55% | atenúa el contexto a "pausado" (VG-D). |
| `preview.backdrop` | — | `#15171A` | **acromático** para ceder el color a la carta (CC-5). |
| `preview.backdrop-alt` | — | `#1E2024` | telón alterno para juzgar transparencias. |

> Los contrastes marcados se calcularon con la fórmula WCAG (luminancia relativa). Deben re-verificarse
> con herramienta automática al integrar (riesgo vigilado en *Impacto en el Futuro*). Cualquier ajuste
> se hace en el primitivo, nunca en el componente (TD/TE).

**Estados como eje (no familia):** un interactivo compone `rest → hover → active` sobre su token base
(p. ej. Accent), aplica `focus.ring` al recibir foco de teclado, `selection.background` si está
seleccionado, y `content.disabled` si está inactivo. Este eje es común a todo el PDS (IC-1/4/5).

---

## Parte V — Decisiones Canónicas

- **DC-C1 —** Tema canónico `studio-dark`; `studio-light`/launcher/visor son mapeos futuros con nombres
  semánticos idénticos.
- **DC-C2 —** Ni `#000` de fondo ni `#FFF` de cuerpo; blanco máximo reservado (CC-2).
- **DC-C3 —** Rampa neutra de temperatura fría única y sutil (CC-3).
- **DC-C4 —** Contraste reservado: chasis medio-bajo estable; alto solo para primary/emphasis/foco/dato/
  alerta (CC-4, VG-K5).
- **DC-C5 —** Un único acento para acción primaria + selección + foco (CC-6).
- **DC-C6 —** Feedback desaturado y raro (modo Alerta); sin "info" (CC-7 + prueba de pertenencia).
- **DC-C7 —** `Preview.Backdrop` acromático: el color pertenece a la carta (CC-5, VL-CONT).
- **DC-C8 —** El color nunca es el único indicador (CC-8, P1).
- **DC-C9 —** "Interaction" no es familia de color: los estados son un eje (DC-TA7).
- **DC-C10 —** Dos artefactos técnicos entregados: `tokens.color.json` (fuente de verdad) y
  `StudioColorTokens.kt` (Compose). El JSON manda; el Kotlin se regenera desde él.

---

## Auditoría Semántica

- Nombres semánticos por **rol**, nunca por apariencia: `content.primary`, no "gris-claro";
  `accent.rest`, no "azul" (TN-2). Un `feedback.danger` puede dejar de ser rojo sin renombrar.
- **Un concepto = una palabra:** "surface" para plano, "content" para foreground, "border" para límite.
  Se elimina "info" (evita dos azules para un rol) y "Interaction" (evita duplicar el eje de estados).
- **"preview" (telón del Studio)** vs **"Preview" (organismo PDS)**: coinciden por diseño; el token viste
  el organismo. Sin colisión con "carta Pokémon" (contenido de dominio), que no tiene token de color.
- Idioma: nombres de token en **inglés canónico** (alineado a la API/código Compose y a los organismos
  PDS); la documentación permanece en español. Regla TN-5 cerrada así para esta familia.

**Veredicto: sin ambigüedad ni duplicación. Aprobada.**

## Auditoría de Coherencia

1. **¿VL / VG?** Cada token cita su ley (Parte II/IV): silencio, protagonismo, contraste reservado,
   planos. Cumple TA-0.
2. **¿Arquitectura de Tokens?** Respeta Foundation→Semantic; sin salto de capa; sin z-index/estado
   aquí; Theme como dimensión (nombres idénticos entre temas). Sin Component tokens (no hicieron falta).
3. **¿Interaction Canon?** Selection/Focus/estados mapean a IC-1/IC-4/IC-5.
4. **¿PDS?** Viste organismos existentes (Panel, Dialog, Preview, Toast…) sin crear componentes.
5. **¿P1–P15 / a11y?** Contrastes AA/AAA para texto; color no es único indicador; foco visible único.
6. **¿Regla de Oro?** No toca `TcgColors` (colores del juego); el Studio no absorbe lógica/estilo del
   juego. Artefacto en subpaquete `tokens`, aislado.
7. **¿Escala/deuda?** Extensión por peldaños; un valor cambia en un solo lugar (primitivo).

**Veredicto: Canónico.**

## Auditoría de Filosofía

1. **Desarrollo Unipersonal:** un tema activo, un acento, mínima configurabilidad; sostenible por una
   persona.
2. **Sin complejidad multi-desarrollador:** no hay temas por usuario ni namespaces por autor.
3. **Simplificable:** ya reducido (9 familias, sin "info" ni "Interaction").
4. **IA como herramienta:** los colores describen diseño; ninguna IA es actor con tema propio.
5. **Preservación/evolución:** nombres = contrato; re-tema sin renombrar (VL-EVO-3).

**Veredicto: alineado con los cuatro pilares.**

## Impacto en el Futuro

- **Habilita** 9.2–9.5 (tipografía/espaciado/elevación/motion) sobre una base cromática estable, y el
  estilado de componentes (Fase 10) que se vestirán solo con estos semantics.
- **Habilita** `studio-light` y temas de ecosistema por remapeo Semantic→Foundation sin tocar UI.
- **Condiciona:** Depth/Elevation (9.4) deberá ser coherente con la escala de `surface.*` ya fijada;
  Motion (9.5) usará estos colores sin introducir nuevos.
- **Riesgos a vigilar:** (a) re-verificar contrastes con herramienta automática al integrar (los
  valores muted rondan el límite AA — no usarlos para body); (b) presión por añadir acentos o "info" →
  rechazar (DC-C5/C6); (c) tentación de teñir `preview.backdrop` → prohibido (DC-C7); (d) mantener el
  JSON como fuente de verdad y regenerar el Kotlin (DC-C10).

## Evaluación AAA

*¿A la altura de Figma / JetBrains / VS Code / Unreal?* Sí: esos entornos comparten exactamente este
patrón —tema oscuro neutro de contraste reservado, un acento único, feedback desaturado, superficies
por plano semántico— porque es lo que sostiene jornadas de ocho horas sin fatiga. Este set lo adopta y
añade dos rasgos defendibles: **contraste reservado como regla explícita** (no accidente de gusto) y
**`preview.backdrop` acromático** que subordina el chasis a la carta Pokémon (protagonismo canónico).
Lo que falta para el estándar pleno es la verificación empírica de contraste sobre pantalla real y el
tema claro; ambos son trabajo acotado posterior. Como *set de color oscuro canónico*, está a la altura
AAA. **Aprobado.**
