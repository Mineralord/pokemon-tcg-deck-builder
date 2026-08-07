# Fase 11 — Screen Design (arquitectura de pantallas del Developer Studio)

**No se diseñan componentes: se ensamblan.** Cada pantalla se construye EXCLUSIVAMENTE por composición
de componentes ya existentes del Pokémon Design System (PDS), consumiendo la cadena congelada
`Component → ComponentStyle → Visual Tokens → Theme`. No se crean componentes, tokens ni principios.

## Objetivo doble
1. Definir la **arquitectura funcional** de cada pantalla (no imágenes, no mockups): finalidad, layout,
   zonas, componentes, jerarquía, flujo, navegación, estados, accesibilidad, teclado, reglas y
   restricciones.
2. **Auditar la suficiencia del PDS.** Si una pantalla exige un componente inexistente o un componente
   que no puede reutilizarse correctamente, se marca **ERROR DE ARQUITECTURA** y se detiene el proceso
   (no se crea una excepción). Completar las 12 sin ninguno demuestra que el PDS es completo y autosuficiente.

## Método (una pantalla por iteración)
Igual que la Fase 10. Cada iteración: arquitectura + componentes reutilizados + auditoría de reutilización
+ compatibilidad con el canon. Sin código Compose todavía.

## Inventario de composición disponible (ComponentStyle de la Fase 10)
- **Controles (10.1):** Button (Primary/Neutral/Ghost/Danger) · IconButton · ToggleButton ·
  SegmentedButton · TextField · SearchField · Checkbox · RadioButton · Switch · Slider · Progress ·
  Spinner · Chip · Badge.
- **Navegación/contenedores (10.2):** Tabs · Toolbar · Sidebar · Dock · Menu · ContextMenu · StatusBar ·
  Divider · ScrollArea · Scrollbar · Panel · Window.
- **Datos/inspector (10.3):** Table · Tree · TreeRow · ListView · ListRow · EmptyState · Placeholder ·
  LoadingPlaceholder · Skeleton · PropertyRow · PropertyGroup · InspectorSection.
- **Feedback/overlays (10.4):** Tooltip · Popover · Snackbar · Toast · Dialog · ModalDialog ·
  ConfirmationDialog · ProgressOverlay · BusyOverlay · BlockingOverlay.
- **Especializados (10.5):** Timeline · TimelineTrack · TimelineMarker · TimelineSelection · Preview ·
  PreviewOverlay · SelectionFrame · TransformHandle · ResizeHandle · GuideLine · DropTarget.

Los conceptos de dominio (Resource Card, tarjeta de proyecto/herramienta) se **componen** de `PanelStyle`
+ controles; no son componentes nuevos.

## Corrección de propósito (auditoría 11.0)
`screens/11.0-navigation-audit.md` corrigió el plan: el Studio es **unipersonal y de proyecto único**
(canon `PRINCIPIO-ESPECIALIZACION-ABSOLUTA`). Se **eliminan** Project Selector y Project Dashboard
(modelo IDE/multi-proyecto, prohibido por §2). El "Workspace" se reencuadra como **Shell único y
persistente**; las demás pantallas son **Labs dentro** del Shell. Arranque directo al Lab por defecto
(Galería de Animaciones), sin selector.

## Plan de pantallas corregido (Shell único + Labs)
| # | Superficie / Lab | Estado |
|---|------------------|--------|
| 11.0 | Auditoría de navegación | ✅ `screens/11.0-navigation-audit.md` |
| 11.1 | Boot + Shell (superficie única persistente) | ✅ `screens/11.1-boot-shell.md` |
| 11.2 | Galería de Animaciones *(Lab por defecto)* | ✅ `screens/11.2-animation-gallery.md` |
| 11.3 | Animation Editor | ✅ `screens/11.3-animation-editor.md` |
| 11.4 | Effect Editor | ✅ `screens/11.4-effect-editor.md` |
| 11.5 | Card Gallery | ✅ `screens/11.5-card-gallery.md` |
| 11.6 | Card Editor | ✅ `screens/11.6-card-editor.md` |
| 11.7 | Combat Lab (Rules/Battlefield) | ✅ `screens/11.7-combat-lab.md` |
| 11.8 | QA & Regression Lab (Snapshots & Replays) | ✅ `screens/11.8-qa-regression-lab.md` |
| 11.9 | Deck Sandbox | ✅ `screens/11.9-deck-sandbox.md` |
| 11.10 | Build & Validation Lab | ✅ `screens/11.10-build-validation-lab.md` |
| 11 · Auditoría Global | Auditoría transversal + dictamen final | ✅ `screens/11.11-auditoria-global.md` |

> **FASE 11 CERRADA (2026-07-22).** La Auditoría Global no detectó ERROR DE ARQUITECTURA. La arquitectura
> funcional del Studio queda incorporada al canon. A partir de aquí, cualquier cambio estructural del
> Studio debe justificarse como cualquier fase canónica (ver `00-PROCESO.md`, DC-3).
| — | Build & Validation | pendiente |
| — | Configuración | pendiente |
| — | ~~Project Selector~~ · ~~Project Dashboard~~ | **eliminadas (ERROR DE ARQUITECTURA)** |
