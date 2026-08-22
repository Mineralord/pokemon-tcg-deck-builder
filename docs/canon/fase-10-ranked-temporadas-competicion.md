FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo I — Principios Fundamentales del Sistema Competitivo 

Objetivo del capítulo 

Este capítulo establece los principios que regirán todo el sistema competitivo del proyecto. 

Su propósito es definir qué representa el modo Ranked, cuál es su papel dentro del juego y cómo convivirá con el resto de sistemas sin entrar en conflicto con el canon establecido en las fases anteriores. 

Los capítulos posteriores desarrollarán la implementación técnica de estos principios. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con todas las fases canónicas. 

Compatibilidad con la Fase 8 (PvE) 

El PvE y el PvP comparten exactamente el mismo motor de reglas, pero tienen objetivos distintos. 

El PvE está orientado a la progresión personal del jugador frente a la inteligencia artificial. 

El PvP Ranked está orientado exclusivamente a la competición entre jugadores. 

Por ello, ambos sistemas utilizarán arquitecturas de progresión completamente independientes. 

Compatibilidad con la Fase 9 (PvP) 

La Fase 9 define cómo se juega una partida entre dos jugadores. 

La Fase 10 no modifica ninguna regla del combate. 

Únicamente define cómo se organizan las competiciones oficiales y cómo se mide el rendimiento competitivo. 

Compatibilidad con la Filosofía del Proyecto 

El proyecto prioriza la preservación del Pokémon TCG por encima de cualquier sistema competitivo. 

El modo Ranked representa una forma de disfrutar el juego, no su finalidad principal. 

Todo el contenido del proyecto continuará siendo plenamente disfrutable sin participar en la competición organizada. 

- 1.1 Naturaleza del sistema competitivo 

1.2 

El sistema competitivo constituye el entorno oficial donde los jugadores pueden medir su habilidad enfrentándose entre sí bajo condiciones de absoluta igualdad. 

Su objetivo es: 

Ofrecer partidas equilibradas; 

Generar una clasificación objetiva; 

Preservar el historial competitivo del proyecto. 

El sistema competitivo nunca modifica las reglas del Pokémon TCG. 

- 1.3 Separación entre PvE y PvP 

1.4 

Aunque ambos modos utilizan exactamente el mismo motor de juego, representan experiencias completamente distintas. 

PvE 

El PvE incorpora un sistema propio de progresión basado en categorías y rangos, que refleja el avance del jugador frente al contenido controlado por la inteligencia artificial. 

Este sistema pertenece exclusivamente al PvE y no tiene ninguna influencia sobre la competición entre jugadores. 

PvP Ranked 

El PvP competitivo no utiliza categorías ni rangos de progresión. 

La habilidad del jugador se representa mediante un Rating Competitivo y su posición dentro de la clasificación oficial de cada temporada. 

De esta forma, ambos modos mantienen identidades claramente diferenciadas y evitan duplicar sistemas de progresión. 

- 1.3 Igualdad de condiciones 

Toda partida Ranked comienza en igualdad absoluta. 

El sistema nunca otorgará ventajas por: 

Antigüedad; 

Nivel de cuenta; 

Progreso PvE; 

Cantidad de partidas jugadas; 

Recompensas obtenidas anteriormente. 

La única diferencia entre jugadores será: 

Su habilidad; 

La construcción de su baraja; 

Las decisiones tomadas durante la partida. 

- 1.5 La habilidad como único criterio competitivo 

1.6 

El modo Ranked existe para medir el rendimiento deportivo. 

La clasificación nunca representará el tiempo invertido en el juego. 

Un jugador únicamente mejorará su posición mediante resultados obtenidos dentro de la competición oficial. 

- 1.5 Independencia de los sistemas 

El sistema competitivo funciona de forma independiente respecto a: 

PvE; 

Economía; 

Crafting; 

Colección; 

Museo; 

Perfil del jugador. 

Estos sistemas pueden compartir información histórica cuando sea necesario, pero ninguno modifica el funcionamiento interno del modo Ranked. 

1.7 Preservación histórica 

1.8 

Cada temporada constituye un documento histórico del proyecto. 

Al finalizar una temporada se conservarán permanentemente: 

La clasificación final; 

Los participantes; 

Las estadísticas; 

Los resultados oficiales; 

Las recompensas obtenidas. 

La información histórica nunca será eliminada ni sobrescrita. 

1.7 Transparencia 

El sistema competitivo debe ser comprensible para todos los jugadores. 

Cada participante podrá conocer en todo momento: 

Su clasificación; 

Su progreso; 

Los resultados de sus partidas; 

Las consecuencias de cada victoria o derrota. 

La competición nunca dependerá de mecánicas ocultas destinadas a favorecer o perjudicar artificialmente a un jugador. 

# 1.9 Escalabilidad 

1.10 

El sistema competitivo está diseñado para mantenerse vigente durante décadas. 

Su arquitectura permitirá incorporar: 

Nuevos formatos; 

Nuevas temporadas; 

Nuevos eventos oficiales; 

Nuevas modalidades competitivas. 

Todo ello sin alterar el historial previamente registrado. 

# 1.9 Neutralidad competitiva 

El sistema nunca manipulará los resultados ni el emparejamiento para favorecer determinados comportamientos. 

No existirán mecanismos destinados a: 

Facilitar ascensos; 

Provocar descensos; 

Aumentar artificialmente el tiempo de juego; 

Favorecer a determinados jugadores. 

Todos los participantes competirán bajo exactamente las mismas reglas. 

- 1.11 Principio de convivencia entre PvE y PvP 

- 1.12 

El proyecto establece una separación permanente entre la progresión PvE y la competición PvP. 

El PvE representa el progreso del jugador mediante categorías y rangos propios. 

El PvP representa la habilidad competitiva mediante un sistema de clasificación independiente. 

Esta decisión evita duplicar sistemas de progresión, refuerza la identidad de cada modo de juego y garantiza una arquitectura sólida, coherente y preparada para evolucionar durante décadas sin generar contradicciones entre ambos entornos. 

Resultado del capítulo 

Con este capítulo queda establecida la filosofía del sistema competitivo del proyecto. 

Se define que: 

El modo Ranked constituye el entorno oficial de competición entre jugadores; 

PvE y PvP comparten el mismo motor de reglas, pero poseen sistemas de progresión completamente independientes; 

Los rangos y categorías pertenecen exclusivamente al PvE; 

El PvP competitivo utilizará un sistema propio de clasificación, desarrollado en los capítulos siguientes; 

La competición se fundamenta en la igualdad de condiciones, la transparencia, la preservación histórica y la habilidad demostrada por cada jugador. 

El siguiente capítulo definirá la Arquitectura del Sistema Ranked, estableciendo cómo se organiza el modo competitivo dentro del ecosistema PvP sin alterar el canon definido en la Fase 9. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo II — Arquitectura del Sistema Ranked 

# Objetivo del capítulo 

Este capítulo define la arquitectura general del modo Ranked y su integración con el sistema PvP establecido en la Fase 9. 

Su propósito es establecer cómo funciona el entorno competitivo sin modificar las reglas del juego ni la arquitectura del PvP ya definida. 

El modo Ranked constituye una modalidad oficial del PvP destinada exclusivamente a la competición organizada entre jugadores. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con la Fase 9 (PvP) 

La Fase 9 estableció todas las reglas relacionadas con los combates entre jugadores. 

El modo Ranked reutiliza íntegramente dicha arquitectura. 

