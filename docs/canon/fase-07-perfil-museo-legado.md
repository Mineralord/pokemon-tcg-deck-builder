**FASE 7 — PERFIL DEL JUGADOR, MUSEO Y LEGADO** 

# Documento Canónico (Edición Definitiva) 

# PARTE 1 

# I. Introducción 

La Fase 7 define la identidad permanente de cada jugador dentro del proyecto Pokémon TCG Clone. 

Hasta este punto del diseño, las fases anteriores han establecido cómo funciona el juego, cómo se obtienen las cartas, cómo se administran los recursos y cómo progresa un jugador mediante logros y recompensas. 

Sin embargo, aún no existía un lugar donde toda esa información pudiera organizarse de forma coherente y permanente. 

Esta fase resuelve ese problema. 

Su objetivo no es únicamente crear una pantalla de perfil, sino diseñar un sistema que acompañe al jugador durante toda la vida del proyecto, incluso si éste continúa desarrollándose durante décadas. 

El Perfil del Jugador se convierte en la representación actual del usuario. 

La Colección representa todo aquello que posee. 

El Museo representa aquello que el jugador desea conservar y exhibir. 

El Legado representa la memoria permanente de toda su trayectoria. 

Estos cuatro sistemas trabajan juntos, pero permanecen completamente desacoplados para garantizar la máxima escalabilidad, facilidad de mantenimiento y compatibilidad con futuras expansiones. 

# II. Objetivos de la Fase 

Esta fase tiene como propósito diseñar una estructura capaz de conservar la historia del jugador sin comprometer el rendimiento del juego ni generar dependencias innecesarias entre sistemas. 

Los objetivos principales son: 

- Definir la identidad permanente del jugador. 

- Organizar correctamente la información del usuario. 

- Separar claramente el estado actual de la información histórica. 

- Permitir la personalización del perfil. 

- Crear un espacio para exhibición de colecciones. 

- Registrar únicamente los hitos realmente importantes. 

- Evitar el almacenamiento innecesario de información irrelevante. 

- Garantizar que el sistema continúe siendo funcional incluso después de décadas de actualizaciones. 

# III. Principios de Diseño 

Toda la arquitectura de esta fase sigue los principios generales establecidos en la Filosofía del Proyecto. 

Además incorpora los siguientes principios específicos. 

## 1. Separación de responsabilidades 

Cada sistema debe tener una única función. Nunca deberán mezclarse responsabilidades. Por ejemplo: 

El Perfil no almacena cartas. 

La Colección no almacena logros. 

El Museo no almacena estadísticas. 

El Legado no almacena la colección completa. 

Cada uno cumple una función específica. 

## 2. Permanencia 

Todo aquello que represente un logro importante del jugador debe poder conservarse para siempre. 

El proyecto pretende sobrevivir durante generaciones. 

Por ello, la información histórica relevante nunca deberá perderse. 

## 3. Simplicidad 

Aunque el sistema pueda contener millones de cartas y miles de logros, el jugador debe entender inmediatamente dónde encontrar cada tipo de información. 

La organización debe resultar intuitiva. 

## 4. Escalabilidad 

El sistema deberá admitir: 

- nuevas expansiones; 

- nuevos modos de juego; 

- nuevas estadísticas; 

- nuevos cosméticos; 

- nuevos eventos; 

- nuevas vitrinas del museo; 

- nuevas formas de personalización. 

Todo ello sin necesidad de modificar la arquitectura base. 

## 5. Independencia 

Cada módulo podrá evolucionar de forma independiente. Una actualización del Museo nunca deberá afectar la Colección. 

Una actualización del Perfil nunca deberá modificar el Legado. 

Esto facilita enormemente el mantenimiento a largo plazo. 

## 6. Preservación 

La información importante nunca será eliminada por una actualización. 

Las nuevas versiones del juego deberán ser completamente compatibles con los datos almacenados durante años anteriores. 

Este principio es fundamental para cumplir la filosofía de preservación permanente del proyecto. 

# IV. Arquitectura General 

Después del análisis completo del proyecto se decidió dividir toda la información del jugador en cuatro grandes sistemas independientes. 

Esta separación evita duplicación de datos, simplifica el mantenimiento y garantiza la compatibilidad futura. 

La arquitectura queda definida de la siguiente manera. 

## Perfil 

Representa al jugador en el presente. 

