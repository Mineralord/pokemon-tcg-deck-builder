# Réplica visual de TCG Live — Spec (guía de diseño desde el vídeo de referencia)

> Basado en el análisis de los fotogramas del vídeo del usuario (`referencias/`, locales, no
> versionados). Se describe la **estructura, disposición y flujo** para recrear el look & feel con
> **assets ORIGINALES** propios. No se incrustan capturas ni arte de The Pokémon Company.

## Hallazgo clave: la batalla es en VERTICAL (portrait)
El cliente móvil de TCG Live (en este vídeo) juega la **batalla en vertical**, con la misma
orientación que nuestro juego: **rival arriba, tú abajo**. → Nuestra dirección correcta es pulir el
**modo vertical**, no forzar apaisado. (El layout horizontal queda como opción secundaria.)

## Tablero de batalla (portrait)
- **Tapete por lado:** rival con tapete **cálido (rojo oscuro)** y textura *acolchada en diagonal*;
  jugador con tapete **frío (azul)**, misma textura. Un **carril central** vertical conecta ambos
  lados (zona de Activo enfrentada). Borde del tapete con franjas finas (amarillo/morado) curvadas.
- **Disposición de cada lado** (rival reflejado del jugador):
  - **Banca:** fila de miniaturas de Pokémon (hasta 5) hacia un lado.
  - **Activo:** carta destacada en el centro-interior, enfrentada al Activo rival por el carril.
  - **Mazo y Descarte:** pilas a un costado; **Premios:** indicador con número (p. ej. "6").
  - **HP en carta:** se muestra como **número grande + icono de tipo** sobre la carta (p. ej. `190 🔥`).
- **HUD:**
  - Arriba-izquierda: **nombre del rival**. Arriba-derecha: **engranaje** (ajustes).
  - Lateral izquierdo: botones hexagonales de **rendirse** y **emote/chat**.
  - Barra inferior: botones de acción (confirmar ✓, cancelar ✗, registro/lista, inicio).
- **Banners de aviso** centrados, tipo cápsula clara con texto oscuro: "El primer turno es tuyo.",
  etc. (mensajes de fase/turno).

## Detalle de carta (panel inferior / bottom-sheet)
Al tocar una carta sube un **panel desde abajo** con: nombre + **HP**, **Habilidad** (etiqueta
morada con su texto), **ataques** (icono(s) de coste de energía + nombre + **daño** a la derecha),
y fila de **Debilidad / Resistencia / Retirada** con iconos de tipo. (Nuestro `jv-zoom` ya hace algo
parecido; alinear estilo: cápsulas, iconos de tipo redondos, daño grande.)

## Menús (referencia, NO se replican 1:1 por estilo propio)
- **Inicio:** cabecera con monedas/recursos, pestañas de modo, selector de formato con arte central,
  botón grande **JUGAR**, navegación inferior hexagonal (Cartadex, Barajas, Perfil, Tienda).
- **Cartadex:** rejilla de huecos por número de set, cartas poseídas a color y vacías atenuadas,
  selector de set, orden configurable.
> Nuestros menús (colección/constructor) ya cubren esto con identidad propia; no es prioridad copiarlos.

## Flujo de la partida (orden observado, para la coreografía)
1. Inicio de partida → **mensaje de quién empieza** ("El primer turno es tuyo").
2. **Preparación:** colocar Activo + Banca, 6 Premios.
3. Turno: **robar** → jugar (energía/entrenadores/evolución/banca) → **atacar**.
4. **Ataque:** resaltado del atacante, impacto en el defensor, **número de daño**, posible **KO**.
5. **KO:** se retira el Pokémon y se **toman Premios** (animación de premio).
6. **Cambio de turno** con banner.
7. Repetir hasta condición de victoria → **pantalla de fin**.

## Plan de recreación (assets originales) — prioridad vertical
1. **Tapete dual:** rival rojo cálido / jugador azul frío con textura diagonal y carril central
   (sustituye/ajusta el tapete teal actual). `css/juego.css`.
2. **HP en carta** como número grande + icono de tipo (badge). `js/juego-ui.js` (cardBadges) + CSS.
3. **Detalle de carta** estilo bottom-sheet con cápsulas/iconos de tipo. `zoomOverlay` + CSS.
4. **HUD:** nombre rival arriba-izq, engranaje/ajustes arriba-der, botones laterales (rendirse/
   emote), barra inferior de acciones. Reusar `closeBtn/muteBtn/jv-utils`.
5. **Banners de fase/turno** tipo cápsula (ya tenemos `jv-turnbanner`; alinear estilo).
6. **Coreografía** (ya iniciada): embestida, impacto, KO, vuelo de premio — afinar *timing* al vídeo.
7. **SFX** sintetizados acordes al *feel* (sin usar el audio del vídeo).

Se implementa **por tandas con feedback**, bump de Service Worker por despliegue.
