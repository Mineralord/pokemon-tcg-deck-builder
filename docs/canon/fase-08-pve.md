# FASE 8 — PvE 

# Capítulo I. Objetivos y Filosofía del PvE 

# I.1 Propósito del PvE 

El modo Jugador contra Entorno (PvE) constituye uno de los pilares permanentes de Pokémon TCG Clone. Su finalidad es ofrecer una experiencia completa para quienes desean aprender, experimentar, coleccionar y disfrutar del Pokémon Trading Card Game sin depender de la existencia de otros jugadores conectados. 

El PvE no es un complemento del PvP ni un simple tutorial. Es un ecosistema completo de juego diseñado para mantenerse vigente durante toda la vida del proyecto y crecer continuamente con cada nueva expansión, mecánica o actualización. 

Desde el inicio del proyecto, cualquier jugador debe poder disfrutar cientos o miles de horas de contenido PvE, independientemente del tamaño de la comunidad activa. 

# I.2 Misión del PvE 

La misión del PvE es preservar toda la historia del Pokémon Trading Card Game mediante experiencias jugables permanentes, ofreciendo desafíos, aprendizaje y entretenimiento que evolucionen durante décadas sin perder compatibilidad con el contenido existente. 

Cada carta publicada oficialmente debe tener siempre un lugar donde pueda utilizarse y disfrutarse. 

# I.3 Principios Fundamentales 

Todo el diseño del PvE deberá respetar los principios establecidos en la Filosofía General del Proyecto, aplicados específicamente al juego contra la IA. 

## Preservación permanente 

Ningún modo PvE será eliminado por la llegada de nuevas expansiones. 

Las aventuras, desafíos, entrenamientos y eventos permanentes seguirán siendo accesibles para las futuras generaciones de jugadores. 

## Fidelidad al Pokémon TCG Oficial 

Las reglas de los combates utilizarán el reglamento oficial del Pokémon Trading Card Game. 

El PvE no modifica las reglas fundamentales del juego. 

La dificultad provendrá del diseño de rivales, estrategias y objetivos, nunca de alterar arbitrariamente las reglas oficiales. 

## Libertad del jugador 

El jugador decidirá cómo desea jugar. Podrá elegir: 

- formato de juego; 

- modo de combate; 

- barajas; 

- dificultad; 

- actividad PvE. 

El sistema nunca obligará a jugar un único tipo de contenido para progresar. 

## Aprendizaje continuo 

El PvE debe acompañar tanto a jugadores nuevos como expertos. Debe existir contenido para: 

- aprender las reglas; 

- practicar estrategias; 

- dominar mecánicas avanzadas; 

- experimentar con nuevas cartas; 

- perfeccionar habilidades competitivas. 

## Diversión antes que optimización 

El PvE no buscará únicamente ofrecer el desafío más difícil. También deberá incentivar: 

- experimentación; 

- creatividad; 

- coleccionismo; 

- nostalgia; 

- partidas relajadas; 

- recreación de momentos históricos del Pokémon TCG. 

## Evolución perpetua 

Cada nueva expansión deberá integrarse naturalmente al PvE. 

El sistema está diseñado para crecer indefinidamente sin requerir rediseños estructurales. 

# I.4 Arquitectura General 

El PvE se divide en dos grandes categorías de contenido. 

## Contenido Diseñado 

Experiencias creadas manualmente por los desarrolladores. Incluye: 

- Academia de Combate. 

- Aventuras. 

- Jefes. 

- Desafíos especiales. 

- Eventos permanentes. 

Este contenido posee identidad propia, narrativa cuando corresponda y objetivos cuidadosamente diseñados. 

## Contenido Procedural 

Experiencias generadas automáticamente por el sistema. Incluye: 

- generación automática de rivales; 

- generación automática de barajas; 

- desafíos infinitos; 

- eventos dinámicos; 

- modos de rejugabilidad permanente. 

Este contenido garantiza que el juego nunca se agote, incluso décadas después de su lanzamiento. 

# I.5 Independencia entre Sistemas 

El PvE está construido mediante sistemas independientes que trabajan conjuntamente. Cada combate se define por la combinación de tres elementos: 

### Actividad 

Define el contenido que se está jugando. 

Ejemplos: 

- Academia. 

- Entrenamiento. 

- Aventura. 

- Jefe. 

- Desafío. 

- Evento. 

