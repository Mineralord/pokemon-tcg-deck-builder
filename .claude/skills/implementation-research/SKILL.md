---
name: implementation-research
description: Investigación técnica OBLIGATORIA antes de implementar cualquier subsistema o característica importante en TCG-Live-Clone. Su propósito NO es revisar código, sino encontrar la mejor implementación open source existente para REUTILIZAR, ADAPTAR o INSPIRARSE, y crear desde cero sólo como último recurso. Actívala SIEMPRE que se vaya a construir algo no trivial —animaciones, render, shaders, partículas, overlays, efectos visuales, audio, red, IA, persistencia, sincronización, PvP, motores, UI premium— o cuando el usuario pida "investiga", "busca implementaciones", "qué existe", "catálogo", "candidatos", "no reinventes", "reutiliza". No empieces escribiendo código: empieza investigando y entrega un informe con fichas técnicas y clasificación A/B/C/D/E antes de tocar nada.
---

# Implementation Research — investigar antes de construir

Actúa como **ingeniero de investigación técnica**. Este proyecto NO reinventa: quiere
construir el mejor Pokémon TCG posible que viva **décadas**. Antes de escribir código
nuevo para un subsistema importante, tu trabajo es encontrar la mejor implementación
existente que se pueda integrar o adaptar.

## Regla de oro
**Primero investigar, después implementar.** Nunca escribas la primera línea de un
subsistema importante sin haber entregado el informe de investigación. Esta skill NO
revisa código (eso es `revision-critica`): esta skill **busca la mejor solución que ya
existe**.

## Prioridad de decisión (en este orden estricto)
1. **Reutilizar** — integrar prácticamente sin cambios.
2. **Adaptar** — integrar con pequeñas adaptaciones a nuestra arquitectura.
3. **Inspirarse** — tomar ideas/técnica, reescribir a nuestro estilo.
4. **Crear desde cero** — SÓLO como último recurso (ver criterios abajo).

Cuando exista una implementación claramente superior, la recomendación por defecto es
**integrarla/adaptarla**, no rehacerla.

## Fuentes obligatorias (no limitarse a GitHub)
Busca en tantas como sea posible; como mínimo intenta: **GitHub, GitLab, Android
Samples, Google Samples, JetBrains, Kotlin Multiplatform Samples, Maven Central,
Awesome Lists, blogs técnicos y papers** cuando sean relevantes. Usa `WebSearch` /
`WebFetch`. Varía las consultas (nombre de técnica, "compose", "android", "library",
"sample", nombres de juegos de referencia). Registra qué fuentes consultaste.

## Ficha técnica por candidato
Para CADA implementación encontrada, genera una ficha:

**Información**: Nombre · Repositorio · URL · Autor · Última actualización · Estado del
proyecto (activo/archivado) · Licencia · Tecnologías · Dependencias.

**Calidad técnica** (evalúa cada eje): arquitectura · rendimiento · mantenimiento ·
escalabilidad · documentación · pruebas · complejidad · **compatibilidad con nuestro
proyecto**.

## Compatibilidad — verificar SIEMPRE antes de recomendar
- **Licencia** (¿permite uso/adaptación? Apache-2.0/MIT/BSD ✅; GPL/AGPL ⚠️ revisar;
  sin licencia = no usable).
- Compatibilidad con **Kotlin**.
- Compatibilidad con **Jetpack Compose**.
- Compatibilidad con **Android** (minSdk 26, compileSdk 35, Compose BOM del proyecto).
- Compatibilidad con **nuestra arquitectura** (Clean Architecture, el framework de
  animación v1.0: `RenderNode`/`Executor`/`AnimationDefinition`/`NodeRenderer`/capas).

Nunca recomiendes una solución incompatible.

## Clasificación (obligatoria por candidato)
- **A** — Integrar prácticamente sin cambios.
- **B** — Integrar con pequeñas adaptaciones.
- **C** — Usar únicamente como inspiración.
- **D** — Sólo estudiar.
- **E** — Descartar (di por qué).

## Integración por candidato
Indica siempre, sin asumir que se incorpora el repo entero:
- qué componentes **reutilizar**;
- qué componentes **adaptar**;
- qué componentes **ignorar**;
- qué componentes **reescribir**.

## Reglas fundamentales
- **Nunca** copiar un repositorio completo.
- **Nunca** copiar código indiscriminadamente.
- **Siempre** adaptar a la arquitectura existente.

## Crear desde cero — sólo si ocurre AL MENOS una:
1. no existe implementación madura;
2. todas son incompatibles con la arquitectura;
3. la licencia impide su uso;
4. la calidad es insuficiente;
5. integrarla costaría más que desarrollarla;
6. la funcionalidad es demasiado específica del proyecto.

En cualquier otro caso, recomienda reutilizar.

## Informe obligatorio (antes de escribir código)
Entrega, en este orden:
1. **Implementaciones encontradas** — lista completa (con fichas).
2. **Comparativa** — ventajas y desventajas.
3. **Clasificación** — A / B / C / D / E.
4. **Recomendación** — cuál integrar y por qué.
5. **Riesgos** — problemas potenciales.
6. **Plan de integración** — cómo incorporarla sin romper la arquitectura.

## Honestidad
No infles el catálogo con resultados irrelevantes ni recomiendes por moda. Si la mejor
decisión es construir a medida, dilo y justifícalo con los criterios de arriba. Marca
claramente cuándo un dato (fecha, licencia, estado) no pudo verificarse en vez de
inventarlo.
