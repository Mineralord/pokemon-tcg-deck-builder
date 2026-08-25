# Réplica "Mis barajas" + Editor (TCG Live) — Hoja de ruta

Ingeniería inversa 1:1 de la sección **Barajas** de Pokémon TCG Live, a partir de los
videos de referencia. Se implementa **por partes**, un comportamiento por vez.

- Design system claro muestreado del video en `core/designsystem/BarajasPalette.kt`.
- Pantalla: `feature/decks/.../DecksScreen.kt`.
- Videos ref (vidref sources): `5`=BARAJAS PRIMERA VISTA, `7`=VIDEO 2, `8`=VIDEO 3.
- **Regla del proyecto: tamaño de mazo = 60/60** (NO 20 de TCG Pocket). El chip y el
  editor muestran `n/60`.

## Estado

### ✅ Hecho
- **Mis barajas (chasis)**: cabecera (título + Editar + `?`), hilo divisor arcoíris,
  chip `n/50` de barajas, fondo claro, rejilla 2 col, celda "Crear nueva".
- **Tile de baraja**: cinta de acento numerada (`01`…), caja 3D con carta destacada
  (Coil), emblema de tipo, carta secundaria asomando, nombre en panel inferior.
  Acento = tipo dominante (pendiente: elección de portada por el usuario).
- **Navegación**: barra inferior 5 iconos + controles flotantes (chevrons, ↩ a Inicio),
  auto-ocultado ligado al arrastre.
- **Modo edición (A)**: cabecera "Edición en curso", badge `−` rojo por tile, botón
  "Guardar" cian.
- **Diálogo "Eliminar baraja" (B)**: tarjeta + Cancelar / Vale (rojo-outline).
- Máximo de barajas: 50 (chip `n/50`).
- **Menú de ayuda `?` (#1)**: popup anclado arriba-derecha con "Ver guía" (→ carrusel
  tutorial de diapositivas con puntos de página + Siguiente/Entendido) y "Guía en vídeo
  sobre los controles" (→ reproductor placeholder). Cierra al tocar fuera.