- Modo Infinito. 

### Formato 

Define las reglas de construcción de mazos. 

Inicialmente existirán: 

- **Estándar** , siguiendo las reglas oficiales vigentes del Pokémon TCG. 

- **Libre** , permitiendo utilizar cartas de toda la historia del juego. 

La arquitectura permitirá incorporar nuevos formatos en el futuro sin modificar el motor del juego. 

### Modo de Combate 

Define el ritmo de la partida. Existirán dos modalidades universales: 

#### **En Vivo** 

- Cronómetro oficial por jugador. 

- Desarrollo continuo de la partida. 

- Equivalente al ritmo competitivo oficial. 

#### **Por Turnos** 

- Sin límite práctico de duración. 

- El estado completo de la partida se guarda al finalizar cada turno. 

- Una partida puede prolongarse durante días o semanas. 

- Disponible tanto contra la IA como contra otros jugadores. 

Estas tres capas son completamente independientes, permitiendo que cualquier actividad PvE pueda jugarse en cualquiera de los formatos y modos compatibles. 

# I.6 Papel del PvE dentro del Proyecto 

El PvE representa el principal medio para preservar la historia jugable del Pokémon Trading Card Game. 

Su función no consiste únicamente en proporcionar rivales controlados por la IA, sino en ofrecer un entorno donde cada expansión, carta y estrategia pueda seguir siendo disfrutada sin depender de la actividad de la comunidad. 

De esta manera, el PvE garantiza que el proyecto continúe siendo plenamente disfrutable incluso dentro de varias décadas, cumpliendo la misión de preservación permanente que define a Pokémon TCG Clone. 

FASE 8 — PvE 

Capítulo II. Arquitectura del PvE 

II.1 Visión General 

El modo PvE está compuesto por un conjunto de sistemas especializados que cumplen funciones diferentes pero complementarias. 

Cada sistema puede evolucionar de forma independiente, permitiendo ampliar el contenido durante décadas sin modificar la arquitectura general. 

Todos los sistemas comparten el mismo motor de combate, las mismas reglas oficiales y los mismos formatos de juego, garantizando una experiencia coherente en todo el proyecto. 

II.2 Academia de Combate 

La Academia de Combate constituye el punto de entrada al juego. 

Su objetivo principal es enseñar al jugador el funcionamiento del Pokémon Trading Card Game de forma progresiva y permanente. 

La Academia incluirá, entre otros: 

Tutoriales básicos. 

Mecánicas intermedias. 

Mecánicas avanzadas. 

Explicaciones de nuevas reglas. 

Lecciones asociadas a futuras expansiones. 

Ejercicios prácticos. 

Combates guiados. 

La Academia permanecerá actualizada durante toda la vida del proyecto. 

II.3 Entrenamiento Libre 

El modo Entrenamiento permite practicar sin presión competitiva. 

El jugador podrá personalizar prácticamente todos los parámetros del combate. 

Entre ellos: 

Formato; 

Modo de combate; 

Dificultad; 

Rival; 

Baraja propia; 

Baraja rival; 

Expansión utilizada; 

Reglas disponibles. 

Su finalidad es facilitar el aprendizaje, las pruebas de nuevas estrategias y el perfeccionamiento de barajas. 

II.4 Desafíos 

Los Desafíos representan objetivos específicos que modifican la forma habitual de jugar. 

Su finalidad es fomentar la creatividad y ampliar la rejugabilidad. 

Podrán incluir restricciones como: 

Número máximo de turnos; 

Tipos específicos de Pokémon; 

Rarezas determinadas; 

Regiones concretas; 

Expansiones específicas; 

Condiciones especiales de victoria; 

Limitaciones de construcción de mazos. 

Existirán desafíos permanentes y desafíos generados automáticamente. 

II.5 Aventuras 

Las Aventuras constituyen el contenido narrativo y progresivo del PvE. 

Cada aventura organiza una serie de combates conectados mediante un recorrido temático. 

Podrán estar inspiradas en: 

Regiones Pokémon; 

Personajes históricos; 

Organizaciones; 

Acontecimientos oficiales; 

Colecciones especiales; 

Expansiones del TCG. 

Cada aventura podrá incorporar: 

Mapas; 

Rutas; 

Rivales; 

Jefes; 

Recompensas; 

Objetivos secundarios. 

Las Aventuras nunca serán eliminadas del juego. 

