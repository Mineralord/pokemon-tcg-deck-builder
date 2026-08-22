# FASE 9 — PvP 

# Capítulo I — Arquitectura General del Jugador contra Jugador 

## Objetivo del capítulo 

Este capítulo define la arquitectura general del sistema PvP del proyecto y establece cómo los jugadores pueden enfrentarse entre sí. Su propósito es crear una base sólida, escalable y permanente que permita incorporar todos los modos competitivos presentes y futuros sin modificar el motor principal del juego. 

El PvP no constituye un sistema independiente del resto del proyecto, sino una extensión natural del motor central de Pokémon TCG. Por ello, todas las partidas PvP utilizan exactamente las mismas reglas, cartas, efectos y mecánicas que el PvE, garantizando una única implementación del reglamento para todo el juego. 

# 1. Filosofía del PvP 

El sistema PvP se diseña siguiendo los principios establecidos en las fases anteriores del proyecto: 

- Un único reglamento para todo el juego. 

- Igualdad absoluta entre todos los jugadores. 

- Libertad para jugar cuando y con quien se desee, respetando las reglas de acceso de cada modalidad. 

- Preservación histórica de todas las cartas y formatos. 

- Compatibilidad permanente con futuras expansiones y modos de juego. 

- Arquitectura desacoplada del sistema de progresión, economía y coleccionismo. 

El PvP nunca utilizará reglas distintas a las oficiales del Pokémon TCG salvo cuando un formato específico las defina expresamente. 

# 2. Arquitectura general 

El sistema PvP estará dividido en cinco grandes componentes: 

- Sistema de acceso a partidas. 

- Sistema de emparejamiento. 

- Sistema de gestión de partidas. 

- Motor de reglas compartido con PvE. 

- Sistema de finalización y recompensas. 

Cada componente será independiente para facilitar el mantenimiento del proyecto durante décadas. 

# 3. Sistema de acceso a partidas 

Todo combate entre jugadores se iniciará mediante uno de los siguientes mecanismos. 

## 3.1 Combates entre amigos 

Los jugadores podrán agregarse como amigos dentro del juego. 

Una vez establecida la amistad, cualquiera de ellos podrá enviar una invitación de combate en cualquier momento. 

El jugador receptor podrá aceptar o rechazar la invitación. 

Este sistema permitirá organizar partidas privadas sin depender del matchmaking automático. 

Las partidas entre amigos podrán utilizar cualquier formato permitido por el juego, incluyendo formatos históricos, formatos oficiales, formatos personalizados o cualquier modalidad que exista en el futuro. 

Asimismo, podrán disputarse tanto en: 

- Modo en Tiempo Real. 

- Modo por Turnos. 

Este sistema representa la forma principal de juego social dentro de la comunidad privada del proyecto. 

## 3.2 Eventos y torneos 

Los eventos y torneos funcionarán de manera completamente independiente del sistema de amistades. 

Los participantes **no necesitarán ser amigos** para enfrentarse entre sí. 

Al inscribirse en un evento, el sistema concederá acceso temporal al emparejamiento correspondiente al torneo. 

Durante la competición, cualquier participante podrá ser emparejado contra cualquier otro participante según las reglas del evento. 

Una vez finalizado el torneo, dicho acceso temporal desaparecerá automáticamente, manteniendo intacta la privacidad de la lista de amigos de cada jugador. 

Esta arquitectura permite organizar: 

- Torneos internos. 

- Ligas. 

- Copas. 

- Eventos especiales. 

- Campeonatos de cualquier tamaño. 

sin necesidad de modificar el sistema de amistades. 

# 4. Principios de diseño 

El sistema de acceso al PvP se rige por los siguientes principios: 

- La amistad únicamente habilita los combates privados. 

- Los torneos utilizan su propio sistema de emparejamiento. 

- Ningún jugador necesita agregar como amigo a otro para participar en eventos oficiales. 

- El sistema será escalable para soportar desde pequeños grupos privados hasta comunidades mucho más grandes. 

- La arquitectura permanecerá compatible con todas las futuras modalidades competitivas del proyecto. 

# Conclusión del capítulo 

La arquitectura general del PvP establece una separación clara entre el juego social y la competición organizada. Los **combates entre amigos** permiten retar libremente a personas de confianza en cualquier momento, mientras que los **eventos y torneos** crean un entorno competitivo donde los participantes pueden enfrentarse entre sí sin necesidad de mantener una relación de amistad dentro del juego. 

Con esta base, el sistema PvP conserva la privacidad de los jugadores, facilita la organización de competiciones de cualquier escala y garantiza una arquitectura robusta, escalable y compatible con la filosofía de preservación permanente del proyecto. 

Capítulo II — Modalidades de Combate 

### Objetivo del capítulo 

Este capítulo define las modalidades oficiales mediante las cuales podrán disputarse los combates PvP dentro del proyecto. Su finalidad es ofrecer diferentes formas de jugar sin fragmentar la comunidad ni alterar el reglamento oficial del Pokémon TCG. 

Las modalidades únicamente modifican la forma en que se desarrolla la partida o cómo se organiza el enfrentamiento; nunca modifican las reglas del juego. 

### 1. Principio de un único reglamento 

Todas las modalidades PvP utilizarán exactamente el mismo motor de reglas. 

No existirán versiones alternativas del reglamento para cada modo de juego. 

Independientemente de la modalidad elegida, todas las partidas compartirán: 

