# Developer Studio — Proceso Oficial de Diseño

Índice y reglas de proceso del diseño del **Pokémon TCG Clone Developer Studio**.

## Orden canónico de fases (no saltar niveles)
1. **Filosofía UX** → `UX-PRINCIPLES.md` ✅ cerrado (P1–P15)
2. **Arquitectura Espacial** → `SPATIAL-ARCHITECTURE.md` ✅ cerrado (Z1–Z12, anillos)
3. **Workflows Canónicos** → `WORKFLOWS.md` ✅ cerrado (W1–W21, O1–O8)
4. **Layout Conceptual** → `LAYOUT-CONCEPTUAL.md` ✅ (canónico; D1/D2 ya cerradas)
   - Cierre D1/D2 → `D1-D2-RESOLUCION.md` ✅ (DC-D1, DC-D2 canónicas)
4.5. **Modelo Mental y Canon Terminológico** → `MENTAL-MODEL.md` ✅ (canónico)
   - Vocabulario de dominio de referencia OBLIGATORIA para docs/UI/código/API.
   - Fundamentales: Project · Lab · Resource. Derivados contenido: Variant · Preset ·
     Snapshot · Benchmark · Preview. Derivados entorno: Session · Workspace. Auxiliar: Asset.
   - Regla: un concepto = una palabra, significado invariante entre Labs. "version" (artefacto)
     → **Snapshot**. Higiene pendiente: alinear "version" en docs previos.
5. **Interaction Language (Interaction Canon)** → `INTERACTION-CANON.md` ✅ (IC-0…IC-13 canónicos)
6. **Design System (PDS)** → `DESIGN-SYSTEM.md` ✅ (catálogo de componentes; arquitectura, sin visual)
   - **PTCG Developer Studio Design System (PDS)** = lenguaje de interfaz de TODO el ecosistema
     (Studio + launcher + visor de cartas + herramientas). Reutilización interna, no genérica.
   - Jerarquía: Átomos → Moléculas → Organismos → Plantillas → Patrones (+ Window). Catálogo
     cerrado por criterio; evolución solo por extensión; capa de tokens reservada para Fase 7.
7. **Visual Language (Visual Language Canon)** → `VISUAL-LANGUAGE.md` ✅ (VL-0…VL-P9, DC-VL1…9)
   - Identidad visual PROFUNDA sin valores concretos: arquetipo (taller artesano-científico),
     proporción de sensaciones (Tec 40/Cie 25/Art 25/Cre 10), silencio de 4 modos, protagonismo del
     contenido, materiales por significado, luz difusa, movimiento con significado, fatiga y evolución.
   - Incluye la **Prueba de Identidad Visual**: requisito de aprobación OBLIGATORIO para toda pantalla
     y decisión visual de las Fases 9–11.
8. **Visual Grammar (Visual Grammar Canon)** → `VISUAL-GRAMMAR.md` ✅ (VG-0, VG-L1…L10, DC-VG1…9)
   - Última capa conceptual: las LEYES universales que explican el *significado* de toda decisión
     visual (jerarquía, espacio, profundidad, contraste, movimiento, contenedores, excepciones).
   - Ley Cero: todo recurso visual significa; nada decora. Los Visual Tokens serán solo la
     implementación técnica de esta gramática.
   - Incluye la **Prueba de Gramática Visual**: requisito de aprobación OBLIGATORIO antes de Fases 9–11.