No modifica: 

Las reglas del combate; 

El motor de juego; 

El flujo de una partida; 

Las mecánicas del Pokémon TCG. 

Únicamente añade un contexto competitivo. 

Compatibilidad con la Fase 8 (PvE) 

Aunque PvE y PvP utilizan exactamente el mismo motor de reglas, ambos permanecen completamente separados. 

El progreso obtenido en PvE no influye sobre el sistema Ranked. 

Del mismo modo, el rendimiento competitivo no modifica la progresión del PvE. 

Compatibilidad con la Filosofía del Proyecto 

El modo Ranked representa una alternativa para quienes desean competir. 

No constituye la experiencia principal del proyecto. 

Todos los jugadores podrán disfrutar del juego completo sin participar nunca en la competición organizada. 

2.1 Definición del modo Ranked 

El modo Ranked es el entorno oficial de competición del proyecto. 

Su finalidad es permitir que los jugadores compitan bajo condiciones iguales y obtengan una clasificación basada exclusivamente en su rendimiento competitivo. 

El sistema Ranked no introduce reglas especiales de juego. 

Todas las partidas utilizan exactamente el mismo reglamento del Pokémon Trading Card Game. 

2.2 Modalidad oficial del PvP 

El modo Ranked forma parte del sistema PvP. 

No constituye un modo independiente. 

Es una modalidad especializada que añade: 

Clasificación competitiva; 

Emparejamiento especializado; 

Temporadas oficiales; 

Historial competitivo; 

Recompensas de temporada. 

Todo lo demás continúa funcionando conforme a la Fase 9. 

2.3 Acceso al modo Ranked 

El acceso al modo Ranked estará disponible para cualquier jugador que cumpla los requisitos generales establecidos por el proyecto. 

Nunca dependerá de: 

Pagos; 

Antigüedad; 

Nivel PvE; 

Progresión dentro del Museo; 

Recompensas obtenidas anteriormente. 

La competición permanecerá abierta en igualdad de condiciones para toda la comunidad. 

2.4 Formatos competitivos 

El sistema Ranked podrá ofrecer distintos formatos oficiales. 

Cada formato constituirá un entorno competitivo independiente. 

Ejemplos: 

Estándar. 

Expandido. 

Formatos históricos. 

Formatos especiales definidos por la organización. 

Cada uno dispondrá de su propia clasificación y temporada cuando corresponda. 

2.5 Independencia entre formatos 

Los formatos Ranked no comparten clasificación. 

Cada uno mantiene de forma independiente: 

Rating Competitivo; 

Clasificación; 

Historial; 

Estadísticas; 

Recompensas de temporada. 

Un jugador puede destacar en un formato sin que ello afecte a los demás. 

# 2.6 Exclusividad del modo En Vivo 

Conforme al canon establecido en la Fase 9, el sistema Ranked utilizará exclusivamente partidas En Vivo. 

Las partidas Por Turnos permanecerán disponibles para: 

PvP casual; 

Desafíos entre amigos; 

Otros contextos definidos por el proyecto. 

No formarán parte de la competición oficial debido a que pueden prolongarse durante días o semanas, lo que impediría mantener un calendario competitivo estable. 

- 2.7 Motor de reglas compartido 

Toda partida Ranked utiliza exactamente el mismo motor empleado por: 

PvP casual; 

Eventos; 

Torneos; 

PvE. 

No existen cartas exclusivas para Ranked. 

No existen reglas exclusivas para Ranked. 

No existen modificaciones competitivas sobre el reglamento oficial. 

Este principio garantiza la fidelidad absoluta al Pokémon TCG y simplifica el mantenimiento técnico del proyecto. 

# 2.8 Arquitectura independiente 

El sistema Ranked constituye un subsistema independiente dentro del proyecto. 

No modifica: 

La economía; 

El crafting; 

La colección; 

El perfil del jugador; 

El Museo; 

La progresión PvE. 

Esta separación reduce el acoplamiento entre sistemas y facilita la evolución del proyecto durante décadas. 

# 2.9 Escalabilidad 

La arquitectura del modo Ranked ha sido diseñada para crecer indefinidamente. 

Permitirá incorporar en el futuro: 

Nuevos formatos oficiales; 

Nuevos circuitos competitivos; 

Nuevos tipos de temporada; 

Nuevos eventos organizados. 

Todo ello sin afectar al historial previamente registrado. 

2.10 Principio de identidad competitiva 

El modo Ranked posee una identidad completamente distinta de la progresión PvE. 

Mientras que el PvE recompensa el avance del jugador mediante categorías y rangos, el modo Ranked evalúa únicamente el rendimiento competitivo obtenido frente a otros jugadores. 

La arquitectura del sistema competitivo se desarrollará en los capítulos siguientes mediante: 

Rating Competitivo; 

Clasificación oficial; 

Temporadas; 

Recompensas competitivas; 

Historial permanente. 

Esta separación constituye un principio canónico del proyecto y garantiza que ambos modos puedan evolucionar de forma independiente sin generar contradicciones. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura general del modo Ranked. 

Se establece que: 

Ranked es la modalidad oficial de competición del PvP. 

Comparte íntegramente el motor de reglas con el resto del juego. 

Utiliza exclusivamente partidas En Vivo. 

Puede albergar múltiples formatos competitivos independientes. 

No modifica la progresión del PvE ni utiliza su sistema de categorías y rangos. 

Su arquitectura está preparada para evolucionar durante décadas respetando plenamente el canon del proyecto. 

El siguiente capítulo desarrollará el Sistema de Rating Competitivo, responsable de medir objetivamente la habilidad de cada jugador y servir de base para la clasificación oficial del modo Ranked. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo III — Sistema de Rating Competitivo 

Objetivo del capítulo 

Este capítulo define la arquitectura del Rating Competitivo, el sistema encargado de medir objetivamente la habilidad de cada jugador dentro del modo Ranked. 

El Rating constituye el núcleo del sistema competitivo. 

No representa la progresión del jugador, sino una estimación dinámica de su nivel competitivo dentro de cada formato oficial. 

Todos los sistemas posteriores (clasificación, emparejamiento, temporadas y recompensas) se apoyan sobre este Rating. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con la Fase 8 (PvE) 

El Rating Competitivo pertenece exclusivamente al PvP Ranked. 

No modifica ni utiliza el sistema de categorías y rangos del PvE. 

Ambos sistemas evolucionan de forma completamente independiente. 

Compatibilidad con la Fase 9 (PvP) 

El Rating no modifica el funcionamiento de una partida. 

Únicamente registra el resultado una vez finalizado el combate y actualiza la clasificación competitiva. 

Compatibilidad con el Capítulo II 

El Rating constituye el elemento central sobre el que se construye toda la arquitectura del modo Ranked. 

Será utilizado posteriormente por: 

El sistema de clasificación; 

El emparejamiento; 

Las temporadas; 

Las estadísticas competitivas. 

3.1 Definición del Rating Competitivo 

El Rating Competitivo es un valor numérico interno que representa la habilidad estimada de un jugador dentro de un formato Ranked. 

Su objetivo es reflejar el nivel competitivo real del jugador en cada momento. 

No representa: 

Experiencia; 

Tiempo jugado; 

Progreso PvE; 

Antigüedad. 

Representa únicamente el rendimiento competitivo. 

3.2 Un Rating por formato 

Cada formato competitivo mantiene su propio Rating independiente. 

Por ejemplo: 