Las mismas reglas oficiales del Pokémon TCG. 

Las mismas cartas. 

Los mismos efectos. 

La misma resolución de habilidades. 

Las mismas condiciones de victoria y derrota. 

Las mismas verificaciones realizadas por el motor del juego. 

Esto garantiza una experiencia consistente y facilita el mantenimiento del proyecto durante décadas. 

### 2. Modalidades oficiales de combate 

El sistema PvP contará con dos modalidades principales. 

### 2.1 Modo en Tiempo Real 

En esta modalidad ambos jugadores permanecen conectados simultáneamente. 

Las acciones se realizan de manera continua respetando el flujo normal del turno. 

El ritmo de la partida dependerá de los tiempos establecidos para cada acción, permitiendo una experiencia similar a la de Pokémon TCG Live o a una partida presencial. 

Esta modalidad está diseñada para: 

Partidas casuales. 

Retos entre amigos. 

Eventos rápidos. 

Torneos presenciales o en línea. 

Competiciones oficiales. 

2.2 Modo por Turnos 

Esta modalidad permite que los jugadores disputen una partida sin necesidad de permanecer conectados al mismo tiempo. 

Cada jugador realiza su turno cuando le resulte conveniente. 

Después de finalizarlo, el servidor guarda el estado completo de la partida y notifica al rival que puede continuar. 

Una partida podrá prolongarse durante horas, días o incluso semanas sin afectar su validez. 

El estado de la partida permanecerá almacenado hasta que exista un ganador, una rendición o cualquier otra condición oficial de finalización. 

Esta modalidad facilita el juego entre personas con horarios diferentes y favorece la preservación de las partidas dentro de una comunidad privada. 

### 3. Compatibilidad entre modalidades 

Todas las modalidades compartirán exactamente el mismo sistema de: 

Construcción de mazos. 

Inventario. 

Formatos disponibles. 

Motor de reglas. 

Recompensas. 

Registro del evento cuando corresponda. 

Cambiar de modalidad nunca requerirá modificar el mazo ni adaptar las cartas utilizadas. 

La única diferencia será la forma en que ambos jugadores interactúan durante la partida. 

### 4. Selección de modalidad 

La modalidad será definida antes del inicio del combate. 

Una vez iniciada la partida, no podrá cambiarse hasta que esta finalice. 

En los combates entre amigos, ambos jugadores deberán aceptar la modalidad elegida. 

En torneos y eventos, la modalidad será establecida por la configuración del organizador y será igual para todos los participantes de ese evento. 

### 5. Escalabilidad 

La separación entre modalidades permitirá incorporar nuevas formas de organizar partidas en el futuro sin modificar el motor del juego. 

Cualquier modalidad adicional utilizará el mismo reglamento oficial y únicamente añadirá una nueva forma de gestionar el flujo del combate. 

De esta manera, el proyecto conserva una arquitectura modular, fácil de mantener y preparada para evolucionar durante toda la vida útil del juego. 

Conclusión del capítulo 

El sistema PvP dispondrá de dos modalidades oficiales: Tiempo Real y Por Turnos. Ambas compartirán el mismo reglamento, el mismo motor de juego y las mismas cartas, diferenciándose únicamente en la forma en que los jugadores desarrollan la partida. 

Esta arquitectura garantiza una experiencia uniforme, accesible y escalable, permitiendo que cada jugador elija la modalidad que mejor se adapte a su disponibilidad sin comprometer la integridad competitiva ni la fidelidad al Pokémon TCG oficial. 

FASE 9 — PvP 

Capítulo III — Inicio de un Combate PvP 

Auditoría de coherencia 

Antes de definir el flujo de inicio del combate, se verificó su compatibilidad con las fases canónicas anteriores. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Se mantiene un único flujo de juego para todos los jugadores. 

No existen diferencias entre partidas privadas y competitivas en cuanto al reglamento. 

Compatibilidad con Fase 1 — Filosofía del Juego 

Se preserva la fidelidad absoluta al Pokémon TCG. 

El sistema únicamente organiza el enfrentamiento; no modifica las reglas. 

Compatibilidad con Fase 7 — Perfil, Museo y Legado 

El inicio del combate utiliza el perfil del jugador, su colección y sus mazos sin crear copias temporales. 

Compatibilidad con Fase 8 — PvE 

Se reutiliza exactamente la misma secuencia oficial de preparación de partida definida para el motor del juego. 

La única diferencia es que el oponente es otro jugador en lugar de una IA. 

No se detectan conflictos arquitectónicos. 

1. Objetivo 

El sistema de inicio de combate tiene como finalidad preparar una partida PvP garantizando que ambos jugadores comiencen exactamente en las mismas condiciones. 

Durante esta fase se validan todos los requisitos necesarios antes de entregar el control del primer turno. 

2. Selección del enfrentamiento 

Todo combate PvP comienza cuando dos jugadores quedan emparejados mediante alguno de los sistemas oficiales: 

Emparejamiento automático. 

Invitación directa. 

Sala privada. 

Evento organizado. 

Torneo. 

Independientemente del origen del enfrentamiento, el flujo posterior será idéntico. 

3. Verificación previa 

Antes de iniciar la partida, el sistema comprobará automáticamente: 

Que ambos jugadores poseen un mazo válido para el formato seleccionado. 

Que el formato permite utilizar dicho mazo. 

Que ambos clientes disponen de la información necesaria para representar todas las cartas del combate. 

Que ambos jugadores cumplen los requisitos del modo seleccionado. 