Es la identidad pública del usuario. 

Contiene únicamente información actual. 

Ejemplos: 

- nombre del jugador; 

- avatar; 

- marco del perfil; 

- nivel; 

- experiencia; 

- monedas; 

- cristales; 

- fichas; 

- estado en línea; 

- estadísticas visibles. 

El Perfil nunca almacenará información histórica. 

## Colección 

Representa todas las posesiones actuales del jugador. Es el inventario permanente del juego. 

Incluye: 

- cartas; 

- mazos; 

- fundas; 

- monedas del juego; 

- cajas; 

- tapetes; 

- accesorios; 

- cosméticos desbloqueados; 

- objetos coleccionables futuros. 

La Colección únicamente representa lo que el jugador posee actualmente. No registra cómo se obtuvo. 

No almacena recuerdos. 

No almacena eventos pasados. 

## Museo 

El Museo es una galería personal. Permite al jugador seleccionar aquello que desea mostrar. Su función no es almacenar objetos, sino exhibirlos. 

Podrá contener, entre otros: 

- cartas favoritas; 

- mazos históricos; 

- colecciones completas destacadas; 

- cartas firmadas (si algún día existieran); 

- trofeos importantes; 

- vitrinas temáticas; 

- exhibiciones personalizadas. 

Todo el contenido del Museo es elegido por el jugador. Nada aparece automáticamente. El Museo representa la parte emocional y coleccionista del proyecto. 

## Legado 

El Legado constituye la memoria permanente del jugador. 

A diferencia del Museo, el Legado no depende de la decisión del usuario. 

Su contenido se registra automáticamente. 

Aquí se almacenan únicamente acontecimientos importantes de toda la vida de la cuenta. Ejemplos: 

- fecha de creación de la cuenta; 

- primer sobre abierto; 

- primera carta Ultra Rara; 

- primer Pokémon ex; 

- primer set completado; 

- primer torneo ganado; 

- títulos históricos; 

- eventos conmemorativos; 

- aniversarios importantes; 

- reconocimientos especiales. 

El Legado jamás pretende registrar todas las acciones del jugador. 

Su propósito consiste únicamente en conservar los hitos que cuentan la historia de su trayectoria dentro del proyecto. 

**Fin de la Parte 1.** En la **Parte 2** se desarrollarán los capítulos **V. Perfil del Jugador** y **VI. Colección** , definiendo en detalle todos los elementos, información visible, organización y funcionamiento de ambos sistemas. 

### FASE 7 — PERFIL DEL JUGADOR, MUSEO Y LEGADO 

Documento Canónico (Edición Definitiva) 

PARTE 2 

### V. Perfil del Jugador 

### 5.1 Objetivo 

El Perfil del Jugador representa la identidad actual del usuario dentro del proyecto. 

No constituye un historial ni un inventario. 

Su finalidad es mostrar quién es el jugador en este momento. 

Debe ser limpio, fácilmente reconocible y personalizable. 

El Perfil será la principal carta de presentación del usuario ante otros jugadores. 

### 5.2 Información mostrada 

El Perfil podrá mostrar únicamente información correspondiente al estado actual de la cuenta. 

Entre ella se incluyen: 

Nombre del jugador. 

ID único de la cuenta. 

Avatar. 

Marco del perfil. Fondo del perfil. 

Insignia o título activo. 

Nivel actual. 

Experiencia acumulada hacia el siguiente nivel. 

Fecha de creación de la cuenta. 

Estado en línea. 

Región o idioma (si aplica). 

Monedas disponibles. 

Cristales disponibles. 

Fichas disponibles. 

Toda esta información podrá actualizarse automáticamente conforme avance la cuenta. 

### 5.3 Información pública 

El sistema permitirá decidir qué información será visible para otros jugadores. 

Por defecto podrán mostrarse: 

Avatar. 

Nombre. 

Nivel. 

Título activo. 

Marco. 

Fondo. 

Fecha de creación. 

Insignias seleccionadas. 

Las estadísticas detalladas podrán configurarse como públicas o privadas. 

La colección nunca será pública de manera obligatoria. 

5.4 Avatar 

Cada jugador podrá seleccionar un avatar. 

El sistema deberá admitir: 

Avatares oficiales; 

Avatares obtenidos mediante logros; 

Avatares obtenidos en eventos; 

Avatares conmemorativos; 

Futuras categorías de avatar. 