II.6 Jefes 

Los Jefes representan enfrentamientos diseñados para poner a prueba el dominio del jugador. 

Cada jefe posee una identidad propia, una estrategia definida y un nivel de dificultad cuidadosamente ajustado. 

Podrán representar: 

Campeones. 

Líderes de gimnasio. 

Profesores. 

Rivales. 

Villanos. 

Personajes históricos del TCG. 

Entrenadores originales del proyecto. 

Un mismo jefe podrá disponer de múltiples niveles de dificultad. 

II.7 Laboratorio de Inteligencia Artificial 

El Laboratorio de IA constituye el núcleo tecnológico del PvE. 

Su función es generar automáticamente contenido jugable utilizando las cartas disponibles en la base de datos. 

Entre sus responsabilidades se encuentran: 

Construir barajas automáticamente; 

Analizar sinergias; 

Evaluar estrategias; 

Seleccionar cartas compatibles; 

Adaptar la dificultad; 

Utilizar nuevas expansiones sin programación manual; 

Alimentar otros modos del PvE con rivales variados. 

Gracias a este sistema, el contenido PvE podrá crecer indefinidamente conforme aumente la colección histórica del juego. 

II.8 Eventos PvE 

Los Eventos ofrecen experiencias especiales asociadas a celebraciones, aniversarios o actividades organizadas por la comunidad privada. 

Podrán reutilizar cualquiera de los sistemas existentes. 

Ejemplos: 

Pokémon Day. 

Halloween. 

Navidad. 

Aniversarios del proyecto. 

Eventos temáticos de una expansión. 

Eventos personalizados para la comunidad. 

Cuando finalice un evento temporal, el proyecto podrá conservarlo como contenido histórico para su futura reutilización. 

II.9 Modo Infinito 

El Modo Infinito representa el sistema de mayor longevidad del PvE. 

Su objetivo es generar una experiencia prácticamente inagotable mediante contenido procedural. 

Podrá combinar automáticamente: 

Rivales; 

Barajas; 

Modificadores; 

Restricciones; 

Dificultades; 

Recompensas; 

Objetivos especiales. 

Cada partida podrá ser diferente de la anterior, garantizando una rejugabilidad permanente. 

II.10 Relación entre los Sistemas 

Los distintos sistemas del PvE no funcionan de forma aislada. 

Comparten recursos tecnológicos y pueden interactuar entre sí. 

Por ejemplo: 

La IA puede generar rivales para Entrenamiento, Desafíos, Aventuras y Modo Infinito. 

Los Jefes pueden aparecer dentro de Aventuras o Eventos. 

Los Eventos pueden reutilizar mapas, desafíos y rivales existentes. 

La Academia puede utilizar versiones simplificadas de la IA para enseñar nuevas mecánicas. 

Esta reutilización evita duplicar contenido y facilita el mantenimiento del proyecto. 

II.11 Escalabilidad de la Arquitectura 

La arquitectura del PvE está diseñada para admitir la incorporación de nuevos sistemas sin modificar los existentes. 

En el futuro podrán añadirse nuevos modos de juego utilizando la misma estructura, siempre que respeten la filosofía general del proyecto. 

De esta forma, el PvE mantiene una arquitectura modular, escalable y preparada para evolucionar durante décadas, preservando toda la historia jugable del Pokémon Trading Card Game sin comprometer la compatibilidad con el contenido ya existente. 

FASE 8 — PvE 

Capítulo III. Formatos y Modos de Combate 

III.1 Objetivo 

Todos los combates del modo PvE se desarrollan utilizando una arquitectura común basada en Formatos de Juego y Modos de Combate. 

Esta separación permite que cualquier actividad del PvE pueda jugarse de distintas maneras sin duplicar contenido ni crear versiones independientes de un mismo modo. 

La arquitectura también será compartida por el PvP, garantizando un funcionamiento uniforme en todo Pokémon TCG Clone. 

III.2 Formatos de Juego 

El formato determina qué cartas pueden utilizarse para construir un mazo. 

El formato no modifica las reglas del combate, únicamente establece la legalidad de las cartas disponibles. 

Inicialmente existirán dos formatos oficiales. 

III.3 Formato Estándar 

El Formato Estándar seguirá en todo momento la normativa oficial vigente del Pokémon Trading Card Game. 

Su objetivo es ofrecer una experiencia idéntica al entorno competitivo oficial. 