Si alguna validación falla, la partida no comenzará. 

4. Creación de la partida 

Una vez superadas las verificaciones: 

Se crea una instancia independiente del combate. 

Se asigna un identificador único. 

Se registra el formato. 

Se registra la modalidad (Tiempo Real o Por Turnos). 

Se vinculan ambos jugadores a esa instancia. 

A partir de ese momento, todas las acciones quedarán asociadas exclusivamente a dicha partida. 

5. Preparación oficial 

Tras crear la partida, comienza la secuencia oficial de preparación definida por el reglamento del Pokémon TCG. 

Esta secuencia incluye, entre otras acciones: 

Determinación del jugador inicial. 

Barajado de los mazos. 

Robo de la mano inicial. 

Comprobación de Pokémon Básicos. 

Mulligan cuando corresponda. 

Colocación del Pokémon Activo. 

Colocación de la Banca. 

Colocación de las cartas de Premio. 

Toda esta lógica será ejecutada por el motor del juego, garantizando que PvP y PvE compartan exactamente el mismo comportamiento. 

6. Sincronización del estado inicial 

Antes de conceder el primer turno, ambos jugadores deberán tener exactamente el mismo estado de partida. 

El sistema sincronizará: 

Mazo. 

Mano. 

Pokémon Activo. 

Banca. 

Cartas de Premio. 

Descartes. 

Zona Perdida. 

Energías. 

Contadores. 

Efectos activos. 

Estado del turno. 

La partida únicamente podrá comenzar cuando ambos estados sean idénticos. 

7. Entrega del primer turno 

Una vez completada la preparación: 

Se determina oficialmente el jugador inicial. 

El sistema habilita únicamente las acciones permitidas durante ese turno. 

El jugador obtiene el control del combate. 

Desde este momento la partida entra en la fase normal de juego, gobernada íntegramente por el motor de reglas. 

8. Recuperación ante interrupciones 

Si una interrupción ocurre antes del primer turno: 

En Tiempo Real, la partida podrá cancelarse o reiniciarse según las reglas del modo de juego. 

En Modo por Turnos, el estado inicial permanecerá almacenado hasta que ambos jugadores puedan continuar. 

Esto garantiza la integridad de la partida sin alterar el reglamento. 

Conclusión del capítulo 

El inicio de un combate PvP constituye una fase de preparación totalmente controlada por el sistema, cuyo objetivo es asegurar que ambos jugadores comiencen bajo condiciones idénticas. La arquitectura reutiliza íntegramente la secuencia oficial de preparación ya establecida para el motor del juego, manteniendo una única implementación compartida entre PvE y PvP, reforzando la consistencia, la escalabilidad y la preservación del proyecto a largo plazo. 

FASE 9 — PvP 

Capítulo IV — Desarrollo de la Partida PvP 

Auditoría de coherencia 

Antes de definir el desarrollo de un combate PvP, se verificó su compatibilidad con todas las fases canónicas aprobadas. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Se mantiene un único motor de reglas. 

No existen diferencias de reglamento entre modos de juego. 

Compatibilidad con Fase 1 — Filosofía del Juego 

Fidelidad absoluta al Pokémon TCG oficial. 

El jugador conserva el control completo de sus decisiones. 

La experiencia prioriza el juego auténtico sobre la automatización innecesaria. 

Compatibilidad con Fase 8 — PvE 

PvP y PvE utilizan exactamente la misma lógica de juego. 

La única diferencia es el origen de las decisiones (jugador humano o IA). 

No se detectan conflictos arquitectónicos. 

1. Objetivo 

Una vez finalizada la preparación oficial, la partida entra en su fase principal de juego. 

Durante esta etapa, ambos jugadores alternarán turnos siguiendo estrictamente el reglamento oficial del Pokémon TCG hasta que se produzca una condición válida de finalización. 

El sistema actúa únicamente como árbitro, garantizando el cumplimiento de las reglas. 

2. Un único motor de juego 

Todo combate PvP será gobernado exclusivamente por el motor de reglas del proyecto. 

El motor será responsable de: 

Validar todas las acciones. 

Resolver habilidades. 

Resolver ataques. 

Aplicar efectos continuos. 

Gestionar estados especiales. 

Controlar las condiciones de victoria y derrota. 

Mantener el estado completo de la partida. 

La interfaz de usuario nunca ejecutará lógica de juego. 

3. Flujo del turno 

Cada turno seguirá exactamente la secuencia definida por el reglamento oficial. 

Entre otras acciones, el motor gestionará: 

Inicio del turno. 

Robo de carta. 

Acciones permitidas durante el turno. 

Uso de habilidades. 

Evoluciones. 

Colocación de Energías. 

Uso de cartas de Entrenador. 

Retirada. 

Ataque. 

Finalización del turno. 

El sistema impedirá automáticamente cualquier acción ilegal. 

4. Validación de acciones 

Cada acción enviada por un jugador será validada antes de ejecutarse. 

Si la acción cumple todas las reglas: 

Será aplicada al estado de la partida. 

Se actualizarán todas las zonas afectadas. 

Se resolverán los efectos correspondientes. 

Si la acción es inválida: 

Será rechazada. 

El estado de la partida permanecerá sin cambios. 

El jugador podrá realizar otra acción permitida. 

5. Sincronización del estado 

Después de cada acción válida, ambos jugadores deberán visualizar exactamente el mismo estado del combate. 

