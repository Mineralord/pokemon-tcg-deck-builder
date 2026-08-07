# Visual Grammar Canon — Pokémon TCG Developer Studio (Fase 8)

**Este documento define la gramática visual del Studio: las leyes universales que explican el
*significado* de toda decisión visual futura.** No es apariencia. Es sintaxis. Así como un idioma
tiene gramática antes de tener palabras, el Studio tiene estas leyes antes de definir un solo token.

**Prohibido en esta fase (pertenece a fases siguientes):** colores, tipografía, iconografía, motion,
sombras, elevaciones, espaciados, radios, tamaños, estilos, componentes y tokens. Aquí solo se define
**qué significa** cada recurso compositivo, no su valor.

**Análisis integrado por dos lentes** (activas, una sola propuesta, sin respuestas paralelas):
`ui-ux-pro-max` (leyes Gestalt, jerarquía, retícula, accesibilidad, consistencia) + `emil-design-eng`
("los detalles invisibles se suman", el movimiento con propósito, la excepción como decisión
deliberada). **Resolución del conflicto raíz:** UI/UX aporta las *leyes objetivas* de composición;
Emil aporta el *criterio de intención* que decide cuándo una ley se aplica o se rompe. La gramática
resultante es objetiva (dos diseñadores llegan a la misma solución) pero deja una puerta reglada a la
excepción con significado.

**Relación con el canon:** este documento se apoya en el **Visual Language Canon (Fase 7)** y lo
operacionaliza. Donde la Fase 7 dijo *qué siente* el Studio (silencio, protagonismo, reposo), esta
fase dice *cómo se construye* ese sentir con reglas verificables. Los **Visual Tokens (Fase 9)** serán
únicamente la *implementación técnica* de esta gramática.

**Canon obligatorio heredado:** 4 pilares · P1–P15 · Arq. Espacial (Z1–Z12) · Workflows · Layout
(DC-D1/DC-D2) · Modelo Mental · Interaction Canon (IC-0…IC-13) · PDS · Visual Language (VL-*).

---

## Ley cero de la gramática

**VG-0 · Todo recurso visual es una palabra; ninguna palabra es muda.** Jerarquía, espacio,
proximidad, contraste, profundidad, alineación, superposición y transición son *signos*: existen para
comunicar una relación o un estado. Ningún recurso visual puede usarse solo porque "se ve bien". Si
un elemento visual no expresa significado, no pertenece a la interfaz. Esta es la ley que todas las
demás desarrollan.

---

## Parte I — SIGNIFICADO DE CADA RECURSO (el diccionario de la gramática)

Definición semántica de cada pregunta planteada. Cada recurso = un significado invariante.

- **Jerarquía** significa **importancia relativa**: qué debe leerse primero, qué es contexto, qué es
  acción. Establece el orden de atención.
- **Espacio** significa **relación**: la distancia entre dos cosas *es* la afirmación sobre cuán
  relacionadas están. El espacio nunca es neutro; siempre dice algo.
- **Proximidad** significa **pertenencia**: lo cercano pertenece al mismo grupo/idea. Es la afirmación
  más fuerte de relación (ley Gestalt como norma dura).
- **Separación** significa **distinción**: lo separado pertenece a grupos o niveles distintos. Separar
  es negar una relación.
- **Agrupación** significa **unidad conceptual**: varias piezas leídas como *una sola cosa*.
- **Alineación** significa **orden y parentesco estructural**: lo alineado comparte un eje lógico
  común; la alineación es la prueba visible de que existe una retícula subyacente.
- **Contraste** significa **diferencia de rol**: dos cosas contrastadas cumplen funciones distintas;
  el grado de contraste es el grado de diferencia de rol.
- **Profundidad** significa **orden de atención y de interrupción**: lo que está "delante" reclama la
  atención ahora; lo que está "detrás" es contexto pausado.
- **Superposición** significa **interrupción temporal**: algo se coloca *encima* porque suspende
  momentáneamente lo de debajo, no porque lo sustituya.
- **Transición** significa **relación entre dos estados en el tiempo**: explica de dónde vino algo y en
  qué se convirtió. Sin transición, dos estados parecen dos cosas sin relación.
- **Superficie** significa **plano que hospeda**: el soporte neutro sobre el que vive el contenido.
- **Borde** significa **límite de un dominio**: dice dónde acaba una cosa y empieza otra. Un borde es
  una afirmación de frontera, no un adorno.
- **Contenedor** significa **ámbito**: define qué pertenece a un conjunto y qué queda fuera.
- **Vacío** significa **respiración y separación deliberada**: el vacío es contenido gramatical (como
  el silencio en la música), no ausencia de diseño.