Entre sus características se encuentran: 

Rotaciones oficiales; 

Marcas de regulación oficiales; 

Listas oficiales de cartas permitidas; 

Futuras modificaciones publicadas oficialmente por The Pokémon Company. 

Cada vez que el formato oficial cambie, Pokémon TCG Clone actualizará automáticamente su Formato Estándar para mantener la máxima fidelidad posible. 

III.4 Formato Libre 

El Formato Libre representa el principal entorno de preservación histórica del proyecto. 

Permite construir barajas utilizando cartas de cualquier expansión disponible en la colección del juego. 

No existen rotaciones. 

Todas las generaciones del Pokémon Trading Card Game pueden coexistir. 

Este formato garantiza que ninguna carta publicada oficialmente pierda utilidad con el paso de los años. 

III.5 Expansión Futura de Formatos 

La arquitectura permite incorporar nuevos formatos sin modificar el motor del juego. 

Ejemplos potenciales: 

Formatos históricos; 

Formatos temáticos; 

Formatos por región; 

Formatos por era; 

Formatos creados por la comunidad privada; 

Cualquier otro formato compatible con la filosofía del proyecto. 

La incorporación de nuevos formatos no afectará a los ya existentes. 

III.6 Modos de Combate 

El modo de combate determina el ritmo temporal de una partida. 

Todos los formatos podrán utilizar cualquiera de los modos compatibles. 

Inicialmente existirán dos modos universales. 

III.7 Modo En Vivo 

El Modo En Vivo reproduce el ritmo tradicional del Pokémon Trading Card Game. 

Sus características principales son: 

Partida continua; 

Cronómetro individual para cada jugador; 

Resolución inmediata de acciones; 

Experiencia equivalente al juego competitivo oficial. 

Este modo estará disponible tanto para PvE como para PvP. 

El tiempo por jugador utilizará la configuración oficial vigente o aquella definida por la organización de eventos privados del proyecto. 

III.8 Modo Por Turnos 

El Modo Por Turnos constituye una de las características exclusivas de Pokémon TCG Clone. 

En este modo cada jugador realiza su turno cuando le resulte conveniente. 

El sistema guarda automáticamente el estado completo de la partida al finalizar cada turno. 

No existe un límite práctico para la duración total del encuentro. 

Una partida podrá extenderse durante horas, días o incluso semanas sin perder información. 

Este modo permite disfrutar del juego incluso cuando los participantes no pueden coincidir al mismo tiempo. 

III.9 Compatibilidad del Modo Por Turnos 

El Modo Por Turnos podrá utilizarse en: 

Entrenamiento; 

Aventuras; 

Desafíos; 

Jefes; 

Eventos; 

PvP Amistoso. 

Su disponibilidad en otros modos dependerá de las características particulares de cada actividad. 

El modo Ranked no utilizará esta modalidad, ya que requiere condiciones competitivas sincronizadas. 

III.10 Combinación de Sistemas 

Cada combate del juego queda definido mediante la combinación de tres elementos independientes: 

Actividad 

Academia. 

Entrenamiento. 

Desafío. Aventura. Jefe. Evento. Modo Infinito. Formato Estándar. Libre. Otros que puedan incorporarse en el futuro. 

Modo de Combate 

En Vivo. 

Por Turnos. 

Gracias a esta arquitectura, un mismo contenido puede ofrecer múltiples experiencias sin necesidad de desarrollarse nuevamente. 

III.11 Persistencia de las Partidas 

Todas las partidas deberán poder almacenar su estado completo para permitir su recuperación cuando sea necesario. 

Esta capacidad es obligatoria en el Modo Por Turnos y podrá utilizarse adicionalmente como sistema de recuperación ante cierres inesperados, desconexiones o fallos técnicos en cualquier modalidad compatible. 

La persistencia garantiza la continuidad de las partidas y contribuye a la preservación técnica del proyecto. 

III.12 Principios de Diseño 

La arquitectura de formatos y modos de combate se fundamenta en los siguientes principios: 

Fidelidad al reglamento oficial. 

Preservación permanente de todas las cartas. 

Libertad del jugador para elegir cómo jugar. 

Compatibilidad con futuras expansiones. 

Escalabilidad a nuevos formatos. 

Reutilización del mismo motor de combate. 

Independencia entre contenido, reglas y ritmo de juego. 

