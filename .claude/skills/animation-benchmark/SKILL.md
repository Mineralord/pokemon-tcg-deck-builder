---
name: animation-benchmark
description: Benchmark VISUAL obligatorio antes de crear cualquier animación importante en TCG-Live-Clone. Su propósito NO es buscar código, sino identificar, analizar y descomponer las mejores animaciones existentes (aunque sean propietarias) para definir el ESTÁNDAR de calidad visual que la app debe igualar. Actívala SIEMPRE antes de diseñar/implementar una animación (movimiento de cartas, ataques, energía, KO, holográficos, partículas, transiciones de cámara, UI premium) o cuando el usuario pida "referencia visual", "benchmark", "cómo lo hace Hearthstone/LoR/Marvel Snap", "qué queremos conseguir", "estándar de calidad". Primero entender la mejor experiencia visual; al terminar, encadena automáticamente con implementation-research para buscar la implementación técnica. Nunca empieces por el código.
---

# Animation Benchmark — entender la calidad antes de construir

Actúa como **director de arte técnico / VFX artist**. Este proyecto quiere ser la
versión **definitiva y permanente** de Pokémon TCG. Antes de tocar código, tu trabajo es
estudiar las mejores animaciones que existen —incluso propietarias— para entender **por
qué transmiten calidad** y fijar el listón. No buscamos copiar: buscamos comprender.

## Regla de oro
**Primero entender → después investigar → finalmente integrar.** No crear una animación
desde cero mientras exista una referencia visual claramente superior sin analizar. El
desarrollo propio es el último recurso.

## Distinción con las otras skills
- `animation-benchmark` (esta): **QUÉ queremos conseguir** (estándar visual). No busca código.
- `implementation-research`: **CÓMO conseguirlo** (mejor implementación open source).
- `revision-critica`: valida la arquitectura del diseño resultante.

## Orden OBLIGATORIO (no saltar pasos)
1. **Identificar** las mejores referencias visuales.
2. **Analizar** técnicamente cada una.
3. **Descomponer** la animación en componentes.
4. **Identificar** qué partes aportan realmente calidad (y cuáles son ruido).
5. **Definir** el estándar visual objetivo.
6. **Solo entonces**, encadenar con `implementation-research`.

## Fuentes
- **Juegos comerciales**: Pokémon TCG Live, Hearthstone, Legends of Runeterra, Marvel
  Snap, Magic Arena, Gwent, Shadowverse, Yu-Gi-Oh Master Duel, Balatro, Slay the Spire,
  Inscryption, y cualquier otro de calidad excepcional.
- **Material audiovisual**: vídeos oficiales, trailers, gameplays, **GDC**, **SIGGRAPH**,
  charlas técnicas, entrevistas a desarrolladores, breakdowns.
- **Comunidad**: Reddit, ArtStation, Behance, Dribbble, foros, blogs de VFX.
- Usa `WebSearch`/`WebFetch`. Aprovecha los vídeos de referencia locales del proyecto
  (skill `replicar-tcglive`, extractor `vidref.py`) para TCG Live. Registra las fuentes.

## Ficha por referencia
**Identificación**: juego · contexto (qué acción) · vídeo/URL · captura · duración aprox.

**Descomposición visual** (marca exactamente qué ocurre): movimiento · aceleración ·
desaceleración · partículas · blur · glow · escalado · rotación · shaders · reflejos ·
profundidad · iluminación · sonido · vibración/haptics · cámara · timing · composición.

**Calidad** (evalúa): claridad · legibilidad · espectacularidad · sensación de impacto ·
cohesión · pulido · identidad visual.

**Complejidad**: Baja · Media · Alta · AAA.

**Reutilización**: reutilizable completamente · parcialmente · solo inspiración.

## Comparación obligatoria
**Nunca** analices una sola animación. Compara siempre varias del mismo tipo de efecto y
**justifica por qué una es superior a otra** (timing, impacto, legibilidad, cohesión).

## Clasificación (tier)
- **S** — Referencia absoluta. Debe ser el objetivo del proyecto.
- **A** — Excelente. Muy recomendable.
- **B** — Buena. Sirve de inspiración.
- **C** — Aceptable. Solo estudiar.
- **D** — Descartar.

## Resultado obligatorio (antes de terminar)
1. **Benchmark visual** — ordenado de la mejor referencia a la peor.
2. **Estándar visual** — cuál intentaremos igualar (concreto y medible: timing, capas,
   feel).
3. **Lista de técnicas** — qué técnicas aparecen repetidamente entre las referencias top.
4. **Recomendación** — qué componentes reproducir y cuáles ignorar.

## Regla de honestidad
No infles con referencias mediocres ni clasifiques por hype. Si no puedes ver un vídeo,
dilo y razona desde descripciones/breakdowns fiables en vez de inventar detalles. La
espectacularidad nunca debe sacrificar **claridad y legibilidad** del estado de juego.

## Encadenamiento OBLIGATORIO
Al terminar el benchmark visual, **invoca automáticamente `implementation-research`** con
el estándar visual definido como objetivo: su misión es encontrar implementaciones reales
(open source, compatibles) que permitan alcanzarlo. Nunca empieces implementando código.

## Flujo permanente de toda animación nueva del proyecto
```
animation-benchmark  (QUÉ: estándar visual)
        ↓
implementation-research  (CÓMO: mejor implementación compatible)
        ↓
Integración con Animation Framework v1.0  (RenderNode + Executor + Definition + NodeRenderer + capa)
        ↓
Solo si es imprescindible: desarrollo propio
```
