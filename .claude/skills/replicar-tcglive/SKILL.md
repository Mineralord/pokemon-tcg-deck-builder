---
name: replicar-tcglive
description: Metodología de ingeniería inversa 1:1 de Pokémon TCG Live para el clon (TCG-Live-Clone). Actívala SIEMPRE que el usuario pida "replicar", "reproducir", "estudiar" o "clonar" un comportamiento/componente de la interfaz de TCG Live a partir de los videos de referencia (Combate Completo 1 y 2). Cubre cómo consumir los videos (extractor vidref.py), cómo analizar antes de codear y cómo implementar con fidelidad.
---

# Replicar TCG Live 1:1

Actúa como **Ingeniero Principal de Software + Arquitecto de Videojuegos + Especialista UI/UX**
haciendo **ingeniería inversa** de Pokémon TCG Live para reproducir su comportamiento en el clon
(`TCG-Live-Clone`) con la mayor fidelidad posible.

## Fuente de verdad (única)
- **Corpus**: `COMBATE COMPLETO 1.mp4` y `COMBATE COMPLETO 2.mp4` en
  `C:\DOCUMENTOS\POKÉMON TCG\TCG LIVE VS MI APP CLON\POKEMON TCG LIVE\`
  (1080×2400, ~24 fps, ~13 min c/u).
- **Conjunto de trabajo activo = Combate 1.** Combate 2 se indexa/analiza **solo cuando el
  usuario lo pida** (no extraer sus frames por defecto).
- **Prohibido** proponer grabar la pantalla del teléfono en vivo o pedir dictado paso a paso
  (plan descartado por el usuario).
- No puedo "ver" un mp4: siempre trabajo con **frames PNG** extraídos con `vidref.py` (abajo).

## Reglas de trabajo
1. **Incremental**: un **único** componente/comportamiento por vez, **elegido por el usuario**.
   No avanzar al siguiente hasta que lo pida. Yo **no** elijo el comportamiento.
2. **Todas las ocurrencias**: no basarse en una sola aparición; localizar **cada** ocurrencia del
   comportamiento dentro del conjunto activo y usarlas todas para detectar patrones y variaciones.
3. **Fidelidad primero**: replicar la experiencia observada por encima de una solución más simple
   o distinta. Reproducir 1:1 es el **default**. Si el usuario pide una **modificación propia**,
   implementarla **manteniendo el estilo/lenguaje visual de TCG Live**.
4. **Incertidumbre explícita**: si un detalle no se determina con certeza en los frames, **decirlo
   antes de escribir código** y explicar qué frame/dato adicional haría falta. Nunca asumir un
   detalle incierto como hecho.
5. **Sin documentación independiente**: el conocimiento del análisis se aplica **directo al
   código** de la app. Nada de MDs de análisis aparte.

## Antes de tocar código (en este orden)
1. **Analizar** el comportamiento observado (no describir la pantalla: entenderlo).
2. **Identificar las reglas** que lo gobiernan.
3. **Comprender la interacción** entre los elementos implicados.
4. **Detectar estados, transiciones y restricciones.**
5. **Observar ritmo, animaciones y respuesta visual**: tiempos (ms), curvas/easing, secuencia,
   feedback (resaltados, escalas, sombras, sonido inferible por UI).
6. **Marcar lo incierto** (regla 4) y solo entonces diseñar/modificar el código.

## Cómo consumir los videos — `tools/scripts/vidref.py` (OpenCV en `tools/.venv`)
Pipeline de dos niveles (no puedo leer miles de frames):
```
# 1) Localizar ocurrencias + timestamps: hojas de contactos de todo Combate 1
py tools/scripts/vidref.py index 1                      # -> referencias_live/combate1/_index/sheet_*.png
# 2) Analizar a fondo un tramo: ráfaga full-res 1080x2400
py tools/scripts/vidref.py burst 1 --from 0:28 --to 0:34 --name <comportamiento> --fps 12
#    -> referencias_live/combate1/<comportamiento>/f_*.png   (leer estos con Read)
# 3) Comprobación puntual
py tools/scripts/vidref.py at 1 --time 0:30
```
Flujo típico por comportamiento: `index` (una vez) → leer hojas para ubicar TODAS las ocurrencias
→ `burst` en cada tramo → `Read` de los frames → análisis (pasos de arriba) → implementar.

## Implementación y verificación
- Aplicar al código del clon: motor en `engine/`, UI en `feature/game/` (p.ej. `GameScreen.kt`,
  `board/BoardGeometry.kt`). Regla del repo: **no duplicar lógica PvE/PvP** (comparten `GameCore`).
- Verificar fidelidad con el arnés existente (ver `tools/README.md`):
  `python -m tcgtools compare` (pixel/SSIM) y `layout-compare` (posición 1:1, espejo
  `refspec/board_start.json` ↔ `feature/game/.../board/BoardGeometry.kt`).
- Arte siempre **original propio**, nunca assets de TPC.
