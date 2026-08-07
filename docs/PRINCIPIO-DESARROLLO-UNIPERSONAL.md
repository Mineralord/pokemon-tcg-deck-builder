# PRINCIPIO CANÓNICO — Desarrollo Unipersonal

**Ámbito: TODO el proyecto Pokémon TCG Clone (juego + Developer Studio).**
**Prioridad: superior a futuras decisiones de arquitectura, UX e implementación.**
Fecha de adopción: 2026-07-22.

---

## 1. Enunciado

El Pokémon TCG Clone tiene **un único desarrollador humano permanente**. No existe —ni existirá—
equipo, administradores, colaboradores, diseñadores simultáneos ni revisores. En consecuencia,
**no** se modelan: flujos de aprobación, conflictos de edición, bloqueos por concurrencia,
permisos internos de desarrollo, roles, ownership ni sincronización entre desarrolladores.

Las **IA** (Claude Code, ChatGPT o cualquiera, presente o futura) son **exclusivamente
herramientas de asistencia**. Nunca son actores del sistema, nunca son "miembros del equipo" y
nunca condicionan la arquitectura como si lo fueran.

La arquitectura **optimiza el flujo de trabajo del único desarrollador humano**. No se hereda
complejidad de herramientas colaborativas solo porque sea práctica común en la industria.

**Cláusula de futuro:** si algún día se necesitara colaboración, se diseñará como una
**extensión completamente independiente**, sin tocar la arquitectura principal ni el canon.

---

## 2. Reglas operativas derivadas

- **R1 · Sin concurrencia.** Ninguna capa modela edición concurrente, locks ni merges. Si el
  desarrollador abre dos ventanas, la política es *last-write-wins simple*; no se coordina.
- **R2 · Sin identidad ni permisos.** No hay usuarios, sesiones de usuario, owners, roles ni
  permisos internos. "Session" = contexto temporal de trabajo de la única persona (no un actor).
- **R3 · Sin flujos de aprobación.** "Promover a Canonical" (Studio) es **curaduría en solitario**,
  un acto del propio desarrollador — NO un review/approval de dos partes.
- **R4 · IA como herramienta.** Entregar contexto a una IA (un escenario, un snapshot) es
  *portabilidad/asistencia*, no colaboración; la IA no aparece en el modelo de dominio.
- **R5 · El "otro" es tu yo futuro.** Toda justificación de documentación/preservación se apoya
  en *el desarrollador dentro de meses o años*, no en "otras personas" o "equipos".
- **R6 · Simplicidad por defecto.** Ante dos diseños equivalentes, gana el más simple que sirva a
  una sola persona; la complejidad "por si hay equipo" está prohibida salvo cláusula de futuro.

---

## 3. Auditoría crítica del canon existente del Developer Studio

Revisión de los 8 documentos canónicos. **No** es un "confirmo que no hay equipo": busca
supuestos implícitos y **simplificaciones habilitadas** por la unipersonalidad.

### Hallazgos y correcciones aplicadas

| # | Documento | Supuesto multi-desarrollador detectado | Acción |
|---|-----------|----------------------------------------|--------|
| H1 | `WORKFLOWS` W13 | "Compartir un escenario / que **otro desarrollador** reproduzca" | **Corregido** → "Portar/reabrir escenario": portabilidad entre *tus entornos* o hacia una **IA de asistencia**. Nunca colaboración. |
| H2 | `M0-CORE` §3–§5 | `SessionManager` con `SessionId`, `POST /api/session`, `/ws/session/{id}` (multiplexado de sesiones/usuarios) | **Simplificado** → canal WS **único** `/ws/studio`, **una sesión viva** + guardadas restaurables, sin `sessionId`, sin locks. `SessionStore` trivial. |
| H3 | `UX-PRINCIPLES` P14 | "con décadas y **rotación de personas**", "cada **nueva generación de desarrolladores**" | **Corregido** → el que retoma es *tu yo futuro* (y las IA de asistencia). Justificación más honesta y más fuerte. |
| H4 | `MENTAL-MODEL` Impacto | "reducción de bugs de comunicación **equipo↔equipo**"; "(R1) cada **equipo** inventando su jerga" | **Corregido** → malentendidos entre *tu yo presente y futuro* / con IA; deriva terminológica del *propio desarrollador o una IA*. |

### Supuestos revisados y declarados INOCUOS (no requieren cambio)
- **"Módulos compartidos" / "contrato de tipos compartido"** (`ARCHITECTURE`, `M0-CORE`): se
  refiere a **reutilización de código** juego↔Studio y a **compartir la forma de los tipos**
  frontend↔backend. Nada colaborativo. ✅
- **"perfil de rendimiento"** (`ARCHITECTURE` top bar): es *profiling*, no perfil de usuario. ✅
- **"reproducción sincronizada"** (`WORKFLOWS` W6/W9): sincroniza A y B en la comparación, no a
  personas. ✅
- **Multi-monitor / Workspaces** (`SPATIAL`, `LAYOUT`, W20/W21): un solo desarrollador con varias
  pantallas y disposiciones. NO es multi-usuario. ✅
- **"Profile"** en `MENTAL-MODEL`: aparece como *sinónimo prohibido* de Preset, no como perfil de
  usuario. ✅