- **Interrupción** significa **quiebre intencionado de una continuidad** para marcar un cambio de
  contexto o de nivel.
- **Excepción** significa **una regla rota a propósito para comunicar algo que la regla no podría**.
- **Cambio de escala** significa **cambio de importancia o de nivel**: agrandar es promover; empequeñecer
  es subordinar. La escala nunca cambia por estética.

---

## Parte II — JERARQUÍA

- **Niveles visuales:** en toda pantalla existe un número **finito y pequeño** de niveles de lectura
  (primario, secundario, terciario, ambiente). Más niveles de los necesarios = ruido; la mente no
  distingue muchas gradaciones simultáneas.
- **Foco principal:** cada pantalla tiene **exactamente uno** en cada momento (coincide con el
  protagonista de VL-U1). El foco principal es el destino garantizado de la primera mirada.
- **Focos secundarios:** existen, pero **subordinados**: apoyan al principal, nunca compiten con él.
- **Competencia visual:** **prohibida.** Dos elementos con el mismo peso disputándose la primera
  lectura es un defecto gramatical (VG-anti). Cuando dos cosas parecen igual de importantes, ninguna
  lo es.
- **Dominancia:** se otorga por **medios acumulativos y mínimos** (posición, peso, contraste, escala),
  usando el menor número posible de ellos para lograr el efecto. Dominar con un solo recurso es más
  gramatical que dominar con cinco.
- **Subordinación:** subordinar es sobre todo **retirar énfasis de lo demás**, no añadir énfasis al
  foco (hereda VL: dirigir la atención es apagar el resto).
- **Estabilidad:** la jerarquía de una pantalla es **estable**: no cambia sola ni por hover casual;
  solo el usuario, con una acción intencionada, reordena el foco. Una jerarquía que baila cansa y
  rompe la memoria muscular.

---

## Parte III — ESPACIO

- **Proximidad:** distancia corta = relación fuerte. Es la primera herramienta de agrupación, antes que
  bordes o fondos. *Agrupa con espacio antes que con líneas.*
- **Separación:** distancia mayor = grupos distintos. El salto de distancia entre "dentro de un grupo"
  y "entre grupos" debe ser **claramente perceptible**; ambigüedad de distancia = ambigüedad de
  significado.
- **Agrupación:** lo que pertenece junto se une por proximidad y alineación compartida; solo si eso no
  basta se añade un contenedor o un borde. **La agrupación más elegante es la que no dibuja nada.**
- **Respiración:** todo grupo necesita margen alrededor; ningún contenido toca el límite de su ámbito
  sin holgura. La respiración protege la legibilidad en jornadas largas (hereda VL-fatiga).
- **Continuidad:** los espacios se repiten con **ritmo constante**; un mismo tipo de relación usa
  siempre la misma distancia en todo el Studio. El ritmo espacial es lo que hace que dos pantallas
  distintas "suenen" iguales.
- **Aislamiento:** aislar un elemento con vacío alrededor es la forma más silenciosa de darle
  importancia (destacar sin gritar, hereda VL-silencio). El vacío promueve.
- **Ritmo espacial:** existe una **familia finita de distancias** con significado (dentro-de-grupo,
  entre-grupos, entre-zonas). No hay distancias arbitrarias; cada una pertenece a la familia y
  significa un grado de relación. (Los *valores* de esa familia son tokens de la Fase 9.)

---

## Parte IV — ESTRUCTURA

- **Alineación:** todo se alinea a ejes comunes; nada flota sin razón. La alineación es la evidencia
  visible de la retícula. Un elemento desalineado *afirma* que es distinto — si no lo es, es un error.
- **Retícula:** existe una retícula subyacente (implícita) que gobierna posiciones y anchos. La
  retícula es la sintaxis del espacio: da el orden que permite densidad alta sin caos (hereda VL:
  densidad organizada).
- **Continuidad:** la retícula y los ritmos se **mantienen** de una pantalla a otra; eso es lo que
  produce la sensación de "mismo producto" (hereda VL-EVO).
- **Interrupción:** romper la continuidad es un acto de habla: marca "aquí empieza otro contexto/nivel".
  Una interrupción sin cambio de significado detrás es ruido; una interrupción con significado es
  puntuación.
- **Modularidad:** la composición se construye con **módulos repetibles** (mismos patrones de
  agrupación reaplicados). La modularidad garantiza consistencia y evolución por extensión (hereda
  F-EVO/F-REUSE del PDS).
- **Equilibrio:** se busca equilibrio de **peso visual**, no simetría literal (hereda VL). Una
  composición equilibrada reposa; una desequilibrada tironea de la atención sin motivo.
