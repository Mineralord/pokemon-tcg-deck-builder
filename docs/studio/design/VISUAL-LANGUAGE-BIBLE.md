# 📖 VISUAL LANGUAGE BIBLE — Pokémon TCG Clone

> **La Constitución del Lenguaje Visual del Proyecto.** No es solo un conjunto de principios: es un
> **sistema de diseño completo** — define el **idioma** (vocabulario, §1–§13), sus reglas de **coherencia y
> evolución** (§14), sus **prohibiciones** (§15) y su **gramática de composición y validación** (§16) — con
> el que hablarán TODAS las animaciones, paneles, efectos, cámaras, transiciones y feedback del juego, hoy
> y dentro de diez años. *(El nombre del archivo se conserva por reconocible; su alcance es el de una
> Design Constitution.)*
>
> **Es transversal:** no habla de eventos (ataque, KO, volado, premios). Habla del **lenguaje** que todos
> esos eventos usarán. Tiene **prioridad sobre cualquier decisión visual individual**.
>
> **Test de pertenencia (obligatorio antes de implementar cualquier elemento visual):**
> *"¿Está alineado con la Visual Language Bible?"* — Sí → pertenece al proyecto. No → se rediseña antes de
> implementar. No hay excepciones "por esta vez".

---

## 0) Identidad — el "norte" del idioma
**Nombre del lenguaje: «Elegancia Cinematográfica».**
Cinco palabras-ancla, en orden de prioridad cuando entren en conflicto:

1. **Claridad** — el estado de juego se entiende SIEMPRE. Es intocable.
2. **Intención** — nada existe porque "se ve bonito"; todo comunica, aclara o emociona.
3. **Peso** — el mundo tiene masa, materia y consecuencia; nada flota sin razón.
4. **Contención** — sobriedad por defecto; el espectáculo se **gana** y se reserva.
5. **Continuidad** — todo fluye; una partida es una sola secuencia, no una lista de efectos.

**Regla maestra de conflicto:** si dos principios chocan, **gana el de número más bajo**. La Claridad
vence al espectáculo siempre; la Intención vence a la decoración siempre.

**Sensación objetivo:** *"peso, precisión y elegancia; calma que se convierte en impacto cuando importa."*

---

## 1) MOVIMIENTO
El movimiento es **físico y con propósito**. Todo lo que se mueve tiene masa aparente.

**Reglas permanentes:**
1.1. **Entrar acelera, salir desacelera.** Los objetos ganan velocidad al aparecer/actuar (ease-in) y se
   asientan al detenerse (ease-out). El "settle" al final es obligatorio en todo movimiento con peso.
1.2. **Anticipación** solo antes de una acción **con consecuencia** (un objeto se "carga" antes de actuar).
   Nunca anticipación en microinteracciones ni en lo rutinario.
1.3. **Follow-through** en objetos con masa (una carta pesada continúa un instante tras llegar). Sutil,
   nunca caricaturesco.
1.4. **Rebote (overshoot) SÍ**: en aterrizajes satisfactorios y confirmaciones táctiles, **medido** (una
   sola oscilación, amplitud pequeña). **Rebote NO**: en texto, información crítica, HUD, o cualquier cosa
   que deba leerse de inmediato.
1.5. **Motion con propósito:** todo movimiento comunica un cambio de estado. Si no comunica nada, no se
   mueve.
1.6. **Coherencia de escala:** velocidades y distancias son **relativas al lienzo** (fracciones), nunca
   literales; un mismo gesto se siente igual en cualquier pantalla.
1.7. **Una intención por movimiento:** un movimiento hace una cosa. Movimientos compuestos = beats
   separados, no un batiburrillo simultáneo.

---

## 2) RITMO
El ritmo tiene **cuatro niveles oficiales**. Ningún elemento inventa el suyo.

