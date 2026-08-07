# Developer Studio — Principios Canónicos de UX

**Fase de diseño de EXPERIENCIA. No hay código, ni pantallas, ni interfaces, ni carpetas.**
Este documento responde a UNA pregunta:

> **¿Cómo debe *sentirse* usar el Developer Studio todos los días durante los próximos 30 años?**

Es la constitución de UX del Studio. Toda pantalla futura se justificará contra estos
principios. Si una pantalla los viola, la pantalla está mal — no el principio.

---

## Parte I — El flujo de trabajo diario (análisis, no interfaz)

Antes de fijar principios hay que entender el *día real* de quien usa esto. No diseñamos
para una demo: diseñamos para la persona que abrirá esta herramienta 5.000 veces.

### 1. Los tres modos de uso (y su proporción real)

El trabajo diario NO es homogéneo. Se reparte en tres modos con frecuencias muy distintas,
y esa proporción debe dictar qué es fácil y qué puede costar más:

- **Modo ITERACIÓN (≈70% del tiempo).** Ya sé qué estoy tocando (una animación, un shader,
  un escenario de reglas). Ajusto un parámetro, veo el resultado, ajusto otro. Es un bucle
  cerrado y muy repetido: *editar → observar → juzgar → repetir*, decenas de veces por
  minuto. Aquí se gana o se pierde la herramienta. Cada fricción se paga multiplicada por
  miles.
- **Modo EXPLORACIÓN (≈20%).** No sé exactamente qué busco. Comparo variantes, hojeo el
  catálogo, busco "esa animación de hace dos años", contrasto contra un benchmark. Es
  navegación y comparación, no ajuste fino.
- **Modo AUTORÍA/CURADURÍA (≈10%).** Guardo una variante, promuevo a canónica, documento,
  archivo un snapshot, limpio, versiono. Es infrecuente pero **crítico para los 30 años**:
  es lo que hace que el trabajo sobreviva.

**Consecuencia:** el bucle de iteración debe ser casi instantáneo y sin cambios de contexto;
la exploración debe escalar a miles de elementos sin ahogar; la autoría puede pedir más
pasos (es rara y conviene que sea deliberada), pero nunca debe ser tan tediosa que la gente
la evite y pierda su trabajo.

### 2. Qué ocurre al abrir el Studio