- **Tensión:** la tensión (asimetría deliberada, un elemento fuera de ritmo) solo se usa **cuando el
  significado la justifica** — para dirigir la mirada a algo que de verdad lo merece. Tensión sin
  propósito = fatiga.

---

## Parte V — PROFUNDIDAD (sin sombras)

Definimos únicamente qué significa "encima", "debajo", "al frente":

- **"Al frente / encima"** significa **reclama tu atención ahora** y, casi siempre, **interrumpe** lo
  de debajo. Lo que está delante es lo que el usuario debe atender en este instante.
- **"Debajo / detrás"** significa **contexto pausado**: sigue existiendo, importa, pero ha cedido la
  atención temporalmente.
- **La profundidad es un eje de atención e interrupción, no un efecto estético.** No existe
  "profundidad decorativa": cada plano adicional debe corresponder a un nivel real de interrupción.
- **Profundidad mínima y semántica** (hereda VL-U8): solo hay tantos planos como significados. La
  escala de planos es corta y fija: *base (contenido) → elevado-persistente (lo que flota con función)
  → interrupción (overlay/diálogo/menú) → alerta*. Ningún plano intermedio "porque queda bonito".
- **Regla dura VG-PROF:** *aumentar la profundidad de algo es afirmar que ahora interrumpe o reclama
  atención.* Si no interrumpe ni reclama, no sube de plano. La profundidad, cuando se materialice
  (sombra/elevación en Fase 9), será solo la *señal* de este significado, jamás su causa.

---

## Parte VI — MOVIMIENTO (sin duraciones ni curvas)

Qué representa conceptualmente cualquier transición (integra el framework de decisión de Emil y la
Filosofía del Movimiento de VL). Toda transición es una **frase sobre un cambio**; sin frase, no hay
transición.

- **Continuidad** — cuando algo se mueve *manteniendo su identidad* (se reposiciona, se reordena una
  lista, un panel se ajusta). Dice: "es el mismo objeto, solo cambió de sitio". Evita el salto que
  haría pensar que apareció otra cosa.
- **Cambio** — cuando algo *entra o sale* de la escena. Dice: "esto empezó a existir aquí" o "esto
  dejó de estar". Entra/sale desde donde lógicamente pertenece (consistencia espacial).
- **Transformación** — cuando un objeto *se convierte* en otro estado del mismo objeto (un control que
  muta, un elemento que se expande en su detalle). Dice: "esto se volvió aquello", preservando el
  vínculo entre ambos estados.
- **Confirmación** — respuesta breve a una acción del usuario (feedback de pulsación). Dice: "te oí".
  Es la transición más pequeña y la más importante para que el Studio se sienta vivo y preciso.
- **Error** — señal de que algo no puede completarse o requiere atención. Dice: "detente, mira". Es la
  única transición autorizada a ser insistente, y precisamente por rara, funciona (hereda VL-silencio,
  modo Alerta).
- **Regla dura VG-MOV:** *ninguna transición existe sin una de estas cinco frases detrás.* Lo de alta
  frecuencia o iniciado por teclado no se anima (hereda VL-U4): la quietud es el estado por defecto y
  el movimiento se gana. Las *curvas y duraciones* concretas son tokens de la Fase 9; aquí solo se fija
  qué está autorizada a *decir* una transición.

---

## Parte VII — CONTRASTE (sin colores)

Qué comunica el contraste, con independencia del color con que se materialice:

- **Qué comunica:** **diferencia de rol e importancia.** Más contraste = "esto es más importante o
  cumple un rol distinto"; menos contraste = "esto es contexto, secundario o del mismo grupo".
- **Cuándo debe aumentar:** para señalar el **foco actual**, el **dato clave** o una **acción/alerta**
  que el usuario debe percibir. El aumento de contraste es promoción de importancia; se usa con
  cuentagotas.
- **Cuándo debe desaparecer (bajar):** entre elementos del **mismo grupo y mismo nivel**, y en todo el
  **chasis en reposo**. Igualdad de rol ⇒ igualdad de contraste. Bajar el contraste es afirmar
  "esto pertenece junto y descansa".
- **Cuándo debe permanecer estable:** en el **fondo estructural** durante toda la jornada. El contraste
  base no fluctúa; solo se mueve el contraste *local* del foco. Un chasis de contraste estable es lo
  que permite mirar ocho horas (hereda VL-fatiga).
- **Regla dura VG-CONT:** *el contraste máximo se reserva para el significado máximo* (foco, dato
  crítico, alerta). Nunca es el estado por defecto (hereda VL-U5). Un aumento de contraste sin cambio
  de rol detrás es ruido.

---

## Parte VIII — CONTINUIDAD

La continuidad es la ley que produce identidad a lo largo de pantallas y de décadas.