| Nivel | Nombre | Duración | Sensación | Uso |
|---|---|---|---|---|
| **R0** | **Invisible** | ≤ 150 ms | No se percibe como animación | Rutina repetitiva, microfeedback |
| **R1** | **Fluido** | 150–350 ms | Ágil, natural | Acciones normales, UI |
| **R2** | **Enfatizado** | 350–700 ms | Tiene acento, "se nota" | Momentos con importancia |
| **R3** | **Cinemático** | 700 ms–1.6 s | Peso, respiración, dilatación del tiempo | Clímax (reservado) |

**Reglas permanentes:**
2.1. **Por defecto se usa el nivel más bajo** que cumpla la intención. Subir de nivel se **justifica**.
2.2. **Respira antes del clímax, nunca durante lo rutinario.** La pausa cargada (settle) es un recurso de
   R2/R3, jamás de R0/R1.
2.3. **Acelerar lo repetitivo** (lo que el jugador ve muchas veces), **desacelerar lo decisivo** (lo que
   ocurre pocas veces y pesa).
2.4. **Cero aire muerto:** ningún tramo sin progreso visual perceptible. Si algo tarda, debe *mostrar* que
   avanza.
2.5. **Saltable:** toda secuencia larga (R3 encadenados) es saltable tras la primera vez. El tiempo del
   jugador es sagrado.

---

## 3) CÁMARA
La cámara es un **director sereno**, no un participante nervioso.

**Movimientos PERMITIDOS** (siempre con intención y desaceleración final):
- **Push-in / pull-out** contenido (acercarse a un clímax, abrir para dar contexto).
- **Settle / reencuadre** sutil hacia el foco.
- **Rack-focus** (profundidad de campo) para dirigir la mirada.
- **Micro-parallax** ambiental (el escenario respira), imperceptible.

**Movimientos PROHIBIDOS:**
- **Shake de cámara como recurso libre** (el shake pertenece a §8, con escala; no es "mover cámara").
- **Barridos/paneos rápidos** sin motivo narrativo.
- **Zoom brusco** sin desaceleración.
- **Rotaciones/dutch tilt** salvo intención expresa y excepcional.
- **Cámara que persigue cada acción menor** (marea y roba claridad).

**Reglas permanentes:**
3.1. **La cámara nunca duda:** un movimiento, una dirección, una desaceleración. Sin correcciones a media
   trayectoria.
3.2. **Acercarse = importancia; alejarse = contexto.** El encuadre comunica jerarquía por sí mismo.
3.3. **Reposo por defecto:** en lo rutinario la cámara NO se mueve. El movimiento de cámara es un acento,
   no un estado.
3.4. **Acompaña, no protagoniza:** la cámara sirve a la lectura del tablero; si compite con ella, sobra.

---

## 4) ILUMINACIÓN — el «Umbral de Luz»
La luz es la **firma emocional** del juego: cálida, con propósito, reveladora.

**Reglas permanentes:**
4.1. **La luz revela, acompaña o acentúa** — nunca decora. Tres funciones, ninguna más.
4.2. **«Umbral de Luz»** es el gesto fundacional: la luz **descubre** (aparición) y **entrega** (cierre).
   Es la continuidad lumínica de todo el juego.
4.3. **Temperatura con significado:** cálida = vida, presencia, victoria, foco; fría/neutra = reposo,
   distancia, peligro contenido. La temperatura **comunica**, no es estética libre.
4.4. **Iluminar cuando algo nace o culmina** (aparición, clímax, entrega). **No iluminar** lo rutinario ni
   lo secundario: la luz gastada pierde valor.
4.5. **Un foco de luz por momento** (coherente con §12): la luz dirige la mirada a **un** sitio.
4.6. **La luz refuerza la emoción, no la sustituye:** acompaña al movimiento y al feedback, no va sola
   salvo por diseño explícito.

---

## 5) PROFUNDIDAD
El tablero es un **lugar**, no una imagen plana.

**Reglas permanentes:**
5.1. **Tres planos de lectura, siempre:** **Fondo/recinto** (más oscuro, ambiental) → **Tapete/juego**
   (el plano de acción, nítido) → **Overlay/foco** (lo que exige atención ahora).