El "Diseño Visual" se materializa en TRES fases separadas (mantenibilidad y coherencia a largo plazo).
Fin de las capas conceptuales; a partir de aquí se materializa el sistema:
9. **Visual Tokens — Arquitectura** → `VISUAL-TOKENS-ARCHITECTURE.md` ✅ (TA-0, DC-TA1…10)
   - 3 capas nucleares (Foundation → Semantic → Component) + Theme como dimensión de Semantic;
     rechazadas Context/Alias/Runtime con justificación. Depth rige Elevation y Z-Index.
   - 19 familias de tokens, cada una citando la ley VG/VL que implementa. Nomenclatura semántica y
     ordinal; el nombre es un contrato de significado inmutable. TA-0: sin ley detrás, no hay token.
   - Materialización de VALORES en subfases (sin nuevas capas/familias; cada una pasa ambas Pruebas):
     - **9.1 Color** ✅ → `COLOR-TOKENS.md` (DC-C1…10) + artefactos: `tokens/tokens.color.json`
       (fuente de verdad) y `core/designsystem/.../tokens/StudioColorTokens.kt` (Compose, compila).
       9 familias semánticas (Surface/Content/Border/Accent/Feedback/Selection/Focus/Overlay/Preview);
       "Interaction" eliminada (los estados son un eje). Tema `studio-dark`; contraste reservado.
     - **9.2 Typography** ✅ → Anexo 9.2 en `VISUAL-TOKENS-ARCHITECTURE.md` (DC-T1…7) + artefactos:
       `tokens/tokens.typography.json` + `core/designsystem/.../tokens/StudioTypographyTokens.kt`
       (Compose, compila). 2 familias (Inter/JetBrains Mono por intención), escala base 13sp, 3 pesos,
       12 roles semánticos (Display…Status); Numeric en Mono para alineación.
     - **9.3 Spatial System** ✅ → Anexo 9.3 en `VISUAL-TOKENS-ARCHITECTURE.md` (DC-S1…7) + artefactos:
       `tokens/tokens.spacing.json` + `core/designsystem/.../tokens/StudioSpacingTokens.kt` (compila).
       Baseline 4dp; 6 relaciones con significado (Hairline…Zone); insets; densidad = modificador de
       altura de fila; contextos (Dock…Table) como aliases; Preview con más aire (protagonismo).
     - **9.4 Depth System** ✅ → Anexo 9.4 en `VISUAL-TOKENS-ARCHITECTURE.md` (DC-DP1…7) + artefactos:
       `tokens/tokens.depth.json` + `core/designsystem/.../tokens/StudioDepthTokens.kt` (compila).
       Solo percepción espacial: 6 planos fijos (Content…Alert), elevación como nivel ordinal, z-index
       derivado (DC-TA5). Radius/border/blur/surface diferidos a 9.5 (Depth ≠ Surface).
     - **9.5 Surface System** ✅ → Anexo 9.5 en `VISUAL-TOKENS-ARCHITECTURE.md` (DC-SF1…7) + artefactos:
       `tokens/tokens.surface.json` + `core/designsystem/.../tokens/StudioSurfaceTokens.kt` (compila).
       Materializa la apariencia física de los planos de Depth: esquina única, 3 radios+pill, 2 bordes,
       3 shadows (`Elevation.forPlane`), chasis opaco, **Glass rechazado**, 11 materiales por rol.
     - **9.6 Motion System** ✅ → Anexo 9.6 en `VISUAL-TOKENS-ARCHITECTURE.md` (DC-MO1…8) + artefactos:
       `tokens/tokens.motion.json` + `core/designsystem/.../tokens/StudioMotionTokens.kt` (compila).
       Las 5 frases (VG-MOV) como roles; 4 duraciones + 3 easings; salida asimétrica; solo
       transform+opacity; quietud por defecto; `reducedMotionSpec` (a11y).
   - **✅ INFRAESTRUCTURA DE VISUAL TOKENS COMPLETA** (9.1–9.6): Color · Typography · Spacing · Depth ·
     Surface · Motion — todo el lenguaje visual existe en código y compila.
10. **Component Styling** → Anexo Fase 10 en `DESIGN-SYSTEM.md` + `core/designsystem/.../tokens/`
    `StudioTheme.kt` (CompositionLocals) y `ComponentStyle.kt` (descriptores). Iteraciones internas:
    - **10.1 Controles básicos** ✅ (Button/IconButton/Toggle/Segmented/TextField/SearchField/Checkbox/
      Radio/Switch/Slider/Progress/Spinner/Chip/Badge) — compila; solo tokens, cero literales.
    - Pendientes: **10.2** Navegación/contenedores · **10.3** Datos · **10.4** Feedback/overlays ·
      **10.5** Especializados (Timeline/Preview/StatusBar).