- **VG-CONTIN-1 —** Las mismas relaciones se expresan siempre con los mismos recursos: una misma
  clase de relación usa siempre la misma proximidad, el mismo contraste, el mismo plano. La constancia
  del signo es lo que permite *leer* la interfaz sin re-aprenderla.
- **VG-CONTIN-2 —** El ritmo (espacial y estructural) se conserva entre pantallas; una pantalla nueva
  es una *variación del mismo tema*, jamás una excepción (hereda VL).
- **VG-CONTIN-3 —** La continuidad se rompe **solo** por interrupción con significado (cambio de
  contexto/nivel). Toda ruptura de continuidad debe poder nombrar qué cambio marca.

---

## Parte IX — CONTENEDORES (como construcciones visuales, no componentes)

Significado *gramatical* de cada uno (su papel semántico coincide con los materiales de VL Fase 7,
aquí visto como construcción compositiva):

- **Panel** — construcción de **territorio permanente**: un ámbito con residencia fija en la retícula.
  Gramaticalmente afirma "aquí vive siempre este tipo de trabajo". Se distingue por *posición estable*,
  no por decoración.
- **Tarjeta** — construcción de **objeto autocontenido y movible**: agrupa lo que pertenece a *una*
  entidad del dominio y afirma "esto es una cosa con identidad que puedes tomar". Su borde/ámbito
  existe para declarar esa unidad, no para adornar.
- **Overlay** — construcción de **interrupción temporal**: vive en un plano superior porque suspende lo
  de debajo; gramaticalmente es un paréntesis que se abre y se cierra sin dejar rastro.
- **Ventana** — construcción de **mundo/continente**: la frontera de todo lo visible; afirma los
  límites del universo de trabajo.
- **Inspector** — construcción de **instrumento de medición y edición**: su gramática prioriza el dato
  y la precisión sobre cualquier tratamiento del contenedor; el marco desaparece ante el valor.
- **Preview** — construcción de **ventana al resultado**: su gramática es la de máxima cesión del
  marco; el contenido observado ocupa el protagonismo casi absoluto (modo cercano al silencio total).
- **Timeline** — construcción de **eje temporal**: organiza elementos según *cuándo* ocurren; su
  gramática introduce el tiempo como dimensión espacial y exige continuidad y ritmo a lo largo del eje.
- **Regla VG-CONTENEDOR:** *un contenedor solo existe para declarar un ámbito o una relación.* Un
  contenedor que no agrupa nada, no delimita un dominio ni marca un plano, es decoración prohibida.

---

## Parte X — EXCEPCIONES

El corazón de la gramática: distinguir una excepción legítima de una inconsistencia.

- **Qué significa romper una regla:** una excepción es **un acto de habla enfático**. Rompe la
  gramática *precisamente para comunicar algo que la gramática regular no podría* con la misma fuerza
  (una alerta crítica, un momento único, el protagonismo excepcional del contenido Pokémon).
- **Cuándo puede romperse una regla (test de las tres condiciones — deben cumplirse las TRES):**
  1. **Propósito nombrable:** la excepción comunica un significado concreto que la regla no permite
     expresar. Si el motivo es "se ve mejor", no es excepción: es inconsistencia.
  2. **Rareza:** la excepción es infrecuente. Una excepción que se repite deja de ser énfasis y se
     convierte en una regla nueva (y entonces debe *canonizarse*, no seguir siendo excepción).
  3. **Reversión limpia:** fuera del caso excepcional, la gramática regular se restablece por completo.
     La excepción no contamina el resto.
- **Cómo distinguir excepción legítima de inconsistencia:**
  - *Legítima:* deliberada, justificada por significado, rara, reversible, y **defendible** citando
    esta gramática ("rompo la continuidad aquí para marcar una alerta crítica").
  - *Inconsistencia:* accidental o cómoda, sin significado detrás, repetida sin criterio, indefendible.
    La pregunta decisiva: *¿otro diseñador, aplicando estas leyes, habría llegado a lo mismo?* Si la
    respuesta es no y no hay significado que lo justifique, es inconsistencia.
- **Regla dura VG-EXC:** *una excepción sin propósito nombrable es siempre un defecto.* Ante la duda,
  no se hace la excepción: se aplica la regla (hereda VL-P7, consistencia obsesiva).

---

## Parte XI — SEMÁNTICA VISUAL (la tesis, hecha reglas)

Reglas universales que hacen operativa la Ley Cero:

- **VG-SEM-1 —** Un aumento de profundidad nunca es decorativo: siempre afirma interrupción o reclamo
  de atención.
- **VG-SEM-2 —** Una separación nunca existe "porque queda bien": siempre afirma una distinción real
  entre grupos o niveles.
