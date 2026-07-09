"""DeviceController — navegación de alto nivel de la app.

Porta la lógica probada de `_refcap/board.ps1`: relanzar la app, volcar la
jerarquía UI, parsear los `bounds` reales de un elemento por su texto y tocar su
centro. Al calcular siempre desde bounds evitamos coordenadas hardcodeadas; el
fallback fijo (540,1469) queda como último recurso configurable.
"""
from __future__ import annotations

import re
import time
from dataclasses import dataclass
from typing import Optional

from .adb import Adb
from . import APP_ID


@dataclass(frozen=True)
class Rect:
    left: int
    top: int
    right: int
    bottom: int

    @property
    def center(self) -> tuple[int, int]:
        return ((self.left + self.right) // 2, (self.top + self.bottom) // 2)


class ElementNotFound(RuntimeError):
    """No se halló el elemento buscado en el volcado UI actual."""


# Prefijo del contentDescription usado por el clon para marcar cajas medibles.
TAG_PREFIX = "tt:"


def _bounds_by_content_desc(xml: str) -> dict[str, Rect]:
    """Extrae {tag: Rect} de los nodos cuyo content-desc empieza por 'tt:'.

    En Compose se etiqueta cada caja con `Modifier.semantics { contentDescription =
    "tt:<tag>" }`; uiautomator la expone como `content-desc="tt:<tag>"` con sus
    `bounds` reales. Asi medimos la caja EXACTA de cada elemento del clon sin ruido
    de vision por computadora, sin depender de APIs experimentales de Compose. Si un
    tag aparece varias veces se conserva la primera aparicion (usa tags unicos).
    """
    out: dict[str, Rect] = {}
    pat = re.compile(
        r'content-desc="' + re.escape(TAG_PREFIX) + r'([^"]+)"[^>]*'
        r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    )
    for m in pat.finditer(xml):
        tag = m.group(1)
        if tag and tag not in out:
            out[tag] = Rect(int(m.group(2)), int(m.group(3)), int(m.group(4)), int(m.group(5)))
    return out


def _bounds_by_text(xml: str, text: str, which: int = 0, *, substring: bool = False) -> Optional[Rect]:
    """Extrae el `bounds` del n-ésimo elemento cuyo atributo text coincide.

    substring=True busca el texto como subcadena (útil para "¡COMBATIR!" con el
    signo de apertura invertido), replicando el match por subcadena de board.ps1.
    """
    if substring:
        core = r'text="[^"]*' + re.escape(text) + r'[^"]*"'
    else:
        core = r'text="' + re.escape(text) + r'"'
    pat = core + r'[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    matches = list(re.finditer(pat, xml))
    if len(matches) <= which:
        return None
    g = matches[which].groups()
    return Rect(int(g[0]), int(g[1]), int(g[2]), int(g[3]))


class DeviceController:
    def __init__(self, adb: Optional[Adb] = None, app_id: str = APP_ID):
        self.adb = adb or Adb()
        self.app_id = app_id

    # --- ciclo de vida de la app ---------------------------------------------
    def relaunch(self, settle: float = 3.0) -> None:
        """force-stop + launch, esperando a que la home cargue."""
        self.adb.force_stop(self.app_id)
        time.sleep(0.5)
        self.adb.launch(self.app_id)
        time.sleep(settle)

    # --- localización e interacción ------------------------------------------
    def bounds_of(self, text: str, which: int = 0, *, substring: bool = False) -> Optional[Rect]:
        return _bounds_by_text(self.adb.dump_ui(), text, which, substring=substring)

    def bounds_by_tag(self) -> dict[str, Rect]:
        """Vuelca la UI y devuelve {tag: Rect} de los elementos etiquetados (content-desc tt:)."""
        return _bounds_by_content_desc(self.adb.dump_ui())

    def tap_text(
        self,
        text: str,
        which: int = 0,
        *,
        substring: bool = False,
        fallback: Optional[tuple[int, int]] = None,
        settle: float = 0.7,
    ) -> tuple[int, int]:
        """Toca el centro del elemento cuyo texto coincide.

        Si no se encuentra y hay `fallback`, toca esas coordenadas; si no, lanza
        ElementNotFound con un mensaje accionable (las anclas son strings en
        español que pueden cambiar si cambia la UI).
        """
        rect = self.bounds_of(text, which, substring=substring)
        if rect is not None:
            x, y = rect.center
        elif fallback is not None:
            x, y = fallback
        else:
            raise ElementNotFound(
                f'No se encontró el elemento con texto {text!r} (índice {which}). '
                f"¿Cambió la UI o la pantalla no había cargado? Sube el tiempo de settle."
            )
        self.adb.tap(x, y)
        time.sleep(settle)
        return (x, y)

    def tap_xy(self, x: int, y: int, settle: float = 0.3) -> None:
        self.adb.tap(x, y)
        time.sleep(settle)