Estándar posee un Rating propio. 

Expandido posee otro distinto. 

Cada formato histórico dispone de su propio Rating. 

Los formatos especiales también mantienen Ratings independientes. 

El rendimiento en un formato nunca modifica el Rating de otro. 

# 3.3 Actualización del Rating 

El Rating se actualiza automáticamente al finalizar cada partida Ranked válida. 

Su variación dependerá del resultado obtenido y del nivel competitivo estimado del rival. 

De esta manera, el sistema ajusta progresivamente la valoración del jugador conforme acumula partidas oficiales. 

3.4 Clasificación inicial 

Cuando un jugador participa por primera vez en un formato Ranked, el sistema inicia un proceso de calibración. 

Durante este período, el Rating puede variar con mayor rapidez para aproximarse al nivel competitivo real del jugador. 

Una vez completada la calibración, el Rating pasa a comportarse de forma estable. 

Este procedimiento mejora la calidad de los emparejamientos desde las primeras partidas. 

3.5 Independencia respecto al tiempo de juego 

El Rating no recompensa la cantidad de partidas disputadas. 

Un mayor número de partidas no garantiza una mejor clasificación. 

La progresión competitiva depende exclusivamente del rendimiento demostrado frente a otros jugadores. 

Este principio garantiza que la clasificación represente habilidad y no dedicación horaria. 

3.6 Partidas que afectan al Rating 

Únicamente las partidas oficiales del modo Ranked modifican el Rating Competitivo. 

No tendrán efecto sobre él: 

Partidas PvE; 

Partidas casuales; 

Desafíos entre amigos; 

Partidas del modo Por Turnos; 

Partidas de prueba; 

Eventos no clasificados. 

Esto garantiza que el Rating represente únicamente el desempeño dentro del entorno competitivo oficial. 

3.7 Transparencia 

Al finalizar una partida Ranked, el jugador podrá consultar claramente: 

Su Rating actualizado; 

La variación producida tras la partida; 

Su posición actual dentro de la clasificación correspondiente. 

El funcionamiento general del sistema será transparente para todos los jugadores. 

No obstante, determinados parámetros internos destinados a proteger la integridad competitiva podrán permanecer reservados para evitar manipulaciones. 

# 3.8 Integridad competitiva 

El sistema incorporará mecanismos destinados a preservar la fiabilidad del Rating. 

Entre ellos: 

Detección de patrones anómalos; 

Prevención del intercambio deliberado de victorias; 

Identificación de comportamientos fraudulentos; 

Revisión administrativa cuando existan evidencias objetivas. 

Estas medidas afectan exclusivamente a la validez competitiva de las partidas y nunca modifican las reglas del juego. 

# 3.9 Evolución futura 

La implementación matemática concreta del Rating podrá evolucionar si en el futuro se demuestra la existencia de un modelo más preciso o más justo. 

Sin embargo, cualquier mejora deberá respetar los siguientes principios: 

Igualdad de condiciones; 

Transparencia; 

Estabilidad; 

Compatibilidad con el historial competitivo. 

La evolución del algoritmo nunca invalidará las temporadas ya finalizadas. 

3.10 Principio de objetividad 

El Rating Competitivo constituye la única medida oficial de habilidad utilizada por el sistema Ranked. 

Todas las decisiones relacionadas con: 

Clasificación; 

Emparejamiento; 

Progresión competitiva; 

Estadísticas oficiales; 

Se fundamentarán en este valor. 

De esta forma, el sistema garantiza que la competición se base exclusivamente en resultados obtenidos dentro del propio entorno competitivo. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura del Rating Competitivo. 

Se establece que: 

El Rating es el único indicador oficial de habilidad del modo Ranked; 

Cada formato competitivo mantiene un Rating independiente; 

Únicamente las partidas Ranked modifican dicho Rating; 

El sistema incorpora un período de calibración para nuevos participantes; 

La clasificación competitiva dependerá exclusivamente del Rating; 

El algoritmo podrá evolucionar técnicamente sin comprometer la preservación histórica del proyecto. 

El siguiente capítulo desarrollará la Clasificación Competitiva, definiendo cómo se ordenan los jugadores durante cada temporada a partir de su Rating Competitivo. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo IV — Clasificación Competitiva 

Objetivo del capítulo 

Este capítulo define la arquitectura de la Clasificación Competitiva (Leaderboard) del modo Ranked. 

Su función es transformar el Rating Competitivo de cada jugador en una clasificación pública, ordenada y permanente durante cada temporada. 

La clasificación constituye la referencia oficial del rendimiento competitivo del proyecto. 

No representa progreso, sino posición competitiva. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con el Capítulo III 

El Rating Competitivo continúa siendo la única medida oficial de habilidad. 

La Clasificación Competitiva se genera directamente a partir de dicho Rating. 

Compatibilidad con la Fase 8 (PvE) 

La Clasificación Competitiva pertenece exclusivamente al PvP Ranked. 

No guarda ninguna relación con las categorías y rangos utilizados por el PvE. 

Ambos sistemas permanecen completamente independientes. 

Compatibilidad con la Filosofía del Proyecto 

La clasificación debe reflejar exclusivamente el rendimiento competitivo del jugador. 

No puede depender del tiempo jugado, la antigüedad ni del progreso conseguido en otros modos de juego. 

4.1 Definición de la Clasificación Competitiva 

Cada formato Ranked dispondrá de una Clasificación Competitiva oficial. 

Esta clasificación ordenará a todos los jugadores participantes de mayor a menor Rating Competitivo. 

La posición obtenida representa el rendimiento del jugador dentro de la temporada activa. 

4.2 Clasificaciones independientes 

Cada formato competitivo mantiene una clasificación completamente independiente. 

Por ejemplo: 

Estándar tendrá su propia clasificación. 

Expandido tendrá otra clasificación distinta. 

Cada formato histórico tendrá su propia tabla. 

Los formatos especiales dispondrán igualmente de clasificaciones independientes. 

El rendimiento en un formato nunca afectará la clasificación de otro. 

4.3 Posición competitiva 

Cada jugador dispondrá de una posición oficial dentro de la clasificación correspondiente. 

Dicha posición se actualizará automáticamente después de cada partida Ranked válida. 

La posición constituye el principal indicador público del rendimiento competitivo durante la temporada. 

4.4 Actualización en tiempo real 

La Clasificación Competitiva se actualizará automáticamente tras la finalización de cada partida Ranked. 

Cada actualización recalculará: 

El Rating del jugador; 

Su posición dentro de la clasificación; 

La posición del rival cuando corresponda. 

De esta forma, la clasificación reflejará permanentemente el estado competitivo actual de la temporada. 

4.5 Consulta de la clasificación 

El jugador podrá consultar en cualquier momento la clasificación oficial del formato seleccionado. 

La interfaz mostrará, como mínimo: 

Posición actual; 

Rating Competitivo; 

Nombre del jugador; 

Variación reciente de posición, cuando corresponda. 

La información deberá presentarse de forma clara, rápida y uniforme en todas las plataformas compatibles con el proyecto. 

4.6 Empates 

Cuando dos o más jugadores posean exactamente el mismo Rating, el sistema aplicará criterios objetivos de desempate. 

Estos criterios serán definidos por la organización del proyecto y permanecerán documentados para garantizar la transparencia del sistema competitivo. 

Su aplicación será idéntica para todos los participantes. 

4.7 Historial de clasificación 

Al finalizar una temporada quedarán registrados permanentemente: 