Los avatares únicamente modifican la apariencia visual. 

Nunca proporcionarán ventajas dentro del juego. 

### 5.5 Marcos 

El marco rodea el avatar. 

Representa uno de los principales elementos cosméticos del perfil. 

Podrá obtenerse mediante: 

Logros; 

Eventos; 

Temporadas; 

Desafíos especiales; 

Recompensas futuras. 

Los marcos no afectan ninguna mecánica. 

### 5.6 Fondos 

Cada jugador podrá elegir un fondo para su perfil. 

Los fondos podrán representar: 

Regiones Pokémon; 

Expansiones; 

Pokémon específicos; 

Ilustraciones especiales; 

Aniversarios; 

Eventos. 

Su única función será estética. 

### 5.7 Títulos 

El jugador podrá equipar un único título activo. 

Ejemplos: 

Entrenador Veterano 

Maestro Coleccionista 

Campeón Regional 

Especialista Dragón 

Explorador de Paldea 

Los títulos representan logros personales. 

No otorgan ventajas jugables. 

5.8 Nivel 

El Perfil mostrará siempre el nivel actual. 

La experiencia y la progresión del nivel se encuentran definidas en la Fase 6. 

La Fase 7 únicamente muestra dicha información. 

No modifica el sistema de progresión. 

### 5.9 Estado en línea 

Cuando exista conexión con otros jugadores podrá indicarse: 

En línea. 

Jugando. 

En combate. 

Construyendo mazos. 

Desconectado. 

Este estado es únicamente informativo. 

5.10 Filosofía del Perfil 

El Perfil debe responder únicamente a una pregunta: 

“¿Quién es este jugador hoy?” 

Nunca intentará responder: 

Qué hizo hace cinco años; 

Cuántos combates jugó en 2028; 

Qué mazo utilizó en un torneo antiguo. 

Esa información pertenece al Legado. 

VI. Colección 

6.1 Objetivo 

La Colección constituye el inventario permanente del jugador. 

Todo objeto que el usuario posea deberá encontrarse aquí. 

La Colección representa el patrimonio completo de la cuenta. 

### 6.2 Principios 

La Colección se diseña bajo cinco principios fundamentales. 

Permanencia 

Nada desaparece sin una acción explícita del jugador o una regla previamente definida por el sistema. 

Organización 

Todo objeto debe pertenecer a una categoría clara. 

Nunca existirán elementos “sin clasificar”. 

Escalabilidad 

El sistema podrá almacenar millones de objetos diferentes sin necesidad de modificar su estructura. 

Compatibilidad histórica 

La llegada de nuevas expansiones nunca romperá la organización existente. 

Independencia 

La Colección funciona independientemente del Museo, Perfil y Legado. 

6.3 Contenido de la Colección 

La Colección podrá almacenar, entre otros: 

Cartas 

Todas las cartas obtenidas. 

Mazos 

Todos los mazos creados por el jugador. 

Cosméticos 

Como por ejemplo: 

Fundas; 

Monedas; 

Cajas; 

Tapetes; 

Accesorios; 

Efectos visuales. 

Objetos futuros 

La arquitectura permitirá incorporar nuevas categorías sin alterar el sistema existente. 

6.4 Organización de cartas 

Las cartas podrán organizarse mediante filtros. 

Ejemplos: 

Expansión; 

Serie; 

Año; 

Tipo; 

Pokémon; 

Entrenador; 

Energía; 

Rareza; 

Regulación; 

Idioma (si existiera); 

Ilustrador; 

Número de colección; 

Nombre. 

El objetivo es permitir localizar cualquier carta en pocos segundos, incluso cuando la colección contenga decenas de miles de cartas. 

6.5 Búsqueda avanzada 

La Colección deberá incluir un sistema de búsqueda potente. 

Será posible combinar múltiples filtros simultáneamente para localizar cartas específicas. 

Este sistema será una herramienta esencial tanto para coleccionistas como para constructores de mazos. 

### 6.6 Relación con el Constructor de Mazos 

El Constructor de Mazos utilizará directamente la información almacenada en la Colección. 

No mantendrá una copia independiente de las cartas. 

Esto garantiza que cualquier cambio en la colección se refleje inmediatamente al construir o editar mazos. 

### 6.7 Relación con el Crafting 

El sistema de Fabricación definido en la Fase 5 interactúa directamente con la Colección. 

