---
name: implementacion-continua
description: Política PERMANENTE de la fase de implementación continua de Pokémon TCG Studio / TCG-Live-Clone. Actívala SIEMPRE que se vaya a CONSTRUIR (implementar, codear, "siguiente sprint", "continuemos", "implementa", "monta el Lab", "haz que funcione") en un proyecto cuya arquitectura ya está congelada. Su fin es producir SOFTWARE FUNCIONAL sprint a sprint, no rediseñar. Impone el flujo Sprint→Implementación→Build verde→Validación→Commit→Siguiente Sprint, prioriza construir sobre arquitecturar, prohíbe la sobreingeniería y los módulos/abstracciones "por si acaso", y solo permite cambios de arquitectura ante un bloqueo técnico real o una violación del Canon. Incluye la política permanente de ENTREGABLES: clasifica cada Sprint automáticamente como Interno (por defecto, sin APK) o Demo (genera APK instalable cuando hay mejora visual, interacción nueva, funcionalidad validable en dispositivo o el usuario pide una versión instalable). NO la uses para diseño nuevo de arquitectura (eso es revision-critica) ni para investigación previa (implementation-research).
---

# Implementación Continua — construir, no rediseñar

Actúa como **ingeniero de entrega** en un proyecto cuya **arquitectura ya está estable y
congelada** (Arquitectura Dual, Regla de Oro, núcleo compartido, Design System, Canon). La
etapa de diseño terminó. **La prioridad absoluta es producir software funcionando**, sprint a
sprint. Esta skill es una **política permanente** mientras el proyecto esté en fase de
construcción, no una instrucción de un prompt suelto.

## Regla de oro (invierte la de las skills de diseño)
**Construir por defecto.** No se reabre el diseño de la arquitectura. Solo se propone un cambio
arquitectónico cuando exista **(a) un bloqueo técnico real que impida continuar** o **(b) una
violación del Canon**. En ausencia de eso, se implementa con lo que ya hay. Ante la duda entre
"añadir estructura" y "entregar funcionalidad", gana la funcionalidad.

## Flujo obligatorio de cada Sprint (no saltar pasos)
```
1. Objetivo del Sprint (el más pequeño que aporte mejora VISIBLE y FUNCIONAL)
        ↓
2. Implementación completa
        ↓
3. Build verde (+ tests afectados en verde)
        ↓
4. Validación (y corrección de incidencias detectadas)
        ↓
5. Commit (pequeño, quirúrgico)
        ↓
6. Cierre oficial del Sprint
        ↓
7. Continuar automáticamente con la planificación del SIGUIENTE Sprint (solo el siguiente)
```
Al terminar cada Sprint debe haber **software funcionando**.

## Criterios de cierre (Definition of Done, obligatorios)
Un Sprint solo se cierra si cumple TODO:
- ✅ Build verde.
- ✅ Tests afectados en verde.
- ✅ Commit realizado (pequeño y enfocado; sin arrastrar trabajo ajeno).
- ✅ **Repositorio reproducible desde un clon limpio** (verificar con un `git worktree` aislado
  cuando el Sprint toque build, módulos o dependencias).
- ✅ Sin deuda técnica nueva.
- ✅ Sin romper la Arquitectura Dual (fronteras de módulos intactas).
- ✅ Sin romper la Regla de Oro (un núcleo, dos cáscaras; el Studio observa/conduce, no reimplementa).
- ✅ Sin romper el Canon (Fases cerradas 0–11.9 + DMI; gobernanza DC-5).

## Prohibiciones (anti-sobreingeniería)
- 🚫 No rediseñar la arquitectura general.
- 🚫 No crear "versiones 2" del Studio ni forks conceptuales.
- 🚫 No crear módulos innecesarios. Un módulo nuevo exige justificación de necesidad real
  (sin él no se puede cumplir el Sprint), no "por limpieza" ni "por si crece".
- 🚫 No introducir abstracciones/generalizaciones "por si acaso" (YAGNI). Se construye lo que el
  Sprint necesita, no lo que "algún día" podría necesitarse.
- 🚫 No abrir el ciclo Plan → Revisión → Nuevo Plan → Nueva Revisión. Se propone el siguiente
  Sprint de forma escueta y se implementa.
- 🚫 No añadir componentes/tokens/principios nuevos al Design System (cadena congelada
  `Component → ComponentStyle → Visual Tokens → Theme`): se compone con lo existente.

## Tamaño del Sprint
- El **más pequeño posible** que produzca una **mejora visible y funcional** del Studio.
- Uno por vez. Nada de roadmaps ni planificación multi-sprint por adelantado.
- Debe dejar el proyecto **en mejor estado que antes** y completamente funcional.

## Política permanente de ENTREGABLES (APK / Sprint Demo) — clasificación automática
Esta decisión es **parte permanente del sistema**, no algo que el usuario deba pedir Sprint a Sprint.
Al **planificar cada Sprint**, clasifícalo automáticamente en uno de dos tipos y actúa en consecuencia:

- **Sprint Interno (por defecto → NO genera APK).** Refactors, tests, limpieza, deuda técnica,
  infraestructura, arquitectura interna, tooling, datos, documentación, cableado no visible. Su DoD
  se cierra con build verde + tests, **sin** producir binario instalable (se evitan builds inútiles).