Posición final; 

Rating final; 

Formato competitivo; 

Temporada correspondiente; 

Estadísticas oficiales asociadas. 

Estos datos pasarán a formar parte del historial competitivo permanente del jugador. 

Nunca podrán eliminarse ni modificarse. 

4.8 Integridad de la clasificación 

Únicamente las partidas oficiales Ranked podrán modificar la Clasificación Competitiva. 

No producirán cambios: 

Partidas PvE; 

Partidas casuales; 

Desafíos entre amigos; 

Partidas del modo Por Turnos; 

Combates de entrenamiento. 

Esto garantiza que la clasificación represente exclusivamente el rendimiento competitivo oficial. 

# 4.9 Escalabilidad 

La arquitectura de la Clasificación Competitiva ha sido diseñada para funcionar durante décadas. 

Permitirá incorporar: 

Nuevos formatos; 

Nuevos circuitos competitivos; 

Nuevas temporadas; 

Futuras modalidades oficiales. 

Todo ello sin alterar los registros históricos existentes. 

- 4.10 Principio de mérito competitivo 

La Clasificación Competitiva constituye el reconocimiento oficial del rendimiento deportivo dentro del proyecto. 

El prestigio competitivo no se representa mediante categorías o rangos. 

Se fundamenta en: 

El Rating Competitivo obtenido; 

La posición alcanzada en la clasificación oficial; 

El rendimiento demostrado durante la temporada; 

El historial competitivo acumulado a lo largo de los años. 

Este principio diferencia claramente la competición PvP del sistema de progresión del PvE y garantiza que ambos modos evolucionen de forma independiente sin generar conflictos de diseño. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura de la Clasificación Competitiva del proyecto. 

Se establece que: 

Cada formato Ranked dispone de una clasificación independiente; 

La clasificación se genera directamente a partir del Rating Competitivo; 

La posición del jugador se actualiza automáticamente tras cada partida oficial; 

Únicamente las partidas Ranked afectan a la clasificación; 

El historial conserva permanentemente la posición y el Rating final de cada temporada; 

El prestigio competitivo se basa en el rendimiento demostrado y no en un sistema de rangos, reservado exclusivamente para el PvE. 

El siguiente capítulo desarrollará el Sistema de Emparejamiento Competitivo, encargado de encontrar enfrentamientos equilibrados utilizando el Rating Competitivo y la estructura de clasificación definida en esta fase. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo V — Sistema de Emparejamiento Competitivo 

Objetivo del capítulo 

Este capítulo define la arquitectura del Sistema de Emparejamiento (Matchmaking) del modo Ranked. 

A diferencia de los videojuegos competitivos con miles de jugadores conectados simultáneamente, el Pokémon TCG Clone está diseñado para una comunidad privada y reducida. 

Por ello, el sistema de emparejamiento prioriza que los jugadores puedan disputar partidas siempre que exista un rival disponible, dejando que el Rating Competitivo sea el encargado de reflejar correctamente la diferencia de nivel entre ambos participantes. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con el Capítulo III 

El Rating Competitivo continúa siendo la medida oficial de habilidad. 

El emparejamiento no intenta igualar Ratings. 

El Rating corrige las diferencias competitivas después de cada partida. 

Compatibilidad con el Capítulo IV 

La Clasificación Competitiva continúa generándose a partir del Rating. 

El sistema de emparejamiento únicamente determina quién juega contra quién. 

Compatibilidad con la Filosofía del Proyecto 

El proyecto está diseñado para una comunidad privada. 

Intentar copiar un sistema de matchmaking de videojuegos masivos produciría tiempos de espera innecesarios y dificultaría encontrar partidas. 

Esta arquitectura garantiza que la competición permanezca siempre activa. 

5.1 Finalidad del sistema de emparejamiento 

El sistema de emparejamiento tiene cuatro objetivos principales: 

Permitir encontrar partidas rápidamente; 

Mantener la competición siempre activa; 

Simplificar la arquitectura del sistema competitivo; 

Dejar que el Rating Competitivo determine el impacto deportivo de cada resultado. 

El equilibrio competitivo no se consigue antes de la partida, sino después de ella mediante el Rating. 

5.2 Colas por formato 

Cada formato competitivo dispone de su propia cola de búsqueda. 

Por ejemplo: 

Estándar; 

Expandido; 

Formatos históricos; 

Formatos especiales. 

Un jugador únicamente podrá enfrentarse contra otro que se encuentre buscando partida dentro del mismo formato. 

# 5.3 Disponibilidad como criterio principal 

Cuando un jugador inicia la búsqueda, el sistema buscará simplemente otro jugador disponible dentro del mismo formato. 

No intentará encontrar un rival con un Rating similar. 

El primer rival disponible será seleccionado automáticamente. 

Este comportamiento garantiza que incluso una comunidad pequeña pueda disputar partidas con facilidad. 

# 5.4 Función del Rating 

La diferencia de habilidad entre jugadores no se corrige mediante el emparejamiento. 

Se corrige mediante el Rating Competitivo. 

Por ejemplo: 

Si un jugador con Rating elevado derrota a un jugador con Rating bajo, obtendrá una ganancia reducida; 

Si pierde frente a ese mismo jugador, sufrirá una pérdida considerable; 

Si un jugador de Rating bajo consigue derrotar a uno de Rating alto, obtendrá una ganancia importante. 

De esta manera, el sistema continúa reflejando correctamente la habilidad de cada jugador sin limitar los enfrentamientos posibles. 

5.5 Exclusividad del modo En Vivo 

El modo Ranked utilizará exclusivamente partidas En Vivo. 

Las partidas Por Turnos permanecerán reservadas para: 

PvP casual; 

Desafíos entre amigos; 

Otros modos compatibles con la Fase 9. 

Esta decisión garantiza el correcto desarrollo de las temporadas competitivas. 

# 5.6 Cancelación de búsqueda 

Mientras no exista un rival asignado, el jugador podrá cancelar libremente la búsqueda. 

Una vez confirmado el enfrentamiento, la partida pasará a formar parte de la competición oficial y se aplicarán las normas correspondientes sobre abandonos y desconexiones. 

# 5.7 Integridad competitiva 

Aunque el sistema permite enfrentar libremente a cualquier jugador disponible, continuará incorporando mecanismos destinados a proteger la competición. 

Entre ellos: 

Detección de intercambio deliberado de victorias; 

Enfrentamientos coordinados para alterar el Rating; 

Patrones estadísticamente anómalos; 

Cualquier comportamiento contrario al reglamento competitivo. 

Estas medidas afectan únicamente a la validez competitiva de las partidas y nunca modifican las reglas del juego. 

5.8 Independencia del emparejamiento 

El sistema de emparejamiento no depende de: 

La posición en la clasificación; 

El progreso PvE; 

La antigüedad del jugador; 

La cantidad de partidas disputadas. 

Su única función consiste en encontrar un rival disponible dentro del mismo formato competitivo. 

5.9 Escalabilidad 

Aunque actualmente el proyecto está pensado para una comunidad reducida, la arquitectura permitirá evolucionar en el futuro. 

Si la cantidad de jugadores creciera significativamente, podrán incorporarse criterios adicionales de emparejamiento sin modificar el historial competitivo ni el funcionamiento del Rating. 

La arquitectura permanece preparada para evolucionar sin romper la compatibilidad con las temporadas anteriores. 

5.10 Principio de disponibilidad 