El sistema mantendrá sincronizadas todas las zonas de juego, incluyendo: 

Mano. 

Mazo. 

Descarte. 

Zona Perdida. 

Pokémon Activo. 

Banca. 

Cartas de Premio. 

Energías. 

Herramientas Pokémon. 

Contadores de daño. 

Condiciones Especiales. 

Efectos activos. 

La sincronización es obligatoria para preservar la integridad competitiva. 

6. Registro de acciones 

Todas las acciones realizadas durante una partida quedarán registradas cronológicamente. 

Este registro permitirá: 

Reconstruir la partida completa. 

Analizar incidencias. 

Generar repeticiones. 

Resolver posibles disputas en eventos organizados. 

El historial forma parte del estado oficial del combate. 

7. Independencia de la modalidad 

El desarrollo interno del combate será idéntico tanto en: 

Tiempo Real. 

Modo por Turnos. 

La diferencia entre ambas modalidades reside únicamente en el momento en que cada jugador realiza sus acciones. 

El motor de reglas ejecutará exactamente la misma secuencia en ambos casos. 

8. Integridad del combate 

Durante toda la partida, el sistema garantizará que: 

Ningún jugador pueda realizar acciones fuera de su turno. 

Ninguna carta pueda utilizarse de forma ilegal. 

Ningún efecto pueda resolverse fuera de orden. 

Ninguna información oculta sea revelada indebidamente. 

Todas las decisiones queden registradas. 

La integridad del combate prevalecerá sobre cualquier aspecto visual o de interfaz. 

Conclusión del capítulo 

El desarrollo de una partida PvP está completamente gobernado por un único motor de reglas compartido con el PvE. Todas las acciones son validadas antes de ejecutarse, el estado del combate permanece sincronizado entre ambos jugadores y cada decisión queda registrada para garantizar una experiencia fiel, consistente y preparada para la preservación histórica del proyecto. 

# FASE 9 — PvP 

# Capítulo V — Comunicación entre Jugadores 

## Auditoría de coherencia 

Antes de definir los sistemas de comunicación, se verificó su compatibilidad con las fases canónicas del proyecto. 

### **Compatibilidad con Fase 0 — Canon del Proyecto** 

- La comunicación es una característica complementaria e independiente del motor de juego. 

- No afecta la lógica ni las reglas del combate. 

### **Compatibilidad con Fase 1 — Filosofía del Juego** 

- Se fomenta una comunidad privada, sana y respetuosa. 

- La comunicación nunca debe convertirse en una herramienta para perjudicar la experiencia de juego. 

### **Compatibilidad con Fase 8 — PvE** 

- Los sistemas de comunicación son exclusivos del PvP. 

- No alteran la arquitectura compartida del motor de reglas. 

No se detectan conflictos arquitectónicos. 

# 1. Objetivo 

El sistema de comunicación permite la interacción entre jugadores durante los combates PvP. 

Su propósito es facilitar una experiencia social agradable sin comprometer la integridad competitiva, la privacidad de los jugadores ni el ritmo de la partida. 

# 2. Independencia del motor de juego 

La comunicación será completamente independiente del motor de reglas. 

Los mensajes, reacciones o cualquier otro medio de interacción: 

- No modificarán el estado del combate. 

- No afectarán las reglas. 

- No alterarán el orden de los turnos. 

- No podrán influir directamente en la resolución de acciones. 

# 3. Comunicación oficial 

El juego podrá ofrecer distintos medios oficiales de comunicación, entre ellos: 

- Mensajes predefinidos. 

- Emotes. 

- Reacciones visuales. 

- Gestos rápidos. 

- Otros sistemas sociales que puedan incorporarse en el futuro. 

Todos estos mecanismos serán opcionales y compatibles con la filosofía del proyecto. 

# 4. Control por parte del jugador 

Cada jugador tendrá control sobre la comunicación que recibe durante una partida. 

El sistema podrá permitir opciones como: 

- Mostrar toda la comunicación. 

- Limitarla a mensajes esenciales. 

- Silenciar completamente al oponente. 

Estas preferencias no afectarán el desarrollo del combate ni la experiencia del otro jugador. 

# 5. Información compartida 

La comunicación nunca podrá revelar información que el reglamento considere oculta. 

El sistema protegerá en todo momento datos como: 

- Cartas en la mano. 

- Orden del mazo. 

- Cartas de Premio. 

- Información privada del rival. 

- Cualquier otro dato no público. 

Toda interacción respetará las reglas oficiales de información visible. 

# 6. Respeto al ritmo de juego 

Los elementos de comunicación estarán diseñados para no interferir con el flujo normal de la partida. 

Por ello: 

- No bloquearán acciones del juego. 

- No pausarán el combate. 

- No modificarán los temporizadores. 

- No alterarán el turno activo. 

La prioridad siempre será la continuidad del enfrentamiento. 

# 7. Seguridad y convivencia 

La arquitectura de comunicación estará orientada a mantener un entorno respetuoso dentro de la comunidad privada del proyecto. 

Los sistemas oficiales deberán minimizar situaciones de: 

- Acoso. 

- Spam. 

- Conductas antideportivas. 

- Interacciones que deterioren la experiencia de juego. 

El objetivo es favorecer una convivencia sana sin limitar la interacción positiva entre los jugadores. 

# 8. Evolución futura 

La arquitectura permitirá incorporar nuevos sistemas sociales sin modificar el funcionamiento del PvP. 

Entre ellos podrán incluirse, en futuras fases del proyecto: 