Esta separación constituye una de las bases arquitectónicas de Pokémon TCG Clone y permite que el proyecto evolucione durante décadas sin comprometer la compatibilidad con el contenido previamente desarrollado. 

# FASE 8 — PvE 

# Capítulo IV. Sistema de Inteligencia Artificial 

# IV.1 Objetivo 

La Inteligencia Artificial constituye el núcleo tecnológico del PvE. 

Su misión no consiste únicamente en controlar al rival durante los combates, sino en construir automáticamente experiencias variadas, coherentes y estratégicamente interesantes que puedan crecer junto con el juego durante décadas. 

La IA está diseñada para evolucionar conforme aumenta la base histórica de cartas, evitando que el contenido PvE dependa exclusivamente de barajas creadas manualmente por los desarrolladores. 

# IV.2 Filosofía de Diseño 

La IA debe comportarse como un jugador real. No recibirá ventajas artificiales. No conocerá cartas ocultas. No modificará probabilidades. No robará cartas especiales. No romperá las reglas oficiales del Pokémon Trading Card Game. 

Su dificultad provendrá exclusivamente de la calidad de sus decisiones. 

# IV.3 Arquitectura General 

El sistema de IA se divide en cuatro componentes principales: 

## Constructor de Barajas 

Genera automáticamente mazos completos. 

## Analizador Estratégico 

Evalúa las sinergias entre cartas. 

## Motor de Decisiones 

Determina las acciones durante la partida. 

## Gestor de Dificultad 

Ajusta el nivel estratégico del rival sin alterar las reglas del juego. Cada componente puede evolucionar independientemente. 

# IV.4 Constructor Automático de Barajas 

El Constructor de Barajas representa uno de los sistemas más importantes del proyecto. Su función consiste en crear mazos competitivos utilizando únicamente las cartas permitidas por el formato seleccionado. 

Para ello analizará automáticamente: 

- efectos; 

- habilidades; 

- ataques; 

- costes de energía; 

- evolución; 

- consistencia; 

- aceleración de recursos; 

- robo de cartas; 

- búsqueda; 

- recuperación; 

- control; 

- sinergias; 

- condiciones de victoria. 

El sistema no dependerá de listas prediseñadas. 

Cada nueva expansión incrementará automáticamente las posibilidades de construcción. 

# IV.5 Análisis de Sinergias 

La IA evaluará las relaciones existentes entre las cartas. Entre otros aspectos analizará: 

- líneas evolutivas; 

- motores de robo; 

- aceleración de energía; 

- buscadores; 

- recuperación; 

- reducción de costes; 

- daño directo; 

- daño distribuido; 

- control del rival; 

- manipulación de premios; 

- estrategias defensivas; 

- combinaciones de habilidades; 

- cartas de apoyo. 

Cuanto mayor sea la colección disponible, mayor será la diversidad estratégica. 

# IV.6 Motor de Decisiones 

Durante la partida la IA analizará continuamente el estado del combate. Entre otros factores tendrá en cuenta: 

- cartas en mano; 

- cartas restantes en la baraja; 

- cartas descartadas; 

- banca; 

- Pokémon Activo; 

- Energías disponibles; 

- Premios restantes; 

- probabilidades; 

- recursos futuros; 

- amenazas del rival; 

- posibles jugadas. 

Su objetivo será seleccionar la mejor decisión posible con la información disponible en ese momento. 

# IV.7 Sistema de Dificultad 

La dificultad nunca se incrementará otorgando ventajas ilegítimas. 

En su lugar, cada nivel modificará la calidad del razonamiento estratégico. Los niveles superiores serán capaces de: 

- planificar varios turnos por adelantado; 

- reconocer más sinergias; 

- administrar mejor los recursos; 

- anticipar amenazas; 

- identificar condiciones de victoria; 

- minimizar errores; 

- optimizar secuencias de juego. 

Los niveles inferiores cometerán errores similares a los de un jugador principiante. 

# IV.8 Diversidad Estratégica 

La IA evitará repetir continuamente las mismas estrategias. 

El sistema favorecerá la aparición de distintos estilos de juego, entre ellos: 

- agresivo; 

- equilibrado; 

- defensivo; 

- control; 

- combinación de estrategias; 

- experimental. 

Esta diversidad incrementará la rejugabilidad del PvE. 

# IV.9 Integración con el PvE 

Todos los sistemas del PvE podrán utilizar la IA. 