El sistema competitivo prioriza que los jugadores puedan competir siempre que exista un rival disponible. 

La justicia competitiva no depende de restringir los enfrentamientos. 

Depende de que el Rating Competitivo evalúe correctamente cada resultado. 

Este principio diferencia al Pokémon TCG Clone de los sistemas competitivos diseñados para millones de jugadores y adapta el modo Ranked a la realidad de una comunidad privada, garantizando una experiencia fluida, sostenible y coherente con la filosofía del proyecto. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura del Sistema de Emparejamiento Competitivo. 

Se establece que: 

El sistema organiza colas independientes por formato; 

El criterio principal de emparejamiento es la disponibilidad de jugadores, no la similitud de Rating; 

El Rating Competitivo es el encargado de equilibrar el sistema a largo plazo; 

El modo Ranked utiliza exclusivamente partidas En Vivo; 

El sistema incorpora mecanismos para proteger la integridad de la competición; 

La arquitectura está optimizada para una comunidad privada y preparada para evolucionar si el proyecto crece en el futuro. 

El siguiente capítulo desarrollará el Sistema de Temporadas Competitivas, definiendo el ciclo de vida de cada temporada, su duración, el reinicio de la clasificación y la preservación histórica del competitivo. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo VI — Sistema de Temporadas Competitivas 

Objetivo del capítulo 

Este capítulo define la arquitectura de las temporadas competitivas del modo Ranked. 

Las temporadas organizan la competición en períodos claramente delimitados, permitiendo reiniciar la clasificación de forma controlada sin perder nunca el historial competitivo acumulado. 

Su finalidad es mantener la competición activa, ofrecer nuevos objetivos a los jugadores y preservar permanentemente la historia competitiva del proyecto. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con la Filosofía del Proyecto 

El proyecto establece que ningún contenido debe perderse. 

Por ello, las temporadas reinician únicamente la competición activa. 

Toda la información histórica permanece conservada para siempre. 

Compatibilidad con el Capítulo III 

Cada temporada utiliza el Rating Competitivo como base para medir la habilidad de los jugadores. 

El inicio de una nueva temporada no elimina el historial de Ratings anteriores. 

Compatibilidad con el Capítulo IV 

Cada temporada genera una nueva Clasificación Competitiva independiente. 

Las clasificaciones de temporadas anteriores permanecen archivadas permanentemente. 

6.1 Definición de temporada 

Una temporada competitiva es un período oficial durante el cual los jugadores participan en el modo Ranked para obtener la mejor posición posible dentro de la clasificación correspondiente. 

Cada temporada constituye una competición independiente. 

Al finalizar, comienza una nueva temporada con su propia clasificación. 

# 6.2 Duración 

Las temporadas tendrán una duración fija establecida por la organización del proyecto. 

La duración podrá modificarse en futuras temporadas si las necesidades de la comunidad así lo requieren. 

No obstante, cualquier cambio únicamente afectará a temporadas futuras y nunca alterará la información histórica de temporadas anteriores. 

6.3 Inicio de una temporada 

Al comenzar una nueva temporada: 

Se crea una nueva clasificación oficial; 

Todos los jugadores comienzan la nueva competición bajo las reglas vigentes para esa temporada; 

Se conserva íntegramente el historial de temporadas anteriores. 

El inicio de una nueva temporada nunca elimina información previamente registrada. 

6.4 Finalización de una temporada 

Cuando una temporada concluye: 

La clasificación queda congelada; 

Las posiciones finales se vuelven definitivas; 

Las estadísticas oficiales quedan archivadas; 

Se asignan las recompensas correspondientes. 

A partir de ese momento, la temporada pasa a formar parte del historial permanente del proyecto. 

6.5 Reinicio competitivo 

El inicio de una nueva temporada implica un reinicio de la competición activa. 

Este reinicio afecta exclusivamente al desarrollo de la nueva temporada. 

No elimina: 

El historial competitivo; 

Las estadísticas históricas; 

Las recompensas obtenidas; 

Las temporadas anteriores. 

El objetivo es ofrecer una nueva oportunidad competitiva sin sacrificar la preservación histórica. 

# 6.6 Independencia entre temporadas 

Cada temporada constituye una entidad independiente. 

Cada una conserva permanentemente: 

Su clasificación; 

Sus participantes; 

Sus resultados; 

Sus estadísticas; 

Sus recompensas; 

Las reglas y formatos vigentes durante su desarrollo. 

Nunca se modificará una temporada ya finalizada para adaptarla a cambios futuros del juego. 

6.7 Formatos por temporada 

Cada temporada podrá incluir uno o varios formatos competitivos oficiales. 

Por ejemplo: 

Estándar; 

Expandido; 

Formatos históricos; 

Formatos especiales. 

Cada formato dispondrá de su propia clasificación dentro de la temporada correspondiente. 

6.8 Identidad de la temporada 

Cada temporada dispondrá de una identidad propia. 

Como mínimo incluirá: 

Nombre oficial; 

Número de temporada; 

Fecha de inicio; 

Fecha de finalización; 

Formatos oficiales; 

Reglamento aplicable. 

Esta información permanecerá archivada permanentemente junto al resto del historial competitivo. 

# 6.9 Escalabilidad 

La arquitectura de temporadas ha sido diseñada para mantenerse vigente durante décadas. 

Permitirá registrar un número ilimitado de temporadas sin afectar al funcionamiento del sistema competitivo. 

Cada nueva temporada ampliará el historial del proyecto sin sustituir ni modificar las anteriores. 

6.10 Principio de preservación histórica 

Cada temporada representa un capítulo permanente de la historia competitiva del Pokémon TCG Clone. 

Una vez finalizada: 

Sus resultados nunca serán eliminados; 

Sus clasificaciones nunca serán modificadas; 

Sus estadísticas permanecerán disponibles para consulta; 

Su contexto histórico quedará preservado para futuras generaciones de jugadores. 

Este principio garantiza que la evolución del competitivo respete la filosofía de preservación absoluta sobre la que se construye todo el proyecto. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura del Sistema de Temporadas Competitivas. 

Se establece que: 

Las temporadas organizan la competición en períodos independientes; 

Cada temporada genera su propia clasificación oficial; 

El inicio de una nueva temporada reinicia únicamente la competición activa; 

Toda la información histórica permanece conservada permanentemente; 

Cada temporada posee su propia identidad y contexto competitivo; 

La arquitectura permite registrar un número ilimitado de temporadas sin comprometer la preservación histórica del proyecto. 

El siguiente capítulo desarrollará el Sistema de Recompensas Competitivas, definiendo cómo se reconocen los logros obtenidos durante cada temporada sin otorgar ventajas jugables y respetando la filosofía del proyecto. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo VII — Sistema de Recompensas Competitivas 

Objetivo del capítulo 

Este capítulo define el sistema de recompensas del modo Ranked. 

Su propósito es reconocer el desempeño competitivo de los jugadores al finalizar cada temporada, respetando los principios fundamentales del proyecto: 

Igualdad de condiciones; 

Ausencia de monetización; 

Preservación permanente; 

Progresión basada en el mérito. 

Las recompensas constituyen un reconocimiento al rendimiento obtenido durante la temporada y nunca una ventaja competitiva para temporadas futuras. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con la Fase 2 (Economía) 

Las recompensas pueden incluir cualquiera de las monedas oficiales del proyecto. 

No introducen nuevas divisas ni sistemas económicos paralelos. 