Cuando una carta es fabricada, pasa automáticamente a formar parte de la Colección. 

Cuando una quinta copia se convierte en Fichas, la Colección actualiza la cantidad correspondiente según las reglas definidas en la Fase 5. 

6.8 Filosofía de la Colección 

La Colección responde a una única pregunta: 

“¿Qué posee actualmente el jugador?” 

No explica cómo consiguió esos objetos. 

No registra cuándo los obtuvo. 

No almacena recuerdos asociados. 

Toda esa información pertenece al Legado o, si el jugador desea exhibirla, al Museo. 

Fin de la Parte 2. La Parte 3 desarrollará íntegramente el Capítulo VII. Museo, incluyendo su filosofía, vitrinas, exhibiciones personalizadas y su papel como espacio de expresión del coleccionista. 

FASE 7 — PERFIL DEL JUGADOR, MUSEO Y LEGADO 

Documento Canónico (Edición Definitiva) 

### PARTE 3 

VII. Museo 

7.1 Objetivo 

El Museo constituye el espacio personal donde cada jugador puede exhibir aquello que considera más importante de su colección. 

No representa un inventario. 

No sustituye a la Colección. 

No almacena la historia del jugador. 

Su propósito es permitir que cada usuario construya una representación visual de aquello que desea conservar y compartir. 

Mientras la Colección responde a la pregunta “¿Qué poseo?”, el Museo responde a “¿Qué quiero mostrar?”. 

7.2 Filosofía 

El Museo nace como una celebración del coleccionismo. 

Uno de los pilares fundamentales del proyecto es preservar toda la historia del Pokémon TCG. 

El Museo convierte esa filosofía en una experiencia visual. 

No importa si una carta es extremadamente rara o completamente común. 

Si tiene un valor sentimental para el jugador, podrá ocupar un lugar destacado dentro de su Museo. 

El valor de una pieza no lo determina su rareza, sino la importancia que tiene para quien la exhibe. 

7.3 Independencia 

El Museo es completamente independiente de la Colección. 

Agregar una carta al Museo no crea una copia. 

Eliminar una carta del Museo tampoco elimina la carta de la Colección. 

El Museo únicamente contiene referencias a objetos existentes. 

Gracias a ello, el sistema permanece ligero, eficiente y sencillo de mantener. 

### 7.4 Personalización 

Cada jugador podrá diseñar libremente su Museo. 

No existirán configuraciones obligatorias. 

El usuario decidirá: 

Qué mostrar; 

Cómo organizarlo; 

Qué vitrinas utilizar; 

Qué colecciones destacar; 

Qué cartas ocuparán los lugares principales. 

El Museo debe reflejar la personalidad del jugador. 

### 7.5 Vitrinas 

El Museo estará compuesto por vitrinas. 

Cada vitrina representa un espacio temático independiente. 

Cada una podrá contener diferentes tipos de elementos. 

Ejemplos: 

Cartas favoritas; 

Pokémon favoritos; 

Colecciones completas; 

Expansiones favoritas; 

Cartas promocionales; 

Cartas con ilustraciones especiales; 

Mazos históricos; 

Trofeos. 

La arquitectura permitirá añadir nuevos tipos de vitrinas en futuras actualizaciones sin modificar el sistema base. 

### 7.6 Exhibición de Cartas 

El jugador podrá seleccionar cualquier carta de su Colección para exhibirla. 

No existirán restricciones por rareza. 

Podrán mostrarse: 

Cartas comunes; 

Poco comunes; 

Raras; 

Ultra raras; 

Ilustraciones especiales; 

Energías; 

Entrenadores. 

El Museo no pretende ser una vitrina de rarezas, sino una expresión personal. 

### 7.7 Exhibición de Mazos 

Los mazos también podrán formar parte del Museo. 

Esto permitirá conservar construcciones importantes para el jugador. 

Por ejemplo: 

Primer mazo competitivo; 

Mazo favorito; 

Mazo temático; 

Mazo campeón de un torneo privado. 

El mazo permanecerá accesible incluso si deja de utilizarse activamente. 

7.8 Exhibición de Trofeos 

Los trofeos obtenidos durante eventos o torneos podrán mostrarse dentro del Museo. 

Estos elementos representan logros visibles que el jugador decide compartir. 

Su presencia en el Museo será opcional. 

