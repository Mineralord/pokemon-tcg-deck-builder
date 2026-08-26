# Canon — Construcción de mazos (Autocreación inteligente)

Reglas y principios que **debe** respetar la herramienta de Autocreación del editor de
barajas. Fuente dura: `docs/canon/rulebook.txt` (Web Rulebook 2026, sección *Deck Building*,
pág. 22). Principios estratégicos: guías competitivas 2025-2026 (ver *Fuentes*).

## Reglas duras (rulebook — innegociables)

- El mazo tiene **exactamente 60 cartas** (ni 59 ni 61).
- **Máximo 4 copias por nombre**, salvo **Energía Básica** (ilimitada).
- Al menos **1 Pokémon Básico**.
- Sólo se pueden incluir cartas que el jugador **posee** (excepto energía básica, que en
  este proyecto es acuñable/ilimitada).
- Líneas de evolución: para jugar una evolución hay que tener el Básico (y la Fase 1 si es
  Fase 2). La herramienta nunca mete una evolución cuya línea no pueda completar.

## Principios estratégicos (guías competitivas)

- **1 o 2 tipos de energía** como foco (los elige el jugador en el popup).
- **1-2 Pokémon insignia** (atacantes principales) + Pokémon **acompañantes** de apoyo en
  banca (habilidades, arranque). Ejemplo del meta: atacante ex + apoyo de banca ex.
- **Regla de 4**: 4 copias de las cartas clave de robo/búsqueda para consistencia.
- **Líneas de evolución** por ratio:
  - Fase 1: **4-3** (4 Básicos, 3 Fase 1).
  - Fase 2: **4-3-2** o, con Rare Candy, **4-2-4**.
  - Líneas secundarias/tech: **3-2** o **2-2**.
- **Motor de consistencia** (Entrenadores): priorizar robo (Professor's Research, Iono),
  búsqueda (Ultra Ball, Nest Ball, Great Ball), cambio (Boss's Orders, Switch) y, si hay
  línea de Fase 2, Rare Candy.
- **Estadios/Herramientas** con moderación (pocas copias, Estadio único en campo).

## Perfiles de ratio (VARIEDAD)

La herramienta rota entre **varios perfiles** (elige uno al azar por creación) para dar
variedad, además de variar la insignia entre los mejores candidatos:

| Perfil        | Pokémon | Entrenador | Energía |
|---------------|:-------:|:----------:|:-------:|
| Competitivo   | 12-18   | 30-40      | 8-12    |
| Equilibrado   | 18-22   | 18-22      | 18-22   |
| Intermedio    | 15-20   | 24-32      | 10-15   |

En todos los casos el **total es 60**. Si el jugador no posee suficientes Entrenadores para
el perfil, el hueco se rellena con energía básica del foco (siempre disponible), garantizando
las 60 cartas independientemente de si tiene playset 2/4 o 4/4.

## Reglas de lógica (permanentes, válidas para cartas futuras)

- **Sólo cartas poseídas**: nunca se incluyen más copias de las que el jugador tiene en su
  colección — **incluida la energía básica** (aunque no tenga límite de 4, sí lo tiene por lo
  poseído). Si tienes 5 energías Lucha, el mazo usará como mucho 5.
- **Afinidad de tipo de los Entrenadores**: un Entrenador cuyo texto sólo beneficia a un tipo
  (p.ej. *Melo*→Fuego, *Generador Eléctrico*→Rayo) se **excluye** si ese tipo no está en el
  foco. La afinidad se **deriva del texto** de la carta (símbolos `{R}`/`{L}`… o `"Fire Energy"`,
  `"Lightning Pokémon"`…), así funciona automáticamente al incorporar cartas nuevas — no hay
  lista que mantener a mano.
- **Incoloro sólo de relleno**: con foco elemental, el mazo se llena con Pokémon de esos tipos;
  los Pokémon **Incoloros** sólo entran si **no hay suficientes** del foco en la colección. Si el
  jugador elige **COLORLESS** como uno de los tipos, los Incoloros pasan a ser de primera clase
  (para quien quiera un mazo incoloro a propósito).

## Fuentes

- TCG Protectors — *Pokémon TCG Deck Building Guide (2025): Ratios, Rules & Strategy*.
- KOOLTHINGS — *How to Build a Pokémon Deck for Beginners: 2026 Pro Guide*.
- Web Rulebook 2026 (`docs/canon/rulebook.txt`).