Compatibilidad con la Fase 4 y Fase 5 

Las recompensas podrán contribuir al crecimiento de la colección o facilitar la obtención de recursos existentes. 

Nunca otorgarán cartas exclusivas imposibles de conseguir por otros medios. 

Compatibilidad con la Filosofía del Proyecto 

La competición debe reconocer el mérito sin generar ventajas permanentes entre jugadores. 

Todas las recompensas respetan este principio. 

7.1 Finalidad de las recompensas 

Las recompensas competitivas tienen cuatro objetivos principales: 

Reconocer el esfuerzo realizado durante la temporada; 

Incentivar la participación continua; 

Preservar el prestigio competitivo; 

Enriquecer la colección y el perfil del jugador. 

Nunca tienen como objetivo crear ventajas competitivas. 

# 7.2 Recompensas de participación 

Todo jugador que participe en una temporada oficial podrá recibir una recompensa básica por su participación. 

Esta recompensa reconoce el compromiso con la competición independientemente de la posición final obtenida. 

Su cuantía será inferior a la obtenida por los jugadores con mejor rendimiento. 

7.3 Recompensas por posición final 

Al finalizar la temporada, las recompensas aumentarán progresivamente según la posición obtenida en la clasificación. 

Cuanto mejor sea la posición final, mayor será el reconocimiento recibido. 

La organización del proyecto definirá la distribución concreta para cada temporada. 

7.4 Tipos de recompensas 

Las recompensas podrán incluir, entre otros elementos: 

Monedas; 

Cristales; 

Créditos o recursos de fabricación; 

Sobres; 

Productos sellados disponibles dentro de la economía del juego; 

Elementos cosméticos; 

Títulos; 

Insignias; 

Reconocimientos para el perfil del jugador. 

Todas ellas deberán respetar las fases económicas previamente establecidas. 

7.5 Recompensas cosméticas 

Las recompensas cosméticas constituyen la principal forma de reconocimiento permanente del modo Ranked. 

Podrán incluir: 

Títulos exclusivos de temporada; 

Insignias conmemorativas; 

Marcos de perfil; 

Fondos; 

Elementos decorativos; 

Otros cosméticos compatibles con el proyecto. 

Estos elementos reflejan logros históricos sin alterar el equilibrio competitivo. 

7.6 Ausencia de ventajas competitivas 

Las recompensas competitivas nunca otorgarán: 

Mayor poder; 

Cartas exclusivas de juego; 

Ventajas durante la siguiente temporada; 

Bonificaciones sobre el Rating; 

Beneficios que alteren la igualdad de condiciones. 

Todos los jugadores comenzarán cada nueva temporada bajo las mismas reglas competitivas. 

7.7 Registro histórico 

Las recompensas obtenidas pasarán automáticamente al historial permanente del jugador. 

El perfil conservará para siempre: 

Títulos; 

Insignias; 

Reconocimientos; 

Recompensas históricas. 

Estos elementos formarán parte del legado competitivo del jugador. 

# 7.8 Entrega de recompensas 

Las recompensas serán entregadas automáticamente al finalizar cada temporada una vez oficializada la clasificación definitiva. 

La entrega quedará registrada dentro del historial del jugador. 

# 7.9 Escalabilidad 

El sistema permitirá incorporar nuevos tipos de recompensas en temporadas futuras. 

Estas incorporaciones no modificarán las recompensas obtenidas en temporadas anteriores. 

Cada temporada conservará permanentemente su propio conjunto de recompensas. 

# 7.10 Principio del mérito permanente 

El verdadero valor de las recompensas competitivas reside en representar un logro histórico. 

Cada recompensa constituye una evidencia permanente del rendimiento alcanzado durante una temporada concreta. 

Con el paso de los años, estos reconocimientos construirán la trayectoria competitiva del jugador dentro del Pokémon TCG Clone. 

El prestigio no dependerá de ventajas obtenidas, sino de la historia competitiva que cada jugador haya construido. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura del Sistema de Recompensas Competitivas. 

Se establece que: 

Toda temporada reconoce el desempeño de sus participantes; 

Las recompensas aumentan según la posición final obtenida; 

El sistema prioriza recompensas cosméticas y recursos ya existentes en la economía del proyecto; 

Nunca se conceden ventajas competitivas para temporadas futuras; 

Todas las recompensas pasan a formar parte del historial permanente del jugador; 

El sistema puede evolucionar durante décadas sin comprometer la igualdad competitiva ni la preservación histórica. 

El siguiente capítulo desarrollará la Integración con Eventos y Torneos, definiendo la relación entre el modo Ranked y las competiciones oficiales organizadas dentro del proyecto. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo VIII — Integración con Eventos y Torneos 

Objetivo del capítulo 

Este capítulo define la relación entre el sistema Ranked y las competiciones organizadas del proyecto. 

Su finalidad es establecer una arquitectura donde el modo Ranked, los eventos y los torneos convivan de forma coherente sin depender unos de otros. 

Aunque todos forman parte del ecosistema competitivo, cada uno cumple una función distinta. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con la Fase 9 (PvP) 

La Fase 9 estableció que: 

Los jugadores únicamente pueden retarse libremente si son amigos; 

En eventos y torneos los participantes pueden enfrentarse entre sí sin necesidad de ser amigos. 

Este capítulo mantiene exactamente ese comportamiento. 

Compatibilidad con el Sistema Ranked 

El modo Ranked continúa funcionando de manera independiente. 

Los eventos y torneos no sustituyen la clasificación competitiva ni forman parte de la cola Ranked. 

Compatibilidad con la Filosofía del Proyecto 

La comunidad del proyecto es privada. 

Los eventos y torneos están diseñados para fortalecer esa comunidad, ofrecer objetivos competitivos adicionales y preservar su historia. 

8.1 Competiciones oficiales 

El proyecto reconoce tres formas oficiales de competición entre jugadores: 

Ranked; 

Eventos; 

Torneos. 

Las tres utilizan exactamente el mismo motor de reglas. 

La diferencia reside únicamente en su organización y objetivos. 

8.2 Independencia entre sistemas 

Cada sistema competitivo funciona de manera independiente. 

Ranked 

Clasificación continua; 

Temporadas; 

Rating Competitivo. 

Eventos 

Actividades organizadas con reglas específicas; 

Duración limitada; 

Objetivos concretos. 

Torneos 

Competición estructurada mediante rondas; 

Eliminación, suizo u otros formatos; 

Campeón oficial. 

Participar en uno de estos sistemas no impide participar simultáneamente en los demás. 

8.3 Acceso a eventos y torneos 

Los eventos y torneos podrán establecer sus propios requisitos de participación. 

Entre ellos, por ejemplo: 

Formato permitido; 

Número máximo de participantes; 

Inscripción previa; 

Fecha de celebración. 

Estos requisitos serán definidos por el organizador del evento. 

8.4 Relación con el Rating Competitivo 

Por defecto, los eventos y torneos no modificarán el Rating Competitivo. 

El Rating pertenece exclusivamente al modo Ranked. 

No obstante, la arquitectura permite que en el futuro puedan organizarse eventos especiales donde la organización decida utilizar el sistema de Rating, siempre respetando las reglas definidas para dicho evento. 

Esta flexibilidad permite ampliar el sistema sin modificar el canon existente. 

8.5 Clasificaciones independientes 

Cada evento y cada torneo dispondrá de su propia clasificación. 

Estas clasificaciones son completamente independientes de: 

La clasificación Ranked; 