Entre ellos: 

- Academia de Combate. 

- Entrenamiento. 

- Desafíos. 

- Aventuras. 

- Jefes. 

- Eventos. 

- Modo Infinito. 

Cada actividad configurará la IA según sus necesidades, reutilizando la misma arquitectura. 

# IV.10 Aprendizaje Permanente 

La IA está diseñada para mantenerse vigente durante toda la vida del proyecto. 

Cada nueva expansión incorporada ampliará automáticamente el conjunto de estrategias disponibles. 

No será necesario rediseñar el sistema con cada lanzamiento de cartas. 

La evolución del juego incrementará de forma natural la complejidad y riqueza estratégica del PvE. 

# IV.11 Duplicación de Barajas 

Cuando la IA utilice una baraja generada automáticamente durante un combate, el jugador podrá guardarla en su colección personal de barajas al finalizar la partida. Esta función permitirá: 

- estudiar estrategias utilizadas por la IA; 

- modificar y mejorar las barajas generadas; 

- utilizarlas posteriormente en otros modos compatibles; 

- enriquecer la colección personal sin necesidad de reconstruir manualmente cada mazo. 

La duplicación únicamente almacenará la **lista de la baraja** ; el jugador seguirá necesitando poseer las cartas correspondientes para poder utilizarla, respetando las reglas generales de colección del proyecto. 

# IV.12 Principios Fundamentales 

Todo el sistema de Inteligencia Artificial deberá cumplir permanentemente los siguientes principios: 

- Respeto absoluto a las reglas oficiales del Pokémon Trading Card Game. 

   - Transparencia y ausencia de ventajas artificiales. 

- 

- Generación automática de contenido escalable. 

- Diversidad estratégica. 

- Compatibilidad con todas las expansiones presentes y futuras. 

- Reutilización por todos los modos PvE. 

- Mantenimiento mínimo a largo plazo. 

- Evolución perpetua, garantizando que la IA continúe ofreciendo desafíos interesantes durante décadas sin comprometer la filosofía de preservación de Pokémon TCG Clone. 

FASE 8 — PvE 

Capítulo V. Progresión, Recompensas e Integración con el Proyecto 

V.1 Objetivo 

El sistema de progresión del PvE tiene como finalidad recompensar el tiempo, el aprendizaje y la constancia del jugador sin convertir ninguna actividad en una obligación. 

Todo el contenido PvE debe contribuir al crecimiento permanente de la cuenta, respetando las decisiones establecidas en las fases anteriores del proyecto. 

El progreso obtenido nunca caducará ni perderá valor con el paso del tiempo. 

V.2 Integración con la Economía 

Todas las recompensas del PvE utilizarán exclusivamente los sistemas económicos definidos en las fases anteriores. 

El PvE podrá otorgar: 

Monedas. 

Cristales. 

Fichas de Crafting. 

Sobres. 

Cartas promocionales. 

Objetos cosméticos. 

Recompensas exclusivas de eventos. 

No se crearán monedas exclusivas para el PvE. 

Toda la economía permanecerá unificada en todo el proyecto. 

V.3 Integración con el Sistema de Logros 

Cada actividad PvE podrá contribuir al progreso de los Logros Permanentes. 

Ejemplos: 

Completar aventuras; 

Derrotar jefes; 

Finalizar desafíos; 

Ganar partidas; 

Utilizar determinados tipos; 

Utilizar determinadas regiones; 

Completar tutoriales; 

Superar dificultades elevadas. 

Los logros nunca obligarán al jugador a abandonar su estilo de juego favorito. 

Siempre existirán múltiples caminos para progresar. 

V.4 Integración con el Museo 

Toda la actividad PvE podrá generar registros para el Museo del jugador. 

Entre ellos: 

Aventuras completadas. 

Jefes derrotados. 

Eventos históricos superados. 

Desafíos especiales. 

Trofeos obtenidos. 

Colecciones desbloqueadas. 

Contenido histórico descubierto. 

Estos registros tendrán carácter documental y de preservación, fortaleciendo el valor histórico de la cuenta. 

V.5 Integración con el Sistema de Legado 

El PvE contribuirá al desarrollo del Legado del jugador mediante el reconocimiento de sus logros a largo plazo. 

El sistema podrá registrar, entre otros aspectos: 

Campañas completadas; 

Dificultades máximas superadas; 

Desafíos únicos; 