11. **Screen Design** → pendiente (pantallas y Labs; cada una supera Prueba de Identidad + Gramática).
10. **Component Styling** → pendiente (apariencia concreta de cada componente del PDS; elige modo de
    silencio, material, plano y frase de movimiento; pasa Prueba de Identidad + Prueba de Gramática).
11. **Screen Design** → pendiente (diseño de pantallas y Labs; cada pantalla declara su protagonista,
    su foco único y sus planos, y supera ambas Pruebas antes de aprobarse).

Arquitectura técnica de referencia: `ARCHITECTURE.md` (v0.2) · `M0-CORE.md`.
Regla de Oro transversal: *el Studio nunca contiene lógica exclusiva del juego* (I1).
**Principios de máxima prioridad (los cuatro pilares):**
1. **Desarrollo Unipersonal** → `../PRINCIPIO-DESARROLLO-UNIPERSONAL.md` (un único desarrollador
   humano; IA = herramientas, no actores; sin equipo/permisos/concurrencia).
2. **Especialización Absoluta** → `PRINCIPIO-ESPECIALIZACION-ABSOLUTA.md` (herramienta específica
   de Pokémon TCG Clone; nunca IDE genérico; plugins = modularidad interna, no marketplace).
3. **El juego como producto** (solo consume componentes validados).
4. **El Studio como herramienta especializada** (todo se desarrolla aquí antes de integrarse).

## NORMA PERMANENTE — secciones obligatorias de cierre
**Todo documento de diseño futuro del Developer Studio DEBE terminar con estas dos secciones,
en este orden.** No son checklists de trámite: son análisis crítico. Si una revela un problema,
se explica con claridad aunque cuestione una decisión ya tomada (mejor descubrir la
inconsistencia ahora que en 5 años).

### 1. `# Auditoría de Coherencia`
Revisa el documento recién generado contra TODO el canon existente. Mínimo:
1. ¿Contradice algún Principio UX (P1–P15)?
2. ¿Contradice la Arquitectura Espacial?
3. ¿Contradice los Workflows Canónicos?
4. ¿Introduce excepciones innecesarias?
5. ¿Introduce duplicación de responsabilidades?
6. ¿Respeta la Regla de Oro (sin lógica exclusiva del juego)?
7. ¿Escala a cientos de Labs, miles de recursos y décadas?
8. ¿Genera deuda técnica o conceptual futura?
9. ¿Alguna decisión debe revisarse antes de continuar?
10. ¿Puede considerarse Canónico o necesita ajustes?

### 1-bis. `# Auditoría Semántica` (cuando el documento introduzca o use terminología)
Auditoría del lenguaje contra el Canon (`MENTAL-MODEL.md`): ¿concepto ambiguo? ¿dos palabras
para un concepto? ¿un concepto con dos significados? ¿puede un usuario nuevo construir el modelo
mental? ¿palabra a prohibir? ¿escala décadas? Va antes de "Impacto en el Futuro".

### 1-ter. `# Auditoría de Filosofía` (OBLIGATORIA en todos los documentos futuros)
Contra `PRINCIPIO-DESARROLLO-UNIPERSONAL.md`. Mínimo:
1. ¿Respeta el principio de Desarrollo Unipersonal?
2. ¿Introduce complejidad pensada para múltiples desarrolladores?
3. ¿Puede simplificarse por existir un único desarrollador humano?
4. ¿Las IA siguen tratándose solo como herramientas, nunca como actores del sistema?
5. ¿Mantiene la filosofía de simplicidad, preservación y evolución a largo plazo?

### 2. `# Impacto en el Futuro`
Pensar varios pasos por delante. Mínimo:
- ¿Qué decisiones futuras quedan condicionadas por este documento?
- ¿Qué módulos se verán afectados?
- ¿Qué oportunidades habilita?
- ¿Qué limitaciones introduce?
- ¿Qué riesgos deberán vigilarse en las siguientes fases?

### 3. `# ¿A la altura de un producto AAA?` (desde Fase 5 en adelante)
A partir del Sistema de Interacción, tratar el Studio como producto comercial. Cada documento
responde: *¿esta decisión estaría a la altura de Figma / Unreal / JetBrains / VS Code / Unity?*
No para copiarlas, sino para sostener un estándar alto. Si la respuesta es "no", el documento
debe explicar **qué le falta** antes de considerarse canónico.