Las temporadas; 

El Rating Competitivo. 

Finalizado el evento, su clasificación quedará archivada como parte de su historial. 

8.6 Recompensas 

Los eventos y torneos podrán otorgar recompensas propias. 

Estas recompensas seguirán exactamente los principios establecidos en el Capítulo VII: 

No conceder ventajas competitivas permanentes; 

Respetar la economía del proyecto; 

Formar parte del historial permanente del jugador. 

8.7 Historial de competiciones 

Todo evento y torneo oficial quedará registrado permanentemente. 

Como mínimo se conservarán: 

Nombre del evento; 

Formato utilizado; 

Participantes; 

Clasificación final; 

Campeón; 

Fecha de celebración; 

Recompensas entregadas. 

Estos registros pasarán a formar parte de la historia del proyecto. 

8.8 Libertad de organización 

La arquitectura permitirá crear nuevos tipos de eventos sin modificar el sistema competitivo principal. 

Por ejemplo: 

Copas temáticas; 

Ligas internas; 

Torneos históricos; 

Eventos con reglas especiales; 

Competiciones con restricciones determinadas por la organización. 

Esta flexibilidad favorece la evolución permanente del proyecto. 

# 8.9 Escalabilidad 

El sistema ha sido diseñado para admitir un número ilimitado de: 

Eventos; 

Torneos; 

Organizadores; 

Formatos oficiales. 

Cada nueva competición ampliará el historial del proyecto sin afectar a las ya existentes. 

# 8.10 Principio de preservación competitiva 

Todo evento y torneo oficial constituye un documento histórico del Pokémon TCG Clone. 

Una vez finalizado: 

Sus resultados permanecerán archivados; 

Sus campeones conservarán dicho reconocimiento permanentemente; 

Sus clasificaciones nunca serán eliminadas; 

Su contexto histórico quedará preservado para futuras generaciones de jugadores. 

De esta manera, el sistema competitivo no solo mide el presente, sino que también conserva la historia completa de la comunidad privada del proyecto. 

Resultado del capítulo 

Con este capítulo queda definida la integración entre el modo Ranked, los eventos y los torneos. 

Se establece que: 

El proyecto reconoce tres sistemas oficiales de competición: Ranked, Eventos y Torneos; 

Cada uno posee objetivos y clasificaciones independientes; 

El Rating Competitivo pertenece exclusivamente al modo Ranked, salvo que un evento especial establezca explícitamente lo contrario; 

Los eventos y torneos generan su propio historial permanente; 

Toda competición oficial pasa a formar parte del legado histórico del Pokémon TCG Clone. 

El siguiente capítulo desarrollará el Historial y Preservación Competitiva, donde se definirá cómo se conserva para siempre la trayectoria competitiva de cada jugador y la historia completa de todas las temporadas, eventos y torneos del proyecto. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo IX — Historial y Preservación Competitiva 

Objetivo del capítulo 

Este capítulo define cómo el proyecto conservará permanentemente toda la historia del sistema competitivo. 

La preservación constituye uno de los pilares fundamentales del Pokémon TCG Clone. 

Por ello, ningún resultado competitivo será eliminado con el paso del tiempo. 

Cada temporada, torneo y evento pasará a formar parte del legado permanente de la comunidad. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con el canon existente. 

Compatibilidad con la Filosofía del Proyecto 

La preservación absoluta constituye uno de los principios fundamentales del proyecto. 

Este capítulo aplica dicho principio al sistema competitivo. 

Compatibilidad con la Fase 7 (Perfil, Museo y Legado) 

El historial competitivo formará parte del legado permanente del jugador. 

Los registros competitivos enriquecerán su perfil sin modificar la arquitectura definida en dicha fase. 

Compatibilidad con la Fase 10 

Todos los elementos definidos anteriormente (Rating, temporadas, clasificaciones, eventos y torneos) generan información histórica. 

Este capítulo establece cómo será conservada. 

9.1 Historial permanente 

Toda competición oficial organizada dentro del proyecto generará un registro permanente. 

Una vez finalizada una competición, su información nunca será eliminada ni sobrescrita. 

La historia competitiva crecerá continuamente con el paso de los años. 

9.2 Historial del jugador 

Cada jugador dispondrá de un historial competitivo permanente. 

Como mínimo conservará: 

Temporadas disputadas; 

Posición final en cada temporada; 

Rating final de cada temporada; 

Eventos jugados; 

Torneos disputados; 

Campeonatos obtenidos; 

Recompensas competitivas recibidas. 

Este historial acompañará al jugador durante toda la vida del proyecto. 

9.3 Historial de temporadas 

Cada temporada conservará permanentemente: 

Nombre oficial; 

Número de temporada; 

Fecha de inicio; 

Fecha de finalización; 

Formatos oficiales; 

Clasificación final; 

Participantes; 

Reglamento utilizado; 

Recompensas entregadas. 

Cada temporada representa un capítulo independiente de la historia competitiva. 

9.4 Historial de eventos 

Todos los eventos oficiales conservarán: 

Nombre; 

Organizador; 

Participantes; 

Formato; 

Clasificación final; 

Fecha; 

Recompensas. 

Nunca serán eliminados. 

# 9.5 Historial de torneos 

Cada torneo conservará permanentemente: 

Cuadro competitivo o sistema utilizado; 

Participantes; 

Resultados; 

Campeón; 

Subcampeón; 

Clasificación final; 

Fecha de celebración. 

Esta información permitirá consultar la historia completa del torneo incluso décadas después. 

9.6 Consulta del historial 

Los jugadores podrán consultar su historial competitivo desde su perfil. 

Asimismo, cuando la organización lo considere oportuno, también podrán consultarse los registros históricos de temporadas, eventos y torneos. 

El sistema facilitará la exploración de la historia competitiva del proyecto. 

# 9.7 Compatibilidad futura 

Si en el futuro se incorporan nuevos formatos competitivos, nuevas modalidades o nuevos tipos de competición, todos ellos seguirán el mismo sistema de preservación. 

La incorporación de nuevo contenido nunca alterará los registros históricos existentes. 

# 9.8 Integridad histórica 

Una vez oficializados los resultados de una competición, estos pasarán a ser definitivos. 

No podrán modificarse salvo en situaciones excepcionales donde la organización determine la existencia de un error administrativo grave. 

En caso de producirse una corrección, el sistema conservará constancia de dicha modificación para preservar la transparencia histórica. 

# 9.9 Escalabilidad 

La arquitectura del historial competitivo está diseñada para conservar un número ilimitado de: 

Temporadas; 

Jugadores; 

Eventos; 

Torneos; 

Formatos; 

Estadísticas. 

El crecimiento continuo del proyecto nunca requerirá eliminar información histórica. 

9.10 Principio del legado competitivo 

Cada partida Ranked contribuye al desarrollo competitivo de una temporada. 

Cada temporada contribuye a la historia del jugador. 

Y cada jugador contribuye a la historia del Pokémon TCG Clone. 

El objetivo del sistema no consiste únicamente en determinar quién gana una temporada, sino en construir una memoria competitiva permanente que pueda consultarse incluso décadas después. 

La historia de la comunidad constituye un patrimonio del proyecto y será preservada con el mismo nivel de importancia que la colección de cartas o la evolución del propio juego. 

Resultado del capítulo 

Con este capítulo queda definida la arquitectura del Historial y Preservación Competitiva. 

Se establece que: 

Toda competición oficial genera un registro permanente; 

