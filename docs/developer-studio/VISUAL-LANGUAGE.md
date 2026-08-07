# Visual Language Canon — Pokémon TCG Developer Studio (Fase 7)

**Este documento define la identidad visual profunda del Studio, no su apariencia concreta.** No
hay colores, tipografía, iconografía, sombras, elevaciones, radios, tamaños, motion ni tokens: todo
eso pertenece a las fases siguientes y **deberá justificarse contra este documento**. Aquí se cierra
el *lenguaje* que hará que cualquier pantalla —de hoy o de dentro de veinte años— se reconozca como
parte del mismo producto.

**Análisis integrado por dos lentes** (activas esta sesión, integradas en una sola propuesta, sin
respuestas paralelas): `ui-ux-pro-max` (claridad, jerarquía, accesibilidad, consistencia, fatiga,
escalabilidad) + `emil-design-eng` (feel, silencio, percepción, "los detalles invisibles se suman",
la belleza como palanca). **Resolución del conflicto raíz:** UI/UX pide sobriedad y reposo; Emil pide
carácter y detalle. Este canon no elige uno: declara que **el carácter del Studio ES su sobriedad
llevada a un nivel de artesanía obsesiva**. La personalidad no se logra decorando, sino puliendo lo
que ya está. Silencio ejecutado con maestría = identidad.

**Canon obligatorio heredado:** 4 pilares · P1–P15 · Arquitectura Espacial (Z1–Z12) · Workflows ·
Layout (DC-D1/DC-D2) · Modelo Mental · Interaction Canon (IC-0…IC-13) · Design System / PDS.

---

## Parte I — Qué es un "lenguaje visual" (y qué no es)

Un lenguaje visual no es un tema ni un estilo. Es el **conjunto de intenciones invariantes** que
sobreviven a cualquier repintado. Si mañana cambiara la paleta entera, el Studio debería seguir
sintiéndose el mismo. Eso que permanece es lo que definimos aquí.

Regla fundacional (**VL-0**): *El lenguaje visual es independiente de los valores visuales.* Los
tokens (Fase 8) son una **realización** de este canon, no su fuente. Cuando un token y este documento
entren en conflicto, gana este documento; el token está mal calibrado.

---

## Parte II — IDENTIDAD

### El arquetipo: **el taller de un artesano-científico**

El Studio no es un juguete ni una consola de mando militar. Es el **banco de trabajo de alguien que
fabrica cartas de Pokémon con precisión de laboratorio y cariño de artesano.** De ahí derivan las
cuatro sensaciones que el usuario debe percibir simultáneamente, en esta proporción exacta:

- **Tecnológica (base, ~40%):** todo responde, todo es exacto, nada es aproximado. La tecnología se
  siente en la *precisión y la latencia cero*, nunca en adornos "futuristas" (sin neón, sin HUD, sin
  rejillas sci-fi). Tecnología = confianza mecánica, no estética de ciencia ficción.
- **Científica (estructura, ~25%):** hay método, unidades, medida, reproducibilidad. Se siente en el
  **orden y la legibilidad de los datos**, no en gráficos decorativos. Un inspector es un
  instrumento de medición, no un panel de laboratorio de película.
- **Artesanal (alma, ~25%):** cada detalle está pulido a mano. Se siente en el *acabado*: bordes que
  encajan, alineaciones perfectas, transiciones que nunca chirrían. Es el 25% que separa "funciona"
  de "se siente bien". Es la aportación directa de Emil: los detalles invisibles se suman.
- **Creativa (destello, ~10%):** es una herramienta para *crear cartas Pokémon*, un dominio con
  color y vida. La creatividad **no vive en el marco del Studio**; vive en el *contenido* que el
  Studio enmarca. El marco es el museo, la carta es la obra. El 10% creativo aparece exclusivamente
  cuando el contenido lo trae.

### Personalidad y carácter

- **Personalidad:** un profesional callado, extremadamente competente y sin ego. No presume. No
  interrumpe. Está siempre listo. Cuando hablas, ya te ha entendido.
- **Carácter:** sereno bajo carga, honesto (P15), preciso, incansable. Nunca ansioso, nunca festivo
  por defecto, nunca condescendiente.
- **Nivel de seriedad:** **serio pero no solemne.** Serio = fiable, exacto, respetuoso con el tiempo
  del usuario. No solemne = no rígido, no ceremonioso, no frío. El punto de calibración: un
  instrumento científico bien diseñado *puede* ser hermoso sin dejar de ser preciso.