- **VG-SEM-3 —** Un cambio de escala siempre comunica un cambio de importancia o de nivel.
- **VG-SEM-4 —** Un cambio de contraste siempre comunica un cambio de rol o de importancia.
- **VG-SEM-5 —** Una transición siempre comunica una de las cinco frases (continuidad, cambio,
  transformación, confirmación, error).
- **VG-SEM-6 —** Un contenedor siempre declara un ámbito o una relación.
- **VG-SEM-7 —** Un vacío siempre es respiración o separación deliberada, nunca "espacio sobrante".
- **VG-SEM-8 —** El corolario universal: **si no puedes nombrar el significado de una decisión visual,
  la decisión no existe todavía; no la tomes.**

---

## Parte XII — EVOLUCIÓN (válido dentro de treinta años)

- **VG-EVO-1 — Leyes, no modas.** Esta gramática describe *relaciones y significados*, no estilos.
  Las modas visuales (colores, texturas, "flat vs. skeuomorfo") cambiarán; "proximidad = pertenencia"
  o "profundidad = interrupción" no. Ninguna ley de este documento depende de una tendencia.
- **VG-EVO-2 — La gramática precede a las palabras.** Cualquier token, componente o pantalla futura es
  una *frase* construida con esta gramática; si un token no puede explicarse como aplicación de una ley
  de aquí, el token está mal, no la ley.
- **VG-EVO-3 — Evolución por extensión.** Se pueden añadir nuevas *palabras* (tokens, patrones) sin
  tocar la gramática. Cambiar el *significado* de un recurso (que la profundidad pase a significar otra
  cosa) es una revisión mayor del lenguaje, con migración explícita (hereda F-EVO / VL-EVO-2).

---

# Leyes Fundamentales de la Gramática Visual

- **VG-L1 · Significado obligatorio.** Todo recurso visual comunica una relación o un estado; nada es
  decorativo (VG-0).
- **VG-L2 · Un signo, un significado.** Cada recurso (proximidad, contraste, profundidad…) tiene un
  significado invariante en todo el Studio.
- **VG-L3 · Jerarquía única.** Cada pantalla tiene un solo foco principal; la competencia visual es un
  defecto.
- **VG-L4 · El espacio habla.** La distancia entre elementos *es* su relación; no hay espacio neutro.
- **VG-L5 · Profundidad semántica.** Los planos existen solo por niveles reales de atención e
  interrupción.
- **VG-L6 · Contraste reservado.** El contraste máximo es para el significado máximo; el chasis reposa.
- **VG-L7 · Movimiento con frase.** Toda transición dice una de cinco cosas; si no, no ocurre.
- **VG-L8 · Continuidad = identidad.** Las mismas relaciones se expresan siempre igual; la ruptura
  exige significado.
- **VG-L9 · Excepción con propósito.** Romper una regla es un énfasis deliberado, raro y reversible;
  si no, es inconsistencia.
- **VG-L10 · Reproducibilidad.** Dos diseñadores aplicando estas leyes llegan a soluciones equivalentes.

---

# Reglas Universales de Composición

1. **VG-U1 —** Cada pantalla declara y sostiene un único foco principal; nada compite con él.
2. **VG-U2 —** Agrupa primero con espacio y alineación; añade contenedor o borde solo si el significado
   lo exige.
3. **VG-U3 —** Toda distancia pertenece a la familia de distancias con significado; no hay separaciones
   arbitrarias.
4. **VG-U4 —** Todo se alinea a la retícula; un elemento desalineado afirma que es distinto y debe
   serlo de verdad.
5. **VG-U5 —** Un plano/una profundidad solo se usan para un nivel real de interrupción o atención.
6. **VG-U6 —** El contraste sube solo con la importancia; el chasis mantiene contraste estable y bajo.
7. **VG-U7 —** Toda transición corresponde a una de las cinco frases canónicas.
8. **VG-U8 —** Ningún recurso visual se usa sin poder nombrar su significado (corolario VG-SEM-8).
9. **VG-U9 —** Ante la duda, se repite el patrón existente antes de inventar (continuidad > novedad).
10. **VG-U10 —** Toda decisión visual futura (token, estilo, pantalla) se justifica citando una ley de
    este documento; si no puede, no es canónica.

---

# Reglas de Jerarquía

- **VG-J1 —** Número finito y pequeño de niveles de lectura por pantalla.
- **VG-J2 —** Un solo foco principal en cada momento; los secundarios lo apoyan, no compiten.
- **VG-J3 —** Se domina con los mínimos recursos posibles (posición > peso > contraste > escala, en ese
  orden de preferencia por sobriedad).
- **VG-J4 —** Subordinar es retirar énfasis del resto, no amontonar énfasis en el foco.
- **VG-J5 —** La jerarquía es estable: solo la acción intencionada del usuario reordena el foco.