Cada jugador dispone de un historial competitivo para toda la vida del proyecto; 

Temporadas, eventos y torneos conservan toda su información histórica; 

Los resultados oficializados únicamente podrán corregirse ante errores administrativos graves, dejando constancia de la modificación; 

La arquitectura está preparada para conservar décadas de historia competitiva sin eliminar ni sustituir registros anteriores. 

El siguiente y último capítulo desarrollará la Arquitectura Técnica y Escalabilidad del Sistema Competitivo, consolidando las bases que permitirán que el modo Ranked evolucione indefinidamente sin romper la compatibilidad con el canon del proyecto. 

FASE 10 — RANKED, TEMPORADAS Y COMPETICIÓN 

Capítulo X — Arquitectura Técnica y Escalabilidad 

Objetivo del capítulo 

Este capítulo consolida la arquitectura del sistema competitivo del Pokémon TCG Clone. 

Su propósito es establecer los principios técnicos que permitirán que el modo Ranked evolucione durante décadas sin perder compatibilidad con temporadas anteriores, sin romper el historial competitivo y sin requerir rediseños de la arquitectura principal. 

Este capítulo no define mecánicas nuevas, sino las reglas de evolución permanente del sistema competitivo. 

Auditoría de coherencia previa 

Antes de redactar este capítulo se verificó su compatibilidad con todas las fases canónicas. 

Compatibilidad con la Filosofía del Proyecto 

El proyecto tiene como objetivo conservar toda la historia del Pokémon TCG. 

La arquitectura del sistema competitivo debe responder a ese mismo principio. 

Toda evolución futura debe ampliar el sistema, nunca sustituirlo. 

Compatibilidad con la Fase 0 

Este capítulo aplica directamente los principios de: 

Preservación absoluta. 

Compatibilidad histórica. 

Compatibilidad retroactiva. 

Evolución perpetua. 

Preservación técnica. 

Compatibilidad con toda la Fase 10 

Los sistemas definidos en los capítulos anteriores (Rating, Clasificación, Emparejamiento, Temporadas, Recompensas, Eventos e Historial) deben poder evolucionar sin afectar la información ya registrada. 

# 10.1 Arquitectura modular 

El sistema competitivo estará dividido en módulos independientes. 

Entre ellos: 

Rating Competitivo. 

Clasificación Competitiva. 

Emparejamiento. 

Temporadas. 

Recompensas. 

Eventos. 

Torneos. 

Historial Competitivo. 

Cada módulo podrá evolucionar sin requerir modificaciones en los demás, siempre que respete las interfaces y contratos establecidos por el proyecto. 

10.2 Compatibilidad retroactiva 

Toda mejora realizada sobre el sistema competitivo deberá mantener la compatibilidad con: 

Temporadas anteriores; 

Eventos históricos; 

Torneos registrados; 

Perfiles de jugadores; 

Historial competitivo. 

Ninguna actualización podrá invalidar información ya preservada. 

10.3 Evolución del Rating 

El algoritmo utilizado para calcular el Rating podrá perfeccionarse en el futuro. 

Sin embargo: 

Las temporadas ya finalizadas conservarán los Ratings registrados en su momento; 

Las nuevas versiones del algoritmo únicamente afectarán a temporadas futuras; 

Nunca se recalcularán clasificaciones históricas. 

Esto garantiza la estabilidad del legado competitivo. 

10.4 Evolución de formatos 

El sistema permitirá incorporar nuevos formatos competitivos sin modificar los existentes. 

Cada formato conservará de manera independiente: 

Su historial; 

Sus clasificaciones; 

Sus temporadas; 

Sus estadísticas. 

La desaparición de un formato activo nunca supondrá la pérdida de su información histórica. 

10.5 Evolución de temporadas 

Las temporadas futuras podrán introducir: 

Nuevos calendarios; 

Nuevos formatos; 

Nuevos sistemas de recompensas; 

Nuevas normas organizativas. 

Estos cambios nunca modificarán las temporadas ya disputadas. 

Cada temporada permanecerá como un registro fiel del contexto competitivo existente en el momento de su celebración. 

10.6 Escalabilidad de la comunidad 

Aunque el proyecto está diseñado para una comunidad privada, la arquitectura permitirá soportar un crecimiento futuro sin necesidad de rediseñar el sistema competitivo. 

Podrán incorporarse: 

Nuevos jugadores; 

Nuevos organizadores; 

Nuevos formatos; 

Nuevas competiciones. 

Todo ello manteniendo la compatibilidad con el historial existente. 

10.7 Preservación de datos 

La información competitiva constituye parte del patrimonio del proyecto. 

Por ello deberán preservarse permanentemente: 

Temporadas; 

Clasificaciones; 

Ratings finales; 

Resultados oficiales; 

Recompensas; 

Campeones; 

Estadísticas históricas. 

La pérdida de cualquiera de estos registros se considerará una pérdida de patrimonio del proyecto. 

10.8 Independencia respecto al contenido del juego 

La evolución del Pokémon TCG no afectará directamente al sistema competitivo. 

La incorporación de: 

Nuevas expansiones; 

Nuevas mecánicas; 

Nuevos Pokémon; 

Nuevos formatos oficiales; 

No requerirá rediseñar la arquitectura del modo Ranked. 

El sistema competitivo está preparado para adaptarse al crecimiento natural del juego. 

10.9 Mantenimiento a largo plazo 

Toda modificación futura del sistema competitivo deberá cumplir obligatoriamente los siguientes principios: 

Preservar el historial competitivo; 

Mantener la igualdad de condiciones; 

Respetar el canon del proyecto; 

Garantizar la compatibilidad con las fases anteriores; 

Evitar cambios que obliguen a reiniciar la historia competitiva. 

Este principio asegura la estabilidad del proyecto durante toda su vida útil. 

10.10 Principio de inmortalidad del competitivo 

El sistema competitivo del Pokémon TCG Clone no ha sido diseñado para una única generación de jugadores. 

Ha sido diseñado para acompañar al proyecto durante toda su existencia. 

Cada temporada añadirá un nuevo capítulo a la historia de la comunidad. 

Cada torneo ampliará el legado competitivo. 

Cada jugador contribuirá con sus resultados a construir una memoria permanente. 

La arquitectura presentada en esta fase garantiza que el sistema competitivo pueda evolucionar indefinidamente sin perder su identidad, sin destruir su historia y sin romper la compatibilidad con el resto del proyecto. 

El modo Ranked deja de ser únicamente un sistema para disputar partidas y se convierte en un archivo vivo de la historia competitiva del Pokémon TCG Clone. 

Resultado del capítulo 

Con este capítulo concluye la FASE 10 — Ranked, Temporadas y Competición. 

Queda establecido que: 

El sistema competitivo posee una arquitectura completamente modular; 

Todas las evoluciones futuras respetarán la compatibilidad retroactiva; 

Las temporadas, clasificaciones, eventos y torneos permanecerán preservados permanentemente; 

El algoritmo de Rating podrá evolucionar sin alterar el historial ya registrado; 

La arquitectura soporta el crecimiento continuo del proyecto sin necesidad de rediseños estructurales; 

El modo Ranked se integra plenamente con la filosofía del Pokémon TCG Clone, priorizando la preservación, la equidad, la escalabilidad y la inmortalidad del proyecto. 

Con esta fase queda definido el ecosistema competitivo completo, proporcionando una base sólida para las siguientes fases del diseño canónico. 

