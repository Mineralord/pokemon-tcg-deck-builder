# 🎬 STUDIO · CATÁLOGO CANÓNICO DE EVENTOS (v1 — CERRADO)

> **Fuente oficial de verdad** de la experiencia cinematográfica del proyecto. Pertenece al **Studio**
> (no al motor, ni al AnimationDirector, ni al renderer). Es el **vocabulario** con el que se investiga,
> diseña, implementa, documenta y discute TODO evento visual del juego.
>
> **Los IDs son PERMANENTES.** Nunca cambian, aunque cambie el nombre visible, la implementación o la
> dirección artística. (Ej.: `E2` sigue siendo `E2` aunque "Volado" pase a llamarse "Lanzamiento de Moneda".)

## Cómo usar este documento
- Para referirse a un evento, usar su **ID** (`V0.1`, `E2`, `C4`, `K5`…).
- **Comando oficial:** *"Usa las Skills para investigar las mejores referencias AAA para el Evento `<ID>`."*
  → Claude identifica el evento aquí y genera su **Research Package** siguiendo la Metodología Oficial (abajo).
- El **Tablero de Progreso** (sección 1) es lo vivo: se actualiza a medida que avanzamos. El **Catálogo**
  (sección 2) es estable: solo cambia si se añaden eventos nuevos (nunca se renumeran los existentes).

## Leyendas
- **Bloques:** Preparación · Turno · Acciones · Combate · KO · Premios · Estados · Utilidades · Final · Post-Partida.
- **Workflow:** `PENDIENTE → INVESTIGACIÓN → DISEÑO → IMPLEMENTACIÓN → ITERACIÓN → CANON`.
- **Madurez (calidad):** `CONCEPTO → STORYBOARD → PROTOTIPO → PLAYABLE → PULIDO → AAA → CANON`.
- **Research Package:** `Pendiente → Investigando → Referencias aprobadas → Cerrado`.
- **Prioridad cinematográfica:** ★ (mínima) … ★★★★★ (clímax).
- **Owner (disciplinas principales):** Camera · Lighting · Audio · VFX · UI · Gameplay · Shared.

## 🔒 Regla absoluta
Nunca investigar dos eventos a la vez. Nunca implementar dos a la vez. **No se avanza al siguiente evento
hasta que el actual sea `CANON`.**

---

## 1) 🟢🟡⚪ TABLERO DE PROGRESO (vivo)

**EVENTO ACTIVO:** `V0.1` — Entrada al combate.

**Por defecto, todo evento no listado abajo está en:** Workflow `PENDIENTE` · Madurez `CONCEPTO` · Research `Pendiente`.

| ID | Workflow | Madurez | Research Package | Notas |
|----|----------|---------|------------------|-------|
| **V0.1** | 🟡 DISEÑO | STORYBOARD | Referencias aprobadas | Estructura de 5 beats + lenguaje "Elegancia Cinematográfica" + 6 Principios fijados. **5 propuestas redactadas** (Umbral de Luz · Descenso · Materialización · Enfoque · Cruce Diegético); pendiente tu OK para implementar las 5 en el Studio. |

*(A medida que un evento avance, se añade/actualiza aquí su fila. Cuando llegue a CANON, se marca 🟢.)*

### Canon transversal ya fijado (aplica a todos los eventos)
- **Lenguaje base:** **"Elegancia Cinematográfica"** — peso, precisión, claridad, intención, elegancia; contención que **acelera en los clímax** (KO, Evolución, Revelación, Victoria).
- **6 Principios Maestros de Dirección:** (1) toda acción importante se anuncia; (2) un solo foco visual; (3) la cámara nunca duda; (4) claridad sobre espectáculo; (5) los VFX acompañan, no dominan; (6) toda transición prepara el siguiente evento (secuencia continua).
- **Regla emocional permanente (por evento):** responder *"¿Qué debe sentir exactamente el jugador al terminar este evento?"* antes de diseñar.
- **Música:** aún NO existe identidad musical; hasta entonces se diseña solo con **diseño sonoro ambiental** (ambiente, whoosh, aire, resonancias).