- Nuevos emotes. 

- Reacciones personalizadas. 

- Mensajes adicionales. 

- Herramientas sociales avanzadas. 

Estas ampliaciones deberán mantenerse completamente desacopladas del motor de reglas. 

# Conclusión del capítulo 

La comunicación entre jugadores constituye un sistema social independiente del combate. Su diseño prioriza la convivencia, la privacidad y el respeto al reglamento oficial, permitiendo la interacción entre jugadores sin afectar el funcionamiento del motor de juego ni la integridad competitiva. La arquitectura queda preparada para evolucionar con nuevas funciones sociales manteniendo la fidelidad y la estabilidad del proyecto. 

FASE 9 — PvP 

Capítulo VI — Desconexión y Reconexión 

Auditoría de coherencia 

Antes de definir este capítulo se verificó su compatibilidad con las fases canónicas del proyecto. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Garantiza la preservación de las partidas. 

Favorece la estabilidad a largo plazo del proyecto. 

Compatibilidad con Fase 1 — Filosofía del Juego 

Prioriza una experiencia justa para ambos jugadores. 

Evita que problemas técnicos determinen el resultado de una partida. 

Compatibilidad con Fase 8 — PvE 

La arquitectura de reconexión puede reutilizarse en los modos que requieran conexión. 

No modifica el motor de reglas. 

No se detectan conflictos arquitectónicos. 

1. Objetivo 

El sistema de desconexión y reconexión tiene como finalidad permitir que una partida pueda continuar cuando uno de los jugadores pierda temporalmente la conexión o cierre la aplicación de forma inesperada. 

Su diseño busca proteger la integridad competitiva sin afectar el funcionamiento del motor del juego. 

2. Persistencia del estado de la partida 

Durante todo el combate, el estado de la partida deberá conservarse de forma íntegra. 

Esto incluye, entre otros: 

Campo de juego. 

Mano de ambos jugadores. 

Mazos. 

Cartas de Premio. 

Contadores. 

Condiciones Especiales. 

Orden de turnos. 

Temporizadores. 

Historial necesario para continuar la partida. 

La restauración deberá reproducir exactamente el mismo estado previo a la desconexión. 

### 3. Reconexión 

Si un jugador recupera la conexión dentro del tiempo permitido, podrá reincorporarse a la partida. 

La reconexión deberá: 

Restaurar completamente el estado del combate. 

Mantener el turno correspondiente. 

Conservar los temporizadores. 

No alterar ninguna regla del juego. 

4. Límite de espera 

El sistema establecerá un tiempo máximo de espera para la reconexión. 

Si dicho tiempo expira sin que el jugador regrese, la partida finalizará conforme a las reglas de abandono definidas por el sistema competitivo. 

Este límite deberá aplicarse de manera uniforme para todos los jugadores. 

5. Desconexiones repetidas 

La arquitectura deberá contemplar múltiples desconexiones durante una misma partida. 

Mientras cada reconexión ocurra dentro del tiempo permitido, el combate podrá continuar sin modificaciones en su estado. 

### 6. Desconexiones intencionales 

El sistema deberá impedir que una desconexión pueda utilizarse como ventaja competitiva. 

En consecuencia: 

La desconexión no pausará el desarrollo lógico de la partida más allá de lo permitido por el sistema. 

No permitirá reiniciar acciones ya confirmadas. 

No alterará el resultado de decisiones previamente registradas. 

7. Independencia del motor de reglas 

La lógica de reconexión será un componente de infraestructura y no del reglamento del juego. 

El motor continuará siendo completamente determinista; la reconexión únicamente restaurará el estado previamente almacenado. 

### 8. Evolución futura 

La arquitectura permitirá incorporar futuras mejoras, como: 

Reanudación desde distintos dispositivos. 

Restauración más rápida del estado de la partida. 

Optimizaciones de sincronización. 

Nuevos mecanismos de tolerancia a fallos. 

Estas mejoras deberán mantener la compatibilidad con las partidas existentes y no modificar las reglas oficiales del juego. 

Conclusión del capítulo 

El sistema de desconexión y reconexión garantiza que los problemas técnicos no comprometan la integridad de una partida PvP. La persistencia completa del estado del combate, junto con una arquitectura desacoplada del motor de reglas, asegura una experiencia justa, estable y preparada para la evolución permanente del proyecto. 

FASE 9 — PvP 

Capítulo VII — Abandono de la Partida (Concesión) 

Auditoría de coherencia 

Antes de definir este capítulo se verificó su compatibilidad con las fases canónicas del proyecto. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Respeta la fidelidad al reglamento oficial. 

Preserva la integridad del historial de partidas. 

Mantiene una arquitectura preparada para décadas de evolución. 

Compatibilidad con Fase 1 — Filosofía del Juego 

El jugador conserva la libertad de abandonar una partida cuando lo considere oportuno. 

Se evita prolongar partidas innecesarias. 

Compatibilidad con capítulos anteriores del PvP 

Es compatible con el sistema de tiempo. 

Es compatible con el sistema de desconexión. 

No modifica ninguna regla del combate. 

No se detectan conflictos. 

1. Objetivo 

El sistema de abandono permite que un jugador conceda voluntariamente la partida antes de que se produzca una condición normal de victoria. 

La concesión constituye una derrota inmediata para el jugador que abandona. 

2. Concesión voluntaria 

Un jugador podrá abandonar una partida en cualquier momento. 