Al abrir, el desarrollador casi nunca llega "en blanco". Llega **continuando algo**: "ayer
estaba afinando el overshoot del Draw Card". El coste psicológico más alto de cualquier
herramienta es la **reconstrucción de contexto** ("¿dónde estaba?, ¿qué tenía abierto?, ¿qué
valores había puesto?"). Una herramienta que se usa a diario y que obliga a reconstruir el
contexto cada mañana sangra tiempo y voluntad.

Por eso lo primero que debe ocurrir al abrir no es "elige un laboratorio" desde cero, sino
**recuperar el estado exacto** de la última sesión: mismo Lab, mismo recurso, mismos
overrides no guardados, misma posición de timeline. El punto de partida por defecto es
*"sigue donde lo dejaste"*, no *"empieza de nuevo"*.

### 3. El bucle de iteración, en detalle

En el 70% del tiempo, la secuencia es: **veo el resultado → identifico qué está mal → toco
el parámetro responsable → vuelvo a ver**. Para que esto no fatigue:

- El **resultado** (preview) y el **control** (el parámetro) tienen que estar visibles **a la
  vez**. Si para cambiar un valor tengo que tapar el preview, rompo el bucle: pierdo la
  referencia visual justo cuando la necesito.
- La **relación causa-efecto** debe ser inmediata y evidente: muevo `overshoot` y veo el
  cambio *sin recargar, sin confirmar, sin esperar*. La latencia entre acción y resultado es
  el enemigo número uno de la iteración; por encima de ~100 ms el cerebro deja de percibir
  "yo causé esto" y empieza a percibir "esperé a que pasara".
- Debo poder **repetir la observación** a voluntad (reproducir la animación otra vez) sin
  reconstruir nada. Para juzgar 8% vs 10% de overshoot necesito verlos varias veces; si cada
  reproducción cuesta preparar, no comparo, adivino.

### 4. Qué se hace con más frecuencia (candidatos a "cero fricción")

Ordenado por frecuencia observable en el trabajo real de animación/shaders/reglas:

1. **Reproducir / re-reproducir** el preview (constante).
2. **Ajustar un parámetro** y ver el efecto (constante).
3. **Comparar dos estados** (variante A vs B, actual vs benchmark) (muy frecuente).
4. **Saltar a otro recurso** relacionado (frecuente).
5. **Guardar/nombrar** una versión que vale la pena (periódico).
6. **Buscar** un recurso entre miles (periódico, crece con los años).

Las acciones 1–3 deben tender a **cero o un gesto**. Las 4–6 pueden costar un poco más, pero
nunca deben requerir "salir de lo que estoy haciendo".

### 5. La dimensión temporal: el problema de los 30 años

Lo que es cómodo con 10 recursos es inhabitable con 10.000. El Studio nace pequeño y morirá
gigante: cientos de laboratorios, miles de animaciones, cientos de shaders, décadas de
snapshots. El diseño debe asumir la **escala desde el día 1**, porque migrar la UX cuando ya
duele es tardísimo. Esto obliga a que la organización sea **emergente y buscable**, no
enumerada: nadie va a recorrer una lista de 10.000 elementos. También obliga a que la
herramienta envejezca bien: un snapshot de hoy debe abrirse dentro de 30 años y *sentirse*
igual de navegable.

---

## Parte II — Principios de UX (justificados, con sus conflictos)

Cada principio incluye el *porqué técnico* y, cuando aplica, con qué otro principio choca.
Los conflictos NO se ocultan: se declaran y se resuelve cuál gana en cada contexto.

### P1 — Continuidad de contexto: abrir = continuar
El estado por defecto al abrir es la sesión anterior restaurada al detalle. **Justificación:**
la reconstrucción de contexto es el mayor impuesto oculto de una herramienta diaria; en 5.000
aperturas, cada segundo de "¿dónde estaba?" son horas perdidas y, peor, desgaste de voluntad.
La continuidad convierte el arranque en un no-evento.

### P2 — El resultado es el rey; el resto sirve al resultado
El preview (lo que estás creando) es el centro permanente; los controles orbitan a su
alrededor y ceden espacio cuando estorban. **Justificación:** en modo iteración se juzga con
los ojos, no con los números. El artefacto visual debe dominar la atención y el espacio; los
paneles son instrumentos, no protagonistas. *Conflicto con P4 (información siempre visible):*
maximizar el preview compite con mantener datos a la vista. **Resolución:** el preview gana
espacio *físico*; la información de contexto se mantiene visible pero *comprimida y
periférica* (glanceable), no expandida.

### P3 — Bucle cerrado sin cambio de contexto
Editar y observar ocurren en el mismo lugar y al mismo tiempo; nunca "ve a otra ventana para
cambiar esto". **Justificación:** cada cambio de ventana/modo cuesta reorientación (coste de
*context switch*) y rompe la memoria visual de referencia justo en el instante de la
comparación. El bucle *editar→ver* del 70% del tiempo debe caber en una sola vista sin saltos.

### P4 — Visible lo permanente, oculto lo ocasional (jerarquía por frecuencia)
Solo permanece siempre a la vista lo que se consulta constantemente (estado del preview,
parámetros del objeto activo, controles de reproducción). Todo lo demás (catálogo completo,
historial, metadatos, exportación, docs) aparece **bajo demanda** y se retira al terminar.
**Justificación:** la carga cognitiva y visual crece con cada elemento presente; mostrar todo
"por si acaso" satura y esconde lo importante entre ruido. La visibilidad es un presupuesto
escaso que se asigna por frecuencia de uso, no por completitud. *Conflicto con P2:* ver más
información tienta a llenar la pantalla. **Resolución:** revelación progresiva — la
información existe siempre, pero su *presencia visual* es proporcional a su frecuencia de uso.

### P5 — Coste proporcional a la frecuencia
Lo que se hace mil veces al día cuesta cero o un gesto; lo que se hace una vez a la semana
puede costar varios pasos deliberados. **Justificación:** optimizar lo raro a costa de lo
frecuente es el error clásico. Reproducir, ajustar y comparar (P.I §4) deben ser inmediatos;
guardar/promover/archivar pueden —y deben— ser más lentos y conscientes, porque son decisiones
con consecuencias de largo plazo. *Conflicto con P8 (reversibilidad/seguridad):* hacer barato
lo frecuente puede facilitar errores destructivos. **Resolución:** barato ≠ irreversible;
todo lo barato es también deshacible (P8), así la velocidad no genera miedo.

### P6 — Comparación como ciudadano de primera clase
Contrastar dos cosas (variante A/B, actual vs benchmark, v3 vs v7) es una operación central,
no un extra. **Justificación:** el juicio de calidad visual es casi siempre *relativo*, no
absoluto ("¿este overshoot es mejor que el otro?"). Sin comparación fluida, el desarrollador
decide de memoria, y la memoria visual es pobre y sesgada. La herramienta debe permitir poner
dos estados frente a frente sin ceremonia.