Participación en eventos históricos; 

Hitos excepcionales. 

El Legado no otorgará ventajas competitivas. 

Su finalidad será preservar la historia personal de cada jugador dentro del proyecto. 

V.6 Recompensas por Dificultad 

Las recompensas estarán relacionadas con el nivel de desafío asumido. 

En términos generales: 

Dificultades superiores ofrecerán mejores recompensas; 

Contenidos más extensos proporcionarán recompensas acordes al tiempo invertido; 

Los desafíos opcionales podrán incluir recompensas adicionales. 

La diferencia entre dificultades nunca será tan grande como para obligar a todos los jugadores a utilizar únicamente el nivel más alto. 

V.7 Recompensas por Primera Superación 

El primer completado de una actividad podrá otorgar recompensas especiales. 

Ejemplos: 

Sobres; 

Cartas promocionales; 

Cosméticos; 

Trofeos; 

Elementos del Museo; 

Insignias. 

Estas recompensas incentivarán descubrir nuevo contenido sin penalizar la repetición posterior. 

#### V.8 Rejugabilidad 

Una vez completada una actividad, el jugador podrá repetirla tantas veces como desee. 

Las repeticiones continuarán ofreciendo recompensas, aunque estas podrán diferir de las obtenidas durante la primera superación. 

El objetivo es que ningún contenido quede obsoleto después de ser completado una sola vez. 

V.9 Integración con la IA 

El progreso del jugador nunca modificará las reglas del combate. 

La IA adaptará únicamente su comportamiento estratégico cuando la actividad así lo requiera. 

Las recompensas dependerán del contenido completado y de la dificultad seleccionada, no de ventajas artificiales otorgadas al rival. 

V.10 Compatibilidad con Futuras Expansiones 

Cada nueva expansión podrá añadir automáticamente: 

Nuevas aventuras; 

Nuevos jefes; 

Nuevos desafíos; 

Nuevos eventos; 

Nuevas recompensas; 

Nuevos logros. 

La arquitectura evita limitar el crecimiento del PvE y permite incorporar contenido de forma continua durante décadas. 

V.11 Principios Fundamentales 

El sistema de progresión del PvE deberá respetar permanentemente los siguientes principios: 

Progreso permanente. 

Recompensas coherentes con la economía general. 

Integración completa con Logros, Museo y Legado. 

Libertad para elegir cualquier actividad PvE. 

Ausencia de contenido obligatorio. 

Rejugabilidad ilimitada. 

Compatibilidad con futuras expansiones. 

Preservación histórica del progreso del jugador. 

De esta forma, el PvE se convierte en un pilar plenamente integrado con el resto de Pokémon TCG Clone, ofreciendo una progresión significativa, sostenible y alineada con la filosofía de preservación y evolución perpetua del proyecto. 

FASE 8 — PvE 

Capítulo VI. Escalabilidad, Preservación y Evolución Permanente 

VI.1 Objetivo 

El PvE de Pokémon TCG Clone está diseñado para mantenerse vigente durante toda la vida del proyecto. 

Su arquitectura permite incorporar nuevas expansiones, mecánicas, aventuras y modos de juego sin reemplazar ni eliminar el contenido existente. 

Este principio garantiza que el PvE pueda evolucionar durante décadas respetando la filosofía de preservación permanente del proyecto. 

VI.2 Preservación del Contenido 

Todo el contenido PvE desarrollado para el proyecto tendrá carácter permanente. 

Esto incluye: 

Academia de Combate. 

Aventuras. 

Jefes. 

Desafíos. 

Eventos permanentes. 

Rivales. 

Recompensas históricas. 

Contenido narrativo. 

La incorporación de nuevo contenido nunca implicará la eliminación del anterior. 

Cada generación del Pokémon Trading Card Game permanecerá accesible para futuras generaciones de jugadores. 

VI.3 Compatibilidad con Nuevas Expansiones 

Cada nueva expansión oficial podrá integrarse en el PvE sin modificar la arquitectura existente. 

La incorporación de una expansión permitirá, entre otros aspectos: 

Ampliar el conjunto de cartas disponibles; 

Generar nuevas barajas mediante la IA; 

Crear nuevas aventuras; 

Añadir nuevos jefes; 

Incorporar nuevos desafíos; 

Habilitar nuevos eventos; 

Ampliar el Museo; 

Generar nuevos logros. 