5.2. **Profundidad por luz, sombra y foco**, no por deformación: viñeta, sombra propia de los objetos,
   profundidad de campo. Nunca a costa de la legibilidad del plano de juego.
5.3. **El plano de juego siempre nítido y legible.** La profundidad vive en el fondo y en el overlay, no
   en la información de partida.
5.4. **Separación de capas por contraste y elevación**, no por saturación de efectos. Cada capa tiene su
   rango de luminosidad; no se pisan.
5.5. **El escenario respira** (micro-parallax/ambiente sutil) para sentirse vivo, siempre por debajo del
   umbral de distracción.

---

## 6) MATERIALES Y PESO
Todo objeto tiene una **materialidad** reconocible. "Peso" = cómo un objeto responde a la fuerza.

**Reglas permanentes:**
6.1. **La carta** es el objeto rey: rígida, con canto y sombra propia; se toma, se posa y **aterriza con
   peso** (settle + follow-through leve). Nunca se desliza sin masa.
6.2. **Los paneles** son superficies **sólidas y elevadas**: entran/salen desde un borde con inercia,
   proyectan sombra, tienen esquinas y grosor coherentes. No son "capas de HTML".
6.3. **Los botones** responden al tacto: **press** (hunde/oscurece), **release** (rebote medido), estado
   claro (activo/inactivo). El tacto siempre confirma.
6.4. **La energía / los recursos** se sienten **cargados**: pequeños, densos, con brillo interior; se
   adjuntan con un "click" físico, no aparecen sin más.
6.5. **Definición de PESO (permanente):** un objeto tiene peso cuando (a) acelera y desacelera con inercia,
   (b) proyecta sombra, (c) tiene follow-through, y (d) produce feedback al detenerse. Cuanto más importa
   un objeto, más peso muestra.
6.6. **Consistencia de material:** el mismo tipo de objeto se comporta **igual** en todo el juego.

---

## 7) PARTÍCULAS
Las partículas son **lenguaje**, no confeti.

**Reglas permanentes:**
7.1. **Solo con significado:** una partícula comunica energía, impacto, materia o transformación. Si no
   comunica, **no existe**.
7.2. **Máximo dos capas** por efecto: **núcleo** (denso, foco) + **halo/estela** (disperso, ambiente).
   Nunca más — la tercera capa es ruido.
7.3. **Nacen del foco y mueren pronto:** aparecen donde ocurre la acción y se disuelven; nunca persisten
   como decoración de fondo.
7.4. **Intensidad por jerarquía:** rutina = ninguna o mínima; acento = núcleo; clímax = núcleo + halo.
   Escala con la importancia, nunca por defecto.
7.5. **Familia visual compartida:** todas las partículas del juego comparten forma, falloff suave y
   temperatura del lenguaje (cálida por defecto). Nada de estilos sueltos.
7.6. **Jamás tapan información** ni compiten con el foco de lectura.

---

## 8) SHAKE — escala oficial (nunca improvisar)
El shake es un **acento de impacto físico**, no un condimento. **Cuatro niveles, y solo cuatro.**

| Nivel | Amplitud | Duración | Uso |
|---|---|---|---|
| **S0 · Micro** | Imperceptible (~1–2 px rel.) | ≤ 80 ms | Confirmación táctil, "click" de recurso |
| **S1 · Normal** | Pequeña | ~120 ms | Acción con contacto, aterrizaje con peso |
| **S2 · Impacto** | Media, con hit-stop breve | ~180 ms | Golpe real, consecuencia fuerte |
| **S3 · Clímax** | Fuerte (una descarga, decae rápido) | ~250 ms + freeze | Momento decisivo (reservadísimo) |

**Reglas permanentes:**
8.1. **Prohibido el shake sin causa física.** Solo hay shake si algo **golpea, aterriza o culmina**.
8.2. **Decaimiento siempre:** el shake nace fuerte y **decae** a cero; nunca oscila plano.
8.3. **Dirección con sentido** cuando aplique (empuja en la dirección del impacto), no aleatorio puro.
8.4. **S3 es excepcional:** si todo tiembla como clímax, nada lo es. Presupuesto estricto.
8.5. **El shake nunca compromete la legibilidad** del estado (dura poco, decae, no borra información).
8.6. **Respeta reduced-motion:** con movimiento reducido, el shake se degrada a un flash/pulso equivalente.