### P7 — Escala por búsqueda y estructura emergente, no por enumeración
La forma primaria de llegar a un recurso es **describir lo que buscas** (buscar/filtrar por
nombre, tag, tipo, estado, fecha), no recorrer listas. La organización se **deriva** de los
metadatos, no se mantiene a mano. **Justificación:** con miles de recursos, cualquier
taxonomía fija se rompe y toda lista se vuelve inmanejable; solo la búsqueda y el filtrado
escalan a 30 años. Enumerar es O(n) para el humano; buscar es O(1) percibido.

### P8 — Todo es reversible; nada asusta
Cualquier acción de edición se puede deshacer; nada valioso se pierde por un clic. Guardar es
explícito; perder es imposible. **Justificación:** el miedo a romper algo mata la
experimentación, y la experimentación es el propósito entero del Studio. Una herramienta que
castiga el error enseña a no explorar. La reversibilidad barata es lo que permite hacer P5
(velocidad) sin ansiedad. *Conflicto con P5:* la seguridad total puede añadir confirmaciones
que ralentizan. **Resolución:** en vez de "¿estás seguro?", *deshacer* — se actúa rápido y se
revierte si hace falta, en lugar de frenar cada acción con un guardián.

### P9 — Consistencia estructural entre laboratorios
Todo laboratorio se siente igual: el sitio del preview, de los parámetros, de la timeline, de
la búsqueda, es el mismo, aunque el contenido cambie. **Justificación:** con cientos de Labs a
lo largo de décadas, aprender cada uno por separado es inviable. Un esqueleto común convierte
"aprender un Lab nuevo" en "reconocer dónde está todo". La transferencia de aprendizaje entre
Labs debe ser total: sé usar uno, sé usar todos. *Conflicto con P12 (adaptabilidad por
dominio):* cada Lab tiene necesidades propias que empujan hacia layouts distintos.
**Resolución:** el *marco* es invariante (dónde vive cada cosa); el *contenido* de cada zona
es libre. Consistencia de estructura, libertad de contenido.

### P10 — Confort para sesiones largas (diseñar contra la fatiga)
La herramienta se usará en sesiones de horas; debe minimizar la fatiga visual y física.
**Justificación:** la fatiga degrada el juicio (justo lo que se necesita para valorar
animación) y expulsa al usuario. Esto implica: reposo visual (nada que parpadee, vibre o
compita por atención sin motivo), estética sobria y de bajo contraste agresivo, foco visual
en una sola zona activa cada vez, y evitar que el ojo salte constantemente por la pantalla. La
comodidad no es cosmética: es lo que permite las 8ª hora tan lúcida como la 1ª.

### P11 — Latencia percibida cercana a cero
La respuesta a las acciones frecuentes (editar parámetro, reproducir, comparar) debe sentirse
instantánea; y cuando algo tarde de verdad, decirlo con honestidad sin bloquear. **Justificación:**
la sensación de causalidad ("yo hice esto") se rompe con la latencia; por encima de ~100 ms el
bucle de iteración se degrada a "petición y espera". La fluidez del bucle es literalmente la
propuesta de valor del Studio (acelerar el desarrollo). Cuando la operación es intrínsecamente
lenta, el principio no es "hazla rápida" sino "no mientas ni congeles": progreso visible,
trabajo no bloqueante.

### P12 — Adaptabilidad al dominio y al usuario, sobre una base estable
El desarrollador puede acomodar su espacio (qué paneles ve, tamaños, disposición) y cada Lab
puede ofrecer lo que su dominio necesita, pero siempre sobre el esqueleto común (P9).
**Justificación:** en 30 años cambiarán las personas, los flujos y los dominios; una UI rígida
envejece mal. La adaptabilidad prolonga la vida útil. *Conflicto con P9 (consistencia):* la
personalización total fragmenta la experiencia. **Resolución:** se adapta la *disposición y el
detalle*, nunca la *gramática* (los conceptos y su ubicación relativa). Hay defaults sensatos
para que la adaptabilidad sea opcional, no una tarea obligatoria de configuración.

### P13 — El trabajo sobrevive al momento (preservación como experiencia, no solo como dato)
Todo lo que valga la pena debe poder capturarse y reencontrarse años después, y *sentirse*
igual de vivo al reabrirlo. **Justificación:** el objetivo declarado es reproducir una
animación de hace 10 años bit a bit; para que eso sea *usable* (no solo *posible*), la
preservación debe integrarse en el flujo diario — capturar un buen resultado no puede ser un
ritual aparte, y reabrir un snapshot antiguo debe restaurar el contexto (P1) como si fuera
hoy. La memoria del proyecto es un rasgo de experiencia, no un backend.