La concesión será una decisión completamente voluntaria. 

No será necesario justificar el abandono. 

3. Confirmación 

Para evitar abandonos accidentales, el sistema solicitará una confirmación antes de finalizar la partida. 

Una vez confirmada la concesión: 

La decisión será irreversible. 

La partida finalizará inmediatamente. 

4. Resultado del combate 

Cuando un jugador conceda la partida: 

El jugador que abandona será registrado como derrotado. 

El jugador restante será registrado como vencedor. 

La partida terminará de forma inmediata sin continuar el desarrollo del turno. 

5. Estado de la partida 

La concesión no modificará: 

El historial del combate. 

Las estadísticas correspondientes. 

Los registros competitivos aplicables. 

La partida simplemente finalizará en el estado en el que se encontraba al momento del abandono. 

6. Relación con la desconexión 

La concesión y la desconexión son mecanismos independientes. 

Una desconexión podrá terminar convirtiéndose en derrota por inactividad conforme al Capítulo VI. 

Una concesión finalizará la partida de manera inmediata por decisión expresa del jugador. 

### 7. Integridad competitiva 

La concesión no permitirá: 

Evitar el registro del resultado. 

Alterar las estadísticas del combate. 

Invalidar la partida una vez iniciada. 

Obtener ventajas frente al sistema competitivo. 

Toda concesión producirá un resultado oficial. 

8. Independencia del reglamento 

La concesión es una función del sistema PvP y no modifica las reglas oficiales del Pokémon TCG. 

Las condiciones normales de victoria continúan siendo las establecidas por el reglamento oficial. 

### 9. Evolución futura 

La arquitectura permitirá incorporar funcionalidades adicionales relacionadas con el abandono, como: 

Confirmaciones configurables. 

Registro detallado de motivos (opcional). 

Estadísticas históricas de concesiones. 

Herramientas administrativas para torneos. 

Estas ampliaciones no modificarán el funcionamiento básico del sistema. 

Conclusión del capítulo 

El sistema de abandono garantiza que cualquier jugador pueda conceder una partida de forma inmediata, segura y transparente. La concesión se registra como una derrota oficial, preserva la integridad competitiva y mantiene la fidelidad al reglamento sin alterar el desarrollo del motor de juego. 

FASE 9 — PvP 

Capítulo VIII — Finalización del Combate y Registro de Resultados 

Auditoría de coherencia 

Antes de redactar este capítulo se verificó su compatibilidad con todas las fases canónicas del proyecto. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Respeta la preservación del proyecto. 

No introduce mecánicas temporales. 

Mantiene una arquitectura preparada para evolucionar durante décadas. 

Compatibilidad con Fase 6 — Progresión y Logros 

La finalización del combate sirve como punto de actualización para los sistemas de progresión y logros, sin duplicar su lógica. 

Compatibilidad con Fase 7 — Perfil del Jugador, Museo y Legado 

Se mantiene la decisión canónica de no conservar un historial permanente de partidas normales. 

Únicamente podrán conservarse registros relacionados con eventos y torneos. 

Compatibilidad con capítulos anteriores 

Es compatible con la victoria normal. 

Es compatible con la concesión. 

Es compatible con la derrota por desconexión. 

Es compatible con todas las modalidades PvP. 

No se detectan conflictos arquitectónicos. 

1. Objetivo 

La finalización del combate representa el cierre oficial de una partida PvP. 

A partir de este momento, el resultado se considera definitivo y el sistema actualiza únicamente la información necesaria para el funcionamiento del proyecto. 

2. Condiciones de finalización 

Una partida PvP podrá finalizar cuando ocurra cualquiera de las siguientes situaciones: 

Un jugador cumple una condición oficial de victoria establecida por el reglamento del Pokémon TCG. 

Un jugador concede la partida voluntariamente. 

Un jugador pierde por desconexión conforme al Capítulo VI. 

El reglamento oficial determine cualquier otra condición válida de finalización. 

El sistema no añadirá condiciones de victoria o derrota ajenas al reglamento oficial. 

### 3. Validación del resultado 

Antes de cerrar definitivamente la partida, el sistema verificará que: 

Todas las acciones pendientes hayan sido resueltas. 

No existan efectos obligatorios sin ejecutar. 

El estado final del combate sea consistente. 

Solo entonces el resultado será declarado oficial. 

4. Registro del resultado 

Una vez validado el combate, el sistema registrará: 

El jugador vencedor. 

El jugador derrotado. 

La fecha y hora de finalización. 

La modalidad utilizada. 

El formato disputado. 

El motivo de finalización (victoria normal, concesión, desconexión u otra condición oficial). 

Este registro constituye el resultado oficial del combate. 

5. Actualización del perfil 

Tras finalizar la partida, el sistema actualizará automáticamente toda la información correspondiente al perfil del jugador. 

Entre otros elementos, podrán actualizarse: 

Estadísticas generales. 

Progreso de logros. 

Misiones aplicables. 

Desafíos activos. 

Progreso de eventos o torneos. 

Cada uno de estos sistemas continuará siendo administrado por su fase correspondiente. 

6. Historial de partidas 

De acuerdo con el canon establecido en la Fase 7: 

Las partidas PvP normales no conservarán un historial permanente. 

Una vez registrados sus resultados y actualizado el progreso correspondiente, la información detallada del combate podrá eliminarse. 

Únicamente los eventos y torneos podrán conservar los registros necesarios para su administración, clasificación o consulta histórica. 