---

## 9) TRANSICIONES
Todas pertenecen a **una sola familia**: revelan y entregan, nunca cortan en seco.

**Vocabulario oficial:**
- **Entrada (reveal):** aparece **desde la luz o desde un borde**, con ease-out de asentamiento.
- **Salida (dismiss):** se retira **hacia la luz o hacia su borde**, cediendo el foco; nunca "desaparece".
- **Handoff:** el final de un momento **prepara** el inicio del siguiente (el último fotograma = el primer
   fotograma en reposo del siguiente). Costura imperceptible.
- **Fundido:** cruzado y con propósito; nunca a negro salvo por diseño narrativo explícito.
- **Continuidad (match-cut):** si dos momentos comparten un elemento, ese elemento **persiste** entre ellos.

**Reglas permanentes:**
9.1. **Nunca un corte seco** entre estados importantes: siempre reveal/handoff.
9.2. **Toda transición prepara la siguiente** (continuidad; canon HANDOFF).
9.3. **Misma familia de curvas y duraciones** (§1, §2) en todas las transiciones del juego.
9.4. **La transición sirve a la lectura:** orienta, no despista; el jugador nunca "se pierde".

---

## 10) UI
Toda la interfaz debe sentirse **construida por el mismo diseñador**.

**Reglas permanentes (por familia):**
- **Paneles:** superficies elevadas con sombra y grosor; entran desde un borde con inercia; esquinas,
   márgenes y radios de una sola familia; jamás bloquean el tablero más de lo necesario.
- **Botones:** jerarquía clara (primario/secundario/terciario); estados inequívocos; feedback táctil
   (press/release) siempre; el destino de cada botón es predecible.
- **Diálogos:** aparecen con foco (scrim que atenúa el fondo, contenido que entra con acento R2); una sola
   decisión clara por diálogo; salida que devuelve el foco al tablero.
- **HUD:** **persistente, sobrio y periférico**; informa sin robar el centro; nunca compite con la acción;
   cambios de valor animados y legibles (nunca saltos bruscos ni ilegibles).
- **Tooltips:** aparecen a demanda, R0/R1, no intrusivos; se van solos.
- **Badges / indicadores:** pequeños, densos, con significado de color consistente; cambian con acento
   proporcional a su importancia.
- **Alertas:** claras, contenidas, temporales; jamás alarmistas ni permanentes; una alerta = un mensaje.

**Reglas transversales de UI:**
10.1. **Una sola familia** de tipografía, color, radio, sombra, espaciado y curvas (tokens del sistema).
10.2. **Jerarquía tipográfica** de 3–4 roles como máximo; nunca "otra fuente para esto".
10.3. **El color comunica** (tipo, bando, estado, peligro); nunca color decorativo sin significado.
10.4. **La UI cede ante la partida:** en la acción, la UI se aparta; nunca al revés.

---

## 11) FEEDBACK
El juego **responde a todo lo relevante**, con la **capa mínima suficiente**.

**Escala de capas de feedback (usar la mínima que comunique):**
1. **Solo movimiento** — cambios de posición/estado leves (R0/R1).
2. **Solo luz** — foco, disponibilidad, selección (un pulso, un glow).
3. **Movimiento + luz** — acciones normales con acento.
4. **Movimiento + luz + partícula + sonido (+ shake)** — clímax, por capas (§6.5, §8).

**Reglas permanentes:**
11.1. **Toda acción del jugador tiene respuesta inmediata** (≤ R1); la latencia percibida es enemiga.
11.2. **Feedback proporcional:** la magnitud de la respuesta = la importancia del suceso. Ni exagerar lo
   trivial ni apagar lo importante.