### P14 — Enseñar dentro del flujo, no en un manual aparte
El conocimiento (el estándar visual de un benchmark, la ficha de research, la revisión
crítica, el porqué de una decisión) vive *junto* a lo que documenta, disponible cuando se
necesita y ausente cuando no. **Justificación:** en un proyecto de **un único desarrollador
durante décadas**, el que "vuelve a un tema años después" es *tu propio yo futuro* (y, como
herramientas de asistencia, las IA que retomen contexto). Mostrar el "porqué" al lado del "qué"
convierte el Studio en su propia documentación viva y hace que retomar cualquier recurso —tras
meses o años— no dependa de la memoria. *Conflicto con P4 (ocultar lo ocasional):* la docs no
debe saturar. **Resolución:** la enseñanza es *invocable en contexto*, no permanente.

### P15 — Honestidad y confianza: la herramienta no engaña
Lo que ves es lo que hay; el Studio no maquilla el resultado ni finge fidelidad que no tiene.
**Justificación:** el propósito es *juzgar* calidad; una herramienta que embellece o distorsiona
el preview corrompe el juicio y erosiona la confianza — y sin confianza, la gente vuelve a
"probarlo en el juego", que es justo lo que queremos eliminar. Esto tiene una consecuencia
directa con la estrategia C→A del preview: cuando el preview es *esquemático* (headless), la
herramienta debe **decir claramente que no es pixel-perfect**, para que nadie confunda "válido
para timing/pipeline" con "válido para juzgar el juice". La honestidad sobre las limitaciones
es parte de la confianza.

---

## Parte III — Tensiones estructurales (mapa de conflictos)

Los principios no son independientes; tiran unos de otros. Estas son las tensiones que todo
diseño de pantalla deberá arbitrar conscientemente:

- **Espacio del preview (P2) ⨯ Información visible (P4).** → El preview gana metros; la
  información gana *presencia proporcional a su frecuencia*, comprimida y periférica.
- **Velocidad (P5) ⨯ Seguridad (P8).** → Se actúa rápido porque todo es deshacible; la
  seguridad se logra con *reversibilidad*, no con *confirmaciones*.
- **Consistencia entre Labs (P9) ⨯ Adaptabilidad por dominio (P12).** → Marco invariante,
  contenido libre. Se adapta la disposición, nunca la gramática.
- **Riqueza de contexto/enseñanza (P13, P14) ⨯ Sobriedad y anti-fatiga (P4, P10).** → El
  contexto está *disponible*, no *presente*: se invoca y se retira.

Regla de arbitraje general: **ante la duda, gana el bucle de iteración del 70%** (P2, P3, P5,
P11). El Studio existe para acelerar ese bucle; todo lo demás lo sirve.

---

## Parte IV — Los Principios Canónicos (lista de referencia)

Base para TODAS las pantallas futuras. Cualquier decisión de interfaz deberá poder citar cuál
de estos satisface y con cuál negocia.

1. **P1 · Abrir = continuar.** El arranque restaura la sesión; nunca empieza de cero.
2. **P2 · El resultado es el rey.** El preview domina; todo lo demás lo sirve.
3. **P3 · Bucle cerrado.** Editar y observar, mismo lugar, mismo instante, sin saltos.
4. **P4 · Visible lo permanente, oculto lo ocasional.** Visibilidad = frecuencia de uso.
5. **P5 · Coste proporcional a la frecuencia.** Lo diario, cero clics; lo raro, deliberado.
6. **P6 · Comparar es de primera clase.** Dos estados frente a frente sin ceremonia.
7. **P7 · Escala por búsqueda, no por enumeración.** Describir, no recorrer; estructura emergente.
8. **P8 · Todo reversible; nada asusta.** Deshacer sobre confirmar; perder es imposible.
9. **P9 · Consistencia estructural entre Labs.** Aprende uno, sabes todos.
10. **P10 · Confort para sesiones largas.** Diseñar activamente contra la fatiga.
11. **P11 · Latencia percibida ≈ cero.** Instantáneo lo frecuente; honesto lo lento.
12. **P12 · Adaptable sobre base estable.** Cambia la disposición, no la gramática.
13. **P13 · El trabajo sobrevive.** Preservar y reencontrar como parte del flujo diario.
14. **P14 · Enseñar en contexto.** El porqué vive junto al qué, invocable.
15. **P15 · La herramienta no engaña.** Fidelidad honesta; declara sus límites.

---

**Ámbito y límite de esta fase:** este documento fija *cómo debe sentirse*, no *cómo se ve*.
No propone ninguna interfaz, pantalla ni disposición concreta. El siguiente nivel de diseño
(cuando se autorice) traducirá estos principios en estructura visual — y se validará, pantalla
por pantalla, contra esta lista canónica.