7. Cierre de la sesión 

Tras registrar el resultado: 

La sesión de combate será finalizada. 

Se liberarán los recursos asociados. 

Ambos jugadores regresarán a la interfaz correspondiente. 

La partida no podrá reanudarse una vez concluida oficialmente. 

8. Escalabilidad 

La arquitectura permitirá incorporar futuras funcionalidades relacionadas con el cierre de partidas, como: 

Estadísticas más avanzadas. 

Nuevos indicadores de rendimiento. 

Integración con futuros sistemas competitivos. 

Estas ampliaciones no modificarán el proceso oficial de finalización del combate. 

Conclusión del capítulo 

La finalización del combate constituye el cierre oficial de una partida PvP. El sistema valida el resultado, actualiza el progreso del jugador y registra únicamente la información necesaria para el funcionamiento del proyecto. En coherencia con el canon establecido, las partidas normales no generan un historial permanente, mientras que los eventos y torneos pueden conservar los registros necesarios para garantizar su correcta administración y preservar la historia competitiva de la comunidad. 

FASE 9 — PvP 

Capítulo IX — Salas Privadas, Eventos y Torneos 

Auditoría de coherencia 

Antes de definir este capítulo se verificó su compatibilidad con todas las fases canónicas del proyecto. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Respeta la filosofía de comunidad privada. 

Mantiene la preservación de la historia competitiva. 

No introduce restricciones incompatibles con el crecimiento futuro del proyecto. 

Compatibilidad con Fase 1 — Filosofía del Juego 

Favorece la libertad del jugador. 

Permite la organización de actividades comunitarias sin alterar el reglamento oficial. 

Compatibilidad con Fase 7 — Perfil del Jugador, Museo y Legado 

Se mantiene la decisión de conservar únicamente los registros relacionados con eventos y torneos. 

Las partidas privadas continúan sin historial permanente. 

Compatibilidad con la futura Fase 10 — Ranked, Temporadas y Competición 

Este capítulo define únicamente la infraestructura para organizar enfrentamientos. 

Los sistemas de clasificación, temporadas y recompensas competitivas serán desarrollados exclusivamente en la Fase 10. 

No se detectan conflictos arquitectónicos. 

1. Objetivo 

Este capítulo establece la infraestructura que permitirá organizar combates entre múltiples jugadores dentro de la comunidad del proyecto. 

Su finalidad es proporcionar herramientas para crear actividades organizadas sin modificar el funcionamiento del motor de juego. 

2. Salas privadas 

El juego permitirá la creación de salas privadas. 

Una sala privada es un espacio temporal donde uno o varios jugadores pueden reunirse para organizar combates. 

Las salas podrán utilizarse para: 

Partidas amistosas. 

Entrenamientos. 

Ligas privadas. 

Eventos comunitarios. 

Torneos. 

La existencia de una sala no modifica el reglamento del juego. 

3. Configuración de una sala 

El creador de una sala podrá definir, entre otros aspectos: 

Nombre de la sala. 

Modalidad (Tiempo Real o Por Turnos). 

Formato de juego permitido. 

Número máximo de participantes. 

Tipo de acceso (abierto o mediante invitación). 

Estas configuraciones únicamente determinan la organización de los enfrentamientos y nunca alteran las reglas del Pokémon TCG. 

4. Eventos 

Un evento es una actividad organizada con un conjunto de reglas administrativas previamente definidas. 

Un evento podrá establecer aspectos como: 

Fecha de inicio. 

Fecha de finalización. 

Formato autorizado. 

Modalidad de juego. 

Requisitos de participación. 

Durante un evento, los participantes podrán enfrentarse entre sí sin necesidad de formar parte de su lista de amigos. 

5. Torneos 

Un torneo es un tipo específico de evento cuyo objetivo es determinar un resultado competitivo entre los participantes. 

El sistema permitirá organizar torneos de distintos tamaños y estructuras. 

La forma de competición (eliminación directa, sistema suizo, ligas u otros modelos) será definida por la configuración del torneo correspondiente. 

La infraestructura estará preparada para admitir nuevos formatos organizativos sin requerir cambios en el motor del juego. 

6. Emparejamientos 

Durante un torneo o evento, el sistema gestionará los emparejamientos conforme a la configuración establecida por el organizador. 

Los participantes podrán ser enfrentados contra cualquier otro jugador inscrito, independientemente de su lista de amigos. 

Una vez concluido el evento, este acceso temporal desaparecerá automáticamente. 

7. Registro de eventos y torneos 

En coherencia con el canon del proyecto, los eventos y torneos podrán conservar información histórica como: 

Participantes. 

Emparejamientos. 

Resultados. 

Clasificación final. 

Campeones. 

Fechas de celebración. 

Estos registros constituyen la memoria competitiva de la comunidad y podrán preservarse de forma permanente. 

8. Independencia del motor de juego 

Las salas privadas, los eventos y los torneos son sistemas organizativos. 

No contienen reglas propias del Pokémon TCG. 

Todas las partidas disputadas dentro de ellos serán gobernadas exclusivamente por el motor oficial del juego. 

9. Escalabilidad 

La arquitectura permitirá incorporar en el futuro nuevas herramientas de organización sin modificar la estructura del PvP. 

Entre ellas podrán incluirse: 

Ligas permanentes. 

Copas temáticas. 

Campeonatos estacionales. 

Eventos especiales. 

Competiciones creadas por la comunidad. 