La compatibilidad con futuras expansiones forma parte del diseño original del sistema. 

VI.4 Arquitectura Modular 

Cada componente del PvE funciona como un módulo independiente. 

Esto permite actualizar o ampliar un sistema sin afectar a los demás. 

Por ejemplo: 

Una nueva aventura no requiere modificar la IA; 

Un nuevo jefe reutiliza el mismo motor de combate; 

Un nuevo desafío utiliza las reglas existentes; 

Un nuevo evento puede combinar sistemas ya desarrollados. 

Esta modularidad reduce el mantenimiento y facilita la evolución continua del proyecto. 

VI.5 Reutilización de Sistemas 

Uno de los principios fundamentales del PvE es la reutilización. 

Los distintos sistemas compartirán componentes comunes, entre ellos: 

Motor de combate; 

Inteligencia artificial; 

Constructor automático de barajas; 

Gestor de dificultad; 

Formatos de juego; 

Modos de combate; 

Sistema de recompensas; 

Economía; 

Logros; 

Museo; 

Legado. 

Evitar la duplicación de sistemas mejora la estabilidad y simplifica el desarrollo a largo plazo. 

VI.6 Evolución Tecnológica 

La evolución del proyecto no estará limitada por la tecnología utilizada en el momento de su creación. 

La arquitectura permitirá sustituir o mejorar componentes internos sin alterar el contenido ya existente. 

Esto incluye, entre otros: 

Mejoras en la Inteligencia Artificial; 

Optimizaciones del motor; 

Nuevas interfaces; 

Mejoras gráficas; 

Nuevas plataformas compatibles. 

La preservación del contenido tendrá siempre prioridad sobre la tecnología empleada para ejecutarlo. 

VI.7 Compatibilidad Retroactiva 

Toda mejora incorporada al PvE deberá mantener la compatibilidad con el contenido desarrollado anteriormente. 

Las nuevas funciones no invalidarán: 

Aventuras antiguas; 

Jefes existentes; 

Desafíos históricos; 

Recompensas obtenidas; 

Progresos registrados; 

Formatos previamente definidos. 

Este principio evita la fragmentación del proyecto con el paso del tiempo. 

VI.8 Crecimiento Ilimitado 

La arquitectura del PvE no establece un límite predeterminado para: 

Número de aventuras; 

Número de jefes; 

Número de desafíos; 

Número de eventos; 

Número de rivales; 

Número de expansiones; 

Número de cartas; 

Número de formatos compatibles. 

El sistema ha sido concebido para crecer de forma continua mientras exista contenido oficial del Pokémon Trading Card Game o nuevo contenido creado para la comunidad privada. 

VI.9 Independencia de la Comunidad Activa 

El PvE garantiza que Pokémon TCG Clone siga siendo completamente disfrutable independientemente del número de jugadores conectados. 

Incluso si la comunidad fuera muy reducida, el jugador dispondrá de una experiencia completa gracias a: 

La Inteligencia Artificial; 

Las Aventuras; 

Los Desafíos; 

Los Jefes; 

El Modo Infinito; 

Los Eventos; 

El Entrenamiento. 

De esta manera, el proyecto conserva su valor incluso décadas después de su lanzamiento. 

VI.10 Filosofía de Longevidad 

Cada decisión relacionada con el PvE deberá responder a una única pregunta antes de implementarse: 

“¿Seguirá siendo útil, compatible y disfrutable dentro de veinte o treinta años?” 

Si la respuesta es negativa, la funcionalidad deberá rediseñarse antes de formar parte del proyecto. 

Este criterio asegura que el PvE permanezca alineado con la misión de crear la versión definitiva y permanente del Pokémon Trading Card Game para la comunidad privada. 

VI.11 Conclusión del Diseño del PvE 

El PvE de Pokémon TCG Clone se concibe como un ecosistema permanente, escalable y autosuficiente. 

Su combinación de contenido diseñado, generación procedural, inteligencia artificial, progresión integrada y arquitectura modular garantiza que cualquier carta, expansión o estrategia pueda seguir utilizándose durante décadas. 

Con ello, el PvE cumple uno de los objetivos fundamentales del proyecto: preservar toda la historia jugable del Pokémon Trading Card Game y asegurar que siempre exista una forma de disfrutarla, aprenderla y explorarla, independientemente del paso del tiempo o del tamaño de la comunidad. 