El mismo trofeo podrá seguir formando parte del Legado, ya que ambos sistemas cumplen funciones distintas. 

### 7.9 Organización 

Cada vitrina podrá reorganizarse libremente. 

El jugador podrá modificar su Museo tantas veces como desee. 

No existirán límites prácticos derivados del diseño. 

El sistema deberá permitir ampliar el Museo conforme el proyecto evolucione durante los años. 

7.10 Compartir el Museo 

Cuando otro jugador visite un perfil, podrá acceder al Museo si su propietario así lo permite. 

El Museo constituye una herramienta social. 

Permite conocer: 

Gustos; 

Pokémon favoritos; 

Estilo de colección; 

Logros destacados; 

Identidad como coleccionista. 

No muestra información privada ni económica. 

7.11 Integración con futuras funciones 

La arquitectura del Museo permitirá incorporar en el futuro nuevas formas de exhibición sin modificar la estructura principal. 

Ejemplos: 

Esculturas digitales; 

Recuerdos de aniversarios; 

Ilustraciones conmemorativas; 

Medallas especiales; 

Objetos exclusivos de eventos privados. 

Todos estos elementos podrán integrarse como nuevas vitrinas o categorías de exhibición. 

7.12 Filosofía del Museo 

El Museo responde a una única pregunta: 

“¿Qué quiero conservar y mostrar con orgullo?” 

No intenta representar toda la colección. 

No registra automáticamente la historia del jugador. 

No sustituye al Legado. 

Es un espacio de expresión personal que evoluciona junto con el jugador durante toda la vida del proyecto. 

Fin de la Parte 3. En la Parte 4 se desarrollará el Capítulo VIII. Legado, considerado el núcleo histórico de la cuenta, donde se definirán los hitos permanentes, reconocimientos y memoria de toda la trayectoria del jugador. 

FASE 7 — PERFIL DEL JUGADOR, MUSEO Y LEGADO 

Documento Canónico (Edición Definitiva) 

PARTE 4 

VIII. Legado 

### 8.1 Objetivo 

El Legado constituye la memoria permanente del jugador. 

Su función es conservar los acontecimientos más importantes de toda la vida de la cuenta, permitiendo que la historia personal del jugador permanezca intacta incluso después de décadas. 

No pretende registrar cada acción realizada. 

Su finalidad consiste en preservar únicamente aquellos momentos que realmente definen la trayectoria del usuario dentro del proyecto. 

### 8.2 Filosofía 

El Legado nace del principio de preservación absoluta. 

En un proyecto pensado para durar generaciones, resulta tan importante conservar las cartas como conservar la historia de quienes las obtuvieron. 

Cada jugador irá construyendo una historia única. 

El Legado será el lugar donde esa historia permanezca para siempre. 

No importa cuántas expansiones aparezcan. 

No importa cuántas temporadas transcurran. 

El Legado continuará creciendo sin perder nunca la información ya registrada. 

8.3 Registro automático 

Todo el contenido del Legado se registra automáticamente. 

El jugador no necesita realizar ninguna acción. 

Cuando ocurre un acontecimiento importante, el sistema crea un registro permanente. 

Esto garantiza la objetividad y evita manipulaciones. 

8.4 Hitos Permanentes 

El Legado podrá registrar, entre otros, acontecimientos como: 

Inicio de la aventura 

Fecha de creación de la cuenta. 

Inicio del tutorial. 

Finalización del tutorial. 

Primeras veces 

Primer sobre abierto. 

Primera carta Ultra Rara. 

Primera carta de Ilustración Especial. 

Primer Pokémon ex. 

Primer mazo creado. 

Primer combate disputado. 

Primera victoria. 

Cada uno de estos momentos ocurre una única vez y pasa a formar parte permanente de la historia del jugador. 

Coleccionismo 

Podrán registrarse hitos como: 

Primer set completado. 

Primera colección al 100%. 

Primera carta promocional. 

Primer cosmético desbloqueado. 

Primer objeto exclusivo de evento. 

Estos registros muestran la evolución del jugador como coleccionista. 

Competición 

El Legado podrá conservar acontecimientos relevantes como: 

Primer torneo jugado. Primera clasificación. Primer campeonato. Títulos importantes. 

Reconocimientos especiales. 

No se almacenará el historial completo de combates. 

Únicamente permanecerán aquellos eventos que representen hitos importantes. 

Comunidad 