### Skills obligatorias de análisis (desde Fase 5)
Las fases de diseño invocan y **integran en una sola propuesta** `ui-ux-pro-max` (ergonomía/
AI/consistencia/productividad/accesibilidad/escalabilidad) y `emil-design-eng` (composición/
lenguaje visual/AAA/percepción/jerarquía). Conflictos entre ambas → resolver con justificación
técnica, nunca dos respuestas paralelas.

## Decisiones abiertas que arrastrar
- **D1** (U2): comparación dual en ultraancha → ¿automática o manual con sugerencia?
  Recomendado: manual con sugerencia (preserva P9). Cerrar antes del diseño visual.
- **D2** (U2): declarar un *tamaño mínimo soportado* (honestidad P15) en vez de comprimir hasta
  lo inservible. Cerrar antes del diseño visual.

## Registro de decisiones de canon (cerradas)
- **DC-1 — Fase 11 (2026-07-22): Renombrado de la Iteración 11.8.** La iteración inicialmente
  denominada **"Snapshots & Replays"** pasa a llamarse **"QA & Regression Lab"**. El nuevo nombre
  refleja su responsabilidad arquitectónica (aseguramiento de calidad, preservación y validación del
  comportamiento del motor real), mientras que **Snapshots** y **Replays** permanecen como capacidades
  internas del Lab. La decisión amplía el alcance del Lab —permitiendo incorporar futuras pruebas
  automáticas de regresión— sin fragmentar la arquitectura en varios Labs pequeños con
  responsabilidades solapadas. No fue un cambio arbitrario. Ref: `screens/11.8-qa-regression-lab.md`.
- **DC-2 — Fase 11 (2026-07-22): Alcance de "Build" en el Build & Validation Lab (11.10).** El término
  **"Build"** designa exclusivamente la **preparación y validación previa** del proyecto (integridad de
  recursos, referencias, IDs, metadatos, dependencias, versiones) y la generación de **reportes
  técnicos**. **No** incluye la **compilación física del APK** (Gradle, empaquetado, firma,
  instaladores): eso depende de la infraestructura de desarrollo y puede cambiar con el tiempo, por lo que
  se mantiene **fuera** del canon del Lab. El Lab **prepara y valida**; la compilación real es un paso
  externo posterior. Ref: `screens/11.10-build-validation-lab.md`.
- **DC-3 — Fase 11 CERRADA (2026-07-22).** Tras la **Auditoría Global de Arquitectura**
  (`screens/11.11-auditoria-global.md`), que **no** detectó ningún ERROR DE ARQUITECTURA, se declara
  **oficialmente cerrada la Fase 11** (Screen Design del Developer Studio: Shell + 9 Labs + auditoría
  transversal). La arquitectura funcional del Studio queda **incorporada al canon y congelada**. En
  adelante, **cualquier cambio estructural del Studio** (nuevo Lab, nueva pantalla, alteración de una
  iteración cerrada, o modificación del contrato host/ID estable) debe **justificarse formalmente como
  cualquier fase canónica** —con su auditoría de suficiencia del Design System y de Regla de Oro— antes de
  incorporarse. Ref: `screens/README.md`.
- **DC-4 — Fase Puente Diseño→Desarrollo (2026-07-22): DMI aprobado.** Se crea el **Documento Maestro de
  Implementación** (`../DMI-DOCUMENTO-MAESTRO-IMPLEMENTACION.md`) como **contrato oficial entre el diseño
  congelado (Fases 0–11) y el desarrollo**. **No** se numera como "Fase 12": no introduce mecánicas de
  juego ni decisiones de diseño; es una **fase puente**. Define objetivos, principios, orden oficial de
  construcción (fundación-primero, distinto del orden de diseño), mapa de dependencias anclado al grafo
  Gradle real, hitos H0–H7 con criterios de aceptación, y estrategias de CI/validación/pruebas/
  migraciones/preservación/compatibilidad + riesgos R1–R10. Auditoría final: **sin contradicciones** con
  el canon. A partir de aquí, toda implementación sigue el DMI.
