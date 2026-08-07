# PRINCIPIO CANÓNICO — Especialización Absoluta

**Ámbito: el Developer Studio (y su relación con el proyecto Pokémon TCG Clone).**
**Prioridad: junto al Desarrollo Unipersonal, gobierna qué llega a existir en el Studio.**
Fecha de adopción: 2026-07-22.

---

## 1. Enunciado

El Developer Studio **no es un IDE genérico** y nunca debe convertirse en uno. Es la **mejor
herramienta posible para desarrollar Pokémon TCG Clone, y nada más**. Su valor nace de hacer
*una cosa* extraordinariamente bien —diseñar, probar, comparar, documentar y preservar los
sistemas visuales y técnicos de ESTE juego— no de acumular capacidades genéricas.

La medida del éxito a 20 años no es cuántas funciones tiene, sino que siga siendo **rápido,
limpio y coherente** porque **rechazó** todo lo que no sirve a este proyecto.

---

## 2. Fuera de alcance (exclusiones canónicas)

El Studio **NO** hace, y no debe hacer nunca, lo siguiente:

- ❌ **Abrir proyectos** (sirve a UN Project fijo: el Pokémon TCG Clone — coherente con el canon).
- ❌ **Explorar el sistema de archivos** (se navega por Resources y Labs, no por carpetas).
- ❌ **Editar texto genérico** (no es un editor de código).
- ❌ **Terminal integrada.**
- ❌ **Gestor de Git** (git es del desarrollador en su shell; el Studio no lo envuelve).
- ❌ **Marketplace** de ningún tipo.
- ❌ **Extensiones arbitrarias** de terceros.
- ❌ **Intentar reemplazar un IDE** (VS Code / IntelliJ hacen lo genérico; el Studio, lo específico).

Estas exclusiones son **canon**: retirar una de esta lista exige pasar por Auditoría de
Coherencia + Filosofía, no un "ya que estamos".

---

## 3. La prueba de pertenencia (cómo decidir si algo entra)

Ante cualquier función candidata, se aplican **tres preguntas**; si alguna falla, **no entra**:

1. **¿Sirve específicamente a desarrollar Pokémon TCG Clone?** (No "podría ser útil en general".)
2. **¿Reutiliza los componentes reales del juego** (engine, framework, renderers) en vez de
   introducir capacidad genérica nueva? (Regla de Oro, I1.)
3. **¿Su ausencia obligaría a "probarlo en el juego"?** Si el juego puede seguir siendo el sitio
   donde NO se experimenta, la función aporta; si es una comodidad genérica, sobra.

Regla de oro operativa: **ante la duda, NO se añade.** La entropía de las herramientas siempre
empuja hacia "más"; este principio es el contrapeso deliberado.

---

## 4. Resolución de la tensión con el sistema de plugins (crítico)

El canon (`ARCHITECTURE` §10, I6) define un **sistema de plugins de Labs**. Esto **NO**
contradice "no extensiones arbitrarias / no marketplace", pero la frontera debe ser explícita:

- **Un plugin de Lab es un módulo de ESTE proyecto**, escrito por el único desarrollador, que
  añade un *laboratorio especializado* para un dominio del juego (Animation, Rules, Shader…).
  Es **especialización interna**, no apertura.
- **NO existe** instalación de plugins de terceros, marketplace, descubrimiento remoto ni
  extensiones de propósito general. El "plugin" es un mecanismo de *modularidad interna* (I6),
  no de *extensibilidad abierta*.
- **Un Lab candidato pasa la prueba de pertenencia (§3):** si un futuro Lab no sirve
  específicamente a Pokémon TCG Clone, no se crea. La arquitectura de plugin acelera añadir
  Labs *legítimos*; no invita a añadir cualquier cosa.

En una frase: **plugins para especializar más, jamás para generalizar.**

---

## 5. Relación con los otros pilares

Los cuatro pilares de la filosofía del Studio, ahora completos:

1. **Desarrollo Unipersonal** — un solo actor humano; IA = herramientas.
2. **Especialización Absoluta** — herramienta específica de este juego; nunca IDE genérico.
3. **El juego como producto** — el cliente del juego solo consume componentes validados.
4. **El Studio como herramienta especializada** — donde todo se desarrolla antes de integrarse.

**Sinergia:** unipersonal + especializado se refuerzan — una persona no necesita (ni debe
mantener) un IDE genérico; necesita el *filo* de una herramienta hecha a medida de su único
proyecto. Ambos principios empujan a lo mínimo suficiente.