En futuras versiones podrán incorporarse reconocimientos relacionados con la comunidad privada del proyecto. 

Por ejemplo: 

Participación en aniversarios. 

Eventos conmemorativos. 

Colaboraciones especiales. 

Reconocimientos otorgados por la administración. 

La arquitectura permite añadir nuevas categorías sin modificar el sistema base. 

8.5 Sin historial de combates 

Como principio arquitectónico del proyecto, el juego no almacenará un historial permanente de partidas. 

Una vez finalizado un combate: 

Se entregan las recompensas correspondientes; 

Se actualizan las estadísticas necesarias; 

Se registran los logros obtenidos, si los hubiera; 

La partida se descarta. 

No existirán: 

Historial de partidas; 

Lista de rivales anteriores; 

Repeticiones; 

Registros completos de cada combate. 

Esta decisión reduce enormemente el almacenamiento necesario, simplifica el mantenimiento y mantiene el Legado centrado en los acontecimientos realmente importantes. 

La única excepción serán los eventos y torneos relevantes, cuyos resultados podrán incorporarse como hitos permanentes. 

8.6 Cronología 

El Legado organizará automáticamente todos los hitos en orden cronológico. 

El jugador podrá recorrer su historia desde la creación de la cuenta hasta el momento actual. 

La cronología actuará como una línea del tiempo personal. 

No mostrará acciones repetitivas. 

Únicamente aparecerán acontecimientos significativos. 

### 8.7 Permanencia 

Los registros del Legado nunca podrán eliminarse. 

Una vez que un acontecimiento forma parte del Legado, permanecerá para siempre. 

Esta decisión garantiza la autenticidad de la historia del jugador y refuerza la filosofía de preservación permanente del proyecto. 

8.8 Compatibilidad futura 

El sistema ha sido diseñado para admitir nuevos tipos de hitos durante décadas. 

Cada nueva expansión, modo de juego o sistema podrá incorporar acontecimientos adicionales sin afectar los registros ya existentes. 

Esto asegura una evolución continua sin comprometer la información histórica. 

### 8.9 Filosofía del Legado 

El Legado responde a una única pregunta: 

“¿Cuál ha sido la historia de este jugador dentro del proyecto?” 

No representa el estado actual de la cuenta. 

No sustituye a la Colección. 

No es una galería como el Museo. 

Es la memoria permanente e inalterable del jugador. 

Cada registro constituye un capítulo de una historia que podrá seguir creciendo durante toda la vida del proyecto. 

Fin de la Parte 4. En la Parte 5 se desarrollarán los capítulos IX. Estadísticas 

Permanentes, X. Sistema de Personalización, XI. Integración con el resto del proyecto y el XII. Resumen Canónico, concluyendo oficialmente la Fase 7. 

FASE 7 — PERFIL DEL JUGADOR, MUSEO Y LEGADO 

Documento Canónico (Edición Definitiva) 

PARTE 5 

IX. Estadísticas Permanentes 

9.1 Objetivo 

Las Estadísticas Permanentes tienen como finalidad ofrecer un resumen cuantitativo de la actividad del jugador a lo largo de toda la vida de la cuenta. 

No constituyen un historial detallado. 

No registran cada acción realizada. 

Únicamente muestran valores acumulativos que permiten al jugador conocer su progreso global. 

9.2 Principios 

Las estadísticas deberán cumplir los siguientes principios: 

Ser claras y fáciles de interpretar. 

Actualizarse automáticamente. 

No almacenar información innecesaria. 

Mantener compatibilidad permanente con futuras actualizaciones. 

Poder ampliarse sin modificar la arquitectura existente. 

9.3 Estadísticas de Colección 

Entre otras, podrán mostrarse: 

Cartas diferentes obtenidas. 

Total de cartas en la colección. 

Sets completados. 

Cosméticos desbloqueados. 

Mazos creados. 

Objetos coleccionables obtenidos. 

9.4 Estadísticas de Juego 

Podrán incluir: 

Combates jugados. 

Victorias. 

Derrotas. 

Porcentaje de victorias. 

IA derrotadas. 

Eventos completados. 

Torneos disputados. 

Torneos ganados. 

Estas estadísticas son únicamente valores acumulados. 

El juego no conservará información individual de cada partida. 

9.5 Estadísticas de Progresión 

También podrán mostrarse indicadores como: 

Nivel actual. 

