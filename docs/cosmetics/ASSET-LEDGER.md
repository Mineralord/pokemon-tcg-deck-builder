# Cosmetic Asset Ledger — Licencias y procedencia

Registro vivo de fuentes de assets cosméticos y su clasificación de licencia. Toda fuente
debe figurar aquí **antes** de empaquetar cualquiera de sus assets. Regla de oro del
proyecto: **solo se empaqueta contenido GREEN** (procedural propio + CC0). El resto se
cataloga para trazabilidad histórica, pero **no viaja en el repo/APK**.

## Clasificación

| Clase | Significado | ¿Se empaqueta? |
|---|---|---|
| **GREEN** | Uso permitido: arte propio procedural o CC0 (dominio público) | ✅ Sí |
| **YELLOW** | Permitido con condiciones (p.ej. CC-BY: requiere atribución) | ⚠️ Sí, con atribución registrada |
| **RED** | Propiedad de terceros / IP protegida | ❌ Nunca |
| **ARCHIVE-ONLY** | Solo referencia/histórico (extraídos de otros juegos) | ❌ Nunca (solo metadata) |

Mapea a `CosmeticLicense` (`GREEN`/`YELLOW`/`RED`/`ARCHIVE_ONLY`) y a `CosmeticStatus`
(los no distribuibles se marcan `RESTRICTED`).

## Fuentes evaluadas

| Fuente | Contenido | Licencia real | Clase | Notas |
|---|---|---|---|---|
| **Arte propio procedural** | Tapetes, fundas, avatares, marcos, cajas (Canvas Compose) | Nuestro | **GREEN** | Baseline por defecto: 0 bytes, offline, escala infinita |
| **Kenney** (kenney.nl / OpenGameArt) | UI Pack, Particle Pack, iconos, audio | **CC0** | **GREEN** | Sin atribución obligatoria; ideal para VFX/badges/SFX |
| **Kenney — Emotes Pack** (kenney.nl/assets/emotes-pack) | Emotes (caritas vector/pixel) | **CC0** | **GREEN** | ✅ **EMPAQUETADO** (1er asset IMAGE): 6 emotes vector en `app/src/main/assets/cosmetics/kenney-emotes/` (+`License.txt`). Pack `kenney-emotes.json`. |
| **OpenGameArt** (filtrado) | UI, VFX, partículas, audio | Variada | GREEN si CC0 · YELLOW si CC-BY | Verificar **asset por asset**; CC-BY exige créditos |
| itch.io / GameDev Market / CraftPix | UI/VFX premium | Licencia propia por asset | **YELLOW** | Revisar términos 1×1 antes de usar |
| **KARDS-Assets** (Gary-nope) | CardBacks, UI, medallas, audio, data JSON | Sin LICENSE; extraídos de 1939 Games; README prohíbe uso comercial | **ARCHIVE-ONLY** | Assets © 1939 Games. **Valor para nosotros = arquitectónico** (taxonomía/metadata), no los binarios |
| **Spirit-PTCGO** (Bratah123) | Emulador PTCGO (Python) | Código **GPL-3.0**; assets = IP Pokémon | Código: referencia · **Assets: RED** | Requiere poseer el cliente PTCGO; los assets son de TPCi |
| **Pokémon oficial** (cualquier origen) | Arte, logos, sonidos | © Nintendo / TPCi / Creatures | **RED** | Nunca empaquetar. Ser privado NO cambia el estatus legal |

## Assets ARCHIVE-ONLY / RESTRICTED catalogados

Presentes como **metadata** en `data/cosmetics/.../resources/cosmetics/restricted.json`
(status `RESTRICTED`, `assetRef=null` → sin binario). Existen para que el sistema "sepa que
existen" (§8/§9/§32) y para el futuro visor de Museo/Legado, pero **la tienda los filtra** y
**no se incluye ningún binario**:

- `ref.sleeve.ptcgo.legacy` — funda de referencia PTCGO (© TPCi) · ARCHIVE_ONLY
- `ref.cardback.kards.legacy` — card back de referencia KARDS (© 1939 Games) · ARCHIVE_ONLY
- `ref.avatar.pokemon.official` — avatar oficial de referencia (© Nintendo/TPCi) · RED

## Registro source-agnostic (nota importante)

El registro referencia assets por `assetRef` (ruta/URL). Si el propietario del proyecto, bajo
su criterio y riesgo, coloca manualmente un binario RESTRICTED en `assets/` y rellena su
`assetRef`, el sistema lo mostrará con trazabilidad completa. Esa decisión es del propietario;
este repositorio y su pipeline automatizado **solo empaquetan GREEN**.

## Cómo añadir una fuente nueva
1. Determinar licencia real (leer LICENSE + términos; no asumir "libre").
2. Clasificar (GREEN/YELLOW/RED/ARCHIVE-ONLY) y anotar en la tabla de arriba.
3. Si GREEN/YELLOW: registrar autor/URL en la metadata del cosmético (`source`, `author`,
   `originalUrl`); YELLOW añade la atribución a este documento.
4. Si RED/ARCHIVE-ONLY: catalogar como metadata `RESTRICTED` sin binario.