- **Percepción profesional:** debe leerse como herramienta de gama alta a los tres segundos, sin
  necesidad de un solo adorno. La profesionalidad se demuestra con *reposo y control*, no con
  densidad de features visibles.

### Valores visuales (jerarquizados — en conflicto, el de arriba gana)

1. **Claridad** — que se entienda antes que gustar.
2. **Reposo** — que se pueda mirar ocho horas sin agotar.
3. **Precisión** — que cada píxel signifique algo.
4. **Continuidad** — que todo pertenezca al mismo mundo.
5. **Acabado** — que el detalle invisible esté, aunque nadie lo note.

### Lenguaje emocional a lo largo del tiempo

- **Al abrirlo por primera vez:** *"esto es serio y está cuidado".* No asombro espectacular —
  **confianza inmediata**. La primera impresión correcta es "puedo confiar mi trabajo aquí", no "qué
  bonito". Bienvenida sin fuegos artificiales.
- **Tras ocho horas seguidas:** *ausencia de fricción y ausencia de fatiga.* El objetivo emocional a
  las 8 horas es **que no sientas nada** — ni cansancio visual, ni irritación, ni ruido. La mejor
  herramienta a las 8 horas es invisible.
- **Tras miles de horas:** *pertenencia y confianza muscular.* El Studio se ha vuelto una extensión
  de la mano. La memoria muscular es sagrada (ver Parte VII). Nada se ha movido de sitio sin razón.
  El usuario ya no "usa" el Studio; *piensa a través de él*.

---

## Parte III — FILOSOFÍA VISUAL

- **Minimalismo:** **minimalismo funcional, no estético.** No quitamos cosas para verse limpios;
  quitamos todo lo que no ayuda a la tarea. Si un elemento gana su sitio por utilidad, se queda
  aunque "cargue". El vacío no es un objetivo, es una consecuencia de la disciplina.
- **Densidad de información:** **alta pero organizada.** Es una herramienta profesional: el usuario
  experto quiere ver mucho a la vez (P-productividad). La densidad se hace tolerable con jerarquía y
  agrupación, no reduciéndola. Rechazamos el minimalismo "consumer" que oculta potencia tras clics.
- **Cantidad de aire:** *el aire estructura, no decora.* El espacio en blanco separa grupos y crea
  respiración; no se usa como lujo vacío. Aire generoso entre grupos, aire ajustado dentro de un
  grupo. El aire es un delimitador, el más silencioso de todos.
- **Jerarquía visual:** siempre existe una y solo una primera lectura por pantalla. El ojo debe saber
  sin esfuerzo qué es primario, qué es contexto y qué es acción. La jerarquía se logra por *peso,
  posición y contraste*, nunca por color decorativo.
- **Ritmo visual:** repetición predecible de espaciados y alineaciones. El ritmo es lo que hace que
  una pantalla nueva "suene" como el resto. Un ritmo constante es memoria muscular para el ojo.
- **Orden, simetría, equilibrio:** **equilibrio > simetría.** No perseguimos simetría literal;
  perseguimos balance de peso visual. Todo se alinea a una retícula implícita; nada flota sin razón.
- **Continuidad:** una pantalla es una variación del mismo tema, nunca una excepción. Continuidad =
  las mismas leyes aplicadas, no los mismos píxeles.
- **Consistencia:** **la consistencia es la característica de identidad número uno.** Ante la duda,
  se repite lo existente antes de inventar. La inconsistencia es el único "bug visual" que el canon
  trata como defecto grave por defecto.

---

## Parte IV — FILOSOFÍA DEL ESPACIO

- **Amplitud:** el Studio nunca debe sentirse apretado aunque esté lleno. La amplitud se logra con
  *jerarquía y agrupación*, no con menos contenido. Una pantalla densa puede sentirse amplia si está
  bien ordenada.
- **Profundidad:** **profundidad mínima y semántica.** Solo hay tantos planos como significados: el
  contenido vive en el plano base; lo que se superpone (overlay, diálogo, menú) está *por encima*
  porque *interrumpe*. La profundidad comunica "esto está delante de aquello en tu atención", nunca
  es decorativa. Rechazamos capas apiladas por estética.
- **Proximidad / agrupación / separación:** proximidad = pertenencia (ley Gestalt como norma dura).
  Lo relacionado se junta; lo no relacionado se separa con aire o con un divider silencioso. La
  distancia entre dos elementos *es* información sobre su relación.
- **Organización:** zonas estables (Z1–Z12). El usuario aprende *dónde* vive cada cosa y eso no se
  renegocia por pantalla. La ubicación es memoria muscular (Parte VII).