---

## 2) 📚 CATÁLOGO (estable)

### 🟦 Bloque PREPARACIÓN
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| V0.1 | Entrada al combate | Transición del menú al espacio de duelo | (Menú) → V0.2 | ★★★★ | Shared |
| V0.2 | Formación del tablero | El tapete y las zonas se materializan | V0.1 → V0.3 | ★★★ | Camera/VFX |
| V0.3 | Presentación del escenario | La cámara y la luz descubren el recinto | V0.2 → V0.4 | ★★★★ | Camera/Lighting |
| V0.4 | Presentación de jugadores | Entran avatares, nombres e indicadores | V0.3 → V0.5 | ★★★★ | UI/Camera |
| V0.5 | Aparición de los mazos | Los mazos se colocan con peso en sus zonas | V0.4 → E1 | ★★★ | VFX/Camera |
| E1 | Saludo | Gesto inicial entre contendientes | V0.5 → E2 | ★★ | UI/Gameplay |
| E2 | Volado | Moneda; el ganador elige jugar 1º o 2º | E1 → E3 | ★★★★★ | Shared |
| E3 | Reparto de la mano inicial | Cada jugador roba sus 7 cartas | E2 → E4 | ★★★★ | VFX/Camera |
| E4 | Comprobación de Básicos | Verificación de Pokémon Básico en la mano | E3 → E5*/E6 | ★★ | UI/Gameplay |
| E5 | Mulligan | Revelar, rebarajar y robar; robo extra del rival | E4 → E6 (bucle) | ★★★ | UI/VFX |
| E6 | Colocación del Pokémon Activo | Un Básico boca abajo en el Activo | E4/E5 → E7 | ★★★ | VFX/Gameplay |
| E7 | Colocación de la Banca | Hasta 5 Básicos boca abajo en la Banca | E6 → E8 | ★★★ | VFX/Gameplay |
| E8 | Reparto de Cartas Premio | 6 cartas Premio boca abajo | E7 → E9 | ★★★★ | VFX/Camera |
| E9 | Revelación simultánea | Ambos voltean Activo y Banca a la vez | E8 → V10 | ★★★★★ | Shared |
| V10 | Entrega del control | El HUD se activa; el foco pasa al jugador inicial | E9 → T1 | ★★★ | UI/Camera |

### 🟩 Bloque TURNO
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| T1 | Inicio de turno | El lado activo se enciende; anuncio "tu turno" | V10/T6 → T2 | ★★★ | Lighting/UI |
| T2 | Robo del turno | Se roba 1 carta al inicio del turno | T1 → T3 | ★★★ | VFX |
| T3 | Entrada a fase de acciones | Se activa el HUD de acciones | T2 → (A*/C*) | ★★ | UI |
| T4 | Declaración de fin de turno | El jugador confirma el fin de su turno | (A*/C*) → T5 | ★★ | UI |
| T5 | Chequeo Pokémon (Checkup) | Resolución entre turnos (estados, efectos) | T4 → T6 | ★★★ | VFX/UI |
| T6 | Cambio de turno | Transición de foco al rival | T5 → T1 (rival) | ★★★ | Lighting/Camera |
| T7 | Turno del rival (telegrafía) | Presentación acelerada de las acciones del rival | T6 → (A*/C* rival) | ★★★ | Shared |
| T8 | Robo imposible / mazo vacío | No se puede robar al inicio → fin de partida | T1 → G2 | ★★★★ | Camera/UI |