11.3. **Vocabulario consistente:** la misma acción produce **siempre** la misma respuesta. El jugador
   aprende el idioma una vez.
11.4. **El impacto AAA se compone por capas**, nunca de un solo efecto grande.
11.5. **Nunca feedback ambiguo:** si el jugador tiene que preguntarse "¿pasó algo?", el feedback falló.

---

## 12) JERARQUÍA VISUAL
En cada instante, **un solo foco**. La mirada se guía, no se dispersa.

**Reglas permanentes:**
12.1. **Un foco por momento** (canon "un foco por beat"). Si hay dos cosas importantes a la vez, se
   ordenan en el tiempo (beats), no en paralelo.
12.2. **Herramientas para crear foco (en orden de fuerza):** movimiento > luz/contraste > escala > color >
   posición. Se usa la mínima necesaria.
12.3. **Todo lo demás cede:** lo no-foco se atenúa (menos luz, menos contraste, ligero desenfoque) para que
   el foco destaque **sin** gritar.
12.4. **El foco sigue a la intención del momento** y se **entrega** limpiamente al siguiente foco (handoff).
12.5. **Nunca competir con el foco:** ningún efecto secundario, HUD o partícula roba la mirada del centro
   de atención.

---

## 13) EMOCIÓN
El lenguaje visual **expresa emoción** de forma coherente y repetible. Paleta emocional oficial:

| Emoción | Expresión visual (movimiento · ritmo · luz · cámara · feedback) |
|---|---|
| **Calma** | Quietud; R0; luz tenue y estable; cámara en reposo; ambiente que respira |
| **Preparación** | Orden que se dispone; R1–R2; luz que revela; leve acercamiento; feedback táctil |
| **Expectativa** | Settle cargado, pausa; R2; luz que crece; micro push-in; contención |
| **Acción** | Movimiento decidido; R1; luz de acompañamiento; cámara firme; feedback inmediato |
| **Impacto** | Golpe por capas + hit-stop; R2–R3; destello; push-in breve; shake S2 |
| **Peligro** | Tensión contenida; pulso lento; temperatura fría/roja focal; cámara estable pero cerrada |
| **Victoria** | Apertura cálida, expansiva; R3; luz que florece; pull-out que abre; clímax por capas |
| **Derrota** | Sobria, digna, nunca humillante; desaceleración; luz que se retira; cámara serena |

**Reglas permanentes:**
13.1. **Cada momento declara su emoción** antes de diseñarse (¿qué debe sentir el jugador?).
13.2. **Coherencia emocional:** la misma emoción se expresa **igual** en todo el juego (mismo idioma).
13.3. **La emoción no compromete la claridad** (regla maestra, §0).
13.4. **Contraste emocional intencional:** la calma da valor al impacto; sin sobriedad no hay clímax.

---

## 14) COHERENCIA (garantía a diez años)
14.1. **Todo elemento visual nuevo pasa el Test de pertenencia** (§0) antes de implementarse.
14.2. **Todo nace de tokens compartidos** (curvas, duraciones, color, sombra, radio, espaciado, luz,
   partícula, shake). Un valor "a mano" es una violación.
14.3. **Un valor nuevo (canal, curva, nivel) solo nace cuando el idioma actual no puede expresar la
   intención** (canon: nacimiento bajo demanda), y entonces se **añade al idioma**, no se usa suelto.
14.4. **Un solo autor aparente:** cualquier animación nueva debe sentirse hecha por la misma mano que la
   primera. Si "desentona", se rediseña.
14.5. **Esta Biblia tiene prioridad** sobre cualquier decisión visual individual. Se actualiza solo de
   forma deliberada y explícita; nunca se contradice ad-hoc.

---