- **Menú "Crear" (#2)**: al pulsar la celda `+`, popup con fila-cabecera oscura
  (`MenuHeaderBg`) "Crear nueva baraja" (→ crea baraja vacía y abre editor) + 3 opciones
  (*Crear a partir de baraja temática · Crear una copia de una baraja · Escanear código*),
  estas tres con `PlaceholderDialog` "Próximamente" hasta implementar sus sub-flujos.

- **Diálogo "Salir sin guardar" (#3)**: al salir del editor con cambios (`state.dirty`),
  pide confirmación. **"Vale" cian-relleno** descarta y restaura el snapshot inicial
  (`DeckEditorViewModel.discardChanges`). Refactor: `DialogButton` (variante `filled`),
  `ConfirmDialog` y diálogos movidos a `DeckDialogs.kt`. NOTA: cableado al `BackHandler`
  del editor viejo; con el editor nuevo (#7, Cancelar/Guardar) se reutiliza igual.

- **Renombrar baraja (#5)**: `RenameDeckDialog` — campo de texto (máx. 22), ayuda
  "Máximo 22 caracteres.", aviso rojo "No utilices información personal.", "Vale" cian
  deshabilitado si vacío. Sustituye el AlertDialog Material del editor. `DialogButton`
  ahora soporta `enabled`. [Video 3]

- **Menú "…" de opciones (#6)**: `DeckOptionsMenu` anclado arriba-derecha (cabecera del
  editor): *Eliminar esta baraja · Ver todas las cartas de la baraja · Mostrar código ·
  Copiar esta baraja*. Eliminar→`ConfirmDialog`; Copiar→`duplicateDeck()` real + `InfoDialog`;
  Ver cartas / Mostrar código → `InfoDialog` "Próximamente". Sustituye el DropdownMenu
  Material del editor. Nuevos reutilizables: `DeckOptionsMenu`, `InfoDialog`. [Video 3, 00:56]
- **DESCUBIERTO** (sub-flujo de #2 "Crear una copia"): pantalla **"Elige qué baraja
  copiar"** = rejilla de barajas con check de selección + Cancelar/Copiar. [Video 3, 00:41]

- **Editor de baraja — chasis (#7)**: `DeckEditorScreen` reescrito al design system claro.
  Cabecera tintada con el **acento de la baraja** (`typeColor`), nombre + lápiz (→ renombrar),
  "…" (→ menú #6), estrella; caja `DeckBox` + "Modificar"; sub-paneles Energía/Accesorios/
  Cartas destacadas; chip `n/60` + Editar; rejilla 3-col (cartas actuales solo lectura + hueco
  "+"); pie Cancelar (→ Salir sin guardar si dirty) / Guardar (cian). [Video 3, 00:49–01:15]

- **Selector de cartas (#8)**: `DeckCardPicker` a pantalla completa (abre desde "Editar",
  huecos "+" y cartas del editor). Panel superior = mazo actual (toolbar "Quitar todas"→
  `clearDeck()`, "Autocreación"→Próximamente, zoom→Próximamente; rejilla 5-col, tocar quita).
  Chip `n/60` + toggle "3/5 columnas" + búsqueda(→Próximamente, #9). Panel inferior = colección
  (tocar añade si `canAdd`; badges de poseídas y copias en mazo; atenuadas si no se pueden
  añadir). Pie "Vale" cian. Cableado real a `addCard/removeCard/clearDeck`. [Video 3, 01:15+]

- **Panel de filtros + Expansiones (#9 y #10)**: se **reutiliza el `FilterSortSheet`
  existente** (el panel fiel de TCG Live, arte propio), abierto desde las lupas 🔍 del picker
  (que se iluminan si hay filtro activo). Cubre las dos pestañas **FILTROS/ORDENAR**:
  Categoría (supertipo), Tipo de energía + Debilidad (emblemas), Características y **sección
  EXPANSIÓN** (marcado por set) — es el "Selector de Expansiones" de #10. Botón rojo
  "VER N CARTAS" (con conteo en vivo vía `viewModel.countMatching`) + ✗. Aplica con
  `setFilter`/`setSort`. NO se crea panel nuevo: `DeckCardPicker` sólo aporta
  `onOpenFilters`/`filtersActive` y `DeckEditorScreen` monta el sheet ya existente.
  [Video 3, 02:38–02:56]

- **Autocreación (toolbar del selector)**: `AutoBuildDialog` — popup réplica de TCG Live
  ("Autocreación" + divisor + "Selecciona hasta dos tipos."), rejilla 2-col de los 10 tipos
  (emblema `TypeEmblem` + nombre en femenino concordando con "Energía"), selección de **hasta 2
  tipos**, pie Cancelar / **Vale** (cian, deshabilitado sin selección). Al confirmar,
  `DeckEditorViewModel.autoComplete(types)` **rellena el mazo hasta 60** con energías básicas de
  esos tipos repartidas en round-robin (no toca las cartas existentes) y muestra un `InfoDialog`
  con el nº añadido. Cableado real desde el pill "Autocreación" del `DeckCardPicker`.

### Política actual
**Todo lo aún NO implementado abre un pop-up "Próximamente"** (`InfoDialog` / `soon(area)`):
Modificar portada, sub-paneles (Energía/Accesorios/Cartas destacadas), Editar, añadir/editar
cartas, estrella (favorita), y las opciones secundarias del menú Crear (temática/copia/escanear)
y del menú "…" (ver cartas / mostrar código).

### ⏳ Pendiente
4. **Reordenar barajas por arrastre**: ⚠️ NO CONFIRMADO en los vídeos (el modo edición
   observado sólo tiene borrar + Guardar). Pendiente de decisión: implementar como función
   deseada, buscar más metraje, o descartar. [suposición inicial, sin evidencia]
6. **Menú de opciones "…"** de una baraja (cambiar nombre, duplicar, borrar, favorita…).
   [Video 3, 00:16–00:47]
7. **Editor de baraja (chasis)**: cabecera **verde**; píldora nombre + lápiz, `…`,
   estrella; caja 3D preview + "Modificar" (portada/acento); sub-paneles **Energía /
   Accesorios / Cartas destacadas**; chip `n/60` + "Editar"; rejilla 3 col de huecos `+`;
   pie **Cancelar / Guardar** (cian). [Video 3, 00:52–01:15]
8. **Selector de cartas** (añadir al mazo): toolbar **Quitar todas** (rojo) /
   **Autocreación** / lupa / zoom % (+/−); chip `n/60` + toggle "5 columnas" + buscador;
   rejilla de colección con contador de poseídas y badge `−`/copias (`1/2`) si está en el
   mazo; botón **Vale** cian. [Video 3, 01:15–02:38]
11. **Estadísticas / curva** del mazo (vista de nodos morada). [Video 3, 02:54–02:58]

> #9 (panel de filtros) y #10 (selector de expansiones) → **hechos** reutilizando
> `FilterSortSheet` (ver sección ✅ Hecho).

## Notas de diseño
- **Botón "Vale": dos variantes** — rojo-outline (destructivo: borrar) vs cian-relleno
  (confirmar: salir sin guardar). Parametrizar `DialogButton`.
- **Cabecera del editor**: gradiente verde (distinto del fondo azul-gris de la lista).
- Arte siempre **propio** (no assets de TPC): replicar silueta/lenguaje, no copiar.
- Acento por baraja = **elección del usuario** (portada). Hoy derivado del tipo dominante;
  conectar cuando exista la pantalla de portada.
</content>