### 🟨 Bloque ACCIONES *(Contextual: en cualquier orden dentro de la fase de acciones)*
| ID | Nombre | Descripción | Prio | Owner |
|----|--------|-------------|------|-------|
| A1 | Poner Básico en Banca | Una carta Básica de la mano entra a la Banca | ★★★ | VFX |
| A2 | Evolución | Una carta de Evolución se coloca sobre su preevolución | ★★★★★ | VFX/Camera |
| A3 | Unir Energía | Una Energía se adjunta a un Pokémon (1/turno) | ★★★ | VFX/Audio |
| A4 | Jugar Objeto (Item) | Se juega un Objeto y se resuelve su efecto | ★★★ | VFX/UI |
| A5 | Jugar Partidario (Supporter) | Se juega un Partidario (1/turno) | ★★★ | VFX/UI |
| A6 | Jugar Estadio (Stadium) | Un Estadio entra en juego y cambia el ambiente | ★★★★ | Lighting/VFX |
| A7 | Anclar Herramienta (Tool) | Una Herramienta se ancla a un Pokémon | ★★ | VFX/UI |
| A8 | Usar Habilidad | Se activa una Habilidad de un Pokémon | ★★★★ | VFX/Camera |
| A9 | Retirada | El Activo se retira a la Banca; entra otro | ★★★ | VFX/Camera |

### 🟥 Bloque COMBATE (cadena del ataque)
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| C1 | Declaración de ataque | Se anuncia el ataque (comprobación de Energía) | (Acciones) → C2 | ★★★ | UI/Camera |
| C2 | Selección de objetivo | Se resaltan/eligen objetivos válidos | C1 → C3 | ★★★ | UI |
| C3 | Anticipación del ataque | Wind-up/carga del atacante | C2 → C4 | ★★★★ | Camera/VFX |
| C4 | Impacto | El ataque golpea (flash/shake/SFX) | C3 → C5 | ★★★★★ | Shared |
| C5 | Debilidad / Resistencia | Modificación del daño con acento | C4 → C6 | ★★★★ | VFX/UI |
| C6 | Daño / contadores | Contadores; baja el HP; números flotantes | C5 → K1*/C7 | ★★★★ | VFX/UI |
| C7 | Efectos secundarios | Efectos adicionales del texto del ataque | C6 → (K/S) | ★★★ | VFX |
| C8 | Ataque anulado | El ataque no se lleva a cabo (Confusión, moneda…) | Contextual | ★★ | VFX/UI |

### 🟪 Bloque KO
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| K1 | Marca de KO | Un Pokémon alcanza su HP: telegrafía/parpadeo | C6/T5 → K2 | ★★★★ | VFX/Camera |
| K2 | Desvanecimiento | El Pokémon noqueado se desvanece con peso | K1 → K3 | ★★★★★ | VFX/Camera/Audio |
| K3 | Envío al descarte | La carta y lo adjunto caen al descarte | K2 → P1 | ★★★ | VFX |
| K4 | Elección de nuevo Activo | El noqueado promueve un Pokémon de Banca | P1 → (retorno) | ★★★ | UI/Gameplay |
| K5 | KO decisivo | Variante enfatizada si el KO puede decidir la partida | C6 → P3/G | ★★★★★ | Shared |

### 🟫 Bloque PREMIOS
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| P1 | Toma de Carta Premio | Se toma 1 Premio a la mano tras un KO | K3 → P2 | ★★★★ | VFX/Camera |
| P2 | Actualización del marcador | El contador de Premios baja de forma destacada | P1 → (retorno)/G1 | ★★★★ | UI/VFX |
| P3 | Última Carta Premio | Variante clímax al tomar el Premio final | P1 → G1 | ★★★★★ | Shared |

### ⬛ Bloque ESTADOS *(Contextual, principalmente en Checkup T5)*
| ID | Nombre | Descripción | Prio | Owner |
|----|--------|-------------|------|-------|
| S1 | Envenenado | Aplicación/mantenimiento de Veneno | ★★ | VFX/UI |
| S2 | Quemado | Aplicación de Quemadura (daño + moneda) | ★★ | VFX/UI |
| S3 | Dormido | Rotación + zzz; moneda de recuperación | ★★ | VFX/UI |
| S4 | Paralizado | Rotación; recuperación tras el turno | ★★ | VFX/UI |
| S5 | Confundido | Rotación; moneda antes de atacar | ★★ | VFX/UI |
| S6 | Recuperación de estado | Un estado se cura (evolución/retirada/moneda) | ★★ | VFX |
| S7 | Curación | Se retira daño de un Pokémon | ★★★ | VFX |