## 15) PROHIBICIONES — "Nunca hacer"
1. **Nunca** un shake sin causa física (§8.1).
2. **Nunca** mover la cámara sin intención ni desaceleración (§3).
3. **Nunca** partículas decorativas o de más de dos capas (§7).
4. **Nunca** romper la legibilidad del estado de juego por espectáculo (§0, regla maestra).
5. **Nunca** ocultar información importante tras un efecto (§7.6, §11.5, §12.5).
6. **Nunca** introducir un efecto que contradiga este idioma (§14).
7. **Nunca** un corte seco entre estados importantes; siempre reveal/handoff (§9.1).
8. **Nunca** valores "a mano" fuera de los tokens compartidos (§14.2).
9. **Nunca** dos focos visuales simultáneos (§12.1).
10. **Nunca** rebote/overshoot en información crítica o texto (§1.4).
11. **Nunca** luz, color o partícula sin significado (§4.1, §7.1, §10.3).
12. **Nunca** subir de nivel de ritmo sin justificar la importancia (§2.1).
13. **Nunca** que la UI compita con la partida por el centro de atención (§10.4).
14. **Nunca** feedback ambiguo o desproporcionado (§11.2, §11.5).
15. **Nunca** aire muerto: si algo tarda, debe mostrar que avanza (§2.4).

---

## 16) GRAMÁTICA VISUAL — cómo se combinan las palabras
Los capítulos §1–§13 son el **vocabulario** (qué palabras existen). Este capítulo es la **gramática**: cómo
se combinan en una **frase visual** coherente. Ninguna regla de aquí pertenece a "Shake", "Cámara" o
"Ritmo" por separado: son las leyes de **combinación** del idioma. Responde *"¿cómo se construye una frase
visual?"*, no *"¿qué palabras existen?"*.

### 16.A — Estructura de una frase visual
16.A.1. **Toda frase visual tiene forma: ANUNCIAR → ACTUAR → RESOLVER → ASENTAR → ENTREGAR.**
   - *Anunciar* (telegrafía/anticipación) · *Actuar* (el gesto) · *Resolver* (el acento/consecuencia) ·
     *Asentar* (reposo cargado) · *Entregar* (handoff al siguiente foco).
   - Cuanto **más intensa** la frase, más obligatorias son las cinco partes. Una microinteracción (R0/R1)
     puede colapsar Anunciar y Asentar; un clímax (R3/S3) **jamás** puede saltárselas.
16.A.2. **Toda palabra de énfasis exige preparación.** Ningún acento fuerte (S2/S3, luz máxima, push-in de
   clímax) aparece "en frío": debe ir precedido de su **anticipación** proporcional. *Corolario:* **nunca
   un Shake S2/S3 sin preparación previa.**
16.A.3. **Todo clímax resuelve antes de entregar el foco.** Un momento de máxima intensidad **debe terminar
   en un estado de reposo** (asentar) antes de ceder el foco al siguiente beat. Prohibido encadenar dos
   clímax sin un reposo entre ellos.

### 16.B — Presupuesto de intensidad (concordancia de énfasis)
16.B.1. **Regla del foco único de énfasis:** en un mismo instante **solo un elemento** puede estar en su
   nivel máximo. **Nunca** combinar simultáneamente varios máximos (R3 **+** S3 **+** iluminación máxima
   **+** cámara máxima) — salvo el **clímax mayor autorizado** (§16.B.3).
16.B.2. **Los demás elementos acompañan un escalón por debajo.** Si el ritmo es R3, el shake acompaña a S2,
   la luz sube pero no satura, la cámara hace un push-in **contenido**. El énfasis se **lidera con una sola
   palabra**; el resto la **apoya**, no compite.
16.B.3. **Clímax mayor (excepción tasada):** la combinación de varios máximos SOLO se permite en el
   **clímax mayor** de una secuencia, una vez, y siempre (a) precedido de preparación (§16.A.2) y (b)
   seguido de reposo (§16.A.3). Si "todo es clímax mayor", nada lo es.

### 16.C — Concordancia (los elementos deben "estar de acuerdo")
16.C.1. **Concordancia de tier:** las palabras de una misma frase pertenecen a niveles **contiguos** (p. ej.
   R2 con S1/S2 y luz de acento). Prohibido mezclar extremos incoherentes (un movimiento R0 con un shake
   S3).