---

# Reglas de Espacio

- **VG-E1 —** Proximidad = pertenencia; separación = distinción (Gestalt como norma dura).
- **VG-E2 —** El salto de distancia entre "dentro de grupo" y "entre grupos" debe ser inequívoco.
- **VG-E3 —** Todo grupo respira: holgura obligatoria antes del límite de su ámbito.
- **VG-E4 —** El ritmo espacial es constante en todo el Studio; una relación = una distancia.
- **VG-E5 —** El aislamiento por vacío es la forma canónica de destacar sin gritar.

---

# Reglas de Profundidad

- **VG-D1 —** "Delante" = reclama atención ahora / interrumpe; "detrás" = contexto pausado.
- **VG-D2 —** Escala de planos corta y fija: base → elevado-persistente → interrupción → alerta.
- **VG-D3 —** Subir de plano es afirmar interrupción o reclamo; sin eso, no se sube.
- **VG-D4 —** No existe profundidad decorativa: cada plano = un significado.
- **VG-D5 —** Elementos del mismo significado comparten plano; distinto significado, distinto plano.

---

# Reglas de Contraste

- **VG-K1 —** Contraste = diferencia de rol e importancia.
- **VG-K2 —** Aumenta solo para foco, dato clave o alerta.
- **VG-K3 —** Baja entre iguales (mismo grupo, mismo nivel) y en el chasis en reposo.
- **VG-K4 —** El contraste base es estable durante toda la jornada; solo fluctúa el contraste local del
  foco.
- **VG-K5 —** El contraste máximo se reserva para el significado máximo; nunca es el estado por defecto.

---

# Reglas de Continuidad

- **VG-C1 —** Misma relación ⇒ mismos recursos visuales, en todas las pantallas.
- **VG-C2 —** El ritmo espacial y estructural se conserva entre pantallas y en el tiempo.
- **VG-C3 —** La continuidad se rompe solo por interrupción con significado nombrable.
- **VG-C4 —** Una pantalla nueva es una variación del mismo tema, jamás una excepción de estilo.

---

# Reglas para las Excepciones

- **VG-X1 —** Una excepción exige las tres condiciones: propósito nombrable + rareza + reversión limpia.
- **VG-X2 —** Una excepción que se repite debe canonizarse como regla o eliminarse; no puede persistir
  como excepción crónica.
- **VG-X3 —** Toda excepción debe ser defendible citando esta gramática; si es indefendible, es
  inconsistencia.
- **VG-X4 —** Ante la duda entre excepción o regla, se aplica la regla.
- **VG-X5 —** Test de reproducibilidad: si otro diseñador aplicando estas leyes no llegaría a la misma
  excepción y no hay significado que lo justifique, es un defecto.

---

# Decisiones Canónicas

- **DC-VG1 —** La **Ley Cero** (todo recurso visual significa; nada decora) es la base de toda la
  gramática y de las fases 9–11.
- **DC-VG2 —** Se adopta el **diccionario de significados** de la Parte I como definición invariante de
  cada recurso compositivo.
- **DC-VG3 —** **Jerarquía de foco único** por pantalla; competencia visual = defecto (VG-L3).
- **DC-VG4 —** **Familia finita de distancias con significado** como única fuente legítima de
  espaciado (los valores se fijan en Fase 9).
- **DC-VG5 —** **Escala de planos corta y semántica** (base → elevado-persistente → interrupción →
  alerta); profundidad = interrupción/atención, nunca estética.
- **DC-VG6 —** **Cinco frases del movimiento** (continuidad, cambio, transformación, confirmación,
  error) como únicas transiciones autorizadas.
- **DC-VG7 —** **Contraste reservado**: máximo para significado máximo; chasis estable.
- **DC-VG8 —** **Test de las tres condiciones** como criterio canónico de excepción legítima.
- **DC-VG9 —** La **Prueba de Gramática Visual** (abajo) es requisito obligatorio antes de aprobar
  Visual Tokens, Component Styling y Screen Design.

---

# Anti-Patrones Visuales (prohibidos)

- **Decoración muda** — cualquier recurso visual sin significado nombrable (viola VG-0).
- **Competencia de focos** — dos elementos disputándose la primera lectura.
- **Distancia arbitraria** — espaciados que no pertenecen a la familia de distancias con significado.
- **Profundidad decorativa** — planos, sombras o elevaciones que no marcan interrupción/atención real.
- **Contraste gratuito** — subir el contraste sin cambio de rol detrás; contraste máximo como norma.
- **Movimiento sin frase** — transiciones que no dicen ninguna de las cinco cosas; animar lo de alta
  frecuencia o iniciado por teclado.