### ⬜ Bloque UTILIDADES *(Reutilizable/Contextual)*
| ID | Nombre | Descripción | Prio | Owner |
|----|--------|-------------|------|-------|
| U1 | Robo de carta (genérico) | Movimiento mazo→mano reutilizable | ★★★ | VFX |
| U2 | Barajar | Animación de barajado | ★★ | VFX |
| U3 | Buscar en el mazo | Overlay de búsqueda (atenúa tablero, enfoca) | ★★★ | UI/Camera |
| U4 | Lanzamiento de moneda (en juego) | Coin flip para efectos de cartas | ★★★★ | VFX/Audio |
| U5 | Selección de objetivo (genérica) | Resaltado/elección de objetivos | ★★★ | UI |
| U6 | Confirmación / decisión | Prompt de decisión del jugador | ★★ | UI |
| U7 | Espera del rival | Indicador de "el rival está pensando" | ★★ | UI |
| U8 | Descarte de carta | Movimiento carta→descarte | ★★ | VFX |
| U9 | Movimiento entre zonas | Vuelo genérico de carta entre zonas | ★★★ | VFX |

### 🟡 Bloque FINAL
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| G1 | Victoria | Revelación del resultado ganador (clímax cálido) | P3/K/T8 → R1 | ★★★★★ | Shared |
| G2 | Derrota | Revelación de derrota (sobria, no humillante) | (condición) → R1 | ★★★★ | Shared |
| G3 | Empate | Resultado neutro y claro | (doble victoria) → G4/R1 | ★★★ | UI |
| G4 | Desempate (tiebreaker) | Nueva preparación de partida de desempate | G3 → V0.1 | ★★★ | Shared |

### 🟠 Bloque POST-PARTIDA *(meta; marcado "futuro")*
| ID | Nombre | Descripción | Anterior → Siguiente | Prio | Owner |
|----|--------|-------------|----------------------|------|-------|
| R1 | Pantalla de resultado | Transición a la pantalla final | G1/G2/G3 → R2 | ★★★ | UI |
| R2 | Recompensas | Entrega de recompensas con anticipación | R1 → R3 | ★★★★ | VFX/UI |
| R3 | Salida / nueva partida | CTA de continuar | R2 → (Menú/Nueva) | ★★ | UI |

---

## 3) 🛠️ METODOLOGÍA OFICIAL (por evento, siempre igual)
> **Principio de la metodología:** las decisiones cinematográficas se toman **comparando propuestas
> JUGABLES en el Studio**, nunca eligiendo sobre papel. Por eso **las 5 propuestas se implementan y se
> iteran a calidad AAA** antes de decidir cuál es CANON.

1. Seleccionar un evento (por ID).
2. Investigación exhaustiva con Skills (videojuegos AAA, TCG digitales, juegos de mesa digitales, cualquier dirección relevante).
3. Crear el **Research Package** (referencias, vídeos, capturas, análisis, decisiones, justificaciones, referencias descartadas).
4. Extraer únicamente **principios reutilizables** (nunca copiar/clonar/reproducir).
5. Diseñar **CINCO propuestas** distintas — cada una con: objetivo emocional · storyboard · beats · cámara · ritmo · iluminación · audio · VFX · transiciones · ventajas · desventajas · riesgos.
6. **Implementar las CINCO propuestas dentro del Studio**, todas reproducibles con Play (seleccionables como variantes del evento).
7. **Iterar las CINCO hasta calidad AAA** (sin límite de iteraciones; cada una debe poder juzgarse como candidata definitiva).
8. **Comparar las CINCO en el Studio** (cambio instantáneo entre variantes; reproducir, pausar, FF, slow-motion, repetir).
9. Recomendar una y **esperar tu elección**.
10. Marcar la elegida como **CANON**; las otras quedan **archivadas como variantes** (no se borran). Solo entonces se avanza al siguiente evento.

---
*v1 — Catálogo cerrado. Los IDs no cambian. Este archivo es la referencia permanente del proyecto.*