---

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No; los sirve. Menos superficie = P4 (solo lo que aporta), P10
   (menos ruido), P11 (rápido por delgado), P7 (se navega por Resources, no por archivos).
2. **¿Contradice Arquitectura Espacial / Layout / Workflows / Canon?** No. De hecho *explica*
   por qué la Biblioteca navega Resources y no un árbol de ficheros, y por qué no hay zona de
   "editor de texto" ni "terminal". Alinea con el modelo mental (Resource-céntrico).
3. **¿Excepciones innecesarias?** No; retira ambigüedad (define qué NO es el Studio).
4. **¿Duplicación de responsabilidades?** No; evita duplicar lo que ya hacen git/IDE/shell.
5. **¿Regla de Oro?** La refuerza: nada genérico nuevo; todo reutiliza el juego (§3.2).
6. **¿Escala a cientos de Labs / décadas?** Sí, y es su objetivo explícito: escala en
   *profundidad de especialización*, no en *amplitud genérica*.
7. **¿Deuda futura?** Reduce deuda (rechaza features que envejecen). Riesgo inverso: rechazar
   algo genuinamente útil por dogmatismo → mitigado por la prueba de pertenencia (§3), que es un
   criterio, no un "no" automático.
8. **¿Decisión a revisar?** Ninguna; la frontera plugins↔marketplace queda resuelta (§4).
9. **¿Canónico?** Sí — principio de gobierno de alcance, con la tensión de plugins resuelta.

# Auditoría Semántica

1. **¿Concepto ambiguo?** Se desambigua "plugin": modularidad interna, **no** extensión abierta.
2. **¿Dos palabras para un concepto?** No; "Lab" sigue siendo el término (no "extensión"/"add-on").
3. **¿Un concepto con dos significados?** "Plugin" queda acotado (§4) para que no signifique
   "extensión de terceros".
4. **¿Modelo mental construible?** Sí; refuerza el Resource-céntrico (no file-céntrico).
5. **¿Palabra a prohibir?** **Sí**, como conceptos del Studio: *marketplace, extensión (de
   terceros), add-on, plugin de terceros, editor de texto, terminal, explorador de archivos*.
6. **¿Escala décadas?** Sí; la especialización es estable y el criterio §3 la protege.

# Auditoría de Filosofía

1. **¿Respeta el Desarrollo Unipersonal?** Sí; lo complementa (una persona no mantiene un IDE
   genérico).
2. **¿Complejidad para múltiples desarrolladores?** No; ninguna.
3. **¿Puede simplificarse por ser unipersonal?** Ya es máximamente simple: menos, no más.
4. **¿IA como herramienta, no actor?** Sí; una IA tampoco "instala extensiones": asiste dentro
   del alcance especializado.
5. **¿Simplicidad, preservación, evolución a largo plazo?** Es, precisamente, la garantía de
   que el Studio siga simple y coherente a 20 años.

# Impacto en el Futuro

- **Decisiones condicionadas:** el Sistema de Interacción y todo lo posterior heredan un
  *catálogo de capacidades cerrado por criterio* (§3); ninguna pantalla añadirá terminal, editor
  genérico ni gestor de git.
- **Módulos afectados:** el sistema de plugins (`StudioLab`) se documenta explícitamente como
  *modularidad interna*, no extensibilidad abierta; la Biblioteca se confirma Resource-céntrica.
- **Oportunidades:** foco y velocidad sostenidos; cada Lab profundiza en su dominio sin presión
  de "paridad con un IDE".
- **Limitaciones (deliberadas):** el Studio jamás será una herramienta de propósito general;
  para tareas genéricas, el desarrollador usa su IDE/shell/git aparte. Es una división de trabajo
  intencional, no una carencia.
- **Riesgos a vigilar:** (R1) *feature creep* por costumbre de la industria ("todo IDE tiene
  terminal") → filtrar con §3. (R2) presión de una IA por sugerir capacidades genéricas →
  rechazar vía Auditoría de Filosofía. (R3) que "plugin" derive hacia "marketplace" → mantener §4.

# ¿A la altura de un producto AAA?

**Sí, y es justo lo que lo eleva.** Las herramientas más respetadas no son las que hacen *todo*,
sino las que hacen *su cosa* mejor que nadie: Figma no es un IDE, Blender no es un editor de
vídeo genérico, un motor especializado supera a uno genérico en su nicho. Un Studio que se niega
a diluirse en un IDE genérico y se concentra en Pokémon TCG Clone puede alcanzar un nivel de
pulido que ninguna herramienta generalista logra en ese dominio. La especialización **es** el
estándar AAA aquí — no su ausencia.
