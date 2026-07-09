"""tcgtools — automatización de dispositivo y comparación visual 1:1.

Módulos:
    adb        Wrapper de bajo nivel sobre adb.exe.
    device     DeviceController: lanzar app, leer bounds del volcado UI, tap por texto.
    capture    Captura de fotogramas (screencap en bucle o screenrecord + extracción).
    timeline   Runner guionizado (YAML) que reproduce un combato paso a paso.
    compare    SSIM + pixel-diff + template matching contra fotogramas de referencia.
    report     Reporte JSON + HTML con mapas de calor de diferencias.
    cli        Interfaz de línea de comandos (deploy/play/capture/compare).
"""

__version__ = "0.1.0"

APP_ID = "com.mineralord.tcg"