- **Contenedor vacío de sentido** — un contenedor que no agrupa, no delimita dominio ni marca plano.
- **Desalineación accidental** — romper la retícula sin afirmar una distinción real.
- **Excepción crónica** — una excepción repetida que nadie canoniza ni retira.
- **Ruptura de continuidad sin causa** — interrumpir el ritmo sin un cambio de contexto que lo motive.
- **Inconsistencia por comodidad** — inventar una solución nueva pudiendo reutilizar el patrón existente.

---

# Auditoría Semántica

Contra `MENTAL-MODEL.md` y el Visual Language Canon:

- Los términos gramaticales (jerarquía, proximidad, contraste, profundidad, superposición, transición,
  vacío, excepción) se usan con **un único significado invariante** (Parte I): un signo, un
  significado (VG-L2). No hay sinónimos compitiendo.
- **"Tarjeta" vs "carta Pokémon":** se mantiene la disciplina ortográfica de la Fase 7 — "Tarjeta"
  = construcción visual/componente; "carta Pokémon" = contenido de dominio. Sin colisión.
- Los nombres de contenedores (Panel, Overlay, Ventana, Inspector, Preview, Timeline, Tarjeta)
  coinciden 1:1 con materiales VL (Fase 7) y organismos PDS (Fase 6): **sin divergencia terminológica**.
- Las "cinco frases del movimiento" y los "cuatro modos de silencio" (VL) son vocabularios
  complementarios y no colisionan: uno describe *transiciones*, el otro *presencia estática*.
- ¿Puede un usuario/diseñador nuevo construir el modelo? Sí: cada recurso tiene una definición de una
  frase y un test de aplicación. **Escala a décadas.**

**Veredicto: sin ambigüedad, sin duplicación. Aprobada.**

---

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No. Operacionaliza la productividad (densidad organizada por retícula), la
   accesibilidad (jerarquía y contraste con significado) y P15 (honestidad: nada decorativo/engañoso).
2. **¿Contradice la Arquitectura Espacial (Z1–Z12)?** No. La retícula y los planos se apoyan en las
   zonas; no las reubican.
3. **¿Contradice los Workflows / Interaction Canon?** No. Las cinco frases del movimiento son la
   lectura gramatical del framework de interacción (IC-8 no bloqueante, feedback de pulsación).
4. **¿Contradice el PDS (Fase 6)?** No. Le da la sintaxis con la que se estilizarán sus componentes en
   Fase 10, sin crear componentes ni valores.
5. **¿Contradice el Visual Language (Fase 7)?** No: lo *implementa*. Cada ley VG desarrolla un
   principio VL (silencio→contraste/planos; protagonismo→jerarquía; reposo→continuidad/contraste
   estable; materiales→contenedores).
6. **¿Introduce excepciones o duplicación innecesarias?** No: reutiliza el vocabulario de VL/PDS.
7. **¿Respeta la Regla de Oro (sin lógica de juego)?** Sí: es gramática del chasis; la expresividad del
   contenido Pokémon se declara fuera de esta sobriedad (excepción legítima de dominio).
8. **¿Escala a cientos de Labs y décadas?** Sí, por diseño (VG-EVO, reproducibilidad VG-L10).
9. **¿Genera deuda?** No; la reduce, al dar criterio verificable previo a tokens/estilos/pantallas.
10. **¿Canónico?** Sí.

**Veredicto: Canónico.**

---

# Auditoría de Filosofía

Contra `PRINCIPIO-DESARROLLO-UNIPERSONAL.md`:

1. **¿Respeta el Desarrollo Unipersonal?** Sí. La gramática asume una sola atención, un solo foco por
   pantalla; ninguna construcción presupone colaboración o presencia de terceros.
2. **¿Introduce complejidad multi-desarrollador?** No. No hay signos de "otros usuarios" ni estados
   colaborativos.
3. **¿Puede simplificarse por ser un solo desarrollador?** Ya lo está: reglas mínimas, foco único,
   familia finita de distancias y planos.
4. **¿Las IA siguen siendo herramientas, no actores?** Sí. Los resultados de IA se expresan como
   contenido/datos dentro de la gramática normal, sin voz visual propia de "actor".
5. **¿Mantiene simplicidad, preservación y evolución a largo plazo?** Es su tesis (Parte XII;
   reproducibilidad y leyes-no-modas).

**Veredicto: alineado con los cuatro pilares.**

---

# Impacto en el Futuro

- **Condiciona la Fase 9 (Visual Tokens):** cada token debe declararse como *implementación de una ley
  de esta gramática* (VG-EVO-2). La familia de distancias (VG-U3), la escala de planos (DC-VG5) y el
  contraste reservado (DC-VG7) predefinen la *estructura* de los token sets antes de elegir un solo
  valor. Un token que no mapea a una ley se rechaza.