Todas ellas reutilizarán la misma infraestructura definida en este capítulo. 

Conclusión del capítulo 

Las salas privadas, los eventos y los torneos constituyen la infraestructura organizativa del PvP. Permiten a la comunidad crear y participar en actividades de cualquier escala sin alterar el reglamento oficial ni el motor del juego. La separación entre organización y reglas garantiza una arquitectura limpia, escalable y preparada para preservar la historia competitiva del proyecto durante décadas. 

FASE 9 — PvP 

Capítulo X — Arquitectura del Sistema PvP 

Auditoría de coherencia 

Antes de definir este capítulo se verificó su compatibilidad con todas las fases canónicas del proyecto. 

Compatibilidad con Fase 0 — Canon del Proyecto 

Mantiene una arquitectura modular y preparada para evolucionar durante décadas. 

Favorece la preservación técnica del proyecto. 

No introduce dependencias innecesarias. 

Compatibilidad con todas las fases anteriores 

El PvP se integra con: 

Perfil del jugador. 

Colección. 

Constructor de mazos. 

Economía. 

Logros. 

Museo. 

PvE. 

Sin duplicar información ni lógica. 

Compatibilidad con la Fase 10 

La Fase 10 añadirá únicamente sistemas competitivos (Ranked, temporadas y competición). 

Toda la infraestructura necesaria para soportarlos queda definida en este capítulo. 

No existen conflictos arquitectónicos. 

1. Objetivo 

El sistema PvP constituye la infraestructura encargada de permitir los enfrentamientos entre jugadores. 

Su responsabilidad es organizar, sincronizar y administrar las partidas. 

Las reglas del Pokémon TCG continúan perteneciendo exclusivamente al motor del juego. 

2. Separación de responsabilidades 

El PvP no interpreta cartas. 

No calcula efectos. 

No decide acciones. 

No modifica reglas. 

Su función consiste únicamente en: 

Localizar rivales; 

Crear partidas; 

Sincronizar jugadores; 

Administrar sesiones; 

Registrar resultados; 

Gestionar eventos y torneos. 

Toda la lógica del reglamento permanece centralizada en el motor del juego. 

3. Componentes principales 

La arquitectura PvP estará formada por los siguientes componentes: 

Sistema de Amigos 

Gestiona las relaciones entre jugadores. 

Permite enviar invitaciones de combate. 

Sistema de Invitaciones 

Gestiona el envío, aceptación y cancelación de retos privados. 

Sistema de Emparejamiento 

Localiza automáticamente un rival cuando una modalidad lo requiera. 

Sistema de Salas 

Administra las salas privadas creadas por los jugadores. 

Sistema de Eventos 

Gestiona la inscripción y organización de eventos comunitarios. 

Sistema de Torneos 

Organiza competiciones oficiales y comunitarias. 

Gestiona emparejamientos, rondas y resultados. 

Sistema de Sesiones 

Mantiene cada combate activo. 

Administra: 

Conexión; 

Reconexión; 

Sincronización; 

Finalización. 

Sistema de Resultados 

Registra el resultado oficial del combate y comunica la información a los sistemas correspondientes del proyecto. 

4. Integración con el motor del juego 

Toda partida PvP utilizará exactamente la misma instancia del motor utilizada por el PvE. 

El PvP únicamente envía las acciones realizadas por los jugadores. 

El motor responde indicando: 

Si la acción es válida; 

Cómo cambia el estado de la partida; 

Qué efectos deben resolverse. 

Esta arquitectura garantiza una única implementación del reglamento. 

5. Integración con otros sistemas 

El PvP reutiliza directamente: 

Perfil del jugador. 

Colección. 

Constructor de mazos. 

Formatos de juego. 

Sistema de logros. 

Sistema de progresión. 

Museo. 

Legado. 

No existen versiones específicas para PvP. 

Todos los sistemas son compartidos. 

### 6. Escalabilidad 

La arquitectura ha sido diseñada para crecer sin modificar sus fundamentos. 

Será posible incorporar en el futuro: 

Nuevos formatos; 

Nuevos modos de juego; 

Nuevos sistemas competitivos; 

Nuevas herramientas sociales; 

Nuevas modalidades de eventos. 

Estas ampliaciones reutilizarán la infraestructura ya existente. 

7. Mantenibilidad 

Cada componente del PvP funcionará de forma independiente. 

Una modificación en un sistema no deberá afectar al resto. 

Esta separación reduce la complejidad del mantenimiento y facilita la evolución continua del proyecto. 

8. Preservación técnica 

El diseño del PvP prioriza la estabilidad a largo plazo. 

Toda la arquitectura busca minimizar la duplicación de lógica, facilitar la compatibilidad retroactiva y garantizar que las futuras generaciones puedan seguir disfrutando del juego utilizando la misma base tecnológica. 

### Conclusión del capítulo 

La arquitectura del sistema PvP establece una infraestructura modular, desacoplada y escalable, cuya única responsabilidad es organizar los enfrentamientos entre jugadores. Todas las reglas del Pokémon TCG permanecen centralizadas en un único motor compartido con el PvE, mientras que los sistemas de amigos, invitaciones, salas, eventos, torneos y sesiones actúan como servicios especializados que pueden evolucionar independientemente. Esta organización garantiza un mantenimiento sencillo, una compatibilidad permanente y una base sólida para desarrollar la Fase 10 — Ranked, Temporadas y Competición, sin introducir cambios estructurales en el PvP ya consolidado. 