### Simplificaciones NUEVAS habilitadas por el principio (y adoptadas)
1. **Sin capa de concurrencia/locks** en ninguna parte del Studio (H2). Elimina diseño entero.
2. **Session sin identidad de usuario** (H2/R2): "una viva + guardadas", no un gestor de sesiones.
3. **Preservación = git + store local del único dev** (`ARCHITECTURE` §8): ya era así; se
   **refuerza** que NO necesita branches de colaboración, PRs ni merges. El historial es lineal
   y personal.
4. **"Promover a Canonical" sin gate de aprobación** (R3): se **refuerza** como curaduría solo.

### Decisiones reforzadas
- El grafo de dependencias y la Regla de Oro (I1) son **independientes** de este principio, pero
  este principio **refuerza** la preferencia por lo simple: nada de infraestructura de equipo.
- La **cláusula de futuro** protege el canon: cualquier colaboración vendría como módulo aparte,
  con su propia Auditoría de Coherencia/Filosofía; **no** se contamina lo actual.

**Conclusión de la auditoría:** 4 correcciones aplicadas, 5 supuestos declarados inocuos con
justificación, 4 simplificaciones/refuerzos adoptados. El canon queda **alineado** con la
unipersonalidad. No queda ningún supuesto de equipo sin resolver.

---

# Auditoría de Coherencia

1. **¿Contradice P1–P15?** No; los refuerza. P13/P14 quedan mejor fundados (yo-futuro > "equipo").
2. **¿Contradice Arquitectura Espacial / Layout / Workflows / Canon?** No; solo *simplifica*
   (H2) y *reencuadra* (H1/H3/H4) sin alterar zonas, layout ni el modelo mental.
3. **¿Excepciones innecesarias?** Elimina complejidad (concurrencia/sesiones), no la añade.
4. **¿Duplicación de responsabilidades?** No; reduce (SessionStore trivial vs SessionManager).
5. **¿Regla de Oro?** Intacta; ortogonal y compatible (ambas empujan a lo mínimo necesario).
6. **¿Escala a cientos de Labs / décadas?** Sí; menos partes móviles escala mejor. La cláusula
   de futuro cubre el único escenario (colaboración) que rompería el supuesto.
7. **¿Deuda futura?** Mínima y controlada: si algún día hay colaboración, será un módulo nuevo,
   no una refactorización del canon (deuda acotada por diseño).
8. **¿Decisión a revisar?** Ninguna pendiente.
9. **¿Canónico?** Sí — principio de máxima prioridad, con auditoría del canon existente cerrada.

# Auditoría Semántica

1. **¿Concepto ambiguo?** Se aclara "Session" (contexto temporal de UNA persona, no actor).
2. **¿Dos palabras para un concepto?** `SessionManager`→`SessionStore` (una sola idea).
3. **¿Un concepto con dos significados?** "Compartir" se retira a favor de "Portar" para no
   sugerir colaboración; queda un único sentido (portabilidad).
4. **¿Modelo mental construible?** Sí, y más simple: un dev, una sesión viva, sin actores.
5. **¿Palabra a prohibir?** **Sí:** en contexto de desarrollo, prohibidos como conceptos:
   *colaborador, equipo, revisor, permiso, rol, owner, concurrencia, merge, bloqueo, sesión de
   usuario*. "Compartir" se evita salvo para portabilidad/entrega a IA.
6. **¿Escala décadas?** Sí; la unipersonalidad es estable y la cláusula de futuro la protege.

# Impacto en el Futuro

- **Decisiones condicionadas:** todo diseño futuro (Sistema de Interacción en adelante) asume un
  solo actor humano; nada de identidad/permisos/concurrencia salvo cláusula de futuro.
- **Módulos afectados:** `studio-server` (sesión única, sin multiplexado), `studio-preservation`
  (historial lineal personal), y el proceso de diseño (nueva Auditoría de Filosofía).
- **Oportunidades:** menos código, menos superficie de bugs, arranque más simple, foco absoluto
  en la productividad de una persona.
- **Limitaciones:** si un día se quisiera abrir a colaboración, **no** está soportado por el
  núcleo — a propósito. Sería un proyecto/extensión aparte.
- **Riesgos a vigilar:** (R1) que una IA "sugiera" patrones de equipo por costumbre de la
  industria → rechazarlos vía Auditoría de Filosofía. (R2) que "portar/compartir" derive hacia
  semántica colaborativa → mantener el reencuadre de W13.

# ¿A la altura de un producto AAA?

**Sí, y con ventaja.** Las herramientas AAA cargan complejidad colaborativa (permisos, cuentas,
tiempo real multi-usuario) porque *sirven a equipos*. Un tool **deliberadamente unipersonal**
puede ser **más afilado**: sin cuentas, sin sincronización, sin latencia de coordinación; todo
el presupuesto de diseño va a la productividad de un individuo. Es la misma filosofía de las
mejores herramientas *locales* de un solo usuario. Lo que "faltaría" (colaboración) se declara
explícitamente **fuera de alcance**, no como carencia sino como decisión — que es exactamente lo
que hace fuerte a una herramienta con propósito claro.