Experiencia total obtenida. 

Logros desbloqueados. 

Misiones completadas. 

Días activos. 

Sobres abiertos. 

Cartas fabricadas. 

Fichas obtenidas. 

Estas cifras permiten visualizar el crecimiento general del jugador. 

### 9.6 Evolución futura 

Las estadísticas podrán ampliarse en cualquier momento. 

Las nuevas categorías se incorporarán sin alterar las ya existentes. 

Esto garantiza que el sistema pueda evolucionar junto con el proyecto durante décadas. 

X. Sistema de Personalización 

10.1 Objetivo 

La personalización permite que cada jugador construya una identidad propia dentro del proyecto. 

Todas las opciones disponibles serán exclusivamente cosméticas. 

Nunca otorgarán ventajas competitivas. 

### 10.2 Elementos personalizables 

Entre otros, el jugador podrá modificar: 

Avatar. 

Marco. 

Fondo. 

Título. 

Insignias visibles. 

Orden del Museo. 

Vitrinas. 

Cartas destacadas. 

Mazos destacados. 

La arquitectura permitirá incorporar nuevos elementos en futuras actualizaciones. 

### 10.3 Obtención 

Los elementos cosméticos podrán obtenerse mediante sistemas ya definidos en fases anteriores, como: 

Logros. 

Eventos. 

Torneos. 

Tienda. 

Recompensas especiales. 

Contenido futuro. 

10.4 Filosofía 

La personalización representa la identidad del jugador. 

No mide habilidad. 

No refleja poder. 

Su función consiste únicamente en permitir que cada usuario exprese su personalidad dentro del proyecto. 

XI. Integración con el resto del proyecto 

La Fase 7 actúa como un punto de conexión entre todos los sistemas diseñados anteriormente. 

Su integración queda definida de la siguiente manera: 

Fase 2 — Economía Principal 

El Perfil muestra las Monedas, Cristales y Fichas disponibles. 

La gestión económica continúa perteneciendo a la Fase 2. 

Fase 3 — Tienda y Precios 

Los cosméticos adquiridos pasan automáticamente a la Colección. 

Posteriormente pueden utilizarse para personalizar el Perfil o el Museo. 

Fase 4 — Sistema de Obtención de Cartas 

Todas las cartas obtenidas ingresan directamente a la Colección. 

Desde allí podrán utilizarse para construir mazos o exhibirse en el Museo. 

Fase 5 — Sistema de Fabricación 

Las cartas fabricadas se incorporan automáticamente a la Colección. 

Las conversiones de quintas copias actualizan la cantidad correspondiente y las Fichas del jugador. 

Fase 6 — Progresión y Logros 

Los logros desbloquean recompensas que pueden reflejarse en: 

Perfil. 

Museo. 

Legado. 

Cosméticos. 

El sistema de experiencia y niveles permanece definido por la Fase 6. 

Futuras fases 

La arquitectura está preparada para integrarse con: 

PvE. 

PvP. 

Ranked. 

Eventos. 

Mercado. 

Expansiones futuras. 

Sin necesidad de rediseñar el Perfil, la Colección, el Museo o el Legado. 

XII. Resumen Canónico 

La Fase 7 establece la estructura definitiva de la identidad del jugador dentro del proyecto. 

A partir de esta fase, toda la información de la cuenta queda organizada en cuatro sistemas claramente diferenciados: 

Perfil, que representa quién es el jugador en el presente. 

Colección, que reúne todas las posesiones actuales de la cuenta. 

Museo, donde el jugador exhibe libremente aquello que desea compartir. 

Legado, que conserva de forma automática e inalterable los hitos más importantes de toda su trayectoria. 

Como principio arquitectónico, el proyecto no almacena un historial permanente de combates. Las partidas individuales se descartan una vez procesadas sus recompensas y estadísticas, preservando únicamente los acontecimientos realmente significativos mediante el sistema de Legado. 

Esta separación de responsabilidades garantiza una arquitectura limpia, escalable y preparada para mantenerse durante décadas, respetando los principios de preservación, compatibilidad histórica y evolución perpetua que definen al Pokémon TCG Clone. 

Con ello queda cerrada oficialmente la FASE 7 — PERFIL DEL JUGADOR, MUSEO Y LEGADO, estableciendo el modelo definitivo para la identidad, memoria y representación de cada jugador dentro del proyecto. 