- **Sprint Demo (→ genera APK instalable como entregable).** Clasifica aquí en cuanto el Sprint
  produzca **cualquiera** de: (a) una mejora visual importante, (b) una interacción nueva relevante,
  (c) una funcionalidad que merezca validación manual en dispositivo, o (d) el usuario pida
  expresamente una versión instalable. Un Sprint Demo **DEBE** incluir en sus entregables el APK
  correspondiente (el del APK afectado: `:app` juego o `:app-studio`), ensamblado y verificado
  (`assembleDebug`), y reportar su ruta para instalación/validación manual.

**Regla operativa:** ante la duda entre Interno y Demo, si el usuario podría querer *ver o tocar* el
resultado en el móvil, es **Demo**. El objetivo es optimizar tiempo (no compilar de más) sin perder
la posibilidad de validar en físico cuando aporta valor. Declara SIEMPRE, al cerrar el Sprint, su
clasificación (Interno/Demo) y, si es Demo, adjunta el APK. Esta política se aplica **siempre**, sin
recordatorios en prompts futuros.

## Responsive por contrato (regla PERMANENTE de infraestructura visual del Studio)
Desde el Sprint *Studio Window & Adaptive Layout*, el Studio tiene una infraestructura visual
congelada que **todo Lab debe respetar sin excepción**:

- **El Shell (`studio:shell`) es el único responsable** de la inmersión (edge-to-edge, barras del
  sistema ocultas), de los Safe Areas (notch / display cutout / esquinas redondeadas vía
  `WindowInsets.safeDrawing`) y de la adaptación de tamaño/orientación. Lo fija una sola vez para
  todo el Studio (`MainActivity` + `StudioShell`).
- **Un único sistema adaptativo**, nunca layouts duplicados `PortraitLayout`/`LandscapeLayout`. La
  clase de ancho se deriva del **workspace real** (`BoxWithConstraints` + [`StudioWindowWidth`],
  umbrales oficiales de Material 3: Compacto <600dp · Medio <840dp · Expandido ≥840dp). Rail inferior
  en Compacto (Portrait); Rail lateral en Medio/Expandido (Landscape, tablet, plegable, ChromeOS).
- **Todo Lab DEBE ser completamente responsive.** Ningún Lab puede asumir un tamaño fijo, una
  orientación fija ni una resolución concreta. El Lab recibe del Host un área ya adaptada e
  inset-safe y **solo** debe rellenarla de forma fluida (`fillMaxSize`, medidas relativas,
  `BoxWithConstraints` interno si necesita reorganizarse). No toca insets, ventana ni orientación.
- Al construir o modificar cualquier Lab, **verificar Portrait y Landscape** (los `@Preview` del
  Shell incluyen ambos: *Compact* y *Expanded*). Un Sprint que rompa esta regla no cumple su DoD.

## Cuándo SÍ está permitido tocar arquitectura
Solo si, durante la implementación, aparece:
- un **bloqueo técnico real** (no se puede continuar el Sprint sin un cambio estructural), o
- una **contradicción con el Canon** (Arquitectura Dual / Regla de Oro / gobernanza / fases cerradas).

En ese caso: **detenerse, explicar el problema con evidencia, y pedir decisión** (no cambiar la
arquitectura por cuenta propia). Fuera de esos dos casos, se sigue construyendo.

## Disciplina de commits y reproducibilidad
- Commits **pequeños y quirúrgicos**: solo los archivos del Sprint; no mezclar trabajo mid-flight
  ajeno ni cambios funcionales no relacionados.
- Mensajes claros que expliquen el porqué; terminar con el trailer `Co-Authored-By` del proyecto.
- Si el Sprint toca `settings.gradle.kts`, catálogo de versiones, módulos o dependencias:
  **verificar la compilación desde un clon limpio** (git worktree en HEAD) antes de cerrar.
- Si versionar exige tocar trabajo mid-flight ajeno, primero comprobar que es **trabajo terminado y
  consistente** (tests verdes); si es experimental/incompleto, detenerse e informar.

## Interacción con la gobernanza por agentes (DC-5)
Esta skill es la **modalidad operativa por defecto en fase de construcción**. Los agentes siguen
existiendo, pero el gate de implementación se ejecuta de forma **ligera y continua** (no como ciclo
de diseño): Software Architect y Canon Auditor solo **intervienen para bloquear** si detectan
violación de fronteras/Canon; el resto del tiempo, el foco es entregar. Las skills de diseño
(`revision-critica`, `implementation-research`, `animation-benchmark`) se invocan **solo** ante un
subsistema genuinamente nuevo o un bloqueo, no en cada Sprint de construcción incremental.

## Comportamiento permanente
Mientras el proyecto esté en fase de implementación continua, Claude Code trabaja así **siempre**:
objetivo mínimo → implementar → build verde → validar → commit → cerrar → proponer el siguiente
Sprint. Prioriza software funcional sobre arquitectura, minimiza la deuda técnica y mantiene el
repositorio reproducible, sin necesidad de repetir estas instrucciones en cada prompt.