16.C.2. **Concordancia de dirección:** movimiento, shake, luz y cámara que coexisten **apuntan al mismo
   sitio** (el foco). Nada tira hacia lados opuestos.
16.C.3. **Concordancia de temperatura y emoción:** todas las palabras de una frase comparten la temperatura
   (§4.3) y la emoción declarada (§13). No se mezcla luz cálida de victoria con feedback de peligro en la
   misma frase.
16.C.4. **Concordancia de material:** el peso mostrado (§6.5) concuerda con la intensidad de la frase; un
   objeto trivial no se comporta como uno decisivo, ni al revés.

### 16.D — Puntuación y escalado
16.D.1. **La pausa es puntuación.** El "settle"/reposo separa frases como un punto separa oraciones; sin
   puntuación, dos frases se leen como ruido. Toda frase R2/R3 termina en pausa antes de la siguiente.
16.D.2. **Curva de escalado:** una secuencia se **construye** (frases de menor a mayor intensidad), **llega
   a UN pico** y **libera** (desciende a reposo). Prohibida la meseta de intensidad máxima sostenida.
16.D.3. **Contraste obligatorio:** un pico solo se percibe si va precedido de contención. La calma **es**
   parte de la frase del clímax, no un hueco.
16.D.4. **Una idea por frase:** cada frase visual comunica **un** cambio de estado. Dos ideas = dos frases,
   con puntuación entre ellas (nunca simultáneas).

### 16.E — Enlace entre frases (del beat a la secuencia)
16.E.1. **Handoff obligatorio entre frases:** el reposo final de una frase es el punto de partida de la
   siguiente (§9, canon HANDOFF). La secuencia se lee como **un párrafo continuo**, no como frases sueltas.
16.E.2. **Ritmo de párrafo:** una secuencia alterna frases rápidas (R0/R1) y frases con acento (R2/R3); una
   sucesión de solo-acentos fatiga y una de solo-rutina aburre. El párrafo **respira**.
16.E.3. **Coherencia de párrafo:** todas las frases de una misma secuencia comparten idioma (vocabulario +
   gramática); si una frase "desentona" con sus vecinas, se rediseña.

### 16.F — Test gramatical (antes de dar por buena una frase visual)
Además del Test de pertenencia (§0), toda frase visual debe poder responder **sí** a:
- ¿Tiene estructura (anuncia · actúa · resuelve · asienta · entrega) proporcional a su intensidad?
- ¿Su acento máximo fue **preparado** y **resuelto** en reposo?
- ¿Respeta el presupuesto de intensidad (un solo máximo, el resto apoya)?
- ¿Concuerdan tier, dirección, temperatura, emoción y material?
- ¿Enlaza con la frase anterior y prepara la siguiente (handoff)?
- ¿Comunica **una** idea con claridad?

Si alguna respuesta es **no**, la frase está mal construida gramaticalmente y se rediseña — aunque cada
palabra por separado sea correcta.

---

## Referentes destilados (principios, nunca implementaciones)
Escenario como lugar vivo y profundidad (Legends of Runeterra) · ritual, tactilidad y feedback rotundo
(Hearthstone) · ritmo sin aire muerto y respeto al tiempo (Marvel Snap) · presentación de contendientes con
economía (pantallas VS de lucha) · entrega/marcadores de calidad **broadcast deportivo** · plano de
establecimiento, match-cut y "settle" (cine) · un foco por momento, curvas con intención, jerarquía
(motion design / UX premium) · impacto por capas y hit-stop (game-feel competitivo) · materialidad, luz y
composición honestas (diseño industrial, fotografía, arquitectura, dirección de fotografía). *Extraídos como
principios; nunca copiados.*

---
*Visual Language Bible — documento visual de máxima prioridad del proyecto. Referencia OBLIGATORIA antes de
diseñar cualquier animación, UI, HUD, VFX, transición, cámara, efecto, overlay, panel o feedback. Si algo no
está alineado con esta Biblia, se rediseña antes de implementarse.*