- **Condiciona la Fase 10 (Component Styling):** el estilo de cada componente PDS debe expresar
  significado (qué agrupa, qué plano ocupa, qué frase de movimiento admite). La Prueba de Gramática se
  aplica componente a componente.
- **Condiciona la Fase 11 (Screen Design):** cada pantalla debe declarar su foco único, sus grupos por
  proximidad y sus planos por interrupción, y superar la Prueba completa.
- **Oportunidades que habilita:** verificabilidad objetiva del diseño (VG-L10, reproducibilidad);
  onboarding de diseñadores/pantallas con criterio compartido; re-tematización sin pérdida de
  significado; defensa razonada de cada decisión visual.
- **Limitaciones que introduce (deliberadas):** prohíbe la decoración libre y la improvisación
  estética; obliga a nombrar el significado de cada decisión. Coste asumido a cambio de longevidad y
  coherencia.
- **Riesgos a vigilar:** (a) que la Fase 9 introduzca valores que traicionen una ley (p. ej. contraste
  alto por defecto); (b) *creep* de decoración muda al materializar estilos; (c) excepciones crónicas
  no canonizadas; (d) fuga de la expresividad del contenido Pokémon al chasis. La Prueba de Gramática
  Visual es la barrera contra los cuatro.

---

# ¿A la altura de un producto AAA?

*¿Estaría esta decisión a la altura de Figma / Unreal / JetBrains / VS Code / Unity?*

Sí. Las herramientas profesionales de referencia comparten exactamente esta tesis silenciosa: su
solidez no viene de un estilo llamativo sino de una **gramática compositiva rigurosa** —jerarquía
inequívoca, espacio con significado, planos semánticos, contraste reservado— aplicada con una
consistencia que se siente sin verse. Este documento formaliza esa gramática y añade dos rasgos
propios y defendibles: la **Ley Cero de significado obligatorio** y el **test de reproducibilidad**
(VG-L10), que convierten la calidad visual en algo *verificable* en lugar de dependiente del gusto —
un estándar que iguala o supera la práctica implícita de esas herramientas. Lo que aún **falta** para
materializar el estándar es, legítimamente, la Fase 9 en adelante: los valores concretos. Como
*gramática*, está a la altura AAA; su *realización* llega después. **Aprobado como canon.**

---

# Prueba de Gramática Visual

**Requisito de aprobación OBLIGATORIO** antes de canonizar cualquier **Visual Token (Fase 9)**,
**Component Styling (Fase 10)** o **Screen Design (Fase 11)**. Toda decisión debe responder **SÍ** a
las preguntas aplicables; un solo "NO" bloquea la aprobación hasta corregirse.

### Significado (Ley Cero)
1. ¿Esta decisión comunica un significado, o solo decora? (VG-0 / VG-SEM-8)
2. ¿Puedo nombrar en una frase qué relación o estado expresa?
3. Si es profundidad/escala/contraste/separación/transición: ¿comunica algo, o es estética pura?
   (VG-SEM-1…5)

### Jerarquía
4. ¿Respeta el foco único de la pantalla, sin crear competencia visual? (VG-L3)
5. ¿Usa los mínimos recursos necesarios para establecer dominancia? (VG-J3)

### Espacio
6. ¿El espacio utilizado expresa una relación real entre elementos? (VG-E1)
7. ¿Toda distancia pertenece a la familia de distancias con significado? (VG-U3)
8. ¿Respeta el ritmo espacial del resto del Studio? (VG-E4)

### Profundidad
9. ¿La profundidad comunica estructura/interrupción, o solo estética? (VG-D3/D4)
10. ¿El plano usado corresponde a un nivel real de atención? (VG-D5)

### Contraste
11. ¿El contraste sube solo por importancia/rol, y el chasis permanece estable? (VG-K2/K4)
12. ¿Evita el contraste máximo como estado por defecto? (VG-K5)

### Movimiento
13. Si hay transición, ¿dice una de las cinco frases (continuidad, cambio, transformación,
    confirmación, error)? (VG-L7)
14. ¿Evita animar lo de alta frecuencia o iniciado por teclado? (VG-MOV)

### Continuidad y excepción
15. ¿Un diseñador diferente llegaría a la misma solución aplicando estas leyes? (VG-L10)
16. ¿Reutiliza patrones existentes antes de inventar? (VG-U9)
17. Si rompe una regla: ¿la excepción cumple las tres condiciones (propósito nombrable + rareza +
    reversión limpia) y es defendible citando esta gramática? (VG-X1/X3)

### Cierre
18. ¿Esta decisión puede justificarse citando explícitamente una ley de este documento? (VG-U10)