- **Respiración visual:** toda región densa necesita un margen de descanso alrededor. Ninguna
  información toca el borde de su contenedor sin holgura. El contenido respira; el contenedor lo
  protege.
- **Dirección visual y foco de atención:** en cada momento hay **un** foco. El resto de la interfaz
  cede protagonismo (baja su contraste percibido) para que el foco gane sin gritar. Dirigir la
  atención es sobre todo *apagar lo demás*, no encender el foco.

---

## Parte V — FILOSOFÍA DE LA LUZ (sin color)

- **Cómo debe sentirse la iluminación:** **luz difusa, uniforme y sin fuente dramática.** Como la luz
  de un buen taller: pareja, sin sombras teatrales, sin brillos. Nada "brilla"; todo se *ve bien*. No
  hay sol ni foco escénico. La iluminación es la de un instrumento, no la de un escenario.
- **Cuánto contraste:** **contraste suficiente, nunca máximo.** El contraste se reserva para la
  jerarquía real (foco, dato clave, acción primaria). El contraste máximo (extremos absolutos) se
  prohíbe como estado por defecto: fatiga a las horas (Parte VII). El chasis vive en contraste
  medio-bajo; el contenido puede subir cuando lo pide.
- **Sensación de volumen:** **casi plano, con relieve funcional mínimo.** El volumen solo aparece
  para comunicar *interactividad* o *separación de plano* (lo pulsable, lo elevado porque interrumpe).
  Un panel no tiene volumen porque sí. El relieve es un mensaje ("esto se puede tocar" / "esto está
  encima"), nunca un adorno.
- **Percepción del relieve:** el relieve es sutil y consistente: un mismo tipo de superficie tiene
  siempre el mismo relieve. Dos cosas con el mismo relieve son la misma clase de cosa.
- **Protagonismo del contenido frente al contenedor:** **regla dura VL-LUZ.** El contenedor
  (panel, chasis, inspector) siempre está *iluminado por debajo* del contenido. La carta Pokémon, el
  preview y los datos son lo más presente; el marco es lo más discreto que pueda ser sin desaparecer.
  Si el ojo va primero al marco, la luz está mal repartida.

---

## Parte VI — FILOSOFÍA DEL MOVIMIENTO (sin curvas ni duraciones)

Integra directamente el *Animation Decision Framework* de Emil: antes de mover algo, se pregunta si
debe moverse, con qué propósito y con qué frecuencia lo verá el usuario.

- **Cuándo una interfaz merece movimiento:** solo cuando el movimiento **transporta significado** que
  la quietud no puede dar: continuidad espacial (algo entra/sale desde donde lógicamente está),
  cambio de estado, causa-efecto, o suavizar una aparición que si no sería brusca.
- **Cuándo debe permanecer completamente estática:** **acciones de alta frecuencia y acciones
  iniciadas por teclado no se animan nunca.** Abrir la Command Palette, cambiar de foco, navegar una
  lista, alternar un panel: instantáneo. Lo que se ve cientos de veces al día no puede tener latencia
  animada. La quietud es el estado por defecto; el movimiento se gana.
- **Qué representa una animación:** una animación es una **frase sobre un cambio**. Dice "esto vino de
  aquí", "esto se convirtió en aquello", "el sistema te oyó". Si una animación no está diciendo una de
  esas cosas, sobra.
- **Cuándo el movimiento aporta significado:** transiciones de plano (overlay/diálogo que entra desde
  su origen), feedback de pulsación, confirmación de un estado que cambió, revelación progresiva bajo
  intención del usuario (hover deliberado).
- **Cuándo distrae:** movimiento decorativo, bucles infinitos ambientales, animaciones "porque queda
  bien" en algo cotidiano, cualquier cosa que retrase una acción experta. La animación decorativa
  vive —si acaso— en el *contenido* (la carta), nunca en el chasis del Studio.
- **Excepción de dominio:** el *contenido* Pokémon (holográficos, ataques, previews de animación)
  tiene su propia física expresiva y NO se rige por esta sobriedad. Este canon gobierna el **marco**;
  el marco es quieto para que el contenido pueda moverse.

---

## Parte VII — FILOSOFÍA DE LOS MATERIALES (qué representa cada cosa)

No definimos materiales concretos; definimos su **significado semántico**. Un material es una
promesa sobre cómo se comporta algo.

- **Superficie:** el concepto base. Un plano neutro que *hospeda* contenido. No compite, no opina.
  Toda otra cosa es una superficie con un rol añadido. La superficie es el silencio hecho material.
- **Panel:** una superficie con **residencia fija** en el Shell (una zona Z). Representa *permanencia
  y territorio*: "aquí vive siempre este tipo de trabajo". Un panel es mobiliario, no un evento.
- **Inspector:** un panel especializado en **medir y editar las propiedades de lo seleccionado.**
  Representa *instrumento de precisión*. Su material dice "esto es exacto y editable", no "esto es
  bonito". Es el material más científico del Studio.
- **Tarjeta (Card):** una **unidad de contenido autocontenida y movible.** Representa *un objeto del
  dominio con identidad propia* (un Resource, un Variant). El material dice "esto es una cosa que
  puedes coger, seleccionar y llevar". A diferencia del panel, una tarjeta es objeto, no lugar.
- **Herramienta:** un elemento que **ejecuta o cambia de modo.** Representa *acción bajo control del
  usuario*. Su material dice "esto responde cuando lo toco" (feedback de pulsación obligatorio, Emil).
  Nunca actúa sola; siempre espera intención.
- **Ventana (Window):** la **frontera del mundo** (superficie de nivel superior; cada monitor en
  multi-monitor). Representa *el continente de todo*. Hospeda exactamente un Shell. Es lo único que
  el sistema operativo ve; para el usuario es "el Studio".
- **Overlay:** una superficie que **interrumpe temporalmente** desde encima del plano base. Representa
  *un paréntesis*: algo que pide atención ahora y luego desaparece sin dejar rastro. Por eso vive en
  un plano superior y por eso entra/sale con movimiento (Parte VI). Un overlay nunca es permanente;
  si algo debe quedarse, es un panel, no un overlay.

**Regla de materiales VL-MAT:** *el material comunica el comportamiento, no la estética.* Dos cosas
que se comportan igual comparten material; dos cosas que se comportan distinto no pueden parecer
iguales aunque simplificara el diseño.

---

## Parte VIII — FILOSOFÍA DE LA FATIGA VISUAL (uso de décadas)

El Studio debe poder mirarse ocho horas hoy y miles de horas en veinte años. La fatiga es un enemigo
de primer orden, no un detalle.

- **Fatiga cognitiva:** se combate con *consistencia y predictibilidad*. Si cada pantalla obedece las
  mismas leyes, el cerebro deja de "parsear la interfaz" y se dedica a la tarea. La sorpresa cuesta
  energía; el Studio no sorprende con su chasis.
- **Fatiga visual:** se combate evitando el contraste máximo permanente (Parte V), los bordes duros
  innecesarios, el brillo y la saturación de fondo. El chasis es un lugar de descanso para el ojo.
- **Repetición y memoria muscular:** **la memoria muscular es sagrada.** Nada de lo que el usuario
  hace cientos de veces puede cambiar de sitio, de tamaño de zona o de comportamiento sin una razón
  de peso y una migración. Mover un elemento familiar es romper una destreza construida en años.
- **Saturación y carga perceptiva:** en cualquier momento, la cantidad de cosas que compiten por la
  atención se mantiene baja. Densidad de *información*, sí; densidad de *estímulos que gritan*, no.
- **Claridad y lectura prolongada:** el texto de trabajo (datos, propiedades, nombres) está calibrado
  para leerse durante horas: sin contraste hiriente, con ritmo y longitud de línea legibles (los
  valores concretos llegan en Fase 8, pero la *intención* se fija aquí). Leer en el Studio nunca debe
  cansar más que leer un buen documento.

---

## Parte IX — FILOSOFÍA DEL CONTENIDO (quién es el protagonista)

Reglas universales de protagonismo, en orden estricto. En cualquier conflicto, gana el de arriba:

1. **La carta Pokémon / el contenido del dominio es SIEMPRE el protagonista visual.** Todo lo demás
   existe para servirla. Es la obra; el Studio es el museo.
2. **Los datos son el segundo protagonista** cuando la tarea es medir/editar (inspector, property
   grid): en esos contextos el dato manda sobre la decoración de su contenedor.
3. **El preview** es protagonista cuando el usuario está *observando un resultado* (una animación, un
   render): entonces el marco cede casi por completo (modo cercano al silencio total, Parte X).
4. **Las herramientas** solo son protagonistas en el *instante* en que se usan; después vuelven al
   fondo.
5. **La interfaz (el chasis) NUNCA es el protagonista.** Es el elemento que más debe desaparecer. Una
   pantalla donde el marco llama más la atención que el contenido está, por definición, mal diseñada.
6. **La animación del propio Studio jamás es protagonista** (Parte VI). La animación *del contenido*
   sí puede serlo, porque entonces es contenido.

**Regla universal VL-CONT:** *el protagonista es la razón por la que el usuario abrió esa pantalla.*
El diseño de cada pantalla debe poder nombrar a su protagonista en una frase; si no puede, no está
lista.

---

## Parte X — FILOSOFÍA DEL SILENCIO VISUAL

El silencio es la herramienta más poderosa del Studio y su rasgo de identidad más distintivo. Se
define en cuatro modos, y cada elemento está siempre en uno:

- **DESAPARECER (silencio total):** cuando el usuario está concentrado en el contenido (previsualizar,
  observar una animación, leer datos densos). El chasis baja al mínimo perceptible. La interfaz "se
  aparta". Es el modo por defecto del marco durante el trabajo profundo.
- **NEUTRO (silencio de reposo):** el estado normal de todo lo que está presente pero inactivo. Ni se
  esconde ni destaca: está, disponible, sin pedir nada. La mayor parte del Studio vive aquí.
- **DESTACAR (voz baja):** cuando un elemento contiene el foco actual o el dato relevante. Sube su
  presencia lo justo para ganar la primera lectura, *bajando lo demás* más que gritando él.
- **LLAMAR LA ATENCIÓN (voz alta, excepcional):** reservado para lo que el usuario **debe** ver:
  errores, pérdidas potenciales de trabajo, confirmaciones destructivas. Es el único caso donde el
  chasis puede ser insistente, y precisamente por ser raro, funciona. Si todo grita, nada se oye.

**Regla del silencio VL-SIL:** *un elemento sube de nivel solo por una razón nombrable, y baja en
cuanto la razón desaparece.* El nivel por defecto de cualquier cosa nueva es NEUTRO; subir requiere
justificación.

---

## Parte XI — FILOSOFÍA DE LA EVOLUCIÓN (sobrevivir décadas)

El lenguaje debe poder evolucionar sin perder identidad. La identidad no está en los valores (que
cambiarán) sino en las **leyes** (que no). Reglas de evolución:

- **VL-EVO-1 — Identidad por leyes, no por píxeles.** Una pantalla pertenece al Studio si obedece
  este canon, aunque su paleta o tipografía sean nuevas. Se evoluciona la realización, no las leyes.
- **VL-EVO-2 — Evolución por extensión, nunca por contradicción** (hereda F-EVO del PDS). Un nuevo
  patrón visual se *añade*; jamás redefine el significado de uno existente. Cambiar un significado =
  versión mayor del lenguaje, con migración explícita.
- **VL-EVO-3 — El re-tema es libre; la gramática es fija.** Cambiar color/tipografía/motion en bloque
  (nuevo token set) es legítimo y esperado con el tiempo. Cambiar qué significa el silencio, el
  relieve o el protagonismo NO lo es.
- **VL-EVO-4 — Toda pantalla nueva pasa la Prueba de Identidad Visual** (abajo). Es el mecanismo que
  garantiza que "dentro de veinte años" una pantalla nueva siga siendo del mismo Studio.

**¿Cómo sabremos en 2046 que una pantalla pertenece al Studio?** No por reconocer un color: por
reconocer *el reposo, el silencio calibrado, el contenido como protagonista, la memoria muscular
intacta y la consistencia obsesiva*. Esas cinco cosas son la huella dactilar.

---

# Principios del Visual Language

- **VL-P1 · Silencio como identidad.** El rasgo distintivo del Studio es un chasis silencioso y
  reposado ejecutado con maestría. El silencio no es ausencia de diseño: es diseño invisible.
- **VL-P2 · El contenido manda.** La carta/dato/preview es siempre el protagonista; el marco cede.
- **VL-P3 · Sobriedad artesanal.** El carácter se logra puliendo, no decorando. Los detalles
  invisibles se suman (Emil).
- **VL-P4 · Reposo antes que impacto.** Diseñado para ocho horas y para décadas, no para una captura
  de pantalla.
- **VL-P5 · Precisión perceptible.** Todo es exacto, alineado y responde al instante. La tecnología se
  siente como confianza mecánica, no como estética futurista.
- **VL-P6 · Movimiento con significado.** La quietud es el defecto; el movimiento se gana y siempre
  dice algo.
- **VL-P7 · Consistencia obsesiva.** Ante la duda, repetir lo existente. La inconsistencia es el
  defecto visual más grave.
- **VL-P8 · Cuatro sensaciones en proporción.** Tecnológica 40 / científica 25 / artesanal 25 /
  creativa 10 — la creativa la trae el contenido, no el marco.
- **VL-P9 · Identidad por leyes, no por valores.** Lo que permanece a través de cualquier repintado es
  la identidad.

---

# Reglas Universales

1. **VL-U1 —** Toda pantalla tiene exactamente un protagonista nombrable; el chasis nunca lo es.
2. **VL-U2 —** El contenedor se ilumina por debajo del contenido (VL-LUZ). Si el ojo va al marco, está
   mal.
3. **VL-U3 —** El nivel de silencio por defecto de cualquier elemento nuevo es NEUTRO; subir de nivel
   exige una razón nombrable y bajar es automático al desaparecer esa razón (VL-SIL).
4. **VL-U4 —** Nada iniciado por teclado o de alta frecuencia se anima jamás.
5. **VL-U5 —** El contraste máximo y la saturación fuerte están prohibidos como estado por defecto del
   chasis; se reservan para foco, dato clave y alerta.
6. **VL-U6 —** El material comunica comportamiento, no estética: mismo comportamiento ⇒ mismo material.
7. **VL-U7 —** La memoria muscular es sagrada: nada familiar cambia de sitio, tamaño de zona o
   comportamiento sin razón de peso y migración.
8. **VL-U8 —** La profundidad es semántica y mínima: solo hay tantos planos como significados.
9. **VL-U9 —** El aire agrupa y separa; nunca es decoración vacía.
10. **VL-U10 —** Toda decisión visual futura (tokens, componentes, pantallas) se justifica citando
    este documento; si no puede, no es canónica.

---

# Anti-Principios (lo que el Studio NUNCA debe ser)

- **Nunca un escaparate.** No se diseña para impresionar en una captura; se diseña para trabajar.
- **Nunca "gamer" ni sci-fi.** Sin neón, HUD, rejillas futuristas, glow, cromados ni estética de
  consola espacial. El dominio es lúdico; el *marco* no.
- **Nunca decorativo por defecto.** Ningún adorno, gradiente ambiental, textura o animación que no
  sirva a la tarea. La belleza llega por acabado, no por añadidos.
- **Nunca ruidoso.** Sin elementos que compiten, parpadean o piden atención sin razón. Si todo grita,
  nada se oye.
- **Nunca infantil ni festivo por defecto.** El contenido es Pokémon; el instrumento es serio.
- **Nunca inconsistente por conveniencia.** No se inventa una excepción visual para ahorrar trabajo.
- **Nunca protagonista.** El chasis que se roba la atención del contenido es un defecto, no un estilo.
- **Nunca cambiante bajo los dedos.** No se reordena, no se mueve, no sorprende al usuario experto.
- **Nunca máximo contraste como norma.** No se confunde "legible" con "hiriente".

---

# Decisiones Canónicas

- **DC-VL1 —** El arquetipo del Studio es **el taller del artesano-científico**; toda decisión visual
  futura se mide contra esa imagen.
- **DC-VL2 —** Proporción de sensaciones fijada: **Tecnológica 40 / Científica 25 / Artesanal 25 /
  Creativa 10**, con la creativa aportada por el contenido, no por el marco.
- **DC-VL3 —** El **silencio visual de cuatro modos** (Desaparecer / Neutro / Destacar / Alerta) es el
  modelo canónico de presencia; todo elemento está siempre en uno.
- **DC-VL4 —** El **protagonismo** sigue el orden fijo: contenido Pokémon → datos → preview →
  herramientas → (nunca el chasis).
- **DC-VL5 —** **Materiales por significado** (Superficie/Panel/Inspector/Tarjeta/Herramienta/Ventana/
  Overlay): definidos por comportamiento, realizados con tokens en fases posteriores.
- **DC-VL6 —** La **iluminación** es difusa y uniforme; contraste y relieve son funcionales y mínimos;
  el contenedor siempre por debajo del contenido.
- **DC-VL7 —** El **movimiento** obedece el framework de decisión (frecuencia → propósito → estática
  por defecto); el marco es quieto para que el contenido pueda moverse.
- **DC-VL8 —** La **identidad reside en las leyes de este documento**, no en los valores; el re-tema es
  libre, la gramática es fija (VL-EVO).
- **DC-VL9 —** La **Prueba de Identidad Visual** (abajo) es requisito de aprobación obligatorio para
  cualquier diseño visual futuro.

---

# Prueba de Identidad Visual

**Requisito de aprobación obligatorio.** Ningún token set (Fase 8), estilo de componente (Fase 9) ni
pantalla/Lab (Fase 10) se considera canónico hasta responder **SÍ** a todas estas preguntas. Un solo
"NO" bloquea la aprobación hasta corregirse.

### Pertenencia
1. ¿Se reconoce inmediatamente como parte del ecosistema PTCG Developer Studio?
2. ¿Podría convivir con una pantalla diseñada dentro de diez o veinte años sin parecer de otra app?
3. ¿Obedece las *leyes* del canon, aunque use valores visuales nuevos? (VL-EVO-1)

### Personalidad
4. ¿Transmite el arquetipo del taller artesano-científico (serio, no solemne)?
5. ¿Respeta la proporción de sensaciones (tecnológica/científica/artesanal/creativa)? (DC-VL2)
6. ¿Está libre de estética "gamer/sci-fi", adorno y festividad por defecto? (Anti-Principios)

### Contenido y protagonismo
7. ¿Puede nombrarse en una frase quién es el protagonista de la pantalla? (VL-U1)
8. ¿El contenido (carta/dato/preview) domina sobre el chasis? (VL-P2)
9. ¿El contenedor está iluminado por debajo del contenido? (VL-U2)

### Silencio y reposo
10. ¿Cada elemento está en un modo de silencio justificado, con NEUTRO por defecto? (VL-SIL)
11. ¿Está libre de contraste máximo, saturación y ruido como estado por defecto? (VL-U5)
12. ¿Podría mirarse ocho horas seguidas sin fatiga? (Parte VIII)

### Movimiento
13. ¿Toda animación dice algo, y nada de alta frecuencia/teclado se anima? (VL-U4)

### Consistencia y evolución
14. ¿Reutiliza patrones existentes antes de inventar, sin introducir excepciones? (VL-P7)
15. ¿Preserva la memoria muscular (nada familiar se ha movido sin razón)? (VL-U7)
16. ¿Cada decisión visual puede justificarse citando este documento? (VL-U10)

---

# Auditoría Semántica

Contra `MENTAL-MODEL.md`. Este documento introduce vocabulario *visual*, no de dominio, y se cuida de
no colisionar con el Canon terminológico:

- **"Superficie", "Panel", "Inspector", "Tarjeta", "Herramienta", "Ventana", "Overlay"** se usan aquí
  como *materiales semánticos* y coinciden 1:1 con los organismos/plantillas del PDS (Fase 6): sin
  divergencia. "Tarjeta (Card)" mantiene su significado del PDS (unidad de contenido movible) y no se
  confunde con "carta Pokémon" (contenido de dominio) — se escribe siempre "carta Pokémon" para el
  dominio y "Tarjeta/Card" para el componente. Recomendación de higiene: mantener esa disciplina
  ortográfica en fases siguientes.
- **"Silencio", "protagonista", "material", "luz", "reposo"** son términos *conceptuales* nuevos, sin
  colisión con el Modelo Mental (que gobierna dominio: Project/Lab/Resource/Variant/…). No se
  introduce ninguna palabra que compita con un concepto de dominio existente.
- Un concepto = una palabra: los cuatro modos de silencio tienen nombres únicos e invariantes
  (Desaparecer/Neutro/Destacar/Alerta). No hay sinónimos flotando.
- ¿Puede un usuario nuevo construir el modelo? Sí: el arquetipo (taller artesano-científico) ancla
  todo lo demás de forma memorable.

**Veredicto:** sin ambigüedad, sin duplicación, escala a décadas. **Aprobada.**

---

# Auditoría de Coherencia

Contra todo el canon existente:

1. **¿Contradice P1–P15?** No. Refuerza P15 (honestidad: reposo/no-espectáculo), la productividad
   (densidad alta organizada) y la accesibilidad (contraste calibrado, foco visible heredado del PDS).
2. **¿Contradice la Arquitectura Espacial (Z1–Z12)?** No. La usa como base de "organización" y
   "memoria muscular"; no reubica zonas.
3. **¿Contradice los Workflows?** No. El silencio y el protagonismo *sirven* a los workflows (foco en
   la tarea, comparación, preview).
4. **¿Contradice el Interaction Canon (IC-0…IC-13)?** No. La filosofía del movimiento es la lectura
   *visual* del framework de interacción (IC-8 no bloqueante, feedback de pulsación, etc.).
5. **¿Contradice el PDS (Fase 6)?** No. Cumple exactamente su promesa: el PDS reservó la capa de
   tokens y prohibió hornear visual; este documento define la *intención* que esos tokens realizarán,
   sin crear componentes ni valores.
6. **¿Introduce excepciones o duplicación innecesarias?** No: reutiliza los materiales del PDS como
   vocabulario y evita crear un léxico paralelo.
7. **¿Respeta la Regla de Oro (sin lógica de juego)?** Sí: es puramente lenguaje visual del chasis; la
   expresividad del contenido Pokémon se declara *fuera* de esta sobriedad, sin meter reglas de juego.
8. **¿Escala a cientos de Labs y décadas?** Sí, por diseño (VL-EVO: identidad por leyes).
9. **¿Genera deuda?** No; la reduce, al dar criterio previo a tokens/componentes/pantallas.
10. **¿Canónico?** Sí.

**Veredicto: Canónico.**

---

# Auditoría de Filosofía

Contra `PRINCIPIO-DESARROLLO-UNIPERSONAL.md`:

1. **¿Respeta el Desarrollo Unipersonal?** Sí. Todo el lenguaje asume un único usuario humano
   trabajando en profundidad; el foco/silencio están calibrados para una atención, no para paneles
   de control colaborativos.
2. **¿Introduce complejidad multi-desarrollador?** No. No hay estados de "otros usuarios", presencia,
   ni convenciones visuales de colaboración.
3. **¿Puede simplificarse por ser un solo desarrollador?** Ya lo está: un solo protagonista, un solo
   foco, memoria muscular de una sola persona.
4. **¿Las IA siguen siendo herramientas, no actores?** Sí. Si una IA produce resultados, se muestran
   como *contenido/datos* dentro del lenguaje normal, no como un "actor" con voz visual propia.
5. **¿Mantiene simplicidad, preservación y evolución a largo plazo?** Es precisamente su tesis central
   (Partes VIII y XI).

**Veredicto: alineado con los cuatro pilares.**

---

# Impacto en el Futuro

- **Condiciona directamente la Fase 8 (Visual Tokens):** cada token (color, tipografía, elevación,
  espaciado, opacidad, motion) deberá derivarse de una ley de este canon. El contraste calibrado
  (VL-U5), la luz difusa (DC-VL6) y el silencio de cuatro modos (DC-VL3) fijan de antemano el *rango*
  admisible de valores. Habrá probablemente **un token set base ("Studio")** y variantes re-tematizadas
  (launcher, visor) que comparten gramática (VL-EVO-3).
- **Condiciona la Fase 9 (Component Styling):** el estilo de cada componente del PDS deberá elegir su
  modo de silencio por defecto (casi todos NEUTRO), su material semántico y su comportamiento de
  movimiento. La Prueba de Identidad Visual se aplica componente a componente.
- **Condiciona la Fase 10 (Screen Design / Labs):** cada pantalla deberá declarar su protagonista
  (VL-U1) y pasar la Prueba completa antes de aprobarse.
- **Oportunidades que habilita:** re-tematización sin pérdida de identidad; onboarding de pantallas
  nuevas con criterio objetivo; una marca visual reconocible sin depender de un color concreto.
- **Limitaciones que introduce (deliberadas):** prohíbe una vía "espectáculo/gamer" que podría tentar
  en marketing; obliga a resistir el impulso decorativo. Es coste asumido a cambio de longevidad.
- **Riesgos a vigilar en fases siguientes:** (a) que los tokens de la Fase 8 suban el contraste "para
  que se vea más pro" y rompan VL-U5; (b) *creep* de animación decorativa en el chasis; (c) confusión
  ortográfica "Tarjeta/Card" vs "carta Pokémon"; (d) que la expresividad del contenido Pokémon se
  filtre al marco. La Prueba de Identidad Visual es la barrera contra los cuatro.

---

# ¿A la altura de un producto AAA?

*¿Estaría esta decisión a la altura de Figma / Unreal / JetBrains / VS Code / Unity?*

Sí, y por la razón correcta: las herramientas AAA de referencia comparten exactamente esta tesis —un
chasis silencioso, reposado y consistente que hace protagonista al contenido (el diseño en Figma, la
escena en Unreal, el código en VS Code). Ninguna gana por decorar su marco; ganan por *acabado
invisible y reposo sostenible*, que es justo lo que aquí se canoniza. El Studio se distingue además
por un rasgo propio y defendible: la **proporción artesano-científica** y el modelo explícito de
**silencio de cuatro modos**, que le dan carácter sin sacrificar sobriedad. Lo que aún **le falta**
para materializar este estándar —y es trabajo legítimo de las Fases 8–10— es la *realización*:
tokens calibrados, estilos de componente y pantallas que superen la Prueba. Como *lenguaje*, está a
la altura AAA; como *apariencia*, la prueba llega después. **Aprobado como canon.**
