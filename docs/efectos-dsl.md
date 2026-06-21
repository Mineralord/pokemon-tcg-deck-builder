# DSL de Efectos — Especificación (Fase 1a)

Lenguaje de **datos** para describir el efecto de un ataque, habilidad, entrenador, energía o
estadio sin escribir JavaScript por carta. Un intérprete (`js/efectos-motor.js`) ejecuta estos datos
contra el estado del juego (`js/juego.js`). Para reglas únicas que el DSL no cubra, cada carta puede
declarar un escape `efectoJS(ctx)` (enfoque **híbrido** acordado).

Objetivos: determinista (mismo estado + misma semilla → mismo resultado, para online), serializable,
testeable en Node, y legible para autoría masiva.

---

## 1. Forma de un efecto

```js
// data/efectos-db.js  →  EFECTOS_DB[cardId]
{
  ataques: {
    'Thunderbolt': {
      ops: [ /* lista ordenada de operaciones */ ],
      // efectoJS: function(ctx){...}   // escape opcional, tiene prioridad sobre ops
    }
  },
  habilidades: {
    'Static': { pasivos: [ /* modificadores estáticos */ ], /* o */ ops: [...] }
  },
  jugar: { ops: [...] },     // entrenador (item/supporter/estadio/tool al jugarse)
  pasivos: [ ... ]           // tool/estadio/energía especial: modificadores siempre activos
}
```

- **`ops`**: se ejecutan en orden cuando se resuelve el ataque/habilidad/jugada. El daño base del
  ataque (de `parseAtaque`) ya se aplicó antes de los `ops`; estos lo modifican o añaden efectos.
- **`pasivos`**: no se "ejecutan"; los lee la capa de stats derivadas (Fase 3) en cada lectura del
  tablero.
- **`efectoJS(ctx)`**: función de escape. Si existe, el intérprete la llama y **omite** `ops`.

---

## 2. Contexto de ejecución (`ctx`)

El intérprete construye y pasa a cada op un contexto:

| Campo        | Significado                                                              |
|--------------|--------------------------------------------------------------------------|
| `est`        | Estado de la partida (mutável, igual que en `juego.js`).                  |
| `lado`       | `'A'`/`'B'` — el dueño del efecto (quien ataca/usa la habilidad/juega).   |
| `op` (rival) | Lado contrario.                                                          |
| `at`         | Pokémon que ejecuta (Activo propio en ataques).                          |
| `def`        | Pokémon defensor (Activo rival en ataques).                              |
| `fuente`     | Carta origen del efecto (para `esteP`, descartes de costo, etc.).        |
| `flip()`     | Moneda determinista (`true`=cara). Reusa la moneda serializable del estado.|
| `pedir(req)` | Solicita una elección al jugador (Fase 2). En Fase 1 los ops no la usan.  |

---

## 3. Vocabulario de `objetivo`

Selector de a qué Pokémon/zona apunta una op.

| Valor              | Resuelve a                                              |
|--------------------|--------------------------------------------------------|
| `esteP`            | El propio Pokémon que ejecuta (`at`/`fuente`).         |
| `propioActivo`     | Activo propio.                                          |
| `rivalActivo`      | Activo del rival (`def`).                               |
| `propiaBanca`      | Lista: banca propia.                                    |
| `rivalBanca`       | Lista: banca del rival.                                 |
| `propioTodos`      | Lista: activo + banca propios.                          |
| `rivalTodos`       | Lista: activo + banca del rival.                        |
| `propioMazo`       | Zona: mazo propio.                                      |
| `propioDescarte`   | Zona: descarte propio.                                  |
| `propiaMano`       | Zona: mano propia.                                      |
| `elegido`          | Resultado de una elección previa (Fase 2).             |

Las ops de daño/curación que reciben una *lista* y no admiten elección aplican a todos; cuando se
requiere "1 de ellos", el objetivo se acompaña de `elegir:true` (Fase 2).

---

## 4. `filtro` (opcional)

Restringe los Pokémon/cartas válidos de un objetivo-lista o de una búsqueda.

```js
{ tipo:'Lightning', etapa:0, nombre:'Pikachu', esBasico:true,
  supertipo:'Pokemon', tieneDanio:true, conEnergia:true }
```

Todas las claves presentes deben cumplirse (AND). Ausente = sin restricción.

---

## 5. `condicion` (opcional)

Hace que una op se ejecute solo si se cumple. Forma `{ tipo, ... }`:

| `tipo`           | Parámetros           | Verdadero si…                                         |
|------------------|----------------------|--------------------------------------------------------|
| `coin`           | —                    | sale cara (consume una moneda).                        |
| `tieneDanio`     | `objetivo`           | el objetivo tiene ≥1 contador de daño.                 |
| `tieneCondicion` | `objetivo`,`estado`  | el objetivo tiene esa condición especial.              |
| `cuenta`         | `objetivo`,`op`,`n`  | comparación numérica del tamaño de una lista/zona.     |
| `primerTurno`    | —                    | es el primer turno del dueño.                          |

---

## 6. Catálogo cerrado de `op` (Fase 1: deterministas, sin elección)

Cada op es `{ op:'<nombre>', ...params }`. Todas aceptan `condicion` opcional.

| `op`              | Parámetros                              | Efecto                                                              |
|-------------------|------------------------------------------|--------------------------------------------------------------------|
| `danio`           | `objetivo`, `cantidad`                   | Suma daño directo (ignora debilidad/resistencia, "snipe").         |
| `danioExtra`      | `cantidad` \| `porCada`                  | Añade daño al `def` del ataque actual (sí aplicado tras base).     |
| `recoil`          | `cantidad`                               | Daño al propio `esteP`.                                             |
| `estado`          | `objetivo`, `estado` (str o array)       | Aplica condición(es) especiales.                                   |
| `quitarEstado`    | `objetivo`, `estado` \| `todas:true`     | Retira condición(es).                                              |
| `curar`           | `objetivo`, `cantidad` \| `todo:true`    | Cura daño.                                                         |
| `descartarEnergia`| `objetivo`, `cantidad` \| `todo:true`    | Descarta energías del objetivo (de su dueño).                      |
| `robar`           | `cantidad`                               | El dueño roba N de su mazo (deckout no se fuerza aquí).            |
| `descartarMano`   | `cantidad` \| `todo:true`                | El dueño descarta de su mano (sin elección: desde el final).      |

### 6.1 `cantidad` y `porCada`

`cantidad` es un entero. Alternativamente, daño/curación escalables usan `porCada`:

```js
{ op:'danioExtra', porCada:{ objetivo:'rivalBanca', op:'cuenta', multiplica:30 } }
{ op:'danioExtra', porCada:{ objetivo:'esteP', op:'contadores', multiplica:20 } } // daño/10
{ op:'danioExtra', porCada:{ objetivo:'rivalActivo', op:'energias', multiplica:10 } }
```

`op` de `porCada`: `cuenta` (tamaño de lista), `energias`, `contadores` (daño/10).

### 6.2 Monedas

- Condición `{tipo:'coin'}` en una op → se ejecuta solo si cara.
- Escalado por monedas se modela con ops repetidas o `porCada` con `op:'caras'` (Fase 1d) usando un
  `flip` interno: `{ op:'danioExtra', porCada:{ op:'carasHasta Cruz', multiplica:N } }` y
  `{ ...op:'carasDe', monedas:N, multiplica:M }`.

---

## 7. `pasivos` (se detallan en Fase 3, aquí solo el formato)

Modificadores leídos por la capa de stats. Forma `{ mod, ...}`:

```js
{ mod:'reduceDanio', cantidad:20, a:'propioTodos' }     // -20 al daño entrante
{ mod:'hpExtra', cantidad:30, a:'esteP' }
{ mod:'costoRetiro', set:0, a:'esteP' }
{ mod:'inmuneEstado', a:'propioActivo' }
{ mod:'proveeEnergia', tipos:['Fire','Water'], a:'esteP' } // energía especial
```

La Fase 1 valida su forma pero **no** los ejecuta (eso es Fase 3).

---

## 8. Reglas de validación (Fase 1b)

`EFECTOS_DSL.validar(def)` devuelve `{ ok, errores[] }`:

1. `op` debe estar en el catálogo cerrado.
2. `objetivo`/`filtro`/`condicion` deben usar vocabulario conocido.
3. Daño/curación requieren `cantidad` **o** `porCada` (no ambos, no ninguno).
4. `efectoJS` debe ser función si está presente.
5. `pasivos[].mod` debe estar en el catálogo de mods.

Las cartas con `op` o `mod` desconocidos no rompen el juego: el motor cae al **respaldo manual
asistido** existente y lo registra en la métrica de cobertura (Fase 7d).

---

## 9. Ejemplos

```js
// Ataque: 150 + Confundido y Envenenado al rival
'Dangerous Toxwhip': { ops: [ { op:'estado', objetivo:'rivalActivo', estado:['confused','poisoned'] } ] }

// Ataque: 60, +100 si este Pokémon tiene daño
'Brave Wing': { ops: [ { op:'danioExtra', cantidad:100, condicion:{ tipo:'tieneDanio', objetivo:'esteP' } } ] }

// Ataque: 330, descarta 3 energías de este Pokémon
'Explosive Vortex': { ops: [ { op:'descartarEnergia', objetivo:'esteP', cantidad:3 } ] }

// Ataque: 50% para dejar Dormido al rival
'Sing': { ops: [ { op:'estado', objetivo:'rivalActivo', estado:'asleep', condicion:{ tipo:'coin' } } ] }

// Habilidad pasiva: -20 al daño entrante en banca (ejemplo)
'Aura Veil': { pasivos: [ { mod:'reduceDanio', cantidad:20, a:'propiaBanca' } ] }
```
